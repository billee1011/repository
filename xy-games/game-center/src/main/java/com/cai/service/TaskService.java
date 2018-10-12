package com.cai.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.cai.common.constant.Symbol;
import com.cai.common.define.EAccountParamType;
import com.cai.common.define.EGameType;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.ELogType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AccountParamModel;
import com.cai.common.domain.AccountWeixinModel;
import com.cai.common.domain.AddCardLog;
import com.cai.common.domain.BrandChildLogModel;
import com.cai.common.domain.BrandDayModel;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.BrandResultModel;
import com.cai.common.domain.CardLogModel;
import com.cai.common.domain.CityLogModel;
import com.cai.common.domain.ClubActivityLogModel;
import com.cai.common.domain.ClubEventLogModel;
import com.cai.common.domain.ClubExclusiveGoldLogModel;
import com.cai.common.domain.ClubLogModel;
import com.cai.common.domain.DayCardResult;
import com.cai.common.domain.DayMatchResult;
import com.cai.common.domain.EveryDayAccountModel;
import com.cai.common.domain.EveryDayClubModel;
import com.cai.common.domain.EveryDayRobotModel;
import com.cai.common.domain.GameLogModel;
import com.cai.common.domain.GameLogModelMoney;
import com.cai.common.domain.GamesAccountModel;
import com.cai.common.domain.GiveCardModel;
import com.cai.common.domain.KeepRateModel;
import com.cai.common.domain.MatchLogModel;
import com.cai.common.domain.MoneyLogModel;
import com.cai.common.domain.NewUserCityReportModel;
import com.cai.common.domain.OneProxyAccountReplaceRoomModel;
import com.cai.common.domain.PlayerLogClientIpModel;
import com.cai.common.domain.PlayerLogNotifyIpModel;
import com.cai.common.domain.PlayerLogServerIpModel;
import com.cai.common.domain.ProxyAccountReplaceRoomModel;
import com.cai.common.domain.ProxyAccountSecondStatModel;
import com.cai.common.domain.ProxyAccountStatModel;
import com.cai.common.domain.ProxyConsumeModel;
import com.cai.common.domain.ProxyGoldLogModel;
import com.cai.common.domain.RecommendActiveModel;
import com.cai.common.domain.ReponseLogModel;
import com.cai.common.domain.RobotModel;
import com.cai.common.domain.ServerErrorLogModel;
import com.cai.common.domain.SubGameOnlineDay;
import com.cai.common.domain.SysGameType;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.SystemLogModel;
import com.cai.common.domain.SystemLogOnlineModel;
import com.cai.common.domain.log.ClubApplyLogModel;
import com.cai.common.domain.log.ClubScoreMsgLogModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.dao.PublicDAO;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.BatchUpdateTarget;
import com.cai.future.runnable.ClearAccountCacheRunnable;
import com.cai.future.runnable.FiveCleanRunnble;
import com.cai.future.runnable.FiveUpgradeGameRunnable;
import com.cai.service.statistics.CenterStatisticsService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import javolution.util.FastMap;

/**
 * 调度
 * 
 * @author run
 *
 */
@Service
public class TaskService {

	private static Logger logger = LoggerFactory.getLogger(TaskService.class);

	@Autowired
	private CenterStatisticsService centerStatisticsService;

	// @Autowired
	// private DailyRechargeStatisticsService dailyRechargeStatisticsService;

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
			// 统计端午节红包活动
			// initRedPacketActiveLog();
			WeiXinProxyConsumeService.getInstance().zeroTask();

			resetNewRecommendReceive();

			// initProxyConsumTongJi();

			// 每日玩家数据统计
			EveryDayAccountModel everyDayAccountModel = everyDayAccount(-1);

			// 留存率
			keepRate();

			// 进入房间方式的统计
			inRoomWayStat();

			// 重置今日玩家属性列表
			resetTodayAccountParam();

			// 二级代理相关统计
			// proxyCreateRoomState();

			// 代理相关统计
			// proxAccountStat(-1);

			// everyDayRobotModel(-1, true);

			// everyDayClubModel(-1, true);

			// 排行榜
			// RankServiceImp.getInstance().doTriggerRank();

			countSubGameNumber();

			// 每日充值分析统计，包括新用户充值行为和总充值行为分析
			// dailyRechargeStatisticsService.call();
			// 金币兑换统计
			centerStatisticsService.coinExchangeStatistics();
			// 统计日报表
			centerStatisticsService.createLastDailyReport(everyDayAccountModel);
			countTVExcluesiveActivity();
		} catch (Exception e) {
			logger.error("error", e);
		}

	}

	/**
	 * TV专享活动的统计
	 */
	public void countTVExcluesiveActivity() {
		try {
			TVExcluesiveService.getInstance().taskJob();
		} catch (Exception e) {
			logger.error("error", e);
		}
	}

	/**
	 * 统计子游戏在线人数
	 */
	public void countSubGameNumber() {
		try {
			Date now = new Date();
			Date targetDate = DateUtils.addDays(now, -1);
			Integer targetDateInt = Integer.valueOf(DateFormatUtils.format(targetDate, "yyyyMMdd"));
			List<SubGameOnlineDay> list = new ArrayList<>();
			Map<Integer, HashSet<Integer>> gameTypeNumberMap = PublicServiceImpl.getInstance().getGameTypeNumberMap();
			for (Entry<Integer, HashSet<Integer>> entry : gameTypeNumberMap.entrySet()) {
				SysGameType gameType = SysGameTypeDict.getInstance().getSysGameType(entry.getKey());
				if (gameType == null) {
					logger.error("gameType is null check", entry.getKey());
					continue;
				}
				SubGameOnlineDay onlineDay = new SubGameOnlineDay();
				onlineDay.setGame_type_index(entry.getKey());
				onlineDay.setGameName(gameType.getDesc());
				onlineDay.setNotes_date(targetDateInt);
				onlineDay.setTotalOnline(entry.getValue().size());
				list.add(onlineDay);

			}
			SpringService.getBean(MongoDBService.class).getMongoTemplate().insertAll(list);

			PublicServiceImpl.getInstance().clearGameTypeNumber();// 清理一下

		} catch (Exception e) {
			logger.error("countSubGameNumber error", e);
		}
	}

	/**
	 * 机器人开房
	 */
	public List<EveryDayRobotModel> everyDayRobotModel(int decDay, boolean isInsert) {
		List<EveryDayRobotModel> list = MongoDBServiceImpl.getInstance().everyDayRobotModel(decDay, isInsert);
		for (EveryDayRobotModel robot : list) {
			Account account = PublicServiceImpl.getInstance().getAccount(robot.getAccount_id());
			if (account == null)
				continue;
			robot.setRecommend_id(account.getAccountModel().getRecommend_id());
		}
		SpringService.getBean(MongoDBService.class).getMongoTemplate().insertAll(list);
		return list;
	}

	/**
	 * , 俱乐部开房
	 */
	public List<EveryDayClubModel> everyDayClubModel(int decDay, boolean isInsert) {
		List<EveryDayClubModel> list = MongoDBServiceImpl.getInstance().everyDayClubModel(decDay, isInsert, 0);
		for (EveryDayClubModel robot : list) {
			Account account = PublicServiceImpl.getInstance().getAccount(robot.getAccount_id());
			if (account == null)
				continue;
			robot.setRecommend_id(account.getAccountModel().getRecommend_id());
		}
		SpringService.getBean(MongoDBService.class).getMongoTemplate().insertAll(list);
		return list;
	}

	/**
	 * 5点
	 */
	public void taskZeroFive() {

		int min = 1;
		int interval = 5;

		GameSchedule.put(new FiveCleanRunnble(-2, ReponseLogModel.class), min += interval, TimeUnit.MINUTES);

		int brandKeep = 7;
		int brandKeepGold = 7;
		FastMap<Integer, SysParamModel> map = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6);
		if (map != null) {
			SysParamModel param = map.get(2237);
			if (param != null && param.getVal2() > 0 && param.getVal2() < 30) {
				brandKeep = param.getVal2();
			}
			if (param != null && param.getVal4() > 0 && param.getVal4() < 200) {
				brandKeepGold = param.getVal4();
			}
		}

		GameSchedule.put(new FiveCleanRunnble(-brandKeep, BrandLogModel.class), min += interval, TimeUnit.MINUTES);

		GameSchedule.put(new FiveCleanRunnble(-brandKeep, BrandChildLogModel.class), min = +interval, TimeUnit.MINUTES);

		GameSchedule.put(new FiveCleanRunnble(-brandKeepGold, GameLogModel.class), min += interval, TimeUnit.MINUTES);

		GameSchedule.put(new FiveCleanRunnble(-20, RobotModel.class), min += interval, TimeUnit.MINUTES);

		GameSchedule.put(new FiveCleanRunnble(-7, SystemLogModel.class), min += interval, TimeUnit.MINUTES);

		GameSchedule.put(new FiveCleanRunnble(-7, ClubLogModel.class), min += interval, TimeUnit.MINUTES);

		GameSchedule.put(new FiveCleanRunnble(-20, BrandResultModel.class), min += interval, TimeUnit.MINUTES);

		GameSchedule.put(new FiveCleanRunnble(-3, GameLogModelMoney.class), min += interval, TimeUnit.MINUTES);

		GameSchedule.put(new FiveCleanRunnble(-7, CardLogModel.class), min += interval, TimeUnit.MINUTES);

		GameSchedule.put(new ClearAccountCacheRunnable(), 0, TimeUnit.MINUTES);

		GameSchedule.put(new FiveUpgradeGameRunnable(), 30, TimeUnit.MINUTES);

		GameSchedule.put(new FiveCleanRunnble(-1, PlayerLogClientIpModel.class), min += interval, TimeUnit.MINUTES);

		GameSchedule.put(new FiveCleanRunnble(-1, PlayerLogNotifyIpModel.class), min += interval, TimeUnit.MINUTES);

		GameSchedule.put(new FiveCleanRunnble(-1, PlayerLogServerIpModel.class), min += interval, TimeUnit.MINUTES);

		GameSchedule.put(new FiveCleanRunnble(-20, SystemLogOnlineModel.class), min += interval, TimeUnit.MINUTES);

		GameSchedule.put(new FiveCleanRunnble(-30, ClubActivityLogModel.class), min += interval, TimeUnit.MINUTES);

		GameSchedule.put(new FiveCleanRunnble(-7, GamesAccountModel.class), min += interval, TimeUnit.MINUTES);
		// 删除所有用户的城市信息上报
		GameSchedule.put(new FiveCleanRunnble(-30, CityLogModel.class), min += interval, TimeUnit.MINUTES);
		// 删除新用户城市信息上报
		GameSchedule.put(new FiveCleanRunnble(-30, NewUserCityReportModel.class), min += interval, TimeUnit.MINUTES);

		GameSchedule.put(new FiveCleanRunnble(-7, ServerErrorLogModel.class), min += interval, TimeUnit.MINUTES);

		// 俱乐部操作事件日志
		GameSchedule.put(new FiveCleanRunnble(-90, ClubEventLogModel.class), min += interval, TimeUnit.MINUTES);

		// 专属豆消耗日志
		GameSchedule.put(new FiveCleanRunnble(-14, ClubExclusiveGoldLogModel.class), min += interval, TimeUnit.MINUTES);

		// 俱乐部疲劳值操作日志
		GameSchedule.put(new FiveCleanRunnble(-14, ClubScoreMsgLogModel.class), min += interval, TimeUnit.MINUTES);

		// 俱乐部申请操作日志
		GameSchedule.put(new FiveCleanRunnble(-14, ClubApplyLogModel.class), min += interval, TimeUnit.MINUTES);

		// ----后续的mongodb的表需要删除的都参考 PlayerLogNotifyIpModel 的按时间索引来删除吧
		// 。。。。。不要再以现在的这种方式搞了。
		// ----------------@Indexed(direction=IndexDirection.DESCENDING,expireAfterSeconds=60*60)

		PerformanceTimer timer = new PerformanceTimer();
		Date d2 = MyDateUtil.getZeroDate(MyDateUtil.getNow());
		Date d1 = DateUtils.addDays(d2, -30);
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);

		// 删除过期数据
		Query query = new Query();
		query.addCriteria(Criteria.where("create_time").lt(d1));
		query.addCriteria(Criteria.where("orderStatus").is(1));
		mongoDBService.getMongoTemplate().remove(query, AddCardLog.class);

		logger.info("FiveCleanRunnble 5点删除过期数据," + AddCardLog.class + "," + timer.getStr());
	}

	public void taskTest() {
		logger.warn("taskTest........");

	}

	public void countProxySell() {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		List<ProxyGoldLogModel> logList = mongoDBService.getMongoTemplate().findAll(ProxyGoldLogModel.class);
		Map<Long, Long> proxyMap = new HashMap<Long, Long>();
		for (ProxyGoldLogModel proxyGold : logList) {
			if (proxyGold.getTarget_proxy_account() == 1)
				continue;
			Long number = proxyMap.get(proxyGold.getAccount_id());
			if (number == null) {
				proxyMap.put(proxyGold.getAccount_id(), proxyGold.getGive_num());
			} else {
				proxyMap.put(proxyGold.getAccount_id(), proxyGold.getGive_num() + number);
			}
		}
		List<Map.Entry<Long, Long>> sortlist = new ArrayList<Map.Entry<Long, Long>>(proxyMap.entrySet());
		Collections.sort(sortlist, new Comparator<Map.Entry<Long, Long>>() {

			@Override
			public int compare(Entry<Long, Long> o1, Entry<Long, Long> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}

		});
		for (Map.Entry<Long, Long> i : sortlist) {
			System.out.println("帐号ID" + i.getKey() + "=转卡总数" + i.getValue());
		}

	}

	/**
	 * 统计金币
	 */
	private void countMoneyAdd() {
		try {
			List<MoneyLogModel> batchList = new ArrayList<MoneyLogModel>();

			Date now = new Date();
			Date targetDate = DateUtils.addDays(now, -1);
			Integer targetDateInt = Integer.valueOf(DateFormatUtils.format(targetDate, "yyyyMMdd"));

			Map<EAccountParamType, List<AccountParamModel>> sortMap = new HashMap<EAccountParamType, List<AccountParamModel>>();
			for (EAccountParamType type : EAccountParamType.values()) {
				if (type.getType() == 1) {
					sortMap.put(type, new ArrayList<AccountParamModel>());
				}
			}
			for (Account account : PublicServiceImpl.getInstance().getAccountIdMap().values()) {
				for (AccountParamModel m : account.getAccountParamModelMap().values()) {
					if (m.getVal1() == null || m.getVal1() <= 0)
						continue;
					EAccountParamType eAccountParamType = EAccountParamType.getEMsgType(m.getType());
					if (eAccountParamType != null && eAccountParamType.getType() == 1) {
						sortMap.get(eAccountParamType).add(m);
					}
				}
			}

			for (Entry<EAccountParamType, List<AccountParamModel>> entry : sortMap.entrySet()) {
				EAccountParamType eAccountParamType = entry.getKey();
				Collections.sort(entry.getValue(), new Comparator<AccountParamModel>() {
					@Override
					public int compare(AccountParamModel o1, AccountParamModel o2) {
						return o2.getVal1().compareTo(o1.getVal1());
					}
				});

				MoneyLogModel logModel = null;
				int i = 0;
				for (AccountParamModel param : entry.getValue()) {
					logModel = new MoneyLogModel();
					logModel.setAccountId(param.getAccount_id());
					Account account = PublicServiceImpl.getInstance().getAccount(param.getAccount_id());
					AccountWeixinModel weixinModel = account.getAccountWeixinModel();
					if (null == weixinModel) {
						logModel.setName("--");
						logger.error("AccountWeixinModel 为空！！！accountid:{}", param.getAccount_id());
					} else {
						logModel.setName(weixinModel.getNickname());
					}

					logModel.setNotes_date(targetDateInt);
					logModel.setToday_count(param.getVal1());
					logModel.setDesc(eAccountParamType.getDesc());
					logModel.setTypeID(eAccountParamType.getId());
					batchList.add(logModel);
					i++;
					if (i >= 100) {
						break;
					}
				}
			}
			if (batchList.size() > 0) {
				MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
				mongoDBService.getMongoTemplate().insertAll(batchList);
			}

		} catch (Exception e) {
			logger.error("金币统计error", e);
		}
	}

	/**
	 * 重置今日玩家属性列表
	 */
	public void resetTodayAccountParam() {
		try {
			PerformanceTimer timer = new PerformanceTimer();

			// TODO 如果要统计数据，在这里处理,取出添加铜钱的前50名的数据
			countMoneyAdd();

			List<AccountParamModel> updateList = new ArrayList<AccountParamModel>();
			// 内存中的
			for (Account account : PublicServiceImpl.getInstance().getAccountIdMap().values()) {
				for (AccountParamModel m : account.getAccountParamModelMap().values()) {
					EAccountParamType eAccountParamType = EAccountParamType.getEMsgType(m.getType());
					boolean flag = false;
					if (eAccountParamType != null) {
						if (eAccountParamType.getType() == 1) {
							if (m.getVal1() != 0) {
								m.setVal1(0);
								flag = true;
							}
							if (StringUtils.isNotEmpty(m.getStr1())) {
								m.setStr1("");
								flag = true;
							}
							if (m.getLong1() != 0) {
								m.setLong1(0L);
								flag = true;
							}
							if (m.getDate1() != null) {
								m.setDate1(null);
								flag = true;
							}
							if (flag) {
								updateList.add(m);// m.setNeedDB(true); 先不标记个数
							}
						}
					}
				}
				// 重置一天打的总局数
				account.setDay_all_round(new AtomicInteger(0));
				// 重置推广数据
				account.getRecommendRelativeModel().initModel();
			}

			List<AccountParamModel> tmp = Lists.newArrayListWithCapacity(10000);

			int range = 10000;
			for (int i = 1; i <= updateList.size(); i++) {
				tmp.add(updateList.get(i - 1));
				if (i % range == 0) {
					GameSchedule.put(new BatchUpdateTarget(tmp), (i / range) * 30, TimeUnit.SECONDS);
					tmp.clear();
				}
			}
			if (tmp.size() > 0) {
				GameSchedule.put(new BatchUpdateTarget(tmp), 10, TimeUnit.SECONDS);
			}

			// 修改数据库中的---
			// SpringService.getBean(PublicService.class).getPublicDAO().resetTodayAccountParam();

			logger.info("重置账号属性今日数据:" + timer.getStr());
		} catch (Exception e) {
			logger.error("error", e);
		}
	}

	public List<DayMatchResult> everyDayMatchCount(int decDay) {

		try {
			Date now = new Date();
			Date targetDate = DateUtils.addDays(now, -1);
			Aggregation aggregation = Aggregation.newAggregation(
					Aggregation.match(Criteria.where("startTime").is(MyDateUtil.getZeroDate(targetDate))),
					Aggregation.group().sum("costGold").as("costGold").sum("applyCount").as("applyCount").sum("matchSuccess").as("matchSuccess")
							.sum("prizeGold").as("prizeGold"));
			AggregationResults<DayMatchResult> models = SpringService.getBean(MongoDBService.class).getMongoTemplate().aggregate(aggregation,
					MatchLogModel.class, DayMatchResult.class);
			return models.getMappedResults();
		} catch (Exception e) {
			logger.error("everyDayMatchCount error", e);
		}
		return null;

	}

	/**
	 * 每日玩家数据统计
	 * 
	 * @param decDay
	 *            指定前多少天
	 */
	@SuppressWarnings("unchecked")
	public EveryDayAccountModel everyDayAccount(int decDay) {
		try {
			Date now = new Date();
			Date targetDate = DateUtils.addDays(now, decDay);
			Integer targetDateInt = Integer.valueOf(DateFormatUtils.format(targetDate, "yyyyMMdd"));

			PublicService publicService = SpringService.getBean(PublicService.class);

			Query query = new Query();
			query.addCriteria(Criteria.where("notes_date").is(targetDateInt));
			EveryDayAccountModel tmpEveryDayAccountModel = SpringService.getBean(MongoDBService.class).getMongoTemplate().findOne(query,
					EveryDayAccountModel.class);

			Map<String, BrandDayModel> brandDayModelMap = new HashMap<String, BrandDayModel>();
			if (tmpEveryDayAccountModel == null) {

				EveryDayAccountModel everyDayAccountModel = new EveryDayAccountModel();
				try {
					Aggregation aggregation = Aggregation.newAggregation(
							Aggregation.match(Criteria.where("log_type").is(ELogType.addGold.getId()).and("create_time")
									.gte(MyDateUtil.getZeroDate(targetDate)).lte(MyDateUtil.getTomorrowZeroDate(targetDate))),
							Aggregation.group("v2").sum("v1").as("total").last("v2").as("v2"));
					AggregationResults<DayCardResult> models = SpringService.getBean(MongoDBService.class).getMongoTemplate().aggregate(aggregation,
							GameLogModel.class, DayCardResult.class);
					List<DayCardResult> list = models.getMappedResults();
					for (DayCardResult result : list) {
						EGoldOperateType type = EGoldOperateType.ELogType(result.getV2());
						if (type == null) {
							logger.error("addGold v2类型记录错误v2=" + result.getV2());
							continue;
						}
						everyDayAccountModel.setGoldValue(type, result.getTotal());
					}
				} catch (Exception e) {
					logger.error("DayCardResult 统计异常", e);
				}

				String mj = "";
				int mjTotal = 0;
				int mjSingleTotal = 0;
				// 牌局统计
				try {
					List<Integer> createTypeList = new ArrayList<Integer>();
					createTypeList.add(0);
					createTypeList.add(1);
					createTypeList.add(2);
					createTypeList.add(3);
					createTypeList.add(5);
					Aggregation aggregation = Aggregation.newAggregation(
							Aggregation.match(Criteria.where("log_type").is(ELogType.parentBrand.getId()).and("create_time")
									.gte(MyDateUtil.getZeroDate(targetDate)).lte(MyDateUtil.getTomorrowZeroDate(targetDate)).and("createType")
									.in(createTypeList)),
							Aggregation.group("v1", "v2").count().as("total").last("v1").as("v1").last("v2").as("v2"));
					AggregationResults<DayCardResult> models = SpringService.getBean(MongoDBService.class).getMongoTemplate().aggregate(aggregation,
							BrandLogModel.class, DayCardResult.class);
					List<DayCardResult> list = models.getMappedResults();

					List<DayCardResult> sortList = new ArrayList<>(list);
					Collections.sort(sortList, new Comparator<DayCardResult>() {

						@Override
						public int compare(DayCardResult o1, DayCardResult o2) {
							if (o1.getV2() == o2.getV2()) {
								return o1.getV1().compareTo(o2.getV1());
							}
							return o1.getV2().compareTo(o2.getV2());
						}
					});

					StringBuffer buffer = new StringBuffer();
					HashMap<Integer, Long> goldMap = getGoldCount(targetDate);
					for (DayCardResult result : sortList) {
						String mjName = SysGameTypeDict.getInstance().getMJname(result.getV2());
						buffer.append("  " + mjName + "(" + result.getV1() + "局)" + "数量:" + result.getTotal() + "|");

						if (brandDayModelMap.get(mjName) == null) {
							BrandDayModel brandDayModel = new BrandDayModel();
							brandDayModelMap.put(mjName, brandDayModel);
						}
						BrandDayModel brandDayModel = brandDayModelMap.get(mjName);
						brandDayModel.setNotes_date(targetDateInt);
						brandDayModel.setMjName(mjName);
						brandDayModel.setMjtype(result.getV2());
						brandDayModel.setGold_count(goldMap.containsKey(result.getV2()) ? goldMap.get(result.getV2()) : 0);
						if (result.getV1() == 4) {
							brandDayModel.setFour(result.getTotal());
						} else if (result.getV1() == 8) {
							brandDayModel.setEight(result.getTotal());
						} else if (result.getV1() == 16) {
							brandDayModel.setSixteen(result.getTotal());
						} else if (result.getV1() == 10) {
							brandDayModel.setTen(result.getTotal());
						} else if (result.getV1() == 20) {
							brandDayModel.setTwenty(result.getTotal());
						} else if (result.getV1() == 30) {
							brandDayModel.setThirty(result.getTotal());
						} else if (result.getV1() == 6) {
							brandDayModel.setSix(result.getTotal());
						} else if (result.getV1() == 9) {
							brandDayModel.setNine(result.getTotal());
						} else if (result.getV1() == 15) {
							brandDayModel.setFifteen(result.getTotal());
						} else if (result.getV1() == 2) {
							brandDayModel.setTwo(result.getTotal());
						} else if (result.getV1() == 3) {
							brandDayModel.setThree(result.getTotal());
						} else if (result.getV1() == 5) {
							brandDayModel.setFive(result.getTotal());
						} else if (result.getV1() == 12) {
							brandDayModel.setTwelve(result.getTotal());
						} else if (result.getV1() == 18) {
							brandDayModel.setEighteenth(result.getTotal());
						} else {
							brandDayModel.setTwenty_four(result.getTotal());
						}
						mjTotal += result.getTotal();
						mjSingleTotal += result.getV1() * result.getTotal();
					}
					mj = buffer.toString();
					for (BrandDayModel brandDayModel : brandDayModelMap.values()) {
						brandDayModel.setAllTotal(mjTotal);
					}
					MongoDBServiceImpl.getInstance().addCollection(brandDayModelMap.values());

				} catch (Exception e) {
					logger.error("DayCardResult 牌局统计异常", e);
				}

				// 最高在线
				long highCount = 0;
				try {
					Criteria crieria = new Criteria();
					Query q = Query.query(crieria);
					crieria.and("log_type").is("accountOnline").and("create_time").gte(MyDateUtil.getZeroDate(targetDate))
							.lte(MyDateUtil.getTomorrowZeroDate(targetDate));
					q.with(new Sort(Direction.DESC, "v1")).limit(1);
					SystemLogModel logmodel = SpringService.getBean(MongoDBService.class).getMongoTemplate().findOne(q, SystemLogModel.class);
					if (logmodel != null) {
						highCount = logmodel.getV1();
					}
				} catch (Exception e) {
					logger.error("最高在线 牌局统计异常", e);
				}

				int num = publicService.getPublicDAO().getAccountNum();
				int numCreate = publicService.getPublicDAO().getAccountCreateNumByTime(MyDateUtil.getZeroDate(targetDate),
						MyDateUtil.getTomorrowZeroDate(targetDate));
				int numActive = publicService.getPublicDAO().getAccountActiveOnlineNum(MyDateUtil.getZeroDate(targetDate), now);

				everyDayAccountModel.setNotes_date(targetDateInt);
				everyDayAccountModel.setAccount_count(num);
				everyDayAccountModel.setRegister(numCreate);
				everyDayAccountModel.setHight_online((int) highCount);
				everyDayAccountModel.setActive_account_num(numActive);
				everyDayAccountModel.setMsg(emptyGameStr(brandDayModelMap));
				everyDayAccountModel.setMjDetail(mj);
				everyDayAccountModel.setMjTotal(mjTotal);
				everyDayAccountModel.setMjSingleTotal(mjSingleTotal);
				everyDayAccountModel.setCreate_time(now);
				everyDayAccountModel.setRobotCount(MongoDBServiceImpl.getInstance().everyDayRobotOpenRoom(decDay));
				Map<Integer, Long> clubMap = MongoDBServiceImpl.getInstance().everyDayClubOpenRoom(decDay);
				everyDayAccountModel.setClubCount(clubMap.get(1) == null ? 0 : clubMap.get(1));
				everyDayAccountModel.setClubOpenRoomCount(clubMap.get(2) == null ? 0 : clubMap.get(2));
				everyDayAccountModel.setNewClubNum(publicService.getPublicDAO().getNewClubNum(targetDate));
				everyDayAccountModel.setNewClubPersonNum(publicService.getPublicDAO().getNewClubPersonNum(targetDate));
				try {
					List<HashMap> goldAndMoneyMap = publicService.getPublicDAO().getGoldAndMoneyRemain();
					for (HashMap map : goldAndMoneyMap) {
						everyDayAccountModel.setTotalGold(((BigDecimal) map.get("totalGold")).longValue());
						everyDayAccountModel.setTotalMoney(((BigDecimal) map.get("totalMoney")).longValue());
						break;
					}
				} catch (Exception e) {
					logger.error("getGoldAndMoneyRemain error !", e);
				}

				List<DayMatchResult> matchList = everyDayMatchCount(-1);
				if (matchList != null && matchList.size() > 0) {
					DayMatchResult match = matchList.get(0);
					everyDayAccountModel.setMatchConsumeGold(match.getCostGold());
					everyDayAccountModel.setMatchCount(match.getMatchSuccess());
					everyDayAccountModel.setMatchReceiveGold(match.getCostGold() - match.getPrizeGold());
					everyDayAccountModel.setApplyCount(match.getApplyCount());

				}
				SpringService.getBean(MongoDBService.class).getMongoTemplate().save(everyDayAccountModel);
				return everyDayAccountModel;
			}

		} catch (Exception e) {
			logger.error("error", e);
		}
		return null;
	}

	// 统计下没人玩的游戏
	public String emptyGameStr(Map<String, BrandDayModel> brandDayModelMap) {
		try {
			ConcurrentHashMap<Integer, SysGameType> gameTypeMap = SysGameTypeDict.getInstance().getSysGameTypeDictionary();

			SysParamModel paramModel = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(EGameType.DT.getId()).get(2243);
			Set<Integer> offLineGames = Sets.newHashSet();
			if (null != paramModel) {
				if (paramModel.getVal1().intValue() == 1) {
					try {
						offLineGames.addAll(com.cai.common.util.StringUtil.toIntSet(paramModel.getStr1(), Symbol.COMMA));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			if (offLineGames.isEmpty())
				return "每日统计";// 碰胡就不需要这个了

			StringBuilder emptyGameStr = new StringBuilder();
			emptyGameStr.append("没人玩的游戏--");
			for (Entry<Integer, SysGameType> entry : gameTypeMap.entrySet()) {// 所有的游戏
				// Integer key = entry.getKey();
				SysGameType gameType = entry.getValue();

				if (gameType.getGameID() < 10)
					continue;
				if (offLineGames.contains(gameType.getGameID()))
					continue;// 下线的不管

				if (brandDayModelMap.get(gameType.getDesc()) != null)
					continue;// 已经统计的不管

				emptyGameStr.append(gameType.getDesc()).append(":");

			}
			return emptyGameStr.toString();
		} catch (Exception e) {

		}
		return "";
	}

	/**
	 * 留存率计算
	 */
	public void keepRate() {
		try {

			PublicDAO publicDAO = SpringService.getBean(PublicService.class).getPublicDAO();
			// 新的统计
			// 1.找出所有要修改的记录
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			Date now = new Date();
			Date yesterdayZero = MyDateUtil.getZeroDate(DateUtils.addDays(now, -1));

			// 上一天的
			Date targetDate = DateUtils.addDays(now, -1);
			Integer targetDateInt = Integer.valueOf(DateFormatUtils.format(targetDate, "yyyyMMdd"));
			Query query = new Query();
			query.addCriteria(Criteria.where("notes_date").is(targetDateInt));
			KeepRateModel keepRateModel = mongoDBService.getMongoTemplate().findOne(query, KeepRateModel.class);
			Integer register = publicDAO.getAccountCreateNumByTime(MyDateUtil.getZeroDate(targetDate), MyDateUtil.getTomorrowZeroDate(targetDate));
			if (keepRateModel == null) {
				keepRateModel = new KeepRateModel();
				keepRateModel.setNotes_date(targetDateInt);
				keepRateModel.setRegister(register);
				mongoDBService.getMongoTemplate().save(keepRateModel);
			} else {
				keepRateModel.setRegister(register);
				Update update = new Update();
				update.set("register", keepRateModel.getRegister());
				mongoDBService.getMongoTemplate().updateFirst(query, update, KeepRateModel.class);
			}

			// 处理以前的数据 1234567
			// =================day1===================
			targetDate = DateUtils.addDays(now, -2);
			targetDateInt = Integer.valueOf(DateFormatUtils.format(targetDate, "yyyyMMdd"));
			query = new Query();
			query.addCriteria(Criteria.where("notes_date").is(targetDateInt));
			keepRateModel = mongoDBService.getMongoTemplate().findOne(query, KeepRateModel.class);
			register = publicDAO.getAccountCreateNumByTime(MyDateUtil.getZeroDate(targetDate), MyDateUtil.getTomorrowZeroDate(targetDate));
			if (keepRateModel == null) {
				keepRateModel = new KeepRateModel();
				keepRateModel.setNotes_date(targetDateInt);
				keepRateModel.setRegister(register);
				mongoDBService.getMongoTemplate().save(keepRateModel);
			} else {
				keepRateModel.setRegister(register);
			}
			int day1 = publicDAO.getAccountActivieByCreateTime(MyDateUtil.getZeroDate(targetDate), MyDateUtil.getTomorrowZeroDate(targetDate),
					yesterdayZero);
			keepRateModel.setDay1(day1);
			Update update = new Update();
			update.set("day1", keepRateModel.getDay1());
			update.set("register", keepRateModel.getRegister());
			mongoDBService.getMongoTemplate().updateFirst(query, update, KeepRateModel.class);

			// =================day2===================
			targetDate = DateUtils.addDays(now, -3);
			targetDateInt = Integer.valueOf(DateFormatUtils.format(targetDate, "yyyyMMdd"));
			query = new Query();
			query.addCriteria(Criteria.where("notes_date").is(targetDateInt));
			keepRateModel = mongoDBService.getMongoTemplate().findOne(query, KeepRateModel.class);
			register = publicDAO.getAccountCreateNumByTime(MyDateUtil.getZeroDate(targetDate), MyDateUtil.getTomorrowZeroDate(targetDate));
			if (keepRateModel == null) {
				keepRateModel = new KeepRateModel();
				keepRateModel.setNotes_date(targetDateInt);
				keepRateModel.setRegister(register);
				mongoDBService.getMongoTemplate().save(keepRateModel);
			} else {
				keepRateModel.setRegister(register);
			}
			int day2 = publicDAO.getAccountActivieByCreateTime(MyDateUtil.getZeroDate(targetDate), MyDateUtil.getTomorrowZeroDate(targetDate),
					yesterdayZero);
			keepRateModel.setDay2(day2);
			update = new Update();
			update.set("day2", keepRateModel.getDay2());
			update.set("register", keepRateModel.getRegister());
			mongoDBService.getMongoTemplate().updateFirst(query, update, KeepRateModel.class);

			// =================day3===================
			targetDate = DateUtils.addDays(now, -4);
			targetDateInt = Integer.valueOf(DateFormatUtils.format(targetDate, "yyyyMMdd"));
			query = new Query();
			query.addCriteria(Criteria.where("notes_date").is(targetDateInt));
			keepRateModel = mongoDBService.getMongoTemplate().findOne(query, KeepRateModel.class);
			register = publicDAO.getAccountCreateNumByTime(MyDateUtil.getZeroDate(targetDate), MyDateUtil.getTomorrowZeroDate(targetDate));
			if (keepRateModel == null) {
				keepRateModel = new KeepRateModel();
				keepRateModel.setNotes_date(targetDateInt);
				keepRateModel.setRegister(register);
				mongoDBService.getMongoTemplate().save(keepRateModel);
			} else {
				keepRateModel.setRegister(register);
			}
			int day3 = publicDAO.getAccountActivieByCreateTime(MyDateUtil.getZeroDate(targetDate), MyDateUtil.getTomorrowZeroDate(targetDate),
					yesterdayZero);
			keepRateModel.setDay3(day3);
			update = new Update();
			update.set("day3", keepRateModel.getDay3());
			update.set("register", keepRateModel.getRegister());
			mongoDBService.getMongoTemplate().updateFirst(query, update, KeepRateModel.class);

			// =================day4===================
			targetDate = DateUtils.addDays(now, -5);
			targetDateInt = Integer.valueOf(DateFormatUtils.format(targetDate, "yyyyMMdd"));
			query = new Query();
			query.addCriteria(Criteria.where("notes_date").is(targetDateInt));
			keepRateModel = mongoDBService.getMongoTemplate().findOne(query, KeepRateModel.class);
			register = publicDAO.getAccountCreateNumByTime(MyDateUtil.getZeroDate(targetDate), MyDateUtil.getTomorrowZeroDate(targetDate));
			if (keepRateModel == null) {
				keepRateModel = new KeepRateModel();
				keepRateModel.setNotes_date(targetDateInt);
				keepRateModel.setRegister(register);
				mongoDBService.getMongoTemplate().save(keepRateModel);
			} else {
				keepRateModel.setRegister(register);
			}
			int day4 = publicDAO.getAccountActivieByCreateTime(MyDateUtil.getZeroDate(targetDate), MyDateUtil.getTomorrowZeroDate(targetDate),
					yesterdayZero);
			keepRateModel.setDay4(day4);
			update = new Update();
			update.set("day4", keepRateModel.getDay4());
			update.set("register", keepRateModel.getRegister());
			mongoDBService.getMongoTemplate().updateFirst(query, update, KeepRateModel.class);

			// =================day5===================
			targetDate = DateUtils.addDays(now, -6);
			targetDateInt = Integer.valueOf(DateFormatUtils.format(targetDate, "yyyyMMdd"));
			query = new Query();
			query.addCriteria(Criteria.where("notes_date").is(targetDateInt));
			keepRateModel = mongoDBService.getMongoTemplate().findOne(query, KeepRateModel.class);
			register = publicDAO.getAccountCreateNumByTime(MyDateUtil.getZeroDate(targetDate), MyDateUtil.getTomorrowZeroDate(targetDate));
			if (keepRateModel == null) {
				keepRateModel = new KeepRateModel();
				keepRateModel.setNotes_date(targetDateInt);
				keepRateModel.setRegister(register);
				mongoDBService.getMongoTemplate().save(keepRateModel);
			} else {
				keepRateModel.setRegister(register);
			}
			int day5 = publicDAO.getAccountActivieByCreateTime(MyDateUtil.getZeroDate(targetDate), MyDateUtil.getTomorrowZeroDate(targetDate),
					yesterdayZero);
			keepRateModel.setDay5(day5);
			update = new Update();
			update.set("day5", keepRateModel.getDay5());
			update.set("register", keepRateModel.getRegister());
			mongoDBService.getMongoTemplate().updateFirst(query, update, KeepRateModel.class);

			// =================day6===================
			targetDate = DateUtils.addDays(now, -7);
			targetDateInt = Integer.valueOf(DateFormatUtils.format(targetDate, "yyyyMMdd"));
			query = new Query();
			query.addCriteria(Criteria.where("notes_date").is(targetDateInt));
			keepRateModel = mongoDBService.getMongoTemplate().findOne(query, KeepRateModel.class);
			register = publicDAO.getAccountCreateNumByTime(MyDateUtil.getZeroDate(targetDate), MyDateUtil.getTomorrowZeroDate(targetDate));
			if (keepRateModel == null) {
				keepRateModel = new KeepRateModel();
				keepRateModel.setNotes_date(targetDateInt);
				keepRateModel.setRegister(register);
				mongoDBService.getMongoTemplate().save(keepRateModel);
			} else {
				keepRateModel.setRegister(register);
			}
			int day6 = publicDAO.getAccountActivieByCreateTime(MyDateUtil.getZeroDate(targetDate), MyDateUtil.getTomorrowZeroDate(targetDate),
					yesterdayZero);
			keepRateModel.setDay6(day6);
			update = new Update();
			update.set("day6", keepRateModel.getDay6());
			update.set("register", keepRateModel.getRegister());
			mongoDBService.getMongoTemplate().updateFirst(query, update, KeepRateModel.class);

			// =================day7===================
			targetDate = DateUtils.addDays(now, -8);
			targetDateInt = Integer.valueOf(DateFormatUtils.format(targetDate, "yyyyMMdd"));
			query = new Query();
			query.addCriteria(Criteria.where("notes_date").is(targetDateInt));
			keepRateModel = mongoDBService.getMongoTemplate().findOne(query, KeepRateModel.class);
			register = publicDAO.getAccountCreateNumByTime(MyDateUtil.getZeroDate(targetDate), MyDateUtil.getTomorrowZeroDate(targetDate));
			if (keepRateModel == null) {
				keepRateModel = new KeepRateModel();
				keepRateModel.setNotes_date(targetDateInt);
				keepRateModel.setRegister(register);
				mongoDBService.getMongoTemplate().save(keepRateModel);
			} else {
				keepRateModel.setRegister(register);
			}
			int day7 = publicDAO.getAccountActivieByCreateTime(MyDateUtil.getZeroDate(targetDate), MyDateUtil.getTomorrowZeroDate(targetDate),
					yesterdayZero);
			keepRateModel.setDay7(day7);
			update = new Update();
			update.set("day7", keepRateModel.getDay7());
			update.set("register", keepRateModel.getRegister());
			mongoDBService.getMongoTemplate().updateFirst(query, update, KeepRateModel.class);

		} catch (Exception e) {
			logger.error("error", e);
		}
	}

	/**
	 * 进入房间方式的统计
	 */
	private void inRoomWayStat() {
		try {
			PerformanceTimer timer = new PerformanceTimer();
			Date d2 = MyDateUtil.getZeroDate(MyDateUtil.getNow());
			Date d1 = DateUtils.addDays(d2, -1);
			//
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			Query query = new Query();
			// query.addCriteria(Criteria.where("log_type").is(ELogType.inRoomWay.getId()));
			// query.addCriteria(Criteria.where("create_time").gte(d1).lt(d2));
			// query.addCriteria(Criteria.where("v1").is(1L));
			// long long1 = mongoDBService.getMongoTemplate().count(query,
			// SystemLogModel.class);
			//
			// query = new Query();
			// query.addCriteria(Criteria.where("log_type").is(ELogType.inRoomWay.getId()));
			// query.addCriteria(Criteria.where("create_time").gte(d1).lt(d2));
			// query.addCriteria(Criteria.where("v1").is(2L));
			// long long2 = mongoDBService.getMongoTemplate().count(query,
			// SystemLogModel.class);
			//
			// MongoDBServiceImpl.getInstance().systemLog(ELogType.inRoomWayStat,
			// "进入房间方式统计:普通=" + long1 + ",分享链接进入的:" + long2, long1, long2,
			// ESysLogLevelType.NONE);
			//
			// // 删除过期数据
			query = new Query();
			query.addCriteria(Criteria.where("log_type").is(ELogType.inRoomWay.getId()));
			query.addCriteria(Criteria.where("create_time").lt(d2));
			mongoDBService.getMongoTemplate().remove(query, SystemLogModel.class);

			logger.info("进入房间方式的统计" + timer.getStr());
			//
		} catch (Exception e) {
			logger.error("error", e);
		}

	}

	/**
	 * 二级代理相关统计
	 */
	private void proxyCreateRoomState() {
		PerformanceTimer timer = new PerformanceTimer();
		try {
			List<ProxyAccountStatModel> proxyAccountStatModelList = Lists.newArrayList();
			// 找出所有二级代理的代理玩家id
			List<HashMap> list1 = SpringService.getBean(PublicService.class).getPublicDAO().getProxyAccountByProxy();
			for (HashMap m : list1) {
				ProxyAccountStatModel pstatModel = new ProxyAccountStatModel();
				pstatModel.setAccount_id((long) m.get("account_id"));
				proxyAccountStatModelList.add(pstatModel);
				// System.out.println(m.get("c"));
			}

			// 查看有二级代理的玩家的所有id
			for (ProxyAccountStatModel model : proxyAccountStatModelList) {
				List<Long> accountIdList = SpringService.getBean(PublicService.class).getPublicDAO()
						.getProxyAccountByProxyInfo(model.getAccount_id());
				List<ProxyAccountSecondStatModel> proxyAccountSecondStatModelList = Lists.newArrayList();
				for (Long id : accountIdList) {
					ProxyAccountSecondStatModel proxyAccountSecondStatModel = new ProxyAccountSecondStatModel();
					proxyAccountSecondStatModel.setAccount_id(id);
					proxyAccountSecondStatModel.setHistory_gold(0);
					proxyAccountSecondStatModelList.add(proxyAccountSecondStatModel);
				}
				model.setProxyAccountSecondStatModelList(proxyAccountSecondStatModelList);
			}

			// 根据上面的id,统计出他们当日的充值与消耗
			for (ProxyAccountStatModel model : proxyAccountStatModelList) {
				List<ProxyAccountSecondStatModel> proxyAccountSecondStatModelList = model.getProxyAccountSecondStatModelList();
				for (ProxyAccountSecondStatModel smodel : proxyAccountSecondStatModelList) {
					// smodel.setGold_count(gold_count);
					Date targetDate = new Date();
					// 一个玩家一天没有多少条的，直接查出这个玩家的所有
					Criteria crieria = new Criteria();
					Query q = Query.query(crieria);
					crieria.and("log_type").is("addGold").and("account_id").is(smodel.getAccount_id()).and("create_time")
							.gte(MyDateUtil.getZeroDate(targetDate)).lte(MyDateUtil.getTomorrowZeroDate(targetDate));
					List<GameLogModel> logmodelList = SpringService.getBean(MongoDBService.class).getMongoTemplate().find(q, GameLogModel.class);
					for (GameLogModel m : logmodelList) {
						if (m.getV1() > 0 && (m.getV2() == 11L || m.getV2() == 13L || m.getV2() == 14L)) {
							smodel.setGold_count(smodel.getGold_count() + m.getV1());
						}
					}
					model.setGive_gold_num(model.getGive_gold_num() + smodel.getGold_count());
				}
			}

			// 打印
			StringBuilder buf = new StringBuilder();
			buf.append("代理相关统计:\n");
			for (ProxyAccountStatModel model : proxyAccountStatModelList) {
				buf.append("代理id:" + model.getAccount_id()).append(",历史推荐代理人数：" + model.getProxyAccountSecondStatModelList().size()).append("(");
				for (ProxyAccountSecondStatModel m : model.getProxyAccountSecondStatModelList()) {
					buf.append(m.getAccount_id()).append(",");
				}
				buf.append(")");

				buf.append(",当日二级代理充值数:" + model.getGive_gold_num()).append("(详情:");
				for (ProxyAccountSecondStatModel m : model.getProxyAccountSecondStatModelList()) {
					buf.append(m.getAccount_id() + "=" + m.getGold_count() + ",");
				}
				buf.append(")");

				buf.append("\n");

			}
			System.out.println(buf.toString());

			MongoDBServiceImpl.getInstance().systemLog(ELogType.secondProxyStat, buf.toString(), null, null, ESysLogLevelType.NONE);

		} catch (Exception e) {
			logger.error("error", e);
		}
		logger.error("proxyCreateRoomState cost time " + timer.duration());

	}

	/**
	 * 代理相关统计
	 */
	public void proxAccountStat(int day) {
		PerformanceTimer timer = new PerformanceTimer();
		try {
			// 日期
			Date now = new Date();
			Date targetDate = DateUtils.addDays(now, day);
			int targetDateInt = Integer.valueOf(DateFormatUtils.format(targetDate, "yyyyMMdd"));
			// System.out.println(targetDateInt);

			// ==========每个代理当天的代开房消耗=============
			List<ProxyAccountReplaceRoomModel> proxyAccountReplaceRoomModelList = Lists.newArrayList();
			Map<Long, ProxyAccountReplaceRoomModel> proxyAccountReplaceRoomModelMap = Maps.newHashMap();// 为下面索引
			// 所有代理
			List<AccountModel> accountModelList = SpringService.getBean(PublicService.class).getPublicDAO().getProxyAccountList();
			for (AccountModel model : accountModelList) {

				ProxyAccountReplaceRoomModel pmodel = new ProxyAccountReplaceRoomModel();
				pmodel.setAccount_id(model.getAccount_id());
				pmodel.setNotes_date(targetDateInt);
				pmodel.setToday_consume(0);
				pmodel.setInvait_account_id(model.getRecommend_id());
				proxyAccountReplaceRoomModelList.add(pmodel);

				// 根据mongodb查询,每个代理代开房每天的真实消耗
				try {
					Aggregation aggregation = Aggregation.newAggregation(
							Aggregation.match(Criteria.where("account_id").is(model.getAccount_id()).and("log_type").is("addGold").and("create_time")
									.gte(MyDateUtil.getZeroDate(targetDate)).lte(MyDateUtil.getTomorrowZeroDate(targetDate)).and("v2")
									.is((long) EGoldOperateType.REAL_OPEN_ROOM.getId())),
							Aggregation.group().sum("v1").as("total").last("account_id").as("account_id"));

					AggregationResults<HashMap> models = SpringService.getBean(MongoDBService.class).getMongoTemplate().aggregate(aggregation,
							"game_log", HashMap.class);
					HashMap mm = models.getUniqueMappedResult();
					if (mm != null) {
						pmodel.setToday_consume((int) ((long) mm.get("total")));
					}
					proxyAccountReplaceRoomModelMap.put(pmodel.getAccount_id(), pmodel);
				} catch (Exception e) {
					logger.error("error", e);
				}

				// //调试
				// for(Map a : models){
				// System.out.println(a);
				// System.out.println(" " + a.get("total"));
				// }
				// System.out.println("======");
			}
			// 调试删除表
			// SpringService.getBean(MongoDBService.class).getMongoTemplate().dropCollection(ProxyAccountReplaceRoomModel.class);;
			// 批量插入数量
			SpringService.getBean(MongoDBService.class).getMongoTemplate().insertAll(proxyAccountReplaceRoomModelList);
			logger.info("统计代理代开房消耗:" + timer.getStr());
			timer.reset();

			// =================有下级代理的相关统计=============================
			// 返利计算
			double multiple = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(1109).getVal1();// 只读game_id=1的值
			multiple = multiple / 1000.0d;

			// 找出所有代理，查找他下面的所有代理
			List<OneProxyAccountReplaceRoomModel> oneProxyAccountReplaceRoomModelList = Lists.newArrayList();
			for (AccountModel model : accountModelList) {
				if (model.getIs_rebate() == 0)
					continue;
				OneProxyAccountReplaceRoomModel oneProxyAccountReplaceRoomModel = new OneProxyAccountReplaceRoomModel();
				oneProxyAccountReplaceRoomModel.setAccount_id(model.getAccount_id());
				oneProxyAccountReplaceRoomModel.setNotes_date(targetDateInt);
				oneProxyAccountReplaceRoomModel.setInvait_account_id(model.getRecommend_id());
				// long account_id = model.getAccount_id();
				// 他下面的所有代理
				List<Long> accountIdList = SpringService.getBean(PublicService.class).getPublicDAO()
						.getProxyAccountByProxyInfo(model.getAccount_id());
				oneProxyAccountReplaceRoomModel.setLower_proxy_account_ids(StringUtils.join(accountIdList, ","));
				// 统计下级所有消耗
				int count = 0;
				for (Long aid : accountIdList) {
					// 从上面的索引查值
					ProxyAccountReplaceRoomModel proxyAccountReplaceRoomModel = proxyAccountReplaceRoomModelMap.get(aid);// 每个代理的当日
																															// 消耗
					if (proxyAccountReplaceRoomModel != null) {
						count += proxyAccountReplaceRoomModel.getToday_consume();
					}
				}
				oneProxyAccountReplaceRoomModel.setToday_consume(count);
				oneProxyAccountReplaceRoomModel.setRebate(multiple);
				oneProxyAccountReplaceRoomModel.setMoney(count * multiple * 1.0d);// 元
				oneProxyAccountReplaceRoomModelList.add(oneProxyAccountReplaceRoomModel);
			}

			// 调试删除表
			// SpringService.getBean(MongoDBService.class).getMongoTemplate().dropCollection(OneProxyAccountReplaceRoomModel.class);;
			// 批量插入数量
			SpringService.getBean(MongoDBService.class).getMongoTemplate().insertAll(oneProxyAccountReplaceRoomModelList);
			System.out.println("统计代理下级的代理代开房消耗总和:" + timer.getStr());

			// 处理加返利数据
			for (OneProxyAccountReplaceRoomModel model : oneProxyAccountReplaceRoomModelList) {
				if (model.getMoney() > 0) {
					// TODO 加返利 rmb
					Account account = PublicServiceImpl.getInstance().getAccount(model.getAccount_id());
					if (account == null) {// 开通返利才算钱
						logger.error("找不到用户" + model.getAccount_id());
						continue;
					}

					SpringService.getBean(ICenterRMIServer.class).addAccountRMB(model.getAccount_id(), model.getMoney(), true,
							"每日统计增加返利(元):" + model.getMoney(), EGoldOperateType.REAL_RMB_COUNT);

				}
			}
			// DataThreadPool.getInstance().addTask(new
			// DBUpdateDto(DbStoreType.PUBLIC, DbOpType.BATCH_UPDATE,
			// "updateAccountModel", batchList));

		} catch (Exception e) {
			logger.error("proxAccountStat error", e);
		}
		logger.error("proxAccountStat timer" + timer.duration());

	}

	// 按游戏统计昨日豆消耗
	public HashMap<Integer, Long> getGoldCount(Date targetDate) {
		AggregationOperation match = Aggregation.match(Criteria.where("log_type").is(ELogType.parentBrand.getId()).and("create_time")
				.gte(MyDateUtil.getZeroDate(targetDate)).lte(MyDateUtil.getTomorrowZeroDate(targetDate)));
		AggregationOperation group = Aggregation.group("v2").sum("gold_count").as("count");
		Aggregation aggregation0 = Aggregation.newAggregation(match, group);
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		AggregationResults<GiveCardModel> aggResult = mongoDBService.getMongoTemplate().aggregate(aggregation0, "brand_log", GiveCardModel.class);
		List<GiveCardModel> resultList = aggResult.getMappedResults();
		HashMap<Integer, Long> map = new HashMap<Integer, Long>();
		if (resultList.size() == 0) {
			return map;
		}
		for (GiveCardModel model : resultList) {
			map.put(model.get_id().intValue(), model.getCount());
		}
		return map;
	}

	public void initProxyConsumTongJi() {
		try {
			Date now = new Date();
			Date targetDate = DateUtils.addDays(now, -1);
			int targetDateInt = Integer.valueOf(DateFormatUtils.format(targetDate, "yyyyMMdd"));
			List<ProxyConsumeModel> proxyConsumeModelList = new ArrayList<ProxyConsumeModel>();
			List<AccountModel> list1 = SpringService.getBean(PublicService.class).getPublicDAO().getProxyAccountList();
			int i = 1;
			for (AccountModel m : list1) {
				ProxyConsumeModel proxyConsumeModel = new ProxyConsumeModel();
				proxyConsumeModel.setAccount_id((long) m.getAccount_id());
				Account account = PublicServiceImpl.getInstance().getAccount(m.getAccount_id());
				if (account == null) {
					continue;
				}
				if (account.getAccountWeixinModel() != null) {
					proxyConsumeModel.setNick_name(account.getAccountWeixinModel().getNickname());
				}
				proxyConsumeModel.setGive_num(MongoDBServiceImpl.getInstance().getProxyGiveNum(proxyConsumeModel.getAccount_id(), now));
				proxyConsumeModel.setProxy_open_num(MongoDBServiceImpl.getInstance().getProxyOpenRoomNum(proxyConsumeModel.getAccount_id(), now));
				proxyConsumeModel.setCreate_time(targetDateInt);
				proxyConsumeModel.setLeftGold(m.getGold());
				proxyConsumeModel.setRecommend_id(m.getRecommend_id());
				proxyConsumeModelList.add(proxyConsumeModel);
				if (i % 100 == 0) {
					SpringService.getBean(MongoDBService.class).getMongoTemplate().insertAll(proxyConsumeModelList);
					proxyConsumeModelList = null;
					proxyConsumeModelList = new ArrayList<ProxyConsumeModel>();
				}
				i++;
			}
			SpringService.getBean(MongoDBService.class).getMongoTemplate().insertAll(proxyConsumeModelList);

		} catch (Exception e) {
		}
	}

	// public void initRedPacketActiveLog() {
	// try {
	// Date date = DateUtils.addDays(new Date(), -1);
	// // 获取当日推荐者的所有id
	// List<Long> accountIdList =
	// SpringService.getBean(PublicService.class).getPublicDAO().getRecommendAccountIdByDate(date);
	// List<RecommendActiveModel> list = new ArrayList<RecommendActiveModel>();
	// for (long account_id : accountIdList) {
	// Account account = PublicServiceImpl.getInstance().getAccount(account_id);
	// Map<Long, AccountRecommendModel> accountRecommendModelMap =
	// account.getAccountRecommendModelMap();
	// if (accountRecommendModelMap == null || accountRecommendModelMap.size()
	// == 0) {
	// RecommendActiveModel model = defaultRecommendActiveModel(account_id,
	// date, 0l, account.getAccountModel().getCreate_time());
	// list.add(model);
	// continue;
	// } else {
	// for (AccountRecommendModel recommendModel :
	// accountRecommendModelMap.values()) {
	// RecommendActiveModel model = null;
	// // 下级账号信息
	// Account downAccount =
	// PublicServiceImpl.getInstance().getAccount(recommendModel.getTarget_account_id());
	// AccountParamModel accountParamModel =
	// downAccount.getAccountParamModelMap().get(EAccountParamType.TODAY_CONSUM_GOLD.getId());
	// //
	// PublicServiceImpl.getInstance().getAccountParamModel(account_id,EAccountParamType.TODAY_CONSUM_GOLD);
	// if (accountParamModel == null) {
	// model = defaultRecommendActiveModel(account_id, date,
	// recommendModel.getTarget_account_id(),
	// account.getAccountModel().getCreate_time());
	// list.add(model);
	// continue;
	// }
	// model = new RecommendActiveModel();
	// model.setAccountId(account_id);
	// model.setTargetAccountId(recommendModel.getTarget_account_id());
	// model.setCreateTime(downAccount.getAccountModel().getCreate_time());
	// model.setCreate_time(date);
	// model.setType(1);
	// // model.setIp(downAccount.getAccountModel().getClient_ip());
	// model.setConsumeGoldNum(accountParamModel.getLong1());
	// model.setNickname(downAccount.getAccountWeixinModel().getNickname());
	// list.add(model);
	// }
	// }
	// }
	// SpringService.getBean(MongoDBService.class).getMongoTemplate().insertAll(list);
	// } catch (Exception e) {
	// }
	// }

	public RecommendActiveModel defaultRecommendActiveModel(long account_id, Date date, long target_account_id, Date createTime) {
		RecommendActiveModel model = new RecommendActiveModel();
		model.setAccountId(account_id);
		model.setTargetAccountId(target_account_id);
		model.setCreate_time(date);
		model.setType(1);
		model.setCreateTime(createTime);
		model.setConsumeGoldNum(0l);
		model.setNickname("");
		return model;
	}

	// 每月月初处理新版推广员返利重置
	private void resetNewRecommendReceive() {
		try {
			RecommenderReceiveService.getInstance().resetReceive();
		} catch (Exception e) {
			logger.error("resetReceive error!", e);
		}
	}
}
