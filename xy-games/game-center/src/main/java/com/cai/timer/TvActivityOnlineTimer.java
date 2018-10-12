/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.timer;

import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.util.ThreadUtil;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.TVExcluesiveService;

/**
 * TV专享活动实时在线统计
 * 
 *
 * @author Administrator date: 2018年6月22日 下午12:09:20 <br/>
 */
public class TvActivityOnlineTimer extends TimerTask {

	private static Logger logger = LoggerFactory.getLogger(TvActivityOnlineTimer.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.TimerTask#run()
	 */
	@Override
	public void run() {
		try {
			TVExcluesiveService.getInstance().taskTVActiveOnlineAccounts();
		} catch (Exception e) {
			logger.error("error", e);
			MongoDBServiceImpl.getInstance().server_error_log(0, ELogType.unkownError, ThreadUtil.getStack(e), 0L, null);
		}
	}

}
