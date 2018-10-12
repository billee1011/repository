/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.timer;

import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.util.GlobalExecutor;
import com.cai.service.MongoDBServiceImpl;

/**
 * 
 *
 * @author DIY 
 * date: 2018年6月25日 下午2:27:39 <br/>
 */
public class DataStatTimer extends TimerTask{

	private static Logger logger = LoggerFactory.getLogger(DataStatTimer.class);
	
	@Override
	public void run() {
		countThreadPool(GlobalExecutor.getScheduledThreadPoolExecutor(), ELogType.GlobalExecutor);
		countThreadPool(GlobalExecutor.getScheduledThreadPoolAsyn(), ELogType.GlobalExecutorASYN);
		countThreadPool(GlobalExecutor.getScheduledThreadPoolDB(), ELogType.GlobalExecutorDBASYN);
	}

	
	public static void countThreadPool(ThreadPoolExecutor scheduledThreadPoolExecutor, ELogType logType) {
		BlockingQueue<Runnable> wapayQueue = scheduledThreadPoolExecutor.getQueue();
		long activeCount = scheduledThreadPoolExecutor.getActiveCount();
		long complementtaskCount = scheduledThreadPoolExecutor.getCompletedTaskCount();
		long taskCount = scheduledThreadPoolExecutor.getTaskCount();
		long queueSize = wapayQueue.size();
		int poolSize = scheduledThreadPoolExecutor.getPoolSize();
		StringBuilder buf = new StringBuilder();
		buf.append("计划执行任务:" + taskCount).append(",已执行任务:" + complementtaskCount).append(",活动线程数:" + activeCount).append(",总线程数:").append(poolSize)
				.append(",队列长度:" + queueSize).append("俱乐部");
		MongoDBServiceImpl.getInstance().systemLog_queue(logType, buf.toString(), activeCount, queueSize, ESysLogLevelType.NONE);

	}
}
