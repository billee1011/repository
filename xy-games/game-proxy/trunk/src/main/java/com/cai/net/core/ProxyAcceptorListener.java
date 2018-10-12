/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.net.core;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.AttributeKeyConstans;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.GlobalExecutor;
import com.cai.common.util.IpUtil;
import com.cai.common.util.RuntimeOpt;
import com.cai.common.util.SessionUtil;
import com.cai.core.RequestHandlerThreadPool;
import com.cai.core.SystemConfig;
import com.cai.dictionary.SysParamServerDict;
import com.cai.domain.IpFirewallModel;
import com.cai.service.C2SSessionService;
import com.cai.service.FirewallServiceImpl;
import com.google.common.base.Strings;
import com.xianyi.framework.core.concurrent.DefaultWorkerLoopGroup;
import com.xianyi.framework.core.transport.event.IOEvent;
import com.xianyi.framework.core.transport.event.IOEventListener;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.ReqExecutor;

import javolution.util.FastMap;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;

/**
 *
 * @author wu_hc
 */
public class ProxyAcceptorListener implements IOEventListener<C2SSession> {

	private static final Logger logger = LoggerFactory.getLogger(ProxyAcceptorListener.class);

	/**
	 * 登陆线程,只负责登陆，登出
	 */
	public static final DefaultWorkerLoopGroup workers = DefaultWorkerLoopGroup.newGroup("session-woker-thread",
			tableSizeFor(RuntimeOpt.availableProcessors() << 1 /* << 2 */));

	static final int tableSizeFor(int cap) {
		int n = cap - 1;
		n |= n >>> 1;
		n |= n >>> 2;
		n |= n >>> 4;
		n |= n >>> 8;
		n |= n >>> 16;
		return n;
	}

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
			if (!session.access()) {
				logger.warn("请求频率异常:{}", request);
			}

			processRequest(request, session);
			processDebugMessage(request, 1);
		} else {
			logger.error("client->proxy，消息包不是Request类型 ");
		}

	}

	/**
	 * 连接注册成功
	 * 
	 * @param session
	 */
	private void registered(C2SSession session) {
		if (SystemConfig.gameDebug == 1) {
			logger.info("[[Client->Proxy]]新加入链接:{}", session.getChannel());
		}
		if (SystemConfig.gameDebug == 0 && !ipFirewall(session)) {
			session.shutdownGracefully();
			return;
		}

		SessionUtil.setAttr(session, AttributeKeyConstans.LOGIN_SESSION_WORKER, workers.next());

		session.setSessionId(SESSION_ID_GENERATOR.getAndIncrement());
		session.setClientIP(((InetSocketAddress) session.channel().remoteAddress()).getAddress().getHostAddress());

		C2SSessionService.getInstance().sessionCreate(session);
	}

	/**
	 * 连接取消注册
	 * 
	 * @param session
	 */
	private void unregistered(C2SSession session) {
		if (SystemConfig.gameDebug == 1) {
			logger.info("[[Client->Proxy]]断开链接:" + session.channel());
		}
		if (null == session)
			return;

		Executor worker = SessionUtil.getAttrOrDefault(session, AttributeKeyConstans.LOGIN_SESSION_WORKER, GlobalExecutor.actuator());
		worker.execute(() -> {
			C2SSessionService.getInstance().sessionFree(session);
		});
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
		if (request.getRequestType() == RequestType.ROOM) {
			Executor worker = SessionUtil.getAttrOrDefault(session, AttributeKeyConstans.LOGIN_SESSION_WORKER, GlobalExecutor.actuator());
			worker.execute(new ReqExecutor(request, session));
		} else if (null != session.getAccount() && null != session.getAccount().getWorkerLoop()) {
			session.getAccount().getWorkerLoop().runInLoop(new ReqExecutor(request, session));
		} else {
			RequestHandlerThreadPool.getInstance().addTask(() -> {
				final Lock lock = session.getMainLock();
				try {
					lock.lock();
					new ReqExecutor(request, session).run();
				} finally {
					lock.unlock();
				}
			});
		}
	}

	

	/**
	 * 
	 * @param
	 * @return
	 */
	private static boolean ipFirewall(C2SSession c2s) {
		// 防火墙
		String ip = ((InetSocketAddress) c2s.channel().remoteAddress()).getAddress().getHostAddress();

		if (!IpUtil.isWhiteIp(ip) && !SysParamServerDict.getInstance().isAutoWhiteIP(ip)) {//非常重要
			IpFirewallModel ipFirewallModel = FirewallServiceImpl.getInstance().addNewLink(ip);
			if (ipFirewallModel != null) {
				long now = System.currentTimeMillis();
				if (!ipFirewallModel.verifyIP(now)) {
					if (FirewallServiceImpl.getInstance().isDebugInfo) {
						logger.error("拒绝链接:" + c2s.channel() + " sessionid:{}", c2s.getSessionId());
					}
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
				logger.info("[[Client->Proxy]] msgsize:{}b,msg:{}", request.toByteArray().length, request);
			}

		}
	}
}
