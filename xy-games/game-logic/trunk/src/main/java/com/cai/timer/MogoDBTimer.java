package com.cai.timer;

import java.util.ArrayList;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
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
	private ArrayList logArrayList = new ArrayList<>(LOG_SIZE);

	private PerformanceTimer timer = new PerformanceTimer();

	@Override
	public void run() {
		handle();
		// taskJob();
	}

	// public void taskJob() {
	// MongoDBService mongoDBService =
	// SpringService.getBean(MongoDBService.class);
	// mongoDBService.taskJob();
	// }

	public void handle() {
		timer.reset();
		logArrayList.clear();
		try {

			// 取出队列中的所有
			MongoDBServiceImpl.getInstance().getLogQueue().drainTo(logArrayList);
			if (logArrayList.size() == 0)
				return;
			// 批量入库
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			mongoDBService.getMongoTemplate().insertAll(logArrayList);

			if (timer.get() > 10000L) {
				logger.warn("Slowly process mogodb入库时间过长:" + timer.getStr());
			}

			if (logArrayList.size() > 10000) {
				logger.warn("Slowly process mongodb每秒入库数量过大请调整参数,数量:" + logArrayList.size());

				MongoDBServiceImpl.getInstance().server_error_log(0, ELogType.unkownError,
						timer.get() + "Slowly process mongodb每秒入库数量过大请调整参数,数量:" + logArrayList.size(), 0L, null, 0);
			}

			logArrayList.clear();

		} catch (Exception e) {
			logger.error("error,mongo定时入库异常", e);
			logger.error("此次入库失败 数据:{}", logArrayList);
		}

	}

}
