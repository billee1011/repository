package com.cai.handler.client;

import java.util.Map;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.S2CCmd;
import com.cai.common.util.PBUtil;
import com.cai.common.util.SpringService;
import com.cai.constant.Club;
import com.cai.constant.ClubMatchWrap;
import com.cai.constant.ClubMatchWrap.ClubMatchStatus;
import com.cai.redis.service.RedisService;
import com.cai.service.ClubCacheService;
import com.cai.service.ClubService;
import com.cai.utils.RoomUtil;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubMatchEnterRoomRequestProto;
import protobuf.clazz.ClubMsgProto.ClubMatchEnterRoomResponse;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * @author zhanglong date: 2018年7月20日 上午11:00:25
 */
@ICmd(code = C2SCmd.PLAYER_ENTER_CLUB_MATCH_ROOM_REQ, desc = "玩家进入亲友圈自建赛房间请求")
public class PlayerEnterClubMatchRoomReqHandler extends IClientExHandler<ClubMatchEnterRoomRequestProto> {

	@Override
	protected void execute(ClubMatchEnterRoomRequestProto req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (club == null) {
			return;
		}
		club.runInClubLoop(() -> {
			long targetId = topReq.getAccountId();
			ClubMatchEnterRoomResponse.Builder builder = ClubMatchEnterRoomResponse.newBuilder();
			ClubMatchWrap matchWrap = club.matchs.get(req.getMatchId());
			if (matchWrap == null || matchWrap.getModel().getStatus() != ClubMatchStatus.ING.status()) {
				// 比赛已结束
				builder.setRet(2);
				builder.setMsg("该比赛已结束");
				session.send(PBUtil.toS_S2CRequet(targetId, S2CCmd.PLAYER_ENTER_CLUB_MATCH_ROOM_RSP, builder));
				return;
			}
			if (!matchWrap.enrollAccountIds().contains(targetId)) {
				// 比赛已结束
				builder.setRet(5);
				builder.setMsg("您未报名该比赛");
				session.send(PBUtil.toS_S2CRequet(targetId, S2CCmd.PLAYER_ENTER_CLUB_MATCH_ROOM_RSP, builder));
				return;
			}
			long matchId = matchWrap.id();
			Map<Long, Integer> map = ClubCacheService.getInstance().memOngoingMatchs.get(targetId);
			if (map == null || !map.containsKey(matchId)) {
				// 该玩家的这场自建赛已打完
				builder.setRet(4);
				builder.setMsg("您本场比赛已打完");
				session.send(PBUtil.toS_S2CRequet(targetId, S2CCmd.PLAYER_ENTER_CLUB_MATCH_ROOM_RSP, builder));
				return;
			}

			int matchRoomId = map.get(matchId);
			int curRoomId = RoomUtil.getRoomId(targetId);
			if (curRoomId > 0 && curRoomId != matchRoomId) {
				// 已在其他房间中
				builder.setRet(3);
				builder.setMsg("您已经在其他房间中,无法进入比赛");
				session.send(PBUtil.toS_S2CRequet(targetId, S2CCmd.PLAYER_ENTER_CLUB_MATCH_ROOM_RSP, builder));
				return;
			}

			RedisService redisService = SpringService.getBean(RedisService.class);
			Boolean result = redisService.hExists(RedisConstant.ROOM, matchRoomId + "");
			if (!result) {
				// 该玩家的这场自建赛已打完
				builder.setRet(4);
				builder.setMsg("您本场比赛已打完");
				session.send(PBUtil.toS_S2CRequet(targetId, S2CCmd.PLAYER_ENTER_CLUB_MATCH_ROOM_RSP, builder));
				return;
			}
			if (curRoomId == matchRoomId || RoomUtil.joinRoom(targetId, matchRoomId)) {
				// 可进入自建赛房间
				builder.setRet(1);
				session.send(PBUtil.toS_S2CRequet(targetId, S2CCmd.PLAYER_ENTER_CLUB_MATCH_ROOM_RSP, builder));
			}
		});
	}

}
