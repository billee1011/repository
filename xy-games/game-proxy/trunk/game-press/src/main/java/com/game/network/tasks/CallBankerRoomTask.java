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
 * 叫庄
 * 
 * @author wu_hc date: 2017年10月13日 下午3:19:46 <br/>
 */
public final class CallBankerRoomTask implements Runnable {

	private final Player player;

	/**
	 * @param player
	 */
	public CallBankerRoomTask(Player player) {
		this.player = player;
	}

	@Override
	public void run() {
		if (null != player) {

			// 创建牛牛
			RoomRequest.Builder builder = RoomRequest.newBuilder();
			builder.setType(MsgConstants.REQUST_ADD_SCORE);
			builder.setGameTypeIndex(9006);
			builder.setGameRuleIndex(297232);
			builder.setGameRound(10);
			builder.setAppId(9);
			builder.setSelectCallBanker(0);

			Request.Builder requestBuilder = Request.newBuilder();
			requestBuilder.setRequestType(Request.RequestType.ROOM);
			requestBuilder.setExtension(Protocol.roomRequest, builder.build());
			player.send(requestBuilder.build());
		}
	}

}
