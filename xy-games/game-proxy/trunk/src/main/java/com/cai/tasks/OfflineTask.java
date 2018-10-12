/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.tasks;

import com.cai.common.domain.Account;
import com.cai.service.C2SSessionService;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

/**
 * 
 *
 * @author wu 玩家在代理服离线的一些操作 date: 2017年8月19日 下午2:33:41 <br/>
 */
public final class OfflineTask implements Runnable {

	private final C2SSession session;

	/**
	 * @param session
	 */
	public OfflineTask(C2SSession session) {
		this.session = session;
	}

	@Override
	public void run() {
		final Account account = this.session.getAccount();
		if (null != account) {
			C2SSessionService.getInstance().notifyToAll(session);
		}
	}
}
