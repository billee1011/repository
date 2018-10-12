package com.cai.handler.c2s;

import java.util.List;
import java.util.Map;

import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.util.SpringService;
import com.cai.constant.Club;
import com.cai.constant.ClubMatchWrap;
import com.cai.constant.ClubMatchWrap.ClubMatchStatus;
import com.cai.redis.service.RedisService;
import com.cai.service.ClubCacheService;
import com.cai.service.ClubService;
import com.cai.service.SessionService;
import com.cai.utils.RoomUtil;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.ClubMsgProto.PlayerOngoingClubMatchRoomNotify;
import protobuf.clazz.s2s.ClubServerProto.RoomGameOverProto;

/**
 * 
 *
 * @author zhanglong date: 2018年7月18日 上午10:00:20
 */
@ICmd(code = S2SCmd.ROOM_GAME_OVER_NOTIFY, desc = "牌局结束通知")
public class RoomGameOverNotifyHandler extends IClientHandler<RoomGameOverProto> {

	@Override
	protected void execute(RoomGameOverProto req, C2SSession session) throws Exception {
		List<Long> accounts = req.getAccountIdList();
		int roomId = req.getRoomId();
		RedisService redisService = SpringService.getBean(RedisService.class);
		for (Long target : accounts) {
			if (RoomUtil.getRoomId(target) > 0) {
				continue;
			}
			// 判断玩家是否有正在进行的亲友圈自建赛
			Map<Long, Integer> map = ClubCacheService.getInstance().memOngoingMatchs.get(target);
			if (map != null && map.size() > 0) {
				for (Map.Entry<Long, Integer> entry : map.entrySet()) {
					long matchId = entry.getKey();
					int matchRoomId = entry.getValue();
					if (roomId != matchRoomId) {
						RoomRedisModel roomRedisModel = redisService.hGet(RedisConstant.ROOM, matchRoomId + "", RoomRedisModel.class);
						if (roomRedisModel != null) {
							int clubId = roomRedisModel.getClub_id();
							Club club = ClubService.getInstance().clubs.get(clubId);
							if (club != null) {
								ClubMatchWrap matchWrap = club.matchs.get(matchId);
								if (matchWrap != null && matchWrap.getModel().getStatus() == ClubMatchStatus.ING.status()) {
									if (RoomUtil.joinRoom(target, matchRoomId)) {
										PlayerOngoingClubMatchRoomNotify.Builder builder = PlayerOngoingClubMatchRoomNotify.newBuilder();
										builder.setRoomId(matchRoomId);
										builder.setClubName(club.getClubName());
										builder.setMatchName(matchWrap.getModel().getMatchName());
										SessionService.getInstance().sendClient(target, S2CCmd.PLAYER_ONGOING_CLUB_MATCH, builder);
										break;
									}
								}
							}
						}
					}
				}
			}
		}
	}
}
