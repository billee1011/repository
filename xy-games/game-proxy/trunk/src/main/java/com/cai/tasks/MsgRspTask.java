/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.tasks;

import com.cai.service.C2SSessionService;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import protobuf.clazz.Protocol.Response;

/**
 * 
 *
 * @author wu_hc date: 2018年2月2日 上午11:41:24 <br/>
 */
public final class MsgRspTask implements Runnable {

	private final long accountId;
	private final Response response;

	/**
	 * @param accountId
	 * @param response
	 */
	public MsgRspTask(long accountId, Response response) {
		this.accountId = accountId;
		this.response = response;
	}

	@Override
	public void run() {
		C2SSession session = C2SSessionService.getInstance().getSession(accountId);
		if (null != session && session.getSessionId() > 0) {
			session.send(response);
		}
	}
}
