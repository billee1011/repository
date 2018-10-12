/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.xianyi.framework.core.concurrent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 工作队列抽象 ，对应一个线程
 * 
 * @author wu_hc
 */
public abstract class AbstractWorkerLoop implements WorkerLoop {

	/**
	 * 日志
	 */
	protected Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * ID生成器
	 */
	protected static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);

	/**
	 * 注册数量
	 */
	protected final AtomicInteger registerCounter = new AtomicInteger(0);

	/**
	 * 提交的任务数量
	 */
	protected AtomicLong submitTaskCount = new AtomicLong(0);

	/**
	 * 完成的任务数量
	 */
	protected AtomicLong completeTaskCount = new AtomicLong(0);

	/**
	 * ID
	 */
	private final int id;

	/**
	 * 
	 * @param id
	 */
	AbstractWorkerLoop(int id) {
		this.id = id;
	}

	@Override
	public void register(final IEventListener listener) {

		if (null != listener) {
			runInPriLoop(new Runnable() {

				@Override
				public void run() {
					registerCounter.incrementAndGet();
					listener.onEvent(this);
				}
			});
		}
	}

	@Override
	public void unRegister(IEventListener listener) {
		registerCounter.decrementAndGet();
	}

	@Override
	public int getRegisterCount() {
		return registerCounter.get();
	}

	@Override
	public int getId() {
		return this.id;
	}

	@Override
	public long getTaskCount() {
		return submitTaskCount.get();
	}

	@Override
	public long getCompletedTaskCount() {
		return completeTaskCount.get();
	}

	/**
	 * 
	 * @param r
	 */
	static void executeTask(final Runnable r) {

		long c = System.currentTimeMillis();
		r.run();
		if (System.currentTimeMillis() - c > 200) {
			System.err.println(String.format("#####thread[%s]--- excute task[%s] spend must time[%dms]--#####", Thread.currentThread().getName(), r,
					System.currentTimeMillis() - c));
		}
	}
}
