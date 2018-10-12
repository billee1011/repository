package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.util.PBUtil;
import com.cai.constant.Club;
import com.cai.constant.ClubMatchWrap;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubMatchCommon;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/8/22 15:38
 */
@ICmd(code = C2SCmd.CLUB_MATCH_BAN_ENROLL_REQ, desc = "自建赛关闭报名入口")
public class ClubMatchBanEnrollReqHandler extends IClientExHandler<ClubMatchCommon> {
	@Override
	protected void execute(ClubMatchCommon req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {
			ClubMemberModel operator = club.members.get(topReq.getAccountId());
			if (operator == null || !EClubIdentity.isManager(operator.getIdentity())) {
				return;
			}
			ClubMatchWrap wrap = club.matchs.get(req.getMatchId());
			if (wrap == null) {
				return;
			}

			if (wrap.getModel().getStatus() != ClubMatchWrap.ClubMatchStatus.PRE.status()) {
				return;
			}

			if (wrap.isBanEnroll() == req.getIsBan()) {
				return;
			}

			wrap.getModel().setIsBanEnroll(req.getIsBan() ? 1 : 0);

			session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_MATCH_BAN_ENROLL_RSP, req.toBuilder().setIsSuccess(true)));
		});
	}
}
