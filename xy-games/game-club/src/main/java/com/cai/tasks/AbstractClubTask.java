/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.util.SystemClock;

/**
 * 
 *
 * @author wu_hc date: 2018年5月21日 上午10:29:50 <br/>
 */
public abstract class AbstractClubTask implements Runnable {

	/**
	 * 日志
	 */
	protected static final Logger logger = LoggerFactory.getLogger(AbstractClubTask.class);

	@Override
	public void run() {
		long cur = SystemClock.CLOCK.now();

		exe();

		long now = SystemClock.CLOCK.now();

		if (now - cur > 1000L) {
			logger.warn("俱乐部耗时任务[{}ms],thread:{},class:{}", now - cur, Thread.currentThread().getName(), this.getClass());
		}
	}

	protected abstract void exe();

}
