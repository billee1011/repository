/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.game.network.tasks;

import com.game.Player;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.RoomRequest;

/**
 * 
 * 创建房间
 * 
 * @author wu_hc date: 2017年10月13日 下午3:19:46 <br/>
 */
public final class CreateRoomTask implements Runnable {

	private final Player player;

	/**
	 * @param player
	 */
	public CreateRoomTask(Player player) {
		this.player = player;
	}

	@Override
	public void run() {
		if (null != player) {

			/**
			 * type: 1 game_type_index: 8009 game_rule_index: 212 game_round: 16
			 * appId: 209
			 */
			RoomRequest.Builder builder = RoomRequest.newBuilder();
			builder.setType(1);
			builder.setGameTypeIndex(8009);
			builder.setGameRuleIndex(212);
			builder.setGameRound(16);
			builder.setAppId(209);

			// 创建牛牛
			// builder.setType(51);
			// builder.setGameTypeIndex(9006);
			// builder.setGameRuleIndex(297232);
			// builder.setGameRound(30);
			// builder.setAppId(9);

			Request.Builder requestBuilder = Request.newBuilder();
			requestBuilder.setRequestType(Request.RequestType.ROOM);
			requestBuilder.setExtension(Protocol.roomRequest, builder.build());
			player.send(requestBuilder.build());
		}
	}

}
