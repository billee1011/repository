package com.cai.core;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public final class TaskThreadPool {
	private static final Logger logger = LoggerFactory.getLogger(TaskThreadPool.class);

	private static TaskThreadPool task = new TaskThreadPool();

	private ThreadPoolExecutor tpe;

	public static TaskThreadPool getInstance() {
		return task;
	}

	private TaskThreadPool() {
		tpe = new ThreadPoolExecutor(4, 4, 300, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1000), new ThreadPoolExecutor.DiscardPolicy());
		ThreadFactoryBuilder tfb = new ThreadFactoryBuilder();
		tfb.setNameFormat("TaskThreadPool");
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