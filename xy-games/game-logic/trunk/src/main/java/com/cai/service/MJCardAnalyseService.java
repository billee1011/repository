/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.service;

import java.util.SortedMap;

import com.cai.common.domain.Event;
import com.cai.core.MonitorEvent;
import com.cai.domain.Session;
import com.cai.game.util.TableManager;

/**
 * 
 *
 * @author wu_hc date: 2017年10月11日 下午3:54:30 <br/>
 */
public final class MJCardAnalyseService extends AbstractService {

	private static MJCardAnalyseService M = new MJCardAnalyseService();

	public static MJCardAnalyseService getInstance() {
		return M;
	}

	@Override
	protected void startService() {
		TableManager.getInstance().load();
	}

	@Override
	public MonitorEvent montior() {
		return null;
	}

	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {

	}

	@Override
	public void sessionCreate(Session session) {

	}

	@Override
	public void sessionFree(Session session) {

	}

	@Override
	public void dbUpdate(int _userID) {
	}
}
