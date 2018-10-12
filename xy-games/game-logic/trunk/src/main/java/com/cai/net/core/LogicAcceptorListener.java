/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.net.core;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.core.SystemConfig;
import com.cai.service.SessionServiceImpl;
import com.xianyi.framework.core.transport.event.IOEvent;
import com.xianyi.framework.core.transport.event.IOEventListener;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import protobuf.clazz.Protocol.Request;

/**
 * 
 * 
 *
 * @author wu date: 2017年8月29日 下午4:06:26 <br/>
 */
public class LogicAcceptorListener implements IOEventListener<C2SSession> {

	private static final Logger logger = LoggerFactory.getLogger(LogicAcceptorListener.class);

	private static final AtomicLong SESSION_ID_GENERATOR = new AtomicLong(1);

	public LogicAcceptorListener() {
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
			logger.warn("=====Logic=======，消息包不是Request类型 ");
			return;
		}

	}

	/**
	 * 连接注册成功
	 * 
	 * @param session
	 */
	private void registered(C2SSession session) {
		if (SystemConfig.gameDebug == 1) {
			logger.info("[[Client->Logic]]新加入链接:{}", session.getChannel());
		}
		session.setSessionId(SESSION_ID_GENERATOR.getAndIncrement());
		session.setClientIP(((InetSocketAddress) session.channel().remoteAddress()).getAddress().getHostAddress());
		SessionServiceImpl.getInstance().sessionCreate(session);
	}

	/**
	 * 连接取消注册
	 * 
	 * @param session
	 */
	private void unregistered(C2SSession session) {
		if (SystemConfig.gameDebug == 1) {
			logger.info("[[Client->Logic]]断开链接:" + session.channel());
		}
		SessionServiceImpl.getInstance().sessionFree(session);
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
