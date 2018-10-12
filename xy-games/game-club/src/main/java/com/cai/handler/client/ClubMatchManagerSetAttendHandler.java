package com.cai.handler.client;

import java.util.Set;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.define.EClubSettingStatus;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.util.PBUtil;
import com.cai.common.util.TimeUtil;
import com.cai.config.ClubCfg;
import com.cai.constant.Club;
import com.cai.constant.ClubMatchCode;
import com.cai.constant.ClubMatchOpenType;
import com.cai.constant.ClubMatchWrap;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubService;
import com.cai.utils.Utils;
import com.google.common.collect.Sets;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubMatchEventNotify;
import protobuf.clazz.ClubMsgProto.ClubMatchSetAttendMemberProto;
import protobuf.clazz.ClubMsgProto.ClubMatchSetAttendResultResponse;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

import static com.cai.constant.ClubMatchWrap.ClubMatchStatus;

/**
 * @author zhanglong date: 2018年6月26日 下午12:06:31
 */
@ICmd(code = C2SCmd.CLUB_MATCH_MANAGER_SET_ATTEND, desc = "管理员设置比赛参赛人员")
public class ClubMatchManagerSetAttendHandler extends IClientExHandler<ClubMatchSetAttendMemberProto> {

	@Override
	protected void execute(ClubMatchSetAttendMemberProto req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {
			ClubMemberModel operater = club.members.get(topReq.getAccountId());
			if (operater == null || !EClubIdentity.isManager(operater.getIdentity())) {
				return;
			}
			ClubMatchSetAttendResultResponse.Builder resultBuilder = ClubMatchSetAttendResultResponse.newBuilder();
			resultBuilder.setClubId(req.getClubId());
			resultBuilder.setMatchId(req.getMatchId());
			ClubMatchWrap wrap = club.matchs.get(req.getMatchId());
			if (wrap == null) {
				resultBuilder.setIsSuccess(false);
				resultBuilder.setMsg("比赛不存在！");
				session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_SET_ATTEND_MEM_RESULT_RSP, resultBuilder));
				return;
			}

			if (wrap.getModel().getStatus() != ClubMatchStatus.PRE.status()) {
				resultBuilder.setIsSuccess(false);
				resultBuilder.setMsg("比赛已不在报名阶段！");
				session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_SET_ATTEND_MEM_RESULT_RSP, resultBuilder));
				return;
			}
			if (club.setsModel.isStatusTrue(EClubSettingStatus.CLUB_FREEZE)) {
				resultBuilder.setIsSuccess(false);
				resultBuilder.setMsg("亲友圈已被冻结,不能设置参赛！");
				session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_SET_ATTEND_MEM_RESULT_RSP, resultBuilder));
				return;
			}
			if (wrap.isBanEnroll()) {
				resultBuilder.setIsSuccess(false);
				resultBuilder.setMsg("管理员已关闭报名入口！");
				session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_SET_ATTEND_MEM_RESULT_RSP, resultBuilder));
				return;
			}

			if (wrap.getModel().getOpenType() == ClubMatchOpenType.COUNT_MODE) {
				resultBuilder.setIsSuccess(false);
				resultBuilder.setMsg("满人赛管理员不能设置参赛！");
				session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_SET_ATTEND_MEM_RESULT_RSP, resultBuilder));
				return;
			}
			int limitTime = ClubCfg.get().getClubMatchSetEnrollTimeLimit();
			if (wrap.getModel().getStartDate().getTime() - System.currentTimeMillis() > limitTime * TimeUtil.MINUTE) {
				resultBuilder.setIsSuccess(false);
				resultBuilder.setMsg("比赛开始前" + limitTime + "分钟内才可设置参赛！");
				session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_SET_ATTEND_MEM_RESULT_RSP, resultBuilder));
				return;
			}

			if (req.getAccountsCount() > wrap.getModel().getMaxPlayerCount()) {
				resultBuilder.setIsSuccess(false);
				resultBuilder.setMsg("设置的人数超过参赛人数！");
				session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_SET_ATTEND_MEM_RESULT_RSP, resultBuilder));
				return;
			}
			Set<Long> oldEnrollAccountIds = Sets.newHashSet();
			oldEnrollAccountIds.addAll(wrap.getEnrollAccountIds());
			// 参赛列表中新增的玩家
			Set<Long> targets = Sets.newHashSet();
			wrap.getEnrollAccountIds().clear();
			boolean hasBanPlayer = false;
			for (Long target : req.getAccountsList()) {
				if (!club.members.containsKey(target)) {
					continue;
				}
				if (club.getIdentify(target) == EClubIdentity.DEFRIEND) {
					hasBanPlayer = true;
					continue;
				}
				if (wrap.getBanPlayerIds().contains(target)) {
					hasBanPlayer = true;
					continue;
				}
				if (wrap.enroll(target)) {
					if (!oldEnrollAccountIds.contains(target)) {
						targets.add(target);
					}
				}
			}
			resultBuilder.setIsSuccess(true);
			String msg = "设置成功！" + (hasBanPlayer ? "暂停娱乐和禁止参赛玩家不能设置参赛" : "");
			resultBuilder.setMsg(msg);
			session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_SET_ATTEND_MEM_RESULT_RSP, resultBuilder));
			Utils.notifyClubMatchEvent(topReq.getAccountId(), club, wrap.id(), ClubMatchCode.SET_ATTEND);

			// 向被设置参赛的玩家发送通知
			targets.remove(topReq.getAccountId());// 如果操作人也在参赛列表里，排除操作人自己

			ClubMatchEventNotify.Builder b = ClubMatchEventNotify.newBuilder().setOperatorId(club.getOwnerId()).setClubId(club.getClubId())
					.setEventCode(ClubMatchCode.BE_SET_ATTEND).setMatchId(wrap.id()).setClubName(club.getClubName())
					.setMatchName(wrap.getModel().getMatchName()).setStartTime((int) (wrap.getModel().getStartDate().getTime() / 1000L));
			Utils.sendClient(targets, S2CCmd.CLUB_MATCH_EVENT, b);

			// 参赛列表中移除的玩家
			targets = Sets.newHashSet();
			for (Long target : oldEnrollAccountIds) {
				if (!wrap.enrollAccountIds().contains(target)) {
					targets.add(target);
				}
			}
			ClubMatchEventNotify.Builder b1 = ClubMatchEventNotify.newBuilder().setOperatorId(club.getOwnerId()).setClubId(club.getClubId())
					.setEventCode(ClubMatchCode.EXIT_BY_MANAGER_SET).setMatchId(wrap.id()).setClubName(club.getClubName())
					.setMatchName(wrap.getModel().getMatchName());
			Utils.sendClient(targets, S2CCmd.CLUB_MATCH_EVENT, b1);
		});
	}

}
