/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.xianyi.framework.handler;

import java.util.concurrent.locks.Lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.service.S2SHandlerService;
import com.google.protobuf.GeneratedMessage;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.Protocol.S2SCommonProto;

/**
 * 回复执行器
 * 
 * @author wu_hc
 */
public final class RspExecutor implements Runnable {

	/**
	 * 日志
	 */
	private static final Logger log = LoggerFactory.getLogger(RspExecutor.class);

	/**
	 * 处理器
	 */
	private static final S2SHandlerService cmdManager = S2SHandlerService.getInstance();

	/**
	 * 回应包
	 */
	private final S2SCommonProto response;

	/**
	 * 会话
	 */
	private final S2SSession session;

	/**
	 * 
	 * @param request
	 * @param session
	 */
	public RspExecutor(S2SCommonProto response, S2SSession session) {
		this.response = response;
		this.session = session;
	}

	@Override
	public void run() {
		final Lock lock = session.getMainLock();
		lock.lock();
		try {
			IServerHandler<? extends GeneratedMessage> handler = cmdManager.getHandler(response.getCmd());

			if (null == handler) {
				log.error("##########连接:{} 请求[{}]号协议，但没有找到相应的处理器!#########", session, response.getCmd());
				return;
			}
			handler.doExecute(response, session);
		} catch (Exception e) {
			log.error("##########处理消息错误,请求类型:{}", response);
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
	}

}
