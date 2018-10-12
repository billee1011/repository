/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.xianyi.framework.handler;

import java.util.concurrent.locks.Lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.domain.Session;
import com.cai.service.HandlerServiceImp;
import com.google.protobuf.GeneratedMessage;

import protobuf.clazz.Protocol.Request;

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
	private final Session session;

	/**
	 * 
	 * @param request
	 * @param session
	 */
	public ReqExecutor(Request request, Session session) {
		this.request = request;
		this.session = session;
	}

	@Override
	public void run() {
		// 枷锁，临时解决方案,后面线程安全问题由外部保证
		final Lock lock = session.getMainLock();
		lock.lock();
		try {
			int requestID = request.getRequestType().getNumber();
			// log.info("client ----> proxy.\t[requestid:{}]", requestID);

			IClientHandler<? extends GeneratedMessage> handler = cmdManager.getHandler(requestID);

			// GAME-TODO??? 需要转到逻辑服????
			if (null == handler) {
				log.error("##########玩家:{} 请求[{}]号协议，但没有找到相应的处理器!#########", session.getAccount(), requestID);
				return;
			}
			if (requestID == LOGIC_REQ_ID || null == handler.getFieldDescriptor()) {
				handler.doExecute(null, request, session);
			} else {
				Object message = request.getField(handler.getFieldDescriptor());
				handler.doExecute(message, request, session);
			}
		} catch (Exception e) {
			log.error("##########处理消息错误,玩家:{},请求类型:{}", session.getAccount(), request.getRequestType());
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
	}

}
