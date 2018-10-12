/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.client;

import java.util.Date;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.util.PBUtil;
import com.cai.common.util.TimeUtil;
import com.cai.constant.Club;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubCommon;
import protobuf.clazz.ClubMsgProto.ClubDailyCostProto;
import protobuf.clazz.ClubMsgProto.ClubProto;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 * 
 *
 * @author wu_hc date: 2018年3月29日 上午11:05:44 <br/>
 */
@ICmd(code = C2SCmd.CLUB_DAILY_COST, desc = "俱乐部每日消耗")
public final class ClubDailyCostHandler extends IClientExHandler<ClubCommon> {

	@Override
	protected void execute(ClubCommon req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}

		club.runInReqLoop(() -> {

			if (!club.members.containsKey(topReq.getAccountId())) {
				return;
			}
			ClubProto.Builder builder = ClubProto.newBuilder();
			builder.setClubId(club.getClubId());
			builder.addAllDailyCosts(club.dailyCosts);

			ClubDailyCostProto.Builder cost = ClubDailyCostProto.newBuilder();
			cost.setCreateAt(TimeUtil.getTimeStart(new Date(), 0));
			cost.setDailyGold(club.costGold);
			cost.setDailyCount(club.gameCount);
			cost.setDailyExclusiveGold(club.costExclusiveGold);
			builder.addDailyCosts(cost);
			
			session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_DAILY_COST, builder));
		});
	}
}
