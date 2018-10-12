package com.cai.core;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public final class RequestHandlerThreadPool {
	Logger logger = LoggerFactory.getLogger(RequestHandlerThreadPool.class);

	private static RequestHandlerThreadPool task = null;

	private int minPoolSize = 40;

	private int maxPoolSize = 50;

	// 消息缓冲队列
	private LinkedBlockingQueue<Runnable> blockQueue = new LinkedBlockingQueue<Runnable>();

	private ThreadPoolExecutor tpe;

	public static RequestHandlerThreadPool getInstance() {
		if (task == null)
			task = new RequestHandlerThreadPool();
		return task;
	}

	public RequestHandlerThreadPool() {
		int seconds = 300;
		if(SystemConfig.gameDebug==0){
			seconds = 60;
		}
		//300为了调试，防止线程被杀
		tpe = new ThreadPoolExecutor(minPoolSize, maxPoolSize, seconds, TimeUnit.SECONDS, blockQueue);
		ThreadFactoryBuilder tfb = new ThreadFactoryBuilder();
		tfb.setNameFormat("RequestHandlerThreadPool-%d");
		tpe.setThreadFactory(tfb.build());
	}

	public void addTask(Runnable task) {
		try {
			// if(tpe.getTaskCount()%100 == 0){
			// System.out.println("当煎收到消息数:"+tpe.getTaskCount()+",已处理消息数:"+tpe.getCompletedTaskCount()+",当前排队数量:"+blockQueue.size());
			// }
			tpe.execute(task);
		} catch (RejectedExecutionException exception) {
			exception.printStackTrace();
			logger.error("server threadpool full,threadpool maxsize is:" + ((ThreadPoolExecutor) tpe).getMaximumPoolSize());
		}
	}

	public synchronized void shutdown() {
		tpe.shutdown();
	}

	public int getActiveCount() {
		return tpe.getActiveCount();
	}

	public synchronized boolean remove(Runnable task) {
		return tpe.remove(task);
	}

	public LinkedBlockingQueue<Runnable> getBlockQueue() {
		return blockQueue;
	}

	public void setBlockQueue(LinkedBlockingQueue<Runnable> blockQueue) {
		this.blockQueue = blockQueue;
	}

	public ThreadPoolExecutor getTpe() {
		return tpe;
	}

}