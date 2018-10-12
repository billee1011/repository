/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cai.common.constant.MsgConstants;
import com.cai.service.C2SSessionService;
import com.cai.service.PtAPIServiceImpl;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.LocationInfor;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomResponse;

/**
 * 请求指定玩家的坐标信息
 *
 * @author wu_hc date: 2017年9月4日 上午9:48:47 <br/>
 */
public class PlayerPositionTask implements Runnable {

	/**
	 * 
	 */
	private static final Logger logger = LoggerFactory.getLogger(PlayerPositionTask.class);

	/**
	 * 超时时间
	 */
	private static final int TIME_OUT = 15 * 1000;

	/**
	 * 提交任务的时间
	 */
	private final long starTime;

	/**
	 * 发起请求的玩家
	 */
	private final long accountId;

	/**
	 * 
	 */
	private final LocationInfor reqlocationInfor;
	/**
	 * 
	 */
	private final int gameId;

	/**
	 * 
	 * @param accountId
	 * @param position
	 */
	public PlayerPositionTask(long accountId, int gameId, LocationInfor reqlocationInfor) {
		this.starTime = System.currentTimeMillis();
		this.accountId = accountId;
		this.reqlocationInfor = reqlocationInfor;
		this.gameId = gameId;
	}

	@Override
	public void run() {
		if (System.currentTimeMillis() - starTime > TIME_OUT) {
			logger.error("玩家id[{}]请求位置信息:{},但超时，任务丢弃~~！", accountId, reqlocationInfor);
			return;
		}

		String result = PtAPIServiceImpl.getInstance().getbaiduPosition(this.gameId, reqlocationInfor.getPosX(), reqlocationInfor.getPosY());

		LocationInfor.Builder locationInfor = LocationInfor.newBuilder();
		locationInfor.setAddress(result);
		locationInfor.setPosX(reqlocationInfor.getPosX());
		locationInfor.setPosY(reqlocationInfor.getPosY());
		locationInfor.setTargetAccountId(reqlocationInfor.getTargetAccountId());

		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.ROOM);

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
		RoomPlayerResponse.Builder room_player = RoomPlayerResponse.newBuilder();
		room_player.setLocationInfor(locationInfor);
		roomResponse.addPlayers(room_player);
		responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());

		C2SSession session = C2SSessionService.getInstance().getSession(accountId);
		if (null != session) {
			session.send(responseBuilder.build());
		}
	}
}
