package com.cai.http.action;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.cai.common.domain.Account;
import com.cai.common.domain.AccountSimple;
import com.cai.common.domain.GiveCardModel;
import com.cai.common.domain.zhuzhou.AccountZZPromoterModel;
import com.cai.common.domain.zhuzhou.IndexModel;
import com.cai.common.domain.zhuzhou.RechargeRankModel;
import com.cai.common.domain.zhuzhou.RechargeRecordModel;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.SpringService;
import com.cai.http.FastJsonJsonView;
import com.cai.http.model.ErrorCode;
import com.cai.http.security.SignUtil;
import com.cai.service.MongoDBService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PublicService;
import com.cai.service.PublicServiceImpl;
import com.cai.service.ZZPromoterService;
import com.google.common.collect.Maps;

@Controller
@RequestMapping("/zzxh")
public class ZZAssociationController {

	private static Logger logger = LoggerFactory.getLogger(ZZAssociationController.class);

	/**
	 * 成功
	 */
	public final static int SUCCESS = 0;

	/**
	 * 失败
	 */
	public final static int FAIL = -1;

	// 株洲麻将协会----首页
	public static final int TYPE_INDEX = 1;

	// 株洲麻将协会----收益详情
	public static final int TYPE_INCOME_DETAILS = 2;

	// 株洲麻将协会----我要提现
	public static final int TYPE_DRAW_CASH = 3;

	// 株洲麻将协会----分享绑定账号的
	public static final int TYPE_SHARE_BIND = 4;

	// 株洲麻将协会----绑定玩家列表
	public static final int TYPE_BIND_PLAYERS = 5;

	// 株洲麻将协会----充值排行
	public static final int TYPE_RECHANGE_RANK = 6;

	// 株洲麻将协会----绑定玩家的收益详情
	public static final int TYPE_BIND_PLAYER_INCOMES = 7;

	public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

	@RequestMapping("/details")
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
		if (type == TYPE_INDEX) {
			IndexInfo(params, resultMap);
		} else if (type == TYPE_INCOME_DETAILS) {
			IncomeDetailsInfo(params, resultMap);
		} else if (type == TYPE_DRAW_CASH) {
			drawCash(params, resultMap);
		} else if (type == TYPE_SHARE_BIND) {
			shareBind(params, resultMap);
		} else if (type == TYPE_BIND_PLAYERS) {
			bindPlayers(params, resultMap);
		} else if (type == TYPE_RECHANGE_RANK) {
			rechargeRank(params, resultMap);
		} else if (type == TYPE_BIND_PLAYER_INCOMES) {
			targetIncomeDetailsInfo(params, resultMap);
		}
		return new ModelAndView(new FastJsonJsonView(), resultMap);
	}

	/**
	 * 株洲麻将协会------根据用户ID查询相关的首页信息
	 * 
	 * @param request
	 * @param params
	 */
	private void IndexInfo(Map<String, String> params, Map<String, Object> resultMap) {
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
		// || account.getAccountModel().getIs_agent() == 0
		if (account == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", ErrorCode.VISIT_FAIL);
			return;
		}

		IndexModel indexInfo = ZZPromoterService.getInstance().getIndex(userID);
		if (indexInfo == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", ErrorCode.VISIT_FAIL);
			return;
		}
		DecimalFormat df = new DecimalFormat("######0.00");
		String canRMB = df.format(indexInfo.getRemainIncome());
		resultMap.put("userId", userID);
		resultMap.put("result", SUCCESS);
		resultMap.put("headUrl", indexInfo.getIcon() != null ? indexInfo.getIcon() : "");
		resultMap.put("nickName", indexInfo.getNick() != null ? indexInfo.getNick() : "-");
		resultMap.put("canRMB", canRMB);
		resultMap.put("historyIncome", indexInfo.getHistoryIncome());
		resultMap.put("yesterdayIncome", indexInfo.getYesterdayIncome());
		resultMap.put("yesterdayBindCount", indexInfo.getYesterdayBind());
		resultMap.put("historyDrawCash", indexInfo.getHistoryDrawCash());
		resultMap.put("totalBindCount", indexInfo.getTotalBind());
		resultMap.put("yesterdayRechargeCount", indexInfo.getYesterdayRecharge());

	}

	/**
	 * 株洲麻将协会------根据用户ID查询相关的收益信息
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void IncomeDetailsInfo(Map<String, String> params, Map<String, Object> resultMap) {
		Date startDate = null;
		Date endDate = null;

		String pageIndex = params.get("pageIndex");
		String pageSize = params.get("pageSize");
		String typeStr = params.get("type"); // 0收益，1提现
		int newPageIndex = 0;
		int newPageSize = 5;
		int type;
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
			type = Integer.parseInt(typeStr);
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
		query.addCriteria(Criteria.where("accountId").is(Long.parseLong(account_id)));
		query.addCriteria(Criteria.where("type").is(type));
		query.addCriteria(Criteria.where("create_time").gte(MyDateUtil.getZeroDate(startDate)).lt(MyDateUtil.getTomorrowZeroDate(endDate)));
		query.with(new Sort(Direction.DESC, "create_time")).skip(newPageIndex * newPageSize).limit(newPageSize);
		List<RechargeRecordModel> rechargeIncomeList = mongoDBService.getMongoTemplate().find(query, RechargeRecordModel.class);
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Map<String, Object> map = null;

		int totalRecharge = 0;
		int totalBalance = 0;
		for (RechargeRecordModel model : rechargeIncomeList) {
			map = Maps.newConcurrentMap();
			AccountSimple targetAccount = PublicServiceImpl.getInstance().getAccountSimpe(model.getRechargeId());
			if (targetAccount == null) {
				map.put("rechargeNickName", "~");
			} else {
				map.put("rechargeNickName", targetAccount.getNick_name());
			}
			map.put("rechargeId", model.getRechargeId());
			SimpleDateFormat timeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String newTimeStr = timeStr.format(model.getCreate_time());
			map.put("createTime", newTimeStr);
			map.put("comeSource", model.getPayType());
			map.put("rechargeMoney", model.getRechargeMoney());
			map.put("receive", model.getReceive());
			totalRecharge += model.getRechargeMoney();
			totalBalance += model.getReceive();
			list.add(map);
		}
		resultMap.put("result", SUCCESS);
		resultMap.put("totalRecharge", totalRecharge);
		resultMap.put("totalBalance", totalBalance);
		resultMap.put("data", list);

	}

	/**
	 * 株洲麻将协会------根据用户ID查询相关的收益信息
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void targetIncomeDetailsInfo(Map<String, String> params, Map<String, Object> resultMap) {
		Date startDate = null;
		Date endDate = null;

		String pageIndex = params.get("pageIndex");
		String pageSize = params.get("pageSize");
		String typeStr = params.get("type"); // 0收益，1提现
		int newPageIndex = 0;
		int newPageSize = 5;
		int type;
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
			type = Integer.parseInt(typeStr);
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
		query.addCriteria(Criteria.where("rechargeId").is(Long.parseLong(account_id)));
		query.addCriteria(Criteria.where("type").is(type));
		query.addCriteria(Criteria.where("create_time").gte(MyDateUtil.getZeroDate(startDate)).lt(MyDateUtil.getTomorrowZeroDate(endDate)));
		query.with(new Sort(Direction.DESC, "create_time")).skip(newPageIndex * newPageSize).limit(newPageSize);
		List<RechargeRecordModel> rechargeIncomeList = mongoDBService.getMongoTemplate().find(query, RechargeRecordModel.class);
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Map<String, Object> map = null;

		int totalRecharge = 0;
		int totalBalance = 0;
		for (RechargeRecordModel model : rechargeIncomeList) {
			map = Maps.newConcurrentMap();
			SimpleDateFormat timeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String newTimeStr = timeStr.format(model.getCreate_time());
			map.put("createTime", newTimeStr);
			map.put("comeSource", model.getPayType());
			map.put("rechargeMoney", model.getRechargeMoney());
			map.put("receive", model.getReceive());
			totalRecharge += model.getRechargeMoney();
			totalBalance += model.getReceive();
			list.add(map);
		}

		GiveCardModel cardModel = MongoDBServiceImpl.getInstance().getPromoteObjectTotalRecharge(Long.parseLong(account_id));
		if (cardModel == null) {
			resultMap.put("totalSingleRecharge", 0);
			resultMap.put("totalSingleBalance", 0);
		} else {
			resultMap.put("totalSingleRecharge", cardModel.getCount());
			resultMap.put("totalSingleBalance", cardModel.getLine());
		}

		AccountSimple targetAccount = PublicServiceImpl.getInstance().getAccountSimpe(Long.parseLong(account_id));
		resultMap.put("rechargeNickName", targetAccount.getNick_name() == null ? "-" : targetAccount.getNick_name());
		resultMap.put("rechargeHeadUrl", targetAccount.getIcon());
		resultMap.put("rechargeId", account_id);
		resultMap.put("result", SUCCESS);
		resultMap.put("totalRecharge", totalRecharge);
		resultMap.put("totalBalance", totalBalance);
		resultMap.put("data", list);

	}

	/**
	 * 株洲麻将协会-----我要提现(只作判断，能不能提现)
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void drawCash(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		String incomeStr = params.get("income");// 提现金额,假设传过来的是正数

		long userID;
		int money;
		try {
			money = Integer.parseInt(incomeStr);// 将金额数变成负数
			userID = Long.parseLong(user_ID);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}

		int flag = ZZPromoterService.getInstance().drawCash(userID, money, "提现");

		if (flag == 1) {
			resultMap.put("result", FAIL);// 0表示成功，-1表示失败
			resultMap.put("msg", "提现成功");// 返回到类型的消息
		} else {
			resultMap.put("result", SUCCESS);// 0表示成功，-1表示失败
			resultMap.put("msg", "提现失败");// 返回到类型的消息
		}

	}

	/**
	 * 株洲麻将协会------分享绑定的账号之间的关系的
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void shareBind(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		String target_ID = params.get("targetID");

		long userID;
		long targetID;
		try {
			userID = Long.parseLong(user_ID);
			targetID = Long.parseLong(target_ID);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		boolean canBind = ZZPromoterService.getInstance().addPromoterObject(userID, targetID);
		if (canBind) {
			resultMap.put("result", SUCCESS);
			resultMap.put("msg", "绑定成功");
		} else {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "绑定失败");
		}

	}

	/**
	 * 株洲麻将协会------绑定的玩家的列表
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void bindPlayers(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		String pageIndex = params.get("pageIndex");
		String pageSize = params.get("pageSize");
		String typeStr = params.get("type");// 0 查所有的 1查单个的
		int newPageIndex = 0;
		int newPageSize = 5;
		long userID;
		int type;
		try {
			if (pageIndex == null || pageSize == null) {
				newPageIndex = 0;
				newPageSize = 5;
			} else {
				newPageIndex = Integer.valueOf(pageIndex);
				newPageSize = Integer.valueOf(pageSize);
			}
			userID = Long.parseLong(user_ID);
			type = Integer.parseInt(typeStr);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		PublicService publicService = SpringService.getBean(PublicService.class);
		if (type == 0) {
			List<AccountZZPromoterModel> bindPlayerList = publicService.getPublicDAO().getAccountZZPromoterModelList(userID, newPageIndex,
					newPageSize);

			List<Map<String, Object>> playerInfo = new ArrayList<Map<String, Object>>();

			for (AccountZZPromoterModel model : bindPlayerList) {
				Map<String, Object> player = new HashMap<String, Object>();
				long targetId = model.getTarget_id();
				AccountSimple targetAccount = PublicServiceImpl.getInstance().getAccountSimpe(targetId);
				if (targetAccount == null) {
					continue;
				}
				player.put("account_id", model.getAccount_id());
				player.put("target_id", model.getTarget_id());
				player.put("create_time", model.getCreate_time());
				player.put("level", model.getLevel());
				player.put("nick", targetAccount.getNick_name());
				player.put("icon", targetAccount.getIcon());
				playerInfo.add(player);
			}
			resultMap.put("bindPlayerList", playerInfo);
		} else {
			AccountZZPromoterModel bindPlay = ZZPromoterService.getInstance().getAccountZZPromoterModel(userID);
			if (bindPlay == null) {
				resultMap.put("bindPlayerList", bindPlay);
			} else {
				Map<String, Object> player = new HashMap<String, Object>();
				long targetId = bindPlay.getTarget_id();
				AccountSimple targetAccount = PublicServiceImpl.getInstance().getAccountSimpe(targetId);
				if (targetAccount == null) {
				}
				player.put("account_id", bindPlay.getAccount_id());
				player.put("target_id", bindPlay.getTarget_id());
				player.put("create_time", bindPlay.getCreate_time());
				player.put("level", bindPlay.getLevel());
				player.put("nick", targetAccount.getNick_name());
				player.put("icon", targetAccount.getIcon());
				resultMap.put("bindPlayerList", player);
			}

		}

		resultMap.put("result", SUCCESS);

	}

	/**
	 * 株洲麻将协会------充值排行
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void rechargeRank(Map<String, String> params, Map<String, Object> resultMap) {
		String typeStr = params.get("type"); // 1.总排行 2.本月排行 3昨日排行
		String user_ID = params.get("userID");
		int type;
		long userID;
		try {
			type = Integer.parseInt(typeStr);
			userID = Long.parseLong(user_ID);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		Set<RechargeRankModel> rankInfo = ZZPromoterService.getInstance().getRankByType(userID, type);
		resultMap.put("result", SUCCESS);
		resultMap.put("rankInfo", rankInfo);

	}

}
