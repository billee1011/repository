package com.cai.core;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.util.RuntimeOpt;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public final class RequestHandlerThreadPool {
	
	Logger logger = LoggerFactory.getLogger(RequestHandlerThreadPool.class);

	private static volatile RequestHandlerThreadPool task = null;

	private int minPoolSize = 16;

	private int maxPoolSize = 40;

	private LinkedBlockingQueue<Runnable> blockQueue = new LinkedBlockingQueue<Runnable>();

	private ThreadPoolExecutor tpe;

	public static RequestHandlerThreadPool getInstance() {
		if (task == null) {
			synchronized (RequestHandlerThreadPool.class) {
				if (task == null) {
					task = new RequestHandlerThreadPool();
				}
			}
		}
		return task;
	}

	public RequestHandlerThreadPool() {
		tpe = new ThreadPoolExecutor(RuntimeOpt.availableProcessors() << 1, RuntimeOpt.availableProcessors() << 1, 300, TimeUnit.SECONDS, blockQueue);
		ThreadFactoryBuilder tfb = new ThreadFactoryBuilder();
		tfb.setNameFormat("RequestHandlerThreadPool");
		tpe.setThreadFactory(tfb.build());
	}

	public void addTask(Runnable task) {
		try {
			tpe.execute(task);
		} catch (RejectedExecutionException exception) {
			exception.printStackTrace();
			logger.error("server threadpool full,threadpool maxsize is:" + ((ThreadPoolExecutor) tpe).getMaximumPoolSize());
		}
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