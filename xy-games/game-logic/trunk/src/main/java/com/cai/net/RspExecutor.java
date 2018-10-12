/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.handler.IServerHandler;
import com.cai.common.util.SystemClock;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.S2SHandlerServiceImp;
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
	private static final Logger logger = LoggerFactory.getLogger(RspExecutor.class);

	/**
	 * 处理器
	 */
	private static final S2SHandlerServiceImp cmdManager = S2SHandlerServiceImp.getInstance();

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

		try {
			IServerHandler<? extends GeneratedMessage> handler = cmdManager.getHandler(response.getCmd());

			if (null == handler) {
				logger.error("##########连接:{} 请求[{}]号协议，但没有找到相应的处理器!#########", session, response.getCmd());
				return;
			}

			long now = SystemClock.CLOCK.now();

			handler.doExecute(response, session);

			long cost = SystemClock.CLOCK.now() - now;

			if (cost > 100L) {
				logger.warn("耗时任务 cmd[ {} ],handler[ {} ] ,耗时[ {}ms ] ,执行线程[ {} ]", response.getCmd(), handler.getClass(), cost,
						Thread.currentThread().getName());

				if (cost > 1000L) {
					MongoDBServiceImpl.getInstance().server_error_log(0, ELogType.logicSlow, Thread.currentThread().getName(), null,
							String.format("cmd:%d handler:%s cost:%dms proto:{}", response.getCmd(), handler.getClass(), cost, response), 0);
				}
			}
		} catch (Exception e) {
			logger.error("##########处理消息错误,请求类型:{}", response);
			e.printStackTrace();
		}
	}

}
