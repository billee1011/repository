/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.tasks;

import com.cai.net.core.ClientHandler;
import com.google.protobuf.GeneratedMessage;

/**
 * 
 * 逻辑服执行器
 *
 * @author wu_hc date: 2018年1月13日 下午4:00:16 <br/>
 */
public final class RoomInvokerTask implements Runnable {

	private final ClientHandler<? extends GeneratedMessage> handler;

	public RoomInvokerTask(ClientHandler<? extends GeneratedMessage> handler) {
		this.handler = handler;
	}

	@Override
	public void run() {
		if (null != handler) {
			try {
				handler.onRequest();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
