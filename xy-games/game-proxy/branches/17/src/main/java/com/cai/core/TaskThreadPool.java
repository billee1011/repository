package com.cai.core;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public final class TaskThreadPool
{

	private static TaskThreadPool task = null;

	private int minPoolSize = 40;

	private int maxPoolSize = 50;

	private ThreadPoolExecutor tpe;

	public static TaskThreadPool getInstance()
	{
		if (task == null)
			task = new TaskThreadPool();
		return task;
	}

	TaskThreadPool()
	{
		tpe = new ThreadPoolExecutor(minPoolSize, maxPoolSize, 0, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(), new ThreadPoolExecutor.CallerRunsPolicy());
		ThreadFactoryBuilder tfb = new ThreadFactoryBuilder();
		tfb.setNameFormat("TaskThreadPool");
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