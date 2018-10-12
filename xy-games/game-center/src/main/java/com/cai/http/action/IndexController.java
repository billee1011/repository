package com.cai.http.action;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.cai.common.config.ExclusiveGoldCfg;
import com.cai.common.constant.GameConstants;
import com.cai.common.constant.RMICmd;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.EBonusPointsType;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.ELogType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.define.ESellType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountGroupModel;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AccountProxyModel;
import com.cai.common.domain.AccountSimple;
import com.cai.common.domain.AddCardLog;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.AutoUpdateRecomLevelModel;
import com.cai.common.domain.BrandResultModel;
import com.cai.common.domain.ClubGroupModel;
import com.cai.common.domain.EveryDayRobotModel;
import com.cai.common.domain.GameRecommend;
import com.cai.common.domain.GlobalModel;
import com.cai.common.domain.OneProxyAccountReplaceRoomModel;
import com.cai.common.domain.Page;
import com.cai.common.domain.ProxyAccountReplaceRoomModel;
import com.cai.common.domain.ProxyGoldLogModel;
import com.cai.common.domain.RevicerRmbLogModel;
import com.cai.common.domain.RmiDTO;
import com.cai.common.domain.RobotModel;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.domain.ShopModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.zhuzhou.AccountZZPromoterModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.rmi.IClubRMIServer;
import com.cai.common.rmi.ILogicRMIServer;
import com.cai.common.util.DescParams;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.IDGeneratorOrder;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.dictionary.ShopDict;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.domain.RobotRoom;
import com.cai.future.runnable.PayCenterRunnable;
import com.cai.http.FastJsonJsonView;
import com.cai.http.model.ErrorCode;
import com.cai.http.model.ProductListInfoResponse.Product;
import com.cai.http.security.SignUtil;
import com.cai.redis.service.RedisService;
import com.cai.rmi.impl.CenterRMIServerImpl;
import com.cai.service.BonusPointsService;
import com.cai.service.ClubExclusiveService;
import com.cai.service.MongoDBService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PublicService;
import com.cai.service.PublicServiceImpl;
import com.cai.service.RMIServiceImpl;
import com.cai.service.RecommenderReceiveService;
import com.cai.service.RedisServiceImpl;
import com.cai.service.ZZPromoterService;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBList;

import javolution.util.FastMap;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountModelResponse;
import protobuf.redis.ProtoRedis.RsAccountProxyResponse;
import protobuf.redis.ProtoRedis.RsAccountResponse;

@Controller
@RequestMapping("/index")
public class IndexController {

	private static Logger logger = LoggerFactory.getLogger(IndexController.class);

	/**
	 * 成功
	 */
	public final static int SUCCESS = 0;

	/**
	 * 失败
	 */
	public final static int FAIL = -1;

	// 获取用户--unionID
	public static final int TYPE_UNIONID = 1;
	// 获取用户--用户ID
	public static final int TYPE_USER = 2;
	// 我的订单 -- 根据用户ID查询转卡记录
	public static final int TYPE_ORDER = 3;
	// 我的下级代理
	public static final int TYPE_PROXY_LIST = 4;
	// 转卡
	public static final int TYPE_TURN_CARD = 5;
	// 删除一级代理
	public static final int TYPE_DELETE_PROXY = 6;
	// 设置为我的一级代理
	public static final int TYPE_SET_PROXY = 7;
	// 备注我的一级代理
	public static final int TYPE_REMARK_PROXY = 8;

	public static final int TYPE_GIVE_CARD = 9;

	// 根据用户查询基本情况
	public static final int TYPE_USER_DETAIL = 11;

	// 昨日下级代理 消耗 明细
	public static final int TYPE_PROXY_DETAIL = 12;

	// 根据用户查询收益明细
	public static final int TYPE_RMB_DETAIL = 13;

	// 提现明细
	public static final int TYPE_RECEIVE_RMB_DETAIL = 14;

	// 推广员----首页
	public static final int TYPE_RECOMENT_INDEX = 15;

	// 推广员----我要提现
	public static final int TYPE_MY_WITHDRAWS = 16;

	// 推广员----我的会员玩家
	public static final int TYPE_MY_MEMBERPLAYER = 17;

	// 推广员----我的会员代理
	public static final int TYPE_MY_MEMBER_PROXY = 18;

	// 推广员----判断能否提现
	public static final int TYPE_JUDGE_WITHDRAWS = 19;

	// 福禄寿代理统计昨日消耗
	public static final int TYPE_FLS_AGENTTJ_DAY = 20;
	// 福禄寿代理统计月消耗
	public static final int TYPE_FLS_AGENTTJ_MONTH = 21;
	// 福禄寿代理统计季度消耗
	public static final int TYPE_FLS_AGENTTJ_SEASON = 22;

	// 推广员----我的下线
	public static final int TYPE_MY_NEXTLEVEL = 23;

	// 新的日常记录
	public static final int TYPE_DALIY_RECORD = 24;

	// 50开始机器人
	// 机器人开房
	public static final int TYPE_OPEN_ROOM = 50;

	// 机器人每日开房详情--根据群ID查
	public static final int TYPE_ROOM_DAY_DETAIL_GROUP = 51;

	// 机器人每日开房详情--用户ID查
	public static final int TYPE_ROOM_DAY_DETAIL_USER = 52;

	// 机器人开房详情--群ID查
	public static final int TYPE_ROOM_DETAIL_GROUP_REALTIME = 53;

	// 绑定ID
	public static final int TYPE_ROOM_BANGDING_ID = 54;

	// 解除绑定ID
	public static final int TYPE_ROOM_JIE_BANG = 55;

	// 绑定详情
	public static final int TYPE_ROOM_BAND_DETAIL = 56;

	// 解散房间
	public static final int TYPE_ROOM_DELETE = 90;
	// 获取商品列表
	public static final int TYPE_SHOP = 66;
	// 充值
	public static final int TYPE_PAY = 77;

	// 修复订单
	public static final int TYPE_PAY_REPAIR = 100;

	private static final String ACCOUNT_NAME = "中心充值商品";

	private static final String ACCOUNT_RECOMMEND_NAME = "推广员代充商品";

	private static final String ACCOUNT_SHOP_NAME = "店铺充值商品";
	// 推广员收益明细
	private static final int TYPE_RECOMMEND_INCOME = 80;
	// 推广员提现记录
	private static final int TYPE_RECOMMEND_OUT = 81;
	// 推广员信息总览
	private static final int TYPE_RECOMMEND_ALL = 82;
	// 下级推广员详情
	private static final int TYPE_DOWN_RECOMMEND_DETAIL = 83;
	// 设置下级推广员或者取消下级推广员身份
	private static final int TYPE_DOWN_RECOMMEND = 84;
	// 统计端午节红包活动
	private static final int TYPE_INIT_ACTIVE = 666;

	// 俱乐部绑定群
	private static final int TYPE_CLUB_BIND_GROUP = 58;
	// 俱乐部绑定成员{clubId,accountId, userID}
	private static final int TYPE_CLUB_BIND_ACCOUNT = 57;
	// 创建俱乐部 {clubName,clubDesc, game_type_index,game_round,
	// game_rule_index,game_rule_index_ex,baseScore,maxTimes,base_score_gang,base_score_ci}
	private static final int TYPE_CLUB_CREATE = 59;
	// 俱乐部修改玩法 描述名字 {clubId,clubName,clubDesc, game_type_index,game_round,
	// game_rule_index,game_rule_index_ex,baseScore,maxTimes,base_score_gang,base_score_ci
	// }
	private static final int TYPE_CLUB_UPDATE = 60;
	// delete 删除俱乐部 {clubId,userID}
	private static final int TYPE_CLUB_DELETE = 61;
	// 获取我的俱乐部 {userID} Response{data:List<ClubModel>}
	private static final int TYPE_CLUB_MY = 62;
	// 获取俱乐部成员{userID,clubId，status} Response
	private static final int TYPE_CLUB_MEMBERS = 63;
	// 俱乐部解绑 {clubId,accountId,userID}
	private static final int TYPE_CLUB_UNBIND_ACCOUNT = 64;
	// 俱乐部详情 {clubId,accountId}
	private static final int TYPE_CLUB_DETAIL = 65;
	// 俱乐部批量审核成员
	private static final int TYPE_CLUB_BIND_ACCOUNT_BATCH = 66;
	// 俱乐部批量解绑
	private static final int TYPE_CLUB_UNBIND_ACCOUNT_BATCH = 67;
	// 俱乐部成员搜索
	private static final int TYPE_CLUB_SEARCH_ACCOUNT = 68;
	// 俱乐部每日消耗查询
	private static final int TYPE_CLUB_COST = 69;

	// 俱乐部牌桌数据查询
	private static final int TYPE_CLUB_ROOM = 70;
	// 查询系统状态
	private static final int TYPE_SYSTEM_STATE = 73;
	// 查询商品状态
	private static final int TYPE_GOODS_STATE = 74;
	// 充值加豆，不走商品
	private static final int TYPE_ADD_GOLD = 75;
	// 查看房间详情
	private static final int TYPE_ROOM_DETAIL = 72;
	// 通过unionId+groupId获取用户信息
	private static final int TYPE_ROBOT_UNION = 10;
	// 查看战绩
	private static final int TYPE_ROBOT_RESULT = 25;

	private static final int TYPE_PH_PAY = 99;
	private static final int INIT = 199;
	// @ModelAttribute("player")
	// public Player getPlayer() {
	// System.out.println("0001");
	// Player player = SpUtil.playerService.get(6L);
	//
	// return player;
	// }

	// @RequestMapping("/myt")
	// //@ResponseBody
	// public Player index() {
	// System.out.println("ok");
	// System.out.println("0001");
	// Player player = SpUtil.playerService.get(6L);
	// //int k = 1/0;
	// return player;
	// }
	//
	// @RequestMapping(value="/register")
	// public String register(Model model){
	// System.out.println("注册");
	// model.addAttribute("w","wwwww");
	// //Player player = SpUtil.playerService.get(6L);
	// //int k = 1/0;
	//
	// //return "myt";
	// return "myt";
	// }
	//
	// @RequestMapping("/{userId}")
	// public String t1(@PathVariable("userId") String userId){
	// System.out.println("t1 "+ userId);
	// return "myt";
	// }

	@RequestMapping(value = "/login")
	public String register(Model model) {
		System.out.println("注册");
		return "myt";
	}

	@RequestMapping("/pay")
	public ModelAndView centerpay(HttpServletRequest request) {
		PerformanceTimer timer = new PerformanceTimer();

		Map<String, Object> resultMap = Maps.newHashMap();

		Map<String, String> params = SignUtil.getParametersHashMap(request);

		// 签名验证
		// try{
		// SignUtil.verifySign(request, SystemConfig.webSecret);
		// }catch(Exception e){
		// resultMap.put("code", "-1");
		// if(e instanceof OssException){
		// OssException ee = (OssException)e;
		// resultMap.put("msg", ee.getMessage());
		// }
		// return new ModelAndView(new FastJsonJsonView(), resultMap);
		// }

		String queryType = params.get("queryType");
		int type;
		try {
			type = Integer.parseInt(queryType);
		} catch (NumberFormatException e) {
			resultMap.put("msg", "参数异常");
			resultMap.put("result", FAIL);
			return new ModelAndView(new FastJsonJsonView(), resultMap);
		}

		if (type == TYPE_USER) {
			getUser(params, resultMap);
		} else if (type == TYPE_UNIONID) {
			getUserByUnionID(params, resultMap);
		} else if (type == TYPE_ORDER) {
			getMyOrder(params, resultMap);
		} else if (type == TYPE_PROXY_LIST) {
			getProxyList(params, resultMap);
		} else if (type == TYPE_TURN_CARD) {
			SysParamModel sysParamModel1000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(1000);
			if (sysParamModel1000 == null || sysParamModel1000.getVal3() == 0) {
				logger.error("服务器维护状态,无法进行次操作==params=" + params);
				resultMap.put("msg", "服务器维护状态,无法进行次操作");
				resultMap.put("result", FAIL);
				return new ModelAndView(new FastJsonJsonView(), resultMap);
			}
			turnCard(params, resultMap);
		} else if (type == TYPE_GIVE_CARD) {
			SysParamModel sysParamModel1000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(1000);
			if (sysParamModel1000 == null || sysParamModel1000.getVal3() == 0) {
				logger.error("服务器维护状态,无法进行次操作==params=" + params);
				resultMap.put("msg", "服务器维护状态,无法进行次操作");
				resultMap.put("result", FAIL);
				return new ModelAndView(new FastJsonJsonView(), resultMap);
			}
			giveCard(params, resultMap);
		} else if (type == TYPE_SET_PROXY) {
			setProxy(params, resultMap);
		} else if (type == TYPE_REMARK_PROXY) {
			setProxyRemark(params, resultMap);
		} else if (type == TYPE_DELETE_PROXY) {
			deleteProxy(params, resultMap);
		} else if (type == TYPE_PAY) {
			pay(params, resultMap);
		} else if (type == TYPE_PAY_REPAIR) {
			repairOrder(params, resultMap);
		} else if (type == TYPE_OPEN_ROOM) {
			SysParamModel sysParamModel1000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(1000);
			if (sysParamModel1000 == null || sysParamModel1000.getVal3() == 0) {
				logger.error("服务器维护状态,无法进行次操作==params=" + params);
				resultMap.put("msg", "服务器维护状态,无法进行次操作");
				resultMap.put("result", FAIL);
				return new ModelAndView(new FastJsonJsonView(), resultMap);
			}
			openRoom(params, resultMap);
		} else if (type == TYPE_USER_DETAIL) {
			userDetail(params, resultMap);
		} else if (type == TYPE_PROXY_DETAIL) {
			userProxyConsumerDetail(params, resultMap);
		} else if (type == TYPE_RMB_DETAIL) {
			userRmbDetail(params, resultMap);
		} else if (type == TYPE_RECEIVE_RMB_DETAIL) {
			userReceiveDetail(params, resultMap);
		} else if (type == TYPE_ROOM_DAY_DETAIL_USER) {
			roomDetailUserID(params, resultMap);
		} else if (type == TYPE_ROOM_DAY_DETAIL_GROUP) {
			roomDetailGroup(params, resultMap);
		} else if (type == TYPE_ROOM_DETAIL_GROUP_REALTIME) {
			roomDetailGroupRealTime(params, resultMap);
		} else if (type == TYPE_ROOM_BANGDING_ID) {
			roomBandingUserID(params, resultMap);
		} else if (type == TYPE_ROOM_JIE_BANG) {
			roomJieBandUserID(params, resultMap);
		} else if (type == TYPE_ROOM_BAND_DETAIL) {
			roomBandDetail(params, resultMap);
		} else if (type == TYPE_ROOM_DELETE) {
			roomDelete(params, resultMap);
		} else if (type == TYPE_RECOMMEND_INCOME) {
			doRecommendIncome(params, resultMap);
		} else if (type == TYPE_RECOMMEND_OUT) {
			doRecommendOutcome(params, resultMap);
		} else if (type == TYPE_RECOMENT_INDEX) {// ---推广员---首页
			recomentIndex(params, resultMap);
		} else if (type == TYPE_MY_WITHDRAWS) {// ---推广员---提现
			myWithdraws(params, resultMap);
		} else if (type == TYPE_MY_MEMBERPLAYER) {// ---推广员---我的会员玩家
			myMemberPlayer(params, resultMap);
		} else if (type == TYPE_MY_MEMBER_PROXY) {// ---推广员---我的会员代理
			myMemberProxy(params, resultMap);
		} else if (type == TYPE_JUDGE_WITHDRAWS) {// ---推广员---判断能否提现，不做入库的操作
			judgeWithdraws(params, resultMap);
		} else if (type == TYPE_MY_NEXTLEVEL) {// ---推广员---我的下级
			queryDownPlayerByMonth(params, resultMap);
		} else if (type == TYPE_DALIY_RECORD) {// 日常记录
			doRecommendDailyDetail(params, resultMap);
		} else if (type == TYPE_RECOMMEND_ALL) {
			doRecommendAll(params, resultMap);
		} else if (type == TYPE_DOWN_RECOMMEND_DETAIL) {
			doDownRecommendDetail(params, resultMap);
		} else if (type == TYPE_INIT_ACTIVE) {
			initRecommendDetail();
		} else if (type == TYPE_DOWN_RECOMMEND) {
			setRecommendLevel(params, resultMap);
		} else if (type == TYPE_FLS_AGENTTJ_DAY) {
			getYesterdayFlsAgentTj(params, resultMap);
		} else if (type == TYPE_FLS_AGENTTJ_MONTH) {
			getFlsAgentTjByMonth(params, resultMap);
		} else if (type == TYPE_FLS_AGENTTJ_SEASON) {
			getFlsAgentTjBySeason(params, resultMap);
			// } else if (type == TYPE_CLUB_BIND_ACCOUNT) {
			// clubBindAccount(params, resultMap);
		} else if (type == TYPE_CLUB_BIND_GROUP) {
			clubBindGroup(params, resultMap);
			// } else if (type == TYPE_CLUB_CREATE) {
			// clubCreate(params, resultMap);
			// } else if (type == TYPE_CLUB_UPDATE) {
			// clubUpdate(params, resultMap);
			// } else if (type == TYPE_CLUB_DELETE) {
			// clubDelete(params, resultMap);
			// } else if (type == TYPE_CLUB_MY) {
			// clubGetMy(params, resultMap);
			// } else if (type == TYPE_CLUB_UNBIND_ACCOUNT) {
			// clubUnbindAccount(params, resultMap);
			// } else if (type == TYPE_CLUB_DETAIL) {
			// clubDetail(params, resultMap);
			// } else if (type == TYPE_CLUB_BIND_ACCOUNT_BATCH) {
			// clubBindAccountBatch(params, resultMap);
			// } else if (type == TYPE_CLUB_UNBIND_ACCOUNT_BATCH) {
			// clubUnbindAccountBatch(params, resultMap);
			// } else if (type == TYPE_CLUB_MEMBERS) {
			// getClubMemebers(params, resultMap);
			// } else if (type == TYPE_CLUB_SEARCH_ACCOUNT) {
			// clubSearchAccount(params, resultMap);
			// } else if (type == TYPE_CLUB_COST) {
			// clubCost(params, resultMap);
			// } else if (type == TYPE_CLUB_ROOM) {
			// getClubRoomLog(params, resultMap);
		} else if (type == TYPE_SYSTEM_STATE) {
			getSystemState(params, resultMap);
		} else if (type == TYPE_GOODS_STATE) {
			getGoodsState(params, resultMap);
		} else if (type == TYPE_ADD_GOLD) {
			addGoldByShop(params, resultMap);
		} else if (type == TYPE_ROBOT_UNION) {
			getRobotUser(params, resultMap);
		} else if (type == TYPE_ROBOT_RESULT) {
			getRobotResult(params, resultMap);
		} else if (type == TYPE_PH_PAY) {
			phPay(params, resultMap);
		} else if (type == INIT) {
			RecommenderReceiveService.getInstance().resetReceive();
		}
		return new ModelAndView(new FastJsonJsonView(), resultMap);
	}

	// private void getClubRoomLog(Map<String, String> params, Map<String,
	// Object> resultMap) {
	// if (StringUtils.isEmpty(params.get("clubId"))) {
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", "参数错误");
	// return;
	// }
	// int club_id = Integer.parseInt(params.get("clubId"));
	// int cur = Integer.parseInt(params.get("page"));
	// String startStr = params.get("start");
	// String endStr = params.get("end");
	// Date startDate = null;
	// Date endDate = null;
	//
	// // 日期兼容处理
	// if (StringUtils.isEmpty(startStr)) {
	// startDate = new Date();
	// endDate = startDate;
	// } else {
	// try {
	// startDate = DateUtils.parseDate(startStr, new String[] { "yyyyMMdd" });
	// } catch (ParseException e) {
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", "开始日期格式错误");
	// return;
	// }
	// if (StringUtils.isEmpty(endStr)) {
	// endDate = startDate;
	// } else {
	// try {
	// endDate = DateUtils.parseDate(endStr, new String[] { "yyyyMMdd" });
	// } catch (ParseException e) {
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", "结束日期格式错误");
	// return;
	// }
	//
	// }
	// }
	// int totalSize =
	// MongoDBServiceImpl.getInstance().getClublogModelCount(startDate, endDate,
	// club_id);
	// Page page = new Page(cur + 1, 10, totalSize);
	//
	// List<ClubLogModel> clubList =
	// MongoDBServiceImpl.getInstance().searchClubLogModel(page, startDate,
	// endDate, club_id);
	//
	// resultMap.put("result", SUCCESS);
	// resultMap.put("data", clubList);
	// resultMap.put("page", cur);
	// resultMap.put("totalSize", totalSize);
	// resultMap.put("maxPage", page.getTotalPage());
	// resultMap.put("msg", "获取成功");
	// }
	//
	// private void clubCost(Map<String, String> params, Map<String, Object>
	// resultMap) {
	// int accountId = Integer.parseInt(params.get("userID"));
	// String startStr = params.get("start");
	// String endStr = params.get("end");
	// int start = 0;
	// int end = 0;
	//
	// // 日期兼容处理
	// if (StringUtils.isEmpty(startStr)) {
	// start = Integer.parseInt(DateFormatUtils.format(new Date(), "yyyyMMdd"));
	// end = start;
	// } else {
	// start = Integer.parseInt(startStr);
	// if (StringUtils.isEmpty(endStr)) {
	// end = start;
	// } else {
	// end = Integer.parseInt(endStr);
	// }
	// }
	// List<EveryDayClubModel> clubList =
	// MongoDBServiceImpl.getInstance().searchDayClubModel(start, end, false,
	// accountId);
	// for (EveryDayClubModel everyDayClubModel : clubList) {
	// ClubService.getInstance().setClubName(everyDayClubModel);
	// }
	//
	// resultMap.put("result", SUCCESS);
	// resultMap.put("data", clubList);
	// resultMap.put("msg", "获取成功");
	// }
	//
	// private void clubSearchAccount(Map<String, String> params, Map<String,
	// Object> resultMap) {
	// int club_id = Integer.parseInt(params.get("clubId"));
	// long searchAccount = Integer.parseInt(params.get("accountId"));
	//
	// ClubModel model = ClubService.getInstance().getClubModel(club_id);
	// if (model == null) {
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", "找不到该俱乐部");
	// return;
	// }
	//
	// List<ClubMemberModel> members =
	// SpringService.getBean(PublicService.class).getPublicDAO().searchClubMember(club_id,
	// searchAccount);
	// resultMap.put("result", SUCCESS);
	// resultMap.put("data", members);
	// resultMap.put("msg", "获取成功");
	// }
	//
	// private void getClubMemebers(Map<String, String> params, Map<String,
	// Object> resultMap) {
	// int club_id = Integer.parseInt(params.get("clubId"));
	// int page = Integer.parseInt(params.get("page"));
	// long accountId = Integer.parseInt(params.get("userID"));
	// resultMap.put("userID", accountId);
	// int status = Integer.parseInt(params.get("status"));
	// float pageCount = 10.0f;
	//
	// Club club = ClubService.getInstance().getClub(club_id);
	// if (club == null) {
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", "找不到该俱乐部");
	// return;
	// }
	//
	// if (page < 0) {
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", "参数错误");
	// return;
	// }
	//
	// ClubMemberPageModel pageModel = new ClubMemberPageModel();
	// pageModel.setPage(page);
	// pageModel.setMaxPage(club.members.size() == 0 ? 0 : (int)
	// Math.ceil(club.members.size() / pageCount));
	// pageModel.setMaxCount(club.members.size());
	// pageModel.setClubId(club_id);
	// pageModel.setStatus(status);
	//
	// if (status != 1) {
	// // 未审核的人不计算在clubModel里，所以这里得另外算
	// int memberCount = 0;
	// if (status != 0) {
	// memberCount =
	// SpringService.getBean(PublicService.class).getPublicDAO().getClubMemberCount(club_id);
	// } else {
	// memberCount =
	// SpringService.getBean(PublicService.class).getPublicDAO().getClubMemberCountByStatus(club_id,
	// status);
	// }
	// pageModel.setMaxPage((int) Math.ceil(memberCount / pageCount));
	// pageModel.setMaxCount(memberCount);
	// }
	//
	// if (pageModel.getMaxPage() >= page) {
	// // 是否过滤审核不审核
	// if (status == 1 || status == 0) {
	// List<ClubMemberModel> members =
	// SpringService.getBean(PublicService.class).getPublicDAO()
	// .getClubMemberByPageAndStatus((int) (page * pageCount), (int) pageCount,
	// club_id, status);
	// pageModel.setMembers(members);
	// } else {
	//
	// List<ClubMemberModel> members =
	// SpringService.getBean(PublicService.class).getPublicDAO()
	// .getClubMemberByPage((int) (page * pageCount), (int) pageCount, club_id);
	// pageModel.setMembers(members);
	//
	// }
	// } else {
	// pageModel.setMembers(new ArrayList<>());
	// }
	// for (ClubMemberModel member : pageModel.getMembers()) {
	// member.setClubName(club.clubModel.getClub_name());
	// }
	// resultMap.put("result", SUCCESS);
	//
	// resultMap.put("data", pageModel);
	// resultMap.put("msg", "获取成功");
	//
	// }
	//
	// private void clubUnbindAccountBatch(Map<String, String> params,
	// Map<String, Object> resultMap) {
	// int club_id = Integer.parseInt(params.get("clubId"));
	// String accountIds = params.get("accountIds");
	// int status = ClubService.getInstance().outClub(club_id, accountIds);
	//
	// switch (status) {
	// case -1:
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", "找不到该俱乐部");
	// return;
	// case -2:
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", "管理员不能解绑");
	// return;
	// case 1:
	// resultMap.put("result", SUCCESS);
	// resultMap.put("msg", "解绑成功");
	// return;
	// }
	// }
	//
	// @Deprecated
	// private void clubBindAccountBatch(Map<String, String> params, Map<String,
	// Object> resultMap) {
	//// int club_id = Integer.parseInt(params.get("clubId"));
	//// String bindAccountID = params.get("accountIds");
	//// int status =
	// ClubService.getInstance().agreeJoinClub(bindAccountID,club_id);
	////
	//// switch (status) {
	//// case -1:
	//// resultMap.put("result", FAIL);
	//// resultMap.put("msg", "找不到该俱乐部");
	//// return;
	//// case 1:
	//// resultMap.put("result", SUCCESS);
	//// resultMap.put("msg", "绑定成功");
	//// return;
	//// }
	// }
	//
	// private void clubDetail(Map<String, String> params, Map<String, Object>
	// resultMap) {
	//
	// int club_id = Integer.parseInt(params.get("clubId"));
	// ClubModel model = ClubService.getInstance().getClubModel(club_id);
	//
	// if (model == null) {
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", "找不到该俱乐部");
	// return;
	// } else {
	// resultMap.put("result", SUCCESS);
	// resultMap.put("data", model);
	// resultMap.put("msg", "获取成功");
	// }
	// }
	//
	// private void clubUnbindAccount(Map<String, String> params, Map<String,
	// Object> resultMap) {
	// int club_id = Integer.parseInt(params.get("clubId"));
	// long account_ID = Integer.parseInt(params.get("userID"));
	// int bindAccountID = Integer.parseInt(params.get("accountId"));
	// int status = ClubService.getInstance().outClub(club_id, bindAccountID);
	//
	// switch (status) {
	// case -1:
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", "找不到该俱乐部");
	// return;
	// case -2:
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", "管理员不能退出");
	// return;
	// case -3:
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", "找不到该成员");
	// return;
	// case 1:
	// resultMap.put("result", SUCCESS);
	// resultMap.put("msg", "绑定成功");
	// return;
	// }
	// }
	//
	// private void clubGetMy(Map<String, String> params, Map<String, Object>
	// resultMap) {
	// int account_ID = Integer.parseInt(params.get("userID"));
	// List<ClubModel> list = ClubService.getInstance().getMyClub(account_ID);
	// resultMap.put("result", SUCCESS);
	// resultMap.put("data", list);
	// resultMap.put("userID", account_ID);
	// resultMap.put("msg", "获取成功");
	// }
	//
	// private void clubDelete(Map<String, String> params, Map<String, Object>
	// resultMap) {
	// long account_ID = Integer.parseInt(params.get("userID"));
	// int clubId = Integer.parseInt(params.get("clubId"));
	//
	// boolean status = ClubService.getInstance().deleteClub(clubId,
	// account_ID);
	// if (status) {
	// resultMap.put("result", SUCCESS);
	// resultMap.put("msg", "删除成功");
	// return;
	// } else {
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", "找不到该俱乐部");
	// }
	// }
	//
	// private void clubUpdate(Map<String, String> params, Map<String, Object>
	// resultMap) {
	// long account_ID = Integer.parseInt(params.get("userID"));
	//
	// String clubName = params.get("clubName");
	// String clubDesc = params.get("clubDesc");
	// int clubId = Integer.parseInt(params.get("clubId"));
	//
	// ClubRuleModel clubRoleModel = null;
	// if (!StringUtils.isEmpty(params.get("game_type_index"))) {
	// int game_type_index = Integer.parseInt(params.get("game_type_index"));
	// int game_rule_index = Integer.parseInt(params.get("game_rule_index"));
	// int game_round = Integer.parseInt(params.get("game_round"));
	// int baseScore = Integer.parseInt(params.get("baseScore"));
	// int maxTimes = Integer.parseInt(params.get("maxTimes"));
	// int base_score_gang = Integer.parseInt(params.get("base_score_gang"));
	// int base_score_ci = Integer.parseInt(params.get("base_score_ci"));
	// String game_rule_index_ex = params.get("game_rule_index_ex");
	//
	// SysGameType gameType =
	// SysGameTypeDict.getInstance().getSysGameType(game_type_index);
	// if (gameType == null) {
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", "找不到该游戏");
	// return;
	// }
	//
	// SysParamModel findParam = null;
	// for (int index : gameType.getGoldIndex()) {
	// SysParamModel tempParam =
	// SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameType.getGameID()).get(index);//
	// 扑克类30
	// if (tempParam == null) {
	// logger.error("不存在的参数game_id" +
	// SysGameTypeDict.getInstance().getGameIDByTypeIndex(game_type_index) +
	// "index=" + index);
	// continue;
	// }
	// if (tempParam.getVal1() == game_round) {
	// findParam = tempParam;
	// break;
	// }
	// }
	//
	// if (findParam == null) {
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", "局数配置有误");
	// return;
	// }
	//
	// clubRoleModel = new ClubRuleModel();
	// clubRoleModel.setBase_score(baseScore);
	// clubRoleModel.setMax_times(maxTimes);
	// clubRoleModel.setGame_round(game_round);
	// clubRoleModel.setBase_score_ci(base_score_ci);
	// clubRoleModel.setGame_rule_index(game_rule_index);
	// clubRoleModel.setGame_type_index(game_type_index);
	// clubRoleModel.setBase_score_gang(base_score_gang);
	// clubRoleModel.setGame_rule_index_ex(game_rule_index_ex);
	// }
	//
	// int status = ClubService.getInstance().updateClub(clubId, clubRoleModel,
	// account_ID, clubDesc, clubName);
	// switch (status) {
	// case -1:
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", "找不到该俱乐部");
	// return;
	// case -2:
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", "该俱乐部不是你的");
	// return;
	// case -3:
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", "描述长度不对");
	// return;
	// case -4:
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", "名字长度不对");
	// return;
	// case 1:
	// resultMap.put("result", SUCCESS);
	// resultMap.put("msg", "修改成功");
	// return;
	// }
	// }
	//
	// private void clubCreate(Map<String, String> params, Map<String, Object>
	// resultMap) {
	// long account_ID = Integer.parseInt(params.get("userID"));
	// int game_type_index = Integer.parseInt(params.get("game_type_index"));
	// int game_rule_index = Integer.parseInt(params.get("game_rule_index"));
	// int game_round = Integer.parseInt(params.get("game_round"));
	// int baseScore = Integer.parseInt(params.get("baseScore"));
	// int maxTimes = Integer.parseInt(params.get("maxTimes"));
	// int base_score_gang = Integer.parseInt(params.get("base_score_gang"));
	// int base_score_ci = Integer.parseInt(params.get("base_score_ci"));
	// String game_rule_index_ex = params.get("game_rule_index_ex");
	// String clubName = params.get("clubName");
	// String clubDesc = params.get("clubDesc");
	//
	// SysGameType gameType =
	// SysGameTypeDict.getInstance().getSysGameType(game_type_index);
	// if (gameType == null) {
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", "找不到该游戏");
	// return;
	// }
	//
	// SysParamModel findParam = null;
	// for (int index : gameType.getGoldIndex()) {
	// SysParamModel tempParam =
	// SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameType.getGameID()).get(index);//
	// 扑克类30
	// if (tempParam == null) {
	// logger.error("不存在的参数game_id" +
	// SysGameTypeDict.getInstance().getGameIDByTypeIndex(game_type_index) +
	// "index=" + index);
	// continue;
	// }
	// if (tempParam.getVal1() == game_round) {
	// findParam = tempParam;
	// break;
	// }
	// }
	//
	// if (findParam == null) {
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", "局数配置有误");
	// return;
	// }
	//
	// if (StringUtils.isEmpty(clubName) || Utils.getLength(clubName) > 16) {
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", "名字长度不对");
	// return;
	// }
	//
	// if (StringUtils.isEmpty(clubDesc) || Utils.getLength(clubDesc) > 72) {
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", "描述长度不对");
	// return;
	// }
	//
	// Account account = PublicServiceImpl.getInstance().getAccount(account_ID);
	//
	// if (account == null || account.getAccountModel().getIs_agent() != 1) {
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", "找不到该代理");
	// return;
	// }
	//
	// ClubModel clubModel = new ClubModel();
	// clubModel.setClub_name(clubName);
	// clubModel.setDesc(clubDesc);
	// clubModel.setAvatar(account.getAccountWeixinModel().getHeadimgurl() ==
	// null ? "" : account.getAccountWeixinModel().getHeadimgurl());
	// clubModel.setAccount_id(account_ID);
	//
	// ClubRuleModel clubRoleModel = new ClubRuleModel();
	// clubRoleModel.setBase_score(baseScore);
	// clubRoleModel.setMax_times(maxTimes);
	// clubRoleModel.setGame_round(game_round);
	// clubRoleModel.setBase_score_ci(base_score_ci);
	// clubRoleModel.setGame_rule_index(game_rule_index);
	// clubRoleModel.setGame_type_index(game_type_index);
	// clubRoleModel.setBase_score_gang(base_score_gang);
	// clubRoleModel.setGame_rule_index_ex(game_rule_index_ex);
	// clubRoleModel.decodeRule();
	// // clubRoleModel.getGame_rules()TODO
	//
	// List<ClubRuleModel> rules = new ArrayList<>();
	// rules.add(clubRoleModel);
	// clubModel.setRules(rules);
	// int status = ClubService.getInstance().createClub(account_ID, clubModel);
	// if(status == -1){
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", "创建失败，生成id失败");
	// return;
	// }
	// resultMap.put("result", SUCCESS);
	// resultMap.put("msg", "创建成功");
	// }
	//
	// private void clubBindAccount(Map<String, String> params, Map<String,
	// Object> resultMap) {
	// String group_id = params.get("groupID");
	// int club_id = Integer.parseInt(params.get("clubId"));
	// String nickName = params.get("nickName");
	// long account_ID = Integer.parseInt(params.get("userID"));
	// int status = ClubService.getInstance().bindClubAccount(club_id,
	// account_ID, nickName);
	//
	// switch (status) {
	// case -1:
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", "找不到该俱乐部");
	// return;
	// case -2:
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", "该用户已绑定该俱乐部");
	// return;
	// case -3:
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", ErrorCode.ROLE_FIND_FAIL);
	// return;
	// case -4:
	// resultMap.put("result", FAIL);
	// resultMap.put("msg", "用户昵称和游戏昵称对应不上");
	// return;
	// case 1:
	// resultMap.put("result", SUCCESS);
	// resultMap.put("msg", "绑定成功");
	// return;
	// }
	// }
	//
	private void clubBindGroup(Map<String, String> params, Map<String, Object> resultMap) {
		String group_id = params.get("groupID");
		int club_id = Integer.parseInt(params.get("clubId"));

		ClubGroupModel clubGroup = SpringService.getBean(PublicService.class).getPublicDAO().getClubGroup(group_id);
		if (clubGroup != null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "该群已被绑定");
			return;
		}

		if (SpringService.getBean(PublicService.class).getPublicDAO().getClub(club_id) == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "找不到该亲友圈");
			return;
		}

		if (StringUtils.isEmpty(group_id)) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数错误");
			return;
		}

		clubGroup = new ClubGroupModel();
		clubGroup.setGroup_id(group_id);
		clubGroup.setClub_id(club_id);

		SpringService.getBean(PublicService.class).getPublicDAO().insertClubGroup(clubGroup);

		resultMap.put("result", SUCCESS);
		resultMap.put("msg", "绑定成功");
	}

	/**
	 * 获取用户信息
	 * 
	 * @param request
	 * @param params
	 */
	private void openRoom(Map<String, String> params, Map<String, Object> resultMap) {
		PerformanceTimer times = new PerformanceTimer();

		String account_ID = params.get("userID");
		String groupID = params.get("groupID");
		String groupName = params.get("groupName");
		if (org.apache.commons.lang.StringUtils.isEmpty(account_ID) || org.apache.commons.lang.StringUtils.isEmpty(groupID)) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", ErrorCode.PARAMETER_EMPTY);
			return;
		}

		// 麻将类型
		int game_type_index;
		// 玩法
		int game_rule_index;
		/// 局数
		int game_round;

		long accountID;

		int isInner = 0;

		boolean isMore = false;
		// 1余下规则，2番数，3底分，4杠底，5次底
		int exRule = 0;
		int fanshu = 0;
		int baseScore = 0;
		int ciScore = 0;
		int gangScore = 0;
		int wcTimes = 0;
		String ruleParams = params.get("ruleParams");
		Map<Integer, Integer> ruleMap = new HashMap<Integer, Integer>();
		try {
			game_type_index = Integer.parseInt(params.get("game_type_index"));
			game_rule_index = Integer.parseInt(params.get("game_rule_index"));
			game_round = Integer.parseInt(params.get("game_round"));
			accountID = Long.parseLong(account_ID);
			String inner = params.get("isInner");
			if (inner != null) {
				isInner = Integer.parseInt(inner);
			}
			String ismore = params.get("isMore");
			if (ismore != null) {
				isMore = Integer.parseInt(ismore) == 1 ? true : false;
			}
			if (StringUtils.isNotBlank(params.get("v0"))) {
				exRule = Integer.parseInt(params.get("v0"));
			}
			if (StringUtils.isNotBlank(params.get("v1"))) {
				fanshu = Integer.parseInt(params.get("v1"));
			}
			if (StringUtils.isNotBlank(params.get("v2"))) {
				baseScore = Integer.parseInt(params.get("v2"));
			}
			if (StringUtils.isNotBlank(params.get("v3"))) {
				gangScore = Integer.parseInt(params.get("v3"));
			}
			if (StringUtils.isNotBlank(params.get("v4"))) {
				ciScore = Integer.parseInt(params.get("v4"));
			}
			if (StringUtils.isNotBlank(params.get("v5"))) {
				wcTimes = Integer.parseInt(params.get("v5"));
			}
			if (StringUtils.isNotBlank(ruleParams)) {
				try {
					ruleMap = JSON.parseObject(ruleParams, HashMap.class);
				} catch (Exception e) {
					logger.error("ruleParams turn error ", e);
				}
			}
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}

		List<RobotRoom> roomList = RedisServiceImpl.getInstance().getListRobotRoom(groupID);
		if (roomList == null) {
			roomList = new ArrayList<RobotRoom>();
			RedisServiceImpl.getInstance().putListRobotRoom(groupID, roomList);
		}

		Account account = PublicServiceImpl.getInstance().getAccount(accountID);
		if (account == null || account.getAccountWeixinModel() == null) {
			resultMap.put("result", SUCCESS);
			resultMap.put("msg", ErrorCode.ROLE_FIND_FAIL);
			return;
		}

		SysParamModel sysParamModel1000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(1000);
		if (sysParamModel1000.getVal3() == 0 && account.getAccountModel().getIs_inner() == 0) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "服务器停服维护中");
			return;
		}

		int game_id = account.getGame_id();

		if (game_id == 0 || game_id == 6 || game_id == 7) {
			game_id = SysGameTypeDict.getInstance().getGameIDByTypeIndex(game_type_index);
		}

		RobotRoom createRoom = null;
		long now = System.currentTimeMillis();
		int max_persion = 0;
		int cur_persion = 0;
		int max = 2;
		SysParamModel sysParamModel5005 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(5005);
		if (sysParamModel5005.getVal2() != 0) {
			max = sysParamModel5005.getVal2();
		}
		if (ruleMap.size() > 0) {
			DescParams descParams = DescParams.create(game_type_index, ruleMap);
			game_rule_index = descParams._game_rule_index;
		}
		synchronized (roomList) {
			int num = 0;
			if (roomList.size() > 0) {
				Iterator<RobotRoom> it = roomList.iterator();
				while (it.hasNext()) {
					RobotRoom robotroom = it.next();
					if (now - robotroom.createTime >= 60 * 1000 * GameConstants.CREATE_ROOM_PROXY_TIME_GAP) {
						it.remove();
						continue;
					}
					if (game_rule_index != robotroom.game_rule_index || game_type_index != robotroom.game_type_index) {
						// it.remove();
						continue;
					}

					RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM, robotroom.roomID + "",
							RoomRedisModel.class);
					if (roomRedisModel == null) {
						it.remove();
						continue;
					}
					if (GameConstants.GS_MJ_FREE != roomRedisModel.getRoomStatus() && GameConstants.GS_MJ_WAIT != roomRedisModel.getRoomStatus()) {
						it.remove();
						continue;
					}
					int maxNumber = roomRedisModel.getPlayer_max();
					if (roomRedisModel.getCur_player_num() >= maxNumber) {
						it.remove();
						continue;
					}

					robotroom.nickNames.clear();
					Set<Long> playerIds = roomRedisModel.getPlayersIdSet();
					for (Long playerId : playerIds) {
						Account taraccount = PublicServiceImpl.getInstance().getAccount(playerId);
						if (taraccount.getAccountWeixinModel() != null) {
							robotroom.nickNames.add(taraccount.getAccountWeixinModel().getNickname());
						} else {
							robotroom.nickNames.add("--");
						}
					}
					createRoom = robotroom;
					max_persion = roomRedisModel.getPlayer_max();
					cur_persion = roomRedisModel.getCur_player_num();

					if (isMore) {
						num++;
					} else {
						break;
					}
				}
			}

			if (num <= max && isMore) {
				createRoom = null;
			}

		}

		if (createRoom != null) {
			SysParamModel sysParamModel2000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(2000);
			if (sysParamModel2000 == null) {
				sysParamModel2000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2000);
			}
			String content = sysParamModel2000.getStr2() + "?game=" + game_id + "&roomid=" + createRoom.roomID;
			resultMap.put("result", SUCCESS);
			resultMap.put("room", createRoom);
			resultMap.put("content", content);
			if (max_persion > 0)
				resultMap.put("memberDesc", getMemberDesc(max_persion, cur_persion));// 几缺几

			if (!createRoom.gameDesc.isEmpty()) {
				resultMap.put("gamedesc", createRoom.gameDesc);
			}

			return;
		}

		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		// 开放判断
		SysParamModel sysParamModel = null;
		try {
			sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id)
					.get(SysGameTypeDict.getInstance().getGameGoldTypeIndex(game_type_index));
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "即将开放,敬请期待!");
			logger.error("SysParamModel 获取失败" + game_type_index);
			return;
		}

		if (sysParamModel != null && sysParamModel.getVal1() != 1) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "即将开放,敬请期待!");
			logger.error("sysParamModel.getVal1() != 1" + game_id + "game_type_index=" + game_type_index);
			return;
		}

		// 判断房卡是否免费
		if (sysParamModel != null && sysParamModel.getVal2() == 1) {
			int[] roundGoldArray = SysGameTypeDict.getInstance().getGoldIndexByTypeIndex(game_type_index);
			if (roundGoldArray == null) {
				resultMap.put("result", FAIL);
				resultMap.put("msg", "即将开放,敬请期待!");
				logger.error("roundGoldArray IS null" + game_id + "game_type_index=" + game_type_index);
				return;
			}

			SysParamModel findParam = null;
			for (int index : roundGoldArray) {
				SysParamModel tempParam = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(index);
				if (tempParam == null) {
					logger.error("不存在的参数game_id" + game_id + "index=" + index);
					continue;
				}
				if (tempParam.getVal1() == game_round) {
					findParam = tempParam;
					break;
				}
			}

			if (findParam == null) {
				logger.error("findParam IS null" + game_id + "game_type_index=" + game_type_index);
				resultMap.put("result", FAIL);
				resultMap.put("msg", "即将开放,敬请期待!");
				return;
			}

			do {
				// 如果开放可以用
				if (ExclusiveGoldCfg.get().isRobotCreateRoomCostExclusiveGold()) {
					// 1 专属豆满足
					if (ClubExclusiveService.getInstance().check(accountID, game_id, findParam.getVal2())) {
						break;
					}
				}
				long gold = account.getAccountModel().getGold();
				if (gold < findParam.getVal2()) {
					resultMap.put("result", FAIL);
					resultMap.put("msg", SysParamServerDict.getInstance().replaceGoldTipsWord("闲逸豆不足,请及时补充闲逸豆!"));
					return;
				}
			} while (false);

		}

		// TODO 创建房间号,写入redis 记录是哪个逻辑计算服的
		int logicSvrId = centerRMIServer.allotLogicId(game_id);
		if (logicSvrId <= 0) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "服务器正在维护，请等待服务器停机维护完成再登录");
			return;
		}

		int room_id = centerRMIServer.randomRoomId(1, logicSvrId);// 随机房间号
		if (room_id == -1) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "创建房间失败!");
			return;
		}
		if (room_id == -2) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "服务器进入停服倒计时,不能创建房间,请等待服务器停机维护完成再登录");
			return;
		}

		long create_time = System.currentTimeMillis();
		// redis房间记录 代理开房间，人不需要进去
		RoomRedisModel roomRedisModel = new RoomRedisModel();
		roomRedisModel.setRoom_id(room_id);
		roomRedisModel.setLogic_index(logicSvrId);
		// roomRedisModel.getPlayersIdSet().add(session.getAccountID());
		roomRedisModel.setCreate_time(create_time);
		roomRedisModel.setGame_round(game_round);
		roomRedisModel.setGame_rule_index(game_rule_index);
		roomRedisModel.setGame_type_index(game_type_index);
		roomRedisModel.setGame_id(game_id);
		roomRedisModel.setProxy_room(true);
		roomRedisModel.setCreate_account_id(account.getAccount_id());

		// 通知逻辑服 TODO 这里写死了
		ILogicRMIServer logicRmiServer = RMIServiceImpl.getInstance().getLogicRMIByIndex(logicSvrId);
		String nickName = account.getAccountWeixinModel() == null ? "" : account.getAccountWeixinModel().getNickname();
		boolean success = false;
		if (ruleMap != null && ruleMap.size() > 0) {
			success = logicRmiServer.createRoomByBobotExtend(accountID, room_id, game_type_index, game_round, nickName, groupID, groupName, isInner,
					ruleMap);
		} else {
			if (exRule > 0 || fanshu > 0 || baseScore > 0 || ciScore > 0 || gangScore > 0 || wcTimes > 0) {
				success = logicRmiServer.createRoomByBobotExtend(accountID, room_id, game_type_index, game_rule_index, game_round, nickName, groupID,
						groupName, isInner, exRule, fanshu, baseScore, gangScore, ciScore, wcTimes);
			} else {
				success = logicRmiServer.createRobotRoom(accountID, room_id, game_type_index, game_rule_index, game_round, nickName, groupID,
						groupName, isInner);
			}
		}

		if (!success) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "开房失败,请稍后再试");
			return;
		}

		String desc = "";

		DescParams descParams = GameDescUtil.params.get();
		if (ruleMap.size() > 0) {
			descParams = DescParams.create(game_type_index, ruleMap);
			descParams.setRuleMap(ruleMap);

		} else {
			descParams.setGameRule(game_rule_index).setGameType(game_type_index);
			// 这里第0位不用
			int[] rules = { game_rule_index, exRule };
			descParams.put(GameConstants.GAME_RULE_BASE_SCORE, baseScore);
			descParams.put(GameConstants.GAME_RULE_MAX_TIMES, fanshu);
			descParams.put(GameConstants.GAME_RULE_BASE_SCORE_CI, ciScore);
			descParams.put(GameConstants.GAME_RULE_BASE_SCORE_GANG, gangScore);
			if (wcTimes > 0) {
				descParams.put(7, wcTimes);
			}
			descParams.game_rules = rules;
		}
		RmiDTO rmiDTO = logicRmiServer.getGameDescAndPeopleNumber(descParams);
		desc = rmiDTO.getDesc();
		// desc = logicRmiServer.getGameDesc(descParams);
		roomRedisModel.setGameRuleDes(desc);
		roomRedisModel.setRoomStatus(GameConstants.GS_MJ_FREE);
		roomRedisModel.setPlayer_max(rmiDTO.getValue());
		roomRedisModel.setGroupID(groupID);
		roomRedisModel.setIsInner(isInner);

		SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM, room_id + "", roomRedisModel);

		createRoom = new RobotRoom(System.currentTimeMillis(), room_id, accountID, game_type_index, game_rule_index, game_round);
		createRoom.gameDesc = desc;
		roomList.add(createRoom);
		SysParamModel sysParamModel2000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(2000);
		if (sysParamModel2000 == null) {
			sysParamModel2000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2000);
		}
		String content = sysParamModel2000.getStr2() + "?game=" + game_id + "&roomid=" + room_id;
		resultMap.put("room", createRoom);
		resultMap.put("result", SUCCESS);
		resultMap.put("content", content);
		resultMap.put("memberDesc", getMemberDesc(roomRedisModel.getPlayer_max(), roomRedisModel.getCur_player_num()));// 几缺几
		resultMap.put("gamedesc", desc);
		resultMap.put("gold", account.getAccountModel().getGold());

		logger.warn("open room param:" + descParams.toString());
		if (times.get() > 50) {
			logger.error("open room cost" + times.duration());
		}
	}

	public String getMemberDesc(int max_persion, int cur_persion) {
		StringBuffer sb = new StringBuffer();
		sb.append(int2ChineseNum(cur_persion)).append("缺").append(int2ChineseNum(max_persion - cur_persion));
		return sb.toString();
	}

	public String int2ChineseNum(int num) {
		String s = null;
		switch (num) {
		case 1:
			s = "一";
			break;
		case 2:
			s = "二";
			break;
		case 3:
			s = "三";
			break;
		case 4:
			s = "四";
			break;
		case 5:
			s = "五";
			break;
		case 6:
			s = "六";
			break;
		case 7:
			s = "七";
			break;
		case 8:
			s = "八";
			break;
		case 9:
			s = "九";
			break;
		default:
			s = "零";
			break;
		}
		return s;
	}

	/**
	 * 提现明细
	 * 
	 * @param request
	 * @param params
	 */
	private void userReceiveDetail(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		int curPage;
		long userID;
		try {
			curPage = Integer.parseInt(params.get("cur_page"));
			userID = Long.parseLong(user_ID);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		Account account = PublicServiceImpl.getInstance().getAccount(userID);
		if (account == null || account.getAccountWeixinModel() == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", ErrorCode.ROLE_FIND_FAIL);
			return;
		}

		int totalSize = MongoDBServiceImpl.getInstance().getRevicerRmbLogModelCount(userID);
		Page page = new Page(curPage, 10, totalSize);
		List<RevicerRmbLogModel> revicerRMbModelList = MongoDBServiceImpl.getInstance().getRevicerRmbLogModelList(page, userID);

		resultMap.put("result", SUCCESS);
		resultMap.put("curPage", curPage);
		resultMap.put("pageSize", 10);
		resultMap.put("totalSize", totalSize);
		resultMap.put("revicerRMbModelList", revicerRMbModelList);

	}

	/**
	 * 根据用户查询收益明细
	 * 
	 * @param request
	 * @param params
	 */
	private void userRmbDetail(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		int curPage;
		long userID;
		try {
			curPage = Integer.parseInt(params.get("cur_page"));
			userID = Long.parseLong(user_ID);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		Account account = PublicServiceImpl.getInstance().getAccount(userID);
		if (account == null || account.getAccountWeixinModel() == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", ErrorCode.ROLE_FIND_FAIL);
			return;
		}

		resultMap.put("userId", account.getAccountWeixinModel().getAccount_id());
		resultMap.put("result", SUCCESS);
		resultMap.put("totalRMB", account.getAccountModel().getHistory_rmb());// 总收益
		resultMap.put("receiveRMB", account.getAccountModel().getReceive_rmb());// 已提现
		resultMap.put("canRMB", account.getAccountModel().getRmb());// 可提现

		int totalSize = MongoDBServiceImpl.getInstance().getOneProxyAccountReplaceRoomModelCount(userID);
		Page page = new Page(curPage, 10, totalSize);
		List<OneProxyAccountReplaceRoomModel> oneProxyAccountReplaceRoomModelList = MongoDBServiceImpl.getInstance()
				.getOneProxyAccountReplaceRoomModelList(page, userID);

		resultMap.put("result", SUCCESS);
		resultMap.put("curPage", curPage);
		resultMap.put("pageSize", 10);
		resultMap.put("totalSize", totalSize);
		resultMap.put("oneproxyList", oneProxyAccountReplaceRoomModelList);

	}

	private void roomDelete(Map<String, String> params, Map<String, Object> resultMap) {
		String roomId = params.get("roomId");
		String accountId = params.get("accountId");
		if (StringUtils.isBlank(roomId) || StringUtils.isBlank(accountId)) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数有误");
			return;
		}
		long account_id = Long.parseLong(accountId);
		int room_id = Integer.parseInt(roomId);
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		RoomRedisModel room = centerRMIServer.getRoomById(room_id);
		if (room == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "房间已经解散");
			return;
		}
		if (room.getCreate_account_id() != account_id) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "操作失败,只有创建者才能解散房间");
			return;
		}
		centerRMIServer.delRoomById(room_id);
		resultMap.put("result", SUCCESS);
		resultMap.put("msg", "房间" + room_id + "解散成功");
	}

	private void roomBandDetail(Map<String, String> params, Map<String, Object> resultMap) {

		String groupID = params.get("groupID");
		if (groupID.isEmpty()) {
			return;
		}
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<AccountGroupModel> accountGroupModels = publicService.getPublicDAO().getAccountGroupModelListByGroupId(groupID);
		for (AccountGroupModel accountGroup : accountGroupModels) {
			AccountSimple accountsimple = PublicServiceImpl.getInstance().getAccountSimpe(accountGroup.getAccount_id());
			accountGroup.setTarget_icon(accountsimple.getIcon());
			accountGroup.setTarget_name(accountsimple.getNick_name());
		}
		resultMap.put("data", accountGroupModels);
		resultMap.put("result", SUCCESS);

	}

	private void roomJieBandUserID(Map<String, String> params, Map<String, Object> resultMap) {

		String groupID = "";
		long userID;
		try {
			groupID = params.get("groupID");
			userID = Long.parseLong(params.get("userID"));
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

		PublicService publicService = SpringService.getBean(PublicService.class);
		AccountGroupModel accountGroupModel = account.getAccountGroupModelMap().remove(groupID);
		if (accountGroupModel == null) {
			resultMap.put("result", SUCCESS);
			return;
		}

		publicService.getPublicDAO().deleteAccountGroupModel(accountGroupModel);

		RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();

		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
		rsAccountResponseBuilder.setAccountId(userID);
		rsAccountResponseBuilder.setGroupID(groupID);
		rsAccountResponseBuilder.setDeleteGroupID(true);
		redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
				ERedisTopicType.topicProxy.getId() + account.getLastProxyIndex());
		IClubRMIServer iClubRMIServer = (IClubRMIServer) SpringService.getBean("clubRMIServer");
		HashMap<String, String> paramsMap = new HashMap<String, String>();
		paramsMap.put("accountId", userID + "");
		paramsMap.put("groupId", groupID);
		try {
			iClubRMIServer.rmiInvoke(RMICmd.GROUP_TO_CLUB, paramsMap);
		} catch (Exception e) {
			logger.error("微信群踢人同步俱乐部踢人异常", e);
		}
		resultMap.put("result", SUCCESS);

	}

	private void roomBandingUserID(Map<String, String> params, Map<String, Object> resultMap) {

		String groupID = "";
		long userID;
		try {
			groupID = params.get("groupID");
			userID = Long.parseLong(params.get("userID"));
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

		AccountGroupModel accountGroup = account.getAccountGroupModelMap().get(groupID);
		if (accountGroup != null) {
			return;
		}

		PublicService publicService = SpringService.getBean(PublicService.class);
		AccountGroupModel accountGroupModel = new AccountGroupModel();
		accountGroupModel.setAccount_id(userID);
		accountGroupModel.setDate(new Date());
		accountGroupModel.setGroupId(groupID);
		account.getAccountGroupModelMap().put(groupID, accountGroupModel);

		publicService.getPublicDAO().insertAccountGroupModel(accountGroupModel);

		RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();

		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
		rsAccountResponseBuilder.setAccountId(userID);
		rsAccountResponseBuilder.setGroupID(groupID);
		redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
				ERedisTopicType.topicProxy.getId() + account.getLastProxyIndex());
		resultMap.put("result", SUCCESS);

	}

	private void roomDetailUserID(Map<String, String> params, Map<String, Object> resultMap) {

		int targetDateInt = 0;
		long userID;
		String startDate;
		String endDate;
		try {
			String targetDateStr = params.get("targetDateInt");
			if (targetDateStr != null) {
				targetDateInt = Integer.parseInt(targetDateStr);
			}
			startDate = params.get("startDate");
			endDate = params.get("endDate");
			userID = Long.parseLong(params.get("userID"));
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

		Date now = new Date();

		Aggregation aggregation = Aggregation.newAggregation(
				Aggregation.match(Criteria.where("account_id").is(userID).and("create_time").gte(MyDateUtil.getZeroDate(now))
						.lte(MyDateUtil.getTomorrowZeroDate(now))),
				Aggregation.group().sum("v1").as("goldtotal").sum("v2").as("brandtotal").last("account_id").as("account_id"));

		AggregationResults<HashMap> models = SpringService.getBean(MongoDBService.class).getMongoTemplate().aggregate(aggregation, "robot_log",
				HashMap.class);
		HashMap mm = models.getUniqueMappedResult();
		if (mm != null) {
			resultMap.put("goldtotal", mm.get("goldtotal") == null ? 0 : mm.get("goldtotal"));
			resultMap.put("brandtotal", mm.get("brandtotal") == null ? 0 : mm.get("brandtotal"));
		} else {
			resultMap.put("goldtotal", 0);
			resultMap.put("brandtotal", 0);
		}

		Criteria crieria = new Criteria();
		Query query = Query.query(crieria);
		crieria.and("account_id").is(userID);

		if (targetDateInt != 0) {
			crieria.and("notes_date").is(targetDateInt);
		} else {
			if (StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)) {
				crieria.and("notes_date").gte(Integer.parseInt(startDate)).lte(Integer.parseInt(endDate));
			} else {
				int start = Integer.parseInt(DateFormatUtils.format(DateUtils.addDays(new Date(), -15), "yyyyMMdd"));
				crieria.and("notes_date").gte(start);
			}
		}
		int nowDateInt = Integer.valueOf(DateFormatUtils.format(new Date(), "yyyyMMdd"));
		List<EveryDayRobotModel> tempList = new ArrayList<EveryDayRobotModel>();
		if (nowDateInt == targetDateInt) {
			List<EveryDayRobotModel> list = MongoDBServiceImpl.getInstance().robotlist;
			if (list != null && !list.isEmpty()) {
				MongoDBServiceImpl.getInstance().everyDayRobotModel(0, false);
				list = MongoDBServiceImpl.getInstance().robotlist;
				for (EveryDayRobotModel everyDay : list) {
					if (everyDay.getAccount_id() == userID) {
						tempList.add(everyDay);
					}
				}
			}
			resultMap.put("data", tempList);
			resultMap.put("userId", account.getAccountModel().getAccount_id());
			resultMap.put("result", SUCCESS);
		} else {
			List<EveryDayRobotModel> list = MongoDBServiceImpl.getInstance().robotlist;
			if (list != null && !list.isEmpty()) {
				MongoDBServiceImpl.getInstance().everyDayRobotModel(0, false);
				list = MongoDBServiceImpl.getInstance().robotlist;
				for (EveryDayRobotModel everyDay : list) {
					if (everyDay.getAccount_id() == userID) {
						tempList.add(everyDay);
					}
				}
			}

			List<EveryDayRobotModel> robotDayList = SpringService.getBean(MongoDBService.class).getMongoTemplate().find(query,
					EveryDayRobotModel.class);
			robotDayList.addAll(tempList);
			resultMap.put("data", robotDayList);

			resultMap.put("userId", account.getAccountModel().getAccount_id());
			resultMap.put("result", SUCCESS);
		}

	}

	/**
	 * 根据群ID 查询 开房详情记录
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void roomDetailGroupRealTime(Map<String, String> params, Map<String, Object> resultMap) {
		int curPage;
		String groupID;
		Date targetDate = new Date();
		try {
			curPage = Integer.parseInt(params.get("cur_page"));
			groupID = params.get("groupID");
			String targetDateStr = params.get("targetDateInt");
			if (targetDateStr != null) {
				targetDate = new SimpleDateFormat("yyyyMMdd").parse(targetDateStr);
			}
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}

		resultMap.put("groupID", groupID);
		resultMap.put("result", SUCCESS);

		int totalSize = MongoDBServiceImpl.getInstance().getRobotRoomModelCount(groupID, targetDate);
		Page page = new Page(curPage, 10, totalSize);
		List<RobotModel> robotList = MongoDBServiceImpl.getInstance().getRobotModelList(page, groupID, targetDate);

		resultMap.put("result", SUCCESS);
		resultMap.put("curPage", curPage);
		resultMap.put("pageSize", 10);
		resultMap.put("totalSize", totalSize);
		resultMap.put("data", robotList);

	}

	/**
	 * 根据acountId 查询推广的收益详情详情记录
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void recommendIncomeDetailByMonth(Map<String, String> params, Map<String, Object> resultMap) {
		int curPage;
		String groupID;
		Date targetDate = new Date();
		try {
			curPage = Integer.parseInt(params.get("cur_page"));
			groupID = params.get("groupID");
			String targetDateStr = params.get("targetDateInt");
			if (targetDateStr != null) {
				targetDate = new SimpleDateFormat("yyyyMMdd").parse(targetDateStr);
			}
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}

		resultMap.put("groupID", groupID);
		resultMap.put("result", SUCCESS);

		int totalSize = MongoDBServiceImpl.getInstance().getRobotRoomModelCount(groupID, targetDate);
		Page page = new Page(curPage, 10, totalSize);
		List<RobotModel> robotList = MongoDBServiceImpl.getInstance().getRobotModelList(page, groupID, targetDate);

		resultMap.put("result", SUCCESS);
		resultMap.put("curPage", curPage);
		resultMap.put("pageSize", 10);
		resultMap.put("totalSize", totalSize);
		resultMap.put("data", robotList);

	}

	private void roomDetailGroup(Map<String, String> params, Map<String, Object> resultMap) {
		int targetDateInt = 0;
		String groupID;
		try {
			String targetDateStr = params.get("targetDateInt");
			if (targetDateStr != null) {
				targetDateInt = Integer.parseInt(targetDateStr);
			}
			groupID = params.get("groupID");
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}

		resultMap.put("result", SUCCESS);

		Date now = new Date();
		Aggregation aggregation = Aggregation.newAggregation(
				Aggregation.match(Criteria.where("groupId").is(groupID).and("create_time").gte(MyDateUtil.getZeroDate(now))
						.lte(MyDateUtil.getTomorrowZeroDate(now))),
				Aggregation.group().sum("v1").as("goldtotal").sum("v2").as("brandtotal").last("account_id").as("account_id"));

		AggregationResults<HashMap> models = SpringService.getBean(MongoDBService.class).getMongoTemplate().aggregate(aggregation, "robot_log",
				HashMap.class);
		HashMap mm = models.getUniqueMappedResult();
		if (mm != null) {
			resultMap.put("goldtotal", mm.get("goldtotal"));
			resultMap.put("brandtotal", mm.get("brandtotal"));
		} else {
			resultMap.put("goldtotal", 0);
			resultMap.put("brandtotal", 0);
		}

		Criteria crieria = new Criteria();
		Query query = Query.query(crieria);
		crieria.and("groupId").is(groupID);
		if (targetDateInt != 0) {
			crieria.and("notes_date").is(targetDateInt);
		}

		int nowDateInt = Integer.valueOf(DateFormatUtils.format(new Date(), "yyyyMMdd"));
		List<EveryDayRobotModel> tempList = new ArrayList<EveryDayRobotModel>();
		if (nowDateInt == targetDateInt) {
			List<EveryDayRobotModel> list = MongoDBServiceImpl.getInstance().robotlist;
			if (list != null || !list.isEmpty()) {
				MongoDBServiceImpl.getInstance().everyDayRobotModel(0, false);
				list = MongoDBServiceImpl.getInstance().robotlist;
				for (EveryDayRobotModel everyDay : list) {
					if (everyDay.getGroupId().equals(groupID)) {
						tempList.add(everyDay);
					}
				}
			}
			resultMap.put("data", tempList);
		} else {
			List<EveryDayRobotModel> robotDayList = SpringService.getBean(MongoDBService.class).getMongoTemplate().find(query,
					EveryDayRobotModel.class);
			resultMap.put("data", robotDayList);
		}

	}

	/**
	 * 昨日下级代理 消耗 明细
	 * 
	 * @param request
	 * @param params
	 */
	private void userProxyConsumerDetail(Map<String, String> params, Map<String, Object> resultMap) {
		String userID = params.get("userID");
		if (org.apache.commons.lang.StringUtils.isEmpty(userID)) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", ErrorCode.PARAMETER_EMPTY);
			return;
		}
		Account account = PublicServiceImpl.getInstance().getAccount(Long.parseLong(userID));
		if (account == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", ErrorCode.ROLE_FIND_FAIL);
			return;
		}

		resultMap.put("userId", account.getAccountModel().getAccount_id());
		resultMap.put("result", SUCCESS);
		resultMap.put("totalRMB", account.getAccountModel().getHistory_rmb());// 总收益
		resultMap.put("receiveRMB", account.getAccountModel().getReceive_rmb());// 已提现
		resultMap.put("canRMB", account.getAccountModel().getRmb());// 可提现

		Date now = new Date();
		Date targetDate = DateUtils.addDays(now, -1);
		int targetDateInt = Integer.valueOf(DateFormatUtils.format(targetDate, "yyyyMMdd"));

		Criteria crieria = new Criteria();
		Query query = Query.query(crieria);
		crieria.and("account_id").is(Long.parseLong(userID));
		crieria.and("notes_date").is(targetDateInt);

		OneProxyAccountReplaceRoomModel oneProxyAccount = SpringService.getBean(MongoDBService.class).getMongoTemplate().findOne(query,
				OneProxyAccountReplaceRoomModel.class);
		if (oneProxyAccount != null) {
			List<ProxyAccountReplaceRoomModel> replaceList = new ArrayList<ProxyAccountReplaceRoomModel>();

			String proxyIDs = oneProxyAccount.getLower_proxy_account_ids();
			String[] ids = StringUtils.split(proxyIDs, ",");
			for (String id : ids) {
				Criteria targetcrieria = new Criteria();
				Query targetquery = Query.query(targetcrieria);
				targetcrieria.and("account_id").is(Long.parseLong(id));
				targetcrieria.and("notes_date").is(targetDateInt);
				ProxyAccountReplaceRoomModel proxyOne = SpringService.getBean(MongoDBService.class).getMongoTemplate().findOne(targetquery,
						ProxyAccountReplaceRoomModel.class);
				if (proxyOne != null) {
					AccountSimple simple = PublicServiceImpl.getInstance().getAccountSimpe(Long.parseLong(userID));
					if (simple != null) {
						proxyOne.setNickName(simple.getNick_name());
						proxyOne.setHeardUrl(simple.getIcon());
					}
					replaceList.add(proxyOne);
				}
			}
			resultMap.put("propxyList", replaceList);
		}

	}

	/**
	 * 获取用户信息
	 * 
	 * @param request
	 * @param params
	 */
	private void userDetail(Map<String, String> params, Map<String, Object> resultMap) {
		String userID = params.get("userID");
		if (org.apache.commons.lang.StringUtils.isEmpty(userID)) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", ErrorCode.PARAMETER_EMPTY);
			return;
		}
		Account account = PublicServiceImpl.getInstance().getAccount(Long.parseLong(userID));
		if (account == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", ErrorCode.ROLE_FIND_FAIL);
			return;
		}

		resultMap.put("userId", account.getAccountModel().getAccount_id());
		resultMap.put("result", SUCCESS);
		resultMap.put("totalRMB", account.getAccountModel().getHistory_rmb());// 总收益
		resultMap.put("receiveRMB", account.getAccountModel().getReceive_rmb());// 已提现
		resultMap.put("canRMB", account.getAccountModel().getRmb());// 可提现
		resultMap.put("banned", account.getAccountModel().getBanned());
		Date now = new Date();
		Date targetDate = DateUtils.addDays(now, -1);
		int targetDateInt = Integer.valueOf(DateFormatUtils.format(targetDate, "yyyyMMdd"));

		Criteria crieria = new Criteria();
		Query query = Query.query(crieria);
		crieria.and("account_id").is(Long.parseLong(userID));
		crieria.and("notes_date").is(targetDateInt);

		OneProxyAccountReplaceRoomModel proxyOne = SpringService.getBean(MongoDBService.class).getMongoTemplate().findOne(query,
				OneProxyAccountReplaceRoomModel.class);
		if (proxyOne != null) {
			resultMap.put("yesterdayRMB", proxyOne.getMoney());// 昨日收益
			resultMap.put("yesterdayNextTotalGold", proxyOne.getToday_consume());// 昨日下级总消耗
		}
	}

	/**
	 * 获取用户信息
	 * 
	 * @param request
	 * @param params
	 */
	private void getUser(Map<String, String> params, Map<String, Object> resultMap) {
		String userID = params.get("userID");
		if (org.apache.commons.lang.StringUtils.isEmpty(userID)) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", ErrorCode.PARAMETER_EMPTY);
			return;
		}
		// long uID = Long.parseLong(userID);
		Account account = PublicServiceImpl.getInstance().getAccount(Long.parseLong(userID));
		if (account == null || account.getAccountWeixinModel() == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", ErrorCode.ROLE_FIND_FAIL);
			return;
		}

		resultMap.put("userId", account.getAccountWeixinModel().getAccount_id());
		resultMap.put("result", SUCCESS);
		resultMap.put("nickName", account.getAccountWeixinModel().getNickname());
		resultMap.put("isAgent", account.getAccountModel().getIs_agent());
		resultMap.put("headUrl", account.getAccountWeixinModel().getHeadimgurl());
		resultMap.put("gold", account.getAccountModel().getGold());
		resultMap.put("upAgent", account.getAccountModel().getUp_proxy());
		resultMap.put("unionid", account.getAccountWeixinModel().getUnionid());
		resultMap.put("totalConsum", account.getAccountModel().getConsum_total());
		resultMap.put("is_rebate", account.getAccountModel().getIs_rebate());
		resultMap.put("banned", account.getAccountModel().getBanned());
		resultMap.put("mobile", account.getAccountModel().getMobile_phone());
		resultMap.put("proxyUpdateDate", account.getHallRecommendModel().getUpdate_time());
		if (account.getHallRecommendModel() != null) {
			resultMap.put("hall_recommend_level", account.getHallRecommendModel().getRecommend_level());
			resultMap.put("proxyUpdateDate", account.getHallRecommendModel().getUpdate_time());
		} else {
			resultMap.put("hall_recommend_level", 0);
		}
		try {
			// Criteria crieria = new Criteria();
			// Query query = Query.query(crieria);
			// crieria.and("account_id").is(userID);
			// int totalRobot = 0;
			// List<EveryDayRobotModel> robotDayList =
			// SpringService.getBean(MongoDBService.class).getMongoTemplate()
			// .find(query, EveryDayRobotModel.class);
			// for(EveryDayRobotModel day:robotDayList) {
			// totalRobot+=day.getGoldTotal();
			// }
			// MongoDBServiceImpl mongoDBServiceImpl =
			// MongoDBServiceImpl.getInstance();
			// long totalRobot =
			// mongoDBServiceImpl.getTotalRobotGold(account.getAccount_id());
			//
			// List<EveryDayRobotModel> tempList =
			// MongoDBServiceImpl.getInstance().robotlist;// 当天
			// if (tempList != null && !tempList.isEmpty()) {
			// MongoDBServiceImpl.getInstance().everyDayRobotModel(0, false);
			// tempList = MongoDBServiceImpl.getInstance().robotlist;
			// for (EveryDayRobotModel everyDay : tempList) {
			// if (everyDay.getAccount_id() == uID) {
			// totalRobot += everyDay.getGoldTotal();
			// }
			// }
			// }

			// resultMap.put("totalRobot", totalRobot);
			// resultMap.put("saleGoldCount",
			// mongoDBServiceImpl.getProxyGoldLogModelGiveCount(account.getAccount_id()));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * 获取用户信息
	 * 
	 * @param request
	 * @param params
	 */
	private void getUserByUnionID(Map<String, String> params, Map<String, Object> resultMap) {
		String unionID = params.get("unionID");
		if (org.apache.commons.lang.StringUtils.isEmpty(unionID)) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", ErrorCode.PARAMETER_EMPTY);
			return;
		}
		Account account = PublicServiceImpl.getInstance().getAccountByWxUnionid(unionID);
		if (account == null || account.getAccountWeixinModel() == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", ErrorCode.ROLE_FIND_FAIL);
			return;
		}

		resultMap.put("userId", account.getAccountWeixinModel().getAccount_id());
		resultMap.put("result", SUCCESS);
		resultMap.put("nickName", account.getAccountWeixinModel().getNickname());
		resultMap.put("isAgent", account.getAccountModel().getIs_agent());
		resultMap.put("headUrl", account.getAccountWeixinModel().getHeadimgurl());
		resultMap.put("gold", account.getAccountModel().getGold());
		resultMap.put("upAgent", account.getAccountModel().getUp_proxy());
		resultMap.put("unionid", account.getAccountWeixinModel().getUnionid());
		resultMap.put("totalConsum", account.getAccountModel().getConsum_total());
		resultMap.put("is_rebate", account.getAccountModel().getIs_rebate());
		resultMap.put("banned", account.getAccountModel().getBanned());
		resultMap.put("openId", account.getAccountWeixinModel().getOpenid());
		resultMap.put("proxy_level", account.getAccountModel().getProxy_level());
		resultMap.put("mobile", account.getAccountModel().getMobile_phone());
		resultMap.put("proxyUpdateDate", account.getHallRecommendModel().getUpdate_time());
		if (account.getHallRecommendModel() != null) {
			resultMap.put("hall_recommend_level", account.getHallRecommendModel().getRecommend_level());
			resultMap.put("proxyUpdateDate", account.getHallRecommendModel().getUpdate_time());
		} else {
			resultMap.put("hall_recommend_level", 0);
		}
		// try {
		// Criteria crieria = new Criteria();
		// Query query = Query.query(crieria);
		// crieria.and("account_id").is(account.getAccountWeixinModel().getAccount_id());
		// int totalRobot = 0;
		// List<EveryDayRobotModel> robotDayList =
		// SpringService.getBean(MongoDBService.class).getMongoTemplate().find(query,
		// EveryDayRobotModel.class);
		// for (EveryDayRobotModel day : robotDayList) {
		// totalRobot += day.getGoldTotal();
		// }
		//
		// List<EveryDayRobotModel> tempList =
		// MongoDBServiceImpl.getInstance().robotlist;
		// if (tempList != null && !tempList.isEmpty()) {
		// MongoDBServiceImpl.getInstance().everyDayRobotModel(0, false);
		// tempList = MongoDBServiceImpl.getInstance().robotlist;
		// for (EveryDayRobotModel everyDay : tempList) {
		// if (everyDay.getAccount_id() ==
		// account.getAccountWeixinModel().getAccount_id()) {
		// totalRobot += everyDay.getGoldTotal();
		// }
		// }
		// }
		// resultMap.put("totalRobot", totalRobot);
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
	}

	/**
	 * 获取用户信息,网页机器人
	 * 
	 * @param request
	 * @param params
	 */
	private void getRobotUser(Map<String, String> params, Map<String, Object> resultMap) {
		String unionID = params.get("unionID");
		String groupId = params.get("groupId");
		String groupName = params.get("groupName");
		String accountId = params.get("userId");
		if (org.apache.commons.lang.StringUtils.isEmpty(unionID)) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", ErrorCode.PARAMETER_EMPTY);
			return;
		}
		Account accountu = PublicServiceImpl.getInstance().getAccountByWxUnionid(unionID);
		if (accountu == null || accountu.getAccountWeixinModel() == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", ErrorCode.ROLE_FIND_FAIL);
			return;
		}
		if (!accountu.getAccountGroupModelMap().containsKey(groupId)) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "群号：" + groupName + " 还未绑定你的游戏id,请联系本群代理绑定");
			return;
		}
		if (StringUtils.isBlank(accountId) || accountId.equals("0")) {
			resultMap.put("result", SUCCESS);
			return;
		}
		Account account = PublicServiceImpl.getInstance().getAccount(Long.parseLong(accountId));
		resultMap.put("userId", account.getAccountWeixinModel().getAccount_id());
		resultMap.put("result", SUCCESS);
		resultMap.put("nickName", account.getAccountWeixinModel().getNickname());
		resultMap.put("isAgent", account.getAccountModel().getIs_agent());
		resultMap.put("headUrl", account.getAccountWeixinModel().getHeadimgurl());
		resultMap.put("gold", account.getAccountModel().getGold());
		resultMap.put("upAgent", account.getAccountModel().getUp_proxy());
		resultMap.put("unionid", account.getAccountWeixinModel().getUnionid());
		resultMap.put("totalConsum", account.getAccountModel().getConsum_total());
		resultMap.put("is_rebate", account.getAccountModel().getIs_rebate());
		resultMap.put("banned", account.getAccountModel().getBanned());
		resultMap.put("openId", account.getAccountWeixinModel().getOpenid());
	}

	/**
	 * 获取战绩
	 * 
	 * @param request
	 * @param params
	 */
	private void getRobotResult(Map<String, String> params, Map<String, Object> resultMap) {
		int curPage;
		String groupID;
		Date targetDate = new Date();
		try {
			curPage = Integer.parseInt(params.get("cur_page"));
			groupID = params.get("groupID");
			String targetDateStr = params.get("targetDateInt");
			if (targetDateStr != null) {
				targetDate = new SimpleDateFormat("yyyyMMdd").parse(targetDateStr);
			}
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		int totalSize = MongoDBServiceImpl.getInstance().getBrandResultCount(groupID);
		Page page = new Page(curPage, 10, totalSize);
		List<BrandResultModel> list = MongoDBServiceImpl.getInstance().getBrandResultModelList(page, groupID, targetDate);
		resultMap.put("result", SUCCESS);
		resultMap.put("curPage", curPage);
		resultMap.put("pageSize", 10);
		resultMap.put("totalSize", totalSize);
		resultMap.put("brandList", list);
	}

	/**
	 * 修复订单
	 * 
	 * @param request
	 * @param params
	 */
	private void repairOrder(Map<String, String> params, Map<String, Object> resultMap) {
		String orderID = params.get("orderID");
		if (org.apache.commons.lang.StringUtils.isEmpty(orderID)) {
			resultMap.put("result", "参数异常");
			return;
		}
		PayCenterRunnable payCenterRunnable = new PayCenterRunnable(orderID, 0);
		payCenterRunnable.run();

		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("orderID").is(orderID));
		AddCardLog addCardLog = mongoDBService.getMongoTemplate().findOne(query, AddCardLog.class);
		if (addCardLog == null) {
			resultMap.put("remark", "找不到订单");
			return;
		}
		resultMap.put("result", addCardLog.getOrderStatus());
		resultMap.put("remark", "订单修复状态" + addCardLog.getOrderStatus());
	}

	/**
	 * 获取商品列表
	 * 
	 * @param request
	 * @param params
	 */
	private void getShop(Map<String, String> params, Map<String, Object> resultMap) {

		String game_id = params.get("appId");
		// 验证必填参数格式
		int gameID;
		try {
			gameID = Integer.parseInt(game_id);
		} catch (Exception e) {
			resultMap.put("result", "参数异常");
			return;
		}

		// 查询商品列表
		FastMap<Integer, ShopModel> shopMap = ShopDict.getInstance().getShopModelMapByGameId(gameID);
		List<ShopModel> shopModels = new ArrayList<>();
		if (shopMap != null) {
			shopModels.addAll(shopMap.values());
		}
		FastMap<Integer, ShopModel> allShopMap = ShopDict.getInstance().getShopModelMapByGameId(0);
		if (allShopMap != null) {
			shopModels.addAll(allShopMap.values());
		}
		List<Product> productList = new ArrayList<>();
		Product product = null;
		for (ShopModel shop : shopModels) {
			product = new Product();
			product.setDisplay_order(shop.getDisplay_order());
			product.setGold(shop.getGold());
			product.setName(shop.getName());
			product.setPrice(shop.getPrice());
			product.setSend_gold(shop.getSend_gold());
			product.setShop_type(shop.getShop_type());
			product.setShopId(shop.getId());
			productList.add(product);
		}

		resultMap.put("appId", game_id);
		resultMap.put("result", SUCCESS);
		resultMap.put("productList", productList);
	}

	/**
	 * 获取转卡记录
	 * 
	 * @param request
	 * @param params
	 */
	private void getMyOrder(Map<String, String> params, Map<String, Object> resultMap) {
		// 当前页
		String cur_page = params.get("curPage");
		String user_ID = params.get("userID");
		String target_ID = params.get("targetID");
		// 验证必填参数格式
		int curPage;
		long userID;
		Long targetID = null;
		try {
			curPage = Integer.parseInt(cur_page);
			userID = Long.parseLong(user_ID);

			if (target_ID != null) {
				targetID = Long.parseLong(target_ID);
			}
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}

		Account account = PublicServiceImpl.getInstance().getAccount(userID);
		if (account == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "找不到玩家");
			return;
		}

		int totalSize = MongoDBServiceImpl.getInstance().getProxyGoldLogModelCount(userID, targetID);
		Page page = new Page(curPage, 10, totalSize);
		List<ProxyGoldLogModel> proxyGoldLogModelList = MongoDBServiceImpl.getInstance().getProxyGoldLogModelList(page, userID, targetID);

		for (ProxyGoldLogModel goldLog : proxyGoldLogModelList) {
			Account targetAccount = PublicServiceImpl.getInstance().getAccount(goldLog.getTarget_account_id());
			if (targetAccount != null) {
				goldLog.setTarget_head_url(targetAccount.getAccountWeixinModel().getHeadimgurl());
				goldLog.setTarget_nick_name(targetAccount.getAccountWeixinModel().getNickname());
			}
		}

		resultMap.put("result", SUCCESS);
		resultMap.put("curPage", curPage);
		resultMap.put("pageSize", 10);
		resultMap.put("totalSize", totalSize);
		resultMap.put("proxyLogList", proxyGoldLogModelList);
	}

	/**
	 * 获取我的代理列表
	 * 
	 * @param request
	 * @param params
	 */
	private void getProxyList(Map<String, String> params, Map<String, Object> resultMap) {

		String user_ID = params.get("userID");
		// 验证必填参数格式
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
			resultMap.put("msg", "找不到玩家");
			return;
		}
		Map<Long, AccountProxyModel> proxyMap = account.getAccountProxyModelMap();
		List<AccountProxyModel> list = new ArrayList<AccountProxyModel>(proxyMap.values());
		for (AccountProxyModel proxyModel : list) {
			Account targetaccount = PublicServiceImpl.getInstance().getAccount(proxyModel.getTarget_account_id());
			if (targetaccount != null) {
				proxyModel.setTarget_gold(targetaccount.getAccountModel().getGold());
				proxyModel.setTarget_total_consum(targetaccount.getAccountModel().getConsum_total());
				if (targetaccount.getAccountWeixinModel() != null) {
					proxyModel.setTarget_icon(targetaccount.getAccountWeixinModel().getHeadimgurl());
					proxyModel.setTarget_name(targetaccount.getAccountWeixinModel().getNickname());
				}
			}
		}
		resultMap.put("result", SUCCESS);
		resultMap.put("proxyList", list);
	}

	/**
	 * 删除我的代理
	 * 
	 * @param request
	 * @param params
	 */
	private void deleteProxy(Map<String, String> params, Map<String, Object> resultMap) {

		String user_ID = params.get("userID");
		String target_ID = params.get("subAgentID");
		// 验证必填参数格式
		long userID;
		long targetID;
		try {
			userID = Long.parseLong(user_ID);
			targetID = Long.parseLong(target_ID);
		} catch (Exception e) {
			resultMap.put("msg", "参数异常");
			resultMap.put("result", FAIL);
			return;
		}
		Account account = PublicServiceImpl.getInstance().getAccount(userID);
		Account targetAccount = PublicServiceImpl.getInstance().getAccount(targetID);
		if (account == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "玩家不存在");
			return;
		}
		if (targetAccount == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "目标玩家不存在");
			return;
		}

		Map<Long, AccountProxyModel> proxyMap = account.getAccountProxyModelMap();
		AccountProxyModel accountProxy = proxyMap.remove(targetID);

		if (targetAccount.getAccountModel().getUp_proxy() != userID || accountProxy == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "对方不是您的下级代理");
			return;
		}

		PublicService publicService = SpringService.getBean(PublicService.class);
		// 同步到redis
		if (accountProxy != null) {
			RsAccountModelResponse.Builder rsTargetAccountModelResponseBuilder = RsAccountModelResponse.newBuilder();

			publicService.getPublicDAO().deleteAccountProxyModel(accountProxy);
			targetAccount.getAccountModel().setUp_proxy(0);
			SpringService.getBean(PublicService.class).updateObject("updateAccountModel", targetAccount.getAccountModel());

			rsTargetAccountModelResponseBuilder.setUpProxy(0);

			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			RsAccountProxyResponse.Builder rsAccountProxyResponse = RsAccountProxyResponse.newBuilder();
			rsAccountProxyResponse.setAccountId(userID);
			rsAccountProxyResponse.setTargetAccountId(targetID);
			rsAccountProxyResponse.setIsDeleteTarget(true);// 删除
			rsAccountResponseBuilder.addRsRsAccountProxyResponseList(rsAccountProxyResponse);

			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			rsAccountResponseBuilder.setAccountId(targetID);
			rsAccountResponseBuilder.setRsAccountModelResponse(rsTargetAccountModelResponseBuilder);
			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
		}

		RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);

		RsAccountProxyResponse.Builder rsAccountProxyResponse = RsAccountProxyResponse.newBuilder();
		rsAccountProxyResponse.setAccountId(userID);
		rsAccountProxyResponse.setTargetAccountId(targetID);
		rsAccountProxyResponse.setIsDeleteTarget(true);// 删除我的下级
		rsAccountResponseBuilder.addRsRsAccountProxyResponseList(rsAccountProxyResponse);
		redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
		resultMap.put("result", SUCCESS);
	}

	/**
	 * 转卡
	 * 
	 * @param request
	 * @param params
	 */
	private void turnCard(Map<String, String> params, Map<String, Object> resultMap) {

		String user_ID = params.get("sendUserID");
		String target_ID = params.get("recUserID");
		String gold_num = params.get("coins");
		// 验证必填参数格式
		long userID;
		int goldNum;
		Long targetID = null;
		try {
			userID = Long.parseLong(user_ID);
			goldNum = Integer.parseInt(gold_num);
			targetID = Long.parseLong(target_ID);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		Account account = PublicServiceImpl.getInstance().getAccount(userID);
		Account targetAccount = PublicServiceImpl.getInstance().getAccount(targetID);

		if (account == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "玩家不存在");
			return;
		}
		if (account.getAccountModel().getBanned() == 1) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "您已封号，暂不能转卡");
			return;
		}

		if (targetAccount == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "目标玩家不存在");
			return;
		}

		if (goldNum <= 0) {
			return;
		}
		// 开通特殊id给下级代理转卡的功能
		boolean specialAccountId = false;
		SysParamModel sysParamModel2234 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2234);
		int openProxyTurnCard = 0;
		if (sysParamModel2234 != null) {
			openProxyTurnCard = sysParamModel2234.getVal2();
			String[] accountIds = sysParamModel2234.getStr1().split(",");
			for (String id : accountIds) {
				if (user_ID.equals(id)) {
					specialAccountId = true;
					break;
				}
			}
		}
		if (specialAccountId) {
			if (targetAccount.getAccountModel().getProxy_level() > 0) {
				if (targetAccount.getHallRecommendModel().getAccount_id() != userID) {
					resultMap.put("result", FAIL);
					resultMap.put("msg", "不能给非自己的下级代理冲卡");
					return;
				}
			}
		} else {
			if (targetAccount.getAccountModel().getProxy_level() > 0) {// 默认不开放上级向下级转豆
				if (openProxyTurnCard == 0 || targetAccount.getHallRecommendModel().getAccount_id() != userID) {
					resultMap.put("result", FAIL);
					resultMap.put("msg", "暂时不支持给代理转卡");
					return;
				}
			}
		}
		int gameID = 5;
		if (account.getGame_id() != 0) {
			gameID = account.getGame_id();
		}
		SysParamModel sysParamModel1109 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameID).get(1109);
		if (!specialAccountId && sysParamModel1109 != null && sysParamModel1109.getVal2() == 0) {// 控制非3级代理是否可以转给普通玩家
			if (account.getAccountModel().getProxy_level() >= 0 && account.getAccountModel().getProxy_level() <= 2
					&& targetAccount.getAccountModel().getUp_proxy() == 0) {
				resultMap.put("result", FAIL);
				resultMap.put("msg", "您不能转卡给普通玩家");
				return;
			}
		}

		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		AddGoldResultModel addGoldResultModel = centerRMIServer.addAccountGold(account.getAccount_id(), -goldNum, false,
				"游戏内转卡,转给account_id:" + targetID, EGoldOperateType.PROXY_GIVE);
		if (!addGoldResultModel.isSuccess()) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "转卡失败,存库不足");
			return;
		}

		// 日志
		String nick_name = "--";
		if (targetAccount.getAccountWeixinModel() != null) {
			String name = targetAccount.getAccountWeixinModel().getNickname();
			if (StringUtils.isNotEmpty(name))
				nick_name = name;
		}
		int target_proxy_account = targetAccount.getAccountModel().getProxy_level();
		MongoDBServiceImpl.getInstance().proxyGoldLog(account.getAccount_id(), targetAccount.getAccount_id(), nick_name, goldNum,
				account.getAccountModel().getClient_ip(), 0, target_proxy_account, account.getAccountModel().getGold());
		centerRMIServer.addAccountGold(targetAccount.getAccount_id(), goldNum, false, "游戏内转卡,接收account_id:" + account.getAccount_id(),
				EGoldOperateType.PROXY_GIVE);
		resultMap.put("result", SUCCESS);
	}

	/**
	 * 转卡
	 * 
	 * @param request
	 * @param params
	 */
	private void giveCard(Map<String, String> params, Map<String, Object> resultMap) {

		String user_ID = params.get("sendUserID");
		String target_ID = params.get("recUserID");
		String gold_num = params.get("coins");
		// 验证必填参数格式
		long userID;
		int goldNum;
		Long targetID = null;
		try {
			userID = Long.parseLong(user_ID);
			goldNum = Integer.parseInt(gold_num);
			targetID = Long.parseLong(target_ID);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		Account account = PublicServiceImpl.getInstance().getAccount(userID);
		Account targetAccount = PublicServiceImpl.getInstance().getAccount(targetID);

		if (account == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "玩家不存在");
			return;
		}
		if (targetAccount == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "目标玩家不存在");
			return;
		}

		if (goldNum <= 0) {
			return;
		}
		if (account.getGame_id() == 6) {
			SysParamModel sysParamModel1109 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(account.getGame_id()).get(1109);
			if (sysParamModel1109 != null && sysParamModel1109.getVal3() == 0) {// 是否控制代理之间互相转卡，0禁止，1不禁止
				if (account.getAccountModel().getProxy_level() > 0 && targetAccount.getAccountModel().getProxy_level() > 0) {
					resultMap.put("result", FAIL);
					resultMap.put("msg", "代理之间不能相互转卡");
					return;
				}
			}
		}
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		AddGoldResultModel addGoldResultModel = centerRMIServer.addAccountGold(account.getAccount_id(), -goldNum, false,
				"游戏内转卡,转给account_id:" + targetID, EGoldOperateType.PROXY_GIVE);
		if (!addGoldResultModel.isSuccess()) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "转卡失败,存库不足");
			return;
		}

		// 日志
		String nick_name = "--";
		if (targetAccount.getAccountWeixinModel() != null) {
			String name = targetAccount.getAccountWeixinModel().getNickname();
			if (StringUtils.isNotEmpty(name))
				nick_name = name;
		}
		int target_proxy_account = targetAccount.getAccountModel().getProxy_level();
		MongoDBServiceImpl.getInstance().proxyGoldLog(account.getAccount_id(), targetAccount.getAccount_id(), nick_name, goldNum,
				account.getAccountModel().getClient_ip(), 0, target_proxy_account, account.getAccountModel().getGold());
		centerRMIServer.addAccountGold(targetAccount.getAccount_id(), goldNum, false, "游戏内转卡,接收account_id:" + account.getAccount_id(),
				EGoldOperateType.PROXY_GIVE);
		resultMap.put("result", SUCCESS);
	}

	/**
	 * setProxyRemark
	 */
	private void setProxyRemark(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		String target_ID = params.get("subAgentID");
		String remark = params.get("remark");
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
		Account targetAccount = PublicServiceImpl.getInstance().getAccount(targetID);
		if (account == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "玩家不存在");
			return;
		}
		if (targetAccount == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "目标玩家不存在");
			return;
		}

		int proxy_account = account.getAccountModel().getIs_agent();
		if (proxy_account <= 0) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "您还不是代理，请找客服申请");
			return;
		}
		AccountProxyModel targertProxy = account.getAccountProxyModelMap().get(targetID);
		if (targertProxy == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "对方不是您的下级代理");
			return;
		}
		targertProxy.setRemark(remark);

		PublicService publicService = SpringService.getBean(PublicService.class);
		publicService.getPublicDAO().updateAccountProxyModel(targertProxy);

		resultMap.put("result", SUCCESS);
	}

	/**
	 * 申请代理
	 * 
	 * @param request
	 * @param params
	 */
	private void setProxy(Map<String, String> params, Map<String, Object> resultMap) {

		String user_ID = params.get("userID");
		String target_ID = params.get("subAgentID");
		String mobile = params.get("mobile");
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
		Account targetAccount = PublicServiceImpl.getInstance().getAccount(targetID);
		if (targetAccount == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "目标玩家不存在");
			return;
		}

		int proxy_account = account.getAccountModel().getIs_agent();
		if (proxy_account <= 0) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "您还不是代理，请找客服申请");
			return;
		}
		int target_proxy_account = targetAccount.getAccountModel().getIs_agent();
		if (target_proxy_account != 0) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "对方已经是代理,请对方先申请解除绑定");
			return;
		}

		if (account.getAccountModel().getProxy_level() >= 3) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "当前代理等级不能增加下级");
			return;
		}

		RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();

		RsAccountModelResponse.Builder rsTargetAccountModelResponseBuilder = RsAccountModelResponse.newBuilder();

		AccountProxyModel accountProxy = account.getAccountProxyModelMap().get(targetID);
		if (accountProxy != null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "当前玩家已经是您的下级");
			return;
		}

		if (targetAccount.getAccountModel().getUp_proxy() != 0) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "对方已经是代理,请对方先申请解除绑定");
		}

		PublicService publicService = SpringService.getBean(PublicService.class);
		AccountProxyModel targertProxy = new AccountProxyModel();
		targertProxy.setAccount_id(userID);
		targertProxy.setApply(1);
		targertProxy.setCreate_time(new Date());
		targertProxy.setTarget_account_id(targetID);
		targertProxy.setTarget_proxy_level(account.getAccountModel().getProxy_level() + 1);// 目标的代理等级
																							// 当前的
		account.getAccountProxyModelMap().put(targetID, targertProxy);
		publicService.getPublicDAO().insertAccountProxyModel(targertProxy);

		targetAccount.getAccountModel().setIs_agent(account.getAccountModel().getProxy_level() + 1);
		targetAccount.getAccountModel().setProxy_level(account.getAccountModel().getProxy_level() + 1);
		targetAccount.getAccountModel().setUp_proxy(userID);

		SpringService.getBean(PublicService.class).updateObject("updateAccountModel", targetAccount.getAccountModel());

		rsTargetAccountModelResponseBuilder.setUpProxy(userID);
		rsTargetAccountModelResponseBuilder.setProxyLevel(account.getAccountModel().getProxy_level() + 1);
		rsTargetAccountModelResponseBuilder.setIsAgent(account.getAccountModel().getProxy_level() + 1);
		if (StringUtils.isNotBlank(mobile)) {
			rsTargetAccountModelResponseBuilder.setMobilePhone(mobile);
		}
		RsAccountProxyResponse.Builder rsAccountProxyResponse = RsAccountProxyResponse.newBuilder();
		rsAccountProxyResponse.setAccountId(targertProxy.getAccount_id());
		rsAccountProxyResponse.setTargetAccountId(targertProxy.getTarget_account_id());
		rsAccountProxyResponse.setCreateTime(targertProxy.getCreate_time().getTime());
		if (targetAccount.getAccountWeixinModel() != null) {
			rsAccountProxyResponse.setTargetName(targetAccount.getAccountWeixinModel().getNickname());
			rsAccountProxyResponse.setTargetIcon(targetAccount.getAccountWeixinModel().getHeadimgurl());
		}
		rsAccountResponseBuilder.addRsRsAccountProxyResponseList(rsAccountProxyResponse);

		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
		//
		rsAccountResponseBuilder.setAccountId(targetID);
		//
		rsAccountResponseBuilder.setRsAccountModelResponse(rsTargetAccountModelResponseBuilder);
		//
		redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
				ERedisTopicType.topicProxy.getId() + targetAccount.getLastProxyIndex());
		resultMap.put("result", SUCCESS);
	}

	private void getGoodsState(Map<String, String> params, Map<String, Object> resultMap) {
		int shopId;
		try {
			shopId = Integer.parseInt(params.get("goodsId"));
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数有误");
			return;
		}
		ShopModel shop = ShopDict.getInstance().getShopModel(shopId);
		if (shop == null || shop.getState() != 1) {
			logger.error("商品不在列表中！！！shopID==" + shopId);
			resultMap.put("result", ErrorCode.SHOP_ERROR_FAIL);
			resultMap.put("remark", "商品不在列表中:" + shopId);
			resultMap.put("msg", "商品暂时停止售卖");
		} else {
			resultMap.put("result", SUCCESS);
		}
	}

	/**
	 * 普通人员店铺充值豆
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void addGoldByShop(Map<String, String> params, Map<String, Object> resultMap) {
		int userId = 0;
		int gold = 0;
		String orderSep = params.get("orderId");
		try {
			userId = Integer.parseInt(params.get("userID"));
			gold = Integer.parseInt(params.get("gold"));
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数有误");
			return;
		}
		CenterRMIServerImpl centerRMIServer = SpringService.getBean(CenterRMIServerImpl.class);
		AddGoldResultModel addGoldResultModel = centerRMIServer.addAccountGold(userId, gold, false, orderSep + " 店铺充值",
				EGoldOperateType.SHOP_RECHARGE);
		if (addGoldResultModel.isSuccess()) {
			resultMap.put("result", SUCCESS);
		} else {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "系统维护中，请稍后再试");
		}
	}

	/**
	 * 系统状态
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void getSystemState(Map<String, String> params, Map<String, Object> resultMap) {
		GlobalModel globalModel = PublicServiceImpl.getInstance().getGlobalModel();
		if (!globalModel.isSystemStopReady()) {
			resultMap.put("result", SUCCESS);
		} else {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "系统维护中，请稍后再试");
		}
	}

	/**
	 * 碰胡充值成为代理
	 * 
	 * @param request
	 * @param params
	 */
	private void phPay(Map<String, String> params, Map<String, Object> resultMap) {
		// http://localhost:6002/center/oss/pay?queryType=3&centerOrderId=6555442s&productId=5&userId=2
		String centerOrderId = params.get("centerOrderId");
		String userId = params.get("userId");
		String productId = params.get("productId");
		String isAgentPay = params.get("isAgentPay");

		// TODO 验证必填参数格式是否正确 用到哪个验证哪个
		int shopId;
		long account_id = 0;
		int isAgent = 0;
		try {
			shopId = Integer.parseInt(productId);
			account_id = Long.parseLong(userId);
			isAgent = Integer.parseInt(isAgentPay);
		} catch (Exception e) {
			resultMap.put("result", "参数异常");
			return;
		}

		// 找下有木有相同的订单号
		Criteria crieria = new Criteria();
		Query query = Query.query(crieria);
		crieria.and("centerOrderID").is(centerOrderId);
		AddCardLog addCardLog = SpringService.getBean(MongoDBService.class).getMongoTemplate().findOne(query, AddCardLog.class);
		if (addCardLog != null) {
			logger.error("addCardLog订单号已经存在属于重复通知？centerOrderId=" + centerOrderId);
			resultMap.put("result", SUCCESS);
			resultMap.put("remark", "重复通知:" + centerOrderId);
			return;
		}

		ShopModel shop = ShopDict.getInstance().getShopModel(shopId);
		if (shop == null) {
			logger.error("商品不在列表中！！！centerOrderId=" + centerOrderId + "shopID==" + shopId);
			resultMap.put("result", ErrorCode.SHOP_ERROR_FAIL);
			resultMap.put("remark", "商品不在列表中:" + shopId);
			return;
		}

		StringBuffer buffer = new StringBuffer();
		try {
			AddGoldResultModel t = null;
			try {
				ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
				if (shop.getShop_type() == 2) {
					t = centerRMIServer.addAccountGold(account_id, shop.getGold() + shop.getSend_gold(), true, "店铺充值商品:" + shopId,
							EGoldOperateType.SHOP_RECHARGE);// 店铺充值
				} else if (shop.getShop_type() == 3) {
					t = centerRMIServer.addAccountGold(account_id, shop.getGold() + shop.getSend_gold(), true, "推广员代充商品:" + shopId,
							EGoldOperateType.RECOMMEND_RECHARGE);
				} else {
					t = centerRMIServer.addAccountGold(account_id, shop.getGold() + shop.getSend_gold(), true, "中心充值商品:" + shopId,
							EGoldOperateType.SHOP_PAY);// 调用游戏充值
				}

				if (t == null || !t.isSuccess()) {
					resultMap.put("result", ErrorCode.GAME_ERROR_FAIL);
					resultMap.put("remark", "游戏服务器返回充值失败");
					logger.error("充值失败！！？centerOrderId=" + centerOrderId);
					return;
				}
				if (isAgent == 1) {
					RsAccountModelResponse.Builder rsAccountModelResponse = RsAccountModelResponse.newBuilder();
					rsAccountModelResponse.setAccountId(account_id);
					rsAccountModelResponse.setIsAgent(isAgent);
					rsAccountModelResponse.setProxyLevel(isAgent);
					centerRMIServer.ossModifyAccountModel(rsAccountModelResponse.build());
				}

			} catch (Exception e) {
				resultMap.put("result", ErrorCode.GAME_NET_FAIL);
				resultMap.put("remark", "游戏服务器链接异常");
				return;
			}
			Account account = PublicServiceImpl.getInstance().getAccount(account_id);
			// SysParamModel sysParamModel5000 =
			// SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(5000);
			// long gameId = sysParamModel5000.getVal1();
			// 大厅的代理充值返利
			// if (gameId == 6) {
			// try {
			// if (account.getAccountModel().getIs_agent() >= 1) {
			// ICenterRMIServer centerRMIServer =
			// SpringService.getBean(ICenterRMIServer.class);
			// centerRMIServer.doAgentRecommendReceived(account,
			// shop.getPrice());// 调用代理充值返利
			// }
			// } catch (Exception e) {
			// logger.error(account + " centerRMIServer.doAgentRecommendReceived
			// " + shop.getPrice(), e);
			// }
			// } else if (gameId == 7) {
			// try {
			// if (account.getAccountModel().getIs_agent() >= 1) {
			// ICenterRMIServer centerRMIServer =
			// SpringService.getBean(ICenterRMIServer.class);
			// centerRMIServer.doAgentRecommendReceived(account,
			// shop.getPrice());// 调用代理充值返利
			// }
			// } catch (Exception e) {
			// logger.error(account + " centerRMIServer.doAgentRecommendReceived
			// " + shop.getPrice(), e);
			// }
			// } else {
			// try {
			// if (account.getAccountModel().getIs_agent() >= 1 &&
			// shop.getPrice() >= 400) {
			// ICenterRMIServer centerRMIServer =
			// SpringService.getBean(ICenterRMIServer.class);
			// centerRMIServer.doAgentReceived(account, shop.getPrice());//
			// 调用代理充值返利
			// }
			// } catch (Exception e) {
			// logger.error(account + " centerRMIServer.doAgentReceived " +
			// shop.getPrice(), e);
			// }
			// }

			buffer.append("account_id=" + account_id);
			String nickName = "";
			if (account.getAccountWeixinModel() != null) {
				buffer.append(";微信昵称=" + account.getAccountWeixinModel().getNickname());
				nickName = account.getAccountWeixinModel().getNickname();
			}
			buffer.append(";修改的方式=" + "中心购买商品");
			buffer.append(";购买的商品ID=" + shopId);
			buffer.append(";购买的房卡=" + shop.getGold());
			buffer.append(";赠送的房卡=" + shop.getSend_gold());
			buffer.append(";修改的值=" + shopId);
			buffer.append(";收款的帐号=" + "中心");
			// 增加售卡日志
			String orderID = IDGeneratorOrder.getInstance().getWPayUniqueID();
			if (shop.getShop_type() == 2) {
				addCardLog(orderID, account_id, nickName, t.getAccountModel().getIs_agent(), ESellType.SHOP_PAY_CARD.getId(), shopId, shop.getGold(),
						shop.getSend_gold(), shop.getPrice(), 0, ACCOUNT_SHOP_NAME, ACCOUNT_SHOP_NAME, "", ACCOUNT_SHOP_NAME, centerOrderId);
			} else if (shop.getShop_type() == 3) {
				addCardLog(orderID, account_id, nickName, t.getAccountModel().getIs_agent(), ESellType.SHOP_PAY_CARD.getId(), shopId, shop.getGold(),
						shop.getSend_gold(), shop.getPrice(), 0, ACCOUNT_RECOMMEND_NAME, ACCOUNT_RECOMMEND_NAME, "", ACCOUNT_RECOMMEND_NAME,
						centerOrderId);
			} else {
				addCardLog(orderID, account_id, nickName, t.getAccountModel().getIs_agent(), ESellType.CENTER_PAY_CARD.getId(), shopId,
						shop.getGold(), shop.getSend_gold(), shop.getPrice(), 0, ACCOUNT_NAME, ACCOUNT_NAME, "", ACCOUNT_NAME, centerOrderId);
			}
			// oss管理员操作日志
			buffer.append(";流水订单=" + orderID);
			buffer.append(";中心流水订单=" + centerOrderId);
			resultMap.put("result", SUCCESS);
			resultMap.put("orderID", orderID);
			resultMap.put("remark", "成功");
			logger.warn(buffer.toString());
		} catch (Exception e) {
			logger.error("充值失败！！？centerOrderId=" + centerOrderId, e);
			return;
		}
	}

	/**
	 * 充值请求
	 * 
	 * @param request
	 * @param params
	 */
	private void pay(Map<String, String> params, Map<String, Object> resultMap) {
		// http://localhost:6002/center/oss/pay?queryType=3&centerOrderId=6555442s&productId=5&userId=2
		String centerOrderId = params.get("centerOrderId");
		String userId = params.get("userId");
		String time = params.get("time");
		String productId = params.get("productId");
		String sign = params.get("sign");
		String isAgentPay = params.get("isAgentPay");

		// TODO 验证必填参数格式是否正确 用到哪个验证哪个
		int shopId;
		long account_id = 0;
		int isAgent = 0;
		try {
			shopId = Integer.parseInt(productId);
			account_id = Long.parseLong(userId);
			isAgent = Integer.parseInt(isAgentPay);
		} catch (Exception e) {
			resultMap.put("result", "参数异常");
			return;
		}

		// 找下有木有相同的订单号
		Criteria crieria = new Criteria();
		Query query = Query.query(crieria);
		crieria.and("centerOrderID").is(centerOrderId);
		AddCardLog addCardLog = SpringService.getBean(MongoDBService.class).getMongoTemplate().findOne(query, AddCardLog.class);
		if (addCardLog != null) {
			logger.error("addCardLog订单号已经存在属于重复通知？centerOrderId=" + centerOrderId);
			resultMap.put("result", SUCCESS);
			resultMap.put("remark", "重复通知:" + centerOrderId);
			return;
		}

		ShopModel shop = ShopDict.getInstance().getShopModel(shopId);
		if (shop == null) {
			logger.error("商品不在列表中！！！centerOrderId=" + centerOrderId + "shopID==" + shopId);
			resultMap.put("result", ErrorCode.SHOP_ERROR_FAIL);
			resultMap.put("remark", "商品不在列表中:" + shopId);
			return;
		}

		StringBuffer buffer = new StringBuffer();
		try {
			AddGoldResultModel t = null;
			try {
				ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
				if (shop.getShop_type() == 2) {
					t = centerRMIServer.addAccountGold(account_id, shop.getGold() + shop.getSend_gold(), true, "店铺充值商品:" + shopId,
							EGoldOperateType.SHOP_RECHARGE);// 店铺充值
					if (isAgent > 0) {
						// 自助开通代理
						BonusPointsService.getInstance().rechargeSendBonusPoints(account_id, shop.getPrice(), EBonusPointsType.SELF_OPENAGENT_BP);
					} else if (shop.getPrice() >= 30000) {
						BonusPointsService.getInstance().rechargeSendBonusPoints(account_id, shop.getPrice(), EBonusPointsType.RECHARGE_SEND_BP);
					}
				} else if (shop.getShop_type() == 3) {
					t = centerRMIServer.addAccountGold(account_id, shop.getGold() + shop.getSend_gold(), true, "推广员代充商品:" + shopId,
							EGoldOperateType.RECOMMEND_RECHARGE);
					// if (isAgent > 0) {
					// 推广员开通代理
					BonusPointsService.getInstance().rechargeSendBonusPoints(account_id, shop.getPrice(), EBonusPointsType.PROMOTER_OPENAGENT_BP);
					// }
				} else {
					t = centerRMIServer.addAccountGold(account_id, shop.getGold() + shop.getSend_gold(), true, "中心充值商品:" + shopId,
							EGoldOperateType.SHOP_PAY);// 调用游戏充值
				}

				if (t == null || !t.isSuccess()) {
					resultMap.put("result", ErrorCode.GAME_ERROR_FAIL);
					resultMap.put("remark", "游戏服务器返回充值失败");
					logger.error("充值失败！！？centerOrderId=" + centerOrderId);
					return;
				}
				if (isAgent == 1) {
					RsAccountModelResponse.Builder rsAccountModelResponse = RsAccountModelResponse.newBuilder();
					rsAccountModelResponse.setAccountId(account_id);
					rsAccountModelResponse.setIsAgent(isAgent);
					centerRMIServer.ossModifyAccountModel(rsAccountModelResponse.build());
				}

			} catch (Exception e) {
				resultMap.put("result", ErrorCode.GAME_NET_FAIL);
				resultMap.put("remark", "游戏服务器链接异常");
				return;
			}
			Account account = PublicServiceImpl.getInstance().getAccount(account_id);
			SysParamModel sysParamModel5000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(5000);
			long gameId = sysParamModel5000.getVal1();
			// 大厅的代理充值返利
			if (gameId == 6) {
				ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
				try {
					Map<String, String> map = new HashMap<String, String>();
					map.put("accountId", account_id + "");
					map.put("money", shop.getPrice() + "");
					if (shop.getShop_type() == 2) {
						if (isAgent > 0) {
							map.put("rechargeType", EBonusPointsType.SELF_OPENAGENT_BP.getId() + "");
						} else if (shop.getPrice() >= 30000) {
							map.put("rechargeType", EBonusPointsType.RECHARGE_SEND_BP.getId() + "");
						}
					} else if (shop.getShop_type() == 3) {
						map.put("rechargeType", EBonusPointsType.PROMOTER_OPENAGENT_BP.getId() + "");
					}
					centerRMIServer.rmiInvoke(RMICmd.RECHARGE_TASK, map);
				} catch (Exception e) {
					logger.error("调用充值任务失败", e);
				}
				try {
					logger.info(account_id + " 充值");
					AccountZZPromoterModel am = ZZPromoterService.getInstance().getAccountZZPromoterModel(account_id);
					if (am != null && am.getAccount_id() > 0) {
						logger.info(centerOrderId + JSON.toJSONString(am));
						ZZPromoterService.getInstance().recharge(3, shop.getPrice(), am.getAccount_id(), account_id, centerOrderId);
					}
					if (am == null) {
						try {
							if (account.getAccountModel().getIs_agent() >= 1) {
								SysParamModel sysParamModel2251 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2251);
								if (sysParamModel2251 != null && sysParamModel2251.getVal4() == 1) {
									SysParamModel sysParamModel2224 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6)
											.get(2224);
									int limitMoney = 20000;
									if (sysParamModel2224 != null && sysParamModel2224.getVal4() > 0) {
										limitMoney = sysParamModel2224.getVal4();
									}
									if (shop.getPrice() >= limitMoney) {
										Map<String, String> map = new HashMap<String, String>();
										map.put("accountId", account_id + "");
										map.put("money", shop.getPrice() + "");
										centerRMIServer.rmiInvoke(RMICmd.RECHARGE_RECEIVE, map);
									}

								} else {
									centerRMIServer.doAgentRecommendReceived(account, shop.getPrice());// 调用代理充值返利
								}
							}
						} catch (Exception e) {
							logger.error(account + " centerRMIServer.doAgentRecommendReceived " + shop.getPrice(), e);
						}
					}
				} catch (Exception e) {
					logger.error("麻将协会推广用户充值返利失败", e);
				}

			} else if (gameId == 7) {
				try {
					if (account.getAccountModel().getIs_agent() >= 1) {
						ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
						centerRMIServer.doAgentRecommendReceived(account, shop.getPrice());// 调用代理充值返利
					}
				} catch (Exception e) {
					logger.error(account + " centerRMIServer.doAgentRecommendReceived " + shop.getPrice(), e);
				}
			}
			buffer.append("account_id=" + account_id);
			String nickName = "";
			if (account.getAccountWeixinModel() != null) {
				buffer.append(";微信昵称=" + account.getAccountWeixinModel().getNickname());
				nickName = account.getAccountWeixinModel().getNickname();
			}
			buffer.append(";修改的方式=" + "中心购买商品");
			buffer.append(";购买的商品ID=" + shopId);
			buffer.append(";购买的房卡=" + shop.getGold());
			buffer.append(";赠送的房卡=" + shop.getSend_gold());
			buffer.append(";修改的值=" + shopId);
			buffer.append(";收款的帐号=" + "中心");
			// 增加售卡日志
			String orderID = IDGeneratorOrder.getInstance().getWPayUniqueID();
			if (shop.getShop_type() == 2) {
				int id = ESellType.SHOP_PAY_CARD.getId();
				if (shop.getGold() >= 3000) {
					id = ESellType.SHOP_AGENT_BUY_CARD.getId();
				}
				addCardLog(orderID, account_id, nickName, t.getAccountModel().getIs_agent(), id, shopId, shop.getGold(), shop.getSend_gold(),
						shop.getPrice(), 0, ACCOUNT_SHOP_NAME, ACCOUNT_SHOP_NAME, "", ACCOUNT_SHOP_NAME, centerOrderId);
			} else if (shop.getShop_type() == 3) {
				addCardLog(orderID, account_id, nickName, t.getAccountModel().getIs_agent(), ESellType.RECOMMEND_BUY_CARD.getId(), shopId,
						shop.getGold(), shop.getSend_gold(), shop.getPrice(), 0, ACCOUNT_RECOMMEND_NAME, ACCOUNT_RECOMMEND_NAME, "",
						ACCOUNT_RECOMMEND_NAME, centerOrderId);
			} else {
				addCardLog(orderID, account_id, nickName, t.getAccountModel().getIs_agent(), ESellType.CENTER_PAY_CARD.getId(), shopId,
						shop.getGold(), shop.getSend_gold(), shop.getPrice(), 0, ACCOUNT_NAME, ACCOUNT_NAME, "", ACCOUNT_NAME, centerOrderId);
			}
			// oss管理员操作日志
			buffer.append(";流水订单=" + orderID);
			buffer.append(";中心流水订单=" + centerOrderId);
			resultMap.put("result", SUCCESS);
			resultMap.put("orderID", orderID);
			resultMap.put("remark", "成功");
			logger.warn(buffer.toString());
			SysParamModel sysParamModel2251 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2251);
			if (isAgent == 1 && sysParamModel2251 != null && sysParamModel2251.getVal4() == 1) {
				log_update_record(account_id, 0, 0, "升级为见习推广员");
			}
		} catch (Exception e) {
			logger.error("充值失败！！？centerOrderId=" + centerOrderId, e);
			return;
		}
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
	 * 房卡出售记录
	 * 
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
	 */
	private void addCardLog(String orderID, long accountId, String nickname, int accountType, int sellType, int shopId, int cardNum, int sendNum,
			int rmb, int cashAccountID, String cashAccountName, String remark, String ossID, String ossName, String centerOrderID) {
		try {
			AddCardLog addcardlog = new AddCardLog();
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
			addcardlog.setSellType(sellType);
			addcardlog.setSendNum(sendNum);
			addcardlog.setShopId(shopId);
			addcardlog.setOssID(ossID);
			addcardlog.setOssName(ossName);
			addcardlog.setCenterOrderID(centerOrderID);
			SpringService.getBean(MongoDBService.class).getMongoTemplate().insert(addcardlog);
			PublicService publicService = SpringService.getBean(PublicService.class);
			publicService.getPublicDAO().insertAddCard(addcardlog);
		} catch (Exception e) {
			logger.error("addcardLog插入日志异常" + e);
		}
	}

	/**
	 * 推广员------根据用户ID查询推广员相关的首页信息
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
		if (account.getAccountWeixinModel() != null) {
			resultMap.put("nickName", account.getAccountWeixinModel().getNickname());
		} else {
			resultMap.put("nickName", "-");
		}
		resultMap.put("canRMB", canRMB);// 推广员可提现
		resultMap.put("promoteLevel", account.getAccountModel().getRecommend_level());// 推广员等级
		resultMap.put("gold", account.getAccountModel().getGold());// 闲逸豆

		long sum = MongoDBServiceImpl.getInstance().getYesterdayIncome(userID);// 获取到的是分为单位
		double yesterdayRMB = sum / 100.00;
		resultMap.put("yesterdayRMB", yesterdayRMB);// 推广员 昨日收益

	}

	/**
	 * 推广员收益记录
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void doRecommendIncome(Map<String, String> params, Map<String, Object> resultMap) {
		Date startDate = null;
		Date endDate = null;

		String pageIndex = params.get("pageIndex");
		String pageSize = params.get("pageSize");
		int newPageIndex = 0;
		int newPageSize = 0;

		try {
			String targetDateStr = params.get("targetDate");

			if (pageIndex == null || pageSize == null) {
				newPageIndex = 1;
				newPageSize = 5;
			} else {
				newPageIndex = Integer.valueOf(pageIndex);
				newPageSize = Integer.valueOf(pageSize);
			}

			startDate = MyDateUtil.getMinMonthDate(targetDateStr);
			endDate = MyDateUtil.getMaxMonthDate(targetDateStr);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		String account_id = params.get("userID");
		int type = 0;
		try {
			type = Integer.parseInt(params.get("type"));
		} catch (Exception e) {
		}
		resultMap.put("result", SUCCESS);
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("account_id").is(Long.parseLong(account_id)));
		query.addCriteria(Criteria.where("log_type").is(ELogType.recommendIncome));
		query.addCriteria(Criteria.where("create_time").lt(MyDateUtil.getTomorrowZeroDate(endDate)).gte(MyDateUtil.getZeroDate(startDate)))
				.skip(newPageIndex * newPageSize).limit(newPageSize);
		// type类型，1查自己获利，2查一级获利，3查二级获利
		// V2 每个值代表的 0，自己推荐的，2，一级代理推荐，3，二级代理推荐,
		// 4,总推广员的代理充值返利，5，一级推广员的代理充值返利，6二级推广员的代理充值返现
		BasicDBList values = new BasicDBList();
		if (type == 1) {
			values.add(0);
			values.add(4);
			query.addCriteria(Criteria.where("v2").in(values));
		} else if (type == 2) {
			values.add(2);
			values.add(5);
			query.addCriteria(Criteria.where("v2").in(values));
		} else if (type == 3) {
			values.add(3);
			values.add(6);
			query.addCriteria(Criteria.where("v2").in(values));
		}
		query.with(new Sort(Direction.DESC, "create_time"));
		List<GameRecommend> recommendIncomeList = mongoDBService.getMongoTemplate().find(query, GameRecommend.class);

		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Map<String, Object> map = null;
		// Double totalBalance = 0.00;
		for (GameRecommend model : recommendIncomeList) {
			map = Maps.newConcurrentMap();
			map.put("accountId", model.getAccount_id());
			map.put("getBalance", model.getV1() / 100.00);
			map.put("comeSource", model.getV2());
			map.put("activity", model.getMsg().split("\\|")[0]);
			SimpleDateFormat timeStr = new SimpleDateFormat("MM-dd");
			String newTimeStr = timeStr.format(model.getCreate_time());
			map.put("createTime", newTimeStr);
			// totalBalance += model.getV1();
			list.add(map);
		}

		// 不考虑分页算这个月的数据
		// Query queryTotal = new Query();
		// queryTotal.addCriteria(Criteria.where("account_id").is(Long.parseLong(account_id)));
		// queryTotal.addCriteria(Criteria.where("log_type").is(ELogType.recommendOutcome));
		// queryTotal.addCriteria(Criteria.where("create_time").lt(MyDateUtil.getTomorrowZeroDate(endDate)).gte(MyDateUtil.getZeroDate(startDate)));
		// List<GameRecommend> MonthList =
		// mongoDBService.getMongoTemplate().find(queryTotal,
		// GameRecommend.class);
		// Double totalMonthBalance = 0.00;
		// for (GameRecommend model : MonthList) {
		// totalMonthBalance += model.getV1();
		// }
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		Account account = centerRMIServer.getAccount(Long.parseLong(account_id));// 调用代理充值返利
		resultMap.put("totalBalance", account.getAccountModel().getRecommend_history_income());// 历史收益
		// resultMap.put("totalMonthBalance", totalMonthBalance / 100);
		resultMap.put("result", SUCCESS);
		resultMap.put("data", list);
	}

	/**
	 * 推广员日常记录
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void doRecommendDailyDetail(Map<String, String> params, Map<String, Object> resultMap) {
		Date startDate = null;
		Date endDate = null;

		String pageIndex = params.get("pageIndex");
		String pageSize = params.get("pageSize");
		int newPageIndex = 0;
		int newPageSize = 0;

		try {

			String startTime = params.get("startTime");
			String endTime = params.get("endTime");

			if (pageIndex == null || pageSize == null) {
				newPageIndex = 1;
				newPageSize = 5;
			} else {
				newPageIndex = Integer.valueOf(pageIndex);
				newPageSize = Integer.valueOf(pageSize);
			}
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
			startDate = dateFormat.parse(startTime);
			endDate = dateFormat.parse(endTime);
			// startDate = MyDateUtil.getMinMonthDate(targetDateStr);
			// endDate = MyDateUtil.getMaxMonthDate(targetDateStr);
		} catch (Exception e) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "参数异常");
			return;
		}
		String account_id = params.get("userID");
		int type = 0;
		try {
			type = Integer.parseInt(params.get("type"));
		} catch (Exception e) {
		}
		resultMap.put("result", SUCCESS);
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("account_id").is(Long.parseLong(account_id)));
		query.addCriteria(Criteria.where("log_type").is(ELogType.recommendIncome));
		query.addCriteria(Criteria.where("create_time").lt(MyDateUtil.getTomorrowZeroDate(endDate)).gte(MyDateUtil.getZeroDate(startDate)))
				.skip(newPageIndex * newPageSize).limit(newPageSize);
		// type类型，1查自己获利，2查一级获利，3查二级获利
		// V2 每个值代表的 0，自己推荐的，2，一级代理推荐，3，二级代理推荐,
		// 4,总推广员的代理充值返利，5，一级推广员的代理充值返利，6二级推广员的代理充值返现
		BasicDBList values = new BasicDBList();
		if (type == 1) {
			values.add(0);
			values.add(4);
			query.addCriteria(Criteria.where("v2").in(values));
		} else if (type == 2) {
			values.add(2);
			values.add(5);
			query.addCriteria(Criteria.where("v2").in(values));
		} else if (type == 3) {
			values.add(3);
			values.add(6);
			query.addCriteria(Criteria.where("v2").in(values));
		}
		query.with(new Sort(Direction.DESC, "create_time"));
		List<GameRecommend> recommendIncomeList = mongoDBService.getMongoTemplate().find(query, GameRecommend.class);

		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Map<String, Object> map = null;
		for (GameRecommend model : recommendIncomeList) {
			map = Maps.newConcurrentMap();
			map.put("accountId", model.getAccount_id());
			map.put("getBalance", model.getV1() / 100.00);
			map.put("comeSource", model.getV2());
			map.put("activity", model.getMsg().split("\\|")[0]);
			if (model.getTarget_id() > 0) {
				map.put("targetId", model.getTarget_id());
				CenterRMIServerImpl centerRMIServer = SpringService.getBean(CenterRMIServerImpl.class);
				Account targetAccount = centerRMIServer.getAccount(model.getTarget_id());
				AccountModel targetModel = targetAccount.getAccountModel();
				if (targetAccount.getAccountWeixinModel() != null) {
					map.put("nickName", targetAccount.getAccountWeixinModel().getNickname());
				} else {
					map.put("nickName", "-");
				}
				// V2 每个值代表的 0，自己推荐的，2，一级代理推荐，3，二级代理推荐,
				// 4,总推广员的代理充值返利，5，一级推广员的代理充值返利，6二级推广员的代理充值返现
				if (model.getV2() == 0) {
					map.put("sourceId", "自己");
				} else if (model.getV2() == 1) {
					map.put("sourceId", "自己");
				} else if (model.getV2() == 2) {
					map.put("sourceId", "一级推广" + targetModel.getRecommend_id());
				} else if (model.getV2() == 3) {
					map.put("sourceId", "二级推广" + targetModel.getRecommend_id());
				} else if (model.getV2() == 4) {
					map.put("sourceId", "自己");
				} else if (model.getV2() == 5) {
					map.put("sourceId", "一级代理" + targetModel.getRecommend_id());
				} else if (model.getV2() == 6) {
					map.put("sourceId", "二级代理" + targetModel.getRecommend_id());
				}
			}
			SimpleDateFormat timeStr = new SimpleDateFormat("MM-dd");
			String newTimeStr = timeStr.format(model.getCreate_time());
			map.put("createTime", newTimeStr);
			list.add(map);
		}

		// 不考虑分页算这个月的数据
		// Query queryTotal = new Query();
		// queryTotal.addCriteria(Criteria.where("account_id").is(Long.parseLong(account_id)));
		// queryTotal.addCriteria(Criteria.where("log_type").is(ELogType.recommendOutcome));
		// queryTotal.addCriteria(Criteria.where("create_time").lt(MyDateUtil.getTomorrowZeroDate(endDate)).gte(MyDateUtil.getZeroDate(startDate)));
		// List<GameRecommend> MonthList =
		// mongoDBService.getMongoTemplate().find(queryTotal,
		// GameRecommend.class);
		// Double totalMonthBalance = 0.00;
		// for (GameRecommend model : MonthList) {
		// totalMonthBalance += model.getV1();
		// }

		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		Account account = centerRMIServer.getAccount(Long.parseLong(account_id));// 调用代理充值返利
		resultMap.put("totalBalance", account.getAccountModel().getRecommend_history_income());// 历史收益
		// resultMap.put("totalMonthBalance", totalMonthBalance / 100);
		resultMap.put("result", SUCCESS);
		resultMap.put("data", list);
	}

	/**
	 * 推广员提现记录
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void doRecommendOutcome(Map<String, String> params, Map<String, Object> resultMap) {
		Date startDate = null;
		Date endDate = null;

		String pageIndex = params.get("pageIndex");
		String pageSize = params.get("pageSize");
		int newPageIndex = 1;
		int newPageSize = 5;

		try {
			String startTime = params.get("startTime");
			String endTime = params.get("endTime");

			if (pageIndex == null || pageSize == null) {
				newPageIndex = 1;
				newPageSize = 5;
			} else {
				newPageIndex = Integer.valueOf(pageIndex);
				newPageSize = Integer.valueOf(pageSize);
			}

			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
			startDate = dateFormat.parse(startTime);
			endDate = dateFormat.parse(endTime);
			// startDate = MyDateUtil.getMinMonthDate(targetDateStr);
			// endDate = MyDateUtil.getMaxMonthDate(targetDateStr);
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
		query.addCriteria(Criteria.where("log_type").is(ELogType.recommendOutcome));
		query.addCriteria(Criteria.where("create_time").lt(MyDateUtil.getTomorrowZeroDate(endDate)).gte(MyDateUtil.getZeroDate(startDate)));
		query.with(new Sort(Direction.DESC, "create_time")).skip(newPageIndex * newPageSize).limit(newPageSize);
		List<GameRecommend> recommendIncomeList = mongoDBService.getMongoTemplate().find(query, GameRecommend.class);

		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Map<String, Object> map = null;
		for (GameRecommend model : recommendIncomeList) {
			map = Maps.newConcurrentMap();
			Account account = PublicServiceImpl.getInstance().getAccount(model.getAccount_id());
			if (account.getAccountWeixinModel() != null) {
				String nickName = account.getAccountWeixinModel().getNickname();
				map.put("nickName", nickName);
			} else {
				map.put("nickName", "闲逸玩家");
			}
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

		// // 不考虑分页算这个月的数据
		// Query queryTotal = new Query();
		// queryTotal.addCriteria(Criteria.where("account_id").is(Long.parseLong(account_id)));
		// queryTotal.addCriteria(Criteria.where("log_type").is(ELogType.recommendOutcome));
		// queryTotal.addCriteria(Criteria.where("create_time").lt(MyDateUtil.getTomorrowZeroDate(endDate)).gte(MyDateUtil.getZeroDate(startDate)));
		// List<GameRecommend> MonthList =
		// mongoDBService.getMongoTemplate().find(queryTotal,
		// GameRecommend.class);
		// Double totalMonthBalance = 0.00;
		// for (GameRecommend model : MonthList) {
		// totalMonthBalance += model.getV1();
		// }
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		Account account = centerRMIServer.getAccount(Long.parseLong(account_id));// 调用代理充值返利
		resultMap.put("totalBalance", account.getAccountModel().getRecommend_receive_income());// 提现总额
		resultMap.put("result", SUCCESS);
		// resultMap.put("totalMonthBalance", totalMonthBalance / 100);
		resultMap.put("data", list);
	}

	/**
	 * 推广员----我要提现(只作判断，能不能提现)
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
	 * 推广员----我要提现（既要判断，还要作操作入库的）
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void myWithdraws(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		Double income = Double.parseDouble(params.get("income"));// 提现金额,假设传过来的是正数
		String desc = "推广员提现的金额";// 描述
		EGoldOperateType eGoldOperateType = EGoldOperateType.REAL_INCOME_RECEIVER;// 推广员提现

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
		AddGoldResultModel resultModel = centerRMIServer.doRecommendIncome(userID, income, 0l, desc, eGoldOperateType, userID);

		DecimalFormat df = new DecimalFormat("######0.00");
		String canRMB = df.format(resultModel.getAccountModel().getRecommend_remain_income());
		if (!resultModel.isSuccess()) {
			resultMap.put("result", FAIL);// 0表示成功，-1表示失败
		} else {
			resultMap.put("result", SUCCESS);// 0表示成功，-1表示失败
		}
		resultMap.put("msg", resultModel.getMsg());// 返回到类型的消息
		resultMap.put("canRMB", canRMB);// 更新后推广员可提现金额
		resultMap.put("yesterdayRMB", resultModel.getAccountModel().getRecommend_yesterday_income());// 更新后推广员昨日收益
		resultMap.put("gold", resultModel.getAccountModel().getGold());// 更新后推广员闲逸豆

	}

	/**
	 * 推广员----我的会员玩家
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void myMemberPlayer(Map<String, String> params, Map<String, Object> resultMap) {

		String account_id = params.get("userID");
		String targetDateStr = params.get("targetDate");
		try {
			Map<String, Object> detailMap = SpringService.getBean(CenterRMIServerImpl.class).queryMyMemberByMonth(Long.parseLong(account_id),
					targetDateStr);

			if (detailMap != null) {
				resultMap.put("totalCount", detailMap.get("totalCount"));
				resultMap.put("details", detailMap.get("details"));
			}
		} catch (Exception e) {
			logger.error("myMemberPlayer插入日志异常" + e);
		}
		resultMap.put("result", SUCCESS);

	}

	/**
	 * 推广员----我的会员代理
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void myMemberProxy(Map<String, String> params, Map<String, Object> resultMap) {

		String account_id = params.get("userID");
		String targetDateStr = params.get("targetDate");
		try {
			Map<String, Object> detailMap = SpringService.getBean(CenterRMIServerImpl.class).queryMyProxyByMonth(Long.parseLong(account_id),
					targetDateStr);
			if (detailMap != null) {
				resultMap.put("totalCount", detailMap.get("totalCount"));
				resultMap.put("details", detailMap.get("details"));
				// resultMap.put("data",detailMap);
			}
		} catch (Exception e) {
			logger.error("myMemberProxy插入日志异常" + e);
		}
		resultMap.put("result", SUCCESS);

	}

	/**
	 * 推广员信息总览
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void doRecommendAll(Map<String, String> params, Map<String, Object> resultMap) {
		String account_id = params.get("userID");
		resultMap.put("result", SUCCESS);
		try {
			Map<String, Object> detailMap = SpringService.getBean(CenterRMIServerImpl.class).queryRecommendAll(Long.parseLong(account_id));
			if (detailMap != null) {
				resultMap.put("data", detailMap);
			}
		} catch (Exception e) {
			logger.error("doRecommendAll插入日志异常" + e);
		}

	}

	/**
	 * 下级推广员详情
	 * 
	 * @param params
	 * @param resultMap
	 */
	private void doDownRecommendDetail(Map<String, String> params, Map<String, Object> resultMap) {

		String account_id = params.get("userID");
		String startTime = params.get("startTime");
		String endTime = params.get("endTime");
		resultMap.put("result", SUCCESS);
		try {
			Map<String, Object> detailMap = SpringService.getBean(CenterRMIServerImpl.class).queryDownRecommendByMonth(Long.parseLong(account_id),
					startTime, endTime);
			if (detailMap != null) {
				resultMap.put("tatalRecommendCount", detailMap.get("tatalRecommendCount"));
				resultMap.put("data", detailMap.get("data"));
			}
		} catch (Exception e) {
			logger.error("doDownRecommendDetail插入日志异常" + e);
			e.printStackTrace();
		}

	}

	public void initRecommendDetail() {
		// TaskService taskService = SpringService.getBean(TaskService.class);
		// taskService.initRedPacketActiveLog();
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
			targetID = Long.parseLong(target_ID);
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
		Account targetAccount = PublicServiceImpl.getInstance().getAccount(targetID);
		if (targetAccount == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "目标玩家不存在");
			return;
		}
		if (targetAccount.getAccountModel().getRecommend_id() != userID) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "目标玩家不是你的下级代理");
			return;
		}
		RsAccountModelResponse.Builder rsAccountModelResponse = RsAccountModelResponse.newBuilder();
		rsAccountModelResponse.setAccountId(targetID);
		rsAccountModelResponse.setRecommendLevel(type);
		SpringService.getBean(CenterRMIServerImpl.class).ossModifyAccountModel(rsAccountModelResponse.build());

		resultMap.put("result", SUCCESS);
	}

	private void getYesterdayFlsAgentTj(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		String targetDateStr = null;
		// 验证必填参数格式
		long userID;
		try {
			userID = Long.parseLong(user_ID);
			targetDateStr = params.get("targetDate");
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
			resultMap.put("msg", "非代理不能统计消耗");
			return;
		}
		Date now = MyDateUtil.parse(targetDateStr);
		MongoDBServiceImpl mongoDBServiceImpl = MongoDBServiceImpl.getInstance();
		resultMap.put("agentOpenRoomNum", mongoDBServiceImpl.getProxyOpenRoomNumByDay(userID, now));
		resultMap.put("giveNum", mongoDBServiceImpl.getProxyGiveNum(userID, now));
		resultMap.put("robotOpenRoomNum", mongoDBServiceImpl.getTotalRobotGold(userID, now));
		resultMap.put("result", SUCCESS);
	}

	private void getFlsAgentTjByMonth(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		String target_user_ID = StringUtils.isBlank(params.get("targetUserID")) ? "0" : params.get("targetUserID");
		String targetDateStr = null;
		// int type = 0;
		// 验证必填参数格式
		long userID;
		long targetUserID;
		try {
			userID = Long.parseLong(user_ID);
			targetUserID = Long.parseLong(target_user_ID);
			targetDateStr = params.get("targetDate");
			// type = Integer.parseInt(params.get("type"));
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
		// int totalconsume = account.getAccountModel().getConsum_total();
		if (account.getAccountModel().getIs_agent() == 0) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "非代理不能统计消耗");
			return;
		}
		long id = 0;
		if (targetUserID == 0) {
			id = userID;
		} else {
			if (!account.getAccountProxyModelMap().containsKey(targetUserID)) {
				resultMap.put("result", FAIL);
				resultMap.put("msg", "无权查看用户信息，非用户下级代理");
				return;
			}
			id = targetUserID;
		}
		Date now = MyDateUtil.parse(targetDateStr);
		Calendar calendar = new GregorianCalendar();
		Date start = null;
		Date end = null;
		// if(type==0){//按月
		// calendar.add(Calendar.MONTH, 0);
		// calendar.set(Calendar.DAY_OF_MONTH, 1);
		// start = calendar.getTime();
		// end = new Date();
		// }else{
		calendar.setTime(now);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		start = calendar.getTime();
		calendar.add(Calendar.MONTH, 1);
		calendar.set(Calendar.DAY_OF_MONTH, 0);
		end = calendar.getTime();
		// }
		MongoDBServiceImpl mongoDBServiceImpl = MongoDBServiceImpl.getInstance();
		long totalOpen = mongoDBServiceImpl.getProxyOpenRoomNumByDate(id, start, end);
		long robotOpen = mongoDBServiceImpl.getRobotOpenRoomByDate(id, start, end);
		long giveNum = mongoDBServiceImpl.getProxyGiveNumByDate(id, start, end);
		if ((totalOpen - robotOpen) < 0) {
			resultMap.put("agentOpenRoomNum", 0);
		} else {
			resultMap.put("agentOpenRoomNum", totalOpen - robotOpen);
		}
		resultMap.put("giveNum", giveNum);
		resultMap.put("robotOpenRoomNum", robotOpen);
		resultMap.put("donateNum", mongoDBServiceImpl.getDenateGoldByDate(id, start, end));
		resultMap.put("total", robotOpen + giveNum);
		// resultMap.put("downAgentConsume",
		// mongoDBServiceImpl.getDownAgentCumsumeGoldByDate(userID, start,
		// end));
		resultMap.put("result", SUCCESS);
	}

	private void getFlsAgentTjBySeason(Map<String, String> params, Map<String, Object> resultMap) {
		String user_ID = params.get("userID");
		String target_user_ID = StringUtils.isBlank(params.get("targetUserID")) ? "0" : params.get("targetUserID");
		String targetDateStr = null;
		// int type = 0;
		// 验证必填参数格式
		long userID;
		long targetUserID;
		try {
			userID = Long.parseLong(user_ID);
			targetUserID = Long.parseLong(target_user_ID);
			targetDateStr = params.get("targetDate");
			// type = Integer.parseInt(params.get("type"));
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
			resultMap.put("msg", "非代理不能统计消耗");
			return;
		}
		long id = 0;
		if (targetUserID == 0) {
			id = userID;
		} else {
			if (!account.getAccountProxyModelMap().containsKey(targetUserID)) {
				resultMap.put("result", FAIL);
				resultMap.put("msg", "无权查看用户信息，非用户下级代理");
				return;
			}
			id = targetUserID;
		}
		Date now = MyDateUtil.parse(targetDateStr);
		Calendar calendar = new GregorianCalendar();
		Date start = null;
		Date end = null;
		// if(type==0){//按月
		// int month = getQuarterInMonth(calendar.get(Calendar.MONTH), true);
		// calendar.set(Calendar.MONTH, month);
		// calendar.set(Calendar.DAY_OF_MONTH, 1);
		// start = calendar.getTime();
		// end = new Date();
		// }else{
		calendar.setTime(now);
		int month = getQuarterInMonth(calendar.get(Calendar.MONTH), false);
		calendar.set(Calendar.MONTH, month);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		start = calendar.getTime();
		calendar.set(Calendar.MONTH, month + 3);
		calendar.set(Calendar.DAY_OF_MONTH, 0);
		end = calendar.getTime();
		// }
		MongoDBServiceImpl mongoDBServiceImpl = MongoDBServiceImpl.getInstance();
		long totalOpen = mongoDBServiceImpl.getProxyOpenRoomNumByDate(id, start, end);
		long robotOpen = mongoDBServiceImpl.getRobotOpenRoomByDate(id, start, end);
		long giveNum = mongoDBServiceImpl.getProxyGiveNumByDate(id, start, end);
		if ((totalOpen - robotOpen) < 0) {
			resultMap.put("agentOpenRoomNum", 0);
		} else {
			resultMap.put("agentOpenRoomNum", totalOpen - robotOpen);
		}
		resultMap.put("giveNum", giveNum);
		resultMap.put("robotOpenRoomNum", robotOpen);
		resultMap.put("donateNum", mongoDBServiceImpl.getDenateGoldByDate(id, start, end));
		resultMap.put("total", robotOpen + giveNum);
		// resultMap.put("downAgentConsume",
		// mongoDBServiceImpl.getDownAgentCumsumeGoldByDate(userID, start,
		// end));
		resultMap.put("result", SUCCESS);
	}

	public static int getQuarterInMonth(int month, boolean isQuarterStart) {
		int months[] = { 1, 4, 7, 10 };
		if (!isQuarterStart) {
			months = new int[] { 3, 6, 9, 12 };
		}
		if (month >= 2 && month <= 4)
			return months[0];
		else if (month >= 5 && month <= 7)
			return months[1];
		else if (month >= 8 && month <= 10)
			return months[2];
		else
			return months[3];
	}

	/**
	 * 推广员----我的下线
	 * 
	 * @param params
	 * @param resultMap
	 */
	@SuppressWarnings("unused")
	private void queryDownPlayerByMonth(Map<String, String> params, Map<String, Object> resultMap) {
		String account_id = params.get("targetUserID");
		String startTime = params.get("startTime");
		String endTime = params.get("endTime");
		String type = params.get("type");

		try {
			Map<String, Object> detailMap = SpringService.getBean(CenterRMIServerImpl.class).queryDownPlayerByMonth(Long.parseLong(account_id),
					startTime, endTime, Integer.parseInt(type));
			if (detailMap != null) {
				resultMap.put("totalCount", detailMap.get("totalCount"));
				resultMap.put("details", detailMap.get("details"));
				// resultMap.put("data",detailMap);
			}
		} catch (Exception e) {
			logger.error("queryDownPlayerByMonth插入日志异常" + e);
		}
		resultMap.put("result", SUCCESS);

	}

}
