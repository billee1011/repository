package com.cai.core;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.util.NamedThreadFactory;
import com.cai.common.util.RuntimeOpt;

public class Global {

	private static final Logger log = LoggerFactory.getLogger(Global.class);

	public static final String SERVER_LOGIC = "logic_server";

	/**
	 * 微信获取订单
	 */
	private static ThreadPoolExecutor wxPayService =new ThreadPoolExecutor(5, 5,60L, TimeUnit.SECONDS,  new LinkedBlockingQueue<Runnable>(1000), new MyThreadFactory(ThreadNameEnum.ASYN_WORK_THREAD.getCode()),new DiscardOldestPolicy());
	

	/**
	 * 业务逻辑处理--不能依赖外部的调用
	 */
	private static ThreadPoolExecutor logicService =new ThreadPoolExecutor(10, 10,300L, TimeUnit.SECONDS,  new LinkedBlockingQueue<Runnable>(), new MyThreadFactory(ThreadNameEnum.LOGIC_SERVER.getCode()));

	/**
	 * 微信登录
	 */
	private static ThreadPoolExecutor wxService =new ThreadPoolExecutor(20, 20,300L, TimeUnit.SECONDS,  new LinkedBlockingQueue<Runnable>(), new MyThreadFactory(ThreadNameEnum.WX_LOGIN_THREAD.getCode()));
			
	/**
	 * 平台转码登录线程
	 */
	private static ThreadPoolExecutor ptLoginService =new ThreadPoolExecutor(20, 20,300L, TimeUnit.SECONDS,  new LinkedBlockingQueue<Runnable>(), new MyThreadFactory(ThreadNameEnum.NORMAL_LOGIN_THREAD.getCode()));
			
	
	/**
	 * 微信切换
	 */
	private static ThreadPoolExecutor useSwitchService =new ThreadPoolExecutor(2, 2,300L, TimeUnit.SECONDS,  new LinkedBlockingQueue<Runnable>(), new MyThreadFactory(ThreadNameEnum.WX_SWITCH_THREAD.getCode()));
			
	
	
	/**
	 * 游戏内业务异步线程---用于特殊的内部消息的转发 非io阻塞的调用
	 */
	private static ThreadPoolExecutor gameDispatchService =new ThreadPoolExecutor(8, 8,300L, TimeUnit.SECONDS,  new LinkedBlockingQueue<Runnable>(), new MyThreadFactory(ThreadNameEnum.GAME_THREAD.getCode()));
	
	
	/**
	 * ios获取订单
	 */
	private static ThreadPoolExecutor appStorePayService =new ThreadPoolExecutor(5, 5,60L, TimeUnit.SECONDS,  new LinkedBlockingQueue<Runnable>(), new MyThreadFactory(ThreadNameEnum.APPSTORE_THREAD.getCode()));
	
	
	/**
	 * 微信刷新线程
	 */
	private static ThreadPoolExecutor weiXinFlushPool =new ThreadPoolExecutor(5, 5,60L, TimeUnit.SECONDS,  new LinkedBlockingQueue<Runnable>(1000), new MyThreadFactory(ThreadNameEnum.FLUSH_WX_THREAD.getCode()),new DiscardOldestPolicy());
	
	
	/**
	 *房间内附属线程--其它地方别调用
	 */
	private static ThreadPoolExecutor roomExtraService =new ThreadPoolExecutor(16, 16,300L, TimeUnit.SECONDS,  new LinkedBlockingQueue<Runnable>(), new MyThreadFactory(ThreadNameEnum.LOGIC_SERVER.getCode()));
	
	
	
	private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(5,
			new NamedThreadFactory("xy-game-global-thread"));
	
	/**
	 * 延迟多少多少毫秒执行
	 * 
	 * @param task
	 */
	public static java.util.concurrent.ScheduledFuture<?> schedule(final Runnable task, long delay) {
		if (null == task)
			return null;

		return (java.util.concurrent.ScheduledFuture<?>) executor.schedule(() -> {
			try {
				task.run();
			} catch (Exception e) {
				e.printStackTrace();
				log.error("global thread pool execute task error", e);
				log.error("execption task:{}", task);
			}
		}, delay, TimeUnit.SECONDS);
	}
	
	
	
	/**
	 *房间内附属线程--其它地方别调用
	 * @return
	 */
	public static ThreadPoolExecutor getRoomExtraService() {
		return roomExtraService;
	}
	
	
	/**
	 *ios获取订单
	 * @return
	 */
	public static ThreadPoolExecutor getAppStoreService() {
		return appStorePayService;
	}
	
	/**
	 *微信刷新用户线程
	 * @return
	 */
	public static ThreadPoolExecutor getWeiXinFlushService() {
		return weiXinFlushPool;
	}
	
	
	
	/**
	 *游戏内业务异步线程---用于特殊的内部消息的转发 非io阻塞的调用
	 * @return
	 */
	public static ThreadPoolExecutor getGameDispatchService() {
		return gameDispatchService;
	}
	
	
	public static ThreadPoolExecutor getService(String serverLogic) {
		if(SERVER_LOGIC.equals(serverLogic)) {
			return logicService;
		}
		return gameDispatchService;
	}

	/**
	 * 微信支付线程--不公用
	 * @return
	 */
	public static ThreadPoolExecutor getWxPayService() {
		return wxPayService;
	}
	
	
	/**
	 * 用户切换线程--不公用--调用微信接口
	 * @return
	 */
	public static ThreadPoolExecutor getUseSwitchService() {
		return useSwitchService;
	}
	
	
	
	
	/**
	 * 平台转码登录线程--不公用只能用户内部登录
	 * @return
	 */
	public static ThreadPoolExecutor getPtLoginService() {
		return ptLoginService;
	}
	
	
	/**
	 * 微信登录--不公用只是用来做微信登录---
	 */
	public static ThreadPoolExecutor getWxLoginService(){
		return wxService;
	}
	
	
	/**
	 * 业务逻辑线程--普通io操作
	 */
	public static ThreadPoolExecutor getLogicService(){
		return logicService;
	}
	

	/**
	 * 关闭线程池
	 */
	public static void shutdownThreadPool() {
		try {
			logicService.shutdownNow();
			log.warn("开始停止异步处理线程池，等待剩余任务执行完...");
			wxPayService.shutdownNow();
			log.warn("DB线程池已执行完毕！");
			wxService.shutdownNow();
		} catch (Exception e) {
			log.error("关闭线程池异常");
		}
	}

}
