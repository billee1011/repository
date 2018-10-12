package com.cai.core;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class MyThreadFactory implements ThreadFactory {

	private AtomicInteger threadNumber = new AtomicInteger(1);

	private String threadName;

	public MyThreadFactory(String namePerfix) {
		this.threadName = namePerfix + "-";
	}

	@Override
	public Thread newThread(Runnable r) {
		String name = threadName + threadNumber.getAndIncrement();
		Thread t = new Thread(r, name);
		t.setDaemon(false);
		t.setPriority(Thread.NORM_PRIORITY);
		return t;
	}

}