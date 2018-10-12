/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.util.PBUtil;
import com.cai.constant.Club;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubCommon;
import protobuf.clazz.ClubMsgProto.ClubProto;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 * 
 *
 * @author wu_hc date: 2018年3月29日 上午11:05:44 <br/>
 */
@ICmd(code = C2SCmd.CLUB_APPLY_JOIN_MSG, desc = "请求加入俱乐部消息")
public final class ClubApplyJoinMsgHandler extends IClientExHandler<ClubCommon> {

	@Override
	protected void execute(ClubCommon req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}

		club.runInReqLoop(() -> {

			final ClubMemberModel operator = club.members.get(topReq.getAccountId());
			if (null == operator || !EClubIdentity.isManager(operator.getIdentity())) {
				return;
			}
			ClubProto.Builder builder = ClubProto.newBuilder();
			builder.setClubId(club.getClubId());
			club.requestMembers.forEach((id, pb) -> {
				builder.addApplyList(pb);
			});

			session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_APPLY_JOIN_MSG, builder));
		});

	}
}
