/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.util.PBUtil;
import com.cai.constant.Club;
import com.cai.constant.ClubActivityWrap;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubActRankProto;
import protobuf.clazz.s2s.ClubServerProto.ClubActivityTransfort;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author wu_hc date: 2018年1月29日 下午1:24:52 <br/>
 */
@ICmd(code = C2SCmd.CLUB_ACTIVITY_RANK_SNAPSHOT, desc = "--")
public final class ClubActivityRankSnapshotHandler extends IClientExHandler<ClubActRankProto> {

	@Override
	protected void execute(ClubActRankProto req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}

		if (!club.members.containsKey(topReq.getAccountId())) {
			return;
		}
		club.runInReqLoop(() -> {
			ClubActivityWrap wrap = club.activitys.get(req.getActivityId());
			if (null == wrap) {
				return;
			}
			ClubActivityTransfort.Builder builder = ClubActivityTransfort.newBuilder();
			builder.setAccountId(topReq.getAccountId());
			builder.setActivityProto(wrap.toActivityBuilder());
			builder.setStatus(wrap.status().status());
			session.send(PBUtil.toS2SResponse(S2SCmd.CLUB_ACTIVITY_RANK_RSP, builder));
		});
	}
}
