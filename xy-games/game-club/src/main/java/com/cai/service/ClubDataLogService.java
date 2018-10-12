package com.cai.service;

import com.cai.constant.ServiceOrder;
import com.cai.manager.ClubDataLogManager;
import com.xianyi.framework.core.service.AbstractService;
import com.xianyi.framework.core.service.IService;

@IService(order = ServiceOrder.CLUB_DATA_LOG, desc = "亲友圈数据统计")
public class ClubDataLogService extends AbstractService {

	private static final ClubDataLogService INSTANCE = new ClubDataLogService();

	public static ClubDataLogService getInstance() {
		return INSTANCE;
	}

	@Override
	public void start() throws Exception {
		ClubDataLogManager.init();
	}

	@Override
	public void stop() throws Exception {
		ClubDataLogManager.saveLog();
	}

}
