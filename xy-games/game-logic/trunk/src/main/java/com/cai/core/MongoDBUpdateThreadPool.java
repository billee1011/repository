package com.cai.core;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.BrandLogModel;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * mongodb数据异步更新线程池
 * @author run
 *
 */
public final class MongoDBUpdateThreadPool {
	
	private static final Logger logger = LoggerFactory.getLogger(MongoDBUpdateThreadPool.class);

	private static volatile MongoDBUpdateThreadPool task = null;

	private int minPoolSize = 5;

	private int maxPoolSize = 20;

	// 消息缓冲队列
	private LinkedBlockingQueue<MongoDBUpdateRunnable> msgQueue = new LinkedBlockingQueue<MongoDBUpdateRunnable>();

	private LinkedBlockingQueue<Runnable> blockQueue = new LinkedBlockingQueue<Runnable>();

	private ThreadPoolExecutor tpe;

	public static MongoDBUpdateThreadPool getInstance() {
		if (task == null){
			synchronized (MongoDBUpdateThreadPool.class) {
				if (task == null) {
					task = new MongoDBUpdateThreadPool();
				}
			}
		}
		return task;
	}

	public MongoDBUpdateThreadPool() {
		tpe = new ThreadPoolExecutor(minPoolSize, maxPoolSize, 300, TimeUnit.SECONDS, blockQueue);
		ThreadFactoryBuilder tfb = new ThreadFactoryBuilder();
		tfb.setNameFormat("MongoDBUpdateThreadPool");
		tpe.setThreadFactory(tfb.build());

	}

	public void addTask(MongoDBUpdateRunnable task) {
		try {
			tpe.execute(task);
		} catch (RejectedExecutionException exception) {
			exception.printStackTrace();
			logger.error("server threadpool full,threadpool maxsize is:" + ((ThreadPoolExecutor) tpe).getMaximumPoolSize());
		}
	}
	
	public void addTask(BrandLogModel brandLogModel){
		addTask(new MongoDBUpdateRunnable(brandLogModel));
	}
	

	public LinkedBlockingQueue<Runnable> getBlockQueue() {
		return blockQueue;
	}

	public ThreadPoolExecutor getTpe() {
		return tpe;
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

}