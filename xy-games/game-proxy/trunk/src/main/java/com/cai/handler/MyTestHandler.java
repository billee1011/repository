/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.handler;

import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.MyTestRequest;
import protobuf.clazz.Protocol.MyTestResponse;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;

/**
 *
 * @author wu_hc
 */
@ICmd(code = RequestType.MY_TEST_VALUE, exName = "myTestRequest")
public class MyTestHandler extends IClientHandler<MyTestRequest> {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.xianyi.framework.handler.IClientHandler#execute(com.google.protobuf.
	 * GeneratedMessage, protobuf.clazz.Protocol.Request,
	 * com.cai.domain.Session)
	 */
	@Override
	protected void execute(MyTestRequest message, Request topRequest, C2SSession session) throws Exception {
		int type = message.getType();

		// 普通消息测试
		if (type == 1) {
			MyTestResponse.Builder myTestResponsebuilder = MyTestResponse.newBuilder();
			myTestResponsebuilder.setType(1);
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.MY_TEST);
			responseBuilder.setExtension(Protocol.myTestResponse, myTestResponsebuilder.build());
			session.send(responseBuilder.build());
		}
	}

}
