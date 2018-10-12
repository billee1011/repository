package com.cai.util;

import com.cai.common.define.ESysMsgType;
import com.cai.domain.Session;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.MsgAllResponse;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;

public class MessageResponse {
	
	
	/**
	 * 例子
	 * @param requestType
	 * @param session
	 * @return
	 */
	public static Request.Builder getLogicRequest(RequestType requestType,Session session){
		Request.Builder requestBuilder = Request.newBuilder();
		return requestBuilder;
	}
	
	/**
	 * 有错误码的提示信息
	 * @param type
	 * @param error_id
	 * @param msg
	 * @return
	 */
	public static Response.Builder getMsgAllResponse(int error_id,String msg){
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.MSG);
		MsgAllResponse.Builder msgBuilder = MsgAllResponse.newBuilder();
		msgBuilder.setType(ESysMsgType.INCLUDE_ERROR.getId());
		msgBuilder.setMsg(msg);
		msgBuilder.setErrorId(error_id);
		responseBuilder.setExtension(Protocol.msgAllResponse, msgBuilder.build());
		return responseBuilder;
	}
	
	
	
	

}
