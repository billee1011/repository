package com.cai.utils;

import com.cai.common.constant.RedisConstant;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

public class RoomUtil {
	public static int getRoomId(long accountId) {
		String roomId = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM_INFO, accountId + "", String.class);
		if (roomId == null) {
			return 0;
		}
		return Integer.parseInt(roomId);
	}

	public static boolean joinRoom(long accountId, int roomId) {
		return SpringService.getBean(RedisService.class).hSetNX(RedisConstant.ROOM_INFO, accountId + "", roomId + "");
	}
}