package com.cai.handler.client;

import java.util.Map;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.util.PBUtil;
import com.cai.constant.Club;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubBanPlayerListReq;
import protobuf.clazz.ClubMsgProto.ClubBanPlayerListResponse;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author zhanglong date: 2018年5月28日 下午6:36:24
 */
@ICmd(code = C2SCmd.CLUB_BAN_PLAYER_LIST_REQ, desc = "玩家禁止同桌的玩家列表")
public class ClubBanPlayerListReqHandler extends IClientExHandler<ClubBanPlayerListReq> {

	@Override
	protected void execute(ClubBanPlayerListReq req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {
			final ClubMemberModel member = club.members.get(req.getAccountId());
			if (null == member) {
				return;
			}
			ClubBanPlayerListResponse.Builder builder = ClubBanPlayerListResponse.newBuilder();
			builder.setClubId(club.getClubId());
			builder.setAccountId(req.getAccountId());
			Map<Long, Long> map = member.getMemberBanPlayerMap();
			if (map != null) {
				for (Long targetId : map.keySet()) {
					builder.addBanAccounts(targetId);
				}
			}
			session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_BAN_PLAYER_LIST_RSP, builder));
		});
	}

}
