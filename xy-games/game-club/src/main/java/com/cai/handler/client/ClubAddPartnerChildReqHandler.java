package com.cai.handler.client;

import java.util.List;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.define.EClubEventType;
import com.cai.common.domain.ClubEventLogModel;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.util.PBUtil;
import com.cai.constant.Club;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubService;
import com.cai.tasks.db.ClubMemberUpdatePartnerDBTask;
import com.cai.utils.ClubEventLog;
import com.google.common.collect.Lists;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubPartnerCommon;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

import static protobuf.clazz.ClubMsgProto.ClubJoinQuitMsgProto;
import static protobuf.clazz.ClubMsgProto.ClubPartnerCommonResultResponse;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/8/14 15:24
 */
@ICmd(code = C2SCmd.CLUB_ADD_PARTNER_CHILD_REQ, desc = "亲友圈设置合伙人子成员")
public class ClubAddPartnerChildReqHandler extends IClientExHandler<ClubPartnerCommon> {
	@Override
	protected void execute(ClubPartnerCommon req, TransmitProto topReq, C2SSession session) throws Exception {
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
			ClubMemberModel targetMember = club.members.get(targetId);
			if (targetMember == null || targetMember.isPartner() || targetMember.getParentId() > 0) {
				return;
			}
			long partnerId = req.getPartnerId();
			ClubMemberModel partnerMember = club.members.get(partnerId);
			if (partnerMember == null || !partnerMember.isPartner()) {
				return;
			}
			targetMember.setParentId(partnerId);
			//save db
			List<ClubMemberModel> list = Lists.newArrayList();
			list.add(targetMember);
			club.runInDBLoop(new ClubMemberUpdatePartnerDBTask(list));

			ClubPartnerCommonResultResponse.Builder b = ClubPartnerCommonResultResponse.newBuilder();
			b.setAccountId(targetMember.getAccount_id());
			b.setRet(1);
			b.setMsg("绑定合伙人成功");
			b.setClubId(club.getClubId());
			b.setParentId(partnerId);
			session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_ADD_PARTNER_CHILD_RSP, b));

			// 事件
			club.sendClubPartnerEventMsgToClient(club.getOwner(), targetMember, partnerMember, ClubJoinQuitMsgProto.MsgType.ADD_CHILD);
			//log
			ClubEventLog.event(new ClubEventLogModel(club.getClubId(), club.getOwnerId(), EClubEventType.ADD_CHILD)
					.setTargetId(partnerMember.getAccount_id()).setParam1(targetMember.getAccount_id()));
		});
	}
}
