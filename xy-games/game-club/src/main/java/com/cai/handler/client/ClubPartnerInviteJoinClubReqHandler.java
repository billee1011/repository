package com.cai.handler.client;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.constant.Club;
import com.cai.constant.ClubIgnoreInviteType;
import com.cai.service.ClubService;
import com.cai.service.PlayerService;
import com.cai.service.SessionService;
import com.google.common.collect.Sets;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubPartnerInviteJoinClubRequest;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

import static protobuf.clazz.ClubMsgProto.ClubPartnerInviteJoinProto;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/8/14 16:55
 */
@ICmd(code = C2SCmd.CLUB_PARTNER_INVITE_JOIN_CLUB_REQ, desc = "亲友圈合伙人邀请成员加入")
public class ClubPartnerInviteJoinClubReqHandler extends IClientExHandler<ClubPartnerInviteJoinClubRequest> {
	@Override
	protected void execute(ClubPartnerInviteJoinClubRequest req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}

		club.runInReqLoop(() -> {
			ClubMemberModel operator = club.members.get(topReq.getAccountId());
			if (operator == null || !operator.isPartner()) {
				return;
			}

			ClubPartnerInviteJoinProto.Builder b = ClubPartnerInviteJoinProto.newBuilder();
			b.setClubId(club.getClubId());
			b.setCreatorId(club.getOwnerId());
			b.setCreatorName(club.getOwnerName());
			b.setClubName(club.getClubName());
			b.setMemberCount(club.getMemberCount());
			b.setOperatorId(operator.getAccount_id());
			b.setOperatorName(operator.getNickname());
			b.setOperatorAvator(operator.getAvatar());
			List<Long> targets = req.getTargetsList();
			Set<Long> emailPlayers = Sets.newHashSet();
			for (Long targetId : targets) {
				if (club.members.containsKey(targetId)) {
					continue;
				}
				// 玩家是否忽略邀请
				boolean isIgnore = false;
				Collection<Club> clubs = ClubService.getInstance().getMyEnterClub(targetId);
				for (Club tmpClub : clubs) {
					ClubMemberModel tmpModel = tmpClub.members.get(targetId);
					if (tmpModel != null && tmpModel.isIgnoreInvite(topReq.getAccountId(), ClubIgnoreInviteType.PARTNER_INVITE_JOIN)) {
						isIgnore = true;
						break;
					}
				}
				if (isIgnore) {
					continue;
				}

				if (PlayerService.getInstance().isPlayerOnline(targetId)) {
					SessionService.getInstance().sendClient(targetId, S2CCmd.CLUB_PARTNER_INVITE_JOIN, b);
				} else {
					emailPlayers.add(targetId);
				}
			}

			//			if (!emailPlayers.isEmpty()) {
			//				ClubPartnerInviteJoinMailProto.Builder builder = ClubPartnerInviteJoinMailProto.newBuilder();
			//				builder.addAllPlayerIds(emailPlayers);
			//				builder.setClubId(club.getClubId());
			//				builder.setClubName(club.getClubName());
			//				builder.setInvitorId(operator.getAccount_id());
			//				builder.setInvitorName(operator.getNickname());
			//				builder.setCreatorName(club.getOwnerName());
			//				builder.setMemberCount(club.getMemberCount());
			//				SessionService.getInstance().sendGate(1, PBUtil.toS2SRequet(S2SCmd.S_2_M, S2STransmitProto.newBuilder().setAccountId(0)
			//						.setRequest(PBUtil.toS2SResponse(S2SCmd.CLUB_PARTNER_INVITE_JOIN_MAIL_TO_MATCH, builder))).build());
			//			}
		});
	}
}
