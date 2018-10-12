package com.cai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cai.common.define.EAccountParamType;
import com.cai.common.domain.AccountParamModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.domain.Session;

/**
 * 调度
 * 
 * @author run
 *
 */
@Service
public class TaskService {

	private static Logger logger = LoggerFactory.getLogger(TaskService.class);

	//
	// 每隔5秒执行一次：*/5 * * * * ?
	// 每隔1分钟执行一次：0 */1 * * * ?
	// 每天23点执行一次：0 0 23 * * ?
	// 每天凌晨1点执行一次：0 0 1 * * ?
	// 每月1号凌晨1点执行一次：0 0 1 1 * ?
	// 每月最后一天23点执行一次：0 0 23 L * ?
	// 每周星期天凌晨1点实行一次：0 0 1 ? * L
	// 在26分、29分、33分执行一次：0 26,29,33 * * * ?
	// 每天的0点、13点、18点、21点都执行一次：0 0 0,13,18,21 * * ?

	public void taskZero() {

		try {
			// 重置今日玩家属性列表
//			resetTodayAccountParam();
		} catch (Exception e) {
			logger.error("error", e);
		}

	}



	/**
	 * 重置今日玩家属性列表
	 */
	public void resetTodayAccountParam() {
		try {
			PerformanceTimer timer = new PerformanceTimer();
			
			for(Session session:SessionServiceImpl.getInstance().getSessionMap().values()){
				if(session.getAccount()==null) continue;
				if(session.getAccount().getAccountParamModelMap()==null) continue;
				for (AccountParamModel m : session.getAccount().getAccountParamModelMap().values()) {
					EAccountParamType eAccountParamType = EAccountParamType.getEMsgType(m.getType());
					if (eAccountParamType != null) {
						if (eAccountParamType.getType() == 1) {
							m.setVal1(0);
							m.setStr1("");
							m.setLong1(0L);
							m.setDate1(null);
						}
					}
				}
			}
			logger.info("重置账号属性今日数据:" + timer.getStr());
		} catch (Exception e) {
			logger.error("error", e);
		}
	}


}
