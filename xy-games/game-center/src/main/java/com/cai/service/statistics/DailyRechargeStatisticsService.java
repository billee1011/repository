package com.cai.service.statistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import com.cai.dao.PublicDAO;

/**
 * 分析每日充值统计
 * @author chansonyan
 * 2018年7月4日
 */
@Service
public class DailyRechargeStatisticsService {

	private static final Logger logger = LoggerFactory.getLogger(DailyRechargeStatisticsService.class);

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private PublicDAO publicDAO;

	//private static DailyRechargeStatisticsService instance = null;

	/**
	 * 调用处理数据分析
	 */
	public void call() {
		try {
			//mongodataImport();
			publicDAO.callProcedureDailyRechargeStats();
		} catch(Exception e) {
			logger.error("充值分析错误", e);
		}
	}
	
	/**
	 * 将mongodb数据导入到
	 * 将mongodb昨日的充值全部查询出插入到Mysql中
	 * 数据量在2K条左右，直接全部查询出，不做分页插入
	 */
//	private void mongodataImport() {
//		Query query = new Query();
//		Calendar calendar = Calendar.getInstance();
//		long todayStart = TimeUtil.getTimeStart(calendar.getTime(), 0);
//		calendar.setTimeInMillis(todayStart);
//		Date todayStartDate = calendar.getTime();
//		calendar.add(Calendar.DATE, -1);
//		Date lastDayStartDate = calendar.getTime();
//		query.addCriteria(Criteria.where("create_time").gte(lastDayStartDate).lt(todayStartDate).and("orderStatus").is(0));
//		List<Integer> sellTypeList = new ArrayList<>();
//		sellTypeList.add(ESellType.BUY_CARD.getId());
//		sellTypeList.add(ESellType.GAME_PAY_CARD.getId());
//		sellTypeList.add(ESellType.SHOP_PAY_CARD.getId());
//		sellTypeList.add(ESellType.IOS_PAY_CARD.getId());
//		sellTypeList.add(ESellType.SHOP_AGENT_BUY_CARD.getId());
//		sellTypeList.add(ESellType.RECOMMEND_BUY_CARD.getId());
//		query.addCriteria(Criteria.where("sellType").in(sellTypeList));
//		List<AddCardLog> addCardLogList = this.mongoTemplate.find(query, AddCardLog.class);
//		if(addCardLogList.size() > 0) {
//			publicDAO.batchInsertAddCard(addCardLogList);
//		}
//	}
	

	public MongoTemplate getMongoTemplate() {
		return mongoTemplate;
	}

	public void setMongoTemplate(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

}
