/**
 * 
 */
package com.cai.http.action;

/**
 * @author xwy
 *
 */
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.define.ESellType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountProxyModel;
import com.cai.common.domain.AccountRecommendModel;
import com.cai.common.domain.AccountRedis;
import com.cai.common.domain.AccountSimple;
import com.cai.common.domain.AddCardLog;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.EveryDayRobotModel;
import com.cai.common.domain.OneProxyAccountReplaceRoomModel;
import com.cai.common.domain.Page;
import com.cai.common.domain.ProxyAccountReplaceRoomModel;
import com.cai.common.domain.ProxyGoldLogModel;
import com.cai.common.domain.RevicerRmbLogModel;
import com.cai.common.domain.RobotModel;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.domain.ShopModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.rmi.ILogicRMIServer;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.IDGeneratorOrder;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.dictionary.ShopDict;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.domain.RobotRoom;
import com.cai.future.runnable.PayCenterRunnable;
import com.cai.http.FastJsonJsonView;
import com.cai.http.model.ErrorCode;
import com.cai.http.model.ProductListInfoResponse.Product;
import com.cai.http.security.SignUtil;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PublicService;
import com.cai.service.PublicServiceImpl;
import com.cai.service.RMIServiceImpl;
import com.cai.service.RedisServiceImpl;
import com.cai.util.MessageResponse;
import com.google.common.collect.Maps;

import javolution.util.FastMap;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountModelResponse;
import protobuf.redis.ProtoRedis.RsAccountProxyResponse;
import protobuf.redis.ProtoRedis.RsAccountResponse;

@Controller
@RequestMapping("/henan")
public class HenanController {

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
	// 删除二级代理
	public static final int TYPE_DELETE_PROXY = 6;
	// 设置为我的二级代理
	public static final int TYPE_SET_PROXY = 7;
	// 备注我的二级代理
	public static final int TYPE_REMARK_PROXY = 8;

	// 根据用户查询基本情况
	public static final int TYPE_USER_DETAIL = 11;

	// 昨日下级代理 消耗 明细
	public static final int TYPE_PROXY_DETAIL = 12;

	// 根据用户查询收益明细
	public static final int TYPE_RMB_DETAIL = 13;

	// 提现明细
	public static final int TYPE_RECEIVE_RMB_DETAIL = 14;
	// 50开始机器人
	// 机器人开房
	public static final int TYPE_OPEN_ROOM = 50;

	// 机器人每日开房详情--根据群ID查
	public static final int TYPE_ROOM_DAY_DETAIL_GROUP = 51;

	// 机器人每日开房详情--用户ID查
	public static final int TYPE_ROOM_DAY_DETAIL_USER = 52;

	// 机器人开房详情--群ID查
	public static final int TYPE_ROOM_DETAIL_GROUP_REALTIME = 53;

	// 获取商品列表
	public static final int TYPE_SHOP = 66;
	// 充值
	public static final int TYPE_PAY = 77;

	// 修复订单
	public static final int TYPE_PAY_REPAIR = 100;

	private static final String ACCOUNT_NAME = "中心充值商品";

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
			turnCard(params, resultMap);
		} else if (type == TYPE_SET_PROXY) {
			// setProxy(params, resultMap);
		} else if (type == TYPE_REMARK_PROXY) {
			// setProxyRemark(params, resultMap);
		} else if (type == TYPE_DELETE_PROXY) {
			// deleteProxy(params, resultMap);
		} else if (type == TYPE_PAY) {
			pay(params, resultMap);
		} else if (type == TYPE_PAY_REPAIR) {
			repairOrder(params, resultMap);
		} else if (type == TYPE_OPEN_ROOM) {
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
		}

		return new ModelAndView(new FastJsonJsonView(), resultMap);

	}

	/**
	 * 获取用户信息
	 * 
	 * @param request
	 * @param params
	 */
	private void openRoom(Map<String, String> params, Map<String, Object> resultMap) {
		String account_ID = params.get("userID");
		String groupID = params.get("groupID");
		if (org.apache.commons.lang.StringUtils.isEmpty(account_ID)
				|| org.apache.commons.lang.StringUtils.isEmpty(groupID)) {
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

		try {
			game_type_index = Integer.parseInt(params.get("game_type_index"));
			game_rule_index = Integer.parseInt(params.get("game_rule_index"));
			game_round = Integer.parseInt(params.get("game_round"));
			accountID = Long.parseLong(account_ID);
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
		int game_id = account.getGame_id();
		game_id = game_id == 0 ? 1 : game_id;

		RobotRoom createRoom = null;
		long now = System.currentTimeMillis();
		if (roomList.size() > 0) {
			Iterator<RobotRoom> it = roomList.iterator();
			while (it.hasNext()) {
				RobotRoom robotroom = it.next();
				if (now - robotroom.createTime >= 60 * 1000 * 10) {
					it.remove();
					continue;
				}
				RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM,
						robotroom.roomID + "", RoomRedisModel.class);
				if (GameConstants.GS_MJ_FREE != roomRedisModel.getRoomStatus()
						&& GameConstants.GS_MJ_WAIT != roomRedisModel.getRoomStatus()) {
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
				break;
			}
		}

		if (createRoom != null) {
			SysParamModel sysParamModel2000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(2000);
			String content = sysParamModel2000.getStr2() + "?game=" + game_id + "&roomid=" + createRoom.roomID;
			resultMap.put("result", SUCCESS);
			resultMap.put("room", createRoom);
			resultMap.put("content", content);
			resultMap.put("gamedesc", GameDescUtil.getGameDesc(createRoom.game_type_index, createRoom.game_rule_index));
			return;
		}

		SysParamModel sysParamModel1000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(1000);
		if (sysParamModel1000.getVal3() == 0 && account.getAccountModel().getIs_inner() == 0) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "服务器停服维护中");
			return;
		}

		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		AccountRedis accountRedis = SpringService.getBean(RedisService.class).hGet(RedisConstant.ACCOUNT_REDIS,
				account.getAccount_id() + "", AccountRedis.class);

		if (!(game_round == 4 || game_round == 8 || game_round == 16)) {
			return;
		}

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
			return;
		}

		// 判断房卡是否免费
		if (sysParamModel != null && sysParamModel.getVal2() == 1) {
			SysParamModel sysParamModel1010 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(1010);
			SysParamModel sysParamModel1011 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(1011);
			SysParamModel sysParamModel1012 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(1012);
			long gold = account.getAccountModel().getGold();
			if (game_round == 4) {
				if (gold < sysParamModel1010.getVal2()) {
					resultMap.put("result", FAIL);
					resultMap.put("msg", "房卡不足!");
					return;
				}
			} else if (game_round == 8) {
				if (gold < sysParamModel1011.getVal2()) {
					resultMap.put("result", FAIL);
					resultMap.put("msg", "房卡不足!");
					return;
				}
			} else if (game_round == 16) {
				if (gold < sysParamModel1012.getVal2()) {
					resultMap.put("result", FAIL);
					resultMap.put("msg", "房卡不足!");
					return;
				}
			}
		}

		// TODO 创建房间号,写入redis 记录是哪个逻辑计算服的

		int room_id = centerRMIServer.randomRoomId(1);// 随机房间号
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
		roomRedisModel.setLogic_index(centerRMIServer.allotLogicId(game_id));// TODO 临时
		// roomRedisModel.getPlayersIdSet().add(session.getAccountID());
		roomRedisModel.setCreate_time(System.currentTimeMillis());
		roomRedisModel.setGame_round(game_round);
		roomRedisModel.setGame_rule_index(game_rule_index);
		roomRedisModel.setGame_type_index(game_type_index);
		roomRedisModel.setGame_id(game_id);
		roomRedisModel.setProxy_room(true);
		roomRedisModel.setCreate_account_id(account.getAccount_id());
		SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM, room_id + "", roomRedisModel);

		// 通知逻辑服
		ILogicRMIServer logicRmiServer = RMIServiceImpl.getInstance().getLogicRMIByIndex(1);
		String nickName = account.getAccountWeixinModel() == null ? "" : account.getAccountWeixinModel().getNickname();
		// boolean success = logicRmiServer.createRobotRoom(accountID, room_id,
		// game_type_index, game_rule_index,
		// game_round, nickName, groupID,createRobotRoom);

		createRoom = new RobotRoom(System.currentTimeMillis(), room_id, accountID, game_type_index, game_rule_index,
				game_round);
		roomList.add(createRoom);
		SysParamModel sysParamModel2000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(2000);

		String content = sysParamModel2000.getStr2() + "?game=" + game_id + "&roomid=" + room_id;
		resultMap.put("room", createRoom);
		resultMap.put("result", SUCCESS);
		resultMap.put("content", content);
		resultMap.put("gamedesc", GameDescUtil.getGameDesc(createRoom.game_type_index, createRoom.game_rule_index));
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
			// curPage = Integer.parseInt(params.get("cur_page"));
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
		// Page page = new Page(curPage, 10, totalSize);
		List<RevicerRmbLogModel> revicerRMbModelList = MongoDBServiceImpl.getInstance().getRevicerRmbLogModelList(null,
				userID);

		resultMap.put("result", SUCCESS);
		// resultMap.put("curPage", curPage);
		// resultMap.put("pageSize", 10);
		// resultMap.put("totalSize", totalSize);
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
			// curPage = Integer.parseInt(params.get("cur_page"));
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

		// int totalSize =
		// MongoDBServiceImpl.getInstance().getOneProxyAccountReplaceRoomModelCount(userID);
		// Page page = new Page(curPage, 10, totalSize);
		List<OneProxyAccountReplaceRoomModel> oneProxyAccountReplaceRoomModelList = MongoDBServiceImpl.getInstance()
				.getOneProxyAccountReplaceRoomModelList(null, userID);

		resultMap.put("result", SUCCESS);
		// resultMap.put("curPage", curPage);
		// resultMap.put("pageSize", 10);
		// resultMap.put("totalSize", totalSize);
		resultMap.put("oneproxyList", oneProxyAccountReplaceRoomModelList);

	}

	private void roomDetailUserID(Map<String, String> params, Map<String, Object> resultMap) {

		int targetDateInt = 0;
		long userID;
		try {
			String targetDateStr = params.get("targetDateInt");
			if (targetDateStr != null) {
				targetDateInt = Integer.parseInt(targetDateStr);
			}
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
				Aggregation.match(Criteria.where("account_id").is(userID).and("create_time")
						.gte(MyDateUtil.getZeroDate(now)).lte(MyDateUtil.getTomorrowZeroDate(now))),
				Aggregation.group().sum("v1").as("goldtotal").sum("v2").as("brandtotal").last("account_id")
						.as("account_id"));

		AggregationResults<HashMap> models = SpringService.getBean(MongoDBService.class).getMongoTemplate()
				.aggregate(aggregation, "robot_log", HashMap.class);
		HashMap mm = models.getUniqueMappedResult();
		if (mm != null) {
			resultMap.put("goldtotal", (long) mm.get("goldtotal"));
			resultMap.put("brandtotal", (long) mm.get("brandtotal"));
		} else {
			resultMap.put("goldtotal", 0);
			resultMap.put("brandtotal", 0);
		}

		Criteria crieria = new Criteria();
		Query query = Query.query(crieria);
		crieria.and("account_id").is(userID);

		if (targetDateInt != 0) {
			crieria.and("notes_date").is(targetDateInt);
		}
		List<EveryDayRobotModel> robotDayList = SpringService.getBean(MongoDBService.class).getMongoTemplate()
				.find(query, EveryDayRobotModel.class);
		resultMap.put("data", robotDayList);

		resultMap.put("userId", account.getAccountModel().getAccount_id());
		resultMap.put("result", SUCCESS);

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
				Aggregation.match(Criteria.where("groupId").is(groupID).and("create_time")
						.gte(MyDateUtil.getZeroDate(now)).lte(MyDateUtil.getTomorrowZeroDate(now))),
				Aggregation.group().sum("v1").as("goldtotal").sum("v2").as("brandtotal").last("account_id")
						.as("account_id"));

		AggregationResults<HashMap> models = SpringService.getBean(MongoDBService.class).getMongoTemplate()
				.aggregate(aggregation, "robot_log", HashMap.class);
		HashMap mm = models.getUniqueMappedResult();
		if (mm != null) {
			resultMap.put("goldtotal", (long) mm.get("goldtotal"));
			resultMap.put("brandtotal", (long) mm.get("brandtotal"));
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
		List<EveryDayRobotModel> robotDayList = SpringService.getBean(MongoDBService.class).getMongoTemplate()
				.find(query, EveryDayRobotModel.class);
		resultMap.put("data", robotDayList);

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

		OneProxyAccountReplaceRoomModel oneProxyAccount = SpringService.getBean(MongoDBService.class).getMongoTemplate()
				.findOne(query, OneProxyAccountReplaceRoomModel.class);
		if (oneProxyAccount != null) {
			List<ProxyAccountReplaceRoomModel> replaceList = new ArrayList<ProxyAccountReplaceRoomModel>();

			String proxyIDs = oneProxyAccount.getLower_proxy_account_ids();
			String[] ids = StringUtils.split(proxyIDs, ",");
			for (String id : ids) {
				Criteria targetcrieria = new Criteria();
				Query targetquery = Query.query(targetcrieria);
				targetcrieria.and("account_id").is(Long.parseLong(id));
				targetcrieria.and("notes_date").is(targetDateInt);
				ProxyAccountReplaceRoomModel proxyOne = SpringService.getBean(MongoDBService.class).getMongoTemplate()
						.findOne(targetquery, ProxyAccountReplaceRoomModel.class);
				if (proxyOne != null) {
					AccountSimple simple = PublicServiceImpl.getInstance().getAccountSimpe(Long.parseLong(id));
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

		Date now = new Date();
		Date targetDate = DateUtils.addDays(now, -1);
		int targetDateInt = Integer.valueOf(DateFormatUtils.format(targetDate, "yyyyMMdd"));

		Criteria crieria = new Criteria();
		Query query = Query.query(crieria);
		crieria.and("account_id").is(Long.parseLong(userID));
		crieria.and("notes_date").is(targetDateInt);

		OneProxyAccountReplaceRoomModel proxyOne = SpringService.getBean(MongoDBService.class).getMongoTemplate()
				.findOne(query, OneProxyAccountReplaceRoomModel.class);
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
		long uId = Long.parseLong(userID);
		Account account = PublicServiceImpl.getInstance().getAccount(uId);
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
			MongoDBServiceImpl mongoDBServiceImpl = MongoDBServiceImpl.getInstance();
			long totalRobot = mongoDBServiceImpl.getTotalRobotGold(account.getAccount_id());

			List<EveryDayRobotModel> tempList = MongoDBServiceImpl.getInstance().robotlist;// 当天
			if (tempList != null || !tempList.isEmpty()) {
				MongoDBServiceImpl.getInstance().everyDayRobotModel(0, false);
				tempList = MongoDBServiceImpl.getInstance().robotlist;
				for (EveryDayRobotModel everyDay : tempList) {
					if (everyDay.getAccount_id() == uId) {
						totalRobot += everyDay.getGoldTotal();
					}
				}
			}

			resultMap.put("totalRobot", totalRobot);
			resultMap.put("saleGoldCount", mongoDBServiceImpl.getProxyGoldLogModelGiveCount(account.getAccount_id()));
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

		try {
			Criteria crieria = new Criteria();
			Query query = Query.query(crieria);
			crieria.and("account_id").is(account.getAccountWeixinModel().getAccount_id());
			int totalRobot = 0;
			List<EveryDayRobotModel> robotDayList = SpringService.getBean(MongoDBService.class).getMongoTemplate()
					.find(query, EveryDayRobotModel.class);
			for (EveryDayRobotModel day : robotDayList) {
				totalRobot += day.getGoldTotal();
			}
			resultMap.put("totalRobot", totalRobot);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		List<ProxyGoldLogModel> proxyGoldLogModelList = MongoDBServiceImpl.getInstance().getProxyGoldLogModelList(page,
				userID, targetID);

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
		Map<Long, AccountRecommendModel> proxyMap = account.getAccountRecommendModelMap();
		List<AccountRecommendModel> list = new ArrayList<AccountRecommendModel>(proxyMap.values());
		List<AccountRecommendModel> returnList = new ArrayList<AccountRecommendModel>();
		for (AccountRecommendModel proxyModel : list) {
			Account targetaccount = PublicServiceImpl.getInstance().getAccount(proxyModel.getTarget_account_id());
			if (targetaccount.getAccountModel().getIs_agent() == 0)
				continue;
			if (targetaccount != null) {
				proxyModel.setTarget_gold(targetaccount.getAccountModel().getGold());
				proxyModel.setTarget_total_consum(targetaccount.getAccountModel().getConsum_total());
				if (targetaccount.getAccountWeixinModel() != null) {
					proxyModel.setTarget_icon(targetaccount.getAccountWeixinModel().getHeadimgurl());
					proxyModel.setTarget_name(targetaccount.getAccountWeixinModel().getNickname());
				}
			}
			returnList.add(proxyModel);
		}
		resultMap.put("result", SUCCESS);
		resultMap.put("proxyList", returnList);
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
		if (targetAccount == null) {
			resultMap.put("result", FAIL);
			resultMap.put("msg", "目标玩家不存在");
			return;
		}

		if (goldNum <= 0) {
			return;
		}

		// if (targetAccount.getAccountModel().getProxy_level() > 0) {
		// if (targetAccount.getAccountModel().getUp_proxy() != userID) {
		// resultMap.put("result", FAIL);
		// resultMap.put("msg", "不能给非自己的下级代理冲卡");
		// return;
		// }
		// }

		int gameID = 3;
		if (account.getGame_id() != 0) {
			gameID = account.getGame_id();
		}
		// SysParamModel sysParamModel1109 =
		// SysParamDict.getInstance().getSysParamModelMapByGameId(gameID).get(1109);
		// if (sysParamModel1109 != null && sysParamModel1109.getVal2() == 0)
		// {// 控制非3级代理是否可以转给普通玩家
		// if (account.getAccountModel().getProxy_level() >= 0 &&
		// account.getAccountModel().getProxy_level() <= 2
		// && targetAccount.getAccountModel().getUp_proxy() == 0) {
		// resultMap.put("result", FAIL);
		// resultMap.put("msg", "您不能转卡给普通玩家");
		// return;
		// }
		// }
		SysParamModel sysParamModel1109 = SysParamDict.getInstance()
				.getSysParamModelDictionaryByGameId(gameID).get(1109);
		if(sysParamModel1109 != null && sysParamModel1109.getVal3() == 0){//是否控制代理之间互相转卡，0禁止，1不禁止
			if (account.getAccountModel().getProxy_level() > 0&& targetAccount.getAccountModel().getProxy_level()>0) {
				 resultMap.put("result", FAIL);
				 resultMap.put("msg", "代理之间不能相互转卡");
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
		MongoDBServiceImpl.getInstance().proxyGoldLog(account.getAccount_id(), targetAccount.getAccount_id(), nick_name,
				goldNum, account.getAccountModel().getClient_ip(), 0, target_proxy_account,
				account.getAccountModel().getGold());
		centerRMIServer.addAccountGold(targetAccount.getAccount_id(), goldNum, false,
				"游戏内转卡,接收account_id:" + account.getAccount_id(), EGoldOperateType.PROXY_GIVE);
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
		AddCardLog addCardLog = SpringService.getBean(MongoDBService.class).getMongoTemplate().findOne(query,
				AddCardLog.class);
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
				t = centerRMIServer.addAccountGold(account_id, shop.getGold() + shop.getSend_gold(), true,
						"中心充值商品:" + shopId, EGoldOperateType.SHOP_PAY);// 调用游戏充值
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
			try {
				if (account.getAccountModel().getIs_agent() == 1 && shop.getPrice() >= 400) {
					ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
					centerRMIServer.doAgentReceived(account, shop.getPrice());// 调用代理充值返利
				}
			} catch (Exception e) {

			}

			buffer.append("account_id=" + account_id);
			buffer.append(";微信昵称=" + account.getAccountWeixinModel().getNickname());
			buffer.append(";修改的方式=" + "中心购买商品");
			buffer.append(";购买的商品ID=" + shopId);
			buffer.append(";购买的房卡=" + shop.getGold());
			buffer.append(";赠送的房卡=" + shop.getSend_gold());
			buffer.append(";修改的值=" + shopId);
			buffer.append(";收款的帐号=" + "中心");
			// 增加售卡日志
			String orderID = IDGeneratorOrder.getInstance().getWPayUniqueID();
			addCardLog(orderID, account_id, account.getAccountWeixinModel().getNickname(),
					t.getAccountModel().getIs_agent(), ESellType.CENTER_PAY_CARD.getId(), shopId, shop.getGold(),
					shop.getSend_gold(), shop.getPrice(), 0, ACCOUNT_NAME, ACCOUNT_NAME, "", ACCOUNT_NAME,
					centerOrderId);
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
	private void addCardLog(String orderID, long accountId, String nickname, int accountType, int sellType, int shopId,
			int cardNum, int sendNum, int rmb, int cashAccountID, String cashAccountName, String remark, String ossID,
			String ossName, String centerOrderID) {
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
		} catch (Exception e) {
			logger.error("addcardLog插入日志异常" + e);
		}
	}

}
