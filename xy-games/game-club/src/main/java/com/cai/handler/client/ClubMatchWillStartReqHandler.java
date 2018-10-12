package com.cai.handler.client;

import java.util.Collection;
import java.util.Date;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.util.PBUtil;
import com.cai.common.util.TimeUtil;
import com.cai.config.ClubCfg;
import com.cai.constant.Club;
import com.cai.constant.ClubMatchOpenType;
import com.cai.constant.ClubMatchWrap.ClubMatchStatus;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubCommon;
import protobuf.clazz.ClubMsgProto.ClubMatchInfoProto;
import protobuf.clazz.ClubMsgProto.ClubMatchWillStartListResponse;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * @author zhanglong date: 2018年6月29日 下午3:23:10
 */
@ICmd(code = C2SCmd.CLUB_MATCH_WILL_START_REQ, desc = "亲友圈处于开赛通知阶段的比赛请求")
public class ClubMatchWillStartReqHandler extends IClientExHandler<ClubCommon> {

	@Override
	protected void execute(ClubCommon req, TransmitProto topReq, C2SSession session) throws Exception {
		Collection<Club> clubs = ClubService.getInstance().getMyEnterClub(topReq.getAccountId());
		ClubMatchWillStartListResponse.Builder builder = ClubMatchWillStartListResponse.newBuilder();
		clubs.forEach((club) -> {
			if (null == club) {
				return;
			}
			Date now = new Date();
			int clubId = club.getClubId();
			club.matchs.forEach((id, wrap) -> {
				if (wrap.getModel().getStatus() == ClubMatchStatus.PRE.status() && wrap.enrollAccountIds().contains(topReq.getAccountId())) {
					ClubMatchInfoProto.Builder b = ClubMatchInfoProto.newBuilder();
					b.setClubId(clubId);
					b.setId(id);
					b.setMatchName(wrap.getModel().getMatchName());
					b.setOpenMatchType(wrap.getModel().getOpenType());
					if (wrap.getModel().getOpenType() == ClubMatchOpenType.TIME_MODE) {
						if (wrap.getModel().getStartDate().getTime() - now.getTime() < ClubCfg.get().getClubMatchWillStartMinute() * TimeUtil.MINUTE) {
							b.setStartDate((int) (wrap.getModel().getStartDate().getTime() / 1000));
							builder.addMatchs(b);
						}
					} else if (wrap.getModel().getOpenType() == ClubMatchOpenType.COUNT_MODE) {
						builder.addMatchs(b);
					}
				}
			});
		});

		session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_MATCH_WILL_START_RSP, builder));
	}
}
