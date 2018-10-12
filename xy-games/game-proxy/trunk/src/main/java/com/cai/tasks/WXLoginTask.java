/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xianyi.framework.core.transport.netty.session.C2SSession;

import protobuf.clazz.Protocol.LoginRequest;

/**
 * 
 *
 * @author wu_hc date: 2017年9月4日 下午2:56:30 <br/>
 */
public class WXLoginTask implements Runnable {

	/**
	 * 
	 */
	private static final Logger logger = LoggerFactory.getLogger(WXLoginTask.class);

	private final LoginRequest request;

	private final C2SSession session;

	/**
	 * @param request
	 */
	public WXLoginTask(LoginRequest request, C2SSession session) {
		this.request = request;
		this.session = session;
	}

	@Override
	public void run() {
	}
}
