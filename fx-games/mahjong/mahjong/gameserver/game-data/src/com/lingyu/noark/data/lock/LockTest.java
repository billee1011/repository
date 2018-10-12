package com.lingyu.noark.data.lock;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class LockTest {

	@Test
	public void test111() {
		ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(4);
		final List<String> list = new LinkedList<>();
		list.add("111111111111111");
		scheduledExecutor.schedule(new Runnable() {
			@Override
			public void run() {

				list.add("String");
				list.add("String");
				list.add("String");
				list.add("String");
				list.add("String");
				list.add("String");
				list.add("String");
				list.add("String");
				list.add("String");
				list.add("String");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}, 0, TimeUnit.MILLISECONDS);

		scheduledExecutor.schedule(new Runnable() {
			@Override
			public void run() {
				for (String l : list) {
					System.out.println(l);
					try {
						Thread.sleep(100000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}, 0, TimeUnit.MILLISECONDS);
		scheduledExecutor.schedule(new Runnable() {
			@Override
			public void run() {
				for (String l : list) {
					System.out.println(l);
					try {
						Thread.sleep(100000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}, 1, TimeUnit.MILLISECONDS);
		scheduledExecutor.shutdown();
	}

	@Test
	public void test() {
		//Object object = new Object();
		//long start = System.currentTimeMillis();
		//for (int i = 0; i < 100000000; i++)
			//synchronized (object) {

			//}
		//System.out.println(System.currentTimeMillis() - start);
	}

	@Test
	public void test1() {
		long start = System.currentTimeMillis();
		for (int i = 0; i < 100000000; i++)

			System.out.println(System.currentTimeMillis() - start);
	}
}
