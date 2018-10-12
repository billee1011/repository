package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.util.PBUtil;
import com.cai.constant.Club;
import com.cai.constant.ClubMatchCode;
import com.cai.constant.ClubMatchWrap;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubService;
import com.cai.service.SessionService;
import com.cai.utils.Utils;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubMatchManagerCancelEnrollProto;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

import static protobuf.clazz.ClubMsgProto.ClubMatchManagerCancelEnrollResponse;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/8/22 16:03
 */
@ICmd(code = C2SCmd.CLUB_MATCH_MANAGER_CANCEL_ENROLL_REQ, desc = "自建赛管理员取消玩家报名")
public class ClubMatchManagerCancelEnrollHandler extends IClientExHandler<ClubMatchManagerCancelEnrollProto> {
	@Override
	protected void execute(ClubMatchManagerCancelEnrollProto req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {
			ClubMemberModel operator = club.members.get(topReq.getAccountId());
			if (operator == null || !EClubIdentity.isManager(operator.getIdentity())) {
				return;
			}
			long targetId = req.getTargetId();
			ClubMemberModel target = club.members.get(targetId);
			if (target == null) {
				return;
			}
			ClubMatchWrap wrap = club.matchs.get(req.getMatchId());
			if (wrap == null) {
				return;
			}
			if (wrap.getModel().getStatus() != ClubMatchWrap.ClubMatchStatus.PRE.status()) {
				return;
			}
			if (!wrap.getEnrollAccountIds().contains(targetId)) {
				return;
			}

			if (wrap.exitMatch(targetId)) {
				if (req.getIsBan()) {
					wrap.banPlayer(targetId);
				}
				session.send(
						PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_MATCH_CANCEL_ENROLL_RESULT_RSP, req.toBuilder().setIsSuccess(true)));

				//通知玩家已被退赛
				ClubMatchManagerCancelEnrollResponse.Builder b = ClubMatchManagerCancelEnrollResponse.newBuilder();
				b.setTargetId(targetId);
				b.setClubName(club.getClubName());
				b.setMatchName(wrap.getModel().getMatchName());
				b.setClubId(club.getClubId());
				b.setMatchId(wrap.id());
				SessionService.getInstance().sendClient(targetId, S2CCmd.CLUB_MATCH_CANCEL_ENROLL, b);

				Utils.notifyClubMatchEvent(targetId, club, wrap.id(), ClubMatchCode.EXIT);
			}
		});
	}
}
