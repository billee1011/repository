package com.cai.common.util;

/**
 * 
 * @author wu
 */
public interface Lifecycle {

	/**
	 * 启动
	 */
	void start() throws Exception;

	/**
	 * 停止
	 */
	void stop() throws Exception;

	/**
	 * 
	 * @return
	 */
	boolean isRunning();
}
