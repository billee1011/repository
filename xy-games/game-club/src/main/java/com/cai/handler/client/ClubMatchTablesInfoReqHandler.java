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

import protobuf.clazz.s2s.S2SProto.TransmitProto;

import static protobuf.clazz.ClubMsgProto.ClubMatchCommon;
import static protobuf.clazz.ClubMsgProto.ClubMatchTableInfoProto;
import static protobuf.clazz.ClubMsgProto.ClubMatchTablesResponse;
import static protobuf.clazz.ClubMsgProto.playerInfoProto;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/9/7 11:45
 */
@ICmd(code = C2SCmd.CLUB_MATCH_TABLES_INFO_REQ, desc = "自建赛桌子信息请求")
public class ClubMatchTablesInfoReqHandler extends IClientExHandler<ClubMatchCommon> {
	@Override
	protected void execute(ClubMatchCommon req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {

			ClubMatchTablesResponse.Builder b = ClubMatchTablesResponse.newBuilder();
			b.setClubId(req.getClubId());
			b.setMatchId(req.getMatchId());
			ClubMatchWrap matchWrap = club.matchs.get(req.getMatchId());
			if (matchWrap == null || matchWrap.getModel().getStatus() != ClubMatchWrap.ClubMatchStatus.ING.status()) {
				session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_MATCH_TABLES_INFO_RSP, b));
				return;
			}
			ClubMemberModel memberModel = club.members.get(topReq.getAccountId());
			if (memberModel == null || !EClubIdentity.isManager(memberModel.getIdentity())) {
				session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_MATCH_TABLES_INFO_RSP, b));
				return;
			}

			b.setGameRound(matchWrap.ruleModel().getGame_round());

			matchWrap.clubMatchTables.forEach((roomId, table) -> {
				ClubMatchTableInfoProto.Builder tableBuilder = ClubMatchTableInfoProto.newBuilder();
				tableBuilder.setRoomId(roomId);
				tableBuilder.setCurRound(table.getCurRound());
				tableBuilder.setIsEnd(table.isEnd());
				table.getPlayers().forEach((playerId) -> {
					playerInfoProto.Builder playerBuilder = playerInfoProto.newBuilder();
					playerBuilder.setPlayerId(playerId);
					ClubMemberModel member = club.members.get(playerId);
					if (member != null) {
						playerBuilder.setNickname(member.getNickname());
					}
					tableBuilder.addPlayers(playerBuilder);
				});
				b.addTables(tableBuilder);
			});
			session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_MATCH_TABLES_INFO_RSP, b));

		});
	}
}
