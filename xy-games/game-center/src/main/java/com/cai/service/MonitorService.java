/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.service;

import java.util.SortedMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ESmsSignType;
import com.cai.common.domain.Event;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.NamedThreadFactory;
import com.cai.core.MonitorEvent;
import com.cai.dictionary.SysParamServerDict;
import com.cai.domain.Session;
import com.cai.util.TempSmsService;

/**
 * 
 * date: 2018年7月18日 下午2:08:35 <br/>
 */
public class MonitorService extends AbstractService {
	/**
	 * 
	 */
	private static Logger logger = LoggerFactory.getLogger(MonitorService.class);

	private static MonitorService monitorService = new MonitorService();

	public static MonitorService getMonitorService() {
		return monitorService;
	}

	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("monitor-schedule-thread"));

	private AtomicInteger createAccountHour = new AtomicInteger(0);

	private int createAccountCheck = 500;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.service.AbstractService#startService()
	 */
	@Override
	protected void startService() {
		executor.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				SysParamModel sysParamModel = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(8888);
				if (sysParamModel != null) {
					createAccountCheck = sysParamModel.getVal1();
				}
				createAccountHour = new AtomicInteger(0);

			}
		}, 1, 60, TimeUnit.MINUTES);
	}

	public void sendMsgManager(String msg) {
		SysParamModel sysParamModel8888 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(8888);
		String mobiles = "18824304825,13670139534";
		if (sysParamModel8888 != null && sysParamModel8888.getStr1().length() > 10) {
			mobiles = sysParamModel8888.getStr1();
		}
		String[] ms = mobiles.split(",");
		for (String mobile : ms) {
			TempSmsService.sendDefineContent(mobile, msg, ESmsSignType.XYYX_SIGN);
		}
	}

	public void addCreateNumber() {
		try {
			if (createAccountHour.getAndIncrement() > createAccountCheck) {
				sendMsgManager("一小时注册数据异常，注册数==" + createAccountHour.get());
				createAccountHour = new AtomicInteger(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public MonitorEvent montior() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.service.AbstractService#onEvent(com.cai.common.domain.Event)
	 */
	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.cai.service.AbstractService#sessionCreate(com.cai.domain.Session)
	 */
	@Override
	public void sessionCreate(Session session) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.service.AbstractService#sessionFree(com.cai.domain.Session)
	 */
	@Override
	public void sessionFree(Session session) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.service.AbstractService#dbUpdate(int)
	 */
	@Override
	public void dbUpdate(int _userID) {

	}

}
