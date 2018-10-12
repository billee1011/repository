package com.cai.handler.client.clubwelfare;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.cai.common.ClubWelfareCode;
import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.define.EClubEventType;
import com.cai.common.define.ERedHeartCategory;
import com.cai.common.domain.ClubEventLogModel;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.domain.log.ClubMemberWelfareChangeLogModel;
import com.cai.common.util.PBUtil;
import com.cai.common.util.SpringService;
import com.cai.constant.Club;
import com.cai.constant.ClubEventCode;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubDaoService;
import com.cai.service.ClubService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.utils.ClubEventLog;
import com.cai.utils.Utils;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubJoinQuitMsgProto.MsgType;

import static protobuf.clazz.ClubMsgProto.ClubEventProto;
import static protobuf.clazz.ClubMsgProto.ClubJoinQuitMsgProto;
import static protobuf.clazz.ClubMsgProto.ClubModifyPlayerWelfareReq;
import static protobuf.clazz.ClubMsgProto.ClubModifyPlayerWelfareResponse;
import static protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/9/11 15:04
 */
@ICmd(code = C2SCmd.CLUB_MODIFY_MEMBER_WELFARE_REQ, desc = "亲友圈修改玩家福卡请求")
public class ClubModifyMemberWelfareReqHandler extends IClientExHandler<ClubModifyPlayerWelfareReq> {
	@Override
	protected void execute(ClubModifyPlayerWelfareReq req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {
			// 检查是否开启了福卡功能
			if (!club.clubWelfareWrap.isOpenClubWelfare()) {
				return;
			}

			long operatorId = topReq.getAccountId();
			List<Long> targetIdList = req.getTargetsList();
			ClubMemberModel operator = club.members.get(operatorId);
			if (operator == null || !EClubIdentity.isManager(operator.getIdentity())) {
				return;
			}

			long afterWelfareValue = req.getWelfareValue();
			int needCostTotalValue = 0;
			Set<Long> notifyIds = Sets.newHashSet();
			List<ClubMemberModel> targetList = Lists.newArrayList();
			for (Long targetId : targetIdList) {
				ClubMemberModel target = club.members.get(targetId);
				if (target == null || operator.getIdentity() < target.getIdentity()) {
					continue;
				}
				long needValue = afterWelfareValue - target.getClubWelfare();
				needCostTotalValue += needValue;
				notifyIds.add(targetId);
				targetList.add(target);
			}
			ClubModifyPlayerWelfareResponse.Builder b = ClubModifyPlayerWelfareResponse.newBuilder();
			b.setClubId(club.getClubId());
			if (needCostTotalValue > 0) {
				if (club.clubWelfareWrap.getTotalClubWelfare() < needCostTotalValue) {
					b.setRet(1);
					b.setMsg("亲友圈福卡不足，请重新设置修改数量");
					session.send(PBUtil.toS_S2CRequet(operatorId, S2CCmd.CLUB_MODIFY_MEMBER_WELFARE_RSP, b));
					return;
				}
			}
			// 设置亲友圈总福卡数
			club.clubWelfareWrap.setTotalClubWelfare(club.clubWelfareWrap.getTotalClubWelfare() - needCostTotalValue);

			// 设置玩家福卡数
			List<ClubJoinQuitMsgProto> msgList = new ArrayList<>();
			for (ClubMemberModel target : targetList) {
				recordModifyLog(club, operator, target, afterWelfareValue, msgList);
				target.setClubWelfare(afterWelfareValue);
			}
			// 修改消息通知
			Utils.sendClubEventMsgBatch(club.getManagerIds(), club.getClubId(), msgList);
			Utils.notifyRedHeart(club, ERedHeartCategory.CLUB_EVENT_NOTIFY);

			club.runInDBLoop(() -> {
				SpringService.getBean(ClubDaoService.class).getDao().batchUpdate("updateClubAccountWelfare", targetList);
			});
			b.setRet(0);
			b.setMsg("修改成功");
			b.setWelfareValue(req.getWelfareValue());
			b.addAllTargets(req.getTargetsList());
			session.send(PBUtil.toS_S2CRequet(operatorId, S2CCmd.CLUB_MODIFY_MEMBER_WELFARE_RSP, b));

			//通知所有被修改成员及管理员福卡数据变化
			notifyIds.addAll(club.getManagerIds());
			ClubEventProto.Builder eventBuilder = ClubEventProto.newBuilder();
			eventBuilder.setClubId(club.getClubId());
			eventBuilder.setEventCode(ClubEventCode.WELFARE_CHANGE);
			Utils.sendClient(notifyIds, S2CCmd.CLUB_EVENT_RSP, eventBuilder);
		});
	}

	/**
	 * 修改消息和日志
	 */
	private void recordModifyLog(Club club, ClubMemberModel operator, ClubMemberModel target, long afterWelfareValue,
			List<ClubJoinQuitMsgProto> msgList) {
		ClubJoinQuitMsgProto.Builder eventMsg = Utils.newEventMsg(club.getClubId(), MsgType.MODIFY_MEM_WELFARE, operator, target);
		eventMsg.setParam1(target.getClubWelfare());
		eventMsg.setParam2(afterWelfareValue);
		club.joinQuitMsgQueueProto.offer(eventMsg.build());
		msgList.add(eventMsg.build());

		ClubEventLog.event(new ClubEventLogModel(club.getClubId(), operator.getAccount_id(), EClubEventType.MODIFY_WELFARE)
				.setTargetId(target.getAccount_id()).setParam1(target.getClubWelfare()).setParam2(afterWelfareValue));

		// 玩家福卡变动日志
		ClubMemberWelfareChangeLogModel changeLogModel = new ClubMemberWelfareChangeLogModel();
		changeLogModel.setCreate_time(new Date());
		changeLogModel.setClubId(club.getClubId());
		changeLogModel.setAccountId(target.getAccount_id());
		changeLogModel.setType(ClubWelfareCode.MEMBER_WELFARE_CHANGE_MODIFY);
		changeLogModel.setOperatorId(operator.getAccount_id());
		changeLogModel.setOperatorName(operator.getNickname());
		changeLogModel.setOldValue(target.getClubWelfare());
		changeLogModel.setNewValue(afterWelfareValue);
		MongoDBServiceImpl.getInstance().getLogQueue().add(changeLogModel);
	}
}
