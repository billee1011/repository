package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.util.PBUtil;
import com.cai.config.ClubCfg;
import com.cai.constant.Club;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubPartnerCommon;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

import static protobuf.clazz.ClubMsgProto.ClubPartnerCommonResultResponse;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/8/14 18:32
 */
@ICmd(code = C2SCmd.AGREE_CLUB_PARTNER_INVITE_JOIN, desc = "接受亲友圈合伙人的加入邀请")
public class ClubAgreePartnerInviteJoinHandler extends IClientExHandler<ClubPartnerCommon> {
	@Override
	protected void execute(ClubPartnerCommon req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {
			long accountId = topReq.getAccountId();
			ClubPartnerCommonResultResponse.Builder b = ClubPartnerCommonResultResponse.newBuilder();
			b.setAccountId(accountId);
			if (club.members.containsKey(accountId)) {
				b.setRet(-1);
				b.setMsg("您已在该亲友圈");
				session.send(PBUtil.toS_S2CRequet(accountId, S2CCmd.AGREE_CLUB_PARTNER_INVITE_JOIN_RSP, b));
				return;
			}
			if (club.getMemberCount() >= ClubCfg.get().getClubMemberMax()) {
				b.setRet(-2);
				b.setMsg("该亲友圈人数已达上限");
				session.send(PBUtil.toS_S2CRequet(accountId, S2CCmd.AGREE_CLUB_PARTNER_INVITE_JOIN_RSP, b));
				return;
			}
			long partnerId = req.getPartnerId();
			ClubMemberModel partnerModel = club.members.get(partnerId);
			if (partnerModel == null || !partnerModel.isPartner()) {
				b.setRet(-3);
				b.setMsg("邀请人已不是该亲友圈合伙人，不能加入");
				session.send(PBUtil.toS_S2CRequet(accountId, S2CCmd.AGREE_CLUB_PARTNER_INVITE_JOIN_RSP, b));
				return;
			}
			boolean r = club.fastJoin(accountId, req);
			if (r) {
				b.setRet(1);
				b.setMsg("您已成功加入" + club.getClubName() + "亲友圈");
			} else {
				b.setRet(-4);
				b.setMsg("加入失败");
			}
			session.send(PBUtil.toS_S2CRequet(accountId, S2CCmd.AGREE_CLUB_PARTNER_INVITE_JOIN_RSP, b));
		});
	}
}
