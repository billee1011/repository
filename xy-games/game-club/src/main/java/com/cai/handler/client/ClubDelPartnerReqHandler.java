package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.constant.Club;
import com.cai.constant.ClubIgnoreInviteType;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubService;
import com.cai.service.SessionService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubPartnerCommonResponse;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

import static protobuf.clazz.ClubMsgProto.ClubCommon;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/8/14 12:30
 */
@ICmd(code = C2SCmd.CLUB_DEL_PARTNER_REQ, desc = "亲友圈解除合伙人关系")
public class ClubDelPartnerReqHandler extends IClientExHandler<ClubCommon> {
	@Override
	protected void execute(ClubCommon req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}

		club.runInReqLoop(() -> {
			ClubMemberModel operator = club.members.get(topReq.getAccountId());
			if (operator == null || club.getIdentify(topReq.getAccountId()) != EClubIdentity.CREATOR) {
				return;
			}

			long targetId = req.getTargetId();
			ClubMemberModel targetModel = club.members.get(targetId);
			if (targetModel == null || !targetModel.isPartner()) {
				return;
			}
			if(targetModel.isIgnoreInvite(topReq.getAccountId(), ClubIgnoreInviteType.DEL_PARTNER)) {
				return;
			}

			ClubPartnerCommonResponse.Builder b = ClubPartnerCommonResponse.newBuilder();
			b.setAccountId(operator.getAccount_id());
			b.setAccountName(operator.getNickname());
			b.setClubName(club.getClubName());
			b.setMemberCount(club.getMemberCount());
			b.setClubId(club.getClubId());
			SessionService.getInstance().sendClient(targetId, S2CCmd.CLUB_PARTNER_DEL, b);
		});
	}
}
