/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.tasks;

import com.cai.common.util.AbsEnsureTask;
import com.cai.core.SystemConfig;
import com.cai.service.SessionServiceImpl;

import protobuf.clazz.Protocol;

/**
 * @author wu_hc date: 2018/9/14 11:33 <br/>
 */
public final class CoinSenderEnsureTask extends AbsEnsureTask {

	private final Protocol.Request request;

	public CoinSenderEnsureTask(Protocol.Request request) {
		this.request = request;
	}

	@Override
	protected boolean execute() {
		return SessionServiceImpl.getInstance().sendToCoin(SystemConfig.connectCoin, this.request);
	}
}
