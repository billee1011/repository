package com.cai.handler;

import com.cai.net.core.ClientHandler;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.MyTestRequest;
import protobuf.clazz.Protocol.MyTestResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;

/**
 * 我的压力测试
 * @author run
 *
 */
public class TestMyTestHandler extends ClientHandler<MyTestRequest>{

	@Override
	public void onRequest() throws Exception {
		
		int type = request.getType();
		
		//普通消息测试
		if(type==1){
			MyTestResponse.Builder myTestResponsebuilder = MyTestResponse.newBuilder();
			myTestResponsebuilder.setType(1);
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.MY_TEST);
			responseBuilder.setExtension(Protocol.myTestResponse, myTestResponsebuilder.build());
			send(responseBuilder.build());
		}
		
	}
	

}
