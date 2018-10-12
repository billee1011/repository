/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.tasks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.cai.common.constant.S2CCmd;
import com.cai.common.define.EGameType;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.PBUtil;
import com.cai.common.util.SpringService;
import com.google.common.collect.Maps;

import io.netty.channel.Channel;
import protobuf.clazz.c2s.C2SProto.MessageReceiveRsp;

/**
 * 
 * 仅为压力测试用
 * 
 * @author wu_hc date: 2017年10月26日 上午10:22:49 <br/>
 */
public final class PressGetFreeRoomTask implements Runnable {

	public static volatile Map<Integer, AtomicInteger> roomPlayerNums = Maps.newConcurrentMap();

	private final Channel channel;

	/**
	 * @param channel
	 */
	public PressGetFreeRoomTask(Channel channel) {
		this.channel = channel;
	}

	@Override
	public void run() {
		try {
			int roomId = -1;
			for (Map.Entry<Integer, AtomicInteger> entry : roomPlayerNums.entrySet()) {
				if (entry.getValue().getAndIncrement() < 4) {
					roomId = entry.getKey().intValue();
					break;
				}
			}

			if (roomId == -1) {

				roomPlayerNums.clear();
				ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
				final List<RoomRedisModel> roomRedisModels = centerRMIServer.getAllRoomRedisModelList();

				for (final RoomRedisModel model : roomRedisModels) {
					if (model.getGame_id() == EGameType.PHUYX.getId() && model.getCur_player_num() < model.getPlayer_max()) {
						roomPlayerNums.put(model.getRoom_id(), new AtomicInteger(model.getCur_player_num()));
					}
				}

				for (Map.Entry<Integer, AtomicInteger> entry : roomPlayerNums.entrySet()) {
					if (entry.getValue().getAndIncrement() < 4) {
						roomId = entry.getKey().intValue();
						break;
					}
				}
			}

			MessageReceiveRsp.Builder builder = MessageReceiveRsp.newBuilder();
			builder.setType(roomId);
			channel.writeAndFlush(PBUtil.toS2CCommonRsp(S2CCmd.FAST_JOIN_ROOM, builder));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
