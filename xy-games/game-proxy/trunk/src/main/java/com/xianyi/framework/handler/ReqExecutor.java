/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.xianyi.framework.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.util.SystemClock;
import com.cai.common.util.ThreadUtil;
import com.cai.service.HandlerServiceImp;
import com.cai.service.MongoDBServiceImpl;
import com.google.protobuf.GeneratedMessage;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;

/**
 * 请求执行器
 * 
 * @author wu_hc
 */
public final class ReqExecutor implements Runnable {

	/**
	 * 日志
	 */
	private static final Logger log = LoggerFactory.getLogger(ReqExecutor.class);

	/**
	 * 处理器
	 */
	private static final HandlerServiceImp cmdManager = HandlerServiceImp.getInstance();

	/**
	 * 请求ID为0时将数据报转发到Logic 服务器
	 */
	private static final int LOGIC_REQ_ID = 0;

	/**
	 * 请求包[顶层]
	 */
	private final Request request;

	/**
	 * 会话
	 */
	private final C2SSession session;

	/**
	 * 
	 * @param request
	 * @param session
	 */
	public ReqExecutor(Request request, C2SSession session) {
		this.request = request;
		this.session = session;
	}

	@Override
	public void run() {
		try {
			int requestID = request.getRequestType().getNumber();
			IClientHandler<? extends GeneratedMessage> handler = cmdManager.getHandler(requestID);

			// GAME-TODO??? 需要转到逻辑服????
			if (null == handler) {
				log.error("##########玩家:{} 请求[{}]号协议，但没有找到相应的处理器!#########", session.getAccount(), requestID);
				return;
			}
			long current = SystemClock.CLOCK.now();
			if (requestID == LOGIC_REQ_ID || null == handler.getFieldDescriptor()) {
				handler.doExecute(null, request, session);
			} else {
				Object message = request.getField(handler.getFieldDescriptor());
				handler.doExecute(message, request, session);
			}
			long now = SystemClock.CLOCK.now();
			long cost = now - current;
			if (cost > 100) {
				log.warn("########## 耗时{}操作,玩家:{},请求类型:{}", cost, session.getAccount(), request.getRequestType());
				if (cost > 500L || request.getRequestType() == RequestType.ROOM) {//房间内的协议转发要求高点
					MongoDBServiceImpl.getInstance().server_error_log(0, ELogType.proxySlow, Thread.currentThread().getName(), null,
							String.format("cmd:%d handler:%s cost:%dms msgdetail:%s", requestID, handler.getClass(), cost,this.toString() ));
				}
			}
		} catch (Exception e) {
			log.error("##########处理消息错误,玩家:{},请求类型:{},e:{}", session.getAccount(), request.getRequestType(), e);

			MongoDBServiceImpl.getInstance().server_error_log(0, ELogType.proxyError, ThreadUtil.getStack(e),
					session.getAccount() == null ? 0 : session.getAccount().getAccount_id(), request.getRequestType() + "");
		}
	}

	@Override
	public String toString() {
		return "ReqExecutor [request=" + request + ", session=" + session + "]";
	}

}
