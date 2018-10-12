package com.cai.service.statistics;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapreduce.MapReduceResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.cai.common.domain.Account;
import com.cai.common.domain.CoinPlayerSummaryLogModel;
import com.cai.common.domain.EveryDayAccountModel;
import com.cai.common.domain.ProxyStatusModel;
import com.cai.common.domain.statistics.AccountDailyBrandStatistics;
import com.cai.common.domain.statistics.DailyCoinExchangeStatistics;
import com.cai.common.domain.statistics.DailyReportStatistics;
import com.cai.common.domain.statistics.RealTimeStatistics;
import com.cai.common.util.MathUtil;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.TimeUtil;
import com.cai.dao.PublicDAO;
import com.cai.mapreduce.MultiResultMapreduceData;
import com.cai.rmi.impl.CenterRMIServerImpl;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PublicServiceImpl;

/**
 * 处理中心服统计业务
 * 
 * @author chansonyan 2018年6月12日
 */
@Service
@SuppressWarnings("unchecked")
public class CenterStatisticsService {

	private static final Logger logger = LoggerFactory.getLogger(CenterStatisticsService.class);

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private PublicDAO publicDAO;

	private static CenterStatisticsService instance = null;

	// 统计当天牌局，只做内存统计
	private Map<Integer, Integer> brandAccountSummaryMap = new HashMap<>(10);
	private Map<Integer, Integer> newAccountSummaryMap = new HashMap<>(10);
	private int totalNewUserBrand = 0;
	private int totalNewUserOnline = 0;
	private int totalClubUser = 0;
	private int totalNormalUser = 0;

	public CenterStatisticsService() {

	}

	/**
	 * 统计玩家参与的牌局人数
	 */
	public void brandStatistics() {
		brandAccountSummaryMap.clear();
		newAccountSummaryMap.clear();
		Map<Long, Account> allAccountMap = PublicServiceImpl.getInstance().getAccountIdMap();
		Iterator<Entry<Long, Account>> iterator = allAccountMap.entrySet().iterator();
		Map.Entry<Long, Account> entry = null;
		Account account = null;
		Map<Integer, AccountDailyBrandStatistics> accountBrandMap = null;
		long zeroTime = MyDateUtil.getZeroDate(new Date()).getTime();
		int totalNewUserBrand = 0;
		int totalNewUserOnline = 0;
		int totalClubUser = 0;
		int totalNormalUser = 0;
		while (iterator.hasNext()) {
			entry = iterator.next();
			account = entry.getValue();
			accountBrandMap = account.getAccountDailyBrandMap();
			for (Integer key : accountBrandMap.keySet()) {
				if (key == 5) {
					totalClubUser++;
				} else {
					totalNormalUser++;
				}
				Integer value = brandAccountSummaryMap.get(key);
				if (null == value) {
					value = 0;
				}
				value += accountBrandMap.get(key).getCount();
				brandAccountSummaryMap.put(key, value);
				if (zeroTime <= account.getAccountModel().getCreate_time().getTime()) {
					totalNewUserOnline++;
					Integer newValue = newAccountSummaryMap.get(key);
					if (null == newValue) {
						newValue = 0;
					}
					newValue += accountBrandMap.get(key).getCount();
					totalNewUserBrand += accountBrandMap.get(key).getCount();
					newAccountSummaryMap.put(key, newValue);
				}
			}
			// 清空牌局记录
			accountBrandMap.clear();
		}
		this.totalNewUserBrand = totalNewUserBrand;
		this.totalNewUserOnline = totalNewUserOnline;
		this.totalClubUser = totalClubUser;
		this.totalNormalUser = totalNormalUser;
	}

	/**
	 * 初始化统计查询器
	 */
	@PostConstruct
	private void initQuartz() {
		// 实时统计
		instance = this;
	}

	/**
	 * 金币兑换统计
	 */
	public void coinExchangeStatistics() {
		try {
			long start = TimeUtil.getTimeStart(new Date(), 0);
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(start);
			Date endDate = calendar.getTime();
			calendar.add(Calendar.DATE, -1);
			Date startDate = calendar.getTime();
			// 使用MapReduce统计金币兑换
			MapReduceResults<MultiResultMapreduceData> orderResult = MongoDBServiceImpl.getInstance().coinExchangeStatisticsMapReduce(startDate,
					endDate);
			for (MultiResultMapreduceData temp : orderResult) {
				JSONObject jsonObject = JSONObject.parseObject(temp.getValue());
				// 兑换人数（去重）和总笔数
				int accountCount = jsonObject.getIntValue("distAcCount");
				int count = jsonObject.getIntValue("count");
				DailyCoinExchangeStatistics dailyCoinExchangeStatistics = new DailyCoinExchangeStatistics();
				dailyCoinExchangeStatistics.setCurrent_date(startDate);
				dailyCoinExchangeStatistics.setCount(count);
				dailyCoinExchangeStatistics.setDist_account(accountCount);
				publicDAO.insertDailyCoinExchangeStatistics(dailyCoinExchangeStatistics);
			}
		} catch (Exception e) {
			logger.error("金币兑换统计出错", e);
		}
	}

	/**
	 * 生成昨日报表，并入库
	 * 
	 */
	public void createLastDailyReport(EveryDayAccountModel everyDayAccountModel) {
		try {
			// 计算昨天结束时间
			Calendar calendar = Calendar.getInstance();
			long startMillion = TimeUtil.getTimeStart(calendar.getTime(), 0);
			calendar.setTimeInMillis(startMillion);
			Date lastDayEndDate = calendar.getTime();
			calendar.add(Calendar.DATE, -1);
			Date lastDayStartDate = calendar.getTime();
			RealTimeStatistics lastDayStatistics = this.lastdayRealTimeStatistics(lastDayStartDate, lastDayEndDate);
			DailyReportStatistics dailyReportStatistics = new DailyReportStatistics();
			dailyReportStatistics.setRegisterAccount(lastDayStatistics.getRegisterAccount());
			// 统计在线有延迟，需要从每日报表中获取
			dailyReportStatistics.setActiveAccount(everyDayAccountModel.getActive_account_num());
			dailyReportStatistics.setNormalPayAccount(lastDayStatistics.getNormalPayAccount());
			dailyReportStatistics.setNormalPayAmount(lastDayStatistics.getNormalPayAmount());
			dailyReportStatistics.setAgentPayAccount(lastDayStatistics.getAgentPayAccount());
			dailyReportStatistics.setAgentPayAmount(lastDayStatistics.getAgentPayAmount());

			// 付费用户 /活跃用户
			dailyReportStatistics
					.setPayRate(MathUtil.divide((dailyReportStatistics.getAgentPayAccount() + dailyReportStatistics.getNormalPayAccount()),
							dailyReportStatistics.getActiveAccount()));
			// 普通ARPPU
			dailyReportStatistics
					.setNormalArppu(MathUtil.divide(dailyReportStatistics.getNormalPayAmount(), dailyReportStatistics.getNormalPayAccount()));
			// 代理ARPPU
			dailyReportStatistics
					.setAgentArppu(MathUtil.divide(dailyReportStatistics.getAgentPayAmount(), dailyReportStatistics.getAgentPayAccount()));
			dailyReportStatistics.setDate(lastDayStartDate);
			setUserAveBrandByType(lastDayEndDate, dailyReportStatistics);
			setBrandCountByType(lastDayEndDate, dailyReportStatistics);
			// 查询平均在线时长
			Long avgOnline = publicDAO.callProcedureDailyOnlineTime(lastDayStartDate);
			dailyReportStatistics.setUserAveOnlineTime(avgOnline);

			this.mongoTemplate.save(dailyReportStatistics);
		} catch (Exception e) {
			logger.error("统计日报表出错", e);
		}
	}

	public void setNewUserBrand(Date date, DailyReportStatistics dailyReportStatistics) {
		try {
			// Date targetDate = DateUtils.addDays(date, -1);
			// int targetDateInt =
			// Integer.valueOf(DateFormatUtils.format(targetDate, "yyyyMMdd"));
			// Date zero = MyDateUtil.getZeroDate(targetDate);
			// Date end = MyDateUtil.getTomorrowZeroDate(targetDate);
			// AggregationOperation match =
			// Aggregation.match(Criteria.where("notes_date").is(targetDateInt).and("registerTime").gte(zero).lt(end));
			// AggregationOperation group =
			// Aggregation.group().count().as("userCount").sum("count").as("brandCount");
			// Aggregation aggregation = Aggregation.newAggregation(match,
			// group);
			// MongoDBService mongoDBService =
			// SpringService.getBean(MongoDBService.class);
			// AggregationResults<HashMap> result =
			// mongoDBService.getMongoTemplate().aggregate(aggregation,
			// "statistics_account_dailybrand",
			// HashMap.class);
			// List<HashMap> list = result.getMappedResults();
			// if (list != null && list.size() > 0) {
			// for (HashMap hashMap : list) {

			if (totalNewUserOnline == 0 || totalNewUserBrand == 0) {
				dailyReportStatistics.setNewUserBrand(0);
				dailyReportStatistics.setNewUserCount(0);
				dailyReportStatistics.setNewUserAveBrand(0);
			} else {
				int userCount = totalNewUserOnline == 0 ? 1 : totalNewUserOnline;
				int brandCount = totalNewUserBrand == 0 ? 1 : totalNewUserBrand;
				dailyReportStatistics.setNewUserBrand(brandCount);
				dailyReportStatistics.setNewUserCount(userCount);
				double newUserAveBrand = MathUtil.divide(brandCount, userCount);
				dailyReportStatistics.setNewUserAveBrand(newUserAveBrand);
			}
			// break;
			// }

			// }
		} catch (Exception e) {
			logger.error("everyDayRobotOpenRoom error", e);
		}
	}

	/**
	 * 昨天实时统计，用于日报表 只生成数据用于日报表中
	 */
	public RealTimeStatistics lastdayRealTimeStatistics(Date lastDayStartDate, Date lastDayEndDate) {
		RealTimeStatistics realTimeStatistics = new RealTimeStatistics();
		realTimeStatistics.setStartDailyDate(lastDayStartDate);
		realTimeStatistics.setDate(lastDayEndDate);
		this.generateRealTimeStatistics(realTimeStatistics);
		return realTimeStatistics;
	}

	/**
	 * 实时统计 5分钟调度
	 */
	public void realTimeStatistics() {
		RealTimeStatistics realTimeStatistics = new RealTimeStatistics();
		realTimeStatistics.setDate(new Date());
		long start = TimeUtil.getTimeStart(realTimeStatistics.getDate(), 0);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(start);
		realTimeStatistics.setStartDailyDate(calendar.getTime());
		this.generateRealTimeStatistics(realTimeStatistics);
		this.mongoTemplate.save(realTimeStatistics);
	}

	/**
	 * 生成实时统计 账号类型0为普通，1为代理
	 * 
	 * @param realTimeStatistics
	 */
	private void generateRealTimeStatistics(RealTimeStatistics realTimeStatistics) {
		try {
			// 统计在线
			CenterRMIServerImpl centerRMIServerImpl = SpringService.getBean(CenterRMIServerImpl.class);
			List<ProxyStatusModel> proxyList = centerRMIServerImpl.getProxyStatusList();
			for (ProxyStatusModel temp : proxyList) {
				realTimeStatistics.setOnline(realTimeStatistics.getOnline() + temp.getOnline_playe_num());
			}
			// 实时注册用户数
			int registerCount = publicDAO.getAccountCreateNumByTime(realTimeStatistics.getStartDailyDate(), realTimeStatistics.getDate());
			realTimeStatistics.setRegisterAccount(registerCount);
			// 实时活跃用户数
			int activeCount = publicDAO.getAccountActiveOnlineNum(realTimeStatistics.getStartDailyDate(), realTimeStatistics.getDate());
			realTimeStatistics.setActiveAccount(activeCount);
			// 使用MapReduce统计充值
			MapReduceResults<MultiResultMapreduceData> orderResult = MongoDBServiceImpl.getInstance()
					.realTimeOrderStatisticsByMemberType(realTimeStatistics.getStartDailyDate(), realTimeStatistics.getDate());
			for (MultiResultMapreduceData temp : orderResult) {
				JSONObject jsonObject = JSONObject.parseObject(temp.getValue());
				int accountCount = jsonObject.getIntValue("accountDistinctCount");
				long cardTotal = jsonObject.getLongValue("cardNum");
				if (cardTotal > 0) {
					// 处理MapReduce结果集只有一条查询返回，直接返回结果集问题
					if (accountCount == 0) {
						accountCount = 1;
					}
				}
				if (temp.getId() == 0) {
					// 普通用户
					realTimeStatistics.setNormalPayAccount(accountCount);
					realTimeStatistics.setNormalPayAmount(cardTotal);
					// 处理Mapreduce结果集一条的情况
				} else if (temp.getId() == 1) {
					// 代理充值
					realTimeStatistics.setAgentPayAccount(accountCount);
					realTimeStatistics.setAgentPayAmount(cardTotal);
				}
			}
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	// 设置日均亲友圈、普通开房牌局场次
	public void setUserAveBrandByType(Date date, DailyReportStatistics dailyReportStatistics) {
		try {
			// Date targetDate = DateUtils.addDays(date, -1);
			// int targetDateInt =
			// Integer.valueOf(DateFormatUtils.format(targetDate, "yyyyMMdd"));
			// List<Integer> typeList = new ArrayList<>();
			// typeList.add(0);
			// typeList.add(1);
			// typeList.add(3);
			// typeList.add(5);
			// AggregationOperation match =
			// Aggregation.match(Criteria.where("notes_date").is(targetDateInt).and("type").in(typeList));
			// AggregationOperation group =
			// Aggregation.group("type").count().as("userCount").sum("count").as("brandCount");
			// Aggregation aggregation = Aggregation.newAggregation(match,
			// group);
			// MongoDBService mongoDBService =
			// SpringService.getBean(MongoDBService.class);
			// AggregationResults<HashMap> result =
			// mongoDBService.getMongoTemplate().aggregate(aggregation,
			// "statistics_account_dailybrand",
			// HashMap.class);
			// List<HashMap> list = result.getMappedResults();
			// if (list != null && list.size() > 0) {
			int clubTotalbrand = 0;
			int normalTotalbrand = 0;
			for (int key : newAccountSummaryMap.keySet()) {
				if (key != 5) {
					normalTotalbrand += newAccountSummaryMap.get(key);
				} else {
					clubTotalbrand += newAccountSummaryMap.get(key);
				}
			}
			// for (HashMap hashMap : list) {
			// int id = hashMap.get("_id") == null ? 0 : (int)
			// hashMap.get("_id");
			// int userCount = hashMap.get("userCount") == null ? 0 : (int)
			// hashMap.get("userCount");
			// int brandCount = hashMap.get("brandCount") == null ? 0 : (int)
			// hashMap.get("brandCount");
			// if (id != 5) {
			// normalTotalbrand += brandCount;
			// normalTotalUser += userCount;
			// } else {
			// clubTotalbrand += brandCount;
			// clubTotalUser += userCount;
			// }
			// }
			if (totalClubUser > 0) {
				double clubAveBrand = MathUtil.divide(clubTotalbrand, totalClubUser);
				dailyReportStatistics.setClubAveBrand(clubAveBrand);

			}
			if (totalNormalUser > 0) {
				double normalAveBrand = MathUtil.divide(normalTotalbrand, totalNormalUser);
				dailyReportStatistics.setNormalAveBrand(normalAveBrand);
			}
			// }
		} catch (Exception e) {
			logger.error("everyDayRobotOpenRoom error", e);
		}

	}

	// 按类型设置开房总数
	public void setBrandCountByType(Date date, DailyReportStatistics dailyReportStatistics) {
		try {
			Date targetDate = DateUtils.addDays(date, -1);
			int targetDateInt = Integer.valueOf(DateFormatUtils.format(targetDate, "yyyyMMdd"));
			Query query = new Query();
			query.addCriteria(Criteria.where("notes_date").is(targetDateInt));
			EveryDayAccountModel model = mongoTemplate.findOne(query, EveryDayAccountModel.class);
			if (model != null) {
				dailyReportStatistics.setClubTotal((int) model.getClubOpenRoomCount());
				dailyReportStatistics.setMatchTotal((int) model.getMatchCount());
				dailyReportStatistics.setNormalTotal((int) (model.getReal_open_room() - model.getClubOpenRoomCount()));
				double matchAveBrand = MathUtil.divide(model.getMatchCount(), model.getApplyCount());
				dailyReportStatistics.setMatchAveBrand(matchAveBrand);
			}
			Date zeroDate = MyDateUtil.getZeroDate(targetDate);
			Query query2 = new Query();
			query2.addCriteria(Criteria.where("timeFlag").is(zeroDate));
			CoinPlayerSummaryLogModel coinModel = mongoTemplate.findOne(query2, CoinPlayerSummaryLogModel.class);
			if (coinModel != null) {
				dailyReportStatistics.setCoinTotal(coinModel.getRoundNum());
				double coinAveBrand = MathUtil.divide(coinModel.getRoundNum(), coinModel.getSumNum());
				dailyReportStatistics.setMatchAveBrand(coinAveBrand);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public MongoTemplate getMongoTemplate() {
		return mongoTemplate;
	}

	public void setMongoTemplate(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public static CenterStatisticsService getInstance() {
		return instance;
	}

	public static void setInstance(CenterStatisticsService instance) {
		CenterStatisticsService.instance = instance;
	}

}
