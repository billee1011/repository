/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
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

import protobuf.clazz.Protocol.CommonProto;
import protobuf.clazz.Protocol.Request;

/**
 * 
 *
 * @author wu_hc date: 2017年7月31日 上午10:48:03 <br/>
 */
public class ReqExExecutor implements Runnable {

	/**
	 * 日志
	 */
	private static final Logger logger = LoggerFactory.getLogger(ReqExExecutor.class);

	/**
	 * 处理器
	 */
	private static final HandlerServiceImp cmdManager = HandlerServiceImp.getInstance();

	/**
	 * 
	 */
	private final CommonProto commProto;

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
	 * @param commProto
	 * @param request
	 * @param session
	 */
	public ReqExExecutor(CommonProto commProto, Request request, C2SSession session) {
		this.commProto = commProto;
		this.request = request;
		this.session = session;
	}

	@Override
	public void run() {
		int cmd = commProto.getCmd();

		IClientHandler<? extends GeneratedMessage> handler = cmdManager.getHandler(cmd);
		if (null == handler) {
			logger.error("消息id：{} 的handler未定义，请检查!", cmd);
			return;
		}

		try {
			long current = SystemClock.CLOCK.now();

			handler.doExecute(commProto, request, session);

			long now = SystemClock.CLOCK.now();
			long cost = now - current;
			if (cost > 200) {
				logger.warn("########## 耗时{}操作,玩家:{},请求类型:{}", cost, session.getAccount(), request.getRequestType());
				if (cost > 500L) {
					MongoDBServiceImpl.getInstance().server_error_log(0, ELogType.proxySlow, Thread.currentThread().getName(), null,
							String.format("cmd:%d handler:%s cost:%dms", cmd, handler.getClass(), cost));
				}
			}
		} catch (Exception e) {
			logger.error("##########!处理消息错误,玩家:{},请求类型:{},e:{}", session.getAccount(), commProto.getCmd(), e);

			MongoDBServiceImpl.getInstance().server_error_log(0, ELogType.proxyError, ThreadUtil.getStack(e),
					session.getAccount() == null ? 0 : session.getAccount().getAccount_id(), commProto.getCmd() + "");
		}
	}
}
