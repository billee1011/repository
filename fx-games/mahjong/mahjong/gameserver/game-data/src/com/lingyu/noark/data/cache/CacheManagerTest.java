package com.lingyu.noark.data.cache;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

public class CacheManagerTest {

	public static void main(String[] args) throws InterruptedException {
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(4);

		final LoadingCache<Serializable, Map<Serializable, String>> caches = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.SECONDS)
				.removalListener(new RemovalListener<Serializable, Map<Serializable, String>>() {
					@Override
					public void onRemoval(RemovalNotification<Serializable, Map<Serializable, String>> notification) {
						System.out.println(Thread.currentThread().getName() + "---->RemovalNotification--->" + sdf.format(new Date()));
						System.out.println("-----key----" + notification.getKey() + "--------value------" + notification.getValue());
					}
				}).build(new CacheLoader<Serializable, Map<Serializable, String>>() {
					@Override
					public Map<Serializable, String> load(Serializable key) throws Exception {
						System.out.println(Thread.currentThread().getName() + "---->缓存命中失败--->" + sdf.format(new Date()));
						return new HashMap<Serializable, String>();
					}
				});

		scheduledExecutor.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				System.out.println(Thread.currentThread().getName() + "---->cleanUp--->" + sdf.format(new Date()));
				caches.cleanUp();
			}
		}, 5, 5, TimeUnit.SECONDS);

		Long roleId = 1L;
		for (int i = 0; i < 1000; i++) {
			Map<Serializable, String> xx = caches.getUnchecked(roleId);
			xx.put(i, String.valueOf(i));
			Thread.sleep(1000);
		}

		scheduledExecutor.shutdown();
		Thread.sleep(50000);
	}

}
