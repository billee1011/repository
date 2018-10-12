package com.cai.timer;

import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.domain.JvmMemoryModel;
import com.cai.common.util.GlobalExecutor;
import com.cai.core.Global;
import com.cai.core.RequestHandlerThreadPool;
import com.cai.core.SystemConfig;
import com.cai.net.core.ProxyAcceptorListener;
import com.cai.service.C2SSessionService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.TempSmsService;
import com.xianyi.framework.core.concurrent.WorkerLoopGroup;

/**
 * 数据统计
 * 
 * @author run
 *
 */
public class DataStatTimer extends TimerTask {

	private static Logger logger = LoggerFactory.getLogger(DataStatTimer.class);

	@Override
	public void run() {
		// 在线玩家,socket链接数日志
		try {
			// SessionServiceImpl sessionServiceImpl =
			// SessionServiceImpl.getInstance();
			long session_count = C2SSessionService.getInstance().getAllSessionCount();
			long online_count = C2SSessionService.getInstance().getOnlineCount();// sessionServiceImpl.getOnlineSessionMap().size();
			MongoDBServiceImpl.getInstance().systemLog(ELogType.socketConnect, null, session_count, null, ESysLogLevelType.NONE);
			MongoDBServiceImpl.getInstance().systemLog(ELogType.onlinePlayer, null, online_count, null, ESysLogLevelType.NONE);

			// 消息队等待情况
			ThreadPoolExecutor tpe = RequestHandlerThreadPool.getInstance().getTpe();
			long activeCount = tpe.getActiveCount();
			long queueSize = tpe.getQueue().size();
			StringBuilder buf = new StringBuilder();
			buf.append("计划执行任务:" + tpe.getTaskCount()).append(",已执行任务:" + tpe.getCompletedTaskCount()).append(",活动线程数:" + activeCount)
					.append(",总线程数:").append(tpe.getPoolSize()).append(",队列长度:" + queueSize);
			MongoDBServiceImpl.getInstance().systemLog(ELogType.requestPool_new, buf.toString(), activeCount, queueSize, ESysLogLevelType.NONE);

			// jvm
			JvmMemoryModel jvmMemoryModel = new JvmMemoryModel();
			MongoDBServiceImpl.getInstance().systemLog(ELogType.jvmMemory, jvmMemoryModel.info(), jvmMemoryModel.getUse(), null,
					ESysLogLevelType.NONE);

			countThreadPool(Global.getWxPayService(), ELogType.requestPayPool_new);

			countThreadPool(Global.getWxLoginService(), ELogType.requestLoginPool_new);

			countThreadPool(Global.getLogicService(), ELogType.requestLogicPool_new);

			countThreadPool(Global.getUseSwitchService(), ELogType.requestSwitchPool_new);

			countThreadPool(Global.getPtLoginService(), ELogType.requestPtLoginPool_new);

			countThreadPool(Global.getAppStoreService(), ELogType.requestAppStorePool_new);

			countThreadPool(Global.getRoomExtraService(), ELogType.requestRoomExtra_new);

			countThreadPool(Global.getGameDispatchService(), ELogType.requestgameDispatchushPool_new);

			countThreadPool(Global.getWeiXinFlushService(), ELogType.requestWeiXinFlushPool_new);

			countWorkerPool(ProxyAcceptorListener.workers, ELogType.requestRoomSessionWorker);
			
			
			
			countThreadPool(GlobalExecutor.getScheduledThreadPoolExecutor(), ELogType.GlobalExecutor);
			countThreadPool(GlobalExecutor.getScheduledThreadPoolAsyn(), ELogType.GlobalExecutorASYN);
			countThreadPool(GlobalExecutor.getScheduledThreadPoolDB(), ELogType.GlobalExecutorDBASYN);
		} catch (Exception e) {
			logger.error("error", e);
		}

	}

	/**
	 * 統計綫程池信息
	 * 
	 * @param scheduledThreadPoolExecutor
	 * @param logType
	 */
	public static void countThreadPool(ThreadPoolExecutor scheduledThreadPoolExecutor, ELogType logType) {
		BlockingQueue<Runnable> wapayQueue = scheduledThreadPoolExecutor.getQueue();
		long activeCount = scheduledThreadPoolExecutor.getActiveCount();
		long complementtaskCount = scheduledThreadPoolExecutor.getCompletedTaskCount();
		long taskCount = scheduledThreadPoolExecutor.getTaskCount();
		long queueSize = wapayQueue.size();
		int poolSize = scheduledThreadPoolExecutor.getPoolSize();
		StringBuilder buf = new StringBuilder();
		buf.append("计划执行任务:" + taskCount).append(",已执行任务:" + complementtaskCount).append(",活动线程数:" + activeCount).append(",总线程数:").append(poolSize)
				.append(",队列长度:" + queueSize).append("转发服:"+SystemConfig.proxy_index);
		MongoDBServiceImpl.getInstance().systemLog_queue(logType, buf.toString(), activeCount, queueSize, ESysLogLevelType.NONE);

		if (queueSize > 1000) {
			Set<String> phoneSet = new HashSet<String>();
			phoneSet.add("13670139534");

			String content = SystemConfig.localip + "proxy:" + SystemConfig.proxy_index + logType.getDesc() + "队列大小" + queueSize + buf.toString();
			TempSmsService.batchSendMsg(phoneSet, content);

			logger.error("Slowly process " + content);

		}

	}

	private static void countWorkerPool(WorkerLoopGroup group, ELogType logType) {
		long complementtaskCount = group.getCompletedTaskCount();
		long taskCount = group.getTaskCount();
		StringBuilder buf = new StringBuilder();
		buf.append("计划执行任务:" + taskCount).append(",已执行任务:" + complementtaskCount).append(",总线程数:").append(group.workerSize());
		MongoDBServiceImpl.getInstance().systemLog_queue(logType, buf.toString(), taskCount - complementtaskCount, 0L, ESysLogLevelType.NONE);
	}
}
