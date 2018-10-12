package com.lingyu.common.manager;

import java.lang.reflect.Field;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MemoryManager {
	private static final Logger logger = LogManager.getLogger(MemoryManager.class);
	private static MemoryManager instance;

	public static MemoryManager getInstance() {
		if (null == instance) {
			instance = new MemoryManager();
		}
		return instance;
	}

	/** 60s后启动 */
	private final static int TIME_DELAY = 60000;
	/** 每60s统计,太短的设置可能会导致玩家登陆不了 */
	private final static int TIME_INTERVAL = 60000;

	public void initialize() {
		this.init(TIME_DELAY, TIME_INTERVAL);
	}

	public void init(int delayTime, int intervalTime) {
		final Timer timer = new Timer("Memory Monitor", true);
		timer.schedule(new StatTimerTask(), delayTime, intervalTime);
		logger.info("内存监控线程将在{}秒后启动，采样间隔为interval={} 秒", (delayTime / 1000), intervalTime / 1000);
	}

	static class StatTimerTask extends TimerTask {
		@Override
		public void run() {
			try {
				Class<?> c = Class.forName("java.nio.Bits");
				Field maxMemory = c.getDeclaredField("maxMemory");
				maxMemory.setAccessible(true);
				Field reserverdMemory = c.getDeclaredField("reservedMemory");
				reserverdMemory.setAccessible(true);
				Long maxMemoryValue = (Long) maxMemory.get(null);
				AtomicLong reserverdMemoryValue = (AtomicLong) reserverdMemory.get(null);
				
				logger.info("服务器堆内存总共 {} M,占用堆内存 {} M,直接内存总共 {} M,占用直接内存 {} M", new Object[] { Runtime.getRuntime().totalMemory() / (1024 * 1024),
						(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024), maxMemoryValue / (1024 * 1024),
						reserverdMemoryValue.floatValue() / (1024 * 1024) });
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}
}
