/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.game.common.util;

import com.google.protobuf.GeneratedMessage;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.CommonProto;
import protobuf.clazz.Protocol.Request;

/**
 * 
 *
 * @author wu_hc date: 2017年10月24日 上午11:29:57 <br/>
 */
public final class PressUtils {
	/**
	 * 组装在服务器处理的消息包
	 * 
	 * @param code
	 * @param builder
	 * @return
	 */
	public static Request.Builder toC2SRequet(int code, GeneratedMessage builder) {
		CommonProto.Builder commBuilder = CommonProto.newBuilder();
		commBuilder.setCmd(code);
		commBuilder.setByte(builder.toByteString());

		Request.Builder requestBuilder = Request.newBuilder();
		requestBuilder.setRequestType(Request.RequestType.C2S);
		requestBuilder.setExtension(Protocol.c2SRequest, commBuilder.build());
		return requestBuilder;
	}

}
