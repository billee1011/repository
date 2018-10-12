/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * 
 * 
 * @author tang date: 2018年05月03日 下午14:02:34 <br/>
 */
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.StringUtils;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;

import com.cai.common.define.DbOpType;
import com.cai.common.define.DbStoreType;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.ELogType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AccountSimple;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.DBUpdateDto;
import com.cai.common.domain.Event;
import com.cai.common.domain.GiveCardModel;
import com.cai.common.domain.HallRecommendModel;
import com.cai.common.domain.RecommendReceiveModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.SpringService;
import com.cai.core.DataThreadPool;
import com.cai.core.MonitorEvent;
import com.cai.core.TaskThreadPool;
import com.cai.dictionary.SysParamServerDict;
import com.cai.domain.Session;
import com.cai.future.runnable.AutoDownRecommendLevelRunnble;
import com.cai.rmi.impl.CenterRMIServerImpl;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBList;

/**
 * 
 * 帐号与手机绑定服务相关
 *
 * @author wu_hc date: 2017年12月4日 上午11:05:26 <br/>
 */
public class RecommenderReceiveService extends AbstractService {

	private static RecommenderReceiveService instance = new RecommenderReceiveService();
	private final Timer timer;

	private Map<Long, RecommendReceiveModel> receiveMap = Maps.newConcurrentMap();

	private RecommenderReceiveService() {
		timer = new Timer("recommenderReceiveService-Timer");
	}

	public static RecommenderReceiveService getInstance() {
		return instance;
	}

	@Override
	protected void startService() {
		load();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					dumpRecommenReceiveModel();
				} catch (Exception e) {
					logger.error("", e);
				}
			}
		}, 60 * 1000L, 60 * 1000L);

	}

	private void load() {
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<RecommendReceiveModel> recommendReceiveModelList = publicService.getPublicDAO().getRecommenderReceiveList();
		for (RecommendReceiveModel model : recommendReceiveModelList) {
			receiveMap.put(model.getAccount_id(), model);
		}
		logger.info("load recommendReceiveModelList success!");
	}

	public void updateLevel(long account_id, int level) {
		RecommendReceiveModel model = receiveMap.get(account_id);
		if (model == null) {
			return;
		} else {
			model.setLevel(level);
			model.setNeedDB(true);
		}
	}

	// 上级调用的方法已经加了锁，这里不再使用锁
	public void addReceive(Account account, double receive) {
		// Account account =
		// PublicServiceImpl.getInstance().getAccount(account_id);
		// if (account == null) {
		// return;
		// }
		// ReentrantLock reentrantLock = account.getRedisLock();
		// reentrantLock.lock();
		int money = (int) (receive * 100.0);
		try {
			RecommendReceiveModel model = receiveMap.get(account.getAccount_id());
			if (model == null) {
				Date date = new Date();
				model = new RecommendReceiveModel();
				model.setAccount_id(account.getAccount_id());
				model.setCreateTime(date);
				model.setLastTime(date);
				model.setReceive(money);
				model.setLevel(account.getHallRecommendModel().getRecommend_level());
				model.setNewAddValue(true);
				receiveMap.put(account.getAccount_id(), model);
			} else {
				model.setReceive(model.getReceive() + money);
				model.setNeedDB(true);
			}
		} catch (Exception e) {
			logger.error("add Receive error!", e);
		}
		// finally {
		// reentrantLock.unlock();
		// }
	}

	private static long planTime = 16 * 24 * 60 * 60000l;

	// 每月月初0点重置返利，并且实施降级处理
	public void resetReceive() {
		SysParamModel sysParamModel2253 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2253);
		if (sysParamModel2253 == null || sysParamModel2253.getVal4() == 0) {
			return;
		}
		Date firstDay = MyDateUtil.getNowMonthSomeDay(sysParamModel2253.getVal5());
		if (!MyDateUtil.isSameDay(firstDay)) {
			return;
		}

		// 16天前的时间点
		long thatTime = System.currentTimeMillis() - planTime;
		for (RecommendReceiveModel model : receiveMap.values()) {
			if (model.getLastTime().getTime() - thatTime > 0) {// 用于判断是否15天开通的用户
				model.resetModel();
				model.setNeedDB(true);
				continue;
			} else {
				if (model.getLevel() > 0) {
					// 降级处理
					autoDownLevel(model.getAccount_id(), model.getReceive());
					model.resetModel();
					model.setNeedDB(true);
				}
			}

		}
	}

	// 降级处理
	private void autoDownLevel(long account_id, int receive) {
		AutoDownRecommendLevelRunnble runnlble = new AutoDownRecommendLevelRunnble(account_id, receive);
		TaskThreadPool.getInstance().addTask(runnlble);
	}

	private void dumpRecommenReceiveModel() {
		PublicService publicService = SpringService.getBean(PublicService.class);
		for (RecommendReceiveModel model : receiveMap.values()) {
			if (model.isNewAddValue()) {
				publicService.getPublicDAO().insertRecommendReceiveModel(model);
				model.setNewAddValue(false);
			} else if (model.isNeedDB()) {
				publicService.getPublicDAO().updateRecommendReceiveModel(model);
				model.setNeedDB(false);
			}
		}
	}

	public RecommendReceiveModel getRecommendReceiveModel(long account_id) {
		RecommendReceiveModel model = receiveMap.get(account_id);
		return model == null ? new RecommendReceiveModel() : model;
	}

	public Map<Long, RecommendReceiveModel> getReceiveMap() {
		return receiveMap;
	}

	// 玩家提现
	public AddGoldResultModel drawCash(long account_id, double income, int payType) {
		AddGoldResultModel resultModel = null;
		if (payType == 1) {
			resultModel = doHallRecommendIncome(account_id, income, 0l, "余额支付", EGoldOperateType.AGENT_BALANCE_PAY, 0l, 0l, 0l, 0, 0, 0, "");
		} else {
			resultModel = doHallRecommendIncome(account_id, income, 0l, "提现", EGoldOperateType.AGENT_RECHARGE_DRAWCASH, 0l, 0l, 0l, 0, 0, 0, "");
		}
		return resultModel;
	}

	private AddGoldResultModel doHallRecommendIncome(long account_id, double income, long level, String desc, EGoldOperateType eGoldOperateType,
			long targetId, long rechargeMoney, long sourceId, int recommend_level, int my_level, int receive_percent, String orderSeq) {
		AddGoldResultModel addGoldResultModel = new AddGoldResultModel();
		addGoldResultModel.setSuccess(false);
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		Account account = centerRMIServer.getAccount(account_id);
		if (account == null) {
			addGoldResultModel.setMsg("账号不存在");
			return addGoldResultModel;
		}
		if (income > 0) {
			income = -income;
		}
		ReentrantLock lock = account.getRedisLock();
		lock.lock();
		try {
			AccountModel accountModel = account.getAccountModel();
			addGoldResultModel.setAccountModel(accountModel);
			double oldValue = accountModel.getRecommend_remain_income();
			if (income < 0) {
				if (accountModel.getIs_rebate() != 1) {
					addGoldResultModel.setMsg("您未开通提现功能");
					return addGoldResultModel;
				}
			} else {
				addGoldResultModel.setMsg("提现金额不为负数");
				return addGoldResultModel;
			}
			double k = accountModel.getRecommend_remain_income() + income;
			if (k < 0) {
				addGoldResultModel.setMsg("提现的金额不能大于余额");
				return addGoldResultModel;
			} else {
				accountModel.setRecommend_remain_income(k);
			}
			accountModel.setRecommend_receive_income(accountModel.getRecommend_receive_income() - income);
			addGoldResultModel.setSuccess(true);
			// 现金操作操作直接入库
			DataThreadPool.getInstance().addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.UPDATE, "updateAccountModel", accountModel));
			double change = (accountModel.getRecommend_remain_income() - oldValue) * 100;// 元换成分
			double newValue = accountModel.getRecommend_remain_income() * 100;
			// 日志
			StringBuffer buf = new StringBuffer();
			buf.append("|");
			if (change < 0) {
				buf.append("减少[" + change + "]");
				buf.append(",值变化:[").append(oldValue * 100.0).append("]->[").append(newValue).append("]分");
				desc = desc + buf.toString();
				MongoDBServiceImpl.getInstance().log_new_hall_recommend(account_id, ELogType.agentOutcome, desc, (long) change, level, null, targetId,
						rechargeMoney, sourceId, recommend_level, my_level, receive_percent, orderSeq);
			}
		} catch (Exception e) {
			logger.error("error", e);
		} finally {
			lock.unlock();
		}
		return addGoldResultModel;
	}

	// 查询下级代理总返利
	public long querySubAgentReceive(long account_id) {
		long count = 0;
		BasicDBList v2 = new BasicDBList();
		v2.add(0);
		v2.add(4);
		AggregationOperation match = Aggregation
				.match(Criteria.where("account_id").is(account_id).and("log_type").is(ELogType.agentIncome.getId()).and("v2").in(v2));
		AggregationOperation group = Aggregation.group().sum("v1").as("line");
		Aggregation aggregation = Aggregation.newAggregation(match, group);
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "new_hall_recommend",
				GiveCardModel.class);
		List<GiveCardModel> sumList = result.getMappedResults();
		if (sumList != null && sumList.size() > 0) {
			for (GiveCardModel giveCardModel : sumList) {
				return giveCardModel.getLine();
			}
		}
		return count;
	}

	// 查询下级推广员总返利
	public long querySubRecommenderReceive(long account_id) {
		long count = 0;
		BasicDBList v2 = new BasicDBList();
		v2.add(1);
		v2.add(2);
		v2.add(3);
		AggregationOperation match = Aggregation
				.match(Criteria.where("account_id").is(account_id).and("log_type").is(ELogType.agentIncome.getId()).and("v2").in(v2));
		AggregationOperation group = Aggregation.group().sum("v1").as("line");
		Aggregation aggregation = Aggregation.newAggregation(match, group);
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "new_hall_recommend",
				GiveCardModel.class);
		List<GiveCardModel> sumList = result.getMappedResults();
		if (sumList != null && sumList.size() > 0) {
			for (GiveCardModel giveCardModel : sumList) {
				return giveCardModel.getLine();
			}
		}
		return count;
	}

	// 查询下级代理收益汇总
	public Map<String, Object> queryHallDownAgent(long account_id, Date startDate, Date endDate) {
		Account account = PublicServiceImpl.getInstance().getAccount(account_id);
		if (account == null) {
			return null;
		}

		int level = account.getAccountModel().getProxy_level();
		if (level == 0) {
			return null;
		}
		Map<Long, HallRecommendModel> recommendMap = account.getHallRecommendModelMap();
		if (recommendMap == null || recommendMap.size() == 0) {
			return null;
		}
		// 在从缓存中取值的时候，就根据日期进行排序
		Map<String, Object> detailsParam = new HashMap<>();

		AggregationOperation match = Aggregation.match(Criteria.where("account_id").is(account_id).and("log_type").is(ELogType.agentIncome.getId())
				.and("create_time").gte(MyDateUtil.getZeroDate(startDate)).lt(MyDateUtil.getTomorrowZeroDate(endDate)).and("v2").is(0));
		AggregationOperation group = Aggregation.group("target_id").sum("recharge_money").as("count").sum("v1").as("line");
		Aggregation aggregation = Aggregation.newAggregation(match, group);
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "new_hall_recommend",
				GiveCardModel.class);
		List<GiveCardModel> sumList = result.getMappedResults();
		if (sumList != null && sumList.size() > 0) {
			long nowTime = System.currentTimeMillis();
			List<Map<String, Object>> list = new ArrayList<Map<String, Object>>(sumList.size());
			for (GiveCardModel giveCardModel : sumList) {
				if (giveCardModel.get_id() > 0) {
					Map<String, Object> details = new HashMap<>();
					AccountSimple accountSimple = PublicServiceImpl.getInstance().getAccountSimpe(giveCardModel.get_id());
					if (accountSimple == null) {
						details.put("nickName", "-");
						details.put("headPic", "");
					} else {
						details.put("nickName", accountSimple.getNick_name());
						details.put("headPic", accountSimple.getIcon());
					}
					details.put("accountId", giveCardModel.get_id());
					details.put("totalReceive", giveCardModel.getLine() / 100.00);
					details.put("totalRecharge", giveCardModel.getCount() / 100.00);
					details.put("updateDays", getDays(recommendMap.get(giveCardModel.get_id()), nowTime));
					list.add(details);
				}
			}
			detailsParam.put("total", sumList.size());// 总人数
			detailsParam.put("data", list);
		}
		return detailsParam;
	}

	// 查询下级推广员详情
	public Map<String, Object> queryHallDownRecommend(long account_id, Date startDate, Date endDate) {

		Account account = PublicServiceImpl.getInstance().getAccount(account_id);
		if (account == null) {
			return null;
		}
		int level = account.getAccountModel().getProxy_level();
		if (level == 0) {
			return null;
		}
		Map<Long, HallRecommendModel> recommendMap = account.getHallRecommendModelMap();
		if (recommendMap == null || recommendMap.size() == 0) {
			return null;
		}
		if (account.getHallRecommendModel().getRecommend_level() == 0) {
			return null;
		}
		// 在从缓存中取值的时候，就根据日期进行排序
		List<HallRecommendModel> lisRecommend = new ArrayList<HallRecommendModel>(recommendMap.values());// 所有的推广员

		Map<String, Object> detailsParam = new HashMap<>();
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		CenterRMIServerImpl centerRMIServer = SpringService.getBean(CenterRMIServerImpl.class);
		int total = 0;
		BasicDBList values = new BasicDBList();
		long nowTime = System.currentTimeMillis();
		for (HallRecommendModel model : lisRecommend) {
			if (model.getRecommend_level() == 0) {
				// 非推广员排除掉
				continue;
			}
			total++;
			values.add(model.getTarget_account_id());
			Map<String, Object> details = Maps.newConcurrentMap();
			// 当前代理账号信息
			Account subAccount = centerRMIServer.getAccount(model.getTarget_account_id());
			if (subAccount == null) {
				continue;
			}
			details.put("accountId", model.getTarget_account_id());
			details.put("nickName", subAccount.getAccountWeixinModel() == null ? "-" : subAccount.getAccountWeixinModel().getNickname());
			HashMap<Integer, Integer> map = agentCount(subAccount.getHallRecommendModelMap());
			details.put("subCount", map.get(2));// 下级推广员人数
			details.put("agentCount", map.get(1));// 下级代理人数
			details.put("playerCount", map.get(3));// 玩家人数
			details.put("recommendLevel", model.getRecommend_level());
			details.put("totalReceive", subAccount.getAccountModel().getRecommend_history_income());// 累计返利
			details.put("createTime", model.getUpdate_time());
			details.put("updateDays", getDays(model, nowTime));
			list.add(details);
		}
		if (list.size() > 0) {
			BasicDBList v2 = new BasicDBList();
			v2.add(1);
			v2.add(2);
			v2.add(3);
			v2.add(4);
			AggregationOperation match = Aggregation
					.match(Criteria.where("account_id").is(account_id).and("log_type").is(ELogType.agentIncome.getId()).and("create_time")
							.gte(MyDateUtil.getZeroDate(startDate)).lt(MyDateUtil.getTomorrowZeroDate(endDate)).and("v2").in(v2));
			AggregationOperation group = Aggregation.group("source_id").sum("recharge_money").as("count").sum("v1").as("line");
			Aggregation aggregation = Aggregation.newAggregation(match, group);
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "new_hall_recommend",
					GiveCardModel.class);
			List<GiveCardModel> sumList = result.getMappedResults();
			Map<Long, GiveCardModel> tjMap = new HashMap<>();
			if (sumList != null && sumList.size() > 0) {
				for (GiveCardModel giveCardModel : sumList) {
					tjMap.put(giveCardModel.get_id(), giveCardModel);
				}
			}
			for (Map<String, Object> details : list) {
				long accountId = (long) details.get("accountId");
				GiveCardModel gm = tjMap.get(accountId);
				long totalRecharge = gm != null ? gm.getCount() : 0;
				long totalReceive = gm != null ? gm.getLine() : 0;
				details.put("totalRecharge", totalRecharge / 100.00);
				details.put("totalReceive", totalReceive / 100.00);
			}
		}
		detailsParam.put("total", total);// 总人数
		detailsParam.put("data", list);
		return detailsParam;
	}

	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

	public long getDays(HallRecommendModel model, long s2) {

		if (model == null || StringUtils.isBlank(model.getUpdate_time()) || model.getUpdate_time().startsWith("0")) {
			return 0;
		}
		java.util.Date date = null;
		try {
			date = sdf.parse(model.getUpdate_time());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		long s1 = date.getTime();// 将时间转为毫秒
		long day = (s2 - s1) / 1000 / 60 / 60 / 24;
		return day;
	}

	// 查询下级推广员详情
	public Map<String, Object> queryHallDownRecommend(long account_id) {

		Account account = PublicServiceImpl.getInstance().getAccount(account_id);
		if (account == null) {
			return null;
		}
		int level = account.getAccountModel().getProxy_level();
		if (level == 0) {
			return null;
		}
		Map<Long, HallRecommendModel> recommendMap = account.getHallRecommendModelMap();
		if (recommendMap == null || recommendMap.size() == 0) {
			return null;
		}
		if (account.getHallRecommendModel().getRecommend_level() == 0) {
			return null;
		}
		// 在从缓存中取值的时候，就根据日期进行排序
		List<HallRecommendModel> lisRecommend = new ArrayList<HallRecommendModel>(recommendMap.values());// 所有的推广员

		Map<String, Object> detailsParam = new HashMap<>();
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		CenterRMIServerImpl centerRMIServer = SpringService.getBean(CenterRMIServerImpl.class);
		int total = 0;
		BasicDBList values = new BasicDBList();
		for (HallRecommendModel model : lisRecommend) {
			if (model.getRecommend_level() == 0) {
				// 非推广员排除掉
				continue;
			}
			total++;
			values.add(model.getTarget_account_id());
			Map<String, Object> details = Maps.newConcurrentMap();
			// 当前代理账号信息
			Account subAccount = centerRMIServer.getAccount(model.getTarget_account_id());
			if (subAccount == null) {
				continue;
			}
			details.put("accountId", model.getTarget_account_id());
			details.put("nickName", subAccount.getAccountWeixinModel() == null ? "-" : subAccount.getAccountWeixinModel().getNickname());
			HashMap<Integer, Integer> map = agentCount(subAccount.getHallRecommendModelMap());
			details.put("subCount", map.get(2));// 下级推广员人数
			details.put("agentCount", map.get(1));// 下级代理人数
			details.put("playerCount", map.get(3));// 下级代理人数
			details.put("recommendLevel", model.getRecommend_level());
			details.put("createTime", model.getUpdate_time());
			list.add(details);
		}
		detailsParam.put("total", total);// 总人数
		detailsParam.put("data", list);
		return detailsParam;
	}

	public static HashMap<Integer, Integer> agentCount(Map<Long, HallRecommendModel> getHallRecommendModelMap) {
		int count = 0;
		int recommCount = 0;
		int playerCount = 0;
		for (HallRecommendModel model : getHallRecommendModelMap.values()) {
			if (model.getRecommend_level() > 0) {
				recommCount++;
			}
			if (model.getRecommend_level() == 0 && model.getProxy_level() > 0) {
				count++;
			}
			if (model.getProxy_level() == 0) {
				playerCount++;
			}
		}
		HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
		map.put(1, count);
		map.put(2, recommCount);
		map.put(3, playerCount);
		return map;
	}

	@Override
	protected void stopService() {
		dumpRecommenReceiveModel();
	}

	@Override
	public MonitorEvent montior() {
		return null;
	}

	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {
	}

	@Override
	public void sessionCreate(Session session) {
	}

	@Override
	public void sessionFree(Session session) {
	}

	@Override
	public void dbUpdate(int _userID) {
	}
}
