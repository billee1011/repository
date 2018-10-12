package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.define.ESysMsgType;
import com.cai.common.util.PBUtil;
import com.cai.constant.Club;
import com.cai.constant.ClubMatchWrap;
import com.cai.service.ClubService;
import com.cai.utils.Utils;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubMatchRankDataReq;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author zhanglong date: 2018年6月29日 下午4:31:50
 */
@ICmd(code = C2SCmd.CLUB_MATCH_RANK_REQ, desc = "亲友圈自建赛排行数据请求")
public class ClubMatchRankDataReqHandler extends IClientExHandler<ClubMatchRankDataReq> {

	@Override
	protected void execute(ClubMatchRankDataReq req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			Utils.sendTip(topReq.getAccountId(), "亲友圈已解散！", ESysMsgType.INCLUDE_ERROR, session);
			return;
		}
		club.runInReqLoop(() -> {
			ClubMatchWrap wrap = club.matchs.get(req.getMatchId());
			if(wrap == null) {
				return;
			}
			session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_MATCH_RANK_DATA_RSP, wrap.toRankBuilder()));
		});

	}

}
