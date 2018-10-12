/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.common.util;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.S2SCmd;
import com.cai.common.domain.Player;
import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.S2SCommonProto;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.ddz.DdzRsp.DdzCallReq;
import protobuf.clazz.s2s.S2SProto.PlayerStatus;

/**
 * 
 *
 * @author wu_hc date: 2017年7月17日 下午2:04:07 <br/>
 */
public final class PBUtil {
	/**
	 * 日志
	 */
	private static Logger log = LoggerFactory.getLogger(PBUtil.class);

	/**
	 * 解析器
	 */
	private static final Map<Integer, com.google.protobuf.Parser<? extends com.google.protobuf.GeneratedMessage>> PARSER = Maps.newConcurrentMap();

	private PBUtil() {

	}

	// 新加的type需要在这里注册
	static {
		PARSER.put(S2SCmd.TEST_CMD, PlayerStatus.getDefaultInstance().getParserForType());
		PARSER.put(1001, DdzCallReq.getDefaultInstance().getParserForType());
	}

	/**
	 * ByteString --> byte[]
	 * 
	 * @param byteString
	 * @return
	 */
	public static byte[] toByteArray(ByteString byteString) {
		return byteString.toByteArray();
	}

	/**
	 * byte[] --> ByteString
	 * 
	 * @param bytes
	 * @return
	 */
	public static ByteString toByteString(byte[] bytes) {
		return ByteString.copyFrom(bytes);
	}

	/**
	 * byte[] --> ByteString
	 * 
	 * @param builder
	 * @return
	 */
	public static ByteString toByteString(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
		return ByteString.copyFrom(builder.build().toByteArray());
	}

	/**
	 * 
	 * @param request
	 * @return
	 */
	public static com.google.protobuf.GeneratedMessage toObject(RoomRequest request) {
		return toObject(request, GeneratedMessage.class);
	}

	/**
	 * 
	 * @param request
	 * @param clazz
	 * @return
	 */
	public static <T extends GeneratedMessage> T toObject(RoomRequest request, Class<? extends T> clazz) {
		return toObject(request.getType(), request.getCommRequet(), clazz);
	}

	/**
	 * 
	 * @param request
	 * @param clazz
	 * @return
	 */
	public static <T extends GeneratedMessage> T toObject(S2SCommonProto commProto, Class<? extends T> clazz) {
		return toObject(commProto.getCmd(), commProto.getByte(), clazz);
	}

	/**
	 * 
	 * @param code
	 * @param byteString
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T extends GeneratedMessage> T toObject(int code, ByteString byteString, Class<? extends T> clazz) {
		com.google.protobuf.Parser<? extends com.google.protobuf.GeneratedMessage> parse = PARSER.get(code);
		if (null == parse) {
			log.error("====找不到类型为:{}的解析器，请确认是否已经配置=========", code);
			return null;
		}

		try {
			return (T) parse.parseFrom(byteString);
		} catch (InvalidProtocolBufferException e) {
			log.error("====解析器错误,类型:[{}]，请确认是否配置错误=========", code);
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 组装在proxy处理的消息包
	 * 
	 * @param code
	 * @param builder
	 * @return
	 */
	public static Request.Builder toS2SRequet(int code, GeneratedMessage.Builder<?> builder) {
		S2SCommonProto.Builder commBuilder = S2SCommonProto.newBuilder();
		commBuilder.setCmd(code);
		commBuilder.setByte(builder.build().toByteString());
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.S2S);
		responseBuilder.setExtension(Protocol.s2SResponse, commBuilder.build());

		Request.Builder requestBuilder = Request.newBuilder();
		requestBuilder.setRequestType(Request.RequestType.S2S);
		requestBuilder.setExtension(Protocol.response, responseBuilder.build());
		return requestBuilder;
	}

	/**
	 * 组装直接发给客户端的消息包
	 * 
	 * @param code
	 * @param player
	 * @param builder
	 * @return
	 */
	public static Request.Builder toS2CRequet(Player player, Response.Builder rspBuilder) {

		Request.Builder requestBuilder = Request.newBuilder();
		requestBuilder.setRequestType(Request.RequestType.PROXY);
		requestBuilder.setProxId(player.getProxy_index());
		requestBuilder.setProxSeesionId(player.getProxy_session_id());
		requestBuilder.setExtension(Protocol.response, rspBuilder.build());
		return requestBuilder;
	}
}
