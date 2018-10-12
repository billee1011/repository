package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.util.PBUtil;
import com.cai.constant.Club;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubMatchRecordReq;
import protobuf.clazz.ClubMsgProto.ClubMatchRecordResponse;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author zhanglong date: 2018年7月1日 上午9:43:17
 */
@ICmd(code = C2SCmd.CLUB_MATCH_RECORD_REQ, desc = "亲友圈自建赛比赛记录请求")
public class ClubMatchRecordReqHandler extends IClientExHandler<ClubMatchRecordReq> {

	@Override
	protected void execute(ClubMatchRecordReq req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {
			if (!club.members.containsKey(topReq.getAccountId())) {
				return;
			}
			ClubMatchRecordResponse.Builder builder = ClubMatchRecordResponse.newBuilder();
			builder.setClubId(req.getClubId());
			if (club.getManagerIds().contains(topReq.getAccountId()) && req.getTargetId() <= 0) {
				club.clubMatchLogWrap.getLogList().forEach((logModel) -> {
					builder.addRecordList(logModel.toBuilder());
				});
			} else {
				club.clubMatchLogWrap.getLogList().forEach((logModel) -> {
					if (logModel.getEnrollAccountIdList() == null) {
						logModel.parseEnrollPlayer();
					}
					if (logModel.getEnrollAccountIdList() != null && logModel.getEnrollAccountIdList().contains(topReq.getAccountId())) {
						builder.addRecordList(logModel.toBuilder());
					}
				});
			}

			session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_MATCH_RECORD_DATA_RSP, builder));
		});
	}

}
