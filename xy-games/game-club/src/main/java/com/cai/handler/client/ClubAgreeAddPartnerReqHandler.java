package com.cai.handler.client;

import java.util.Set;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.define.EClubEventType;
import com.cai.common.domain.ClubEventLogModel;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.util.PBUtil;
import com.cai.common.util.SpringService;
import com.cai.constant.Club;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubDaoService;
import com.cai.service.ClubService;
import com.cai.utils.ClubEventLog;
import com.cai.utils.Utils;
import com.google.common.collect.Sets;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubJoinQuitMsgProto;
import protobuf.clazz.ClubMsgProto.ClubPartnerCommonResultResponse;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

import static protobuf.clazz.ClubMsgProto.ClubCommon;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/8/14 11:56
 */
@ICmd(code = C2SCmd.CLUB_AGREE_ADD_PARTNER, desc = " 亲友圈同意合伙人邀请")
public class ClubAgreeAddPartnerReqHandler extends IClientExHandler<ClubCommon> {
	@Override
	protected void execute(ClubCommon req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}

		club.runInReqLoop(() -> {
			ClubMemberModel operator = club.members.get(topReq.getAccountId());
			if (operator == null) {
				return;
			}
			ClubPartnerCommonResultResponse.Builder b = ClubPartnerCommonResultResponse.newBuilder();
			b.setAccountId(topReq.getAccountId());
			if (operator.isPartner() || operator.getParentId() > 0) {
				b.setRet(-1);
				b.setMsg("您已经是该亲友圈合伙人或者合伙人下属成员");
				session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_PARTNER_ACCEPT_INVITE, b));
				return;
			}

			operator.setIsPartner(1);
			operator.setIdentity(EClubIdentity.MANAGER.identify());
			Set<Long> notifyIds = Sets.newHashSet(club.getManagerIds());
			Utils.notityIdentityUpdate(notifyIds, operator.getAccount_id(), club.getClubId(), operator.getIdentity());
			// save db
			club.runInDBLoop(() -> {
				SpringService.getBean(ClubDaoService.class).getDao().updateClubAccountPartner(operator);
				SpringService.getBean(ClubDaoService.class).getDao().updateClubAccountIdentity(operator);
			});
			b.setRet(1);
			b.setMsg("恭喜成为" + club.getClubName() + "亲友圈合伙人");
			b.setClubId(club.getClubId());
			session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_PARTNER_ACCEPT_INVITE, b));

			// 事件
			club.sendClubPartnerEventMsgToClient(club.getOwner(), operator, null, ClubJoinQuitMsgProto.MsgType.ADD_PARTNER);
			//log
			ClubEventLog.event(new ClubEventLogModel(club.getClubId(), club.getOwnerId(), EClubEventType.ADD_PARTNER)
					.setTargetId(operator.getAccount_id()));
		});
	}
}
