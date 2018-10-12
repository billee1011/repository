/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.service;

import java.util.Set;

import com.cai.common.util.ConcurrentSet;
import com.cai.constant.ServiceOrder;
import com.xianyi.framework.core.service.AbstractService;
import com.xianyi.framework.core.service.IService;

/**
 * 
 *
 * @author wu date: 2017年8月29日 下午4:07:15 <br/>
 */
@IService(order = ServiceOrder.PLAYER, desc = "--")
public final class PlayerService extends AbstractService {

	private static final PlayerService M = new PlayerService();

	private final Set<Long> clubSecnePlayers = new ConcurrentSet<>();

	public static PlayerService getInstance() {
		return M;
	}

	public void enter(final long accountId) {
		clubSecnePlayers.add(accountId);
	}

	public void exit(final long accountId) {
		clubSecnePlayers.remove(accountId);
	}

	public boolean inClubScene(final long accountId) {
		return clubSecnePlayers.contains(accountId);
	}

	/**
	 * 玩家在线
	 * 
	 * @param accountId
	 * @return
	 */
	public boolean isPlayerOnline(final long accountId) {
		return SessionService.getInstance().getProxyByServerIndex(accountId) > 0;
	}

	@Override
	public void stop() throws Exception {

	}

	@Override
	public void start() throws Exception {

	}

}
