package com.cai.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import com.cai.common.define.ELogType;
import com.cai.common.domain.statistics.AccountBrandModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.timer.AccountBrandTimer;

@Service
public class MongoDBService {

	private static Logger logger = LoggerFactory.getLogger(MongoDBService.class);
	private LinkedBlockingQueue<TimerTask> logQueue = new LinkedBlockingQueue<>();
	private PerformanceTimer timer = new PerformanceTimer();
	private static final int LOG_SIZE = 10000;

	/**
	 * 临时数组
	 */
	private ArrayList<TimerTask> logArrayList = new ArrayList<>(LOG_SIZE);
	@Autowired
	private MongoTemplate mongoTemplate;

	public MongoTemplate getMongoTemplate() {
		return mongoTemplate;
	}

	public void setMongoTemplate(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	/**
	 * 记录玩家当天牌局统计数据
	 * 
	 * @param accountId
	 * @param brandType
	 * @param date
	 */
	public void saveAccountDailyBrandStatitistic(long accountId, int brandType, int notes_date, Date registerTime) {
		AccountBrandModel model = new AccountBrandModel();
		model.setAccount_id(accountId);
		model.setNotes_date(notes_date);
		model.setType(brandType);
		model.setRegisterTime(registerTime);
		TimerTask task = new AccountBrandTimer(model, mongoTemplate);
		logQueue.add(task);

	}

	public void taskJob() {
		try {
			timer.reset();
			logArrayList.clear();
			// 取出队列中的所有
			logQueue.drainTo(logArrayList);
			if (logArrayList.size() == 0)
				return;
			for (TimerTask task : logArrayList) {
				task.run();
			}
			if (timer.get() > 10000L || logArrayList.size() > 40000) {
				logger.warn("mogodb入库时间过长:" + timer.getStr());
				logger.warn("mongodb每秒入库数量过大请调整参数,数量:" + logArrayList.size());
				StringBuilder buf = new StringBuilder();
				buf.append("mogodb入库时间过长:" + timer.getStr()).append("mongodb每秒入库数量过大请调整参数,数量:" + logArrayList.size());

				MongoDBServiceImpl.getInstance().server_error_log(0, ELogType.mongoSlow, buf.toString(), 0L, null);
			}
		} catch (Exception e) {
			logger.error("MongoDBService taskJob error", e);
		}

	}

	// public <T> List<T> test(Query query, Class<T> entityClass){
	// return mongoTemplate.find(query, entityClass);
	// }

	// @Override
	// public List<Object> findListByParams(Map<String, Object> params) {
	//
	// }
	//
	// @Override
	// public List<Object> findListByQuery(Query query) {
	// // TODO Auto-generated method stub
	// return null;
	// }
	//
	// @Override
	// public Object findOne(Map<String, Object> params) {
	// // TODO Auto-generated method stub
	// return null;
	// }
	//
	// @Override
	// public Object findOne(Query query) {
	// // TODO Auto-generated method stub
	// return null;
	// }
	//
	// @Override
	// public void insert(Object object) {
	// // TODO Auto-generated method stub
	//
	// }
	//
	// @Override
	// public void save(Object object) {
	// // TODO Auto-generated method stub
	//
	// }
	//
	// @Override
	// public void insertAll(List<Object> object) {
	// // TODO Auto-generated method stub
	//
	// }

}
