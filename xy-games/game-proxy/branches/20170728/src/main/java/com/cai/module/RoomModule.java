/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.module;

import com.cai.common.constant.RedisConstant;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.domain.Account;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.util.SpringService;
import com.cai.domain.Session;
import com.cai.redis.service.RedisService;
import com.cai.service.RedisServiceImpl;

import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountResponse;

/**
 *
 * @author wu_hc
 */
public final class RoomModule {

	/**
	 * 检测玩家是否有房间,有则告诉客户端发起重连
	 * 
	 * @param account
	 * @param session
	 */
	public static boolean checkHasRoom(final Account account, Session session) {
		// 1缓存数据是否有房间号
		int room_id = account.getRoom_id();
		if (room_id == 0) {
			return false;
		}

		// 2 redis的房间数据
		RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM, room_id + "",
				RoomRedisModel.class);

		// 3缓存有，但redis没有放房间，通知中心服更新玩家缓存数据
		if (roomRedisModel == null) {

			account.setRoom_id(0);

			// ========同步到中心========
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			//
			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			rsAccountResponseBuilder.setAccountId(account.getAccount_id());
			rsAccountResponseBuilder.setRoomId(account.getRoom_id());
			//
			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
					ERedisTopicType.topicAll);
			// =======================

			return false;
		}

		return true;
	}
}
