package com.cai.handler;

import com.cai.core.SystemConfig;
import com.cai.net.core.ClientHandler;
import com.cai.service.ClientServiceImpl;
import com.cai.util.MessageResponse;

import protobuf.clazz.Protocol.Request;

/**
 * 所有逻辑服消息处理
 * @author run
 *
 */
public class TestLogicMsgHandler extends ClientHandler<Request>{

	@Override
	public void onRequest() throws Exception {
		
		//消息再次封装
		Request logicRequest = MessageResponse.getLogicRequest(topRequest.toBuilder(),session).build();
		ClientServiceImpl.getInstance().sendMsg(logicRequest);
		
		
		
	}

}
