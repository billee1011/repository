package com.cai.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.mapreduce.MapReduceResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.cai.common.define.EBonusPointsType;
import com.cai.common.define.ELogType;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.domain.AccountGamesModel;
import com.cai.common.domain.AccountSimple;
import com.cai.common.domain.AgentRecommend;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.BrandResultModel;
import com.cai.common.domain.ClubLogModel;
import com.cai.common.domain.Event;
import com.cai.common.domain.EveryDayAccountModel;
import com.cai.common.domain.EveryDayClubModel;
import com.cai.common.domain.EveryDayRobotModel;
import com.cai.common.domain.GameItemLogModel;
import com.cai.common.domain.GameLogModel;
import com.cai.common.domain.GameLogModelMoney;
import com.cai.common.domain.GameRecommend;
import com.cai.common.domain.GamesAccountModel;
import com.cai.common.domain.GiveCardModel;
import com.cai.common.domain.HallRecommend;
import com.cai.common.domain.InviteMoneyModel;
import com.cai.common.domain.InviteRedPacketModel;
import com.cai.common.domain.NewHallRecommend;
import com.cai.common.domain.OneProxyAccountReplaceRoomModel;
import com.cai.common.domain.Page;
import com.cai.common.domain.PlayerLogModel;
import com.cai.common.domain.PromoterProxyGameLog;
import com.cai.common.domain.ProxyGoldLogModel;
import com.cai.common.domain.RedPackageRecordModel;
import com.cai.common.domain.RedpacketPoolLogModel;
import com.cai.common.domain.RevicerRmbLogModel;
import com.cai.common.domain.RobotModel;
import com.cai.common.domain.ServerErrorLogModel;
import com.cai.common.domain.SubGameOnline;
import com.cai.common.domain.SystemLogModel;
import com.cai.common.domain.SystemLogQueueModel;
import com.cai.common.domain.TVExcluesiveLogModel;
import com.cai.common.domain.TempWeiXinProxyConsumeModel;
import com.cai.common.domain.TurntableRewardModel;
import com.cai.common.domain.TvActivityOnlineModel;
import com.cai.common.domain.WeiXinProxyConsumeModel;
import com.cai.common.domain.bonuspoints.BonusPointsExchangeLog;
import com.cai.common.domain.bonuspoints.BonusPointsLog;
import com.cai.common.domain.log.UseRedPacketLogModel;
import com.cai.common.domain.sdk.DiamondLogModel;
import com.cai.common.domain.zhuzhou.IndexModel;
import com.cai.common.domain.zhuzhou.RechargeRankModel;
import com.cai.common.domain.zhuzhou.RechargeRecordModel;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.SpringService;
import com.cai.core.MonitorEvent;
import com.cai.core.SystemConfig;
import com.cai.domain.Session;
import com.cai.mapreduce.MultiResultMapreduceData;
import com.cai.mapreduce.query.CoinExchangeMapReduce;
import com.cai.mapreduce.query.RealTimeStatisticMapReduce;
import com.cai.timer.MogoDBTimer;
import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * mogodb服务类
 * 
 * @author run
 * @param <E>
 *
 */
public class MongoDBServiceImpl<E> extends AbstractService {

	private static final Logger logger = LoggerFactory.getLogger(MongoDBServiceImpl.class);
	/**
	 * 日志队列
	 */
	private LinkedBlockingQueue<Object> logQueue = new LinkedBlockingQueue<>();

	private Timer timer;

	public List<EveryDayRobotModel> robotlist;

	private static MongoDBServiceImpl instance = null;

	// 引用
	private MogoDBTimer mogoDBTimer;

	private MongoDBServiceImpl() {
		timer = new Timer("Timer-MongoDBServiceImpl Timer");
	}

	public static MongoDBServiceImpl getInstance() {
		if (null == instance) {
			instance = new MongoDBServiceImpl();
		}
		return instance;
	}

	/**
	 * 玩家日志
	 * 
	 * @param account_id
	 * @param eLogType
	 * @param msg
	 * @param v1
	 * @param v2
	 * @param account_ip
	 * @param oldGold
	 *            历史房卡
	 * @param currentGold
	 *            当前房卡
	 */
	public void log(long account_id, ELogType eLogType, String msg, Long v1, Long v2, String account_ip, long oldGold, long currentGold) {
		GameLogModel gameLogModel = new GameLogModel();
		gameLogModel.setCreate_time(new Date());
		gameLogModel.setAccount_id(account_id);
		gameLogModel.setCenter_id(1);
		gameLogModel.setLog_type(eLogType.getId());
		gameLogModel.setMsg(msg);
		gameLogModel.setV1(v1);
		gameLogModel.setV2(v2);
		gameLogModel.setLocal_ip(SystemConfig.localip);
		gameLogModel.setAccount_ip(account_ip);
		gameLogModel.setOldGold(oldGold);
		gameLogModel.setCurrGold(currentGold);
		logQueue.add(gameLogModel);
	}

	/**
	 * 服务器报错日志
	 * 
	 * @param account_id
	 * @param eLogType
	 * @param msg
	 * @param v1
	 * @param v2
	 * @param account_ip
	 */
	public void server_error_log(int roomId, ELogType eLogType, String msg, Long accountID, String extractMsg) {
		try {
			ServerErrorLogModel gameLogModel = new ServerErrorLogModel();
			gameLogModel.setCreate_time(new Date());
			gameLogModel.setCenter_id(1);
			gameLogModel.setLog_type(eLogType.getId());
			gameLogModel.setMsg(msg);
			gameLogModel.setRoomId(roomId);
			gameLogModel.setExtractMsg(extractMsg);
			logQueue.add(gameLogModel);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void log_item_money(long account_id, ELogType eLogType, String msg, Long v1, Long v2, String account_ip, int toolsId) {
		GameItemLogModel gameLogModel = new GameItemLogModel();
		gameLogModel.setCreate_time(new Date());
		gameLogModel.setAccount_id(account_id);
		gameLogModel.setCenter_id(1);
		gameLogModel.setLog_type(eLogType.getId());
		gameLogModel.setMsg(msg);
		gameLogModel.setV1(v1);
		gameLogModel.setV2(v2);
		gameLogModel.setLocal_ip(SystemConfig.localip);
		gameLogModel.setAccount_ip(account_ip);
		gameLogModel.setToolsId(toolsId);
		logQueue.add(gameLogModel);
	}

	/**
	 * 使用道具
	 * 
	 * @param account_id
	 * @param eLogType
	 * @param msg
	 * @param v1
	 * @param v2
	 * @param account_ip
	 */
	public void log_use_item(long account_id, ELogType eLogType, String msg, Long v1, Long v2, String account_ip) {
		GameLogModelMoney gameLogModel = new GameLogModelMoney();
		gameLogModel.setCreate_time(new Date());
		gameLogModel.setAccount_id(account_id);
		gameLogModel.setCenter_id(1);
		gameLogModel.setLog_type(eLogType.getId());
		gameLogModel.setMsg(msg);
		gameLogModel.setV1(v1);
		gameLogModel.setV2(v2);
		gameLogModel.setLocal_ip(SystemConfig.localip);
		gameLogModel.setAccount_ip(account_ip);
		logQueue.add(gameLogModel);
	}

	/**
	 * 推广或推荐相关日志
	 * 
	 * @param account_id
	 * @param eLogType
	 * @param msg
	 * @param v1
	 * @param v2
	 * @param account_ip
	 */
	public void log_recommend(long account_id, ELogType eLogType, String msg, Long v1, Long v2, String account_ip, long target_id, long money) {
		GameRecommend gameLogModel = new GameRecommend();
		gameLogModel.setCreate_time(new Date());
		gameLogModel.setAccount_id(account_id);
		gameLogModel.setCenter_id(1);
		gameLogModel.setLog_type(eLogType.getId());
		gameLogModel.setMsg(msg);
		gameLogModel.setV1(v1);
		gameLogModel.setV2(v2);
		gameLogModel.setLocal_ip(SystemConfig.localip);
		gameLogModel.setAccount_ip(account_ip);
		gameLogModel.setTarget_id(target_id);
		gameLogModel.setRecharge_money(money);
		logQueue.add(gameLogModel);
	}

	/**
	 * 推广或推荐相关日志
	 * 
	 * @param account_id
	 * @param eLogType
	 * @param msg
	 * @param v1
	 * @param v2
	 * @param account_ip
	 */
	public void log_agent_recommend(long account_id, ELogType eLogType, String msg, Long v1, Long v2, String account_ip, long target_id,
			long rechargeMoney) {
		AgentRecommend agentLogModel = new AgentRecommend();
		agentLogModel.setCreate_time(new Date());
		agentLogModel.setAccount_id(account_id);
		agentLogModel.setCenter_id(1);
		agentLogModel.setLog_type(eLogType.getId());
		agentLogModel.setMsg(msg);
		agentLogModel.setV1(v1);
		agentLogModel.setV2(v2);
		agentLogModel.setLocal_ip(SystemConfig.localip);
		agentLogModel.setAccount_ip(account_ip);
		agentLogModel.setTarget_id(target_id);
		agentLogModel.setRecharge_money(rechargeMoney);
		logQueue.add(agentLogModel);
	}

	/**
	 * 大厅推广员二期相关日志
	 * 
	 * @param account_id
	 * @param eLogType
	 * @param msg
	 * @param v1
	 * @param v2
	 * @param account_ip
	 */
	public void log_hall_recommend(long account_id, ELogType eLogType, String msg, Long v1, Long v2, String account_ip, long target_id,
			long rechargeMoney, long sourceId) {
		HallRecommend hallLogModel = new HallRecommend();
		hallLogModel.setCreate_time(new Date());
		hallLogModel.setAccount_id(account_id);
		hallLogModel.setCenter_id(1);
		hallLogModel.setLog_type(eLogType.getId());
		hallLogModel.setMsg(msg);
		hallLogModel.setV1(v1);
		hallLogModel.setV2(v2);
		hallLogModel.setLocal_ip(SystemConfig.localip);
		hallLogModel.setAccount_ip(account_ip);
		hallLogModel.setTarget_id(target_id);
		hallLogModel.setRecharge_money(rechargeMoney);
		hallLogModel.setSource_id(sourceId);
		logQueue.add(hallLogModel);
	}

	public void log_new_hall_recommend(long account_id, ELogType eLogType, String msg, long v1, long v2, String account_ip, long target_id,
			long rechargeMoney, long sourceId, int recommend_level, int my_level, int receive_percent, String orderSeq) {
		NewHallRecommend hallLogModel = new NewHallRecommend();
		hallLogModel.setCreate_time(new Date());
		hallLogModel.setAccount_id(account_id);
		hallLogModel.setLog_type(eLogType.getId());
		hallLogModel.setMsg(msg);
		hallLogModel.setV1(v1);
		hallLogModel.setV2(v2);
		hallLogModel.setAccount_ip(account_ip);
		hallLogModel.setTarget_id(target_id);
		hallLogModel.setRecharge_money(rechargeMoney);
		hallLogModel.setSource_id(sourceId);
		hallLogModel.setRecommend_level(recommend_level);
		hallLogModel.setMy_level(my_level);
		hallLogModel.setReceive_percent(receive_percent);
		hallLogModel.setOrderSeq(StringUtils.isBlank(orderSeq) ? "" : orderSeq);
		SpringService.getBean(MongoDBService.class).getMongoTemplate().insert(hallLogModel);
	}

	/**
	 * 玩家非重要数值日志
	 * 
	 * @param account_id
	 * @param eLogType
	 * @param msg
	 * @param v1
	 * @param v2
	 * @param account_ip
	 */
	public void player_log(long account_id, ELogType eLogType, String msg, Long v1, Long v2, String account_ip) {
		PlayerLogModel playerLogModel = new PlayerLogModel();
		playerLogModel.setCreate_time(new Date());
		playerLogModel.setAccount_id(account_id);
		playerLogModel.setCenter_id(1);
		playerLogModel.setLog_type(eLogType.getId());
		playerLogModel.setMsg(msg);
		playerLogModel.setV1(v1);
		playerLogModel.setV2(v2);
		playerLogModel.setLocal_ip(SystemConfig.localip);
		playerLogModel.setAccount_ip(account_ip);
		logQueue.add(playerLogModel);
	}

	/**
	 * 系统日志
	 * 
	 * @param eLogType
	 * @param msg
	 * @param v1
	 * @param v2
	 */
	public void systemLog(ELogType eLogType, String msg, Long v1, Long v2, ESysLogLevelType eSysLogLevelType) {
		SystemLogModel systemLogModel = new SystemLogModel();
		systemLogModel.setCreate_time(new Date());
		systemLogModel.setCenter_id(1);
		systemLogModel.setLog_type(eLogType.getId());
		systemLogModel.setMsg(msg);
		systemLogModel.setV1(v1);
		systemLogModel.setV2(v2);
		systemLogModel.setLocal_ip(SystemConfig.localip);
		systemLogModel.setLevel(eSysLogLevelType.getId());
		logQueue.add(systemLogModel);
	}

	/**
	 * 系统日志
	 * 
	 * @param eLogType
	 * @param msg
	 * @param v1
	 * @param v2
	 */
	public void systemLog_queue(ELogType eLogType, String msg, Long v1, Long v2, ESysLogLevelType eSysLogLevelType) {
		SystemLogQueueModel systemLogModel = new SystemLogQueueModel();
		systemLogModel.setCreate_time(new Date());
		systemLogModel.setCenter_id(1);
		systemLogModel.setLog_type(eLogType.getId());
		systemLogModel.setMsg(msg);
		systemLogModel.setV1(v1);
		systemLogModel.setV2(v2);
		systemLogModel.setLocal_ip(SystemConfig.localip);
		systemLogModel.setLevel(eSysLogLevelType.getId());
		logQueue.add(systemLogModel);
	}

	/**
	 * 系统日志
	 * 
	 * @param eLogType
	 * @param msg
	 * @param v1
	 * @param v2
	 */
	public void insertSubGameOnline(SubGameOnline subGameOnline) {
		logQueue.add(subGameOnline);
	}

	public long getYesterdayIncome(long account_id) {
		long sum = 0;
		try {
			Date now = DateUtils.addDays(new Date(), -1);
			AggregationOperation match = Aggregation.match(Criteria.where("account_id").is(account_id).and("log_type").is("recommendIncome")
					.and("create_time").lte(MyDateUtil.getTomorrowZeroDate(now)).gte(MyDateUtil.getZeroDate(now)));
			AggregationOperation group = Aggregation.group().sum("v1").as("count").count().as("line");
			Aggregation aggregation = Aggregation.newAggregation(match, group);
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "game_recommend",
					GiveCardModel.class);
			List<GiveCardModel> list = result.getMappedResults();
			if (list != null && list.size() > 0) {
				GiveCardModel giveCardModel = list.get(0);
				sum = giveCardModel.getCount();
			}
		} catch (Exception e) {
		}
		return sum;
	}

	//
	public long getAgentYesterdayIncome(long account_id) {
		long sum = 0;
		try {
			Date now = DateUtils.addDays(new Date(), -1);
			AggregationOperation match = Aggregation.match(Criteria.where("account_id").is(account_id).and("log_type").is("agentIncome")
					.and("create_time").lte(MyDateUtil.getTomorrowZeroDate(now)).gte(MyDateUtil.getZeroDate(now)));
			AggregationOperation group = Aggregation.group().sum("v1").as("count");
			Aggregation aggregation = Aggregation.newAggregation(match, group);
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "hall_recommend",
					GiveCardModel.class);
			List<GiveCardModel> list = result.getMappedResults();
			if (list != null && list.size() > 0) {
				GiveCardModel giveCardModel = list.get(0);
				sum = giveCardModel.getCount();
			}
		} catch (Exception e) {
		}
		return sum;
	}

	/**
	 * 昨日代理转卡总数
	 * 
	 * @param page
	 * @param account_id
	 * @return
	 */
	public long getProxyGiveNum(long account_id, Date now) {
		long sum = 0;
		try {

			AggregationOperation match = Aggregation.match(Criteria.where("account_id").is(account_id).and("create_time")
					.lte(MyDateUtil.getTomorrowZeroDate(DateUtils.addDays(now, -1))).gte(MyDateUtil.getZeroDate(DateUtils.addDays(now, -1))));
			AggregationOperation group = Aggregation.group().sum("give_num").as("count").count().as("line");
			Aggregation aggregation = Aggregation.newAggregation(match, group);
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "proxy_gold_log",
					GiveCardModel.class);
			List<GiveCardModel> list = result.getMappedResults();
			if (list != null && list.size() > 0) {
				GiveCardModel giveCardModel = list.get(0);
				sum = giveCardModel.getCount();
			}
		} catch (Exception e) {
		}
		return sum;
	}

	/**
	 * 昨日代理代开房总数
	 * 
	 * @param page
	 * @param account_id
	 * @return
	 */
	public long getProxyOpenRoomNum(long account_id, Date now) {
		long sum = 0;
		try {
			Date targetDate = DateUtils.addDays(now, -1);
			int targetDateInt = Integer.valueOf(DateFormatUtils.format(targetDate, "yyyyMMdd"));
			AggregationOperation match = Aggregation.match(Criteria.where("account_id").is(account_id).and("notes_date").is(targetDateInt));
			AggregationOperation group = Aggregation.group().sum("today_consume").as("count").count().as("line");
			Aggregation aggregation = Aggregation.newAggregation(match, group);
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "proxy_account_replace_room",
					GiveCardModel.class);
			List<GiveCardModel> list = result.getMappedResults();
			if (list != null && list.size() > 0) {
				GiveCardModel giveCardModel = list.get(0);
				sum = giveCardModel.getCount();
			}
		} catch (Exception e) {
		}
		return sum;
	}

	/**
	 * 昨日代理代开房总数
	 * 
	 * @param page
	 * @param account_id
	 * @return
	 */
	public long getProxyOpenRoomNumByDay(long account_id, Date now) {
		long sum = 0;
		try {
			// Date targetDate = DateUtils.addDays(now, -1);
			int targetDateInt = Integer.valueOf(DateFormatUtils.format(now, "yyyyMMdd"));
			AggregationOperation match = Aggregation.match(Criteria.where("account_id").is(account_id).and("notes_date").is(targetDateInt));
			AggregationOperation group = Aggregation.group().sum("today_consume").as("count").count().as("line");
			Aggregation aggregation = Aggregation.newAggregation(match, group);
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "proxy_account_replace_room",
					GiveCardModel.class);
			List<GiveCardModel> list = result.getMappedResults();
			if (list != null && list.size() > 0) {
				GiveCardModel giveCardModel = list.get(0);
				sum = giveCardModel.getCount();
			}
		} catch (Exception e) {
		}
		return sum;
	}

	/**
	 * 代理转卡历史记录
	 * 
	 * @param page
	 * @param account_id
	 * @return
	 */
	public List<ProxyGoldLogModel> getProxyGoldLogModelList(Page page, long account_id, Long target_account_id) {
		// Date now = new Date();
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("account_id").is(account_id));
		// query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(now,
		// -2)));
		if (target_account_id != null) {
			query.addCriteria(Criteria.where("target_account_id").is(target_account_id));
		}
		query.with(new Sort(Direction.DESC, "create_time"));
		query.skip(page.getBeginNum());
		query.limit(page.getPageSize());
		List<ProxyGoldLogModel> proxyGoldLogModelList = mongoDBService.getMongoTemplate().find(query, ProxyGoldLogModel.class);
		return proxyGoldLogModelList;
	}

	public List<BrandResultModel> getBrandResultModelList(Page page, String groupId, Date date) {
		// Date now = new Date();
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("groupId").is(groupId));
		// query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(now,
		// -2)));
		query.addCriteria(Criteria.where("create_time").lte(MyDateUtil.getTomorrowZeroDate(date)).gte(MyDateUtil.getZeroDate(date)));
		query.with(new Sort(Direction.DESC, "create_time"));
		query.skip(page.getBeginNum());
		query.limit(page.getPageSize());
		List<BrandResultModel> brandResultModelList = mongoDBService.getMongoTemplate().find(query, BrandResultModel.class);
		return brandResultModelList;
	}

	/**
	 * 每日机器人开房总数
	 * 
	 * @param account_id
	 * @return
	 */
	public long getTotalRobotGold(long account_id, Date targetDate) {
		long sum = 0;
		try {
			int targetDateInt = Integer.valueOf(DateFormatUtils.format(targetDate, "yyyyMMdd"));
			Query query = new Query();
			query.addCriteria(Criteria.where("account_id").is(account_id));
			query.addCriteria(Criteria.where("notes_date").is(targetDateInt));
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			EveryDayRobotModel result = mongoDBService.getMongoTemplate().findOne(query, EveryDayRobotModel.class);
			return result.getGoldTotal();
		} catch (Exception e) {
		}
		return sum;
	}

	/**
	 * 机器人开房总数
	 * 
	 * @param account_id
	 * @return
	 */
	public long getTotalRobotGold(long account_id) {
		long sum = 0;
		try {
			AggregationOperation match = Aggregation.match(Criteria.where("account_id").is(account_id));
			AggregationOperation group = Aggregation.group().sum("goldTotal").as("count").count().as("line");
			Aggregation aggregation = Aggregation.newAggregation(match, group);
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "every_day_robot",
					GiveCardModel.class);
			List<GiveCardModel> list = result.getMappedResults();
			if (list != null && list.size() > 0) {
				GiveCardModel giveCardModel = list.get(0);
				sum = giveCardModel.getCount();
			}
		} catch (Exception e) {
		}
		return sum;
	}

	/**
	 * 代理转卡总数
	 * 
	 * @param account_id
	 * @return
	 */
	public long getProxyGoldLogModelGiveCount(long account_id) {
		long sum = 0;
		try {
			AggregationOperation match = Aggregation.match(Criteria.where("account_id").is(account_id));
			AggregationOperation group = Aggregation.group().sum("give_num").as("count").count().as("line");
			Aggregation aggregation = Aggregation.newAggregation(match, group);
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "proxy_gold_log",
					GiveCardModel.class);
			List<GiveCardModel> list = result.getMappedResults();
			if (list != null && list.size() > 0) {
				GiveCardModel giveCardModel = list.get(0);
				sum = giveCardModel.getCount();
			}
		} catch (Exception e) {
		}
		return sum;
	}

	/**
	 * 代理转卡历史记录数
	 * 
	 * @param account_id
	 * @return
	 */
	public int getProxyGoldLogModelCount(long account_id, Long target_account_id) {
		// Date now = new Date();
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("account_id").is(account_id));
		// query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(now,
		// -2)));
		if (target_account_id != null) {
			query.addCriteria(Criteria.where("target_account_id").is(target_account_id));
		}
		query.with(new Sort(Direction.DESC, "create_time"));
		long count = mongoDBService.getMongoTemplate().count(query, ProxyGoldLogModel.class);
		return (int) count;
	}

	/**
	 * 战绩记录数
	 * 
	 * @param account_id
	 * @return
	 */
	public int getBrandResultCount(String groupId) {
		// Date now = new Date();
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("groupId").is(groupId));
		long count = mongoDBService.getMongoTemplate().count(query, BrandResultModel.class);
		return (int) count;
	}

	/**
	 * 代理收益记录数
	 * 
	 * @param account_id
	 * @return
	 */
	public int getOneProxyAccountReplaceRoomModelCount(long account_id) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("account_id").is(account_id));
		query.with(new Sort(Direction.DESC, "create_time"));
		long count = mongoDBService.getMongoTemplate().count(query, OneProxyAccountReplaceRoomModel.class);
		return (int) count;
	}

	/**
	 * 群开房记录
	 * 
	 * @param account_id
	 * @return
	 */
	public int getRobotRoomModelCount(String groupId, Date targetDate) {

		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("groupId").is(groupId));
		query.with(new Sort(Direction.DESC, "create_time"));
		Date zeroDate = MyDateUtil.getZeroDate(targetDate);
		query.addCriteria(Criteria.where("create_time").gte(zeroDate).lte(MyDateUtil.getTomorrowZeroDate(targetDate)));

		long count = mongoDBService.getMongoTemplate().count(query, RobotModel.class);
		return (int) count;
	}

	/**
	 * 群开房记录
	 * 
	 * @param page
	 * @param account_id
	 * @return
	 */
	public List<RobotModel> getRobotModelList(Page page, String groupId, Date targetDate) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("groupId").is(groupId));
		query.addCriteria(Criteria.where("create_time").gte(MyDateUtil.getZeroDate(targetDate)).lte(MyDateUtil.getTomorrowZeroDate(targetDate)));
		query.with(new Sort(Direction.DESC, "create_time"));
		query.skip(page.getBeginNum());
		query.limit(page.getPageSize());
		List<RobotModel> robotList = mongoDBService.getMongoTemplate().find(query, RobotModel.class);
		return robotList;
	}

	/**
	 * 提现
	 * 
	 * @param account_id
	 * @return
	 */
	public int getRevicerRmbLogModelCount(long account_id) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("account_id").is(account_id));
		query.with(new Sort(Direction.DESC, "create_time"));
		long count = mongoDBService.getMongoTemplate().count(query, RevicerRmbLogModel.class);
		return (int) count;
	}

	/**
	 * 提现
	 * 
	 * @param page
	 * @param account_id
	 * @return
	 */
	public List<RevicerRmbLogModel> getRevicerRmbLogModelList(Page page, long account_id) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("account_id").is(account_id));
		query.with(new Sort(Direction.DESC, "notes_date"));
		// query.skip(page.getBeginNum());
		// query.limit(page.getPageSize());
		List<RevicerRmbLogModel> proxyGoldLogModelList = mongoDBService.getMongoTemplate().find(query, RevicerRmbLogModel.class);
		return proxyGoldLogModelList;
	}

	/**
	 * 代理收益记录数
	 * 
	 * @param page
	 * @param account_id
	 * @return
	 */
	public List<OneProxyAccountReplaceRoomModel> getOneProxyAccountReplaceRoomModelList(Page page, long account_id) {
		Date now = new Date();
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("account_id").is(account_id));
		query.with(new Sort(Direction.DESC, "notes_date"));
		// query.skip(page.getBeginNum());
		// query.limit(page.getPageSize());
		List<OneProxyAccountReplaceRoomModel> proxyGoldLogModelList = mongoDBService.getMongoTemplate().find(query,
				OneProxyAccountReplaceRoomModel.class);
		return proxyGoldLogModelList;
	}

	/**
	 * 代理转卡记录
	 * 
	 * @param account_id
	 * @param target_account_id
	 * @param give_num
	 * @param account_ip
	 * @param code
	 */
	public void proxyGoldLog(long account_id, long target_account_id, String target_nick_name, long give_num, String account_ip, int code,
			int target_proxy_account, long leftGold) {
		ProxyGoldLogModel model = new ProxyGoldLogModel();
		model.setCreate_time(MyDateUtil.getNow());
		model.setAccount_id(account_id);
		model.setTarget_account_id(target_account_id);
		model.setTarget_nick_name(target_nick_name);
		model.setGive_num(give_num);
		model.setAccount_ip(account_ip);
		model.setCode(code);
		model.setTarget_proxy_account(target_proxy_account);
		model.setLeftGold(leftGold);
		logQueue.add(model);
	}

	/**
	 * 机器人开房
	 */
	public List<EveryDayRobotModel> everyDayRobotModel(int decDay, boolean isInsert) {
		List<EveryDayRobotModel> returnlist = new ArrayList<EveryDayRobotModel>();
		try {
			Date now = new Date();
			Date targetDate = DateUtils.addDays(now, decDay);
			int targetDateInt = Integer.valueOf(DateFormatUtils.format(targetDate, "yyyyMMdd"));

			Date zeroDate = MyDateUtil.getZeroDate(targetDate);
			Aggregation aggregation = Aggregation.newAggregation(
					Aggregation.match(Criteria.where("create_time").gte(zeroDate).lte(MyDateUtil.getTomorrowZeroDate(targetDate))),
					Aggregation.group("groupId").sum("v1").as("goldTotal").sum("v2").as("brandtotal").last("account_id").as("account_id")
							.last("groupId").as("groupId"));

			AggregationResults<EveryDayRobotModel> models = SpringService.getBean(MongoDBService.class).getMongoTemplate().aggregate(aggregation,
					RobotModel.class, EveryDayRobotModel.class);

			List<EveryDayRobotModel> list = models.getMappedResults();
			if (list != null && list.size() > 0) {
				for (EveryDayRobotModel robotDay : list) {
					robotDay.setNotes_date(targetDateInt);
				}
				returnlist.addAll(list);
			}

		} catch (Exception e) {
			logger.error("everyDayRobotModel error", e);
		}
		robotlist = returnlist;
		return returnlist;

	}

	/**
	 * 
	 * 
	 * @param account_id
	 * @return
	 */
	public int getClublogModelCount(Date startDate, Date endDate, int clubId) {
		// Date now = new Date();
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("clubId").is(clubId).and("create_time").gte(MyDateUtil.getZeroDate(startDate))
				.lte(MyDateUtil.getTomorrowZeroDate(endDate)));
		query.with(new Sort(Direction.DESC, "create_time"));

		long count = mongoDBService.getMongoTemplate().count(query, ClubLogModel.class);
		return (int) count;
	}

	/**
	 * 俱乐部开房数据搜索
	 */
	public List<ClubLogModel> searchClubLogModel(Page page, Date startDate, Date endDate, int clubId) {

		Query query = new Query();
		query.addCriteria(Criteria.where("clubId").is(clubId).and("create_time").gte(MyDateUtil.getZeroDate(startDate))
				.lte(MyDateUtil.getTomorrowZeroDate(endDate)));
		query.with(new Sort(Direction.DESC, "create_time"));
		query.skip(page.getBeginNum());
		query.limit(page.getPageSize());
		List<ClubLogModel> list = SpringService.getBean(MongoDBService.class).getMongoTemplate().find(query, ClubLogModel.class);
		return list;
	}

	/**
	 * 俱乐部开房数据搜索
	 */
	public ClubLogModel searchClubLogModel(int clubId, int roomId) {

		Query query = new Query();
		query.addCriteria(Criteria.where("clubId").is(clubId).and("roomID").is(roomId));
		return SpringService.getBean(MongoDBService.class).getMongoTemplate().findOne(query, ClubLogModel.class);
	}

	/**
	 * 俱乐部开房数据搜索
	 */
	public List<EveryDayClubModel> searchDayClubModel(int startDate, int endDate, boolean isInsert, long account_id) {
		int targetDateInt = Integer.valueOf(DateFormatUtils.format(new Date(), "yyyyMMdd"));

		if (startDate == targetDateInt && endDate == targetDateInt) {
			// 如果只是要当天的数据
			List<EveryDayClubModel> todays = everyDayClubModel(0, false, account_id);
			return todays;
		}

		Aggregation aggregation = Aggregation.newAggregation(
				Aggregation.match(Criteria.where("account_id").is(account_id).and("notes_date").gte(startDate).lte(endDate)),
				Aggregation.group("clubId").sum("goldTotal").as("goldTotal").sum("brandtotal").as("brandtotal").last("account_id").as("account_id")
						.last("clubId").as("clubId"));

		AggregationResults<EveryDayClubModel> models = SpringService.getBean(MongoDBService.class).getMongoTemplate().aggregate(aggregation,
				EveryDayClubModel.class, EveryDayClubModel.class);

		List<EveryDayClubModel> list = models.getMappedResults();

		// 今天的统计还没加到 EveryDayClubModel 如果需要得从club_log里拉取
		if (startDate <= targetDateInt && endDate >= targetDateInt) {
			List<EveryDayClubModel> todays = everyDayClubModel(0, false, account_id);
			for (EveryDayClubModel everyDayClubModel : list) {
				for (EveryDayClubModel today : todays) {
					if (today.getClubId() == everyDayClubModel.getClubId()) {
						everyDayClubModel.setBrandtotal(everyDayClubModel.getBrandtotal() + today.getBrandtotal());
						everyDayClubModel.setGoldTotal(everyDayClubModel.getGoldTotal() + today.getGoldTotal());
					}
				}
			}

		}

		return list;
	}

	/**
	 * 俱乐部开房 获得
	 */
	public List<EveryDayClubModel> everyDayClubModel(int decDay, boolean isInsert, long account_id) {
		List<EveryDayClubModel> returnlist = new ArrayList<EveryDayClubModel>();
		try {
			Date date = new Date();
			Date targetDate = DateUtils.addDays(date, decDay);
			int targetDateInt = Integer.valueOf(DateFormatUtils.format(targetDate, "yyyyMMdd"));

			Date zeroDate = MyDateUtil.getZeroDate(targetDate);
			Criteria c = Criteria.where("create_time").gte(zeroDate).lte(MyDateUtil.getTomorrowZeroDate(targetDate));

			// 是否要查指定代理的
			if (account_id > 0) {
				c = c.and("account_id").is(account_id);
			}

			Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(c), Aggregation.group("clubId").sum("v1").as("goldTotal").sum("v2")
					.as("brandtotal").last("account_id").as("account_id").last("clubId").as("clubId").last("game_type_index").as("game_type_index"));

			AggregationResults<EveryDayClubModel> models = SpringService.getBean(MongoDBService.class).getMongoTemplate().aggregate(aggregation,
					ClubLogModel.class, EveryDayClubModel.class);

			List<EveryDayClubModel> list = models.getMappedResults();
			if (list != null && list.size() > 0) {
				for (EveryDayClubModel robotDay : list) {
					robotDay.setNotes_date(targetDateInt);
				}
				returnlist.addAll(list);
			}

		} catch (Exception e) {
			logger.error("everyDayClubModel error", e);
		}
		// robotlist = returnlist;
		return returnlist;
	}

	/**
	 * 俱乐部按日统计总开房扣豆数
	 */
	public Map<Integer, Long> everyDayClubOpenRoom(int decDay) {
		Map<Integer, Long> map = new HashMap<Integer, Long>();
		long sum = 0;
		long count = 0;
		try {
			Date date = new Date();
			Date targetDate = DateUtils.addDays(date, decDay);
			Date zeroDate = MyDateUtil.getZeroDate(targetDate);
			AggregationOperation match = Aggregation.match(
					Criteria.where("create_time").gte(zeroDate).lt(MyDateUtil.getTomorrowZeroDate(targetDate)).and("isExclusiveGold").is(false));
			AggregationOperation group = Aggregation.group().sum("v1").as("count").count().as("line");
			Aggregation aggregation = Aggregation.newAggregation(match, group);
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "club_log", GiveCardModel.class);
			List<GiveCardModel> list = result.getMappedResults();
			if (list != null && list.size() > 0) {
				GiveCardModel giveCardModel = list.get(0);
				sum = giveCardModel.getCount();
				count = giveCardModel.getLine();
			}
			map.put(1, sum);
			map.put(2, count);
		} catch (Exception e) {
			logger.error("everyDayClubOpenRoom error", e);
		}
		return map;
	}

	/**
	 * 机器人按日统计总开房扣豆数
	 */
	public long everyDayRobotOpenRoom(int decDay) {
		long sum = 0;
		try {
			Date date = new Date();
			Date targetDate = DateUtils.addDays(date, decDay);
			Date zeroDate = MyDateUtil.getZeroDate(targetDate);
			AggregationOperation match = Aggregation
					.match(Criteria.where("create_time").gte(zeroDate).lt(MyDateUtil.getTomorrowZeroDate(targetDate)));
			AggregationOperation group = Aggregation.group().sum("v1").as("count");
			Aggregation aggregation = Aggregation.newAggregation(match, group);
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "robot_log", GiveCardModel.class);
			List<GiveCardModel> list = result.getMappedResults();
			if (list != null && list.size() > 0) {
				GiveCardModel giveCardModel = list.get(0);
				sum = giveCardModel.getCount();
			}
		} catch (Exception e) {
			logger.error("everyDayRobotOpenRoom error", e);
		}
		return sum;
	}

	/**
	 * 月/季度代理转卡总数
	 * 
	 * @param page
	 * @param account_id
	 * @return
	 */
	public long getProxyGiveNumByDate(long account_id, Date start, Date end) {
		long sum = 0;
		try {
			AggregationOperation match = Aggregation.match(Criteria.where("account_id").is(account_id).and("create_time")
					.gte(MyDateUtil.getZeroDate(start)).lte(MyDateUtil.getTomorrowZeroDate(end)));
			AggregationOperation group = Aggregation.group().sum("give_num").as("count").count().as("line");
			Aggregation aggregation = Aggregation.newAggregation(match, group);
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "proxy_gold_log",
					GiveCardModel.class);
			List<GiveCardModel> list = result.getMappedResults();
			if (list != null && list.size() > 0) {
				GiveCardModel giveCardModel = list.get(0);
				sum = giveCardModel.getCount();
			}
		} catch (Exception e) {
		}
		return sum;
	}

	/**
	 * 月/季度代理代开房总数
	 * 
	 * @param page
	 * @param account_id
	 * @return
	 */
	public long getProxyOpenRoomNumByDate(long account_id, Date start, Date end) {
		long sum = 0;
		try {
			// Aggregation aggregation = Aggregation.newAggregation(
			// Aggregation.match(Criteria.where("account_id").is(account_id).and("log_type")
			// .is("addGold").and("create_time").gte(MyDateUtil.getZeroDate(start))
			// .lte(MyDateUtil.getTomorrowZeroDate(end)).and("v2")
			// .is((long) EGoldOperateType.REAL_OPEN_ROOM.getId())),
			// Aggregation.group().sum("v1").as("total").last("account_id").as("account_id"));
			//
			// AggregationResults<HashMap> models =
			// SpringService.getBean(MongoDBService.class).getMongoTemplate()
			// .aggregate(aggregation, "game_log", HashMap.class);
			// AggregationOperation match = Aggregation.match(
			// Criteria.where("account_id").is(account_id).and("log_type").is("addGold").and("create_time").gte(MyDateUtil.getZeroDate(start))
			// .lte(MyDateUtil.getTomorrowZeroDate(end)).and("v2").is((long)
			// EGoldOperateType.REAL_OPEN_ROOM.getId()));
			// AggregationOperation group =
			// Aggregation.group().sum("v1").as("count").count().as("line");
			// Aggregation aggregation = Aggregation.newAggregation(match,
			// group);
			// MongoDBService mongoDBService =
			// SpringService.getBean(MongoDBService.class);
			// AggregationResults<GiveCardModel> result =
			// mongoDBService.getMongoTemplate().aggregate(aggregation,
			// "game_log", GiveCardModel.class);
			int startDateInt = Integer.valueOf(DateFormatUtils.format(start, "yyyyMMdd"));
			int endDateInt = Integer.valueOf(DateFormatUtils.format(end, "yyyyMMdd"));
			AggregationOperation match = Aggregation
					.match(Criteria.where("account_id").is(account_id).and("notes_date").gte(startDateInt).lte(endDateInt));
			AggregationOperation group = Aggregation.group().sum("today_consume").as("count").count().as("line");
			Aggregation aggregation = Aggregation.newAggregation(match, group);
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "proxy_account_replace_room",
					GiveCardModel.class);
			List<GiveCardModel> list = result.getMappedResults();
			if (list != null && list.size() > 0) {
				GiveCardModel giveCardModel = list.get(0);
				sum = giveCardModel.getCount();
			}
		} catch (Exception e) {
		}
		return sum;
	}

	public long getProxyOpenRoomNumByDate2(long account_id, Date start, Date end) {
		long sum = 0;
		try {
			// Aggregation aggregation = Aggregation.newAggregation(
			// Aggregation.match(Criteria.where("account_id").is(account_id).and("log_type")
			// .is("addGold").and("create_time").gte(MyDateUtil.getZeroDate(start))
			// .lte(MyDateUtil.getTomorrowZeroDate(end)).and("v2")
			// .is((long) EGoldOperateType.REAL_OPEN_ROOM.getId())),
			// Aggregation.group().sum("v1").as("total").last("account_id").as("account_id"));
			//
			// AggregationResults<HashMap> models =
			// SpringService.getBean(MongoDBService.class).getMongoTemplate()
			// .aggregate(aggregation, "game_log", HashMap.class);
			int startDateInt = Integer.valueOf(DateFormatUtils.format(start, "yyyyMMdd"));
			int endDateInt = Integer.valueOf(DateFormatUtils.format(end, "yyyyMMdd"));
			AggregationOperation match = Aggregation
					.match(Criteria.where("account_id").is(account_id).and("notes_date").gte(startDateInt).lte(endDateInt));
			AggregationOperation group = Aggregation.group().sum("today_consume").as("count").count().as("line");
			Aggregation aggregation = Aggregation.newAggregation(match, group);
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "proxy_account_replace_room",
					GiveCardModel.class);
			List<GiveCardModel> list = result.getMappedResults();
			if (list != null && list.size() > 0) {
				GiveCardModel giveCardModel = list.get(0);
				sum = giveCardModel.getCount();
			}
		} catch (Exception e) {
		}
		return sum;
	}

	/**
	 * 月/季度机器人开房总数
	 * 
	 * @param page
	 * @param account_id
	 * @return
	 */
	public long getRobotOpenRoomByDate(long account_id, Date start, Date end) {
		long sum = 0;
		try {
			int startDateInt = Integer.valueOf(DateFormatUtils.format(start, "yyyyMMdd"));
			int endDateInt = Integer.valueOf(DateFormatUtils.format(end, "yyyyMMdd"));
			AggregationOperation match = Aggregation
					.match(Criteria.where("account_id").is(account_id).and("notes_date").gte(startDateInt).lte(endDateInt));
			AggregationOperation group = Aggregation.group().sum("goldTotal").as("count").count().as("line");
			Aggregation aggregation = Aggregation.newAggregation(match, group);
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "every_day_robot",
					GiveCardModel.class);
			List<GiveCardModel> list = result.getMappedResults();
			if (list != null && list.size() > 0) {
				GiveCardModel giveCardModel = list.get(0);
				sum = giveCardModel.getCount();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sum;
	}

	/**
	 * 月/季度代理赠送房卡总数
	 * 
	 * @param page
	 * @param account_id
	 * @return
	 */
	public long getDenateGoldByDate(long account_id, Date start, Date end) {
		long sum = 0;
		try {
			AggregationOperation match = Aggregation.match(Criteria.where("accountId").is(account_id).and("shopId").is(0).and("create_time")
					.gte(MyDateUtil.getZeroDate(start)).lte(MyDateUtil.getTomorrowZeroDate(end)));
			AggregationOperation group = Aggregation.group().sum("sendNum").as("count").count().as("line");
			Aggregation aggregation = Aggregation.newAggregation(match, group);
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "add_card_log", GiveCardModel.class);
			List<GiveCardModel> list = result.getMappedResults();
			if (list != null && list.size() > 0) {
				GiveCardModel giveCardModel = list.get(0);
				sum = giveCardModel.getCount();
			}
		} catch (Exception e) {
		}
		return sum;
	}

	// /**
	// * 月/季度下级代理总消耗房卡总数
	// * @param page
	// * @param account_id
	// * @return
	// */
	// public long getDownAgentCumsumeGoldByDate(long account_id,Date start,Date
	// end) {
	// long sum = 0;
	// try{
	// int startDateInt = Integer.valueOf(DateFormatUtils.format(start,
	// "yyyyMMdd"));
	// int endDateInt = Integer.valueOf(DateFormatUtils.format(end,
	// "yyyyMMdd"));
	// AggregationOperation match =
	// Aggregation.match(Criteria.where("account_id").is(account_id).and("notes_date").gte(startDateInt).lte(endDateInt));
	// AggregationOperation group =
	// Aggregation.group().sum("today_consume").as("count").count().as("line");
	// Aggregation aggregation = Aggregation.newAggregation(match, group);
	// MongoDBService mongoDBService =
	// SpringService.getBean(MongoDBService.class);
	// AggregationResults<GiveCardModel> result =
	// mongoDBService.getMongoTemplate().aggregate(aggregation,
	// "one_proxy_account_replace_room", GiveCardModel.class);
	// List<GiveCardModel> list = result.getMappedResults();
	// if(list!=null&&list.size()>0){
	// GiveCardModel giveCardModel = list.get(0);
	// sum = giveCardModel.getCount();
	// }
	// }catch(Exception e){
	// }
	// return sum;
	// }
	//

	/**
	 * 获取最大当前mongoDB上已经生成的最大brand_id
	 * 
	 * @param account_id
	 */
	public long getMaxBrandId() {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		// query.with(new Sort(Direction.DESC, "brand_id"));
		query.with(new Sort(Direction.DESC, "create_time"));
		query.limit(1);// 1局
		BrandLogModel brandLogModel = mongoDBService.getMongoTemplate().findOne(query, BrandLogModel.class);

		return (null == brandLogModel || brandLogModel.getBrand_id() > 999999999) ? 10101010L : brandLogModel.getBrand_id();
	}

	/**
	 * 红包排行榜
	 * 
	 * @return
	 */
	public List<Entry<Long, Long>> getRedPackageRankByActiveId() {
		DBObject groupFields = new BasicDBObject("_id", "$account_id");
		groupFields.put("total", new BasicDBObject("$sum", "$money"));
		DBObject group = new BasicDBObject("$group", groupFields);
		// sort
		DBObject sort = new BasicDBObject("$sort", new BasicDBObject("total", -1));
		// limit
		// limit
		DBObject limit = new BasicDBObject("$limit", 10);
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		AggregationOutput output = mongoDBService.getMongoTemplate().getCollection("red_package_model").aggregate(group, sort, limit);
		Iterable<DBObject> list = output.results();
		TreeMap<Long, Long> map = new TreeMap<Long, Long>();
		for (DBObject dbObject : list) {
			map.put(Long.parseLong(String.valueOf(dbObject.get("_id"))), Long.parseLong(String.valueOf(dbObject.get("total"))));
		}
		List<Entry<Long, Long>> list2 = new ArrayList<Entry<Long, Long>>(map.entrySet());
		Collections.sort(list2, new Comparator<Map.Entry<Long, Long>>() {
			// 降序排序
			public int compare(Entry<Long, Long> o1, Entry<Long, Long> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});
		return list2;
	}

	public void red_package_record_log(long account_id, int money, String nickName, int remainMoney) {
		RedPackageRecordModel redPackageModel = new RedPackageRecordModel();
		redPackageModel.setCreate_time(new Date());
		redPackageModel.setAccount_id(account_id);
		redPackageModel.setMoney(money);
		redPackageModel.setNickName(nickName);
		redPackageModel.setRemainMoney(remainMoney);
		logQueue.add(redPackageModel);
	}

	public void addCollection(Collection<? extends E> c) {
		logQueue.addAll(c);
	}

	public void log_turntable_reward(TurntableRewardModel reward) {
		logQueue.add(reward);
	}

	/**
	 * 获取转盘中奖纪录中20名
	 * 
	 * @return
	 */
	public List<TurntableRewardModel> getTurnTableRewardLog() {
		Query query = new Query();
		query.with(new Sort(Direction.DESC, "createTime"));
		query.limit(20);
		return SpringService.getBean(MongoDBService.class).getMongoTemplate().find(query, TurntableRewardModel.class);
	}

	// 获取有效邀请人数
	public long getEffectiveInvitePersonsCount(long account_id) {
		Query query = new Query();
		query.addCriteria(Criteria.where("account_id").is(account_id).and("state").is(1));
		long count = SpringService.getBean(MongoDBService.class).getMongoTemplate().count(query, InviteRedPacketModel.class);
		return count;
	}

	// 获取有效邀请人数
	public long getAllEffectiveInvitePersonsCount() {
		Query query = new Query();
		query.addCriteria(Criteria.where("state").is(1));
		long count = SpringService.getBean(MongoDBService.class).getMongoTemplate().count(query, InviteRedPacketModel.class);
		return count;
	}

	// 获取有效邀请人数
	public long getAllRedPacketCount() {
		Query query = new Query();
		query.addCriteria(Criteria.where("opt_type").is(0));
		long count = SpringService.getBean(MongoDBService.class).getMongoTemplate().count(query, InviteMoneyModel.class);
		return count;
	}

	public AccountGamesModel getAccountGamesModelByAccountId(long accountId) {
		Query query = new Query();
		query.addCriteria(Criteria.where("accountId").is(accountId));
		AccountGamesModel accountGamesModel = SpringService.getBean(MongoDBService.class).getMongoTemplate().findOne(query, AccountGamesModel.class);
		if (accountGamesModel == null) {
			accountGamesModel = new AccountGamesModel();
		}
		return accountGamesModel;
	}

	public void addOrUpdateAccountGamesModel(AccountGamesModel accountGamesModel) {

		Query query = new Query();
		query.addCriteria(Criteria.where("accountId").is(accountGamesModel.getAccountId()));
		Update update = new Update();
		update.set("games", accountGamesModel.getGames());
		SpringService.getBean(MongoDBService.class).getMongoTemplate().upsert(query, update, AccountGamesModel.class);

	}

	public void addGamsAccount(AccountGamesModel accountGamesModel, int game_type_index) {
		GamesAccountModel model = new GamesAccountModel();
		model.setAccountId(accountGamesModel.getAccountId());
		model.setCreate_time(new Date());
		model.setGame_type_index(game_type_index);
		logQueue.add(model);
	}

	public long queryOwnerRechargeMoney(long account_id, Date start, Date end) {
		long count = 0;
		try {
			// BasicDBList values = new BasicDBList();
			// values.add(0);
			// values.add(5);
			AggregationOperation match = Aggregation.match(Criteria.where("create_time").gte(start).lte(end).and("target_id").is(account_id)
					.and("log_type").is(ELogType.agentIncome.getId()));// .and("v2").in(values));
			AggregationOperation group = Aggregation.group().sum("recharge_money").as("count");
			Aggregation aggregation = Aggregation.newAggregation(match, group);
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "new_hall_recommend",
					GiveCardModel.class);
			List<GiveCardModel> sumLlist = result.getMappedResults();

			if (sumLlist != null && sumLlist.size() > 0) {
				for (GiveCardModel giveCardModel : sumLlist) {
					count = giveCardModel.getCount() != null ? giveCardModel.getCount() : 0;
				}
			}
		} catch (Exception e) {
			logger.error("querySubRechargeMoney error accountId=" + account_id, e);
		}
		return count;
	}

	public void addRedpacketPoolLogModel(long account_id, int log_type, int money, int remain, String msg) {
		RedpacketPoolLogModel model = new RedpacketPoolLogModel();
		model.setAccount_id(account_id);
		model.setCreate_time(new Date());
		model.setLog_type(log_type);
		model.setMoney(money);
		model.setRemain(remain);
		model.setMsg(msg);
		logQueue.add(model);
	}

	public long querySubRechargeMoney(long account_id, Date start, Date end) {
		long count = 0;
		try {
			AggregationOperation match = Aggregation.match(Criteria.where("create_time").gte(start).lte(end).and("account_id").is(account_id)
					.and("log_type").is(ELogType.agentIncome.getId()));
			AggregationOperation group = Aggregation.group().sum("recharge_money").as("count");
			Aggregation aggregation = Aggregation.newAggregation(match, group);
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "new_hall_recommend",
					GiveCardModel.class);
			List<GiveCardModel> sumLlist = result.getMappedResults();

			if (sumLlist != null && sumLlist.size() > 0) {
				for (GiveCardModel giveCardModel : sumLlist) {
					count = giveCardModel.getCount() != null ? giveCardModel.getCount() : 0;
				}
			}
		} catch (Exception e) {
			logger.error("querySubRechargeMoney error accountId=" + account_id, e);
		}
		return count;
	}

	// 根据订单查询返利列表
	public List<NewHallRecommend> queryNewHallRecommendListByOrderSeq(String orderSeq) {
		Query query = new Query();
		query.addCriteria(Criteria.where("orderSeq").is(orderSeq));
		return SpringService.getBean(MongoDBService.class).getMongoTemplate().find(query, NewHallRecommend.class);
	}

	// 退单更新
	public void updateNewHallRecommendListByRollback(List<NewHallRecommend> list) {
		for (NewHallRecommend model : list) {
			SpringService.getBean(MongoDBService.class).getMongoTemplate().save(model);
		}
	}

	/**
	 * 根据会员类型统计充值用户数和充值金额
	 * 
	 * @param start
	 * @param end
	 */
	public MapReduceResults<MultiResultMapreduceData> realTimeOrderStatisticsByMemberType(Date start, Date end) {
		MongoTemplate mongoTemplate = SpringService.getBean(MongoDBService.class).getMongoTemplate();
		Query query = new Query();
		query.addCriteria(Criteria.where("create_time").gte(start).lte(end).and("orderStatus").is(0));
		query.with(new Sort(Direction.DESC, "accountId"));
		MapReduceResults<MultiResultMapreduceData> outResult = mongoTemplate.mapReduce(query, "add_card_log", RealTimeStatisticMapReduce.MAP,
				RealTimeStatisticMapReduce.REDUCE, MultiResultMapreduceData.class);
		return outResult;
	}

	/**
	 * 金币兑换根据兑换金币数量Mapreduce
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	public MapReduceResults<MultiResultMapreduceData> coinExchangeStatisticsMapReduce(Date start, Date end) {
		MongoTemplate mongoTemplate = SpringService.getBean(MongoDBService.class).getMongoTemplate();
		Query query = new Query();
		query.addCriteria(Criteria.where("create_time").gte(start).lt(end).and("v2").is(EMoneyOperateType.DUIHUAN_PROP.getId()));
		query.with(new Sort(Direction.DESC, "account_id"));
		MapReduceResults<MultiResultMapreduceData> outResult = mongoTemplate.mapReduce(query, "game_log_money", CoinExchangeMapReduce.MAP,
				CoinExchangeMapReduce.REDUCE, MultiResultMapreduceData.class);
		return outResult;
	}

	public EveryDayAccountModel getLastestEveryDayAccountModel(Date date) {
		Query query = new Query();
		query.addCriteria(Criteria.where("create_time").gte(date));
		MongoTemplate mongoTemplate = SpringService.getBean(MongoDBService.class).getMongoTemplate();
		EveryDayAccountModel log = mongoTemplate.findOne(query, EveryDayAccountModel.class);
		if (log == null) {
			logger.error("查询昨日EveryDayAccountModel记录为空!!!");
			return null;
		}
		return log;
	}

	public void addTvActivityOnlineModel(int count, int total) {
		TvActivityOnlineModel model = new TvActivityOnlineModel();
		model.setCreate_time(new Date());
		model.setOnlineCount(count);
		model.setTotalCount(total);
		logQueue.add(model);
	}

	public void addTVExcluesiveLogModel(TVExcluesiveLogModel logModel) {
		logQueue.add(logModel);
	}

	public void addExchangeOrder(BonusPointsExchangeLog model) {
		logQueue.add(model);
	}

	public void addPromoterProxyGameLog(PromoterProxyGameLog logModel) {
		logQueue.add(logModel);
	}

	@Override
	protected void startService() {
		mogoDBTimer = new MogoDBTimer();
		timer.schedule(mogoDBTimer, 1000, 1000);
	}

	@Override
	public MonitorEvent montior() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sessionCreate(Session session) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sessionFree(Session session) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dbUpdate(int _userID) {
		// TODO Auto-generated method stub
	}

	public LinkedBlockingQueue getLogQueue() {
		return logQueue;
	}

	public MogoDBTimer getMogoDBTimer() {
		return mogoDBTimer;
	}

	public UseRedPacketLogModel getUseRedPacketLog(String orderId) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("orderId").is(orderId));

		UseRedPacketLogModel log = mongoDBService.getMongoTemplate().findOne(query, UseRedPacketLogModel.class);
		if (log == null) {
			logger.error("orderId={},UseRedPacket 订单居然找不到了!!!", orderId);
			return null;
		}
		return log;
	}

	public void addBonusPointsStream(long accountId, long score, long remainScore, String remark, EBonusPointsType etype, int money) {
		BonusPointsLog model = new BonusPointsLog();
		model.setAccountId(accountId);
		model.setCreate_time(new Date());
		model.setOperateType(etype.getId());
		model.setScore(score);
		model.setRemainScore(remainScore);
		model.setRemark(remark);
		model.setMoney(money);
		logQueue.add(model);
	}

	public List<Entry<Long, Long>> getExchangeGoodsRank() {
		DBObject groupFields = new BasicDBObject("_id", "$goodsId");
		groupFields.put("total", new BasicDBObject("$sum", "$count"));
		DBObject group = new BasicDBObject("$group", groupFields);
		DBObject sort = new BasicDBObject("$sort", new BasicDBObject("total", -1));
		DBObject limit = new BasicDBObject("$limit", 20);
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		AggregationOutput output = mongoDBService.getMongoTemplate().getCollection("bonus_points_exchange_log").aggregate(group, sort, limit);
		Iterable<DBObject> list = output.results();
		TreeMap<Long, Long> map = new TreeMap<Long, Long>();
		for (DBObject dbObject : list) {
			map.put(Long.parseLong(String.valueOf(dbObject.get("_id"))), Long.parseLong(String.valueOf(dbObject.get("total"))));
		}
		List<Entry<Long, Long>> list2 = new ArrayList<Entry<Long, Long>>(map.entrySet());
		Collections.sort(list2, new Comparator<Map.Entry<Long, Long>>() {
			// 降序排序
			public int compare(Entry<Long, Long> o1, Entry<Long, Long> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});
		return list2;
	}

	public void addProxyConsumeInMongo(ConcurrentHashMap<Long, ConcurrentHashMap<Integer, WeiXinProxyConsumeModel>> saveMap) {
		try {
			int notesDate = WeiXinProxyConsumeService.getInstance().getZeroDate();
			for (Map.Entry<Long, ConcurrentHashMap<Integer, WeiXinProxyConsumeModel>> entry : saveMap.entrySet()) {
				for (Map.Entry<Integer, WeiXinProxyConsumeModel> childEntry : entry.getValue().entrySet()) {
					WeiXinProxyConsumeModel curModel = childEntry.getValue();
					curModel.setNotes_date(notesDate);
					logQueue.add(curModel);
				}
			}
		} catch (Exception e) {
			logger.error("proxy consume save mongo error", e);
		}
	}

	public void addTempProxyConsumeInMongo(ConcurrentHashMap<Long, ConcurrentHashMap<Integer, WeiXinProxyConsumeModel>> saveMap) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			int notesDate = Integer.valueOf(sdf.format(new Date()));
			for (Map.Entry<Long, ConcurrentHashMap<Integer, WeiXinProxyConsumeModel>> entry : saveMap.entrySet()) {
				for (Map.Entry<Integer, WeiXinProxyConsumeModel> childEntry : entry.getValue().entrySet()) {
					WeiXinProxyConsumeModel curModel = childEntry.getValue();
					TempWeiXinProxyConsumeModel model = new TempWeiXinProxyConsumeModel();
					model.setAccount_id(curModel.getAccount_id());
					model.setBrand(curModel.getBrand());
					model.setExclusive_gold(curModel.getExclusive_gold());
					model.setGame_type_index(curModel.getGame_type_index());
					model.setGold_count(curModel.getGold_count());
					model.setNotes_date(notesDate);
					logQueue.add(model);
				}
			}
		} catch (Exception e) {
			logger.error("proxy consume save mongo error", e);
		}
	}

	public ConcurrentHashMap<Long, ConcurrentHashMap<Integer, WeiXinProxyConsumeModel>> getTempProxyConsume() {
		ConcurrentHashMap<Long, ConcurrentHashMap<Integer, WeiXinProxyConsumeModel>> proxyConsumeMap = new ConcurrentHashMap<Long, ConcurrentHashMap<Integer, WeiXinProxyConsumeModel>>();
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			int notesDate = Integer.valueOf(sdf.format(new Date()));
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			Query query = new Query();
			query.addCriteria(Criteria.where("notes_date").is(notesDate));
			List<TempWeiXinProxyConsumeModel> list = mongoDBService.getMongoTemplate().find(query, TempWeiXinProxyConsumeModel.class);
			for (TempWeiXinProxyConsumeModel model : list) {
				ConcurrentHashMap<Integer, WeiXinProxyConsumeModel> temp = proxyConsumeMap.get(model.getAccount_id());
				WeiXinProxyConsumeModel currentModel = null;
				if (temp != null) {
					currentModel = OToO(model);
					temp.put(model.getGame_type_index(), currentModel);
				} else {
					temp = new ConcurrentHashMap<Integer, WeiXinProxyConsumeModel>();
					currentModel = OToO(model);
					temp.put(model.getGame_type_index(), currentModel);
					proxyConsumeMap.put(model.getAccount_id(), temp);
				}
			}
		} catch (Exception e) {
			logger.error("proxy consume save mongo error", e);
		}
		return proxyConsumeMap;
	}

	private WeiXinProxyConsumeModel OToO(TempWeiXinProxyConsumeModel curModel) {
		WeiXinProxyConsumeModel model = new WeiXinProxyConsumeModel();
		model.setAccount_id(curModel.getAccount_id());
		model.setBrand(curModel.getBrand());
		model.setExclusive_gold(curModel.getExclusive_gold());
		model.setGame_type_index(curModel.getGame_type_index());
		model.setGold_count(curModel.getGold_count());
		model.setNotes_date(curModel.getNotes_date());
		return model;
	}

	/**
	 * 保存钻石流水记录
	 * 
	 * @param diamondLogModel
	 */
	public void saveDiamondLog(DiamondLogModel diamondLogModel) {
		MongoTemplate mongoTemplate = SpringService.getBean(MongoDBService.class).getMongoTemplate();
		mongoTemplate.save(diamondLogModel);
	}

	public void saveRechargeRecord(long accountId, long targetId, int rechargeType, int receive, int rechargeMoney, int type, String orderId,
			int percent, String desc) {
		RechargeRecordModel model = new RechargeRecordModel();
		model.setAccountId(accountId);
		model.setOrderId(orderId);
		model.setPayType(rechargeType);
		model.setReceive(receive);
		model.setRechargeMoney(rechargeMoney);
		model.setRechargeId(targetId);
		model.setReceivePer(percent);
		model.setType(type);
		model.setCreate_time(new Date());
		model.setDesc(desc);
		model.setOperateTime(MyDateUtil.getDateFormat(model.getCreate_time(), "yyyy-MM-dd"));
		this.logQueue.add(model);
	}

	public void getPromoterData(IndexModel index, Date start, Date end) {
		try {
			AggregationOperation match = Aggregation.match(Criteria.where("accountId").is(index.getAccountId()).and("create_time")
					.gte(MyDateUtil.getZeroDate(start)).lte(MyDateUtil.getTomorrowZeroDate(end)).and("type").is(0));
			AggregationOperation group = Aggregation.group().sum("receive").as("count");
			Aggregation aggregation = Aggregation.newAggregation(match, group);
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "zzxh_recharge_receive_log",
					GiveCardModel.class);
			List<GiveCardModel> list = result.getMappedResults();
			if (list != null && list.size() > 0) {
				GiveCardModel giveCardModel = list.get(0);
				index.setYesterdayIncome(giveCardModel.getCount() == null ? 0 : giveCardModel.getCount());
			}
			DBObject queryObject = new BasicDBObject("create_time", new BasicDBObject("$gte", start).append("$lte", end));
			queryObject.put("accountId", index.getAccountId());
			queryObject.put("type", 0);
			int count = mongoDBService.getMongoTemplate().getCollection("zzxh_recharge_receive_log").distinct("rechargeId", queryObject).size();
			index.setYesterdayRecharge(count);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public TreeSet<RechargeRankModel> getPromoteRechargeRank(long accountId, Date start, Date end) {
		TreeSet<RechargeRankModel> rankSet = new TreeSet<>();
		try {

			DBObject sumRechargeMoney = new BasicDBObject("$sum", "$rechargeMoney");
			DBObject sumReceiveMoney = new BasicDBObject("$sum", "$receive");
			DBObject groupFields = new BasicDBObject("_id", "$rechargeId");
			groupFields.put("totalRecharge", sumRechargeMoney);
			groupFields.put("totalReceive", sumReceiveMoney);
			DBObject group = new BasicDBObject("$group", groupFields);
			DBObject sort = new BasicDBObject("$sort", new BasicDBObject("totalRecharge", -1));
			DBObject limit = new BasicDBObject("$limit", 100);
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			AggregationOutput output = null;
			DBObject dbmatch = new BasicDBObject("accountId", accountId);
			dbmatch.put("type", 0);
			if (start != null) {
				DBObject dbObject = new BasicDBObject("$gte", start);
				dbObject.put("$lte", end);
				dbmatch.put("create_time", dbObject);
			}
			DBObject match = new BasicDBObject("$match", dbmatch);
			output = mongoDBService.getMongoTemplate().getCollection("zzxh_recharge_receive_log").aggregate(match, group, sort, limit);
			Iterable<DBObject> list = output.results();

			for (DBObject dbObject : list) {
				RechargeRankModel model = new RechargeRankModel();
				model.setAccountId(Long.parseLong(String.valueOf(dbObject.get("_id"))));
				model.setReceiveMoney(Integer.parseInt(String.valueOf(dbObject.get("totalReceive"))));
				model.setRechargeMoney(Integer.parseInt(String.valueOf(dbObject.get("totalRecharge"))));
				AccountSimple as = PublicServiceImpl.getInstance().getAccountSimpe(model.getAccountId());
				if (as != null) {
					model.setIcon(as.getIcon());
					model.setNick(as.getNick_name());
				} else {
					model.setIcon("-");
					model.setNick("");
				}
				rankSet.add(model);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rankSet;
	}

	// 获取推广对象的总充值返利
	public GiveCardModel getPromoteObjectTotalRecharge(long account_id) {
		GiveCardModel giveCardModel = new GiveCardModel();
		try {
			AggregationOperation match = Aggregation.match(Criteria.where("rechargeId").is(account_id).and("type").is(0));
			AggregationOperation group = Aggregation.group().sum("rechargeMoney").as("count").sum("receive").as("line");
			Aggregation aggregation = Aggregation.newAggregation(match, group);
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "zzxh_recharge_receive_log",
					GiveCardModel.class);
			List<GiveCardModel> list = result.getMappedResults();
			if (list != null && list.size() > 0) {
				giveCardModel = list.get(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return giveCardModel;
	}

	/**
	 * 钻石流水日志
	 * 
	 * @param orderID
	 * @param accountId
	 * @param nickname
	 * @param accountType
	 * @param sellType
	 * @param shopId
	 * @param cardNum
	 * @param sendNum
	 * @param rmb
	 * @param cashAccountID
	 * @param cashAccountName
	 * @param remark
	 * @param ossID
	 * @param ossName
	 * @param centerOrderID
	 * @param gameID
	 * @param shopName
	 * @param uiType
	 * @param subUiType
	 * @param opId
	 * @param channelId
	 */
	public void addDiamondLog(String orderID, long accountId, String nickname, int accountType, int sellType, int shopId, int cardNum, int sendNum,
			int rmb, int costDiamond, int cashAccountID, String cashAccountName, String remark, String ossID, String ossName, String centerOrderID,
			long gameID, String shopName, int uiType, int subUiType, int opId, int channelId, int oldNum, int curNum, int orderStatus) {
		try {
			DiamondLogModel addcardlog = new DiamondLogModel();
			addcardlog.setAccountId(accountId);
			addcardlog.setOrderID(orderID);
			addcardlog.setAccountType(accountType);
			addcardlog.setCardNum(cardNum);
			addcardlog.setCashAccountID(cashAccountID);
			addcardlog.setCashAccountName(cashAccountName);
			addcardlog.setCreate_time(new Date());
			addcardlog.setNickname(nickname);
			addcardlog.setRemark(remark);
			addcardlog.setRmb(rmb);
			addcardlog.setCostDiamond(costDiamond);
			addcardlog.setSellType(sellType);
			addcardlog.setSendNum(sendNum);
			addcardlog.setShopId(shopId);
			addcardlog.setOssID(ossID);
			addcardlog.setOssName(ossName);
			addcardlog.setCenterOrderID(centerOrderID);
			addcardlog.setGameId(gameID);
			addcardlog.setShopName(shopName);
			addcardlog.setOrderStatus(orderStatus);
			addcardlog.setUiType(uiType);
			addcardlog.setSubUiType(subUiType);
			addcardlog.setOpId(opId);
			addcardlog.setChannelId(channelId);
			addcardlog.setOldNum(oldNum);
			addcardlog.setCurNum(curNum);
			SpringService.getBean(MongoDBService.class).getMongoTemplate().insert(addcardlog);
		} catch (Exception e) {
			logger.error("addcardLog插入日志异常" + e);
		}
	}
}
