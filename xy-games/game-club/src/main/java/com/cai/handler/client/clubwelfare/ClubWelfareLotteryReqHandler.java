package com.cai.handler.client.clubwelfare;

import com.cai.common.ClubMemWelfareLotteryInfo;
import com.cai.common.constant.C2SCmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.constant.Club;
import com.cai.service.ClubCacheService;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import static protobuf.clazz.ClubMsgProto.ClubCommon;
import static protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/9/11 15:06
 */
@ICmd(code = C2SCmd.CLUB_WELFARE_LOTTERY_REQ, desc = "亲友圈福卡抽奖请求")
public class ClubWelfareLotteryReqHandler extends IClientExHandler<ClubCommon> {
	@Override
	protected void execute(ClubCommon req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}

		club.runInReqLoop(() -> {
			ClubMemberModel memberModel = club.members.get(topReq.getAccountId());
			if (memberModel == null) {
				return;
			}
			ClubMemWelfareLotteryInfo lotteryInfo = ClubCacheService.getInstance().lotteryMembers.get(memberModel.getAccount_id());
			if (lotteryInfo == null) {
				return;
			}

			if (club.clubWelfareWrap.isOpenClubWelfare()) {
				club.clubWelfareWrap.lotteryReward(memberModel, lotteryInfo, req.getParam1());
			}
		});
	}
}
