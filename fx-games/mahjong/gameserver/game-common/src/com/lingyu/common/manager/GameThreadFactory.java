package com.lingyu.common.manager;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class GameThreadFactory implements ThreadFactory {
	private final String name;
	private final AtomicInteger threadCounter = new AtomicInteger(1);

	public Thread newThread(Runnable runnable) {
		StringBuilder threadName = new StringBuilder(name);

		threadName.append("-").append(threadCounter.getAndIncrement());

		Thread thread = new Thread(group, runnable, threadName.toString());
		if (thread.isDaemon())
			thread.setDaemon(false);
		if (thread.getPriority() != Thread.NORM_PRIORITY)
			thread.setPriority(Thread.NORM_PRIORITY);
		return thread;
	}

	final ThreadGroup group;

	public GameThreadFactory(String name) {
		SecurityManager securitymanager = System.getSecurityManager();
		this.group = securitymanager == null ? Thread.currentThread().getThreadGroup() : securitymanager.getThreadGroup();
		this.name = name;
	}
}
