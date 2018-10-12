package com.cai.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * 游戏任务延迟处理
 * 
 * @author run
 *
 */
public class GameSchedule {

	public static ScheduledExecutorService q = null;

	/**
	 * 初始化
	 */
	static {
		ThreadFactoryBuilder tfb = new ThreadFactoryBuilder();
		tfb.setNameFormat("GameSchedule-ThreadPool-%d");
		q = Executors.newScheduledThreadPool(20, tfb.build());
	}
	
	/**
	 * 插入一个新的任务
	 * @param run
	 * @param time
	 * @param unit
	 * @return
	 * <pre>
	 * 返回的ScheduledFuture由生成者自已维护(仅需要以后取消的任务)，引用保存
	 * 如f.cancel(false) 如果任务线程已在运行中，为true表示立即中断线程，为false表示就不管了
	 * 一般设计采用f.cancel(false),不要去中断运行中的线程，这样数据处理更麻烦，设计要避免直接取消运行中的任务
	 * </pre>
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static ScheduledFuture<Object> put(Runnable run,long time, TimeUnit unit){
		ScheduledFuture f = q.schedule(run, time, unit);
		return f;
	}
	

//	//暂时用不到，关闭
//	public static <V> ScheduledFuture<V> put(Callable<V> callable,long time, TimeUnit unit){
//		ScheduledFuture f = q.schedule(callable, time, unit);
//		return f;
//	}
	
	
	/**
	 * 立即停止所有任务，系统关闭时调用,维护缓存同步数据库之前
	 */
	public static void sysShutdown(){
		q.shutdownNow();
	}
	
	

}
