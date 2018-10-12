/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.net.core;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.util.GlobalExecutor;
import com.cai.config.SystemConfig;
import com.cai.service.SessionService;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.ExtensionRegistry;
import com.xianyi.framework.core.transport.event.IOEvent;
import com.xianyi.framework.core.transport.event.IOEventListener;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.ReqExecutor;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.clazz.Protocol.S2SCommonProto;

/**
 * 
 * 
 *
 * @author wu date: 2017年8月29日 下午4:06:26 <br/>
 */
public class ClubAcceptorListener implements IOEventListener<C2SSession> {

	private static final Logger logger = LoggerFactory.getLogger(ClubAcceptorListener.class);

	private static final AtomicLong SESSION_ID_GENERATOR = new AtomicLong(1);

	/**
	 * 
	 */
	private final FieldDescriptor fieldDescriptor;

	public ClubAcceptorListener() {
		ExtensionRegistry registry = ExtensionRegistry.newInstance();
		Protocol.registerAllExtensions(registry);
		fieldDescriptor = registry.findExtensionByName("s2sRequest").descriptor;
	}

	@Override
	public void onEvent(IOEvent<C2SSession> ioEvent) {
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

	/**
	 * 读数据就绪
	 * 
	 * @param session
	 * @param msg
	 */
	private void read(C2SSession session, Object msg) {
		if (!(msg instanceof Request)) {
			logger.warn("=====club=======，消息包不是Request类型 ");
			return;
		}

		Request request = (Request) msg;
		if (request.getRequestType() != RequestType.S2S) {
			logger.warn("=====club=======，消息包不是Request:S2S类型 ");
			return;
		}
		session.access();

		S2SCommonProto commProto = (S2SCommonProto) request.getField(fieldDescriptor);
		GlobalExecutor.execute(new ReqExecutor(commProto, session));
	}

	/**
	 * 连接注册成功
	 * 
	 * @param session
	 */
	private void registered(C2SSession session) {
		if (SystemConfig.gameDebug == 1) {
			logger.info("[[Client->Club]]新加入链接:{}", session.getChannel());
		}
		session.getHzUtil().setCheckHz(10000);
		session.setSessionId(SESSION_ID_GENERATOR.getAndIncrement());
		session.setClientIP(((InetSocketAddress) session.channel().remoteAddress()).getAddress().getHostAddress());
		SessionService.getInstance().sessionCreate(session);
	}

	/**
	 * 连接取消注册
	 * 
	 * @param session
	 */
	private void unregistered(C2SSession session) {
		if (SystemConfig.gameDebug == 1) {
			logger.info("[[Client->Club]]断开链接:" + session.channel());
		}
		SessionService.getInstance().sessionFree(session);
	}

	/**
	 * 出现异常
	 * 
	 * @param session
	 * @param cause
	 */
	private void exceptionCaught(C2SSession session, Throwable cause) {
		session.shutdownGracefully();
	}
}
