package com.cai.handler.client;

import java.util.ArrayList;
import java.util.List;
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
import com.cai.tasks.db.ClubMemberUpdatePartnerDBTask;
import com.cai.utils.ClubEventLog;
import com.cai.utils.Utils;
import com.google.common.collect.Sets;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto;
import protobuf.clazz.ClubMsgProto.ClubPartnerCommonResultResponse;

import static protobuf.clazz.ClubMsgProto.ClubCommon;
import static protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/8/14 14:37
 */
@ICmd(code = C2SCmd.CLUB_AGREE_DEL_PARTNER, desc = "亲友圈同意解除合伙人关系")
public class ClubAgreeDelPartnerReqHandler extends IClientExHandler<ClubCommon> {
	@Override
	protected void execute(ClubCommon req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}

		club.runInReqLoop(() -> {
			ClubMemberModel operator = club.members.get(topReq.getAccountId());
			if (operator == null || !operator.isPartner()) {
				return;
			}
			operator.setIsPartner(0);
			operator.setIdentity(EClubIdentity.COMMONER.identify());
			Set<Long> notifyIds = Sets.newHashSet(club.getManagerIds());
			notifyIds.add(operator.getAccount_id());
			Utils.notityIdentityUpdate(notifyIds, operator.getAccount_id(), club.getClubId(), operator.getIdentity());

			//解除该合伙人和其下属成员的绑定关系
			List<ClubMemberModel> tmpList = new ArrayList<>();
			club.members.forEach((id, member) -> {
				if (member.getParentId() == operator.getAccount_id()) {
					member.setParentId(0);
					tmpList.add(member);
				}
			});
			club.runInDBLoop(new ClubMemberUpdatePartnerDBTask(tmpList));
			//save db
			club.runInDBLoop(() -> {
				SpringService.getBean(ClubDaoService.class).getDao().updateClubAccountPartner(operator);
				SpringService.getBean(ClubDaoService.class).getDao().updateClubAccountIdentity(operator);
			});
			ClubPartnerCommonResultResponse.Builder b = ClubPartnerCommonResultResponse.newBuilder();
			b.setAccountId(topReq.getAccountId());
			b.setRet(1);
			b.setMsg("您已与" + club.getClubName() + "亲友圈解除合伙人关系");
			b.setClubId(club.getClubId());
			session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_PARTNER_ACCEPT_DEL, b));

			// 事件
			club.sendClubPartnerEventMsgToClient(club.getOwner(), operator, null, ClubMsgProto.ClubJoinQuitMsgProto.MsgType.DEL_PARTNER);
			//log
			ClubEventLog.event(new ClubEventLogModel(club.getClubId(), club.getOwnerId(), EClubEventType.DEL_PARTNER)
					.setTargetId(operator.getAccount_id()));
		});
	}
}
