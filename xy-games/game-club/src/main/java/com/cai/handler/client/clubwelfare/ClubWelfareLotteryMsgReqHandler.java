package com.cai.handler.client.clubwelfare;

import java.util.List;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.domain.log.ClubWelfareLotteryMsgLogModel;
import com.cai.common.util.PBUtil;
import com.cai.constant.Club;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import static protobuf.clazz.ClubMsgProto.ClubCommon;
import static protobuf.clazz.ClubMsgProto.ClubWelfareLotteryMsgResponse;
import static protobuf.clazz.ClubMsgProto.LotteryMsgProto;
import static protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/9/11 14:59
 */
@ICmd(code = C2SCmd.CLUB_WELFARE_LOTTERY_MSG_REQ, desc = "亲友圈福卡抽奖记录请求")
public class ClubWelfareLotteryMsgReqHandler extends IClientExHandler<ClubCommon> {
	@Override
	protected void execute(ClubCommon req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}

		club.runInReqLoop(() -> {
			long operatorId = topReq.getAccountId();
			ClubMemberModel operator = club.members.get(operatorId);
			if (operator == null || !EClubIdentity.isManager(operator.getIdentity())) {
				return;
			}
			ClubWelfareLotteryMsgResponse.Builder b = ClubWelfareLotteryMsgResponse.newBuilder();
			b.setClubId(club.getClubId());
			List<ClubWelfareLotteryMsgLogModel> list = club.clubWelfareWrap.getClubWelfareLotteryLogWrap().getLogList();
			for (ClubWelfareLotteryMsgLogModel logModel : list) {
				LotteryMsgProto.Builder msgBuilder = LotteryMsgProto.newBuilder();
				msgBuilder.setRecordTime((int) (logModel.getCreate_time().getTime() / 1000));
				msgBuilder.setAccountId(logModel.getAccountId());
				msgBuilder.setNickname(logModel.getNickname() == null ? "" : logModel.getNickname());
				msgBuilder.setCost(logModel.getCostNum());
				msgBuilder.setSubName(logModel.getSubName() == null ? "" : logModel.getSubName());
				b.addMsg(msgBuilder);
			}
			session.send(PBUtil.toS_S2CRequet(operatorId, S2CCmd.CLUB_WELFARE_LOTTERY_MSG_RSP, b));
		});
	}
}
