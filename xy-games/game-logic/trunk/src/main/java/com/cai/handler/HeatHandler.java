package com.cai.handler;

import com.cai.net.core.ClientHandler;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.HeartRequest;
import protobuf.clazz.Protocol.HeartResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;

/**
 * 心跳包处理
 * @author run
 *
 */
public class HeatHandler extends ClientHandler<HeartRequest>{

	@Override
	public void onRequest() throws Exception {
//		
//		if(!request.hasTime())
//			return;
//		
//		System.out.println("==进入心跳handler=="+request.getTime());
//		int k = request.getTime();
//		HeartResponse.Builder hearResponse = HeartResponse.newBuilder();
//		hearResponse.setSysTime((int)(System.currentTimeMillis()/1000L));
//		Response.Builder responseBuilder = Response.newBuilder();
//		responseBuilder.setResponseType(ResponseType.HEAR);
//		responseBuilder.setExtension(Protocol.heartResponse, hearResponse.build());
//		send(responseBuilder.build());
	}

}
