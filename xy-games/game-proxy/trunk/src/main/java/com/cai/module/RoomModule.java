/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.module;

import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.S2SCmd;
import com.cai.common.domain.Account;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.util.PBUtil;
import com.cai.common.util.Pair;
import com.cai.common.util.SessionUtil;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.C2SSessionService;
import com.cai.service.ClientServiceImpl;
import com.xianyi.framework.core.transport.netty.NettySocketConnector;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import protobuf.clazz.s2s.S2SProto.RoomWealthUpdateProto;

/**
 * @author wu_hc
 */
public final class RoomModule {

	/**
	 * 检测玩家是否有房间,有则告诉客户端发起重连
	 * 
	 * @param account
	 * @param session
	 */
	public static boolean checkHasRoom(final Account account, C2SSession session) {
		return null != getRoomRedisModelIfExsit(account, session);
	}

	/**
	 * 
	 * @param account
	 * @param session
	 * @return
	 */
	public static RoomRedisModel getRoomRedisModelIfExsit(final Account account, C2SSession session) {

		int room_id = getRoomId(account.getAccount_id());
		if (room_id == 0) {
			return null;
		}

		// 2 redis的房间数据
		RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM, room_id + "", RoomRedisModel.class);

		// 3缓存有，但redis没有放房间，通知中心服更新玩家缓存数据
		if (roomRedisModel == null) {
			clearRoom(account.getAccount_id(), room_id);
			return null;
		}

		return roomRedisModel;
	}

	/**
	 * 通过房间号获得房间
	 * 
	 * @param roomId
	 * @return
	 */
	public static RoomRedisModel getRoomRedisModelIfExsit(final int roomId) {
		return SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM, Integer.toString(roomId), RoomRedisModel.class);
	}

	public static int getRoomId(long accountId) {
		String roomId = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM_INFO, accountId + "", String.class);
		if (roomId == null) {
			return 0;
		}
		return Integer.parseInt(roomId);
	}

	public static void clearRoom(long accountId, int oldRoomId) {
		SpringService.getBean(RedisService.class).hDel(RedisConstant.ROOM_INFO, accountId + "");
	}

	/**
	 * 
	 * @param accountId
	 */
	public static void notifyWealthUpdate(final long accountId) {
		C2SSession client = C2SSessionService.getInstance().getSession(accountId);
		if (null == client) {
			return;
		}
		final Account account = client.getAccount();
		if (null == account) {
			return;
		}
		Pair<Integer, Integer> lastAcess = SessionUtil.getLastAccess(client);
		if (null == lastAcess) {
			return;
		}
		int source_room_id = lastAcess.getFirst().intValue(), serverIndex = lastAcess.getSecond().intValue();
		if (source_room_id <= 0 || serverIndex <= 0) {
			return;
		}
		RoomWealthUpdateProto.Builder builder = RoomWealthUpdateProto.newBuilder();
		builder.setAccountId(accountId);
		builder.setGold(account.getAccountModel().getGold());
		builder.setMoney(account.getAccountModel().getMoney());
		builder.setRoomId(source_room_id);
		NettySocketConnector connector = ClientServiceImpl.getInstance().getLogic(serverIndex);
		if (null != connector) {
			connector.send(PBUtil.toS2SRequet(S2SCmd.ACCOUNT_WEALTH_UPATE, builder).build());
		}
	}
}
