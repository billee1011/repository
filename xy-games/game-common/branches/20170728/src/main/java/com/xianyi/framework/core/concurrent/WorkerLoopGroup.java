package com.xianyi.framework.core.concurrent;

/**
 * 处理线程组
 * 
 * @author vicent.wu
 *
 */
public interface WorkerLoopGroup {

	/**
	 * 下一个
	 * 
	 * @return
	 */
	WorkerLoop next();

	/**
	 * 返回分配策略
	 * 
	 * @return
	 */
	SelectStrategy getStrategy();

	/**
	 * 总提交任务数量
	 * 
	 * @return
	 */
	long getTaskCount();

	/**
	 * 已经执行的任务数量
	 * 
	 * @return
	 */
	long getCompletedTaskCount();

	/**
	 * 
	 * 选择策略
	 */
	public static enum SelectStrategy {
		ROUND, BALANCE, RANDOM;
	}
}