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
 *
 * @author wu_hc date: 2017年10月24日 上午11:09:14 <br/>
 */
public final class EmjChatTask implements Runnable {

	private final Player player;

	private final long targetAccountId;

	private final int emjId;

	/**
	 * @param player
	 * @param targetAccountId
	 * @param emjId
	 */
	public EmjChatTask(Player player, long targetAccountId, int emjId) {
		this.player = player;
		this.targetAccountId = targetAccountId;
		this.emjId = emjId;
	}

	@Override
	public void run() {
		RoomRequest.Builder builder = RoomRequest.newBuilder();
		builder.setType(MsgConstants.REQUST_GOODS);
		builder.setTargetAccountId(targetAccountId);
		builder.setAppId(9);
		builder.setGoodsID(emjId);

		Request.Builder requestBuilder = Request.newBuilder();
		requestBuilder.setRequestType(Request.RequestType.ROOM);
		requestBuilder.setExtension(Protocol.roomRequest, builder.build());
		player.send(requestBuilder.build());
	}
}
