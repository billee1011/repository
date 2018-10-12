/**
 * 
 */
package com.cai.http.action;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.ELogType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountSimple;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.AutoUpdateRecomLevelModel;
import com.cai.common.domain.GiveCardModel;
import com.cai.common.domain.HallRecommendModel;
import com.cai.common.domain.NewHallRecommend;
import com.cai.common.domain.RecommendLimitModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.json.ReceivePercentModel;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.SpringService;
import com.cai.dictionary.RecommendLimitDict;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.http.FastJsonJsonView;
import com.cai.http.model.ErrorCode;
import com.cai.http.security.SignUtil;
import com.cai.rmi.impl.CenterRMIServerImpl;
import com.cai.service.MongoDBService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PublicServiceImpl;
import com.cai.service.RecommendService;
import com.cai.service.RecommenderReceiveService;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBList;

import protobuf.redis.ProtoRedis.RsAccountModelResponse;

@Controller
@RequestMapping("/hall")
public class NewRecommendController {

	private static Logger logger = LoggerFactory.getLogger(NewRecommendController.class);

	/**
	 * 成功
	 */
	public final static int SUCCESS = 0;

	/**
	 * 失败
	 */
	public final static int FAIL = -1;

	// 钻石黄金推广员----首页
	public static final int TYPE_RECOMENT_INDEX = 1;

	// 钻石黄金推广员----我要提现
	public static final int TYPE_MY_WITHDRAWS = 2;

	// 钻石黄金推广员----我的会员代理
	public static final int TYPE_MY_MEMBER_PROXY = 3;

	// 钻石黄金推广员----判断能否提现
	public static final int TYPE_JUDGE_WITHDRAWS = 4;

	// 钻石黄金推广员收益明细
	public static final int TYPE_RECOMMEND_INCOME = 5;

	// 钻石黄金推广员提现记录
	public static final int TYPE_RECOMMEND_OUT = 6;

	// 下级推广员详情
	public static final int TYPE_MY_RECOMMEND = 10;
	// 晋升、取消推广员
	public static final int TYPE_OPERATE_RECOMMEND = 11;
	// 充值开通代理
	public static final int TYPE_SET_PROXY = 12;
	// 判断玩家和目标玩家是否满足条件
	public static final int TYPE_SET_PLAYER = 13;
	// 钻石黄金推广员----我的玩家
	public static final int TYPE_MY_PLAYER = 14;
	// 扫描二维码自动设置推荐人
	public static final int TYPE_SET_RECOMMENDER = 15;
	// 开通代理人数排行
	public static final int TYPE_OPENAGENT_RANK = 16;
	// 获取代理充值排行
	public static final int TYPE_AGENT_RECHARGE_RANK = 17;
	// 获取下级推广员充值排行
	public static final int TYPE_RECOMMENT_RECHARGE_RANK = 18;
	// 推广员晋升记录
	public static final int TYPE_RECOMMENT_UPDATE_LOG = 19;
	// 见习推广员提升等级详情页（提升等级功能入口）
	public static final int TYPE_AGENT_UPDATE_DETAIL = 20;
	// 推广返利金额详情（包括代理，推广员返利）
	public static final int TYPE_RECEIVE_DETAIL = 21;
	// 退单详情
	public static final int TYPE_PAYBACK = 22;
	// 下级人员概况
	public static final int TYPE_SUB_REVIEW = 23;
	// 当前id带来的收益记录
	public static final int TYPE_INCOME_RECORD = 24;

	public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
	// 下级钻石黄金推广员详情

	@RequestMapping("/recommend/help")
	public ModelAndView centerpay(HttpServletRequest request) {
		Map<String, Object> resultMap = Maps.newHashMap();
		Map<String, String> params = SignUtil.getParametersHashMap(request);
		String queryType = params.get("queryType");
		int type;
		try {
			type = Integer.parseInt(queryType);
		} catch (NumberFormatException e) {
			resultMap.put("msg", "参数异常");
			resultMap.put("result", FAIL);
			return new ModelAndView(new FastJsonJsonView(), resultMap);
		}
		if (type == TYPE_RECOMMEND_INCOME) {
			doRecommendIncome(params, resultMap);
		} else if (type == TYPE_RECOMMEND_OUT) {
			doRecommendOutcome(params, resultMap);
		} else if (type == TYPE_RECOMENT_INDEX) {// ---钻石黄金推广员---首页
			recomentIndex(params, resultMap);
		} else if (type == TYPE_MY_WITHDRAWS) {// ---钻石黄金推广员---提现
			myWithdraws(params, resultMap);
		} else if (type == TYPE_MY_MEMBER_PROXY) {// ---钻石黄金推广员---我的会员代理
			myAgentDetail(params, resultMap);
		} else if (type == TYPE_JUDGE_WITHDRAWS) {// ---钻石黄金推广员---判断能否提现，不做入库的操作
			judgeWithdraws(params, resultMap);
		} else if (type == TYPE_MY_RECOMMEND) {// ---钻石黄金推广员-我的下级推广员
			myRecommendDetail(params, resultMap);
		} else if (type == TYPE_OPERATE_RECOMMEND) {// ---钻石黄金推广员-我的下级推广员
			setRecommendLevel(params, resultMap);
		} else if (type == TYPE_SET_PROXY) {// ---充值成功后设置代理身份，并将他设置为推广员直属代理
			setProxy(params, resultMap);
		} else if (type == TYPE_MY_PLAYER) {// ---我的玩家
			myPlayerDetail(params, resultMap);
		} else if (type == TYPE_SET_PLAYER) {// ---判断玩家和目标玩家是否满足条件
			chargePlayer(params, resultMap);
		} else if (type == TYPE_SET_RECOMMENDER) {
			setRecommender(params, resultMap);
		} else if (type == TYPE_OPENAGENT_RANK) {
			openAgentRank(params, resultMap);
		} else if (type == TYPE_AGENT_RECHARGE_RANK) {
			agentRechargeRank(params, resultMap);
		} else if (type == TYPE_RECOMMENT_RECHARGE_RANK) {
			subRecommenderRechargeRank(params, resultMap);
		} else if (type == TYPE_RECOMMENT_UPDATE_LOG) {
			autoSetLeveLog(params, resultMap);
		} else if (type == TYPE_AGENT_UPDATE_DETAIL) {
			agentUpdateDetail(params, resultMap);
		} else if (type == TYPE_RECEIVE_DETAIL) {
			receiveDetail(params, resultMap);
		} else if (type == TYPE_PAYBACK) {
			paybackRecord(params, resultMap);
		} else if (type == TYPE_SUB_REVIEW) {
			subReviw(params, resultMap);
		} else if (type == TYPE_INCOME_RECORD) {
			incomeRecord(params, resultMap);
		}
		return new ModelAndView(new FastJsonJsonView(), resultMap);
	}

	private static SimpleDateFormat timeStr = new SimpleDateFormat("yyyy/MM/dd");

	/**
	 * 收益记录
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void doRecommendIncome(Map<String, String> params, Map<String, Object> resultMap) {
		Date startDate = null;
		Date endDate = null;
		String pageIndex = params.get("pageIndex");
		String pageSize = params.get("pageSize");
		String account_id = params.get("userID");
		int newPageIndex = 0;
		int newPageSize = 0;
		try {
			if (pageIndex == null || pageSize == null) {
				newPageIndex = 0;
				newPageSize = 5;
			} else {
				newPageIndex = Integer.valueOf(pageIndex);
				newPageSize = Integer.valueOf(pageSize);
			}
			String startDateStr = params.get("startDate");
			String endDateStr = params.get("endDate");
			startDate = dateFormat.parse(startDateStr);
			endDate = dateFormat.parse(endDateStr);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}

		int type = 0;
		try {
			type = Integer.parseInt(params.get("type"));
		} catch (Exception e) {
		}
		resultMap.put("result", SUCCESS);
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("account_id").is(Long.parseLong(account_id)));
		query.addCriteria(Criteria.where("log_type").is(ELogType.agentIncome.getId()));
		query.addCriteria(Criteria.where("create_time").gte(MyDateUtil.getZeroDate(startDate)).lt(MyDateUtil.getTomorrowZeroDate(endDate)))
				.skip(newPageIndex * newPageSize).limit(newPageSize);

		BasicDBList values = new BasicDBList();
		if (type == 1) {
			values.add(0);
			values.add(4);
			query.addCriteria(Criteria.where("v2").in(values));
		} else if (type == 2) {
			values.add(1);
			values.add(2);
			values.add(3);
			query.addCriteria(Criteria.where("v2").in(values));
		}
		query.with(new Sort(Direction.DESC, "create_time"));
		List<NewHallRecommend> recommendIncomeList = mongoDBService.getMongoTemplate().find(query, NewHallRecommend.class);
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Map<String, Object> map = null;
		// Double totalBalance = 0.00;

		for (NewHallRecommend model : recommendIncomeList) {
			map = new HashMap<>();
			map.put("accountId", model.getAccount_id());
			map.put("getBalance", model.getV1() / 100.00);
			map.put("comeSource", model.getV2());
			map.put("activity", model.getMsg().split("\\|")[0]);
			String newTimeStr = timeStr.format(model.getCreate_time());
			map.put("createTime", newTimeStr);
			map.put("sourceId", model.getSource_id());
			map.put("myLevel", model.getMy_level());// 我的等级
			map.put("reChargeLevel", model.getRecommend_level());// 充值时玩家的等级
			map.put("receive_percent", model.getReceive_percent() + "%");
			if (model.getSource_id() > 0) {
				// Account account =
				// PublicServiceImpl.getInstance().getAccount(model.getSource_id());
				AccountSimple accountSimple = PublicServiceImpl.getInstance().getAccountSimpe(model.getSource_id());
				if (accountSimple == null) {
					logger.error("帐号不存在" + model.getSource_id());
					continue;
				}
				map.put("targetId", model.getSource_id());
				map.put("headPic", accountSimple.getIcon());
				map.put("nickName", accountSimple.getNick_name() == null ? "-" : accountSimple.getNick_name());
			} else {
				map.put("nickName", "-");
				map.put("targetId", 0);
			}
			// 充值返利
			map.put("recharge_receive", model.getRecharge_money() / 100.00 + "/" + model.getV1() / 100.00);
			map.put("recharge", model.getRecharge_money() / 100.00);
			map.put("receive", model.getV1() / 100.00);
			list.add(map);
		}
		Account account = SpringService.getBean(CenterRMIServerImpl.class).getAccount(Long.parseLong(account_id));
		resultMap.put("totalBalance", account.getAccountModel().getRecommend_history_income());// 总收益
		resultMap.put("result", SUCCESS);
		resultMap.put("data", list);
	}

	/**
	 * 钻石黄金推广员------根据用户ID查询钻石黄金推广员相关的首页信息
	 * 
	 * @param request
	 * @param params
	 */
	private void recomentIndex(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		long userID;
		try {
			userID = Long.parseLong(user_ID);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		Account account = PublicServiceImpl.getInstance().getAccount(userID);
		if (account == null || account.getAccountModel().getIs_agent() == 0) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", ErrorCode.VISIT_FAIL);
			return;
		}

		DecimalFormat df = new DecimalFormat("######0.00");
		String canRMB = df.format(account.getAccountModel().getRecommend_remain_income());
		resultMap.put("userId", userID);
		resultMap.put("result", SUCCESS);
		resultMap.put("isAgent", account.getAccountModel().getIs_agent());
		resultMap.put("headUrl", account.getAccountWeixinModel() != null ? account.getAccountWeixinModel().getHeadimgurl() : "");
		resultMap.put("nickName", account.getAccountWeixinModel() != null ? account.getAccountWeixinModel().getNickname() : "-");
		resultMap.put("canRMB", canRMB);// 钻石黄金推广员可提现
		resultMap.put("historyIncome", account.getAccountModel().getRecommend_history_income());// 历史总收益
		resultMap.put("promoteLevel", account.getHallRecommendModel().getRecommend_level());// 钻石黄金白银推广员等级
		resultMap.put("gold", account.getAccountModel().getGold());// 闲逸豆
		resultMap.put("mobile", account.getAccountModel().getMobile_phone());
		resultMap.put("banned", account.getAccountModel().getBanned());
		resultMap.put("is_rebate", account.getAccountModel().getIs_rebate());
		if (account.getHallRecommendModel() != null) {
			resultMap.put("hall_recommend_level", account.getHallRecommendModel().getRecommend_level());
		} else {
			resultMap.put("hall_recommend_level", 0);
		}
		long sum = MongoDBServiceImpl.getInstance().getAgentYesterdayIncome(userID);// 获取到的是分为单位
		double yesterdayRMB = sum / 100.00;
		resultMap.put("yesterdayRMB", yesterdayRMB);// 钻石黄金推广员 昨日收益
		HashMap<Integer, Integer> countMap = RecommenderReceiveService.agentCount(account.getHallRecommendModelMap());
		resultMap.put("playerCount", countMap.get(3));
		resultMap.put("agentCount", countMap.get(1));
		resultMap.put("recommendCount", countMap.get(2));
		resultMap.put("receivePercent", getReceivePercent(account.getHallRecommendModel()));
	}

	private List<ReceivePercentModel> getReceivePercent(HallRecommendModel model) {
		RecommendLimitModel recommendLimitModel = RecommendLimitDict.getInstance().getRecommendLimitModelById(model.getTarget_account_id());
		SysParamModel sysParamModel2248 = null;
		SysParamModel sysParamModel2249 = null;
		SysParamModel sysParamModel2250 = null;
		SysParamModel sysParamModel2223 = null;
		sysParamModel2223 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2223);
		// 新钻石推广返利比
		sysParamModel2248 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2248);
		// 新黄金推广返利比
		sysParamModel2249 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2249);
		// 新白银推广返利比
		sysParamModel2250 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2250);
		// 被推广的人比推广员比自己的等级高返利比
		ReceivePercentModel subAgent = null;
		ReceivePercentModel subRecommender = null;
		if (model.getRecommend_level() == 0) {
			subAgent = new ReceivePercentModel(1, 0, sysParamModel2223.getVal3());
			subRecommender = new ReceivePercentModel(2, 0, sysParamModel2250.getVal1());
		}
		if (model.getRecommend_level() == 3) {
			if (recommendLimitModel != null) {
				subAgent = new ReceivePercentModel(1, recommendLimitModel.getAgent_receive_per(), recommendLimitModel.getAgent_receive_per());
				subRecommender = new ReceivePercentModel(2, recommendLimitModel.getRecom_receive_per(), recommendLimitModel.getRecom_receive_per());
			} else {
				subAgent = new ReceivePercentModel(1, sysParamModel2223.getVal3(), sysParamModel2223.getVal2());
				subRecommender = new ReceivePercentModel(2, sysParamModel2250.getVal1(), sysParamModel2249.getVal1());
			}
		}
		if (model.getRecommend_level() == 2) {
			if (recommendLimitModel != null) {
				subAgent = new ReceivePercentModel(1, recommendLimitModel.getAgent_receive_per(), recommendLimitModel.getAgent_receive_per());
				subRecommender = new ReceivePercentModel(2, recommendLimitModel.getRecom_receive_per(), recommendLimitModel.getRecom_receive_per());
			} else {
				subAgent = new ReceivePercentModel(1, sysParamModel2223.getVal2(), sysParamModel2223.getVal1());
				subRecommender = new ReceivePercentModel(2, sysParamModel2249.getVal1(), sysParamModel2248.getVal1());
			}
		}
		if (model.getRecommend_level() == 1) {
			if (recommendLimitModel != null) {
				subAgent = new ReceivePercentModel(1, recommendLimitModel.getAgent_receive_per(), recommendLimitModel.getAgent_receive_per());
				subRecommender = new ReceivePercentModel(2, recommendLimitModel.getRecom_receive_per(), recommendLimitModel.getRecom_receive_per());
			} else {
				subAgent = new ReceivePercentModel(1, sysParamModel2223.getVal1(), sysParamModel2223.getVal1());
				subRecommender = new ReceivePercentModel(2, sysParamModel2248.getVal1(), sysParamModel2248.getVal1());
			}

		}
		List<ReceivePercentModel> list = new ArrayList<ReceivePercentModel>(2);
		list.add(subAgent);
		list.add(subRecommender);
		return list;
	}

	/**
	 * 钻石黄金推广员提现记录
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void doRecommendOutcome(Map<String, String> params, Map<String, Object> resultMap) {
		Date startDate = null;
		Date endDate = null;

		String pageIndex = params.get("pageIndex");
		String pageSize = params.get("pageSize");
		int newPageIndex = 0;
		int newPageSize = 5;

		try {

			if (pageIndex == null || pageSize == null) {
				newPageIndex = 0;
				newPageSize = 5;
			} else {
				newPageIndex = Integer.valueOf(pageIndex);
				newPageSize = Integer.valueOf(pageSize);
			}
			String startDateStr = params.get("startDate");
			String endDateStr = params.get("endDate");
			startDate = dateFormat.parse(startDateStr);
			endDate = dateFormat.parse(endDateStr);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		String account_id = params.get("userID");
		resultMap.put("result", SUCCESS);
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);

		// 分页算出的数据
		Query query = new Query();
		query.addCriteria(Criteria.where("account_id").is(Long.parseLong(account_id)));
		query.addCriteria(Criteria.where("log_type").is(ELogType.agentOutcome.getId()));
		query.addCriteria(Criteria.where("create_time").gte(MyDateUtil.getZeroDate(startDate)).lt(MyDateUtil.getTomorrowZeroDate(endDate)));
		query.with(new Sort(Direction.DESC, "create_time")).skip(newPageIndex * newPageSize).limit(newPageSize);
		List<NewHallRecommend> recommendIncomeList = mongoDBService.getMongoTemplate().find(query, NewHallRecommend.class);
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Map<String, Object> map = null;
		Account account = PublicServiceImpl.getInstance().getAccount(Long.parseLong(account_id));
		for (NewHallRecommend model : recommendIncomeList) {
			map = Maps.newConcurrentMap();
			map.put("nickName", account.getAccountWeixinModel() != null ? account.getAccountWeixinModel().getNickname() : "");
			if (model.getV1() == 0) {
				map.put("balance", model.getV1());
			} else {
				String balance = String.valueOf(model.getV1() / 100.00);
				map.put("balance", balance.toString().split("-")[1]);
			}
			map.put("accountId", model.getAccount_id());
			SimpleDateFormat timeStr = new SimpleDateFormat("MM-dd");
			String newTimeStr = timeStr.format(model.getCreate_time());
			map.put("createTime", newTimeStr);
			list.add(map);
		}
		// AggregationOperation match =
		// Aggregation.match(Criteria.where("account_id").is(Long.parseLong(account_id)).and("log_type").is(ELogType.agentOutcome.getId()));
		// AggregationOperation group =
		// Aggregation.group().sum("v1").as("count").count().as("line");
		// Aggregation aggregation = Aggregation.newAggregation(match, group);
		// AggregationResults<GiveCardModel> result =
		// mongoDBService.getMongoTemplate().aggregate(aggregation,
		// "new_hall_recommend", GiveCardModel.class);
		// List<GiveCardModel> sumLlist = result.getMappedResults();
		// long totalBalance = 0;// 累计提现金额
		// if (sumLlist != null && sumLlist.size() > 0) {
		// GiveCardModel giveCardModel = sumLlist.get(0);
		// totalBalance = giveCardModel.getCount();
		// }
		resultMap.put("result", SUCCESS);
		resultMap.put("totalBalance", account.getAccountModel().getRecommend_receive_income());
		resultMap.put("data", list);
	}

	/**
	 * 钻石黄金推广员----我要提现(只作判断，能不能提现)
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void judgeWithdraws(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		Double income = Double.parseDouble(params.get("income"));// 提现金额,假设传过来的是正数

		long userID;
		try {
			income = -income;// 将金额数变成负数
			userID = Long.parseLong(user_ID);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}

		CenterRMIServerImpl centerRMIServer = SpringService.getBean(CenterRMIServerImpl.class);
		AddGoldResultModel resultModel = centerRMIServer.judgeWithdraw(userID, income);

		if (!resultModel.isSuccess()) {
			resultMap.put("result", FAIL);// 0表示成功，-1表示失败
		} else {
			resultMap.put("result", SUCCESS);// 0表示成功，-1表示失败
		}
		resultMap.put("msg", resultModel.getMsg());// 返回到类型的消息

	}

	/**
	 * 钻石黄金推广员----我的下级推广员
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void myRecommendDetail(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		long userID;
		Date startDate = null;
		Date endDate = null;
		try {
			userID = Long.parseLong(user_ID);
			String startDateStr = params.get("startDate");
			String endDateStr = params.get("endDate");
			startDate = dateFormat.parse(startDateStr);
			endDate = dateFormat.parse(endDateStr);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		Map<String, Object> detailMap = null;
		try {
			detailMap = RecommenderReceiveService.getInstance().queryHallDownRecommend(userID, startDate, endDate);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (detailMap != null) {
			resultMap.put("details", detailMap.get("data"));
			resultMap.put("total", detailMap.get("total"));
		}
		resultMap.put("result", SUCCESS);// 0表示成功，-1表示失败

	}

	/**
	 * 钻石黄金推广员----我的下级代理
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void myAgentDetail(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		long userID;
		Date startDate = null;
		Date endDate = null;
		try {
			userID = Long.parseLong(user_ID);
			String startDateStr = params.get("startDate");
			String endDateStr = params.get("endDate");
			startDate = dateFormat.parse(startDateStr);
			endDate = dateFormat.parse(endDateStr);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}

		Map<String, Object> detailMap = RecommenderReceiveService.getInstance().queryHallDownAgent(userID, startDate, endDate);
		if (detailMap != null) {
			resultMap.put("details", detailMap.get("data"));
			resultMap.put("total", detailMap.get("total"));
		}
		resultMap.put("result", SUCCESS);// 0表示成功，-1表示失败

	}

	/**
	 * 钻石黄金推广员----我的玩家
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void myPlayerDetail(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		long userID;
		Date startDate = null;
		Date endDate = null;
		try {
			userID = Long.parseLong(user_ID);
			String startDateStr = params.get("startDate");
			String endDateStr = params.get("endDate");
			startDate = dateFormat.parse(startDateStr);
			endDate = dateFormat.parse(endDateStr);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		CenterRMIServerImpl centerRMIServer = SpringService.getBean(CenterRMIServerImpl.class);
		Map<String, Object> detailMap = centerRMIServer.queryMyPlayers(userID, startDate, endDate);
		if (detailMap != null) {
			resultMap.put("details", detailMap.get("data"));
			resultMap.put("total", detailMap.get("total"));
		}
		resultMap.put("result", SUCCESS);// 0表示成功，-1表示失败

	}

	/**
	 * 钻石黄金推广员----我要提现（既要判断，还要作操作入库的）
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void myWithdraws(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		Double income = Double.parseDouble(params.get("income"));// 提现金额,假设传过来的是正数
		String pay_type = params.get("payType");// 1 余额支付 2.余额提现
		long userID;
		int payType;
		try {
			income = -income;// 将金额数变成负数
			userID = Long.parseLong(user_ID);
			payType = Integer.parseInt(pay_type);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}

		if (income > 0) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "提现的金额数应该为负!");
			return;
		}
		AddGoldResultModel resultModel = null;
		resultModel = RecommenderReceiveService.getInstance().drawCash(userID, income, payType);
		DecimalFormat df = new DecimalFormat("######0.00");
		String canRMB = df.format(resultModel.getAccountModel().getRecommend_remain_income());
		if (!resultModel.isSuccess()) {
			resultMap.put("result", FAIL);// 0表示成功，-1表示失败
		} else {
			resultMap.put("result", SUCCESS);// 0表示成功，-1表示失败
		}
		resultMap.put("msg", resultModel.getMsg());// 返回到类型的消息
		resultMap.put("canRMB", canRMB);// 更新后钻石黄金推广员可提现金额
		// resultMap.put("yesterdayRMB",
		// resultModel.getAccountModel().getRecommend_yesterday_income());//
		// 更新后钻石黄金推广员昨日收益
		resultMap.put("gold", resultModel.getAccountModel().getGold());// 更新后钻石黄金推广员闲逸豆

	}

	/**
	 * 设置上级推广员
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void setRecommender(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		String target_ID = params.get("targetUserID");
		String union_ID = params.get("unionID");// 如果此unionId还未生成账号
		// 验证必填参数格式
		long userID;
		long targetId = 0;
		try {
			userID = Long.parseLong(user_ID);
			if (StringUtils.isNotBlank(target_ID)) {
				targetId = Long.parseLong(target_ID);
			}
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		Account account = PublicServiceImpl.getInstance().getAccount(userID);
		if (account == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "玩家不存在");
			return;
		}
		// if (account.getHallRecommendModel().getRecommend_level() == 0) {
		// resultMap.put("result", FAIL);
		// resultMap.put("msg", "无法设置下级推广员");
		// return;
		// }
		if (account.getAccountModel().getProxy_level() == 0) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "无效的推荐");
			return;
		}
		Account targetAccount = null;
		if (targetId > 0) {
			targetAccount = PublicServiceImpl.getInstance().getAccount(targetId);
			if (targetAccount == null) {
				resultMap.put("result", FAIL);
				resultMap.put("msg", "无效的推荐");
				return;
			}
		} else {
			targetAccount = PublicServiceImpl.getInstance().getAccountByWxUnionid(union_ID);
		}
		if (targetAccount == null) {
			HallRecommendModel nowHallRecommendModel = new HallRecommendModel();// 现在的推荐关系
			nowHallRecommendModel.setAccount_id(userID);
			nowHallRecommendModel.setRecommend_level(0);
			nowHallRecommendModel.setCreate_time(new Date());
			nowHallRecommendModel.setTarget_account_id(0);
			nowHallRecommendModel.setUpdate_time(DateFormatUtils.format(new Date(), "yyyy-MM-dd"));
			RecommendService.getInstance().pushRecommender(union_ID, nowHallRecommendModel);
		} else {
			if (targetAccount.getHallRecommendModel().getAccount_id() > 0) {
				resultMap.put("result", FAIL);
				resultMap.put("msg", "已经推荐过了");
				return;
			}
			if (targetAccount.getAccount_id() == userID || targetAccount.getAccountModel().getProxy_level() > 0) {
				resultMap.put("result", FAIL);
				resultMap.put("msg", "无效的推荐");
				return;
			}
			SysParamModel sysParamModel2004 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2004);
			int addGold = 50;
			if (sysParamModel2004 != null) {
				addGold = sysParamModel2004.getVal4();
			}
			CenterRMIServerImpl centerRMIServer = SpringService.getBean(CenterRMIServerImpl.class);
			centerRMIServer.addAccountGold(targetAccount.getAccount_id(), addGold, false, "填写推广员推荐人送豆，推广员account_id:" + userID,
					EGoldOperateType.PADDING_RECOMMEND_ID);
			RsAccountModelResponse.Builder rsAccountModelResponse = RsAccountModelResponse.newBuilder();
			rsAccountModelResponse.setAccountId(targetAccount.getAccount_id());
			rsAccountModelResponse.setHallRecommentId(userID);
			rsAccountModelResponse.setHallRecommentLevel(0);
			SpringService.getBean(CenterRMIServerImpl.class).ossModifyAccountModel(rsAccountModelResponse.build());
		}
		resultMap.put("result", SUCCESS);
	}

	/**
	 * 设置为下级推广员或者取消下级推广员身份
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void setRecommendLevel(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		String target_ID = params.get("targetUserID");
		String opType = params.get("type");// 0取消推广员身份，1设置为下级推广员
		// 验证必填参数格式
		long userID;
		Long targetID = null;
		int type = 0;
		try {
			userID = Long.parseLong(user_ID);
			type = Integer.parseInt(opType);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		Account account = PublicServiceImpl.getInstance().getAccount(userID);
		if (account == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "玩家不存在");
			return;
		}
		if (type == 1) {
			try {
				targetID = Long.parseLong(target_ID);
			} catch (Exception e) {
				resultMap.put("result", FAIL);
				resultMap.put("msg", "参数异常");
				return;
			}
			if (account.getHallRecommendModel().getRecommend_level() == 0 || account.getHallRecommendModel().getRecommend_level() > 2) {
				resultMap.put("result", FAIL);
				resultMap.put("msg", "无法设置下级推广员");
				return;
			}
			Account targetAccount = PublicServiceImpl.getInstance().getAccount(targetID);
			if (targetAccount == null) {
				resultMap.put("result", FAIL);
				resultMap.put("msg", "目标玩家不存在");
				return;
			}
			if (targetAccount.getHallRecommendModel().getAccount_id() != userID) {
				resultMap.put("result", FAIL);
				resultMap.put("msg", "目标玩家不是你的下级代理");
				return;
			}
			RecommendLimitModel model = RecommendLimitDict.getInstance().getRecommendLimitModelById(account.getAccount_id());
			if (model != null) {
				int max = model.getRecom_num_limit();
				Map<Long, HallRecommendModel> map = account.getHallRecommendModelMap();
				if (map.size() > max) {
					int a = 0;
					for (HallRecommendModel hallModel : map.values()) {
						if (hallModel.getRecommend_level() > 0) {
							a++;
						}
					}
					if (a >= max) {
						resultMap.put("result", FAIL);
						resultMap.put("msg", "您的下级推广已经达到最高上限，无法设置下级推广员");
						return;
					}
				}
			}
			RsAccountModelResponse.Builder rsAccountModelResponse = RsAccountModelResponse.newBuilder();
			rsAccountModelResponse.setAccountId(targetID);
			rsAccountModelResponse.setHallRecommentId(userID);
			rsAccountModelResponse.setHallRecommentLevel(3);// 只能设置白银推广员
			SpringService.getBean(CenterRMIServerImpl.class).ossModifyAccountModel(rsAccountModelResponse.build());
			log_update_record(targetID, 3, 0, "升级为白银推广员");
		} else {
			String[] userIds = target_ID.split(",");
			for (String userId : userIds) {
				try {
					targetID = Long.parseLong(userId);
				} catch (Exception e) {
					resultMap.put("result", FAIL);
					resultMap.put("msg", "参数异常");
					return;
				}
				Account targetAccount = PublicServiceImpl.getInstance().getAccount(targetID);
				if (targetAccount == null) {
					resultMap.put("result", FAIL);
					resultMap.put("msg", "目标玩家不存在");
					return;
				}
				if (targetAccount.getHallRecommendModel().getAccount_id() != userID) {
					resultMap.put("result", FAIL);
					resultMap.put("msg", "目标玩家不是你的下级代理");
					return;
				}
				RsAccountModelResponse.Builder rsAccountModelResponse = RsAccountModelResponse.newBuilder();
				rsAccountModelResponse.setAccountId(targetID);
				rsAccountModelResponse.setHallRecommentId(0);
				rsAccountModelResponse.setHallRecommentLevel(account.getHallRecommendModel().getRecommend_level() + 1);
				SpringService.getBean(CenterRMIServerImpl.class).ossModifyAccountModel(rsAccountModelResponse.build());
			}
		}
		resultMap.put("result", SUCCESS);
	}

	/**
	 * 推广员充值设置代理
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void setProxy(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		String target_ID = params.get("targetUserID");
		// 验证必填参数格式
		long userID;
		Long targetID = null;
		try {
			userID = Long.parseLong(user_ID);
			targetID = Long.parseLong(target_ID);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		Account account = PublicServiceImpl.getInstance().getAccount(userID);
		if (account == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "玩家不存在");
			return;
		}
		if (account.getAccountModel().getProxy_level() == 0) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "非代理无法设置代理");
			return;
		}

		Account targetAccount = PublicServiceImpl.getInstance().getAccount(targetID);
		if (targetAccount == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "目标玩家不存在");
			return;
		}
		if (targetAccount.getAccountModel().getProxy_level() > 0) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "目标已经是代理");
			return;
		}
		RsAccountModelResponse.Builder rsAccountModelResponse = RsAccountModelResponse.newBuilder();
		rsAccountModelResponse.setAccountId(targetID);
		rsAccountModelResponse.setHallRecommentId(userID);
		rsAccountModelResponse.setHallRecommentLevel(0);
		rsAccountModelResponse.setIsAgent(1);
		CenterRMIServerImpl centerRMIServer = SpringService.getBean(CenterRMIServerImpl.class);
		centerRMIServer.ossModifyAccountModel(rsAccountModelResponse.build());
		resultMap.put("result", SUCCESS);
		log_update_record(targetID, 0, 0, "升级见习推广员");
	}

	/***
	 * 创建订单前 判断玩家和目标是不是满足条件
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void chargePlayer(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		String target_ID = params.get("targetUserID");
		// 验证必填参数格式
		long userID;
		Long targetID = null;
		try {
			userID = Long.parseLong(user_ID);
			targetID = Long.parseLong(target_ID);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		Account account = PublicServiceImpl.getInstance().getAccount(userID);
		if (account == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "玩家不存在");
			return;
		}
		if (account.getAccountModel().getIs_agent() == 0) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "非代理身份无法设置代理");
			return;
		}
		Account targetAccount = PublicServiceImpl.getInstance().getAccount(targetID);
		if (targetAccount == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "目标玩家不存在");
			return;
		}
		if (targetAccount.getAccountModel().getProxy_level() > 0) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "目标已经是代理");
			return;
		}
		resultMap.put("result", SUCCESS);

	}

	private void openAgentRank(Map<String, String> params, Map<String, Object> resultMap) {
		String accountId = params.get("accountId");
		String typeStr = params.get("type");
		// 验证必填参数格式
		long userID = 0;
		int type = 1;
		try {
			userID = Long.parseLong(accountId);
			type = Integer.parseInt(typeStr);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		Account account = PublicServiceImpl.getInstance().getAccount(userID);
		if (account == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "玩家不存在");
			return;
		}
		HallRecommendModel model = account.getHallRecommendModel();
		if (model == null || model.getRecommend_level() == 0) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "无权访问");
			return;
		}
		Map<Long, Integer> map = RecommendService.getInstance().getOpenAgentRankData(type, userID);
		JSONArray array = new JSONArray();
		for (long id : map.keySet()) {
			AccountSimple accountSimple = PublicServiceImpl.getInstance().getAccountSimpe(id);
			if (accountSimple == null) {
				logger.error("帐号不存在" + id);
				continue;
			}
			HallRecommendModel hallRecommendModel = PublicServiceImpl.getInstance().getHallRecommendModel(id);
			JSONObject json = new JSONObject();
			json.put("accountId", id);
			json.put("nickName", accountSimple.getNick_name());
			json.put("count", map.get(id));
			json.put("recommendLevel", hallRecommendModel.getRecommend_level());
			array.add(json);
		}
		resultMap.put("data", array);
		resultMap.put("result", SUCCESS);
	}

	private void agentRechargeRank(Map<String, String> params, Map<String, Object> resultMap) {
		String accountId = params.get("accountId");
		String typeStr = params.get("type");
		// 验证必填参数格式
		long userID = 0;
		int type = 1;
		String pageIndex = params.get("pageIndex");
		String pageSize = params.get("pageSize");
		int newPageIndex = 0;
		int newPageSize = 0;
		try {
			if (pageIndex == null || pageSize == null) {
				newPageIndex = 0;
				newPageSize = 5;
			} else {
				newPageIndex = Integer.valueOf(pageIndex);
				newPageSize = Integer.valueOf(pageSize);
			}
			userID = Long.parseLong(accountId);
			type = Integer.parseInt(typeStr);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		Account account = PublicServiceImpl.getInstance().getAccount(userID);
		if (account == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "玩家不存在");
			return;
		}
		HallRecommendModel model = account.getHallRecommendModel();
		if (model == null || model.getRecommend_level() == 0) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "无权访问");
			return;
		}
		Map<Long, GiveCardModel> map = RecommendService.getInstance().getNewRechargeRankData(type, userID, 0, newPageSize, newPageIndex);
		JSONArray array = new JSONArray();
		for (long id : map.keySet()) {
			AccountSimple accountSimple = PublicServiceImpl.getInstance().getAccountSimpe(id);
			if (accountSimple == null) {
				logger.error("帐号不存在" + id);
				continue;
			}
			GiveCardModel entity = map.get(id);
			JSONObject json = new JSONObject();
			json.put("accountId", id);
			json.put("nickName", accountSimple.getNick_name());
			json.put("recommendLevel", PublicServiceImpl.getInstance().getHallRecommendModel(id).getRecommend_level());
			json.put("recharge", entity.getLine());
			json.put("receive", entity.getCount());
			array.add(json);
		}
		resultMap.put("data", array);
		resultMap.put("result", SUCCESS);
	}

	private void subRecommenderRechargeRank(Map<String, String> params, Map<String, Object> resultMap) {
		String accountId = params.get("accountId");
		String typeStr = params.get("type");
		// 验证必填参数格式
		long userID = 0;
		int type = 1;
		String pageIndex = params.get("pageIndex");
		String pageSize = params.get("pageSize");
		int newPageIndex = 0;
		int newPageSize = 0;
		try {
			if (pageIndex == null || pageSize == null) {
				newPageIndex = 0;
				newPageSize = 5;
			} else {
				newPageIndex = Integer.valueOf(pageIndex);
				newPageSize = Integer.valueOf(pageSize);
			}
			userID = Long.parseLong(accountId);
			type = Integer.parseInt(typeStr);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		Account account = PublicServiceImpl.getInstance().getAccount(userID);
		if (account == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "玩家不存在");
			return;
		}
		HallRecommendModel model = account.getHallRecommendModel();
		if (model == null || model.getRecommend_level() == 0) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "无权访问");
			return;
		}
		Map<Long, GiveCardModel> map = RecommendService.getInstance().getNewRechargeRankData(type, userID, 1, newPageSize, newPageIndex);
		JSONArray array = new JSONArray();
		for (long id : map.keySet()) {
			AccountSimple accountSimple = PublicServiceImpl.getInstance().getAccountSimpe(id);
			if (accountSimple == null) {
				logger.error("帐号不存在" + id);
				continue;
			}
			GiveCardModel entity = map.get(id);
			JSONObject json = new JSONObject();
			HallRecommendModel hModel = PublicServiceImpl.getInstance().getHallRecommendModel(id);
			if (hModel != null && hModel.getRecommend_level() > 0) {
				json.put("recommendLevel", hModel.getRecommend_level());
			} else {
				json.put("recommendLevel", 0);
			}
			json.put("accountId", id);
			json.put("nickName", accountSimple.getNick_name());
			json.put("recharge", entity.getLine());
			json.put("receive", entity.getCount());
			array.add(json);
		}
		resultMap.put("data", array);
		resultMap.put("result", SUCCESS);
	}

	/**
	 * 晋升等级记录
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void autoSetLeveLog(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		long userID;
		try {
			userID = Long.parseLong(user_ID);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("account_id").is(userID));
		query.with(new Sort(Direction.DESC, "create_time"));
		List<AutoUpdateRecomLevelModel> list = mongoDBService.getMongoTemplate().find(query, AutoUpdateRecomLevelModel.class);
		resultMap.put("data", list);
		resultMap.put("result", SUCCESS);
	}

	private void agentUpdateDetail(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		long userID;
		try {
			userID = Long.parseLong(user_ID);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		Account account = PublicServiceImpl.getInstance().getAccount(userID);
		if (account == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "玩家不存在");
			return;
		}
		if (account.getAccountModel().getIs_agent() == 0) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "无权访问该页面");
			return;
		}
		Map<String, Object> map = new HashMap<>();
		SysParamModel sysParamModel2252 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2252);
		SysParamModel sysParamModel2253 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2253);
		int limit = 3;
		int minRecharge = 500000;
		int up3MinReceive = 150000;
		int up2MinReceive = 600000;
		if (sysParamModel2252 != null) {
			limit = sysParamModel2252.getVal5();
			minRecharge = sysParamModel2252.getVal3();
			up2MinReceive = sysParamModel2252.getVal2();
			up3MinReceive = sysParamModel2252.getVal1();
		}
		int down3MinReceive = 75000;
		int down2MinReceive = 150000;
		int down1MinReceive = 600000;
		if (sysParamModel2253 != null) {
			down3MinReceive = sysParamModel2253.getVal1();
			down2MinReceive = sysParamModel2253.getVal2();
			down1MinReceive = sysParamModel2253.getVal3();
		}
		Date now = new Date();
		Date start = MyDateUtil.getNowMonthFirstDay();
		HashMap<Integer, Integer> countMap = RecommenderReceiveService.agentCount(account.getHallRecommendModelMap());
		int level = account.getHallRecommendModel().getRecommend_level();

		if (level == 0) {
			long count = MongoDBServiceImpl.getInstance().queryOwnerRechargeMoney(userID, start, now);
			map.put("recharge", count);// 充值金额
			map.put("receive", 0);// 返利金额
			map.put("uplevelMoney", minRecharge);
		} else {
			map.put("recharge", 0);// 充值金额
			map.put("receive", RecommenderReceiveService.getInstance().getRecommendReceiveModel(userID).getReceive());// 返利金额
			if (level == 2) {
				map.put("uplevelMoney", up2MinReceive);// 升级最低限制
				map.put("downlevelMoney", down2MinReceive);// 降级最低限制
			} else if (level == 3) {
				map.put("uplevelMoney", up3MinReceive);// 升级最低限制
				map.put("downlevelMoney", down3MinReceive);// 降级最低限制
			} else if (level == 1) {
				map.put("uplevelMoney", 10000000);// 升级最低限制
				map.put("downlevelMoney", down1MinReceive);// 降级最低限制
			}
		}
		map.put("limit", limit);
		map.put("level", level);
		map.put("myAgents", countMap.get(1));
		map.put("myRecoms", countMap.get(2));
		map.put("myPlayers", countMap.get(3));
		resultMap.put("data", map);
		resultMap.put("result", SUCCESS);
	}

	private void receiveDetail(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		long userID;
		try {
			userID = Long.parseLong(user_ID);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		Account account = PublicServiceImpl.getInstance().getAccount(userID);
		if (account == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "玩家不存在");
			return;
		}
		if (account.getHallRecommendModel().getProxy_level() == 0) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "无权访问该页面");
			return;
		}
		Map<String, Object> map = new HashMap<>();
		if (account.getHallRecommendModel().getRecommend_level() == 0) {
			map.put("agentIncome", 0);// 代理收入
			map.put("recommenderIncome", 0);// 推广员带来的收入
		} else {
			map.put("agentIncome", RecommenderReceiveService.getInstance().querySubAgentReceive(userID));// 代理收入
			map.put("recommenderIncome", RecommenderReceiveService.getInstance().querySubRecommenderReceive(userID));// 推广员带来的收入
		}
		resultMap.put("data", map);
		resultMap.put("result", SUCCESS);
	}

	public void paybackRecord(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		long userID;
		try {
			userID = Long.parseLong(user_ID);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		// 分页算出的数据
		Query query = new Query();
		query.addCriteria(Criteria.where("account_id").is(userID));
		query.addCriteria(Criteria.where("log_type").is(ELogType.agentPayback.getId()));
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		List<NewHallRecommend> recommendIncomeList = mongoDBService.getMongoTemplate().find(query, NewHallRecommend.class);
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Map<String, Object> map = null;
		SimpleDateFormat timeStr = new SimpleDateFormat("MM-dd");
		for (NewHallRecommend model : recommendIncomeList) {
			AccountSimple accountSimple = PublicServiceImpl.getInstance().getAccountSimpe(model.getSource_id());
			map = new HashMap<>();
			map.put("nickName", accountSimple.getNick_name());
			map.put("accountId", model.getSource_id());
			String newTimeStr = timeStr.format(model.getCreate_time());
			map.put("money", -model.getV1());
			map.put("createTime", newTimeStr);
			list.add(map);
		}
		resultMap.put("result", SUCCESS);
		resultMap.put("data", list);
	}

	/**
	 * 钻石黄金推广员----下级推广员人员概况
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void subReviw(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		long userID;
		try {
			userID = Long.parseLong(user_ID);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		Map<String, Object> detailMap = null;
		try {
			detailMap = RecommenderReceiveService.getInstance().queryHallDownRecommend(userID);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (detailMap != null) {
			resultMap.put("details", detailMap.get("data"));
			resultMap.put("total", detailMap.get("total"));
		}
		resultMap.put("result", SUCCESS);// 0表示成功，-1表示失败

	}

	@SuppressWarnings("unchecked")
	private void log_update_record(long account_id, int curLevel, int oldLevel, String desc) {
		AutoUpdateRecomLevelModel levelModel = new AutoUpdateRecomLevelModel();
		levelModel.setAccount_id(account_id);
		levelModel.setCreate_time(new Date());
		levelModel.setCurLevel(curLevel);
		levelModel.setOldLevel(oldLevel);
		levelModel.setDesc(desc);
		levelModel.setType(1);
		MongoDBServiceImpl.getInstance().getLogQueue().add(levelModel);
	}

	/**
	 * 查询某人带来的收益明细
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void incomeRecord(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		String target_ID = params.get("targetID");
		Date startDate = null;
		Date endDate = null;
		long userID;
		long targetID;
		try {
			String startDateStr = params.get("startDate");
			String endDateStr = params.get("endDate");
			startDate = MyDateUtil.getZeroDate(dateFormat.parse(startDateStr));
			endDate = MyDateUtil.getTomorrowZeroDate(dateFormat.parse(endDateStr));
			userID = Long.parseLong(user_ID);
			targetID = Long.parseLong(target_ID);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		try {
			Query query = new Query();
			query.addCriteria(Criteria.where("account_id").is(userID).and("create_time").gte(startDate).lt(endDate));
			query.addCriteria(Criteria.where("log_type").is(ELogType.agentIncome.getId()).and("source_id").is(targetID));
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			List<NewHallRecommend> recommendIncomeList = mongoDBService.getMongoTemplate().find(query, NewHallRecommend.class);
			resultMap.put("data", recommendIncomeList);
		} catch (Exception e) {
			e.printStackTrace();
		}
		resultMap.put("result", SUCCESS);// 0表示成功，-1表示失败

	}
}
