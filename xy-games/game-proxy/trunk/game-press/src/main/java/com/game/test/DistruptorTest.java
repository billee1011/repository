/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.game.test;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.cai.common.util.RuntimeOpt;
import com.game.common.disruptor.Dispatcher;
import com.game.common.disruptor.TaskDispatcher;

/**
 * 
 *
 * @author wu_hc date: 2017年10月24日 上午10:22:38 <br/>
 */
public final class DistruptorTest extends AbstractTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Dispatcher<Runnable> dispatcher = new TaskDispatcher(3);
		Executor executor = Executors.newFixedThreadPool(RuntimeOpt.availableProcessors());

		for (;;) {
			executor.execute(() -> {
				dispatcher.dispatch(() -> {
					System.out.println(Thread.currentThread().getName());
				});
			});

			try {
				TimeUnit.MILLISECONDS.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
