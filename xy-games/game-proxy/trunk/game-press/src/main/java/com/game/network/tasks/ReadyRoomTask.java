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
 * @author wu_hc date: 2017年10月13日 下午3:32:22 <br/>
 */
public final class ReadyRoomTask implements Runnable {

	private final Player player;
	private final int roomId;

	/**
	 * @param player
	 */
	public ReadyRoomTask(Player player, int roomId) {
		this.player = player;
		this.roomId = roomId;
	}

	@Override
	public void run() {
		if (null != player) {
			RoomRequest.Builder builder = RoomRequest.newBuilder();
			builder.setType(MsgConstants.REQUST_PLAYER_READY);
			builder.setRoomId(roomId);

			Request.Builder requestBuilder = Request.newBuilder();
			requestBuilder.setRequestType(Request.RequestType.ROOM);
			requestBuilder.setExtension(Protocol.roomRequest, builder.build());
			player.getConnector().send(requestBuilder.build());
		}
	}
}
