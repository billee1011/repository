package com.cai.timer;

import java.util.ArrayList;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.service.MongoDBService;
import com.cai.service.MongoDBServiceImpl;

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
	@SuppressWarnings("rawtypes")
	private ArrayList logArrayList = new ArrayList<>(LOG_SIZE);

	private PerformanceTimer timer = new PerformanceTimer();

	@Override
	public void run() {
		handle();
	}

	@SuppressWarnings("unchecked")
	public synchronized void handle() {
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

			if (timer.get() > 10000L) {
				logger.error("Slowly process mogodb入库时间过长:" + timer.getStr());
			}

			if (logArrayList.size() > 40000) {
				logger.error("Slowly process mongodb每秒入库数量过大请调整参数,数量:" + logArrayList.size());
			}

			
		} catch (Exception e) {
			logger.error("error", e);
		}finally{
			logArrayList.clear();
		}
	}

}
