/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.util.PBUtil;
import com.cai.constant.Club;
import com.cai.constant.ClubMatchWrap;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubMatchGroup;
import protobuf.clazz.ClubMsgProto.ClubMatchInfoReqProto;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 * 
 *
 * @author wu_hc date: 2018年6月21日 下午8:54:34 <br/>
 */
@ICmd(code = C2SCmd.CLUB_MATCH_DATA_REQ, desc = "请求亲友圈比赛数据")
public final class ClubMatchInfoHandler extends IClientExHandler<ClubMatchInfoReqProto> {

	@Override
	protected void execute(ClubMatchInfoReqProto req, TransmitProto topReq, C2SSession session) throws Exception {

		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {
			if (!club.members.containsKey(topReq.getAccountId())) {
				return;
			}

			ClubMatchGroup.Builder builder = ClubMatchGroup.newBuilder();
			builder.setClubId(req.getClubId());

			if (req.getMatchIdsCount() > 0) {
				builder.setCategory(1);
				for (Long matchId : req.getMatchIdsList()) {
					ClubMatchWrap wrap = club.matchs.get(matchId);
					if (wrap != null) {
						builder.addMatchList(wrap.toBuilder());
					}
				}
			} else {
				builder.setCategory(2);
				club.matchs.forEach((id, wrap) -> {
					builder.addMatchList(wrap.toBuilder());
				});
			}

			session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_MATCH_DATA_RSP, builder));
		});
	}
}
