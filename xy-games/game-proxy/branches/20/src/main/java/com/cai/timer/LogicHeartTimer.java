package com.cai.timer;

import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.service.ClientServiceImpl;
import com.cai.util.MessageResponse;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.HeartRequest;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;

/**
 * 定时发送心跳给逻辑服
 * @author run
 *
 */
public class LogicHeartTimer extends TimerTask{

	private static Logger logger = LoggerFactory.getLogger(LogicHeartTimer.class);
	
	@Override
	public void run() {
		//logger.info("心跳通知逻辑计算服.......");
		//通知逻辑服
		Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.HEAR);
		HeartRequest.Builder heartRequestBuider = HeartRequest.newBuilder();
		heartRequestBuider.setTime((int)(System.currentTimeMillis()/1000L));
		requestBuider.setExtension(Protocol.heartRequest,heartRequestBuider.build());
		ClientServiceImpl.getInstance().sendMsg(requestBuider.build());
	}

}
