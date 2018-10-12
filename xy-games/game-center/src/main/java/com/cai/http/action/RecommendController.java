/**
 * 
 */
package com.cai.http.action;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.ELogType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.AgentRecommend;
import com.cai.common.domain.AgentRecommendModel;
import com.cai.common.domain.GiveCardModel;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.SpringService;
import com.cai.http.FastJsonJsonView;
import com.cai.http.model.ErrorCode;
import com.cai.http.security.SignUtil;
import com.cai.rmi.impl.CenterRMIServerImpl;
import com.cai.service.MongoDBService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PublicServiceImpl;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBList;

import protobuf.redis.ProtoRedis.RsAccountModelResponse;

@Controller
@RequestMapping("/recommend")
public class RecommendController {

	private static Logger logger = LoggerFactory.getLogger(IndexController.class);

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
	public static final int TYPE_JUDGE_WITHDRAWS =4;
	
	// 钻石黄金推广员收益明细
	public static final int TYPE_RECOMMEND_INCOME = 5;
	
	// 钻石黄金推广员提现记录
	public static final int TYPE_RECOMMEND_OUT = 6;
	
	// 钻石黄金推广员信息总览
	public static final int TYPE_RECOMMEND_ALL = 7;
	
	// 设置代理推荐人id
	public static final int TYPE_AGENT_RECOMMEND_ID = 8;
	
	// 上级取消与下级的代理绑定关系
	public static final int TYPE_CANCEL_RECOMEND = 9;
	
	
	public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
	// 下级钻石黄金推广员详情
	
	@RequestMapping("/detail")
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
		}else if (type == TYPE_MY_MEMBER_PROXY) {// ---钻石黄金推广员---我的会员代理
			myMemberProxy(params, resultMap);
		} else if (type == TYPE_JUDGE_WITHDRAWS) {// ---钻石黄金推广员---判断能否提现，不做入库的操作
			judgeWithdraws(params, resultMap);
		} else if (type == TYPE_RECOMMEND_ALL) {
			doRecommendAll(params, resultMap);
		} else if (type == TYPE_AGENT_RECOMMEND_ID) {
			setRecommendAgentId(params, resultMap);
		} else if (type == TYPE_CANCEL_RECOMEND) {
			cancelAgentRecommend(params, resultMap);
		} 
		return new ModelAndView(new FastJsonJsonView(), resultMap);
	}
	/**
	 * 钻石黄金推广员收益记录
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void doRecommendIncome(Map<String, String> params, Map<String, Object> resultMap) {
		Date startDate = null;
		Date endDate = null;

		String pageIndex = params.get("pageIndex");
		String pageSize = params.get("pageSize");
		String targetID = params.get("targetID");//查询输入的userId带来的收益记录
		String account_id = params.get("userID");
		int newPageIndex = 0;
		int newPageSize = 0;
		long targetId = 0;
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
			targetId = Long.parseLong(targetID);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		if(targetId>0){
			getRecordByUserId(Long.parseLong(account_id), targetId, resultMap);
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
		// type类型，1查自己获利，2查二级获利
		// V2 每个值代表的 0，自己推荐的，2，二级代理推荐，3，三级代理推荐,
		// 4,一级钻石黄金推广员的代理充值返利，5，二级钻石黄金推广员的代理充值返利，6三级钻石黄金推广员的代理充值返现
		BasicDBList values = new BasicDBList();
		if (type == 1) {
			values.add(4);
			query.addCriteria(Criteria.where("v2").in(values));
		} else if (type == 2) {
			values.add(5);
			query.addCriteria(Criteria.where("v2").in(values));
		}
		query.with(new Sort(Direction.DESC, "create_time"));
		List<AgentRecommend> recommendIncomeList = mongoDBService.getMongoTemplate().find(query, AgentRecommend.class);
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Map<String, Object> map = null;
//		Double totalBalance = 0.00;
		SimpleDateFormat timeStr = new SimpleDateFormat("yyyy/MM/dd");
		for (AgentRecommend model : recommendIncomeList) {
			map = Maps.newConcurrentMap();
			map.put("accountId", model.getAccount_id());
			map.put("getBalance", model.getV1() / 100.00);
			map.put("comeSource", model.getV2());
			map.put("activity", model.getMsg().split("\\|")[0]);
			String newTimeStr = timeStr.format(model.getCreate_time());
			map.put("createTime", newTimeStr);
			map.put("sourceId", model.getAccount_id());
//			totalBalance += model.getV1();
			if(model.getTarget_id()>0){
				Account account = PublicServiceImpl.getInstance().getAccount(model.getTarget_id());
				map.put("targetId", model.getTarget_id());
				map.put("nickName", account.getAccountWeixinModel()==null?"-":account.getAccountWeixinModel().getNickname());
			}else{
				map.put("nickName", "-");
				map.put("targetId", 0);
			}
			//充值返利
			map.put("recharge_receive", model.getRecharge_money()/100.00+"/"+model.getV1()/100.00);
			list.add(map);
		}
//		AggregationOperation match = Aggregation.match(Criteria.where("account_id").is(account_id).and("log_type").is(ELogType.agentIncome.getId()));
//		AggregationOperation group = Aggregation.group().sum("v1").as("count").count().as("line");
//		Aggregation aggregation = Aggregation.newAggregation(match, group);
//		AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "agent_recommend",
//				GiveCardModel.class);
//		List<GiveCardModel> sumLlist = result.getMappedResults();
//		long totalBalance = 0;
//		if (sumLlist != null && sumLlist.size() > 0) {
//			GiveCardModel giveCardModel = sumLlist.get(0);
//			totalBalance = giveCardModel.getCount();
//		}
		Account account = SpringService.getBean(CenterRMIServerImpl.class).getAccount(Long.parseLong(account_id));
		resultMap.put("totalBalance", account.getAccountModel().getRecommend_history_income() );//总收益
		resultMap.put("result", SUCCESS);
		resultMap.put("data", list);
	}
	
	private void getRecordByUserId(long accountId,long userId ,Map<String, Object> resultMap){
//		Account account = PublicServiceImpl.getInstance().getAccount(accountId);
//		if(account.getAgentRecommendModelMap()!=null){
//			if(!account.getAgentRecommendModelMap().containsKey(userId)){
//				resultMap.put("result", FAIL);
//				resultMap.put("msg", "不能查看非自己下级代理的信息");
//				return ;
//			}
//		}
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("account_id").is(accountId).and("target_id").is(userId));
		query.addCriteria(Criteria.where("log_type").is(ELogType.agentIncome.getId()));
		// type类型，1查自己获利，2查二级获利
		// V2 每个值代表的 0，自己推荐的，2，二级代理推荐，3，三级代理推荐,
		// 4,一级钻石黄金推广员的代理充值返利，5，二级钻石黄金推广员的代理充值返利，6三级钻石黄金推广员的代理充值返现
		query.with(new Sort(Direction.DESC, "create_time"));
		List<AgentRecommend> recommendIncomeList = mongoDBService.getMongoTemplate().find(query, AgentRecommend.class);
		Map<String, Object> map = null;
//		Double totalBalance = 0.00;
		SimpleDateFormat timeStr = new SimpleDateFormat("yyyy/MM/dd");
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Account targetAccount = PublicServiceImpl.getInstance().getAccount(userId);
		for (AgentRecommend model : recommendIncomeList) {
			map = Maps.newConcurrentMap();
			map.put("accountId", model.getAccount_id());
			map.put("getBalance", model.getV1() / 100.00);
			map.put("comeSource", model.getV2());
			map.put("activity", model.getMsg().split("\\|")[0]);
			String newTimeStr = timeStr.format(model.getCreate_time());
			map.put("createTime", newTimeStr);
//			totalBalance += model.getV1();
			if(model.getTarget_id()>0){
				map.put("targetId", model.getTarget_id());
				map.put("nickName", targetAccount.getAccountWeixinModel()==null?"-":targetAccount.getAccountWeixinModel().getNickname());
			}else{
				map.put("nickName", "-");
				map.put("targetId", 0);
			}
			//充值返利
			map.put("recharge_receive", model.getRecharge_money()/100.00+"/"+model.getV1()/100.00);
			list.add(map);
		}
		Account account = SpringService.getBean(CenterRMIServerImpl.class).getAccount(accountId);
		resultMap.put("totalBalance", account.getAccountModel().getRecommend_history_income() );//总收益
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
		if (account == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", ErrorCode.ROLE_FIND_FAIL);
			return;
		}

		DecimalFormat df = new DecimalFormat("######0.00");
		String canRMB = df.format(account.getAccountModel().getRecommend_remain_income());

		resultMap.put("userId", userID);
		resultMap.put("result", SUCCESS);
		resultMap.put("nickName", account.getAccountWeixinModel()!=null?account.getAccountWeixinModel().getNickname():"-");
		resultMap.put("canRMB", canRMB);// 钻石黄金推广员可提现
		resultMap.put("promoteLevel", account.getAccountModel().getProxy_level());// 钻石黄金推广员等级
		resultMap.put("gold", account.getAccountModel().getGold());// 闲逸豆
		resultMap.put("agentRecommendId", account.getAccountModel().getRecommend_agent_id());//代理推荐人id
		long sum = MongoDBServiceImpl.getInstance().getAgentYesterdayIncome(userID);// 获取到的是分为单位
		double yesterdayRMB = sum / 100.00;
		resultMap.put("yesterdayRMB", yesterdayRMB);// 钻石黄金推广员 昨日收益

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
		List<AgentRecommend> recommendIncomeList = mongoDBService.getMongoTemplate().find(query, AgentRecommend.class);
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Map<String, Object> map = null;
		Account account = PublicServiceImpl.getInstance().getAccount(Long.parseLong(account_id));
		for (AgentRecommend model : recommendIncomeList) {
			map = Maps.newConcurrentMap();
			map.put("nickName", account.getAccountWeixinModel() != null?account.getAccountWeixinModel().getNickname():"");
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
		AggregationOperation match = Aggregation.match(Criteria.where("account_id").is(account_id).and("log_type").is(ELogType.agentIncome.getId()));
		AggregationOperation group = Aggregation.group().sum("v1").as("count").count().as("line");
		Aggregation aggregation = Aggregation.newAggregation(match, group);
		AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "agent_recommend",
				GiveCardModel.class);
		List<GiveCardModel> sumLlist = result.getMappedResults();
		long totalBalance = 0;//累计提现金额
		if (sumLlist != null && sumLlist.size() > 0) {
			GiveCardModel giveCardModel = sumLlist.get(0);
			totalBalance = giveCardModel.getCount();
		}
		resultMap.put("result", SUCCESS);
		resultMap.put("totalBalance", totalBalance / 100.00);
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
	 * 钻石黄金推广员----我要提现（既要判断，还要作操作入库的）
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void myWithdraws(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		Double income = Double.parseDouble(params.get("income"));// 提现金额,假设传过来的是正数
		String desc = "提现";// 描述
		long userID;
		try {
			income = -income;// 将金额数变成负数
			userID = Long.parseLong(user_ID);
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
		if (income % 50 != 0 && income != 0) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "您输入的金额非整五十或整五十数，请重新输入");
			return;
		}

		CenterRMIServerImpl centerRMIServer = SpringService.getBean(CenterRMIServerImpl.class);
		AddGoldResultModel resultModel = centerRMIServer.doAgentIncome(userID, income,0l,desc,EGoldOperateType.AGENT_RECHARGE_DRAWCASH, 0, 0);
		DecimalFormat df = new DecimalFormat("######0.00");
		String canRMB = df.format(resultModel.getAccountModel().getRecommend_remain_income());
		if (!resultModel.isSuccess()) {
			resultMap.put("result", FAIL);// 0表示成功，-1表示失败
		} else {
			resultMap.put("result", SUCCESS);// 0表示成功，-1表示失败
		}
		resultMap.put("msg", resultModel.getMsg());// 返回到类型的消息
		resultMap.put("canRMB", canRMB);// 更新后钻石黄金推广员可提现金额
		resultMap.put("yesterdayRMB", resultModel.getAccountModel().getRecommend_yesterday_income());// 更新后钻石黄金推广员昨日收益
		resultMap.put("gold", resultModel.getAccountModel().getGold());// 更新后钻石黄金推广员闲逸豆

	}
	/**
	 * 钻石黄金推广员----我的会员代理
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void myMemberProxy(Map<String, String> params, Map<String, Object> resultMap) {

		String account_id = params.get("userID");
		Date startDate = null;
		Date endDate = null;
		try {
			String startDateStr = params.get("startDate");
			String endDateStr = params.get("endDate");
			startDate = dateFormat.parse(startDateStr);
			endDate = dateFormat.parse(endDateStr);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		try {
			Map<String, Object> detailMap = SpringService.getBean(CenterRMIServerImpl.class).queryDownRecommend(Long.parseLong(account_id),MyDateUtil.getZeroDate(startDate),
					MyDateUtil.getTomorrowZeroDate(endDate));
			if (detailMap != null) {
				resultMap.put("details", detailMap.get("data"));
			}
		} catch (Exception e) {
			logger.error("myMemberProxy插入日志异常" + e);
		}
		resultMap.put("result", SUCCESS);

	}

	/**
	 * 钻石黄金推广员信息总览
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void doRecommendAll(Map<String, String> params, Map<String, Object> resultMap) {
		String account_id = params.get("userID");
		resultMap.put("result", SUCCESS);
		try {
			Map<String, Object> detailMap = SpringService.getBean(CenterRMIServerImpl.class).queryAgentRecommendAll(Long.parseLong(account_id));
			if (detailMap != null) {
				resultMap.put("data", detailMap);
			}
		} catch (Exception e) {
			logger.error("doRecommendAll插入日志异常" + e);
		}

	}
	private void setRecommendAgentId(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");//自己的id
		String target_ID = params.get("targetUserID");//代理推荐人id
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
		if(targetID==0){
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		Account account = PublicServiceImpl.getInstance().getAccount(userID);
		if (account == null||account.getAccountModel().getIs_agent()==0) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "玩家不是代理,不能设置代理推荐人");
			return;
		}
		if(account.getAccountModel().getRecommend_agent_id()>0){
			resultMap.put("result", FAIL);
			resultMap.put("msg", "您已经设置过推荐人,不能再设置");
			return;
		}
		int level = account.getAccountModel().getProxy_level();
		if(level == 1){
			resultMap.put("result", FAIL);
			resultMap.put("msg", "钻石推广员已经是最高级别，不能设置代理推荐人");
			return;
		}
		Account targetAccount = PublicServiceImpl.getInstance().getAccount(targetID);
		if (targetAccount == null||targetAccount.getAccountModel().getIs_agent()==0) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "目标玩家不是代理，不能成为推荐人");
			return;
		}
		if(level == targetAccount.getAccountModel().getProxy_level()){
			resultMap.put("result", FAIL);
			resultMap.put("msg", "不能设置同级别的代理为推荐人");
			return;	
		}
		if(level-1!=targetAccount.getAccountModel().getProxy_level()){
			resultMap.put("result", FAIL);
			resultMap.put("msg", "不能越级设置推荐人");
			return;
		}
		
		RsAccountModelResponse.Builder rsAccountModelResponse = RsAccountModelResponse.newBuilder();
		rsAccountModelResponse.setAccountId(userID);
		rsAccountModelResponse.setAgentRecommentId(targetID);
		SpringService.getBean(CenterRMIServerImpl.class).ossModifyAccountModel(rsAccountModelResponse.build());
		resultMap.put("result", SUCCESS);
	}
	private void cancelAgentRecommend(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");//自己的id
		String target_ID = params.get("targetUserID");//代理推荐人id
		// 验证必填参数格式
		String[] targetIDs = target_ID.split(",");
		long userID;
		try {
			userID = Long.parseLong(user_ID);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		Account account = PublicServiceImpl.getInstance().getAccount(userID);
		if (account == null||account.getAccountModel().getIs_agent()==0) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "非代理无权操作");
			return;
		}
		if (account.getAccountModel().getProxy_level()>2) {//三级代理没有取消功能
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数有误");
			return;
		}
		for(String id:targetIDs){
			Long targetID = null;
			try {
				targetID = Long.parseLong(id);
			} catch (Exception e) {
				resultMap.put("result", FAIL);
				resultMap.put("msg", "参数异常");
				return;
			}
			
			Map<Long, AgentRecommendModel> agentRecommendModelMap = account.getAgentRecommendModelMap();
			if(agentRecommendModelMap==null||agentRecommendModelMap.size()==0){
				resultMap.put("result", FAIL);
				resultMap.put("msg", "非下级代理不能取消关系");
				return;
			}
			if(agentRecommendModelMap.containsKey(targetID)){
				//删除绑定关系
				RsAccountModelResponse.Builder rsAccountModelResponse = RsAccountModelResponse.newBuilder();
				rsAccountModelResponse.setAccountId(targetID);
				rsAccountModelResponse.setAgentRecommentId(0);
				SpringService.getBean(CenterRMIServerImpl.class).ossModifyAccountModel(rsAccountModelResponse.build());
				resultMap.put("result", SUCCESS);
			}else{
				resultMap.put("result", FAIL);
				resultMap.put("msg", "已经取消，无需再次请求");
			}
		}
		
		
		
	}
}
