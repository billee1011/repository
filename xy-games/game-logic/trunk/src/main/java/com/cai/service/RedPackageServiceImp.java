/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.service;

import java.util.SortedMap;

import com.cai.common.domain.Event;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.SpringService;
import com.cai.core.MonitorEvent;
import com.cai.domain.Session;

/**
 *
 * 操作账户红包信息
 * 
 * @author tang
 */
public class RedPackageServiceImp extends AbstractService {

	private static RedPackageServiceImp instance = null;

	public static RedPackageServiceImp getInstance() {
		if (null == instance) {
			instance = new RedPackageServiceImp();
		}
		return instance;
	}

	public boolean addRedPackageRecord(long accountId,int money,int activity_id){
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		return centerRMIServer.operateRedActivityModel(accountId, money, 1,activity_id);
	}
	@Override
	protected void startService() {
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
