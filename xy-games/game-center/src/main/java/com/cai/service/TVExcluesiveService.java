package com.cai.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.cai.common.constant.RMICmd;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountRecommendModel;
import com.cai.common.domain.AgentRecommendModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.TVExcluesiveLogModel;
import com.cai.common.rmi.IClubRMIServer;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.SpringService;
import com.cai.dao.PublicDAO;
import com.cai.dictionary.SysParamServerDict;

/**
 * 电视台专享统计
 * 
 * @author run
 *
 */
@Service
public class TVExcluesiveService {

	private static TVExcluesiveService instance = null;

	public static TVExcluesiveService getInstance() {
		if (null == instance) {
			instance = new TVExcluesiveService();
		}
		return instance;
	}

	private static Logger logger = LoggerFactory.getLogger(TVExcluesiveService.class);

	public void taskJob() {
		try {
			SysParamModel sysParamModel2261 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2261);
			if (sysParamModel2261 == null || sysParamModel2261.getVal1() != 1) {
				return;
			}
			Account account = PublicServiceImpl.getInstance().getAccount(sysParamModel2261.getVal2());
			if (account == null) {
				return;
			}
			Map<Long, AccountRecommendModel> map = account.getAccountRecommendModelMap();
			List<Long> idList = new ArrayList<>(map.keySet());
			int total_register = idList.size();
			int today_inc = 0;
			StringBuffer idBuffer = new StringBuffer();
			Date now = new Date();
			// now = DateUtils.addDays(now, -1);
			Date yesterdayZero = MyDateUtil.getYesterdayZeroDate(now.getTime());
			Date zeroDate = MyDateUtil.getZeroDate(now);
			long yesterdayTime = yesterdayZero.getTime();
			long yesterdayEndTime = zeroDate.getTime();
			// Date targetDate = DateUtils.addDays(now, -1);
			for (long accountId : idList) {
				Account subAccount = PublicServiceImpl.getInstance().getAccount(accountId);
				if (subAccount == null) {
					continue;
				}
				if (subAccount.getAccountModel().getCreate_time().getTime() >= yesterdayTime
						&& subAccount.getAccountModel().getCreate_time().getTime() < yesterdayEndTime) {
					today_inc += 1;
				}

				idBuffer.append(accountId).append(",");
			}
			String ids = idBuffer.toString().substring(0, idBuffer.toString().length() - 1);
			TVExcluesiveLogModel logModel = new TVExcluesiveLogModel();
			todayConsumeData(idList, logModel, yesterdayZero, zeroDate);
			Integer targetDateInt = Integer.valueOf(DateFormatUtils.format(yesterdayZero, "yyyyMMdd"));
			logModel.setNotes_date(targetDateInt);
			RemainAccount(ids, logModel);
			logModel.setTotal_register(total_register);
			logModel.setToday_inc(today_inc);
			payData(idList, logModel, yesterdayZero, zeroDate);
			matchAndCoinJoinData(idList, logModel, yesterdayZero, zeroDate);
			coinExchange(idList, logModel, yesterdayZero, zeroDate);
			clubData(idList, logModel);
			totalConsume(logModel, Integer.valueOf(DateFormatUtils.format(DateUtils.addDays(yesterdayZero, -1), "yyyyMMdd")));
			MongoDBServiceImpl.getInstance().addTVExcluesiveLogModel(logModel);
		} catch (Exception e) {
			logger.error("taskJob error!", e);
		}
	}

	public void taskTVActiveOnlineAccounts() {
		try {
			SysParamModel sysParamModel2261 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2261);
			if (sysParamModel2261 == null || sysParamModel2261.getVal3() != 1) {
				return;
			}
			Account account = PublicServiceImpl.getInstance().getAccount(sysParamModel2261.getVal2());
			if (account == null) {
				return;
			}
			Map<Long, AgentRecommendModel> map = account.getAgentRecommendModelMap();
			List<Long> idList = new ArrayList<>(map.keySet());
			Set<Long> ids = SessionServiceImpl.getInstance().getOnlineAccountIdSessionIdMap().keySet();
			int count = 0;
			for (long id : idList) {
				for (long oId : ids) {
					if (id == oId) {
						count++;
					}
				}
			}
			MongoDBServiceImpl.getInstance().addTvActivityOnlineModel(count, idList.size());
		} catch (Exception e) {
			logger.error("taskTVActiveOnlineAccounts error", e);
		}
	}

	public void brandData(List<Long> list, TVExcluesiveLogModel logModel, Date yesterdayZero, Date zeroDate) {

	}

	public void totalConsume(TVExcluesiveLogModel logModel, int yesterDayInt) {
		try {
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);

			Query query = new Query();
			query.addCriteria(Criteria.where("notes_date").is(yesterDayInt));
			TVExcluesiveLogModel model = mongoDBService.getMongoTemplate().findOne(query, TVExcluesiveLogModel.class);
			if (model != null) {
				logModel.setTotal_consume_gold(logModel.getToday_consume_gold() + model.getTotal_consume_gold());
				logModel.setTotal_recharge_gold(logModel.getToday_recharge_gold() + model.getTotal_recharge_gold());
				logModel.setTotal_exchange_coin(logModel.getToday_exchange_coin() + model.getTotal_exchange_coin());
				logModel.setTotal_brand(logModel.getTotay_brand() + model.getTotal_brand());
			} else {
				logModel.setToday_consume_gold(logModel.getToday_consume_gold());
				logModel.setTotal_recharge_gold(logModel.getToday_recharge_gold());
				logModel.setTotal_exchange_coin(logModel.getToday_exchange_coin());
			}
		} catch (Exception e) {
			logger.error("totalConsume error", e);
		}
	}

	public void todayConsumeData(List<Long> list, TVExcluesiveLogModel logModel, Date yesterdayZero, Date zeroDate) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		try {
			List<Long> sellTypeList = new ArrayList<Long>();
			sellTypeList.add(19L);
			sellTypeList.add(38L);
			sellTypeList.add(39L);
			sellTypeList.add(22L);
			sellTypeList.add(47L);
			// 查询条件
			Criteria criteria = Criteria.where("create_time").gte(yesterdayZero).lt(zeroDate).and("v2").in(sellTypeList).and("account_id").in(list);
			// 统计内容
			AggregationOperation match = Aggregation.match(criteria);
			AggregationOperation group = Aggregation.group("v2").count().as("count").sum("v1").as("total");
			Aggregation aggregation = Aggregation.newAggregation(match, group);
			AggregationResults<HashMap> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "game_log", HashMap.class);
			List<HashMap> resultMapList = result.getMappedResults();
			int total = 0;
			int brandCount = 0;
			for (HashMap map : resultMapList) {
				long id = map.get("_id") == null ? 0 : (long) map.get("_id");
				if (id == 19) {
					brandCount = map.get("count") == null ? 0 : (int) map.get("count");
				}
				if (id == 39) {
					total -= Math.abs(map.get("total") == null ? 0 : (long) map.get("total"));
				} else {
					total += Math.abs(map.get("total") == null ? 0 : (long) map.get("total"));
				}
				break;
			}
			logModel.setTotay_brand(brandCount);
			logModel.setToday_consume_gold(total);
		} catch (Exception e) {
			logger.error("todayConsumeData error", e);
		}
		// logModel.setToday_consume_gold(today_consume_gold);
	}

	// 支付相关统计
	public void payData(List<Long> list, TVExcluesiveLogModel logModel, Date yesterdayZero, Date zeroDate) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		try {
			List<Integer> sellTypeList = new ArrayList<Integer>();
			sellTypeList.add(1);
			sellTypeList.add(4);
			sellTypeList.add(6);
			sellTypeList.add(7);
			sellTypeList.add(8);
			sellTypeList.add(9);
			sellTypeList.add(10);
			// 查询条件
			Criteria criteria = Criteria.where("create_time").gte(yesterdayZero).lt(zeroDate).and("orderStatus").is(0).and("sellType")
					.in(sellTypeList).and("accountId").in(list);
			// 统计内容
			AggregationOperation match = Aggregation.match(criteria);
			AggregationOperation group = Aggregation.group().sum("rmb").as("total").sum("cardNum").as("cardNum").sum("sendNum").as("sendNum");
			Aggregation aggregation = Aggregation.newAggregation(match, group);
			AggregationResults<HashMap> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "add_card_log", HashMap.class);
			List<HashMap> resultMapList = result.getMappedResults();
			for (HashMap map : resultMapList) {
				int totalRecharge = map.get("total") == null ? 0 : (int) map.get("total");
				int cardNum = map.get("cardNum") == null ? 0 : (int) map.get("cardNum");
				int sendNum = map.get("sendNum") == null ? 0 : (int) map.get("sendNum");
				logModel.setToday_pay(totalRecharge);
				logModel.setToday_recharge_gold(cardNum + sendNum);
				break;
			}
		} catch (Exception e) {
			logger.error("payData error", e);
		}
	}

	// 比赛场跟金币场参与次数统计
	public void matchAndCoinJoinData(List<Long> list, TVExcluesiveLogModel logModel, Date yesterdayZero, Date zeroDate) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		try {
			Criteria criteria = Criteria.where("startTime").gte(yesterdayZero).lt(zeroDate).and("accountId").in(list);
			AggregationOperation match = Aggregation.match(criteria);
			AggregationOperation group = Aggregation.group().count().as("count");
			Aggregation aggregation = Aggregation.newAggregation(match, group);
			AggregationResults<HashMap> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "match_player_log", HashMap.class);
			List<HashMap> resultMapList = result.getMappedResults();
			for (HashMap map : resultMapList) {
				int count = map.get("count") == null ? 0 : (int) map.get("count");
				logModel.setMatch_brand_count(count);
				break;
			}
			Criteria criteria2 = Criteria.where("create_time").gte(yesterdayZero).lt(zeroDate).and("account_id").in(list).and("v2").is(24l);
			AggregationOperation match2 = Aggregation.match(criteria2);
			AggregationOperation group2 = Aggregation.group().count().as("count");
			Aggregation aggregation2 = Aggregation.newAggregation(match2, group2);
			AggregationResults<HashMap> result2 = mongoDBService.getMongoTemplate().aggregate(aggregation2, "game_log_money", HashMap.class);
			List<HashMap> resultMapList2 = result.getMappedResults();
			for (HashMap map : resultMapList2) {
				int count = map.get("count") == null ? 0 : (Integer) map.get("count");
				logModel.setCoin_brand_count(count);
				break;
			}
		} catch (Exception e) {
			logger.error("matchAndCoinJoinData error", e);
		}
	}

	// 金币兑换相关统计
	public void coinExchange(List<Long> list, TVExcluesiveLogModel logModel, Date yesterdayZero, Date zeroDate) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		List<Long> moneyTypeList = new ArrayList<>();
		moneyTypeList.add(12l);
		moneyTypeList.add(27l);
		moneyTypeList.add(28l);
		try {
			Criteria criteria = Criteria.where("create_time").gte(yesterdayZero).lt(zeroDate).and("account_id").in(list).and("v2").in(moneyTypeList);
			AggregationOperation match = Aggregation.match(criteria);
			AggregationOperation group = Aggregation.group().sum("v1").as("count");
			Aggregation aggregation = Aggregation.newAggregation(match, group);
			AggregationResults<HashMap> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "game_log_money", HashMap.class);
			List<HashMap> resultMapList = result.getMappedResults();
			for (HashMap map : resultMapList) {
				long count = map.get("count") == null ? 0 : (long) map.get("count");
				logModel.setToday_exchange_coin(count);
				break;
			}
		} catch (Exception e) {
			logger.error("coinExchange error", e);
		}
	}

	// 俱乐部相关统计
	public void clubData(List<Long> ids, TVExcluesiveLogModel logModel) {
		try {
			IClubRMIServer iClubRMIServer = (IClubRMIServer) SpringService.getBean("clubRMIServer");
			int count = iClubRMIServer.rmiInvoke(RMICmd.ACCOUNT_HAS_CLUB, ids);
			logModel.setJoin_club_members(count);
			int percent = count * 1000 / ids.size();
			logModel.setJoin_club_members_percent(percent);// 千分比
		} catch (Exception e) {
			logger.error("clubData error", e);
		}
	}

	// 留存率,今日活跃账号数
	private void RemainAccount(String ids, TVExcluesiveLogModel logModel) {
		try {
			PublicDAO publicDAO = SpringService.getBean(PublicService.class).getPublicDAO();
			// 新的统计
			// 1.找出所有要修改的记录
			Date now = new Date();
			Date yesterdayZero = MyDateUtil.getZeroDate(DateUtils.addDays(now, -1));
			int activeCount = publicDAO.getTVTodayActiveAccountNum(yesterdayZero, ids);
			// 上一天的
			Date targetDate = DateUtils.addDays(now, -1);
			// =================day1===================
			targetDate = DateUtils.addDays(now, -2);

			int day1 = publicDAO.getTVActiveAccountNum(MyDateUtil.getZeroDate(targetDate), MyDateUtil.getTomorrowZeroDate(targetDate), yesterdayZero,
					ids);

			// =================day3===================
			targetDate = DateUtils.addDays(now, -4);

			int day3 = publicDAO.getTVActiveAccountNum(MyDateUtil.getZeroDate(targetDate), MyDateUtil.getTomorrowZeroDate(targetDate), yesterdayZero,
					ids);

			// =================day5===================
			targetDate = DateUtils.addDays(now, -6);

			int day5 = publicDAO.getTVActiveAccountNum(MyDateUtil.getZeroDate(targetDate), MyDateUtil.getTomorrowZeroDate(targetDate), yesterdayZero,
					ids);

			// =================day7===================
			targetDate = DateUtils.addDays(now, -8);

			int day7 = publicDAO.getTVActiveAccountNum(MyDateUtil.getZeroDate(targetDate), MyDateUtil.getTomorrowZeroDate(targetDate), yesterdayZero,
					ids);

			// =================day15===================
			targetDate = DateUtils.addDays(now, -16);

			int day15 = publicDAO.getTVActiveAccountNum(MyDateUtil.getZeroDate(targetDate), MyDateUtil.getTomorrowZeroDate(targetDate), yesterdayZero,
					ids);

			// =================day30===================
			targetDate = DateUtils.addDays(now, -31);

			int day30 = publicDAO.getTVActiveAccountNum(MyDateUtil.getZeroDate(targetDate), MyDateUtil.getTomorrowZeroDate(targetDate), yesterdayZero,
					ids);
			logModel.setTotay_active(activeCount);
			logModel.setDay1(day1);
			logModel.setDay3(day3);
			logModel.setDay5(day5);
			logModel.setDay7(day7);
			logModel.setDay15(day15);
			logModel.setDay30(day30);
		} catch (Exception e) {
			logger.error("RemainAccount error", e);
		}
	}
}
