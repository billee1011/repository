/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.handler;

import com.cai.domain.Session;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.HeartRequest;
import protobuf.clazz.Protocol.HeartResponse;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.clazz.Protocol.Response.ResponseType;

/**
 *
 * @author wu_hc
 */
@ICmd(code = RequestType.HEAR_VALUE, exName = "heartRequest")
public class HeatHandler extends IClientHandler<HeartRequest> {

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.xianyi.framework.cmd.IClientHandler#execute(com.google.protobuf.
	 * GeneratedMessage, com.cai.domain.Session)
	 */
	@Override
	protected void execute(HeartRequest request, Request topRequest, Session session) throws Exception {
		if (request.hasTime() && request.hasSeqNum()) {
			// 返回

			HeartResponse.Builder heartResponseBuilder = HeartResponse.newBuilder();
			heartResponseBuilder.setSeqNum(request.getSeqNum());
			heartResponseBuilder.setSysTime(request.getTime());

			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.HEAR);
			responseBuilder.setExtension(Protocol.heartResponse, heartResponseBuilder.build());
			session.send(responseBuilder.build());
		}
	}

}
