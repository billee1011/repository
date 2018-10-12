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
		
		if(request.hasTime() && request.hasSeqNum()){
			//返回
			
			HeartResponse.Builder heartResponseBuilder = HeartResponse.newBuilder();
			heartResponseBuilder.setSeqNum(request.getSeqNum());
			heartResponseBuilder.setSysTime(request.getTime());
			
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.HEAR);
			responseBuilder.setExtension(Protocol.heartResponse, heartResponseBuilder.build());
			send(responseBuilder.build());
		}
		
		
		
	}

}
