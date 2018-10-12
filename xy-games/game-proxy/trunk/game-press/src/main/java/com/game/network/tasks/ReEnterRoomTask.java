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
 *
 * @author wu_hc date: 2017年10月24日 上午11:09:14 <br/>
 */
public final class ReEnterRoomTask implements Runnable {

	private final Player player;

	/**
	 * @param player
	 */
	public ReEnterRoomTask(Player player) {
		this.player = player;
	}

	@Override
	public void run() {
		RoomRequest.Builder builder = RoomRequest.newBuilder();
		builder.setType(3);

		Request.Builder requestBuilder = Request.newBuilder();
		requestBuilder.setRequestType(Request.RequestType.ROOM);
		requestBuilder.setExtension(Protocol.roomRequest, builder.build());
		player.send(requestBuilder.build());
	}
}
