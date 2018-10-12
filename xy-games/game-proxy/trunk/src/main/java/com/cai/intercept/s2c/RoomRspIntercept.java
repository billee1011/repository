/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.intercept.s2c;

import com.cai.common.util.RuntimeOpt;
import com.cai.tasks.MsgRspTask;
import com.xianyi.framework.core.concurrent.DefaultWorkerLoopGroup;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RoomResponse;

/**
 * 
 *
 * @author wu_hc date: 2017年9月8日 下午12:31:53 <br/>
 */
public final class RoomRspIntercept implements Intercept {

	/**
	 * 俱乐部同步消息线程
	 */
	private static final DefaultWorkerLoopGroup roomWorker = DefaultWorkerLoopGroup.newGroup("room-rsp-thread", RuntimeOpt.availableProcessors());

	@Override
	public boolean intercept(Response response, C2SSession session) {
		if (response.getResponseType() != ResponseType.ROOM) {
			return false;
		}
		RoomResponse roomRsp = response.getExtension(Protocol.roomResponse);
		int roomId = roomRsp.getRoomId();
		if(roomId==0 && roomRsp.hasRoomInfo()) {
			roomId = roomRsp.getRoomInfo().getRoomId();
		}
		roomWorker.next(roomId).runInLoop(new MsgRspTask(session.getAccountID(), response));
		return true;
	}

}
