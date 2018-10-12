package com.cai.handler.client.clubwelfare;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.domain.ClubWelfareSwitchModel;
import com.cai.common.util.PBUtil;
import com.cai.constant.Club;
import com.cai.constant.EClubIdentity;
import com.cai.dictionary.ClubWelfareSwitchModelDict;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import static protobuf.clazz.ClubMsgProto.ClubCommon;
import static protobuf.clazz.ClubMsgProto.ClubWelfareInfoResponse;
import static protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/9/11 14:54
 */
@ICmd(code = C2SCmd.CLUB_MEMBER_WELFARE_REQ, desc = "玩家亲友圈福卡请求")
public class ClubMemberWelfareReqHandler extends IClientExHandler<ClubCommon> {
	@Override
	protected void execute(ClubCommon req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}

		club.runInReqLoop(() -> {
			long operatorId = topReq.getAccountId();
			ClubMemberModel memberModel = club.members.get(operatorId);
			if (memberModel == null) {
				return;
			}
			ClubWelfareInfoResponse.Builder b = ClubWelfareInfoResponse.newBuilder();
			b.setClubId(club.getClubId());
			b.setSelfClubWelfare(memberModel.getClubWelfare());
			if (EClubIdentity.isManager(memberModel.getIdentity())) {
				b.setTotalClubWelfare(club.clubWelfareWrap.getTotalClubWelfare());
			}
			ClubWelfareSwitchModel switchModel = ClubWelfareSwitchModelDict.getInstance().getClubWelfareSwitchModel();
			if (switchModel != null) {
				b.setCanGetValue(switchModel.getCanGetCond());
			}
			session.send(PBUtil.toS_S2CRequet(operatorId, S2CCmd.CLUB_WELFARE_INFO_RSP, b));
		});
	}
}
