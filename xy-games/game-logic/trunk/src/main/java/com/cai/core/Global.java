package com.cai.core;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.util.RuntimeOpt;

public class Global {

	private static final Logger log = LoggerFactory.getLogger(Global.class);

	public static final String SERVER_LOGIC = "logic_server";

	/**
	 * 业务逻辑处理耗时任务异步单线程
	 */
	private static ThreadPoolExecutor logicService = new ThreadPoolExecutor(2, 2, 300L, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(), new MyThreadFactory(ThreadNameEnum.LOGIC_SERVER.getCode()));


	/**
	 * 房间消息转发线程
	 */
	private static final ThreadPoolExecutor LOGIC_ROOM_EXECUTOR =  new ThreadPoolExecutor(RuntimeOpt.availableProcessors(), RuntimeOpt.availableProcessors(), 300L, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(), new MyThreadFactory(ThreadNameEnum.ASYN_POSTITION_THREAD.getCode()));
	
	
	/**
	 * 房间逻辑业务执行备用线程
	 */
	private static final ThreadPoolExecutor LOGIC_ROOM_EXECUTOR_BACK=  new ThreadPoolExecutor(RuntimeOpt.availableProcessors(), RuntimeOpt.availableProcessors(), 300L, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(), new MyThreadFactory(ThreadNameEnum.ASYN_POSTITION_THREAD_BACK.getCode()));

	public static ThreadPoolExecutor getLogicThreadPool() {
		return logicService;
	}

	/**
	 * 房间逻辑消息转发线程池----完全独立只能用于房间逻辑---其它地方别调用
	 */
	public static ThreadPoolExecutor getRoomPool() {
		return LOGIC_ROOM_EXECUTOR;
	}
	
	
	/**
	 * 房间逻辑执行线程池备用----完全独立只能用于房间逻辑---其它地方别调用
	 */
	public static ThreadPoolExecutor getRoomPoolBACK() {
		return LOGIC_ROOM_EXECUTOR_BACK;
	}
	


	/**
	 * 此线程池  只能处理房间内的牌桌逻辑---其它地方别调用
	 * @param task
	 */
	public static void executeLogicRoomThread(final Runnable task) {
		LOGIC_ROOM_EXECUTOR.execute(() -> {
			try {
				task.run();
			} catch (Exception e) {
				e.printStackTrace();
				log.error("ANALYSE_EXECUTOR execption task:{}", task);
			}
		});
	}


	/**
	 * 关闭线程池
	 */
	public static void shutdownThreadPool() {
		logicService.shutdownNow();
		log.warn("开始停止异步处理线程池，等待剩余任务执行完...");
		log.warn("DB线程池已执行完毕！");
	}

}
