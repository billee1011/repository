package com.cai.core;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Global {

	private static final Logger log = LoggerFactory.getLogger(Global.class);

	public static final String SERVER_LOGIC = "logic_server";
	

	/**
	 * 处理异步任务线程池 (DB操作)
	 */
	public static ScheduledThreadPoolExecutor dbService = new ScheduledThreadPoolExecutor(20,
			new MyThreadFactory(ThreadNameEnum.ASYN_WORK_THREAD.getCode()));


	/**
	 * 业务逻辑处理耗时任务异步单线程 
	 */
	public static ScheduledExecutorService logicService = Executors.newScheduledThreadPool(1,
			new MyThreadFactory(ThreadNameEnum.LOGIC_SERVER.getCode()));

	
	/**
	 * 预留接口可以根据标识指定线程池执行
	 */
	public static ScheduledExecutorService getService(String taskCategary) {
		if (taskCategary.equals(SERVER_LOGIC))
			return logicService;
		return dbService;
	}
	
	
	public static ScheduledExecutorService getDbService() {
		return dbService;
	}
	
	

	/**
	 * 在指定延迟后执行一个任务
	 * 
	 * @param taskCategary
	 *            任务分类标识
	 * @param command
	 *            需要执行的任务
	 * @param delay
	 *            延迟的时间
	 * @param unit
	 *            时间单位
	 * @return 执行结果的回调对象
	 */
	public static ScheduledFuture<?> schedule(String taskCategary, Runnable command, long delay, TimeUnit unit) {
		return getService(taskCategary).schedule(command, delay, unit);
	}

	/**
	 * 以固定的执行间隔执行指定的任务
	 * 
	 * @param taskCategary
	 *            任务分类标识
	 * @param command
	 *            需要执行的任务
	 * @param initialDelay
	 *            延迟的时间
	 * @param period
	 *            执行间隔
	 * @param unit
	 *            时间单位
	 * @return 执行结果的回调对象
	 */
	public static ScheduledFuture<?> scheduleWithFixedDelay(String taskCategary, Runnable command, long initialDelay,
			long period, TimeUnit unit) {
		return getService(taskCategary).scheduleAtFixedRate(command, initialDelay, period, unit);
	}

	/**
	 * 以固定的周期执行指定的任务
	 * 
	 * @param taskCategary
	 *            任务分类标识
	 * @param command
	 *            需要执行的任务
	 * @param initialDelay
	 *            延迟的时间
	 * @param period
	 *            执行周期
	 * @param unit
	 *            时间单位
	 * @return 执行结果的回调对象
	 */
	public static ScheduledFuture<?> scheduleAtFixedRate(String taskCategary, Runnable command, long initialDelay,
			long period, TimeUnit unit) {
		return getService(taskCategary).scheduleAtFixedRate(command, initialDelay, period, unit);
	}

	/**
	 * 关闭线程池
	 */
	public static void shutdownThreadPool() {
		logicService.shutdownNow();
		log.warn("开始停止异步处理线程池，等待剩余任务执行完...");
		dbService.shutdownNow();
		log.warn("DB线程池已执行完毕！");
	}
}
