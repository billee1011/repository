/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.net.core;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.util.IpUtil;
import com.cai.core.RequestHandlerThreadPool;
import com.cai.core.SystemConfig;
import com.cai.domain.IpFirewallModel;
import com.cai.net.server.GameSocketServer;
import com.cai.service.C2SSessionService;
import com.cai.service.FirewallServiceImpl;
import com.xianyi.framework.core.transport.event.IOEvent;
import com.xianyi.framework.core.transport.event.IOEventListener;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;

/**
 *
 * @author wu_hc
 */
public class ProxyAcceptorListener implements IOEventListener<C2SSession> {

	private static final Logger log = LoggerFactory.getLogger(ProxyAcceptorListener.class);

	/**
	 * session id 生成器
	 */
	private static final AtomicLong SESSION_ID_GENERATOR = new AtomicLong(1);

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
		if (msg instanceof Request) {
			Request request = (Request) msg;
			session.access();

			// 心跳不打印
			if (SystemConfig.gameDebug == 1) {
				if (request.getRequestType().getNumber() != RequestType.HEAR.getNumber()) {
					log.info("[[Client->Proxy]] msgsize:{}b,msg:{}", request.toByteArray().length, msg);
				}
			}

			processRequest(request, session);
			processDebugMessage(request, 1);
			processIOStatistics(request);
		} else {
			log.error("client->proxy，消息包不是Request类型 ");
		}

	}

	/**
	 * 连接注册成功
	 * 
	 * @param session
	 */
	private void registered(C2SSession session) {
		if (SystemConfig.gameDebug == 1) {
			log.info("[[Client->Proxy]]新加入链接:{}", session.getChannel());
		}
		if (!ipFirewall(session)) {
			// log.info("拒绝链接:" + ctx.channel());
			session.shutdownGracefully();
			return;
		}

		session.setSessionId(SESSION_ID_GENERATOR.getAndIncrement());
		session.setClientIP(((InetSocketAddress) session.channel().remoteAddress()).getAddress().getHostAddress());
	}

	/**
	 * 连接取消注册
	 * 
	 * @param session
	 */
	private void unregistered(C2SSession session) {
		if (SystemConfig.gameDebug == 1) {
			log.info("[[Client->Proxy]]断开链接:" + session.channel());
		}
		if (null == session)
			return;
		C2SSessionService.getInstance().offline(session);
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

	/**
	 * 
	 * @param request
	 * @param session
	 */
	private static void processRequest(Request request, C2SSession session) {
		if (null != session.getAccount() && null != session.getAccount().getWorkerLoop()) {
//			session.getAccount().getWorkerLoop().runInLoop(new ReqExecutor(request, session));
		} else {

			RequestHandlerThreadPool.getInstance().addTask(new Runnable() {
				@Override
				public void run() {
					final Lock lock = session.getMainLock();
					try {
						lock.lock();
//						new ReqExecutor(request, session).run();
					} finally {
						lock.unlock();
					}
				}
			});
		}
	}

	/**
	 * 流量统计
	 * 
	 * @param request
	 */
	private static void processIOStatistics(final Request request) {
		GameSocketServer.statistics.getInMessages().incrementAndGet();
		GameSocketServer.statistics.getInBytes().addAndGet(request.toByteArray().length);
	}

	/**
	 * 
	 * @param ctx
	 * @return
	 */
	private static boolean ipFirewall(C2SSession c2s) {
		// 防火墙
		String ip = ((InetSocketAddress) c2s.channel().remoteAddress()).getAddress().getHostAddress();
		if (!IpUtil.isWhiteIp(ip)) {
			IpFirewallModel ipFirewallModel = FirewallServiceImpl.getInstance().addNewLink(ip);
			if (ipFirewallModel != null) {
				if (!ipFirewallModel.verifyIP()) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 日至
	 * 
	 * @param request
	 */
	private static void processDebugMessage(final Request request, int type) {
		// 心跳不打印
		if (SystemConfig.gameDebug == 1) {
			if (request.getRequestType().getNumber() != RequestType.HEAR.getNumber()) {
				log.info("[[Client->Proxy]] msgsize:{}b,msg:{}", request.toByteArray().length, request);
			}

		}
	}
}
