/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.xianyi.framework.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.util.SystemClock;
import com.cai.domain.Session;
import com.cai.service.C2SHandlerServiceImpl;
import com.cai.service.MongoDBServiceImpl;
import com.google.protobuf.GeneratedMessage;

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
	private static final C2SHandlerServiceImpl cmdManager = C2SHandlerServiceImpl.getInstance();

	/**
	 * 
	 */
	private final S2SCommonProto commProto;

	/**
	 * 会话
	 */
	private final Session session;

	/**
	 * 
	 * @param commProto
	 * @param request
	 * @param session
	 */
	public ReqExecutor(S2SCommonProto commProto, Session session) {
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
				logger.warn("耗时任务 cmd[ {} ],handler[ {} ] ,耗时[ {}ms ]", cmd, handler.getClass(), cost);

				if (cost > 1000L) {
					MongoDBServiceImpl.getInstance().server_error_log(0, ELogType.roomLogicSlow, Thread.currentThread().getName(), null,
							String.format("cmd:%d handler:%s cost:%dms msgdetail:%s", cmd, handler.getClass(), cost,this.toString()), 0);
				}
			}
			
		} catch (Exception e) {
			logger.error("##########!处理消息错误,请求类型:{},e:{}" , commProto.getCmd(), e);
			e.printStackTrace();
		}
	}
	
	@Override
	public String toString() {
		return "ReqExecutor [request=" + commProto + ", session=" + session + "]";
	}
}
