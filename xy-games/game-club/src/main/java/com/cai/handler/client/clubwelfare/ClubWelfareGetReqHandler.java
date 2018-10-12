package com.cai.handler.client.clubwelfare;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.domain.ClubWelfareSwitchModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.PBUtil;
import com.cai.common.util.SpringService;
import com.cai.config.ClubCfg;
import com.cai.constant.Club;
import com.cai.constant.ClubEventCode;
import com.cai.constant.ClubWelfareWrap;
import com.cai.constant.EClubIdentity;
import com.cai.dictionary.ClubWelfareSwitchModelDict;
import com.cai.service.ClubService;
import com.cai.utils.Utils;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto;

import static protobuf.clazz.ClubMsgProto.ClubCommon;
import static protobuf.clazz.ClubMsgProto.ClubWelfareGetResponse;
import static protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/9/11 14:57
 */
@ICmd(code = C2SCmd.CLUB_WELFARE_GET_REQ, desc = "亲友圈福卡领取请求")
public class ClubWelfareGetReqHandler extends IClientExHandler<ClubCommon> {
	@Override
	protected void execute(ClubCommon req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}

		club.runInReqLoop(() -> {
			long operatorId = topReq.getAccountId();
			ClubMemberModel memberModel = club.members.get(operatorId);
			if (memberModel == null || !EClubIdentity.isManager(memberModel.getIdentity())) {
				return;
			}
			ClubWelfareWrap clubWelfareWrap = club.clubWelfareWrap;
			// 检查是否开启了福卡功能
			if (!clubWelfareWrap.isOpenClubWelfare()) {
				return;
			}
			ClubWelfareGetResponse.Builder b = ClubWelfareGetResponse.newBuilder();
			b.setClubId(club.getClubId());
			ClubWelfareSwitchModel switchModel = ClubWelfareSwitchModelDict.getInstance().getClubWelfareSwitchModel();
			if (switchModel == null) {
				return;
			}
			// 检查是否满足领取条件
			if (!clubWelfareWrap.canGetWelfare()) {
				b.setRet(1);
				b.setMsg("亲友圈福卡小于" + switchModel.getCanGetCond() + "后才可领取");
				session.send(PBUtil.toS_S2CRequet(operatorId, S2CCmd.CLUB_WELFARE_GET_RSP, b));
				return;
			}

			// 检查已领取次数
			if (club.clubModel.getClubWelfareGetCount() >= ClubCfg.get().getClubWelfareDailyGetCount()) {
				b.setRet(3);
				b.setMsg("今日亲友圈福卡领取次数已用完");
				session.send(PBUtil.toS_S2CRequet(operatorId, S2CCmd.CLUB_WELFARE_GET_RSP, b));
				return;
			}

			// 检查是否绑定了手机号
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			// 检查是否在游戏认证中心绑定了手机号
			AccountModel accountModel = centerRMIServer.getAccountModel(operatorId);
			if (accountModel.getMobile_phone() == null || accountModel.getMobile_phone().isEmpty()) {
				b.setRet(2);
				b.setMsg("没有绑定手机号");
				session.send(PBUtil.toS_S2CRequet(operatorId, S2CCmd.CLUB_WELFARE_GET_RSP, b));
				return;
			}

			//领取
			clubWelfareWrap.setTotalClubWelfare(clubWelfareWrap.getTotalClubWelfare() + switchModel.getGetNum());
			club.clubModel.setClubWelfareGetCount(club.clubModel.getClubWelfareGetCount() + 1);

			b.setRet(0);
			b.setMsg("恭喜您领取" + switchModel.getGetNum() + "福卡");
			session.send(PBUtil.toS_S2CRequet(operatorId, S2CCmd.CLUB_WELFARE_GET_RSP, b));

			// 通知所有管理员亲友圈福卡数量变化
			ClubMsgProto.ClubEventProto.Builder eventBuilder = ClubMsgProto.ClubEventProto.newBuilder();
			eventBuilder.setClubId(club.getClubId());
			eventBuilder.setEventCode(ClubEventCode.WELFARE_CHANGE);
			Utils.sendClient(club.getManagerIds(), S2CCmd.CLUB_EVENT_RSP, eventBuilder);
		});
	}
}
