package com.xianyi.framework.net.server;

/**
 * 
 * 网络接收器接口
 */
public interface WRServer {

	/**
	 * 启动
	 */
	void start();

	/**
	 * 关闭
	 */
	void shutdown();

	/**
	 * 邦定端口
	 * 
	 * @return
	 */
	int boundPort();
}
