/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.game.network.tasks;

import com.cai.common.constant.MsgConstants;
import com.game.Player;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.RoomRequest;

/**
 * 
 * 开始房间
 * 
 * @author wu_hc date: 2017年10月13日 下午3:19:46 <br/>
 */
public final class StartRoomTask implements Runnable {

	private final Player player;

	/**
	 * @param player
	 */
	public StartRoomTask(Player player) {
		this.player = player;
	}

	@Override
	public void run() {
		if (null != player) {

			RoomRequest.Builder builder = RoomRequest.newBuilder();
			builder.setType(MsgConstants.REQUES_OX_GAME_START);
			builder.setGameTypeIndex(8009);
			builder.setGameRuleIndex(212);
			builder.setGameRound(24);
			builder.setAppId(209);

			// builder.setGameTypeIndex(9006);
			// builder.setGameRuleIndex(297232);
			// builder.setGameRound(10);
			// builder.setAppId(9);

			Request.Builder requestBuilder = Request.newBuilder();
			requestBuilder.setRequestType(Request.RequestType.ROOM);
			requestBuilder.setExtension(Protocol.roomRequest, builder.build());
			player.send(requestBuilder.build());
		}
	}

}
