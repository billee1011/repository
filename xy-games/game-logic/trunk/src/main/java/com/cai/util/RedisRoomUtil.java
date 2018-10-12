package com.cai.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

/**
 * @author demon 
 * date: 2017年12月19日 上午11:01:48 <br/>
 */
public class RedisRoomUtil {
	
	public static Logger logger = LoggerFactory.getLogger(RedisRoomUtil.class);
	
	public static void clearRoom(long accountId, int oldRoomId){
		SpringService.getBean(RedisService.class).hDel(RedisConstant.ROOM_INFO, accountId+"");
	}
	
	public static boolean joinRoom(long accountId, int roomId){
		return SpringService.getBean(RedisService.class).hSetNX(RedisConstant.ROOM_INFO, accountId+"", roomId+"");
	}
}
