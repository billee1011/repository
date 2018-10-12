/**
请求 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.S2SCmd;
import com.cai.common.util.GlobalExecutor;
import com.cai.util.ClubMsgSender;
import com.google.protobuf.InvalidProtocolBufferException;
import com.xianyi.framework.core.concurrent.WorkerLoop;
import com.xianyi.framework.core.transport.SocketBehaviour;
import com.xianyi.framework.core.transport.event.IOEvent;
import com.xianyi.framework.core.transport.event.IOEventListener;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.S2SCommonProto;
import protobuf.clazz.s2s.ClubServerProto.ClubCreateRoomNewProto;

/**
 * 连接club服务器的socket事件监听器
 * 
 *
 * @author wu date: 2017年8月30日 下午5:42:39 <br/>
 */
public class ClubConnectorListener implements IOEventListener<S2SSession>, SocketBehaviour<S2SSession> {

	private static final Logger logger = LoggerFactory.getLogger(ClubConnectorListener.class);

	@Override
	public void onEvent(IOEvent<S2SSession> ioEvent) {
		switch (ioEvent.event()) {
		case READ:
			read(ioEvent.session(), ioEvent.attachment());
			break;
		case REGISTERED:
			registered(ioEvent.session());
			break;
		case UNREGISTERED:
			unregistered(ioEvent.session());
			break;
		case EXCEPTION:
			exceptionCaught(ioEvent.session(), (Throwable) ioEvent.attachment());
		default:
			break;
		}
	}

	@Override
	public void read(S2SSession session, Object msg) {
		if (!(msg instanceof Request)) {
			logger.warn("=====club=======，消息包不是Request类型 ");
			return;
		}

		Request request = (Request) msg;

		if (request.getRequestType() == RequestType.S2S) {
			processClubHandler(session, request);
		} else {
			logger.warn("=====club=======，消息包不是正确的类型{} ", request.getRequestType());
		}
	}

	@Override
	public void registered(S2SSession session) {
		logger.info("新加入链接:" + session.channel());
	}

	@Override
	public void unregistered(S2SSession session) {
		logger.info("断开链接:" + session.channel());
		session.shutdownGracefully();
	}

	@Override
	public void exceptionCaught(S2SSession session, Throwable cause) {
		logger.info("error", cause);
	}

	/**
	 * 代理服处理
	 * 
	 * @param ctx
	 * @param msg
	 */
	public static void processClubHandler(S2SSession s2s, Request request) {
		Response response = request.getExtension(Protocol.response);
		S2SCommonProto commProto = response.getExtension(Protocol.s2SResponse);
		if (commProto.getCmd() == S2SCmd.CREATE_CLUB_ROOM_RSP || commProto.getCmd() == S2SCmd.CREATE_ENTER_ROOM_RSP) {
			try {
				ClubCreateRoomNewProto proto = ClubCreateRoomNewProto.parseFrom(commProto.getByte());
				WorkerLoop worker = ClubMsgSender.worker(proto.getLogicRoomRequest().getRoomRequest().getClubId());
				worker.runInLoop(new RspExecutor(commProto, s2s));

			} catch (InvalidProtocolBufferException e) {
				GlobalExecutor.execute(new RspExecutor(commProto, s2s));
				e.printStackTrace();
			}
		} else {
			GlobalExecutor.execute(new RspExecutor(commProto, s2s));
		}

	}
}
