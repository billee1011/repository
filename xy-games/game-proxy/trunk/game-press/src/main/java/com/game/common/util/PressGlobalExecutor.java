/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.game.common.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.util.GlobalExecutor;
import com.cai.common.util.NamedThreadFactory;
import com.cai.common.util.RuntimeOpt;

/**
 * 
 *
 * @author wu_hc date: 2017年10月24日 下午12:12:06 <br/>
 */
public final class PressGlobalExecutor {
	private final static Logger logger = LoggerFactory.getLogger(GlobalExecutor.class);

	private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(RuntimeOpt.availableProcessors() << 1,
			new NamedThreadFactory("global-thread"));

	/**
	 * 
	 * @param task
	 */
	public static void execute(final Runnable task) {
		if (null == task)
			return;

		executor.execute(() -> {
			try {
				task.run();
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("global thread pool execute task error", e);
				logger.error("execption task:{}", task);
			}
		});
	}

	/**
	 * 延迟多少多少毫秒执行
	 * 
	 * @param task
	 */
	public static void schedule(final Runnable task, long delay) {
		if (null == task)
			return;

		executor.schedule(() -> {
			try {
				task.run();
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("global thread pool execute task error", e);
				logger.error("execption task:{}", task);
			}
		}, delay, TimeUnit.MILLISECONDS);
	}

	/**
	 * 
	 */
	public static void shutdownGracefully() {
		executor.shutdown();
	}

	private PressGlobalExecutor() {
	}
}
