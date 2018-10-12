package com.cai.timer;

import java.util.ArrayList;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.service.MongoDBService;
import com.cai.service.MongoDBServiceImpl;
import com.google.common.collect.Lists;

/**
 * mongo定时入库
 * 
 * @author run
 *
 */
public class MogoDBTimer extends TimerTask {

	private static Logger logger = LoggerFactory.getLogger(MogoDBTimer.class);

	private static final int LOG_SIZE = 10000;

	/**
	 * 临时数组
	 */
	private ArrayList logArrayList = new ArrayList<>(LOG_SIZE);

	private PerformanceTimer timer = new PerformanceTimer();

	@Override
	public void run() {
		handle();
	}

	public void handle() {
		try {
			timer.reset();
			logArrayList.clear();
			// 取出队列中的所有
			MongoDBServiceImpl.getInstance().getLogQueue().drainTo(logArrayList);
			if (logArrayList.size() == 0)
				return;
			// 批量入库
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			mongoDBService.getMongoTemplate().insertAll(logArrayList);

			if (timer.get() > 10000L || logArrayList.size() > 40000) {
				logger.warn("mogodb入库时间过长:" + timer.getStr());
				logger.warn("mongodb每秒入库数量过大请调整参数,数量:" + logArrayList.size());
				
				StringBuilder buf = new StringBuilder();
				buf.append("mogodb入库时间过长:" + timer.getStr()).append("mongodb每秒入库数量过大请调整参数,数量:" + logArrayList.size());

				MongoDBServiceImpl.getInstance().server_error_log(0,ELogType.mongoSlow, buf.toString(), 0L, null);
			}
			
		} catch (Exception e) {
			logger.error("error", e);
		}finally{
			logArrayList.clear();
		}
	}

}
