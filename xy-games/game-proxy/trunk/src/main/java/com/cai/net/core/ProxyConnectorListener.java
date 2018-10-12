/**
请求 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.net.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.Account;
import com.cai.common.util.GlobalExecutor;
import com.cai.core.SystemConfig;
import com.cai.intercept.s2c.Intercept;
import com.cai.intercept.s2c.RoomRspIntercept;
import com.cai.service.C2SSessionService;
import com.cai.tasks.MsgRspTask;
import com.xianyi.framework.core.concurrent.WorkerLoop;
import com.xianyi.framework.core.transport.SocketBehaviour;
import com.xianyi.framework.core.transport.event.IOEvent;
import com.xianyi.framework.core.transport.event.IOEventListener;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.core.transport.netty.session.S2SSession;
import com.xianyi.framework.handler.RspExecutor;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.S2SCommonProto;

/**
 *
 * @author wu_hc
 */
public class ProxyConnectorListener implements IOEventListener<S2SSession>, SocketBehaviour<S2SSession> {

	private static final Logger log = LoggerFactory.getLogger(ProxyConnectorListener.class);

	/**
	 * 拦截器
	 */
	private static final Intercept intercept = new RoomRspIntercept();

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
			log.warn("=====Logic->Proxy->Client=======，消息包不是Request类型 ");
			return;
		}

		Request request = (Request) msg;

		if (request.getRequestType() == RequestType.S2S) {
			// 1是否需要代理服处理的
			processProxyHandler(session, request);
		} else {
			// 2直接发给客户端
			processClientHandler(session, request);
		}
	}

	@Override
	public void registered(S2SSession session) {
		log.info("新加入链接逻辑服:" + session.channel());
	}

	@Override
	public void unregistered(S2SSession session) {
		log.info("断开链接逻辑服:" + session.channel());
		session.shutdownGracefully();
	}

	@Override
	public void exceptionCaught(S2SSession session, Throwable cause) {
		log.info("error", cause);
		cause.printStackTrace();
	}

	/**
	 * 代理服处理
	 * 
	 * @param ctx
	 * @param msg
	 */
	private static void processProxyHandler(S2SSession s2s, Request request) {
		Response response = request.getExtension(Protocol.response);
		S2SCommonProto commProto = response.getExtension(Protocol.s2SResponse);
		GlobalExecutor.execute(new RspExecutor(commProto, s2s));
	}

	/**
	 * 发给客户端
	 * 
	 * @param ctx
	 * @param msg
	 */
	private static void processClientHandler(S2SSession s2s, Request request) {
		if (!request.hasProxSeesionId()) {
			log.error("=====Logic->Proxy->Client=======，没有指定ProxSeesionId");
			return;
		}

		long proxySessionId = request.getProxSeesionId();
		C2SSession session = C2SSessionService.getInstance().getSession(proxySessionId);
		if (null == session) {
			// //
			// log.error("=====Logic->Proxy->Client=======，没有找到玩家的sessionid[{}]",
			// // proxySessionId);
			// // for (final C2SSession online :
			// // C2SSessionService.getInstance().getAllOnlieSession()) {
			// // log.info("\t[[ 在线玩家:{} ]] ", online.getAccount());
			// // }
			// log.warn("logic->proxy->client session is null " + proxySessionId
			// + "request==" + request.getRequestType());
			return;
		}

		if (proxySessionId != session.getAccountID()) {// 应该是异常情况
			log.warn("session_id is not equals accountID and session_id=", proxySessionId + "account==" + session.getAccountID());
		}

		Response response = request.getExtension(Protocol.response);

		// 拦截器，临时，后面优化GAME-TODO
		if (intercept.intercept(response, session)) {
			return;
		}

		if (SystemConfig.gameDebug == 1) {
			// log.info("=====Logic->Proxy->Client=======\n{}",
			// response.toString());
		}
		Account account = session.getAccount();
		if (null == account) {
			return;
		}
		WorkerLoop worker = account.getWorkerLoop();
		if (null != worker) {
			worker.runInLoop(new MsgRspTask(proxySessionId, response));
		}
	}
}
