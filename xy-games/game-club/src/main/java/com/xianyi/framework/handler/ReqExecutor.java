/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.xianyi.framework.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.util.SystemClock;
import com.cai.common.util.ThreadUtil;
import com.cai.service.C2SHandlerService;
import com.cai.service.MongoDBServiceImpl;
import com.google.protobuf.GeneratedMessage;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import protobuf.clazz.Protocol.S2SCommonProto;

/**
 * 
 * 
 *
 * @author wu date: 2017年8月29日 下午5:19:06 <br/>
 */
public class ReqExecutor implements Runnable {

	/**
	 * 日志
	 */
	private static final Logger logger = LoggerFactory.getLogger(ReqExecutor.class);

	/**
	 * 处理器
	 */
	private static final C2SHandlerService cmdManager = C2SHandlerService.getInstance();

	/**
	 * 
	 */
	private final S2SCommonProto commProto;

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
	public ReqExecutor(S2SCommonProto commProto, C2SSession session) {
		this.commProto = commProto;
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
			long now = SystemClock.CLOCK.now();

			Object message = handler.toObject(commProto);
			handler.doExecute(message, session);

			long cost = SystemClock.CLOCK.now() - now;
			if (cost > 100L) {
				logger.warn("耗时任务 cmd[ {} ],handler[ {} ] ,耗时[ {}ms ] ,执行线程[ {} ]", cmd, handler.getClass(), cost, Thread.currentThread().getName());

				if (cost > 1000L) {
					MongoDBServiceImpl.getInstance().server_error_log(0, ELogType.clubSlow, Thread.currentThread().getName(), null,
							String.format("cmd:%d handler:%s cost:%dms proto:{}", cmd, handler.getClass(), cost, message), 0);
				}
			}
		} catch (Exception e) {
			logger.error("##########!处理消息错误,玩家:{},请求类型:{}", session.getAccount(), commProto.getCmd(), e);
			MongoDBServiceImpl.getInstance().server_error_log(0, ELogType.clubError, ThreadUtil.getStack(e),
					session.getAccount() == null ? 0 : session.getAccount().getAccount_id(), "协议号" + commProto.getCmd(), 0);
		}
	}
}
