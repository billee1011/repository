package com.cai.timer;

import java.util.TimerTask;

import com.cai.service.ClientServiceImpl;
import com.cai.util.MessageResponse;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.HeartRequest;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;

/**
 * 定时发送心跳给逻辑服
 * 
 * @author run
 *
 */
public class LogicHeartTimer extends TimerTask {

	@Override
	public void run() {
		// 通知逻辑服
		Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.HEAR);
		HeartRequest.Builder heartRequestBuider = HeartRequest.newBuilder();
		heartRequestBuider.setTime((int) (System.currentTimeMillis() / 1000L));
		requestBuider.setExtension(Protocol.heartRequest, heartRequestBuider.build());
		ClientServiceImpl.getInstance().sendAllLogic(requestBuider.build());
	}

}
