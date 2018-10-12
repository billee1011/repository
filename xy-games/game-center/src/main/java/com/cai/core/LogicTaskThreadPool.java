package com.cai.core;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public final class LogicTaskThreadPool
{

	private static volatile LogicTaskThreadPool task = null;

	private int minPoolSize = 5;

	private int maxPoolSize = 5;

	private ThreadPoolExecutor tpe;

	public static LogicTaskThreadPool getInstance()
	{
		if (task == null) {
			synchronized (LogicTaskThreadPool.class) {
				if(task==null) {
					task = new LogicTaskThreadPool();
				}
			}
		}
		return task;
	}

	LogicTaskThreadPool()
	{
		tpe = new ThreadPoolExecutor(minPoolSize, maxPoolSize, 0, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(), new ThreadPoolExecutor.CallerRunsPolicy());
		ThreadFactoryBuilder tfb = new ThreadFactoryBuilder();
		tfb.setNameFormat("LogicTaskThreadPool");
		tpe.setThreadFactory(tfb.build());
	}

	public synchronized void addTask(Runnable task)
	{
		tpe.execute(task);
	}

	public synchronized void shutdown()
	{
		tpe.shutdown();
	}

	public int getActiveCount()
	{
		return tpe.getActiveCount();
	}

	public synchronized boolean remove(Runnable task)
	{
		return tpe.remove(task);
	}

}