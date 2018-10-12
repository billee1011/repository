package com.cai.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.cai.common.define.DbOpType;
import com.cai.common.define.DbStoreType;
import com.cai.common.define.EAccountParamType;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.ELogType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AccountParamModel;
import com.cai.common.domain.AccountSimple;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.AgentRecommend;
import com.cai.common.domain.AgentRecommendModel;
import com.cai.common.domain.DBUpdateDto;
import com.cai.common.domain.Event;
import com.cai.common.domain.GiveCardModel;
import com.cai.common.domain.HallRecommendModel;
import com.cai.common.domain.RecommendLimitModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.SpringService;
import com.cai.core.DataThreadPool;
import com.cai.core.MonitorEvent;
import com.cai.dictionary.RecommendLimitDict;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.domain.Session;
import com.cai.rmi.impl.CenterRMIServerImpl;
import com.google.common.collect.Maps;
import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountModelResponse;
import protobuf.redis.ProtoRedis.RsAccountResponse;

/**
 * 
 * 钻石代理
 *
 * @author Administrator date: 2017年8月21日 上午10:43:45 <br/>
 */
public class RecommendService extends AbstractService {

	private static Logger logger = LoggerFactory.getLogger(RecommendService.class);

	private static ConcurrentHashMap<String, HallRecommendModel> recommendMap;
	private static RecommendService instance;

	private RecommendService() {
	}

	public static RecommendService getInstance() {
		if (null == instance) {
			instance = new RecommendService();
			recommendMap = new ConcurrentHashMap<String, HallRecommendModel>();
		}
		return instance;
	}

	// 添加缓存
	public void pushRecommender(String key, HallRecommendModel model) {
		recommendMap.put(key, model);
	}

	// 入库的时候删除缓存
	public HallRecommendModel reduceRecommendMap(String key) {
		return recommendMap.remove(key);
	}

	// 查询缓存
	public boolean containsKey(String key) {
		return recommendMap.containsKey(key);
	}

	public void doAgentReceived(Account account, int money) {
		// 代理充值返利
		try {
			if (account == null || account.getAccountModel().getIs_agent() == 1) {
				return;
			}
			AccountModel accountModel = account.getAccountModel();
			// 钻石代理充值没有返利
			if (accountModel.getIs_agent() == 0 || accountModel.getRecommend_agent_id() == 0 || accountModel.getProxy_level() == 1) {
				return;
			}
			if (accountModel.getIs_agent() > 0) {
				Account recommendAccount = PublicServiceImpl.getInstance().getAccount(accountModel.getRecommend_agent_id());
				AccountModel recommendModel = recommendAccount.getAccountModel();
				// 上级非代理或者为3级代理，没有返利
				if (recommendModel.getProxy_level() == 0 || recommendModel.getProxy_level() == 3) {
					return;
				}
				int level = recommendModel.getProxy_level();
				if (account.getGame_id() == 0) {
					account.setGame_id(6);
				}
				SysParamModel sysParamModel2222 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2222);
				// SysParamModel sysParamModel2222 =
				// SysParamDict.getInstance().getSysParamModelDictionaryByGameId(account.getGame_id()).get(2222);
				if (sysParamModel2222 == null) {
					return;
				}
				double totalReceiveMoney = money * sysParamModel2222.getVal1() / 10000.00;
				if (level == 1) {// 返利10%
					logger.info(recommendAccount.getAccount_id() + " 钻石代理下的代理充值金额：" + money + " 返利:" + totalReceiveMoney);
					doAgentIncome(recommendAccount.getAccount_id(), totalReceiveMoney, 4l, "钻石代理下的代理充值", EGoldOperateType.AGENT_RECEIVE,
							account.getAccount_id(), money);
					return;
				} else if (level == 2) {
					logger.info(
							recommendAccount.getAccount_id() + " 黄金代理下的代理充值金额：" + money + " 返利:" + money * sysParamModel2222.getVal2() / 10000.00);
					doAgentIncome(recommendAccount.getAccount_id(), money * sysParamModel2222.getVal2() / 10000.0, 4l, "黄金代理下的代理充值",
							EGoldOperateType.AGENT_RECEIVE, account.getAccount_id(), money);
					if (recommendAccount.getAccountModel().getRecommend_agent_id() != 0) {
						Account recommendAccountUp = PublicServiceImpl.getInstance()
								.getAccount(recommendAccount.getAccountModel().getRecommend_agent_id());
						if (recommendAccountUp.getAccountModel().getProxy_level() == 1) {
							logger.info(recommendAccount.getAccount_id() + " 黄金代理下的代理充值金额：" + money + " 返利:"
									+ money * sysParamModel2222.getVal3() / 10000.00);
							doAgentIncome(recommendAccountUp.getAccount_id(), money * sysParamModel2222.getVal3() / 10000.00, 5l, "黄金代理下的代理充值",
									EGoldOperateType.AGENT_RECEIVE, account.getAccount_id(), money);
							return;
						}
					}
				}
			}
		} catch (Exception e) {

		}
	}

	/**
	 * 钻石推广员收益操作
	 * 
	 * @param account_id
	 * @param income
	 * @param level
	 * @param desc
	 * @param eGoldOperateType
	 * @param targetId
	 * @return
	 */
	public AddGoldResultModel doAgentIncome(long account_id, double income, long level, String desc, EGoldOperateType eGoldOperateType, long targetId,
			long rechargeMoney) {
		AddGoldResultModel addGoldResultModel = new AddGoldResultModel();
		addGoldResultModel.setSuccess(false);
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		Account account = centerRMIServer.getAccount(account_id);
		if (account == null) {
			addGoldResultModel.setMsg("账号不存在");
			return addGoldResultModel;
		}
		ReentrantLock lock = account.getRedisLock();
		lock.lock();
		try {
			AccountModel accountModel = account.getAccountModel();
			addGoldResultModel.setAccountModel(accountModel);
			double oldValue = accountModel.getRecommend_remain_income();
			if (income > 0) {
				accountModel.setRecommend_remain_income(accountModel.getRecommend_remain_income() + income);
				accountModel.setRecommend_history_income(accountModel.getRecommend_history_income() + income);
				addGoldResultModel.setSuccess(true);
			} else {
				if (income < 0) {
					if (accountModel.getIs_rebate() != 1) {
						addGoldResultModel.setMsg("您未开通提现功能");
						return addGoldResultModel;
					}
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
			}

			// 现金操作操作直接入库
			DataThreadPool.getInstance().addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.UPDATE, "updateAccountModel", accountModel));
			double change = (accountModel.getRecommend_remain_income() - oldValue) * 100;// 元换成分
			double newValue = accountModel.getRecommend_remain_income() * 100;
			// 日志
			StringBuffer buf = new StringBuffer();
			buf.append("|");
			if (change > 0) {
				buf.append("增加[" + change + "]");
				buf.append(",值变化:[").append(oldValue * 100.0).append("]->[").append(newValue).append("]分");
				desc = desc + buf.toString();
				MongoDBServiceImpl.getInstance().log_agent_recommend(account_id, ELogType.agentIncome, desc, (long) change, level, null, targetId,
						rechargeMoney);
			} else {
				buf.append("减少[" + change + "]");
				buf.append(",值变化:[").append(oldValue * 100.0).append("]->[").append(newValue).append("]分");
				desc = desc + buf.toString();
				MongoDBServiceImpl.getInstance().log_agent_recommend(account_id, ELogType.agentOutcome, desc, (long) change, level, null, targetId,
						rechargeMoney);
			}
			// SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT,
			// account_id + "", account);// 更新缓存
			// ========同步到中心========
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			//
			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			rsAccountResponseBuilder.setAccountId(account_id);
			//
			RsAccountModelResponse.Builder rsAccountModelResponseBuilder = RsAccountModelResponse.newBuilder();
			rsAccountModelResponseBuilder.setRecommendHistoryIncome(accountModel.getRecommend_history_income());
			rsAccountModelResponseBuilder.setRecommendReceiveIncome(accountModel.getRecommend_receive_income());
			rsAccountModelResponseBuilder.setRecommendRemainIncome(accountModel.getRecommend_remain_income());
			rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponseBuilder);
			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
					ERedisTopicType.topicProxy.getId() + account.getLastProxyIndex());
		} catch (Exception e) {
			logger.error("error", e);
		} finally {
			lock.unlock();
		}

		return addGoldResultModel;
	}

	// 查询推广员信息总览
	public Map<String, Object> queryRecommendAll(long account_id) {
		Account account = PublicServiceImpl.getInstance().getAccount(account_id);
		if (account == null) {
			return null;
		}
		AccountModel accountModel = account.getAccountModel();
		int level = accountModel.getProxy_level();
		if (level == 0) {
			return null;
		}
		Map<Long, AgentRecommendModel> recommendMap = account.getAgentRecommendModelMap();
		if (recommendMap == null || recommendMap.size() == 0) {
			return null;
		}
		Map<String, Object> map = Maps.newConcurrentMap();
		map = getRecommendTotal(account);
		// map.put("remainIncome", accountModel.getRecommend_remain_income());//
		// 可提现
		// map.put("receiveIncome",
		// accountModel.getRecommend_receive_income());// 已经提现
		// map.put("historyIncome",
		// accountModel.getRecommend_history_income());// 总收益记录
		map.put("level", level);// 玩家的等级
		if (level == 1) {
			// 黄金推广员人数
			if (account.getAgentRecommendModelMap() != null && account.getAgentRecommendModelMap().size() > 0) {
				map.put("secondAgentCount", account.getAgentRecommendModelMap().size());
				Map<Long, AgentRecommendModel> agentMap = account.getAgentRecommendModelMap();
				int size = 0;
				for (Long agentId : agentMap.keySet()) {
					Account downAccount = PublicServiceImpl.getInstance().getAccount(agentId);
					if (downAccount.getAgentRecommendModelMap() != null) {
						size += downAccount.getAgentRecommendModelMap().size();
					}
				}
				map.put("thirdAgentCount", size);
			} else {
				map.put("secondAgentCount", 0);
				map.put("thirdAgentCount", 0);
			}

		} else if (level == 2) {
			// 普通推广员人数
			map.put("thirdAgentCount", account.getAgentRecommendModelMap() != null ? account.getAgentRecommendModelMap().size() : 0);
		}
		return map;
	}

	public Map<String, Object> getRecommendTotal(Account targetAccount) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("account_id").is(targetAccount.getAccount_id()));
		query.addCriteria(Criteria.where("log_type").is(ELogType.agentIncome.getId()));// 钻石推广员获得的收益
		List<AgentRecommend> recommendIncomeList = mongoDBService.getMongoTemplate().find(query, AgentRecommend.class);
		if (recommendIncomeList == null || recommendIncomeList.size() == 0) {
			return Maps.newConcurrentMap();
		}
		double secondRecharge = 0.0;// 黄金代理充值
		double secondReceive = 0.0;// 黄金代理给我的返利
		double thirdRecharge = 0.0;// 代理充值
		double thirdReceive = 0.0;// 代理给我的返利
		AccountModel accountModel = targetAccount.getAccountModel();
		if (accountModel.getProxy_level() == 1) {
			for (AgentRecommend model : recommendIncomeList) {
				if (model.getV2() == 4l) {// 自己的代理充值返利
					secondRecharge += model.getRecharge_money();
					secondReceive += model.getV1();
				} else if (model.getV2() == 5l) {// 下级推广员充值返利
					thirdReceive += model.getV1();
					thirdRecharge += model.getRecharge_money();
				}
			}
		} else if (accountModel.getProxy_level() == 2) {
			for (AgentRecommend model : recommendIncomeList) {
				thirdReceive += model.getV1();
				thirdRecharge += model.getRecharge_money();
			}
		}
		Map<String, Object> map = Maps.newConcurrentMap();
		map.put("secondRecharge", secondRecharge / 100.00);
		map.put("secondReceive", secondReceive / 100.00);
		map.put("thirdRecharge", thirdRecharge / 100.00);
		map.put("thirdReceive", thirdReceive / 100.00);
		return map;
	}

	// 查询下级推广员详情
	public Map<String, Object> queryDownRecommend(long account_id, Date startDate, Date endDate) {

		Account account = PublicServiceImpl.getInstance().getAccount(account_id);
		if (account == null) {
			return null;
		}

		String start = DateFormatUtils.format(startDate, "yyyy-MM-dd");
		String end = DateFormatUtils.format(endDate, "yyyy-MM-dd");
		int level = account.getAccountModel().getProxy_level();
		if (level == 0) {
			return null;
		}
		Map<Long, AgentRecommendModel> recommendMap = account.getAgentRecommendModelMap();
		if (recommendMap == null || recommendMap.size() == 0) {
			return null;
		}
		// 在从缓存中取值的时候，就根据日期进行排序
		List<AgentRecommendModel> lisRecommend = new ArrayList<AgentRecommendModel>(recommendMap.values());// 所有的推广员
		Collections.sort(lisRecommend, new Comparator<AgentRecommendModel>() {
			public int compare(AgentRecommendModel o1, AgentRecommendModel o2) {
				// 按照时间进行排列
				String time1 = o1.getUpdate_time();
				String time2 = o2.getUpdate_time();
				if (time1.compareTo(time2) < 0) {
					return 1;
				}
				if (time1.compareTo(time2) == 0) {
					return 0;
				}
				return -1;
			}
		});

		Map<String, Object> detailsParam = Maps.newConcurrentMap();
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		CenterRMIServerImpl centerRMIServer = SpringService.getBean(CenterRMIServerImpl.class);
		BasicDBList values = new BasicDBList();
		if (level == 1) {
			for (AgentRecommendModel model : lisRecommend) {
				// 下级玩家是否在当前查询月份内
				if (model.getUpdate_time().compareTo(start) >= 0 && model.getUpdate_time().compareTo(end) <= 0) {
					values.add(model.getTarget_account_id());
					Map<String, Object> details = Maps.newConcurrentMap();
					// 当前代理账号信息
					Account subAccount = centerRMIServer.getAccount(model.getTarget_account_id());
					details.put("accountId", model.getTarget_account_id());
					details.put("nickName", subAccount.getAccountWeixinModel() == null ? "-" : subAccount.getAccountWeixinModel().getNickname());
					if (subAccount.getAgentRecommendModelMap() != null) {
						details.put("subCount", subAccount.getAgentRecommendModelMap().size());// 下级代理人数
					} else {
						details.put("subCount", 0);// 下级代理人数
					}
					details.put("totalReceive", subAccount.getAccountModel().getRecommend_history_income());// 累计返利
					details.put("createTime", model.getUpdate_time());
					list.add(details);
				}
			}
			if (list.size() > 0) {
				HashMap<Long, Long> map = getTotalRechargeByAccountIds(values);
				for (Map<String, Object> details : list) {
					long accountId = (long) details.get("accountId");
					if (map.containsKey(accountId)) {
						details.put("totalRecharge", map.get(accountId) / 100.00);
					} else {
						details.put("totalRecharge", 0);
					}
				}
			}
			detailsParam.put("total", lisRecommend.size());// 总人数
			detailsParam.put("data", list);
		} else if (level == 2) {
			HashMap<Long, Long> rechargeMap = getTotalGroupBySubAccountId(account_id, 1);
			for (AgentRecommendModel model : lisRecommend) {
				// 下级玩家是否在当前查询月份内
				if (model.getAgent_level() > 0) {
					if (model.getUpdate_time().compareTo(start) >= 0 && model.getUpdate_time().compareTo(end) <= 0) {
						Map<String, Object> details = Maps.newConcurrentMap();
						// 当前代理账号信息
						Account subAccount = centerRMIServer.getAccount(model.getTarget_account_id());
						details.put("accountId", model.getTarget_account_id());
						details.put("nickName", subAccount.getAccountWeixinModel() == null ? "-" : subAccount.getAccountWeixinModel().getNickname());
						details.put("subCount", 0);// 下级代理人数
						details.put("totalRecharge", rechargeMap == null ? 0 : rechargeMap.get(model.getTarget_account_id()) / 100.00);// 累计充值
						details.put("totalReceive", subAccount.getAccountModel().getRecommend_history_income());// 累计返利
						details.put("createTime", model.getUpdate_time());
						list.add(details);
					}
				}
			}
			detailsParam.put("total", lisRecommend.size());// 总人数
			detailsParam.put("data", list);
		}
		return detailsParam;
	}

	public HashMap<Long, Long> getTotalGroupBySubAccountId(long accountId, int type) {
		AggregationOperation match = Aggregation.match(Criteria.where("account_id").is(accountId).and("log_type").is(ELogType.agentIncome.getId()));
		AggregationOperation group = null;
		if (type == 1) {
			group = Aggregation.group("target_id").sum("recharge_money").as("count").count().as("line");
		} else {
			group = Aggregation.group("target_id").sum("v1").as("count").count().as("line");
		}
		Aggregation aggregation = Aggregation.newAggregation(match, group);
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "agent_recommend", GiveCardModel.class);
		List<GiveCardModel> sumLlist = result.getMappedResults();
		HashMap<Long, Long> map = new HashMap<Long, Long>();
		if (sumLlist != null && sumLlist.size() > 0) {
			for (GiveCardModel giveCardModel : sumLlist) {
				map.put(giveCardModel.get_id(), giveCardModel.getCount());
			}
		}
		return map;
	}

	public HashMap<Long, Long> getTotalRechargeByAccountIds(BasicDBList values) {
		AggregationOperation match = Aggregation.match(Criteria.where("account_id").in(values).and("log_type").is(ELogType.agentIncome.getId()));
		AggregationOperation group = Aggregation.group("account_id").sum("recharge_money").as("count").count().as("line");
		Aggregation aggregation = Aggregation.newAggregation(match, group);
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "agent_recommend", GiveCardModel.class);
		List<GiveCardModel> sumLlist = result.getMappedResults();
		HashMap<Long, Long> map = new HashMap<Long, Long>();
		if (sumLlist != null && sumLlist.size() > 0) {
			for (GiveCardModel giveCardModel : sumLlist) {
				map.put(giveCardModel.get_id(), giveCardModel.getCount());
			}
		}
		return map;
	}

	public long getTotalReceiveByAccountId(long accountId) {
		AggregationOperation match = Aggregation.match(Criteria.where("account_id").is(accountId).and("log_type").is(ELogType.agentIncome.getId()));
		AggregationOperation group = Aggregation.group().sum("v1").as("count").count().as("line");
		Aggregation aggregation = Aggregation.newAggregation(match, group);
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "agent_recommend", GiveCardModel.class);
		List<GiveCardModel> sumLlist = result.getMappedResults();
		long totalBalance = 0;// 累计提现金额
		if (sumLlist != null && sumLlist.size() > 0) {
			GiveCardModel giveCardModel = sumLlist.get(0);
			totalBalance = giveCardModel.getCount();
		}
		return totalBalance;
	}

	// 大厅二期推广员

	public void doHallRecommendReceived(Account account, int money) {
		// 代理充值返利
		try {
			if (account == null || account.getAccountModel().getIs_agent() == 0) {
				return;
			}
			HallRecommendModel hallRecommendModel = account.getHallRecommendModel();
			if (hallRecommendModel.getAccount_id() == 0) {// 没有推荐人
				return;
			}
			// 上级推荐人
			Account upAccount = PublicServiceImpl.getInstance().getAccount(hallRecommendModel.getAccount_id());
			if (upAccount == null) {
				return;
			}
			int upLevel = upAccount.getHallRecommendModel().getRecommend_level();
			if (upLevel == 0) {// 上级推荐人的身份已经被取消
				return;
			}
			SysParamModel sysParamModel5000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(5000);
			long gameId = sysParamModel5000.getVal1();
			SysParamModel sysParamModel2223 = null;
			if (gameId == 7) {
				sysParamModel2223 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(7).get(2223);
			} else if (gameId == 6) {
				sysParamModel2223 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2223);
			}
			if (sysParamModel2223 == null) {
				return;
			}
			if (upLevel == 1) {
				double receive = 0.00;
				if (hallRecommendModel.getRecommend_level() == 0) {
					RecommendLimitModel model = RecommendLimitDict.getInstance().getRecommendLimitModelById(upAccount.getAccount_id());
					if (model != null) {
						receive = money * model.getAgent_receive_per() / 10000.00;// 直属代理返利
					} else {
						receive = money * sysParamModel2223.getVal1() / 10000.00;// 直属代理返利
					}
					doHallRecommendIncome(upAccount.getAccount_id(), receive, 1, "直属代理充值", EGoldOperateType.AGENT_RECHARGE_RECEIVER,
							account.getAccount_id(), money, account.getAccount_id());
				} else {
					RecommendLimitModel model = RecommendLimitDict.getInstance().getRecommendLimitModelById(upAccount.getAccount_id());
					if (model != null) {
						receive = money * model.getRecom_receive_per() / 10000.00;// 下级推广员充值
					} else {
						receive = money * sysParamModel2223.getVal4() / 10000.00;
					}
					doHallRecommendIncome(upAccount.getAccount_id(), receive, 4, "下级推广员充值", EGoldOperateType.AGENT_RECHARGE_RECEIVER,
							account.getAccount_id(), money, account.getAccount_id());
				}
				if (upAccount.getHallRecommendModel().getTop_id() > 0) {
					doHallRecommendIncome(upAccount.getHallRecommendModel().getTop_id(), money * 3 / 10000.00, 10, "钻石+返利",
							EGoldOperateType.AGENT_RECHARGE_RECEIVER, account.getAccount_id(), money, upAccount.getAccount_id());
				}

			} else if (upLevel == 2) {
				double receive1 = 0.00;// 直属代理返利
				if (hallRecommendModel.getRecommend_level() == 0) {
					RecommendLimitModel model = RecommendLimitDict.getInstance().getRecommendLimitModelById(upAccount.getAccount_id());
					if (model != null) {
						receive1 = money * model.getAgent_receive_per() / 10000.00;// 直属代理返利
					} else {
						receive1 = money * sysParamModel2223.getVal2() / 10000.00;// 直属代理返利
					}
					doHallRecommendIncome(upAccount.getAccount_id(), receive1, 1, "直属代理充值", EGoldOperateType.AGENT_RECHARGE_RECEIVER,
							account.getAccount_id(), money, account.getAccount_id());
					HallRecommendModel upHallRecommendModel = upAccount.getHallRecommendModel();
					if (upHallRecommendModel.getAccount_id() == 0) {// 没有推荐人
						return;
					}
					Account upUpAccount = PublicServiceImpl.getInstance().getAccount(upHallRecommendModel.getAccount_id());
					if (upUpAccount == null) {
						return;
					}
					int upUpLevel = upUpAccount.getHallRecommendModel().getRecommend_level();
					if (upUpLevel == 0) {// 上级推荐人的身份已经被取消
						return;
					}
					double receive2 = 0;
					RecommendLimitModel model2 = RecommendLimitDict.getInstance().getRecommendLimitModelById(upUpAccount.getAccount_id());
					if (model2 != null) {
						receive2 = money * model2.getTwo_gen_agent_receive_per() / 10000.00;// 直属代理返利
					} else {
						receive2 = money * sysParamModel2223.getVal4() / 10000.00;// 给上级返利额度
					}
					doHallRecommendIncome(upUpAccount.getAccount_id(), receive2, 2, "下级推广员的代理充值", EGoldOperateType.AGENT_RECHARGE_RECEIVER,
							account.getAccount_id(), money, upAccount.getAccount_id());
					if (upUpAccount.getHallRecommendModel().getTop_id() > 0) {
						doHallRecommendIncome(upUpAccount.getHallRecommendModel().getTop_id(), money * 3 / 10000.00, 10, "钻石+返利",
								EGoldOperateType.AGENT_RECHARGE_RECEIVER, account.getAccount_id(), money, upUpAccount.getAccount_id());
					}
				} else {
					RecommendLimitModel model = RecommendLimitDict.getInstance().getRecommendLimitModelById(upAccount.getAccount_id());
					if (model != null) {
						receive1 = money * model.getRecom_receive_per() / 10000.00;// 下级推广员充值
					} else {
						receive1 = money * sysParamModel2223.getVal5() / 10000.00;// 下级推广员充值
					}
					doHallRecommendIncome(upAccount.getAccount_id(), receive1, 4, "下级推广员充值", EGoldOperateType.AGENT_RECHARGE_RECEIVER,
							account.getAccount_id(), money, account.getAccount_id());
					HallRecommendModel upHallRecommendModel = upAccount.getHallRecommendModel();
					if (upHallRecommendModel.getAccount_id() == 0) {// 没有推荐人
						return;
					}
					Account upUpAccount = PublicServiceImpl.getInstance().getAccount(upHallRecommendModel.getAccount_id());
					if (upUpAccount == null) {
						return;
					}
					int upUpLevel = upUpAccount.getHallRecommendModel().getRecommend_level();
					if (upUpLevel == 0) {// 上级推荐人的身份已经被取消
						return;
					}
					double receive2 = 0;
					RecommendLimitModel model2 = RecommendLimitDict.getInstance().getRecommendLimitModelById(upUpAccount.getAccount_id());
					if (model2 != null) {
						receive2 = money * model2.getTwo_gen_recom_receive_per() / 10000.00;// 直属代理返利
					} else {
						receive2 = money * sysParamModel2223.getVal4() / 10000.00;// 给上级返利额度
					}
					doHallRecommendIncome(upUpAccount.getAccount_id(), receive2, 5, "下级-下级推广员充值", EGoldOperateType.AGENT_RECHARGE_RECEIVER,
							account.getAccount_id(), money, upAccount.getAccount_id());
					if (upUpAccount.getHallRecommendModel().getTop_id() > 0) {
						doHallRecommendIncome(upUpAccount.getHallRecommendModel().getTop_id(), money * 3 / 10000.00, 10, "钻石+返利",
								EGoldOperateType.AGENT_RECHARGE_RECEIVER, account.getAccount_id(), money, upUpAccount.getAccount_id());
					}
				}

			} else if (upLevel == 3) {
				double receive1 = 0;
				RecommendLimitModel model = RecommendLimitDict.getInstance().getRecommendLimitModelById(upAccount.getAccount_id());
				if (model != null) {
					receive1 = money * model.getAgent_receive_per() / 10000.00;// 直属代理返利
				} else {
					receive1 = money * sysParamModel2223.getVal3() / 10000.00;// 直属代理返利
				}
				doHallRecommendIncome(upAccount.getAccount_id(), receive1, 1, "直属代理充值", EGoldOperateType.AGENT_RECHARGE_RECEIVER,
						account.getAccount_id(), money, account.getAccount_id());
				HallRecommendModel upHallRecommendModel = upAccount.getHallRecommendModel();
				if (upHallRecommendModel.getAccount_id() == 0) {// 没有推荐人
					return;
				}
				Account upUpAccount = PublicServiceImpl.getInstance().getAccount(upHallRecommendModel.getAccount_id());
				if (upUpAccount == null) {
					return;
				}
				int upUpLevel = upUpAccount.getHallRecommendModel().getRecommend_level();
				if (upUpLevel == 0) {// 上级推荐人的身份已经被取消
					return;
				}
				double receive2 = 0;
				RecommendLimitModel model2 = RecommendLimitDict.getInstance().getRecommendLimitModelById(upUpAccount.getAccount_id());
				if (model2 != null) {
					receive2 = money * model2.getTwo_gen_agent_receive_per() / 10000.00;// 直属代理返利
				} else {
					receive2 = money * sysParamModel2223.getVal5() / 10000.00;// 给上级返利额度
				}
				doHallRecommendIncome(upUpAccount.getAccount_id(), receive2, 2, "下级推广员的代理充值", EGoldOperateType.AGENT_RECHARGE_RECEIVER,
						account.getAccount_id(), money, upAccount.getAccount_id());
				HallRecommendModel upUpHallRecommendModel = upUpAccount.getHallRecommendModel();
				if (upUpHallRecommendModel.getAccount_id() == 0) {// 没有推荐人
					return;
				}
				Account upUpUpAccount = PublicServiceImpl.getInstance().getAccount(upUpHallRecommendModel.getAccount_id());
				if (upUpUpAccount == null) {
					return;
				}
				int upUpUpLevel = upUpUpAccount.getHallRecommendModel().getRecommend_level();
				if (upUpUpLevel == 0) {// 上级推荐人的身份已经被取消
					return;
				}
				double receive3 = 0;
				RecommendLimitModel model3 = RecommendLimitDict.getInstance().getRecommendLimitModelById(upUpUpAccount.getAccount_id());
				if (model3 != null) {
					receive3 = money * model3.getTwo_gen_agent_receive_per() / 10000.00;// 直属代理返利
				} else {
					receive3 = money * sysParamModel2223.getVal4() / 10000.00;// 给上级返利额度
				}
				doHallRecommendIncome(upUpUpAccount.getAccount_id(), receive3, 3, "下级-下级推广员的代理充值", EGoldOperateType.AGENT_RECHARGE_RECEIVER,
						account.getAccount_id(), money, upUpAccount.getAccount_id());
				if (upUpUpAccount.getHallRecommendModel().getTop_id() > 0) {
					doHallRecommendIncome(upUpUpAccount.getHallRecommendModel().getTop_id(), money * 3 / 10000.00, 10, "钻石+返利",
							EGoldOperateType.AGENT_RECHARGE_RECEIVER, account.getAccount_id(), money, upUpUpAccount.getAccount_id());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void doRecommenPlayerReceived(Account account, int money) {
		// 推广玩家返利
		try {
			if (account == null) {
				return;
			}
			AccountParamModel accountParamModel = getAccountParamModel(account, EAccountParamType.RECOMMEND_PLAYER_RECEIVE);
			// val1=1获得返利资格
			if (accountParamModel.getVal1() != 1) {
				return;
			}
			HallRecommendModel hallRecommendModel = account.getHallRecommendModel();
			if (hallRecommendModel.getAccount_id() == 0) {// 没有推荐人
				return;
			}
			// 上级推荐人
			Account upAccount = PublicServiceImpl.getInstance().getAccount(hallRecommendModel.getAccount_id());
			// 上级推荐人不存在了，可能是换了靓号
			if (upAccount == null) {
				return;
			}
			int upLevel = upAccount.getHallRecommendModel().getRecommend_level();
			if (upLevel == 0) {// 上级推荐人的身份已经被取消
				return;
			}
			SysParamModel sysParamModel2225 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(7).get(2225);
			if (sysParamModel2225 == null) {
				return;
			}
			if (upLevel == 1) {
				double receive = 0.00;
				receive = sysParamModel2225.getVal1() / 100.00;// 直属代理返利
				doHallRecommendIncome(upAccount.getAccount_id(), receive, 7, "推荐玩家", EGoldOperateType.RECOMMEN_RECEIVE, account.getAccount_id(),
						money, account.getAccount_id());
			} else if (upLevel == 2) {
				double receive1 = 0.00;// 直属代理返利
				receive1 = sysParamModel2225.getVal2() / 100.00;// 直属代理返利
				doHallRecommendIncome(upAccount.getAccount_id(), receive1, 8, "推荐玩家", EGoldOperateType.RECOMMEN_RECEIVE, account.getAccount_id(),
						money, account.getAccount_id());
				// HallRecommendModel upHallRecommendModel =
				// upAccount.getHallRecommendModel();
				// if (upHallRecommendModel.getAccount_id() == 0) {// 没有推荐人
				// return;
				// }
				// Account upUpAccount =
				// PublicServiceImpl.getInstance().getAccount(upHallRecommendModel.getAccount_id());
				// int upUpLevel =
				// upUpAccount.getHallRecommendModel().getRecommend_level();
				// if (upUpLevel == 0) {// 上级推荐人的身份已经被取消
				// return;
				// }
				// double receive2 = money * sysParamModel2223.getVal4() /
				// 10000.00;// 给上级返利额度
				// doHallRecommendIncome(upUpAccount.getAccount_id(), receive2,
				// 8, "下级推荐玩家", EGoldOperateType.RECOMMEN_RECEIVE,
				// account.getAccount_id(), money, upAccount.getAccount_id());

			} else if (upLevel == 3) {
				double receive1 = sysParamModel2225.getVal3() / 100.00;// 直属代理返利
				doHallRecommendIncome(upAccount.getAccount_id(), receive1, 9, "推荐玩家", EGoldOperateType.RECOMMEN_RECEIVE, account.getAccount_id(),
						money, account.getAccount_id());
				// HallRecommendModel upHallRecommendModel =
				// upAccount.getHallRecommendModel();
				// if (upHallRecommendModel.getAccount_id() == 0) {// 没有推荐人
				// return;
				// }
				// Account upUpAccount =
				// PublicServiceImpl.getInstance().getAccount(upHallRecommendModel.getAccount_id());
				// int upUpLevel =
				// upUpAccount.getHallRecommendModel().getRecommend_level();
				// if (upUpLevel == 0) {// 上级推荐人的身份已经被取消
				// return;
				// }
				// double receive2 = money * sysParamModel2223.getVal5() /
				// 10000.00;// 给上级返利额度
				// doHallRecommendIncome(upUpAccount.getAccount_id(), receive2,
				// 8, "下级推广员推广玩家", EGoldOperateType.RECOMMEN_RECEIVE,
				// account.getAccount_id(), money, upAccount.getAccount_id());
				// HallRecommendModel upUpHallRecommendModel =
				// upUpAccount.getHallRecommendModel();
				// if (upUpHallRecommendModel.getAccount_id() == 0) {// 没有推荐人
				// return;
				// }
				// Account upUpUpAccount =
				// PublicServiceImpl.getInstance().getAccount(upUpHallRecommendModel.getAccount_id());
				// int upUpUpLevel =
				// upUpUpAccount.getHallRecommendModel().getRecommend_level();
				// if (upUpUpLevel == 0) {// 上级推荐人的身份已经被取消
				// return;
				// }
				// double receive3 = money * sysParamModel2223.getVal4() /
				// 10000.00;// 给上级返利额度
				// doHallRecommendIncome(upUpUpAccount.getAccount_id(),
				// receive3, 9, "下级-下级推广员推广玩家",
				// EGoldOperateType.RECOMMEN_RECEIVE,
				// account.getAccount_id(), money, upUpAccount.getAccount_id());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public AccountParamModel getAccountParamModel(Account account, EAccountParamType eAccountParamType) {
		AccountParamModel accountParamModel = account.getAccountParamModelMap().get(eAccountParamType.getId());
		if (accountParamModel == null) {
			accountParamModel = new AccountParamModel();
			accountParamModel.setAccount_id(account.getAccount_id());
			accountParamModel.setType(eAccountParamType.getId());
			account.getAccountParamModelMap().put(eAccountParamType.getId(), accountParamModel);
		}
		return accountParamModel;
	}

	/**
	 * 钻石推广员收益操作
	 * 
	 * @param account_id
	 * @param income
	 * @param level
	 * @param desc
	 * @param eGoldOperateType
	 * @param targetId
	 * @return
	 */
	public AddGoldResultModel doHallRecommendIncome(long account_id, double income, long level, String desc, EGoldOperateType eGoldOperateType,
			long targetId, long rechargeMoney, long sourceId) {
		AddGoldResultModel addGoldResultModel = new AddGoldResultModel();
		addGoldResultModel.setSuccess(false);
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		Account account = centerRMIServer.getAccount(account_id);
		if (account == null) {
			addGoldResultModel.setMsg("账号不存在");
			return addGoldResultModel;
		}
		ReentrantLock lock = account.getRedisLock();
		lock.lock();
		try {
			AccountModel accountModel = account.getAccountModel();
			addGoldResultModel.setAccountModel(accountModel);
			double oldValue = accountModel.getRecommend_remain_income();
			if (income > 0) {
				accountModel.setRecommend_remain_income(accountModel.getRecommend_remain_income() + income);
				accountModel.setRecommend_history_income(accountModel.getRecommend_history_income() + income);
				addGoldResultModel.setSuccess(true);
			} else {
				if (income < 0) {
					if (accountModel.getIs_rebate() != 1) {
						addGoldResultModel.setMsg("您未开通提现功能");
						return addGoldResultModel;
					}
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
			}

			// 现金操作操作直接入库
			DataThreadPool.getInstance().addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.UPDATE, "updateAccountModel", accountModel));
			double change = (accountModel.getRecommend_remain_income() - oldValue) * 100;// 元换成分
			double newValue = accountModel.getRecommend_remain_income() * 100;
			// 日志
			StringBuffer buf = new StringBuffer();
			buf.append("|");
			if (change > 0) {
				buf.append("增加[" + change + "]");
				buf.append(",值变化:[").append(oldValue * 100.0).append("]->[").append(newValue).append("]分");
				desc = desc + buf.toString();
				MongoDBServiceImpl.getInstance().log_hall_recommend(account_id, ELogType.agentIncome, desc, (long) change, level, null, targetId,
						rechargeMoney, sourceId);
			} else {
				buf.append("减少[" + change + "]");
				buf.append(",值变化:[").append(oldValue * 100.0).append("]->[").append(newValue).append("]分");
				desc = desc + buf.toString();
				MongoDBServiceImpl.getInstance().log_hall_recommend(account_id, ELogType.agentOutcome, desc, (long) change, level, null, targetId,
						rechargeMoney, sourceId);
			}
			// SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT,
			// account_id + "", account);// 更新缓存
			// ========同步到中心========
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			//
			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			rsAccountResponseBuilder.setAccountId(account_id);
			//
			RsAccountModelResponse.Builder rsAccountModelResponseBuilder = RsAccountModelResponse.newBuilder();
			rsAccountModelResponseBuilder.setRecommendHistoryIncome(accountModel.getRecommend_history_income());
			rsAccountModelResponseBuilder.setRecommendReceiveIncome(accountModel.getRecommend_receive_income());
			rsAccountModelResponseBuilder.setRecommendRemainIncome(accountModel.getRecommend_remain_income());
			rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponseBuilder);
			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
					ERedisTopicType.topicProxy.getId() + account.getLastProxyIndex());
		} catch (Exception e) {
			logger.error("error", e);
		} finally {
			lock.unlock();
		}

		return addGoldResultModel;
	}

	// 查询下级推广员详情
	public Map<String, Object> queryHallDownRecommend(long account_id, Date startDate, Date endDate) {

		Account account = PublicServiceImpl.getInstance().getAccount(account_id);
		if (account == null) {
			return null;
		}

		String start = DateFormatUtils.format(startDate, "yyyy-MM-dd");
		String end = DateFormatUtils.format(endDate, "yyyy-MM-dd");
		int level = account.getAccountModel().getProxy_level();
		if (level == 0) {
			return null;
		}
		Map<Long, HallRecommendModel> recommendMap = account.getHallRecommendModelMap();
		if (recommendMap == null || recommendMap.size() == 0) {
			return null;
		}
		// 在从缓存中取值的时候，就根据日期进行排序
		List<HallRecommendModel> lisRecommend = new ArrayList<HallRecommendModel>(recommendMap.values());// 所有的推广员
		Collections.sort(lisRecommend, new Comparator<HallRecommendModel>() {
			public int compare(HallRecommendModel o1, HallRecommendModel o2) {
				// 按照时间进行排列
				String time1 = o1.getUpdate_time();
				String time2 = o2.getUpdate_time();
				if (time1.compareTo(time2) < 0) {
					return 1;
				}
				if (time1.compareTo(time2) == 0) {
					return 0;
				}
				return -1;
			}
		});

		Map<String, Object> detailsParam = Maps.newConcurrentMap();
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		CenterRMIServerImpl centerRMIServer = SpringService.getBean(CenterRMIServerImpl.class);
		int total = 0;
		BasicDBList values = new BasicDBList();
		if (level == 1) {
			for (HallRecommendModel model : lisRecommend) {
				if (model.getRecommend_level() == 0) {
					// 非推广员排除掉
					continue;
				}
				total++;
				// 下级玩家是否在当前查询月份内
				if (model.getUpdate_time().compareTo(start) >= 0 && model.getUpdate_time().compareTo(end) <= 0) {
					values.add(model.getTarget_account_id());
					Map<String, Object> details = Maps.newConcurrentMap();
					// 当前代理账号信息
					Account subAccount = centerRMIServer.getAccount(model.getTarget_account_id());
					if (subAccount == null) {
						continue;
					}
					details.put("accountId", model.getTarget_account_id());
					details.put("nickName", subAccount.getAccountWeixinModel() == null ? "-" : subAccount.getAccountWeixinModel().getNickname());
					HashMap<Integer, Integer> map = this.agentCount(subAccount.getHallRecommendModelMap());
					details.put("subCount", map.get(2));// 下级推广员人数
					details.put("agentCount", map.get(1));// 下级代理人数
					details.put("totalReceive", subAccount.getAccountModel().getRecommend_history_income());// 累计返利
					details.put("createTime", model.getUpdate_time());
					list.add(details);
				}
			}
		} else if (level == 2) {
			for (HallRecommendModel model : lisRecommend) {
				if (model.getRecommend_level() == 0) {
					// 非推广员排除掉
					continue;
				}
				total++;
				// 下级玩家是否在当前查询月份内
				if (model.getUpdate_time().compareTo(start) >= 0 && model.getUpdate_time().compareTo(end) <= 0) {
					values.add(model.getTarget_account_id());
					Map<String, Object> details = Maps.newConcurrentMap();
					// 当前代理账号信息
					Account subAccount = centerRMIServer.getAccount(model.getTarget_account_id());
					if (subAccount == null) {
						continue;
					}
					details.put("accountId", model.getTarget_account_id());
					details.put("nickName", subAccount.getAccountWeixinModel() == null ? "-" : subAccount.getAccountWeixinModel().getNickname());
					HashMap<Integer, Integer> map = this.agentCount(subAccount.getHallRecommendModelMap());
					details.put("subCount", map.get(2));// 下级推广员人数
					details.put("agentCount", map.get(1));// 下级代理人数
					details.put("totalReceive", subAccount.getAccountModel().getRecommend_history_income());// 累计返利
					details.put("createTime", model.getUpdate_time());
					list.add(details);
				}
			}
		}
		if (list.size() > 0) {
			HashMap<Long, Long> map = getTotalSubRechargeByAccountIds(values, account.getAccount_id());
			HashMap<Long, Long> map2 = getTotalSubReceiveByAccountIds(values, account.getAccount_id());
			for (Map<String, Object> details : list) {
				long accountId = (long) details.get("accountId");
				if (map.containsKey(accountId)) {
					details.put("totalRecharge", map.get(accountId) / 100.00);
				} else {
					details.put("totalRecharge", 0);
				}
				if (map2.containsKey(accountId)) {
					details.put("totalReceive", map2.get(accountId) / 100.00);
				} else {
					details.put("totalReceive", 0);
				}
			}
		}
		detailsParam.put("total", total);// 总人数
		detailsParam.put("data", list);
		return detailsParam;
	}

	// 查询下级推广员详情
	public Map<String, Object> queryHallDownAgent(long account_id, Date startDate, Date endDate) {

		Account account = PublicServiceImpl.getInstance().getAccount(account_id);
		if (account == null) {
			return null;
		}

		String start = DateFormatUtils.format(startDate, "yyyy-MM-dd");
		String end = DateFormatUtils.format(endDate, "yyyy-MM-dd");
		int level = account.getAccountModel().getProxy_level();
		if (level == 0) {
			return null;
		}
		Map<Long, HallRecommendModel> recommendMap = account.getHallRecommendModelMap();
		if (recommendMap == null || recommendMap.size() == 0) {
			return null;
		}
		// 在从缓存中取值的时候，就根据日期进行排序
		List<HallRecommendModel> lisRecommend = new ArrayList<HallRecommendModel>(recommendMap.values());// 所有的推广员
		Collections.sort(lisRecommend, new Comparator<HallRecommendModel>() {
			public int compare(HallRecommendModel o1, HallRecommendModel o2) {
				// 按照时间进行排列
				String time1 = o1.getUpdate_time();
				String time2 = o2.getUpdate_time();
				if (time1.compareTo(time2) < 0) {
					return 1;
				}
				if (time1.compareTo(time2) == 0) {
					return 0;
				}
				return -1;
			}
		});

		Map<String, Object> detailsParam = Maps.newConcurrentMap();
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		int total = 0;
		BasicDBList values = new BasicDBList();
		for (HallRecommendModel model : lisRecommend) {
			if (model.getProxy_level() == 0 || model.getRecommend_level() > 0) {
				// 非下级代理排除掉
				continue;
			}
			total++;
			// 下级玩家是否在当前查询月份内
			if (model.getUpdate_time().compareTo(start) >= 0 && model.getUpdate_time().compareTo(end) <= 0) {
				values.add(model.getTarget_account_id());
				Map<String, Object> details = Maps.newConcurrentMap();
				// 当前代理账号信息
				// Account subAccount =
				// centerRMIServer.getAccount(model.getTarget_account_id());
				AccountSimple accountSimple = PublicServiceImpl.getInstance().getAccountSimpe(model.getTarget_account_id());
				if (accountSimple == null) {
					continue;
				}
				details.put("accountId", model.getTarget_account_id());
				details.put("nickName", accountSimple.getNick_name());
				details.put("headPic", accountSimple.getIcon());
				details.put("createTime", model.getUpdate_time());
				list.add(details);
			}
		}
		if (list.size() > 0) {
			HashMap<Long, Long> map = getTotalHallRechargeByAccountIds(values, account.getAccount_id());
			HashMap<Long, Long> map2 = getTotalHallReceiveByAccountIds(values, account.getAccount_id());
			for (Map<String, Object> details : list) {
				long accountId = (long) details.get("accountId");
				if (map.containsKey(accountId)) {
					details.put("totalRecharge", map.get(accountId) / 100.00);
				} else {
					details.put("totalRecharge", 0);
				}
				if (map2.containsKey(accountId)) {
					details.put("totalReceive", map2.get(accountId) / 100.00);
				} else {
					details.put("totalReceive", 0);
				}
			}
		}
		detailsParam.put("total", total);// 总人数
		detailsParam.put("data", list);
		return detailsParam;
	}

	// 查询我的玩家详情
	public Map<String, Object> queryMyPlayers(long account_id, Date startDate, Date endDate) {

		Account account = PublicServiceImpl.getInstance().getAccount(account_id);
		if (account == null) {
			return null;
		}

		String start = DateFormatUtils.format(startDate, "yyyy-MM-dd");
		String end = DateFormatUtils.format(endDate, "yyyy-MM-dd");
		int level = account.getAccountModel().getProxy_level();
		if (level == 0) {
			return null;
		}
		Map<Long, HallRecommendModel> recommendMap = account.getHallRecommendModelMap();
		if (recommendMap == null || recommendMap.size() == 0) {
			return null;
		}
		// 在从缓存中取值的时候，就根据日期进行排序
		List<HallRecommendModel> lisRecommend = new ArrayList<HallRecommendModel>(recommendMap.values());// 所有的推广员
		Collections.sort(lisRecommend, new Comparator<HallRecommendModel>() {
			public int compare(HallRecommendModel o1, HallRecommendModel o2) {
				// 按照时间进行排列
				String time1 = o1.getUpdate_time();
				String time2 = o2.getUpdate_time();
				if (time1.compareTo(time2) < 0) {
					return 1;
				}
				if (time1.compareTo(time2) == 0) {
					return 0;
				}
				return -1;
			}
		});

		Map<String, Object> detailsParam = Maps.newConcurrentMap();
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		// CenterRMIServerImpl centerRMIServer =
		// SpringService.getBean(CenterRMIServerImpl.class);
		int total = 0;
		BasicDBList values = new BasicDBList();
		for (HallRecommendModel model : lisRecommend) {
			if (model.getProxy_level() > 0 || model.getRecommend_level() > 0) {
				// 代理跟推广员排除掉
				continue;
			}
			total++;
			// 下级玩家是否在当前查询月份内
			if (model.getUpdate_time().compareTo(start) >= 0 && model.getUpdate_time().compareTo(end) <= 0) {
				values.add(model.getTarget_account_id());
				Map<String, Object> details = Maps.newConcurrentMap();
				// 当前代理账号信息
				// Account subAccount =
				// centerRMIServer.getAccount(model.getTarget_account_id());
				AccountSimple accountSimple = PublicServiceImpl.getInstance().getAccountSimpe(model.getTarget_account_id());
				if (accountSimple == null) {
					continue;
				}
				details.put("accountId", model.getTarget_account_id());
				details.put("nickName", accountSimple.getNick_name());
				details.put("createTime", model.getUpdate_time());
				details.put("isAgent", model.getProxy_level());
				list.add(details);
			}
		}
		detailsParam.put("total", total);// 总人数
		detailsParam.put("data", list);
		return detailsParam;
	}

	private HashMap<Integer, Integer> agentCount(Map<Long, HallRecommendModel> getHallRecommendModelMap) {
		int count = 0;
		int recommCount = 0;
		for (HallRecommendModel model : getHallRecommendModelMap.values()) {
			if (model.getRecommend_level() > 0) {
				recommCount++;
			}
			if (model.getRecommend_level() == 0 && model.getProxy_level() > 0) {
				count++;
			}
		}
		HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
		map.put(1, count);
		map.put(2, recommCount);
		return map;
	}

	public HashMap<Long, Long> getTotalHallReceiveByAccountIds(BasicDBList values, long accountId) {
		AggregationOperation match = Aggregation
				.match(Criteria.where("account_id").is(accountId).and("target_id").in(values).and("log_type").is(ELogType.agentIncome.getId()));
		AggregationOperation group = Aggregation.group("target_id").sum("v1").as("count").count().as("line");
		Aggregation aggregation = Aggregation.newAggregation(match, group);
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "hall_recommend", GiveCardModel.class);
		List<GiveCardModel> sumLlist = result.getMappedResults();
		HashMap<Long, Long> map = new HashMap<Long, Long>();
		if (sumLlist != null && sumLlist.size() > 0) {
			for (GiveCardModel giveCardModel : sumLlist) {
				map.put(giveCardModel.get_id(), giveCardModel.getCount());
			}
		}
		return map;
	}

	public HashMap<Long, Long> getTotalHallRechargeByAccountIds(BasicDBList values, long accountId) {
		AggregationOperation match = Aggregation
				.match(Criteria.where("account_id").is(accountId).and("target_id").in(values).and("log_type").is(ELogType.agentIncome.getId()));
		AggregationOperation group = Aggregation.group("target_id").sum("recharge_money").as("count").count().as("line");
		Aggregation aggregation = Aggregation.newAggregation(match, group);
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "hall_recommend", GiveCardModel.class);
		List<GiveCardModel> sumLlist = result.getMappedResults();
		HashMap<Long, Long> map = new HashMap<Long, Long>();
		if (sumLlist != null && sumLlist.size() > 0) {
			for (GiveCardModel giveCardModel : sumLlist) {
				map.put(giveCardModel.get_id(), giveCardModel.getCount());
			}
		}
		return map;
	}

	public HashMap<Long, Long> getTotalSubRechargeByAccountIds(BasicDBList values, long accountId) {
		AggregationOperation match = Aggregation
				.match(Criteria.where("account_id").is(accountId).and("source_id").in(values).and("log_type").is(ELogType.agentIncome.getId()));
		AggregationOperation group = Aggregation.group("source_id").sum("recharge_money").as("count").count().as("line");
		Aggregation aggregation = Aggregation.newAggregation(match, group);
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "hall_recommend", GiveCardModel.class);
		List<GiveCardModel> sumLlist = result.getMappedResults();
		HashMap<Long, Long> map = new HashMap<Long, Long>();
		if (sumLlist != null && sumLlist.size() > 0) {
			for (GiveCardModel giveCardModel : sumLlist) {
				map.put(giveCardModel.get_id(), giveCardModel.getCount());
			}
		}
		return map;
	}

	{
		DBObject groupFields = new BasicDBObject("_id", "$account_id");
		groupFields.put("total", new BasicDBObject("$sum", "$money"));
		DBObject group = new BasicDBObject("$group", groupFields);
		// sort
		DBObject sort = new BasicDBObject("$sort", new BasicDBObject("total", -1));
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
	}

	public HashMap<Long, GiveCardModel> getTotalRechargeByAccountIds(long accountId, Date start, Date end, BasicDBList v2, int pageSize, int page) {
		HashMap<Long, GiveCardModel> map = new HashMap<Long, GiveCardModel>();
		try {
			AggregationOperation match = Aggregation.match(Criteria.where("create_time").gte(start).lte(end).and("account_id").is(accountId)
					.and("log_type").is(ELogType.agentIncome.getId()).and("v2").in(v2));
			AggregationOperation group = Aggregation.group("source_id").sum("v1").as("count").sum("recharge_money").as("line");
			AggregationOperation sort = Aggregation.sort(new Sort(Sort.Direction.DESC, "count"));
			AggregationOperation limit = Aggregation.limit(pageSize);
			AggregationOperation skip = Aggregation.skip(pageSize * page);
			Aggregation aggregation = Aggregation.newAggregation(match, group, sort, limit, skip);
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "hall_recommend",
					GiveCardModel.class);
			List<GiveCardModel> sumLlist = result.getMappedResults();

			if (sumLlist != null && sumLlist.size() > 0) {
				for (GiveCardModel giveCardModel : sumLlist) {
					map.put(giveCardModel.get_id(), giveCardModel);
				}
			}
		} catch (Exception e) {
			logger.error("统计业绩排行榜出错 accountId=" + accountId, e);
		}
		return map;
	}

	public HashMap<Long, GiveCardModel> getNewTotalRechargeByAccountIds(long accountId, Date start, Date end, BasicDBList v2, int pageSize,
			int page) {
		HashMap<Long, GiveCardModel> map = new HashMap<Long, GiveCardModel>();
		try {
			AggregationOperation match = Aggregation.match(Criteria.where("create_time").gte(start).lte(end).and("account_id").is(accountId)
					.and("log_type").is(ELogType.agentIncome.getId()).and("v2").in(v2));
			AggregationOperation group = Aggregation.group("source_id").sum("v1").as("count").sum("recharge_money").as("line");
			AggregationOperation sort = Aggregation.sort(new Sort(Sort.Direction.DESC, "count"));
			AggregationOperation limit = Aggregation.limit(pageSize);
			AggregationOperation skip = Aggregation.skip(pageSize * page);
			Aggregation aggregation = Aggregation.newAggregation(match, group, sort, limit, skip);
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "new_hall_recommend",
					GiveCardModel.class);
			List<GiveCardModel> sumLlist = result.getMappedResults();

			if (sumLlist != null && sumLlist.size() > 0) {
				for (GiveCardModel giveCardModel : sumLlist) {
					map.put(giveCardModel.get_id(), giveCardModel);
				}
			}
		} catch (Exception e) {
			logger.error("统计业绩排行榜出错 accountId=" + accountId, e);
		}
		return map;
	}

	public HashMap<Long, Long> getTotalSubReceiveByAccountIds(BasicDBList values, long accountId) {
		AggregationOperation match = Aggregation
				.match(Criteria.where("account_id").is(accountId).and("source_id").in(values).and("log_type").is(ELogType.agentIncome.getId()));
		AggregationOperation group = Aggregation.group("source_id").sum("v1").as("count").count().as("line");
		Aggregation aggregation = Aggregation.newAggregation(match, group);
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "hall_recommend", GiveCardModel.class);
		List<GiveCardModel> sumLlist = result.getMappedResults();
		HashMap<Long, Long> map = new HashMap<Long, Long>();
		if (sumLlist != null && sumLlist.size() > 0) {
			for (GiveCardModel giveCardModel : sumLlist) {
				map.put(giveCardModel.get_id(), giveCardModel.getCount());
			}
		}
		return map;
	}

	/**
	 * 充值退单，收回返利
	 * 
	 * @return
	 */
	public void paybackReceive(long account_id, double income, String desc, long level, int rechargeMoney, long sourceId, long targetId) {
		AddGoldResultModel addGoldResultModel = new AddGoldResultModel();
		addGoldResultModel.setSuccess(false);
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		Account account = centerRMIServer.getAccount(account_id);
		if (account == null) {
			addGoldResultModel.setMsg("账号不存在");
			return;
		}
		ReentrantLock lock = account.getRedisLock();
		lock.lock();
		try {
			AccountModel accountModel = account.getAccountModel();
			addGoldResultModel.setAccountModel(accountModel);
			double oldValue = accountModel.getRecommend_remain_income();
			if (income > 0) {
				income = -income;
			}
			if (income == 0) {
				return;
			} else {
				double k = accountModel.getRecommend_remain_income() + income;
				accountModel.setRecommend_remain_income(k);
				accountModel.setRecommend_receive_income(accountModel.getRecommend_receive_income() - income);
				addGoldResultModel.setSuccess(true);
			}
			// 现金操作操作直接入库
			DataThreadPool.getInstance().addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.UPDATE, "updateAccountModel", accountModel));
			double change = (accountModel.getRecommend_remain_income() - oldValue) * 100;// 元换成分
			double newValue = accountModel.getRecommend_remain_income() * 100;
			// 日志
			StringBuffer buf = new StringBuffer();
			buf.append("|");

			buf.append("减少[" + change + "]");
			buf.append(",值变化:[").append(oldValue * 100.0).append("]->[").append(newValue).append("]分");
			desc = desc + buf.toString();
			MongoDBServiceImpl.getInstance().log_hall_recommend(account_id, ELogType.agentPayback, desc, (long) change, level, null, targetId,
					rechargeMoney, sourceId);
			// SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT,
			// account_id + "", account);// 更新缓存
			// ========同步到中心========
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			//
			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			rsAccountResponseBuilder.setAccountId(account_id);
			//
			RsAccountModelResponse.Builder rsAccountModelResponseBuilder = RsAccountModelResponse.newBuilder();
			rsAccountModelResponseBuilder.setRecommendHistoryIncome(accountModel.getRecommend_history_income());
			rsAccountModelResponseBuilder.setRecommendReceiveIncome(accountModel.getRecommend_receive_income());
			rsAccountModelResponseBuilder.setRecommendRemainIncome(accountModel.getRecommend_remain_income());
			rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponseBuilder);
			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
					ERedisTopicType.topicProxy.getId() + account.getLastProxyIndex());
		} catch (Exception e) {
			logger.error("error", e);
		} finally {
			lock.unlock();
		}
	}

	// 返单退利
	public void doPaybackReceived(Account account, int money) {
		try {
			if (account == null || account.getAccountModel().getIs_agent() == 0) {
				return;
			}
			HallRecommendModel hallRecommendModel = account.getHallRecommendModel();
			if (hallRecommendModel.getAccount_id() == 0) {// 没有推荐人
				return;
			}
			// 上级推荐人
			Account upAccount = PublicServiceImpl.getInstance().getAccount(hallRecommendModel.getAccount_id());
			if (upAccount == null) {
				return;
			}
			int upLevel = upAccount.getHallRecommendModel().getRecommend_level();
			if (upLevel == 0) {// 上级推荐人的身份已经被取消
				return;
			}
			SysParamModel sysParamModel5000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(5000);
			long gameId = sysParamModel5000.getVal1();
			SysParamModel sysParamModel2223 = null;
			if (gameId == 7) {
				sysParamModel2223 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(7).get(2223);
			} else if (gameId == 6) {
				sysParamModel2223 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2223);
			} else {
				return;
			}
			if (sysParamModel2223 == null) {
				return;
			}
			if (upLevel == 1) {
				double receive = 0.00;
				if (hallRecommendModel.getRecommend_level() == 0) {
					receive = money * sysParamModel2223.getVal1() / 10000.00;// 直属代理返利
					paybackReceive(upAccount.getAccount_id(), receive, "直属代理退单", 1, money, account.getAccount_id(), account.getAccount_id());
				} else {
					receive = money * sysParamModel2223.getVal4() / 10000.00;// 直属代理返利
					paybackReceive(upAccount.getAccount_id(), receive, "下级推广员退单", 1, money, account.getAccount_id(), account.getAccount_id());
				}
			} else if (upLevel == 2) {
				double receive1 = 0.00;// 直属代理返利
				if (hallRecommendModel.getRecommend_level() == 0) {
					receive1 = money * sysParamModel2223.getVal2() / 10000.00;// 直属代理返利
					paybackReceive(upAccount.getAccount_id(), receive1, "直属代理退单", 1, money, account.getAccount_id(), account.getAccount_id());
					HallRecommendModel upHallRecommendModel = upAccount.getHallRecommendModel();
					if (upHallRecommendModel.getAccount_id() == 0) {// 没有推荐人
						return;
					}
					Account upUpAccount = PublicServiceImpl.getInstance().getAccount(upHallRecommendModel.getAccount_id());
					if (upUpAccount == null) {
						return;
					}
					int upUpLevel = upUpAccount.getHallRecommendModel().getRecommend_level();
					if (upUpLevel == 0) {// 上级推荐人的身份已经被取消
						return;
					}
					double receive2 = money * sysParamModel2223.getVal4() / 10000.00;// 给上级返利额度
					paybackReceive(upUpAccount.getAccount_id(), receive2, "下级代理退单", 1, money, upAccount.getAccount_id(), account.getAccount_id());
				} else {
					receive1 = money * sysParamModel2223.getVal5() / 10000.00;// 直属代理返利
					paybackReceive(upAccount.getAccount_id(), receive1, "下级推广员退单", 1, money, account.getAccount_id(), account.getAccount_id());
					HallRecommendModel upHallRecommendModel = upAccount.getHallRecommendModel();
					if (upHallRecommendModel.getAccount_id() == 0) {// 没有推荐人
						return;
					}
					Account upUpAccount = PublicServiceImpl.getInstance().getAccount(upHallRecommendModel.getAccount_id());
					if (upUpAccount == null) {
						return;
					}
					int upUpLevel = upUpAccount.getHallRecommendModel().getRecommend_level();
					if (upUpLevel == 0) {// 上级推荐人的身份已经被取消
						return;
					}
					double receive2 = money * sysParamModel2223.getVal4() / 10000.00;// 给上级返利额度
					paybackReceive(upUpAccount.getAccount_id(), receive2, "下级的下级推广员退单", 1, money, upAccount.getAccount_id(), account.getAccount_id());
				}
			} else if (upLevel == 3) {
				double receive1 = money * sysParamModel2223.getVal3() / 10000.00;// 直属代理返利
				paybackReceive(upAccount.getAccount_id(), receive1, "直属代理退单", 1, money, upAccount.getAccount_id(), account.getAccount_id());
				HallRecommendModel upHallRecommendModel = upAccount.getHallRecommendModel();
				if (upHallRecommendModel.getAccount_id() == 0) {// 没有推荐人
					return;
				}
				Account upUpAccount = PublicServiceImpl.getInstance().getAccount(upHallRecommendModel.getAccount_id());
				if (upUpAccount == null) {
					return;
				}
				int upUpLevel = upUpAccount.getHallRecommendModel().getRecommend_level();
				if (upUpLevel == 0) {// 上级推荐人的身份已经被取消
					return;
				}
				double receive2 = money * sysParamModel2223.getVal5() / 10000.00;// 给上级返利额度
				paybackReceive(upUpAccount.getAccount_id(), receive2, "下级代理退单", 1, money, upAccount.getAccount_id(), account.getAccount_id());

				HallRecommendModel upUpHallRecommendModel = upUpAccount.getHallRecommendModel();
				if (upUpHallRecommendModel.getAccount_id() == 0) {// 没有推荐人
					return;
				}
				Account upUpUpAccount = PublicServiceImpl.getInstance().getAccount(upUpHallRecommendModel.getAccount_id());
				if (upUpUpAccount == null) {
					return;
				}
				int upUpUpLevel = upUpUpAccount.getHallRecommendModel().getRecommend_level();
				if (upUpUpLevel == 0) {// 上级推荐人的身份已经被取消
					return;
				}
				double receive3 = money * sysParamModel2223.getVal4() / 10000.00;// 给上级返利额度
				paybackReceive(upUpUpAccount.getAccount_id(), receive3, "下级-下级的代理退单", 1, money, upUpAccount.getAccount_id(), account.getAccount_id());

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * 
	 * 按类型获取代理或者推广员充值返利总数据排行
	 * 
	 * @param type
	 * @param accountId
	 * @param recommend_level
	 * @return
	 */
	public HashMap<Long, GiveCardModel> getRechargeRankData(int type, long accountId, int recommend_level, int pageSize, int page) {
		Map<Integer, Date> dateMap = RecommendService.getDateByType(type);
		BasicDBList list = new BasicDBList();
		if (recommend_level == 0) {
			list.add(1);
		} else {
			list.add(1);
			list.add(2);
			list.add(3);
			list.add(4);
			list.add(5);
		}
		HashMap<Long, GiveCardModel> map = getTotalRechargeByAccountIds(accountId, dateMap.get(1), dateMap.get(2), list, pageSize, page);
		return map;
	}

	/**
	 * 
	 * 按类型获取代理或者推广员充值返利总数据排行
	 * 
	 * @param type
	 * @param accountId
	 * @param recommend_level
	 * @return
	 */
	public HashMap<Long, GiveCardModel> getNewRechargeRankData(int type, long accountId, int recommend_level, int pageSize, int page) {
		Map<Integer, Date> dateMap = RecommendService.getDateByType(type);
		BasicDBList list = new BasicDBList();
		if (recommend_level == 0) {
			list.add(0);
		} else {
			// list.add(0);
			list.add(1);
			list.add(2);
			list.add(3);
		}
		HashMap<Long, GiveCardModel> map = getNewTotalRechargeByAccountIds(accountId, dateMap.get(1), dateMap.get(2), list, pageSize, page);
		return map;
	}

	/**
	 * 获取开通代理人数排行
	 * 
	 * @param type
	 * @param accountId
	 * @return
	 */
	public Map<Long, Integer> getOpenAgentRankData(int type, long accountId) {
		Map<Long, Integer> returnMap = new HashMap<Long, Integer>();
		try {
			Map<Integer, String> dateMap = RecommendService.getTimeByType(type);
			PublicService publicService = SpringService.getBean(PublicService.class);
			List<HashMap> list = publicService.getPublicDAO().getOpenAgentRankByDate(accountId, dateMap.get(1), dateMap.get(2));
			for (HashMap map : list) {
				returnMap.put(Long.parseLong(map.get("account_id").toString()), Integer.parseInt(map.get("totals").toString()));
			}
		} catch (Exception e) {
			logger.error("统计开通代理人数排行榜出错 accountId=" + accountId, e);
		}
		return returnMap;
	}

	private static final FastDateFormat ISO_DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd");

	public static Map<Integer, String> getTimeByType(int type) {
		Map<Integer, String> map = new HashMap<>();
		String first = null;
		String end = null;
		if (type == 1) {
			Calendar c = Calendar.getInstance();
			c.add(Calendar.MONTH, 0);
			c.set(Calendar.DAY_OF_MONTH, 1);// 设置为1号,当前日期既为本月第一天
			first = ISO_DATE_FORMAT.format(c.getTime());
			end = ISO_DATE_FORMAT.format(new Date());
		} else if (type == 2) {
			Calendar cal_1 = Calendar.getInstance();// 获取当前日期
			cal_1.add(Calendar.MONTH, -1);
			cal_1.set(Calendar.DAY_OF_MONTH, 1);// 设置为1号,当前日期既为本月第一天
			// 获取前月的最后一天
			Calendar cale = Calendar.getInstance();
			cale.set(Calendar.DAY_OF_MONTH, 0);// 设置为1号,当前日期既为本月第一天
			first = ISO_DATE_FORMAT.format(cal_1.getTime());
			end = ISO_DATE_FORMAT.format(cale.getTime());
		} else {
			Calendar currCal = Calendar.getInstance();
			int currentYear = currCal.get(Calendar.YEAR);
			currCal.clear();
			currCal.set(Calendar.YEAR, currentYear);
			first = ISO_DATE_FORMAT.format(currCal.getTime());
			end = ISO_DATE_FORMAT.format(new Date());
		}
		map.put(1, first);
		map.put(2, end);
		return map;
	}

	public static Map<Integer, Date> getDateByType(int type) {
		Map<Integer, Date> map = new HashMap<>();
		Date first = null;
		Date end = null;
		if (type == 1) {
			Calendar c = Calendar.getInstance();
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.MILLISECOND, 0);
			c.add(Calendar.MONTH, 0);
			c.set(Calendar.DAY_OF_MONTH, 1);// 设置为1号,当前日期既为本月第一天
			first = c.getTime();
			end = new Date();
		} else if (type == 2) {
			Calendar cal_1 = Calendar.getInstance();// 获取当前日期
			cal_1.add(Calendar.MONTH, -1);
			cal_1.set(Calendar.DAY_OF_MONTH, 1);// 设置为1号,当前日期既为本月第一天
			// 获取前月的最后一天
			Calendar cale = Calendar.getInstance();
			cale.set(Calendar.DAY_OF_MONTH, 0);// 设置为1号,当前日期既为本月第一天
			cal_1.set(Calendar.HOUR_OF_DAY, 0);
			cal_1.set(Calendar.SECOND, 0);
			cal_1.set(Calendar.MINUTE, 0);
			cal_1.set(Calendar.MILLISECOND, 0);
			first = cal_1.getTime();
			end = cale.getTime();
		} else {
			Calendar currCal = Calendar.getInstance();
			int currentYear = currCal.get(Calendar.YEAR);
			currCal.clear();
			currCal.set(Calendar.YEAR, currentYear);
			first = currCal.getTime();
			end = new Date();
		}
		map.put(1, first);
		map.put(2, end);
		return map;
	}

	@Override
	protected void startService() {

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
