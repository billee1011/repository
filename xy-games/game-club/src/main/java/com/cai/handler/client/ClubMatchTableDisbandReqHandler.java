package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.util.PBUtil;
import com.cai.constant.Club;
import com.cai.constant.ClubMatchCode;
import com.cai.constant.ClubMatchTable;
import com.cai.constant.ClubMatchWrap;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubService;
import com.cai.utils.Utils;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import static protobuf.clazz.ClubMsgProto.ClubMatchEventNotify;
import static protobuf.clazz.ClubMsgProto.ClubMatchTableDisbandProto;
import static protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/9/7 12:12
 */
@ICmd(code = C2SCmd.CLUB_MATCH_DISBAND_TABLE_REQ, desc = "自建赛管理员解散桌子")
public class ClubMatchTableDisbandReqHandler extends IClientExHandler<ClubMatchTableDisbandProto> {
	@Override
	protected void execute(ClubMatchTableDisbandProto req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {
			ClubMatchWrap matchWrap = club.matchs.get(req.getMatchId());
			if (matchWrap == null || matchWrap.getModel().getStatus() != ClubMatchWrap.ClubMatchStatus.ING.status()) {
				return;
			}
			ClubMemberModel memberModel = club.members.get(topReq.getAccountId());
			if (memberModel == null || !EClubIdentity.isManager(memberModel.getIdentity())) {
				return;
			}
			ClubMatchTable clubMatchTable = matchWrap.clubMatchTables.get(req.getRoomId());
			if (clubMatchTable != null) {
				String msg = String.format("管理员[%s]已解散该比赛牌桌，本牌桌比赛视为放弃", memberModel.getNickname());
				clubMatchTable.release(msg);
				clubMatchTable.setDisband(true);

				ClubMatchEventNotify.Builder b = ClubMatchEventNotify.newBuilder().setOperatorId(topReq.getAccountId()).setClubId(club.getClubId())
						.setEventCode(ClubMatchCode.MANAGER_DISBAND_TABLE).setMatchId(matchWrap.id()).setOperatorName(memberModel.getNickname());
				Utils.sendClient(clubMatchTable.getPlayers(), S2CCmd.CLUB_MATCH_EVENT, b);

				session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_MATCH_DISBAND_TABLE_RSP, req.toBuilder().setIsSuccess(true)));
			}
		});
	}
}
