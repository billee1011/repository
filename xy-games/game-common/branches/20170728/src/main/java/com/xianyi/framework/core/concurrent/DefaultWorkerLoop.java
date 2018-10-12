package com.xianyi.framework.core.concurrent;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 
 * @author wu_hc
 *
 */
public final class DefaultWorkerLoop extends AbstractWorkerLoop implements WorkerLoop {

	/**
	 * 循环间隔
	 */
	// private static final int LOOP_INTERVAL_MS = 80;

	/**
	 * 高优先级的任务,在每次主循環中執行完
	 */
	private final Queue<Runnable> priorityQueue = new ConcurrentLinkedQueue<>();

	/**
	 * 任务缓冲队列，在每次主循环中取出一个任务进行处理
	 */
	private final BlockingQueue<Runnable> taskQueue;

	/**
	 * 
	 */
	// private int counter = 0;

	// private long resetCounterTime = System.currentTimeMillis();

	/**
	 * 
	 * @param prefix
	 */
	public DefaultWorkerLoop(final String prefix) {
		this(prefix, -1);
	}

	/**
	 * 
	 * @param prefix
	 * @param index
	 */
	public DefaultWorkerLoop(final String prefix, final int index) {
		this(prefix, index, 1 << 16);
	}

	/**
	 * 
	 * @param prefix
	 * @param index
	 * @param capacity
	 */
	public DefaultWorkerLoop(final String prefix, final int index, int capacity) {
		super(ID_GENERATOR.incrementAndGet());
		if (capacity <= 0)
			throw new IllegalArgumentException("capacity must more than zero!!");

		// 多生产者单消费者
		// taskQueue = PlatformDependent.newMpscQueue();
		taskQueue = new LinkedBlockingQueue<Runnable>(capacity);

		EventWorker worker = new EventWorker(index >= 0 ? (prefix + "-" + index) : prefix);
		worker.start();
	}

	@Override
	public void runInLoop(Runnable task) {
		boolean result = taskQueue.offer(task);
		submitTaskCount.incrementAndGet();
		if (!result) {
			log.error("##### thread[{}]--- NOR task Queue full!! ---#####", Thread.currentThread().getName());
		}
	}

	@Override
	public void runInPriLoop(Runnable task) {
		boolean result = priorityQueue.offer(task);
		submitTaskCount.incrementAndGet();
		if (!result) {
			log.error("##### thread[{}]--- PRI task Queue error!! ----#####", Thread.currentThread().getName());
		}
	}

	// 处理事件的线程
	private final class EventWorker extends Thread {

		public EventWorker(final String name) {
			super(name);
			setDaemon(true);
		}

		@Override
		public void run() {
			for (;;) {
				try {
					mainLoop();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		/**
		 * 线程主循环
		 * 
		 * @throws Exception
		 */
		private void mainLoop() throws Exception {

			Runnable r;
			while ((r = priorityQueue.poll()) != null) {
				executeTask(r);
				completeTaskCount.incrementAndGet();
			}
			r = taskQueue.take();
			if (null != r) {
				executeTask(r);
				completeTaskCount.incrementAndGet();
			}

			// 每十万次循环耗时较短(防止队列中任务太少，线程跑空)
			// if (counter++ >= 100000) {
			// long currentMs = System.currentTimeMillis();
			// long interval = currentMs - resetCounterTime;
			// if (interval < LOOP_INTERVAL_MS) {
			// TimeUnit.MILLISECONDS.sleep(LOOP_INTERVAL_MS - interval);
			// }
			// counter = 0;
			// resetCounterTime = currentMs;
			// }
		}
	}

	@Override
	public String toString() {
		return "DefaultEventLoop [getRegisterCount()=" + getRegisterCount() + ", getId()=" + getId() + "]";
	}

}
