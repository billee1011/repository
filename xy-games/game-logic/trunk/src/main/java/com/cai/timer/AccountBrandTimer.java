package com.cai.timer;

import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.cai.common.domain.statistics.AccountBrandModel;
import com.cai.common.domain.statistics.AccountDailyBrandStatistics;

/**
 * 个人牌局数据记录
 * 
 * @author run
 *
 */
public class AccountBrandTimer extends TimerTask {

	private static Logger logger = LoggerFactory.getLogger(AccountBrandTimer.class);

	private AccountBrandModel model;
	private MongoTemplate mongoTemplate;

	public AccountBrandTimer(AccountBrandModel model, MongoTemplate mongoTemplate) {
		this.model = model;
		this.mongoTemplate = mongoTemplate;
	}

	@Override
	public void run() {
		try {
			Query query = new Query();
			query.addCriteria(Criteria.where("account_id").is(model.getAccount_id())).addCriteria(Criteria.where("date").is(model.getNotes_date()))
					.addCriteria(Criteria.where("type").is(model.getType()));
			// 牌局次数由Mongodb自增1
			Update update = new Update();
			update.inc("count", 1);
			update.set("registerTime", model.getRegisterTime());
			this.mongoTemplate.upsert(query, update, AccountDailyBrandStatistics.class);
		} catch (Exception e) {
			logger.error("MongoDBService taskJob error", e);
		}

	}
}
