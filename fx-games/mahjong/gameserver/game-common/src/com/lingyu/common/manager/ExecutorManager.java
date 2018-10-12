package com.lingyu.common.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

/**
 * @author Allen Jiang
 */
@Service
public class ExecutorManager {
	private static final Logger logger = LogManager.getLogger(ExecutorManager.class);
	private static List<ExecutorService> executorList = new ArrayList<ExecutorService>();

	public static abstract class ExecTask implements Runnable {
		public ExecTask(Object target) {
			this.target = target;
		}

		private Object target;

		public Object getTarget() {
			return target;
		}

		public void setTarget(Object target) {
			this.target = target;
		}
	}

	/**
	 * @param name
	 *            线程名，当threadNum>1，代表线程名前缀（name-num）
	 * @param threadNum
	 *            线程池的大小
	 */
	public static ExecutorService createAndRegistExecutor(String name, int threadNum) {
		ExecutorService ret = Executors.newFixedThreadPool(threadNum, new GameThreadFactory(name));
		executorList.add(ret);
		return ret;
	}

	public void destory() {
		for (ExecutorService each : executorList) {
			each.shutdown();
			try {
				if (!each.awaitTermination(5, TimeUnit.SECONDS)) {
					each.shutdownNow();
					if (!each.awaitTermination(5, TimeUnit.SECONDS)) {
						logger.error("不能停止服务:", each);
					}
				}
			} catch (InterruptedException e) {
				each.shutdownNow();
				Thread.currentThread().interrupt();
				logger.info("", e);
			}
		}
	}
}
