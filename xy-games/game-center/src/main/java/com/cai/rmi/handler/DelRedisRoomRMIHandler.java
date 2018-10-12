package com.cai.rmi.handler;

import java.util.List;

import com.cai.common.constant.RMICmd;
import com.cai.common.constant.RedisConstant;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

/**
 * 
 * 
 * 
 *
 * @author tang date: 2018年1月11日 下午2:15:25 <br/>
 */
@IRmi(cmd = RMICmd.DEL_REDIS_ROOM, desc = "删除redis房间")
public final class DelRedisRoomRMIHandler extends IRMIHandler<List<String>, Integer> {

	@Override
	protected Integer execute(List<String> roomIds) {
		try {
			RedisService redisService = SpringService.getBean(RedisService.class);
			for (String roomId : roomIds) {
				redisService.hDel(RedisConstant.ROOM, roomId.getBytes());
			}

		} catch (Exception e) {
			logger.error("redis operate error!", e);
		}
		return 1;
	}

}
