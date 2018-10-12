package com.cai.rmi.impl;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.alibaba.fastjson.JSONObject;
import com.cai.common.config.ExclusiveGoldCfg;
import com.cai.common.constant.AccountConstant;
import com.cai.common.constant.GameConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.DbOpType;
import com.cai.common.define.DbStoreType;
import com.cai.common.define.DictStringType;
import com.cai.common.define.EAccountParamType;
import com.cai.common.define.EGameType;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.ELogType;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.define.ERankType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.define.EServerStatus;
import com.cai.common.define.EServerType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.define.EWelfareOperateType;
import com.cai.common.define.IDType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AccountParamModel;
import com.cai.common.domain.AccountRecommendModel;
import com.cai.common.domain.AccountSimple;
import com.cai.common.domain.AccountWeixinModel;
import com.cai.common.domain.AddCardLog;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.AddMoneyResultModel;
import com.cai.common.domain.AddWelfareResultModel;
import com.cai.common.domain.AgentRecommendModel;
import com.cai.common.domain.AppItem;
import com.cai.common.domain.ClubAccountModel;
import com.cai.common.domain.ClubRoomModel;
import com.cai.common.domain.ClubRuleModel;
import com.cai.common.domain.ClubStatusModel;
import com.cai.common.domain.DBUpdateDto;
import com.cai.common.domain.GameNoticeModel;
import com.cai.common.domain.GameRecommend;
import com.cai.common.domain.GiveCardModel;
import com.cai.common.domain.GlobalModel;
import com.cai.common.domain.HallRecommendModel;
import com.cai.common.domain.LogicGameServerModel;
import com.cai.common.domain.LogicRoomInfo;
import com.cai.common.domain.LogicStatusModel;
import com.cai.common.domain.PlayerViewVO;
import com.cai.common.domain.ProxyGameServerModel;
import com.cai.common.domain.ProxyStatusModel;
import com.cai.common.domain.RankModel;
import com.cai.common.domain.RedActivityModel;
import com.cai.common.domain.RmiDTO;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.domain.SysGameType;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.TurntableRewardModel;
import com.cai.common.domain.TurntableSystemModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.rmi.IFoundationRMIServer;
import com.cai.common.rmi.ILogicRMIServer;
import com.cai.common.rmi.IProxyRMIServer;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.util.ClubRangeCostUtil;
import com.cai.common.util.DescParams;
import com.cai.common.util.EmojiFilter;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.IDGeneratorOrder;
import com.cai.common.util.ModelToRedisUtil;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.MyStringUtil;
import com.cai.common.util.Pair;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.RoomComonUtil;
import com.cai.common.util.SerializeUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.WealthUtil;
import com.cai.core.DataThreadPool;
import com.cai.dictionary.ActivityDict;
import com.cai.dictionary.AppItemDict;
import com.cai.dictionary.ContinueLoginDict;
import com.cai.dictionary.CustomerSerNoticeDict;
import com.cai.dictionary.GameDescDict;
import com.cai.dictionary.GameGroupRuleDict;
import com.cai.dictionary.GameRecommendDict;
import com.cai.dictionary.GoodsDict;
import com.cai.dictionary.IPGroupDict;
import com.cai.dictionary.ItemDict;
import com.cai.dictionary.LogicServerBalanceDict;
import com.cai.dictionary.LoginNoticeDict;
import com.cai.dictionary.MainUiNoticeDict;
import com.cai.dictionary.MoneyShopDict;
import com.cai.dictionary.RedPackageRuleDict;
import com.cai.dictionary.ServerDict;
import com.cai.dictionary.ShopDict;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysNoticeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.dictionary.TurntableDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.PayCenterDiamondRunnable;
import com.cai.future.runnable.PayCenterRunnable;
import com.cai.future.runnable.PayIosDiamondRunnable;
import com.cai.future.runnable.PayIosRunnable;
import com.cai.future.runnable.SystemStopReadyNoticeRunnable;
import com.cai.future.runnable.SystemStopReadyRunnable;
import com.cai.redis.service.RedisService;
import com.cai.service.ClubExclusiveService;
import com.cai.service.IDServiceImpl;
import com.cai.service.MatchServiceImp;
import com.cai.service.MongoDBService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.MonitorService;
import com.cai.service.OssModifyService;
import com.cai.service.OssModifyService2;
import com.cai.service.PtAPIServiceImpl;
import com.cai.service.PublicService;
import com.cai.service.PublicServiceImpl;
import com.cai.service.RMIHandlerServiceImp;
import com.cai.service.RMIServiceImpl;
import com.cai.service.RankServiceImp;
import com.cai.service.RecommendService;
import com.cai.service.RedPackageServiceImp;
import com.cai.service.RedisServiceImpl;
import com.cai.service.ServerBalanceServiceImp;
import com.cai.service.TaskService;
import com.cai.service.TurntableService;
import com.cai.service.ZZPromoterService;
import com.cai.util.AccountUtil;
import com.cai.util.MessageResponse;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;

import javolution.util.FastMap;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountModelResponse;
import protobuf.redis.ProtoRedis.RsAccountRecommendResponse;
import protobuf.redis.ProtoRedis.RsAccountResponse;
import protobuf.redis.ProtoRedis.RsCmdResponse;
import protobuf.redis.ProtoRedis.RsDictUpdateResponse;
import protobuf.redis.ProtoRedis.RsDictUpdateResponse.RsDictType;
import protobuf.redis.ProtoRedis.RsHallRecommendResponse;
import protobuf.redis.ProtoRedis.RsMyTestResponse;
import protobuf.redis.ProtoRedis.RsRMIResultResponse;
import protobuf.redis.ProtoRedis.RsRoomResponse;
import protobuf.redis.ProtoRedis.RsSystemStopReadyResultResponse;
import protobuf.redis.ProtoRedis.RsSystemStopReadyStatusResponse;

public class CenterRMIServerImpl implements ICenterRMIServer {

	private static Logger logger = LoggerFactory.getLogger(CenterRMIServerImpl.class);

	@Override
	public void sayHello() {
		// TODO Auto-generated method stub
		System.out.println("!!!!!!!!!!");
	}

	/**
	 * 不同游戏房间号生成
	 *
	 * @param game_id
	 * @return -1表示生成失败 -2表示停服倒计时中
	 */
	@Override
	public int randomRoomId(int game_id) {

		// 是否在停服维护倒计时
		GlobalModel globalModel = PublicServiceImpl.getInstance().getGlobalModel();
		if (globalModel.isSystemStopReady()) {
			return -2;
		}

		return PublicServiceImpl.getInstance().randomRoomId(game_id);
	}

	public int randomRoomId(int game_id, int logicIndex) {
		int roomId = randomRoomId(game_id);
		//
		try {
			PublicServiceImpl.getInstance().logicToRoom(roomId, logicIndex);
		} catch (Exception e) {
			logger.error("randomRoomId error logic room", e);
		}
		return roomId;
	}

	/**
	 * 不同游戏房间号生成---取金币场房间
	 *
	 * @param game_id
	 */
	@Override
	public int[] randomRoomIds(int game_id, int count) {

		// // 是否在停服维护倒计时
		// GlobalModel globalModel =
		// PublicServiceImpl.getInstance().getGlobalModel();
		// if (globalModel.isSystemStopReady()) {
		// return -2;
		// }

		int[] temp = new int[count];

		for (int i = 0; i < temp.length; i++) {
			temp[i] = PublicServiceImpl.getInstance().moneyRandomRoomId(1);
		}

		return temp;
	}

	@Override
	public int moneyRandomRoomId(int game_id) {
		// 是否在停服维护倒计时
		GlobalModel globalModel = PublicServiceImpl.getInstance().getGlobalModel();
		if (globalModel.isSystemStopReady()) {
			return -2;
		}

		return PublicServiceImpl.getInstance().moneyRandomRoomId(game_id);
	}

	@Override
	public Account getAccount(String account_name) {
		return PublicServiceImpl.getInstance().getAccount(account_name);
	}

	@Override
	public Account getAccount(long account_id) {
		return PublicServiceImpl.getInstance().getAccount(account_id);
	}

	@Override
	public Account getAndCreateAccount(String pt_flag, String account_name, String ip, String last_client_flag, String client_version,
			int proxy_index) {

		Account account = null;
		try {
			account = PublicServiceImpl.getInstance().getAccount(account_name);
			if (account != null) {
				if (proxy_index > 0) {
					account.setLastProxyIndex(proxy_index);
				}
				return account;
			}

			PerformanceTimer timer = new PerformanceTimer();
			PublicService publicService = SpringService.getBean(PublicService.class);
			AccountModel accountModel = publicService.insertAccountModel(pt_flag, account_name, ip, last_client_flag, client_version);
			account = PublicServiceImpl.getInstance().getAccount(accountModel.getAccount_id());
			if (proxy_index > 0) {
				account.setLastProxyIndex(proxy_index);
			}
			MongoDBServiceImpl.getInstance().player_log(account.getAccount_id(), ELogType.register, "注册账号成功", timer.get(), null, ip);

			MonitorService.getMonitorService().addCreateNumber();

		} catch (Exception e) {
			logger.error("创建账号失败", e);
		}

		return account;
	}

	@Override
	public Account updateAccount(Account account) {

		// 暂时不开放
		// try {
		// Account account2 = this.getAccount(account.getAccount_name());
		// BeanUtils.copyProperties(account2, account);
		// account2.getAccountModel().setNeedDB(true);
		//
		// } catch (Exception e) {
		// logger.error("error", e);
		// }

		return null;
	}

	public void updateAccountRoomId(int account_id, int roomId) {

	}

	public FastMap<Integer, FastMap<Integer, SysParamModel>> getSysParamModelDictionary() {
		return SysParamDict.getInstance().getSysParamModelDictionary();
	}

	/**
	 * 重加载参数缓存
	 *
	 * @return
	 */
	public boolean reLoadSysParamModelDictionary() {
		SysParamDict.getInstance().load();

		// ========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
		//
		RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
		rsDictUpdateResponseBuilder.setRsDictType(RsDictType.SYS_PARAM);
		//
		redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topProxAndLogic);
		// ===================

		return true;
	}

	/**
	 * 重加载系统公告
	 *
	 * @return
	 */
	public boolean reLoadSysNoticeModelDictionary() {
		SysNoticeDict.getInstance().load();

		// ========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
		//
		RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
		rsDictUpdateResponseBuilder.setRsDictType(RsDictType.SYS_NOTICE);
		//
		redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
		// ===================

		return true;
	}

	/**
	 * 重加载游戏玩玩法说明字典
	 */
	public boolean reLoadGameDescDictionary() {
		GameDescDict.getInstance().load();
		// ========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
		//
		RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
		rsDictUpdateResponseBuilder.setRsDictType(RsDictType.GAME_DESC);
		//
		redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
		// ===================
		return true;
	}

	/**
	 * 重加载某个游戏新版本
	 */
	// public boolean reLoadNewAppItemDictionary(int appId) {
	// AppItemDict appItemDict = AppItemDict.getInstance();
	// appItemDict.load();
	// List<AppItem> appItemList = appItemDict.getAppItemList();
	// for(AppItem appItem:appItemList){
	// if(appId == appItem.getAppId()){
	//
	// }
	// }
	// // ================
	// RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
	// redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
	// RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder =
	// RsDictUpdateResponse.newBuilder();
	// rsDictUpdateResponseBuilder.setRsDictType(RsDictType.APPITEM);
	// redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
	// RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
	// ERedisTopicType.topicProxy);
	// // ===================
	// return true;
	// }

	/**
	 * 重加载游戏玩玩法说明字典
	 */
	public boolean reLoadAppItemDictionary() {
		AppItemDict appItemDict = AppItemDict.getInstance();
		appItemDict.load();
		// ========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
		//
		RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
		rsDictUpdateResponseBuilder.setRsDictType(RsDictType.APPITEM);
		//
		redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
		// ===================
		return true;
	}

	/**
	 * 重加载红包活动规则
	 */
	public boolean reLoadRedPackageRuleDictionary() {
		RedPackageRuleDict redPackageRuleDict = RedPackageRuleDict.getInstance();
		redPackageRuleDict.load();
		// ========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
		//
		RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
		rsDictUpdateResponseBuilder.setRsDictType(RsDictType.RED_PACKAGE_RULE);
		//
		redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicLogic);
		// ===================
		return true;
	}

	/**
	 * 重加载背包配置
	 */
	@Override
	public boolean reLoadItemDictionary() {
		ItemDict itemDict = ItemDict.getInstance();
		itemDict.load();
		// ========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
		//
		RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
		rsDictUpdateResponseBuilder.setRsDictType(RsDictType.PACKAGE_ITEM);
		//
		redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicMatch);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicFoundation);
		// ===================
		return true;
	}

	/**
	 * 重加载转盘规则
	 */
	@Override
	public boolean reLoadTurntableDictionary() {
		TurntableDict.getInstance().load();

		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);

		RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
		rsDictUpdateResponseBuilder.setRsDictType(RsDictType.TURNTABLE_RULE);
		redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topProxAndLogic);
		return true;
	}

	/**
	 * 重加载游戏规则
	 */
	public boolean reLoadGameGroupRuleDictionary() {
		GameGroupRuleDict gameGroupRuleDict = GameGroupRuleDict.getInstance();
		gameGroupRuleDict.load();
		// ========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
		//
		RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
		rsDictUpdateResponseBuilder.setRsDictType(RsDictType.GAME_GROUP_RULE);
		redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topProxAndLogic);
		// ===================
		return true;
	}

	/**
	 * 重加载游戏类型对应收费索引 游戏类型 描述
	 */
	public boolean reLoadSysGameTypeDictionary() {
		SysGameTypeDict sysGameTypeDict = SysGameTypeDict.getInstance();
		sysGameTypeDict.load();
		// ========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
		//
		RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
		rsDictUpdateResponseBuilder.setRsDictType(RsDictType.SYS_GAME_TYPE);
		//
		redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topProxAndLogic);
		// ===================
		return true;
	}

	/**
	 * 重加载游戏玩玩法说明字典
	 */
	public boolean reLoadContinueLoginDictionary() {
		ContinueLoginDict.getInstance().load();
		// ========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
		//
		RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
		rsDictUpdateResponseBuilder.setRsDictType(RsDictType.CONTINUE_LOGIN);
		//
		redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
		// ===================
		return true;
	}

	/**
	 * 重新加载服务器列表缓存
	 *
	 * @return
	 */
	public boolean reLoadSysParamDict() {
		// ServerDict.getInstance().load();
		this.reLoadServerDictDictionary();
		return true;
	}

	/**
	 * 重新加载后台服务器列表缓存
	 *
	 * @return
	 */
	public boolean reLoadSysParamServerDict() {
		SysParamServerDict.getInstance().load();
		// ========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
		//
		RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
		rsDictUpdateResponseBuilder.setRsDictType(RsDictType.SYS_PARAM_SERVER);
		//
		redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topProxAndLogic);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicFoundation);
		// ===================
		return true;

	}

	/**
	 * 重新加载主界面公告缓存
	 *
	 * @return
	 */
	public boolean reLoadMainUiNoticeDictionary() {
		MainUiNoticeDict.getInstance().load();
		// ========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
		//
		RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
		rsDictUpdateResponseBuilder.setRsDictType(RsDictType.MAIN_UI_NOTICE);
		//
		redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
		// ===================
		return true;
	}

	/**
	 * 重新加载客服界面公告缓存
	 *
	 * @return
	 */
	public boolean reLoadCustomerSerNoticeDict() {
		CustomerSerNoticeDict.getInstance().load();
		// ========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
		//
		RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
		rsDictUpdateResponseBuilder.setRsDictType(RsDictType.CUSTOMER_SER_NOTICE);
		//
		redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
		// ===================
		return true;
	}

	/**
	 * 重新加载活动缓存
	 *
	 * @return
	 */
	public boolean reLoadActivityDictionary() {
		ActivityDict.getInstance().load();
		// ========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
		//
		RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
		rsDictUpdateResponseBuilder.setRsDictType(RsDictType.ACTIVITY);
		//
		redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicFoundation);

		// ===================
		return true;
	}

	/**
	 * 重新加载登录公告
	 *
	 * @return
	 */
	public boolean reLoadLoginNoticeDictionary() {
		LoginNoticeDict.getInstance().load();// 登录公告
		// ========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
		//
		RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
		rsDictUpdateResponseBuilder.setRsDictType(RsDictType.LOGIN_NOTICE);
		//
		redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
		// ===================
		return true;
	}

	/**
	 * 重新加载服务器列表
	 *
	 * @return
	 */
	@Override
	public boolean reLoadServerDictDictionary() {
		ServerDict.getInstance().load();// 逻辑服列表
		// ========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
		//
		RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
		rsDictUpdateResponseBuilder.setRsDictType(RsDictType.SERVER_LOGIC);
		//
		redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topProxAndLogic);
		// ===================
		return true;
	}

	@Override
	public boolean reLoadGateServerDict() {
		return false;
	}

	public List<RoomRedisModel> getAllRoomRedisModelList() {
		RedisService redisService = SpringService.getBean(RedisService.class);
		PerformanceTimer timer = new PerformanceTimer();
		Map<byte[], byte[]> map = redisService.hGetAll(RedisConstant.ROOM);
		if (map == null)
			return null;

		List<RoomRedisModel> list = Lists.newArrayList();
		for (byte[] key : map.keySet()) {
			String skey = new String(key);
			byte[] values = map.get(key);
			RoomRedisModel roomRedisModel = (RoomRedisModel) SerializeUtil.unserialize(values);
			list.add(roomRedisModel);
		}
		logger.error("getAllRoomRedisModelList cost time" + timer.duration() + " =and list size =" + list.size());
		return list;
	}

	public String getAllRoomRedisModelListWithStr() {
		RedisService redisService = SpringService.getBean(RedisService.class);
		PerformanceTimer timer = new PerformanceTimer();
		Map<byte[], byte[]> map = redisService.hGetAll(RedisConstant.ROOM);
		if (map == null)
			return null;

		List<RoomRedisModel> list = Lists.newArrayList();
		for (byte[] key : map.keySet()) {
			String skey = new String(key);
			byte[] values = map.get(key);
			RoomRedisModel roomRedisModel = (RoomRedisModel) SerializeUtil.unserialize(values);
			list.add(roomRedisModel);
		}
		logger.error("getAllRoomRedisModelList cost time" + timer.duration() + " =and list size =" + list.size());

		Map<Integer, Integer> logicMap = new HashMap<Integer, Integer>();

		Map<Integer, Integer> gameType = new HashMap<Integer, Integer>();

		Map<Integer, Integer> waitType = new HashMap<Integer, Integer>();

		int moneyRoomNumber = 0;
		int waitCount = 0;
		int fullCount = 0;

		for (RoomRedisModel roomRedis : list) {
			if (roomRedis == null)
				continue;
			if (roomRedis.isMoneyRoom()) {
				moneyRoomNumber++;
				continue;
			}

			Integer num = logicMap.get(roomRedis.getLogic_index());
			if (num == null)
				num = 1;
			else
				num++;
			logicMap.put(roomRedis.getLogic_index(), num);

			if (roomRedis.getCur_player_num() < roomRedis.getPlayer_max()) {
				Integer waiternumber = waitType.get(roomRedis.getGame_type_index());
				waiternumber = waiternumber == null ? 0 : waiternumber;
				waitType.put(roomRedis.getGame_type_index(), waiternumber + 1);
				waitCount++;
			} else {
				Integer number = gameType.get(roomRedis.getGame_type_index());
				number = number == null ? 0 : number;
				gameType.put(roomRedis.getGame_type_index(), number + 1);
				fullCount++;
			}

		}

		StringBuffer buf = new StringBuffer();
		buf.append("逻辑服房间分布:");
		for (Entry<Integer, Integer> entry : logicMap.entrySet()) {
			buf.append("逻辑服").append(entry.getKey()).append(" 房间数量:").append("<font color = 'red'>" + entry.getValue() + "</font></br>");
		}

		buf.append("另外金币场房间:" + moneyRoomNumber);
		buf.append("</br>");

		buf.append("游戏总开桌:<font color = 'red'>" + (fullCount + waitCount) + "</font></br>");
		buf.append("进行中桌数:<font color = 'red'>" + fullCount + " </font>进行中详情: ");
		buf.append("<font color = 'red'>");
		for (Entry<Integer, Integer> entry : gameType.entrySet()) {
			buf.append(SysGameTypeDict.getInstance().getMJname(entry.getKey()) + "(" + entry.getValue() + ")桌,  ");
		}
		buf.append("</font>");
		buf.append("</br>");

		buf.append("等待中桌数:<font color = 'red'>" + waitCount + " </font>等待中详情: ");
		buf.append("<font color = 'red'>");
		for (Entry<Integer, Integer> entry : waitType.entrySet()) {
			buf.append(SysGameTypeDict.getInstance().getMJname(entry.getKey()) + "(" + entry.getValue() + ")桌,  ");
		}
		buf.append("</font>");

		return buf.toString();
	}

	public void delRoomById(int room_id) {
		delRoomById(room_id, "游戏已被系统解散");
	}

	@Override
	public void delRoomById(int room_id, String desc) {
		RedisService redisService = SpringService.getBean(RedisService.class);
		RoomRedisModel roomRedisModel = redisService.hGet(RedisConstant.ROOM, room_id + "", RoomRedisModel.class);

		if (roomRedisModel != null) {
			// ========同步到中心========
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ROOM);
			//
			RsRoomResponse.Builder rsRoomResponseBuilder = RsRoomResponse.newBuilder();
			rsRoomResponseBuilder.setType(1);
			rsRoomResponseBuilder.setRoomId(room_id);
			if (!Strings.isNullOrEmpty(desc)) {
				rsRoomResponseBuilder.setDesc(desc);
			}

			redisResponseBuilder.setRsRoomResponse(rsRoomResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
					ERedisTopicType.topicLogic.getId() + roomRedisModel.getLogic_index());
		}
		//

	}

	/**
	 * 查询指定房间
	 *
	 * @param room_id
	 */
	public RoomRedisModel getRoomById(int room_id) {
		RedisService redisService = SpringService.getBean(RedisService.class);
		RoomRedisModel roomRedisModel = redisService.hGet(RedisConstant.ROOM, room_id + "", RoomRedisModel.class);
		return roomRedisModel;
	}

	/**
	 * 所有代理服的状态
	 *
	 * @return
	 */
	public List<ProxyStatusModel> getProxyStatusList() {
		List<ProxyStatusModel> list = Lists.newArrayList();
		FastMap<Integer, ProxyGameServerModel> proxyMap = ServerDict.getInstance().getProxyGameServerModelDict();
		for (ProxyGameServerModel model : proxyMap.values()) {
			if (model.getStatus() == EServerStatus.ACTIVE || model.getStatus() == EServerStatus.REPAIR) {
				try {
					IProxyRMIServer proxyRMIServer = RMIServiceImpl.getInstance().getIProxyRMIByIndex(model.getProxy_game_id());
					if (proxyRMIServer == null)
						continue;
					ProxyStatusModel proxyStatusModel = proxyRMIServer.getProxyStatus();
					list.add(proxyStatusModel);
				} catch (Exception e) {
					logger.error("error", e);
				}
			}
		}
		return list;
	}

	/**
	 * 所有逻辑服的状态
	 *
	 * @return
	 */
	public List<LogicStatusModel> getLogicStatusList() {
		List<LogicStatusModel> list = Lists.newArrayList();
		FastMap<Integer, LogicGameServerModel> logicMap = ServerDict.getInstance().getLogicGameServerModelDict();
		for (LogicGameServerModel model : logicMap.values()) {
			if (model.getStatus() == EServerStatus.ACTIVE || model.getStatus() == EServerStatus.REPAIR) {
				try {
					ILogicRMIServer logicRMIServer = RMIServiceImpl.getInstance().getLogicRMIByIndex(model.getLogic_game_id());
					if (logicRMIServer == null)
						continue;
					LogicStatusModel logicStatusModel = logicRMIServer.getLogicStatus();
					list.add(logicStatusModel);
				} catch (Exception e) {
					logger.error("error", e);
				}
			}
		}
		return list;
	}

	public LogicRoomInfo getLogicRoomInfo(int roomId) {
		int logicIndex = PublicServiceImpl.getInstance().getLogicIndexByRoomId(roomId);
		if (logicIndex > 0) {
			FastMap<Integer, LogicGameServerModel> logicMap = ServerDict.getInstance().getLogicGameServerModelDict();
			LogicGameServerModel model = logicMap.get(logicIndex);
			if (model != null && (model.getStatus() == EServerStatus.ACTIVE || model.getStatus() == EServerStatus.REPAIR)) {
				ILogicRMIServer logicRMIServer = RMIServiceImpl.getInstance().getLogicRMIByIndex(model.getLogic_game_id());
				if (logicRMIServer != null) {
					return logicRMIServer.getLogicRoomInfo(roomId);
				}
			}
		}
		return null;
	}

	/**
	 * 所有逻辑服的牌桌状态
	 *
	 * @return
	 */
	public List<LogicRoomInfo> getLogicRoomInfoList() {
		List<LogicRoomInfo> list = Lists.newArrayList();
		FastMap<Integer, LogicGameServerModel> logicMap = ServerDict.getInstance().getLogicGameServerModelDict();
		for (LogicGameServerModel model : logicMap.values()) {
			if (model.getStatus() == EServerStatus.ACTIVE || model.getStatus() == EServerStatus.REPAIR) {
				try {
					ILogicRMIServer logicRMIServer = RMIServiceImpl.getInstance().getLogicRMIByIndex(model.getLogic_game_id());
					if (logicRMIServer == null)
						continue;

					List<LogicRoomInfo> logicRooms = logicRMIServer.getLogicRoomInfos();
					list.addAll(logicRooms);

					SysParamModel sysparamModel = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(1198);
					if (sysparamModel == null || sysparamModel.getVal2() == 0)
						continue;

					if (logicRooms.size() < 5 && logicRooms.size() > 0) {
						StringBuilder builder = new StringBuilder();
						for (LogicRoomInfo roomInfo : logicRooms) {
							builder.append("玩家-").append(Arrays.toString(roomInfo.getPlayerIDs()));
						}
						MongoDBServiceImpl.getInstance().server_error_log(0, ELogType.leftRoom, builder.toString(), 0L, null);
					}
				} catch (Exception e) {
					logger.error("error", e);
				}
			}
		}
		return list;
	}

	/**
	 * 后台测试牌型
	 *
	 * @param cards
	 */
	@Override
	public String testCard(String cards) {
		try {
			String[] bodys = StringUtils.split(cards, "#");
			if (bodys.length <= 0) {
				return "参数非法";
			}
			if ("COIN".equalsIgnoreCase(bodys[0])) {
				Map<Integer, ILogicRMIServer> rmis = RMIServiceImpl.getInstance().getLogicRMIServerMap();
				rmis.forEach((index, rmi) -> {
					try {
						rmi.testCard(cards);
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
				return "操作完成！";
			} else {
				int roomID = Integer.parseInt(bodys[1]);
				RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM, roomID + "", RoomRedisModel.class);
				if (roomRedisModel == null) {
					return "房间不存在";
				}
				ILogicRMIServer logicRMIServer = RMIServiceImpl.getInstance().getLogicRMIByIndex(roomRedisModel.getLogic_index());
				if (logicRMIServer == null) {
					return "逻辑服关闭";
				}
				return logicRMIServer.testCard(cards);
			}

		} catch (Exception e) {
			return "服务器异常" + e.getMessage();
		}
	}

	/**
	 * 增减玩家房卡
	 *
	 * @param account_id
	 * @param gold
	 * @param desc
	 * @param eGoldOperateTypeStr-----EGoldOperateType防止依赖，可以穿int值过来
	 * @return
	 */
	public AddGoldResultModel addAccountGold(long account_id, int gold, boolean isExceed, String desc, int eGoldOperateTypeStr) {

		// logger.info("===========================" + desc + "\t" +
		// eGoldOperateType.getIdstr() + "\t" + account_id);
		AddGoldResultModel addGoldResultModel = new AddGoldResultModel();
		addGoldResultModel.setSuccess(false);

		Account account = this.getAccount(account_id);
		if (account == null) {
			addGoldResultModel.setMsg("账号不存在");
			return addGoldResultModel;
		}
		ReentrantLock lock = account.getRedisLock();
		lock.lock();
		try {
			AccountModel accountModel = account.getAccountModel();
			AccountWeixinModel wxModel = account.getAccountWeixinModel();
			addGoldResultModel.setAccountModel(accountModel);
			if (wxModel != null) {
				addGoldResultModel.setWxNickName(wxModel.getNickname());
			}
			long oldValue = accountModel.getGold();

			if (gold > 0) {
				accountModel.setGold(accountModel.getGold() + gold);
				accountModel.setHistory_pay_gold(accountModel.getHistory_pay_gold() + gold);
				addGoldResultModel.setSuccess(true);

				AccountParamModel accountParamModel = PublicServiceImpl.getInstance().getAccountParamModel(account_id,
						EAccountParamType.TODAY_ADD_GOLD);
				if (accountParamModel != null) {
					accountParamModel.setLong1(accountParamModel.getLong1() + gold);
					accountParamModel.setNeedDB(true);
				}
				if (eGoldOperateTypeStr == EGoldOperateType.FAILED_ROOM.getId()) {
					accountModel.setConsum_total(accountModel.getConsum_total() - gold);// gold<0
				}
			} else {
				long k = accountModel.getGold() + gold;
				if (!isExceed) {
					if (k < 0) {
						addGoldResultModel.setMsg("库存不足");
						return addGoldResultModel;
					}
				}

				if (k < 0) {
					accountModel.setGold(0L);
				} else {
					accountModel.setGold(k);
				}
				AccountParamModel accountParamModel = PublicServiceImpl.getInstance().getAccountParamModel(account_id,
						EAccountParamType.TODAY_CONSUM_GOLD);
				if (accountParamModel != null) {
					accountParamModel.setLong1(accountParamModel.getLong1() - gold);
					accountParamModel.setNeedDB(true);
				}
				if (eGoldOperateTypeStr == EGoldOperateType.PROXY_GIVE.getId() || eGoldOperateTypeStr == EGoldOperateType.OPEN_ROOM.getId()) {
					accountModel.setConsum_total(accountModel.getConsum_total() - gold);// gold<0
				}
				addGoldResultModel.setSuccess(true);
				if (accountModel.getIs_agent() > 0) {
					try {
						SysParamModel sysParamModel2260 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2260);
						if (sysParamModel2260 != null && sysParamModel2260.getVal1() == 1) {
							IFoundationRMIServer iFoundationRMIServer = (IFoundationRMIServer) SpringService.getBean("foundationRMIServer");
							iFoundationRMIServer.lackGoldPush(account_id);
						}
					} catch (Exception e) {
						logger.error("");
					}
				}
			}

			if (EGoldOperateType.OPEN_ROOM.getId() == eGoldOperateTypeStr) {
				accountModel.setNeedDB(true);
			} else {
				// 房卡操作直接入库
				DataThreadPool.getInstance().addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.UPDATE, "updateAccountModel", accountModel));
			}

			long change = accountModel.getGold() - oldValue;
			long newValue = accountModel.getGold();
			// 日志
			StringBuffer buf = new StringBuffer();
			buf.append("|");
			if (change > 0) {
				buf.append("增加[" + change + "]");
			} else {
				buf.append("减少[" + change + "]");
				// MongoDBServiceImpl.getInstance().log(account_id,
				// ELogType.addGold, desc, change,
				// (long) EAccountParamType.TODAY_CONSUM_GOLD.getId(), null);
			}
			buf.append(",值变化:[").append(oldValue).append("]->[").append(newValue).append("]");
			desc = desc + buf.toString();
			MongoDBServiceImpl.getInstance().log(account_id, ELogType.addGold, desc, change, (long) eGoldOperateTypeStr, null, oldValue, newValue);
			// ========同步到中心========
			// 房间消耗的逻辑服直接同步了，不需要走redis广播
			if (!WealthUtil.roomGoldTypeInteger.contains(eGoldOperateTypeStr)) {
				RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
				redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
				//
				RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
				rsAccountResponseBuilder.setAccountId(account_id);
				//
				RsAccountModelResponse.Builder rsAccountModelResponseBuilder = RsAccountModelResponse.newBuilder();
				rsAccountModelResponseBuilder.setGold(accountModel.getGold());
				rsAccountModelResponseBuilder.setHistoryPayGold(accountModel.getHistory_pay_gold());
				rsAccountModelResponseBuilder.setConsumTotal(accountModel.getConsum_total());
				rsAccountModelResponseBuilder.setGoldChangeType(eGoldOperateTypeStr);
				rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponseBuilder);
				//
				redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
				RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
						ERedisTopicType.topicProxy.getId() + account.getLastProxyIndex());
			}

			// 通知刷新
			// RMIMsgSender.callClub(RMICmd.ACCOUNT_WEALTH_UPDATE,
			// AccountWealthVo.newVo(account_id, newValue, EWealthCategory.GOLD,
			// eGoldOperateType));

		} catch (Exception e) {
			logger.error("error", e);
		} finally {
			lock.unlock();
		}
		return addGoldResultModel;
	}

	/**
	 * 增减玩家钻石
	 *
	 * @param account_id
	 * @param gold
	 * @param desc
	 * @return
	 */
	public AddGoldResultModel addAccountDiamond(long account_id, int diamond, boolean isExceed, String desc, int ediamondOperateTypeStr) {

		// logger.info("===========================" + desc + "\t" +
		// eGoldOperateType.getIdstr() + "\t" + account_id);
		AddGoldResultModel addGoldResultModel = new AddGoldResultModel();
		addGoldResultModel.setSuccess(false);

		Account account = this.getAccount(account_id);
		if (account == null) {
			addGoldResultModel.setMsg("账号不存在");
			return addGoldResultModel;
		}
		ReentrantLock lock = account.getRedisLock();
		lock.lock();
		try {
			AccountModel accountModel = account.getAccountModel();
			AccountWeixinModel wxModel = account.getAccountWeixinModel();
			addGoldResultModel.setAccountModel(accountModel);
			if (wxModel != null) {
				addGoldResultModel.setWxNickName(wxModel.getNickname());
			}
			long oldValue = accountModel.getDiamond();

			if (diamond > 0) {
				accountModel.setDiamond(accountModel.getDiamond() + diamond);
				addGoldResultModel.setSuccess(true);
			} else {
				int k = accountModel.getDiamond() + diamond;
				if (!isExceed) {
					if (k < 0) {
						addGoldResultModel.setMsg("库存不足");
						return addGoldResultModel;
					}
				}

				if (k < 0) {
					accountModel.setDiamond(0);
				} else {
					accountModel.setDiamond(k);
				}

				addGoldResultModel.setSuccess(true);
			}

			// 房卡操作直接入库
			DataThreadPool.getInstance().addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.UPDATE, "updateAccountModel", accountModel));

			long change = accountModel.getDiamond() - oldValue;
			long newValue = accountModel.getDiamond();
			// 日志
			StringBuffer buf = new StringBuffer();
			buf.append("|");
			if (change > 0) {
				buf.append("增加[" + change + "]");
			} else {
				buf.append("减少[" + change + "]");
				// MongoDBServiceImpl.getInstance().log(account_id,
				// ELogType.addGold, desc, change,
				// (long) EAccountParamType.TODAY_CONSUM_GOLD.getId(), null);
			}
			buf.append(",值变化:[").append(oldValue).append("]->[").append(newValue).append("]");
			desc = desc + buf.toString();
			MongoDBServiceImpl.getInstance().log(account_id, ELogType.addDIAMOND, desc, change, (long) ediamondOperateTypeStr, null, oldValue,
					newValue);
			// ========同步到中心========

			// 发送钻石变化到代理服
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			//
			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			rsAccountResponseBuilder.setAccountId(accountModel.getAccount_id());
			//
			RsAccountModelResponse.Builder rsAccountModelResponseBuilder = RsAccountModelResponse.newBuilder();
			rsAccountModelResponseBuilder.setDiamond(accountModel.getDiamond());
			rsAccountModelResponseBuilder.setDiamondChangeType(ediamondOperateTypeStr);
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

	/**
	 * 增减玩家房卡
	 *
	 * @param account_id
	 * @param gold
	 * @param desc
	 * @return
	 */
	public AddGoldResultModel addAccountGold(long account_id, int gold, boolean isExceed, String desc, EGoldOperateType eGoldOperateType) {
		int eGoldOperateTypeInt = eGoldOperateType == null ? EGoldOperateType.UNKOWN_TYPE.getId() : eGoldOperateType.getId();
		return addAccountGold(account_id, gold, isExceed, desc, eGoldOperateTypeInt);

	}

	/**
	 * 增减玩家福卡
	 */
	public AddWelfareResultModel addAccountWelfare(long account_id, int welfare, String desc, EWelfareOperateType eWelfareOperateType) {
		int eGoldOperateTypeInt = eWelfareOperateType == null ? EWelfareOperateType.UNKOWN_TYPE.getId() : eWelfareOperateType.getId();
		return addAccountWelfare(account_id, welfare, desc, eGoldOperateTypeInt);
	}

	/**
	 * 增减玩家福卡
	 */
	public AddWelfareResultModel addAccountWelfare(long account_id, int welfare, String desc, int eWelfareOperateTypeInt) {

		AddWelfareResultModel addWelfareResultModel = new AddWelfareResultModel();
		addWelfareResultModel.setSuccess(false);

		Account account = this.getAccount(account_id);
		if (account == null) {
			addWelfareResultModel.setMsg("账号不存在");
			return addWelfareResultModel;
		}
		ReentrantLock lock = account.getRedisLock();
		lock.lock();
		try {
			AccountModel accountModel = account.getAccountModel();
			AccountWeixinModel wxModel = account.getAccountWeixinModel();
			addWelfareResultModel.setAccountModel(accountModel);
			if (wxModel != null) {
				addWelfareResultModel.setWxNickName(wxModel.getNickname());
			}
			long oldValue = accountModel.getWelfare();

			if (welfare > 0) {
				accountModel.setWelfare(accountModel.getWelfare() + welfare);
				addWelfareResultModel.setSuccess(true);

				AccountParamModel accountParamModel = PublicServiceImpl.getInstance().getAccountParamModel(account_id,
						EAccountParamType.TODAY_ADD_WELFARE);
				if (accountParamModel != null) {
					accountParamModel.setLong1(accountParamModel.getLong1() + welfare);
					accountParamModel.setNeedDB(true);
				}
			} else {
				long k = accountModel.getWelfare() + welfare;
				if (k < 0) {
					addWelfareResultModel.setMsg("库存不足");
					return addWelfareResultModel;
				}
				accountModel.setWelfare(k);
				AccountParamModel accountParamModel = PublicServiceImpl.getInstance().getAccountParamModel(account_id,
						EAccountParamType.TODAY_CONSUME_WELFARE);
				if (accountParamModel != null) {
					accountParamModel.setLong1(accountParamModel.getLong1() - welfare);
					accountParamModel.setNeedDB(true);
				}
				addWelfareResultModel.setSuccess(true);
			}

			if (welfare != 0)
				accountModel.setNeedDB(true);

			long change = accountModel.getWelfare() - oldValue;
			long newValue = accountModel.getWelfare();
			// 日志
			StringBuffer buf = new StringBuffer();
			buf.append("|");
			if (change > 0) {
				buf.append("增加[" + change + "]");
			} else {
				buf.append("减少[" + change + "]");
			}
			buf.append(",值变化:[").append(oldValue).append("]->[").append(newValue).append("]");
			desc = desc + buf.toString();
			MongoDBServiceImpl.getInstance().log(account_id, ELogType.addWelfare, desc, change, (long) eWelfareOperateTypeInt, null, oldValue,
					newValue);
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			//
			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			rsAccountResponseBuilder.setAccountId(account_id);
			//
			RsAccountModelResponse.Builder rsAccountModelResponseBuilder = RsAccountModelResponse.newBuilder();
			rsAccountModelResponseBuilder.setWelfareChangeType(eWelfareOperateTypeInt);
			rsAccountModelResponseBuilder.setWelfare(accountModel.getWelfare());
			rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponseBuilder);
			//
			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
					ERedisTopicType.topicProxy.getId() + account.getLastProxyIndex());
		} catch (Exception e) {
			logger.error("error", e);
		} finally {
			lock.unlock();
		}
		return addWelfareResultModel;
	}

	public void doAgentReceived(Account account, int money) {
		// 代理充值返利
		try {
			if (account == null || money < 400) {
				return;
			}
			AccountModel accountModel = account.getAccountModel();
			if (accountModel.getIs_agent() == 1 && accountModel.getRecommend_id() > 0) {
				Account recommendAccount = PublicServiceImpl.getInstance().getAccount(accountModel.getRecommend_id());
				AccountModel recommendModel = recommendAccount.getAccountModel();
				if (recommendModel.getRecommend_level() == 0) {
					return;
				}
				int level = recommendModel.getRecommend_level();
				ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
				if (account.getGame_id() == 0) {
					account.setGame_id(1);
				}
				SysParamModel sysParamModel6000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(account.getGame_id()).get(6000);
				if (sysParamModel6000 == null) {
					return;
				}
				double totalReciveMoney = money * sysParamModel6000.getVal1() / 10000.0;
				if (level == 1) {// 返利10%
					logger.info(recommendAccount.getAccount_id() + " 总推广员下的代理充值房卡数：" + totalReciveMoney + " 返利:" + totalReciveMoney);
					centerRMIServer.doRecommendIncome(recommendAccount.getAccount_id(), totalReciveMoney, 4l, "总推广员下的代理充值",
							EGoldOperateType.AGENT_RECHARGE_RECEIVER, account.getAccount_id(), money);
					return;
				} else if (level == 2) {
					logger.info(recommendAccount.getAccount_id() + " 一级推广员下的代理充值房卡数：" + totalReciveMoney + " 返利:"
							+ totalReciveMoney * sysParamModel6000.getVal4() / 100.0);
					centerRMIServer.doRecommendIncome(recommendAccount.getAccount_id(), totalReciveMoney * sysParamModel6000.getVal4() / 100.0, 4l,
							"一级推广员下的代理充值", EGoldOperateType.AGENT_RECHARGE_RECEIVER, account.getAccount_id(), money);
					if (recommendAccount.getAccountModel().getRecommend_id() != 0) {
						Account recommendAccountUp = PublicServiceImpl.getInstance().getAccount(recommendAccount.getAccountModel().getRecommend_id());
						if (recommendAccountUp.getAccountModel().getRecommend_level() == 1) {
							logger.info(recommendAccount.getAccount_id() + " 一级推广员下的代理充值房卡数：" + totalReciveMoney + " 返利:"
									+ totalReciveMoney * sysParamModel6000.getVal2() / 100.0);
							centerRMIServer.doRecommendIncome(recommendAccountUp.getAccount_id(),
									totalReciveMoney * sysParamModel6000.getVal2() / 100.0, 5l, "一级推广员下的代理充值",
									EGoldOperateType.AGENT_RECHARGE_RECEIVER, account.getAccount_id(), money);
							return;
						}
					}

				} else if (level == 3) {
					logger.info(recommendAccount.getAccount_id() + " 二级推广员下的代理充值房卡数：" + totalReciveMoney + " 返利:"
							+ totalReciveMoney * sysParamModel6000.getVal5() / 100.0);
					centerRMIServer.doRecommendIncome(recommendAccount.getAccount_id(), totalReciveMoney * sysParamModel6000.getVal5() / 100.0, 4l,
							"二级推广员下的代理充值", EGoldOperateType.AGENT_RECHARGE_RECEIVER, account.getAccount_id(), money);
					if (recommendAccount.getAccountModel().getRecommend_id() != 0) {
						Account recommendAccountUp = PublicServiceImpl.getInstance().getAccount(recommendAccount.getAccountModel().getRecommend_id());
						if (recommendAccountUp.getAccountModel().getRecommend_level() == 2) {
							logger.info(recommendAccount.getAccount_id() + " 二级推广员下的代理充值房卡数：" + totalReciveMoney + " 返利:"
									+ totalReciveMoney * sysParamModel6000.getVal3() / 100.0);
							centerRMIServer.doRecommendIncome(recommendAccountUp.getAccount_id(),
									totalReciveMoney * sysParamModel6000.getVal3() / 100.0, 6l, "二级推广员下的代理充值",
									EGoldOperateType.AGENT_RECHARGE_RECEIVER, account.getAccount_id(), money);
							if (recommendAccountUp.getAccountModel().getRecommend_id() != 0) {
								Account recommendAccountUpUp = PublicServiceImpl.getInstance()
										.getAccount(recommendAccountUp.getAccountModel().getRecommend_id());
								if (recommendAccountUpUp.getAccountModel().getRecommend_level() == 1) {
									logger.info(recommendAccount.getAccount_id() + " 二级推广员下的代理充值房卡数：" + totalReciveMoney + " 返利:"
											+ totalReciveMoney * sysParamModel6000.getVal2() / 100.0);
									centerRMIServer.doRecommendIncome(recommendAccountUpUp.getAccount_id(),
											totalReciveMoney * sysParamModel6000.getVal2() / 100.0, 6l, "二级推广员下的代理充值",
											EGoldOperateType.AGENT_RECHARGE_RECEIVER, account.getAccount_id(), money);
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("error", e);
		}
	}

	/**
	 * 分配逻辑服
	 *
	 * @return
	 */
	public int allotLogicId(int game_id) {

		// TODO 负载均衡算法,目前先用随机
		try {
			// List<LogicGameServerModel> logicList =
			// ServerDict.getInstance().getActiveLogicList();
			// if (logicList.isEmpty()) {
			// return -1;
			// }
			// int index = RandomUtil.getRandomNumber(logicList.size());
			//
			// LogicGameServerModel logicGameServerModel = logicList.get(index);
			// return logicGameServerModel.getLogic_game_id();

			return ServerBalanceServiceImp.getInstance().allotLogicId(game_id);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("center server alloc logic server occurr error", e);
		}
		return -1;
	}

	public RmiDTO getMaxNumberAllotLogicId(int game_id, DescParams params) {
		int logicSvrId = allotLogicId(game_id);
		if (logicSvrId <= 0) {
			logger.error("center server getMaxNumberAllotLogicId occurr error");
			return null;
		}
		ILogicRMIServer logicRmiServer = RMIServiceImpl.getInstance().getLogicRMIByIndex(logicSvrId);
		return logicRmiServer.getGameDescAndPeopleNumber(params);
	}

	/**
	 * 压力测试
	 *
	 * @param type
	 * @param num
	 */
	public void myTest(int type, int num) {
		// ========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.MY_TEST);

		RsMyTestResponse.Builder rsMyTestResponseBuilder = RsMyTestResponse.newBuilder();
		rsMyTestResponseBuilder.setType(type);
		rsMyTestResponseBuilder.setNum(num);
		redisResponseBuilder.setRsMyTestResponse(rsMyTestResponseBuilder);

		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
	}

	/**
	 * 实时发送游戏公告
	 *
	 * @param game_id
	 * @param content
	 */
	public void sendGameNotice(int game_type, String content, int play_type) {

		GameNoticeModel model = new GameNoticeModel();
		model.setId(0);
		model.setContent(content);
		model.setGame_type(game_type);
		model.setDelay(1);
		model.setCreate_time(new Date());
		model.setEnd_time(new Date());
		model.setPlay_type(play_type);// 全局,哪里都可以显示的

		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.GAME_NOTICE);
		redisResponseBuilder.setRsGameNoticeModelResponse(ModelToRedisUtil.getRsGameNoticeModelResponse(model));
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
		// 日志
		StringBuilder buf = new StringBuilder();
		buf.append("临时公告:id:0").append(",content:" + model.getContent()).append(",game_type:" + model.getGame_type());
		MongoDBServiceImpl.getInstance().systemLog(ELogType.gameNotice, buf.toString(), null, null, ESysLogLevelType.NONE);

	}

	/**
	 * 提供给oss修改数据的,只处理已做的
	 */
	public boolean ossModifyAccountModel(RsAccountModelResponse rsAccountModelResponse) {
		SysParamModel sysParamModel2251 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2251);
		if (sysParamModel2251 == null || sysParamModel2251.getVal4() == 0) {
			return OssModifyService.getInstance().ossModifyAccountModel(rsAccountModelResponse);
		} else {
			return OssModifyService2.getInstance().ossModifyAccountModel(rsAccountModelResponse);
		}
	}

	// 推荐代理返利
	private void addRecommendAgentIncome(Account account) {
		try {
			AccountParamModel accountParamModel = PublicServiceImpl.getInstance().getAccountParamModel(account.getAccount_id(),
					EAccountParamType.UP_RECOMMEND_AGENT_INCOME);
			if (accountParamModel.getVal1() != null && accountParamModel.getVal1() == 1) {
				return;
			}
			accountParamModel.setVal1(1);
			accountParamModel.setNeedDB(true);
			// ========同步到中心========
			// RedisResponse.Builder redisResponseBuilder =
			// RedisResponse.newBuilder();
			// redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			// //
			// RsAccountResponse.Builder rsAccountResponseBuilder =
			// RsAccountResponse.newBuilder();
			// rsAccountResponseBuilder.setAccountId(account.getAccount_id());
			// //
			// RsAccountParamModelResponse.Builder rsAccountParamModelResponse =
			// RsAccountParamModelResponse.newBuilder();
			// rsAccountParamModelResponse.setAccountId(account.getAccount_id());
			// rsAccountParamModelResponse.setType(EAccountParamType.UP_RECOMMEND_AGENT_INCOME.getId());
			// rsAccountParamModelResponse.setVal1(accountParamModel.getVal1());
			// rsAccountParamModelResponse.setNeedDb(true);
			// rsAccountResponseBuilder.addRsAccountParamModelResponseList(rsAccountParamModelResponse);
			// redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			// RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
			// ERedisTopicType.topicCenter);
			// 活动相关
			AccountModel accountModel = account.getAccountModel();
			if (accountModel.getRecommend_id() == 0) {
				return;// 无推荐人
			}
			Account recommendAccount = getAccount(accountModel.getRecommend_id());
			int level = recommendAccount.getAccountModel().getRecommend_level();
			if (recommendAccount == null || level == 0) {
				return;// 推荐人不存在或推荐人不是推广员
			}
			if (account.getGame_id() == 0) {
				account.setGame_id(1);
			}
			SysParamModel sysParamModel6002 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(account.getGame_id()).get(6002);
			if (sysParamModel6002 == null) {
				return;
			}
			if (level == 1) {
				doRecommendIncome(recommendAccount.getAccount_id(), sysParamModel6002.getVal1().doubleValue(), 0l, "推荐新代理",
						EGoldOperateType.RECOMMEND_AGENT, account.getAccount_id());
				return;
			} else if (level == 2) {
				doRecommendIncome(recommendAccount.getAccount_id(), sysParamModel6002.getVal2().doubleValue(), 0l, "推荐新代理",
						EGoldOperateType.RECOMMEND_AGENT, account.getAccount_id());
				if (recommendAccount.getAccountModel().getRecommend_id() != 0) {
					Account recommendAccountUp = getAccount(recommendAccount.getAccountModel().getRecommend_id());
					if (recommendAccountUp.getAccountModel().getRecommend_level() == 1) {
						doRecommendIncome(recommendAccountUp.getAccount_id(), sysParamModel6002.getVal4().doubleValue(), 2l, "下级推广员推荐新代理",
								EGoldOperateType.RECOMMEND_AGENT, account.getAccount_id());
						return;
					}
				}
			} else if (level == 3) {
				doRecommendIncome(recommendAccount.getAccount_id(), sysParamModel6002.getVal3().doubleValue(), 0l, "推荐新代理",
						EGoldOperateType.RECOMMEND_AGENT, account.getAccount_id());
				if (recommendAccount.getAccountModel().getRecommend_id() != 0) {
					Account recommendAccountUp = getAccount(recommendAccount.getAccountModel().getRecommend_id());
					if (recommendAccountUp.getAccountModel().getRecommend_level() == 2) {
						doRecommendIncome(recommendAccountUp.getAccount_id(), sysParamModel6002.getVal5().doubleValue(), 3l, "下级推广员推荐新代理",
								EGoldOperateType.RECOMMEND_AGENT, account.getAccount_id());
						if (recommendAccountUp.getAccountModel().getRecommend_id() != 0) {
							Account recommendAccountUpUp = getAccount(recommendAccountUp.getAccountModel().getRecommend_id());
							if (recommendAccountUpUp.getAccountModel().getRecommend_level() == 1) {
								doRecommendIncome(recommendAccountUpUp.getAccount_id(), sysParamModel6002.getVal4().doubleValue(), 3l, "下级推广员推荐新代理",
										EGoldOperateType.RECOMMEND_AGENT, account.getAccount_id());
							}
						}
					}
				}
			}
		} catch (Exception e) {
		}
	}

	/**
	 * 清除账号缓存
	 *
	 * @param account_id
	 * @return
	 */
	public boolean clearAccountCache(long account_id) {
		Account account = this.getAccount(account_id);
		if (account == null)
			return true;

		// 本地内存
		PublicServiceImpl.getInstance().clearAccount(account);

		// // redis
		// RedisService redisService =
		// SpringService.getBean(RedisService.class);
		// redisService.hDel(RedisConstant.ACCOUNT, (account_id +
		// "").getBytes());
		return true;
	}

	/**
	 * 重新加载有效商品列表
	 */
	public void reloadVailShopList() {
		ShopDict.getInstance().load();

		// ========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
		//
		RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
		rsDictUpdateResponseBuilder.setRsDictType(RsDictType.SHOP);
		//
		redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicFoundation);

		// ===================

	}

	/**
	 * 重新加载金币商品列表
	 */
	public void reloadVailMoneyShopList() {
		MoneyShopDict.getInstance().load();
		// ========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
		//
		RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
		rsDictUpdateResponseBuilder.setRsDictType(RsDictType.MONEY_SHOP);
		//
		redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
	}

	/**
	 * 踢下线
	 *
	 * @param account_id
	 * @return
	 */
	public boolean offlineAccount(long account_id) {
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.CMD);
		RsCmdResponse.Builder rsCmdResponseBuilder = RsCmdResponse.newBuilder();
		rsCmdResponseBuilder.setType(1);
		rsCmdResponseBuilder.setAccountId(account_id);
		redisResponseBuilder.setRsCmdResponse(rsCmdResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
		return true;
	}

	/**
	 * 当前系统关闭准备状态
	 *
	 * @return
	 */
	public RsSystemStopReadyStatusResponse systemStopReadyStatus() {
		GlobalModel globalModel = PublicServiceImpl.getInstance().getGlobalModel();
		RsSystemStopReadyStatusResponse resposne = MessageResponse.getRsSystemStopReadyStatusResponse(globalModel);
		return resposne;
	}

	/**
	 * 系统关闭准备
	 */
	public RsSystemStopReadyResultResponse systemStopReady(int minute) {

		GlobalModel globalModel = PublicServiceImpl.getInstance().getGlobalModel();
		RsSystemStopReadyResultResponse.Builder rsSystemStopReadyResultResponseBuilder = RsSystemStopReadyResultResponse.newBuilder();

		if (minute < 1 || minute > 120) {
			rsSystemStopReadyResultResponseBuilder.setMsg("参数不正确,时间必须大于等于1分钟,小于120分钟");
			rsSystemStopReadyResultResponseBuilder.setCode(-1);
			rsSystemStopReadyResultResponseBuilder
					.setRsSystemStopReadyStatusResponse(MessageResponse.getRsSystemStopReadyStatusResponse(globalModel));
			return rsSystemStopReadyResultResponseBuilder.build();
		}

		if (globalModel.isSystemStopReady()) {
			rsSystemStopReadyResultResponseBuilder.setMsg("已有执行任务了,请不要重复操作!");
			rsSystemStopReadyResultResponseBuilder.setCode(-1);
			rsSystemStopReadyResultResponseBuilder
					.setRsSystemStopReadyStatusResponse(MessageResponse.getRsSystemStopReadyStatusResponse(globalModel));
			return rsSystemStopReadyResultResponseBuilder.build();
		}

		Date targetDate = DateUtils.addMinutes(MyDateUtil.getNow(), minute);
		globalModel.setSystemStopReady(true);
		globalModel.setStopDate(targetDate);
		ScheduledFuture future = GameSchedule.put(new SystemStopReadyRunnable(), minute * 60L, TimeUnit.SECONDS);
		globalModel.setSystemStopFuture(future);
		future = GameSchedule.put(new SystemStopReadyNoticeRunnable(), 60L, TimeUnit.SECONDS);
		globalModel.setSystemStopNoticeFuture(future);

		rsSystemStopReadyResultResponseBuilder.setMsg("操作成功!");
		rsSystemStopReadyResultResponseBuilder.setCode(0);
		rsSystemStopReadyResultResponseBuilder.setRsSystemStopReadyStatusResponse(MessageResponse.getRsSystemStopReadyStatusResponse(globalModel));
		return rsSystemStopReadyResultResponseBuilder.build();

	}

	/**
	 * 取消系统关闭
	 *
	 * @return
	 */
	public RsSystemStopReadyResultResponse systemStopCancel() {

		GlobalModel globalModel = PublicServiceImpl.getInstance().getGlobalModel();
		RsSystemStopReadyResultResponse.Builder rsSystemStopReadyResultResponseBuilder = RsSystemStopReadyResultResponse.newBuilder();
		if (!globalModel.isSystemStopReady()) {
			rsSystemStopReadyResultResponseBuilder.setMsg("当前没有执行任务,无需关闭!");
			rsSystemStopReadyResultResponseBuilder.setCode(-1);
			rsSystemStopReadyResultResponseBuilder
					.setRsSystemStopReadyStatusResponse(MessageResponse.getRsSystemStopReadyStatusResponse(globalModel));
			return rsSystemStopReadyResultResponseBuilder.build();
		}

		globalModel.setSystemStopReady(false);
		globalModel.setStopDate(null);

		ScheduledFuture scheduledFuture = globalModel.getSystemStopFuture();
		if (scheduledFuture != null) {
			scheduledFuture.cancel(false);
		}
		globalModel.setSystemStopFuture(null);

		scheduledFuture = globalModel.getSystemStopNoticeFuture();
		if (scheduledFuture != null) {
			scheduledFuture.cancel(false);
		}
		globalModel.setSystemStopNoticeFuture(null);

		rsSystemStopReadyResultResponseBuilder.setMsg("操作成功!");
		rsSystemStopReadyResultResponseBuilder.setCode(0);
		rsSystemStopReadyResultResponseBuilder.setRsSystemStopReadyStatusResponse(MessageResponse.getRsSystemStopReadyStatusResponse(globalModel));
		return rsSystemStopReadyResultResponseBuilder.build();
	}

	/**
	 * 刷新微信缓存
	 *
	 * @param account_id
	 * @return
	 */
	public RsRMIResultResponse flushWxCache(long account_id) {

		RsRMIResultResponse.Builder builder = RsRMIResultResponse.newBuilder();

		Account account = PublicServiceImpl.getInstance().getAccount(account_id);
		if (account == null) {
			builder.setCode(-1);
			builder.setMsg("账号不存在!");
			return builder.build();
		}

		AccountWeixinModel accountWeixinModel = account.getAccountWeixinModel();
		if (accountWeixinModel == null) {
			builder.setCode(-1);
			builder.setMsg("非微信账号,不能操作!");
			return builder.build();
		}

		JSONObject jsonObject = PtAPIServiceImpl.getInstance().wxFlushToken(account.getGame_id(), accountWeixinModel.getRefresh_token());
		// 刷新
		Integer errCode = 0;
		if (jsonObject.containsKey("errcode")) {
			errCode = (Integer) jsonObject.get("errcode");
		}
		if (errCode != 0) {
			builder.setCode(-1);
			builder.setMsg("请求微信接口刷新失败!");
			return builder.build();
		}

		String access_token = jsonObject.getString("access_token");
		String openid = jsonObject.getString("openid");
		String refresh_token = jsonObject.getString("refresh_token");
		String scope = jsonObject.getString("scope");

		jsonObject = PtAPIServiceImpl.getInstance().wxUserinfo(account.getGame_id(), access_token, openid);
		if (jsonObject == null) {
			builder.setCode(-1);
			builder.setMsg("请求微信获取玩家详细信息失败!");
			return builder.build();
		}

		if (jsonObject.containsKey("errcode")) {
			errCode = (Integer) jsonObject.get("errcode");
		}
		if (errCode != 0) {
			builder.setCode(-1);
			builder.setMsg("请求微信获取玩家详细信息失败,error=" + errCode);
			return builder.build();
		}

		openid = jsonObject.getString("openid");
		String nickname = jsonObject.getString("nickname");
		// nickname转码，过滤mysql识别不了的
		nickname = EmojiFilter.filterEmoji(nickname);
		// 长度控制
		nickname = MyStringUtil.substringByLength(nickname, AccountConstant.NICK_NAME_LEN);

		String sex = jsonObject.getString("sex");
		String province = jsonObject.getString("province");
		String city = jsonObject.getString("city");
		String country = jsonObject.getString("country");
		String headimgurl = jsonObject.getString("headimgurl");
		String privilege = jsonObject.getString("privilege");
		String unionid = jsonObject.getString("unionid");// 全平台唯一id

		accountWeixinModel.setAccess_token(access_token);
		accountWeixinModel.setRefresh_token(refresh_token);
		accountWeixinModel.setOpenid(openid);
		accountWeixinModel.setScope(scope);
		accountWeixinModel.setUnionid(unionid);
		accountWeixinModel.setNickname(nickname);
		accountWeixinModel.setSex(sex);
		accountWeixinModel.setProvince("");
		accountWeixinModel.setCity("");
		accountWeixinModel.setCountry("");
		accountWeixinModel.setHeadimgurl(headimgurl);
		accountWeixinModel.setPrivilege(privilege);
		accountWeixinModel.setLast_flush_time(new Date());
		// accountWeixinModel.setSelf_token(accountWeixinModel.getAccount_id() +
		// "-" + RandomUtil.getRandomString(20));
		// accountWeixinModel.setLast_false_self_token(new Date());
		accountWeixinModel.setNeedDB(true);

		builder.setCode(0);
		builder.setMsg("刷新成功!");
		return builder.build();
	}

	@Override
	public String getGameOrderID() {
		return IDGeneratorOrder.getInstance().getGamePayUniqueID();
	}

	@Override
	public String getDiamondUniqueID() {
		return IDGeneratorOrder.getInstance().getDiamondUniqueID();
	}

	@Override
	public void payCenterCall(String gameOrderID, int rechargeType) {
		Runnable run = new PayCenterRunnable(gameOrderID, rechargeType);
		if (gameOrderID.startsWith(IDGeneratorOrder.DIAMOND_INDEX)) {
			run = new PayCenterDiamondRunnable(gameOrderID, rechargeType);
		}
		GameSchedule.put(run, 10, TimeUnit.SECONDS);
	}

	@Override
	public boolean payCenterCallIOS(long accountID, String gameOrderID, String receipt, int rechargeType) {
		Runnable iosRunnable = new PayIosRunnable(accountID, gameOrderID, receipt, System.currentTimeMillis(), rechargeType);
		if (gameOrderID.startsWith(IDGeneratorOrder.DIAMOND_INDEX)) {
			iosRunnable = new PayIosDiamondRunnable(accountID, gameOrderID, receipt, System.currentTimeMillis(), rechargeType);
		}
		iosRunnable.run();
		return false;
	}

	@Override
	public boolean payCenterCallIOSByTransactionId(long accountID, String gameOrderID, String receipt, String transactionId, int rechargeType) {
		Runnable iosRunnable = new PayIosRunnable(accountID, gameOrderID, receipt, System.currentTimeMillis(), rechargeType, transactionId);

		if (gameOrderID.startsWith(IDGeneratorOrder.DIAMOND_INDEX)) {
			iosRunnable = new PayIosDiamondRunnable(accountID, gameOrderID, receipt, System.currentTimeMillis(), rechargeType, transactionId);
		}
		iosRunnable.run();
		return false;
	}

	@Override
	public String orderRepair(String orderID) {
		if (org.apache.commons.lang.StringUtils.isEmpty(orderID)) {
			return "gameOrderID 必填参数无效";
		}
		try {
			PayCenterRunnable payCenterRunnable = new PayCenterRunnable(orderID, 0);
			payCenterRunnable.run();

			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			Query query = new Query();
			query.addCriteria(Criteria.where("orderID").is(orderID));
			AddCardLog addCardLog = mongoDBService.getMongoTemplate().findOne(query, AddCardLog.class);
			if (addCardLog == null) {
				return "找不到订单";
			}
			return "订单修复状态" + addCardLog.getOrderStatus();
		} catch (Exception e) {
			logger.error("补单失败" + e);
		}
		return "";
	}

	@Override
	public String orderRepairIOS(long accountID, String orderID, String recepit) {
		if (org.apache.commons.lang.StringUtils.isEmpty(orderID)) {
			return "gameOrderID 必填参数无效";
		}
		try {
			PayIosRunnable payCenterRunnable = new PayIosRunnable(accountID, orderID, recepit, System.currentTimeMillis(), 0);
			payCenterRunnable.run();

			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			Query query = new Query();
			query.addCriteria(Criteria.where("orderID").is(orderID));
			AddCardLog addCardLog = mongoDBService.getMongoTemplate().findOne(query, AddCardLog.class);
			if (addCardLog == null) {
				return "找不到订单";
			}
			return "订单修复状态" + addCardLog.getOrderStatus();
		} catch (Exception e) {
			logger.error("补单失败" + e);
		}
		return "";
	}

	@Override
	public void everyDayRepair(int beforeDay) {
		TaskService taskService = SpringService.getBean(TaskService.class);
		taskService.everyDayAccount(beforeDay);
	}

	/**
	 * 增减玩家铜钱
	 *
	 * @param account_id
	 * @param money
	 * @param desc
	 * @return
	 */
	@Override
	public AddMoneyResultModel addAccountMoney(long account_id, int money, boolean isExceed, String desc, EMoneyOperateType eMoneyOperateType) {
		eMoneyOperateType = eMoneyOperateType == null ? EMoneyOperateType.UNKOWN_TYPE : eMoneyOperateType;
		return addAccountMoney(account_id, money, isExceed, desc, eMoneyOperateType.getId(), 0, null);
	}

	/**
	 * 增减玩家铜钱
	 *
	 * @param account_id
	 * @param money
	 * @param desc
	 * @return
	 */
	@Override
	public AddMoneyResultModel addAccountMoney(long account_id, int money, boolean isExceed, String desc, int eMoneyOperateTypeInt) {
		return addAccountMoney(account_id, money, isExceed, desc, eMoneyOperateTypeInt, 0, null);
	}

	@Override
	public AddMoneyResultModel addAccountMoney(long account_id, int money, boolean isExceed, String desc, EMoneyOperateType eMoneyOperateType,
			int toolsId) {
		eMoneyOperateType = eMoneyOperateType == null ? EMoneyOperateType.UNKOWN_TYPE : eMoneyOperateType;
		return addAccountMoney(account_id, money, isExceed, desc, eMoneyOperateType.getId(), toolsId, null);
	}

	@Override
	public AddMoneyResultModel addAccountMoney(long account_id, int money, boolean isExceed, String desc, EMoneyOperateType eMoneyOperateType,
			int toolsId, String strTip) {
		eMoneyOperateType = eMoneyOperateType == null ? EMoneyOperateType.UNKOWN_TYPE : eMoneyOperateType;
		return addAccountMoney(account_id, money, isExceed, desc, eMoneyOperateType.getId(), toolsId, strTip);
	}

	/**
	 * 增减玩家铜钱
	 *
	 * @param account_id
	 * @param money
	 * @param desc
	 * @return
	 */
	public AddMoneyResultModel addAccountMoney(long account_id, int money, boolean isExceed, String desc, int eMoneyOperateTypeInt, int toolsId,
			String strTip) {
		// eMoneyOperateType = eMoneyOperateType == null ?
		// EMoneyOperateType.UNKOWN_TYPE : eMoneyOperateType;

		AddMoneyResultModel addMoneyResultModel = new AddMoneyResultModel();
		addMoneyResultModel.setSuccess(false);

		Account account = this.getAccount(account_id);
		if (account == null) {
			addMoneyResultModel.setMsg("账号不存在");
			return addMoneyResultModel;
		}
		ReentrantLock lock = account.getRedisLock();
		lock.lock();
		try {
			AccountModel accountModel = account.getAccountModel();
			AccountWeixinModel wxModel = account.getAccountWeixinModel();
			addMoneyResultModel.setAccountModel(accountModel);
			if (wxModel != null) {
				addMoneyResultModel.setWxNickName(wxModel.getNickname());
			}
			long oldValue = accountModel.getMoney();

			if (money > 0) {
				accountModel.setMoney(accountModel.getMoney() + money);
				addMoneyResultModel.setSuccess(true);
				// addMoneyResultModel.setRealMoney(money);
				AccountParamModel accountParamModel = PublicServiceImpl.getInstance().getAccountParamModel(account_id,
						EAccountParamType.TODAY_ADD_MONEY);
				if (accountParamModel != null) {
					accountParamModel.setLong1(accountParamModel.getLong1() + money);
					accountParamModel.setNeedDB(true);
					// DataThreadPool.getInstance().addTask(new
					// DBUpdateDto(DbStoreType.PUBLIC, DbOpType.UPDATE,
					// "updateAccountParamModel", accountParamModel));
				}
			} else {
				long k = accountModel.getMoney() + money;

				if (!isExceed) {
					if (k < 0) {
						addMoneyResultModel.setMsg("库存不足");
						return addMoneyResultModel;
					}
				}

				if (k < 0) {
					// addMoneyResultModel.setRealMoney(-accountModel.getMoney());
					accountModel.setMoney(0L);
				} else {
					accountModel.setMoney(k);
					// addMoneyResultModel.setRealMoney(-money);
				}

				addMoneyResultModel.setSuccess(true);

				AccountParamModel accountParamModel = PublicServiceImpl.getInstance().getAccountParamModel(account_id,
						EAccountParamType.TODAY_DES_MONEY);
				if (accountParamModel != null) {
					accountParamModel.setLong1(accountParamModel.getLong1() - money);
					accountParamModel.setNeedDB(true);
				}

			}

			// 房卡操作直接入库
			// DataThreadPool.getInstance().addTask(new
			// DBUpdateDto(DbStoreType.PUBLIC, DbOpType.UPDATE,
			// "updateAccountModel", accountModel));
			if (money != 0)
				accountModel.setNeedDB(true);

			long change = accountModel.getMoney() - oldValue;
			if (change != 0) {
				if (EMoneyOperateType.USE_PROP.getId() == eMoneyOperateTypeInt) {
					MongoDBServiceImpl.getInstance().log_item_money(account_id, ELogType.addMoney, desc, change, (long) eMoneyOperateTypeInt,
							accountModel.getMoney() + "", toolsId);
				} else {
					MongoDBServiceImpl.getInstance().log_use_item(account_id, ELogType.addMoneyNew, desc, change, (long) eMoneyOperateTypeInt,
							accountModel.getMoney() + "");
				}

			}

			if (money != 0) {
				// ========同步到中心========
				RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
				redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
				//
				RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
				rsAccountResponseBuilder.setAccountId(account_id);
				//
				RsAccountModelResponse.Builder rsAccountModelResponseBuilder = RsAccountModelResponse.newBuilder();
				rsAccountModelResponseBuilder.setMoney(accountModel.getMoney());
				// rsAccountModelResponseBuilder.setHistoryPayGold(accountModel.getHistory_pay_gold());
				rsAccountModelResponseBuilder.setMoneyChangeType(eMoneyOperateTypeInt);
				if (strTip != null) {
					rsAccountModelResponseBuilder.setStrTip(strTip);
				}

				rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponseBuilder);

				//
				redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
				RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
						ERedisTopicType.topicProxy.getId() + account.getLastProxyIndex());
			}

		} catch (Exception e) {
			logger.error("error", e);
		} finally {
			lock.unlock();
		}

		return addMoneyResultModel;
	}

	/**
	 * 添加历史牌局次数
	 *
	 * @param num
	 * @return
	 */
	public boolean addHistorySamllBrandTimes(long account_id, int num) {

		Account account = this.getAccount(account_id);
		if (account == null)
			return false;

		AccountParamModel accountParamModel = PublicServiceImpl.getInstance().getAccountParamModel(account_id,
				EAccountParamType.HISTORY_SAMLL_BRAND_TIMES);
		if (accountParamModel != null) {
			accountParamModel.setVal1(accountParamModel.getVal1() + num);
			accountParamModel.setNeedDB(true);
		}

		return true;
	}

	/**
	 * 添加历史大牌局次数
	 *
	 * @param num
	 * @return
	 */
	public boolean addHistoryBigBrandTimes(long account_id, int num) {
		Account account = this.getAccount(account_id);
		if (account == null)
			return false;

		AccountParamModel accountParamModel = PublicServiceImpl.getInstance().getAccountParamModel(account_id,
				EAccountParamType.HISTORY_BIG_BRAND_TIMES);
		if (accountParamModel != null) {
			accountParamModel.setVal1(accountParamModel.getVal1() + num);
			accountParamModel.setNeedDB(true);
		}

		return true;
	}

	/**
	 * 新增邀请记录
	 *
	 * @param accountRecommendModel
	 */
	public boolean addAccountRecommendModel(AccountRecommendModel accountRecommendModel) {
		try {
			long account_id = accountRecommendModel.getAccount_id();
			Account account = this.getAccount(account_id);
			if (account == null)
				return false;
			account.getRecommendRelativeModel().incre();
			AccountRecommendModel model = account.getAccountRecommendModelMap().get(accountRecommendModel.getTarget_account_id());
			if (model != null) {
				logger.warn("玩家:{} 已经推荐过 玩家:{},不能重复推荐!!", account_id, accountRecommendModel.getTarget_account_id());
				return false;
			}

			if (accountRecommendModel.getTarget_account_id() == account_id) {
				logger.warn("玩家:{} ，不可以推荐自己!!", account_id);
				return false;
			}
			// 设置株洲麻将协会推荐人
			ZZPromoterService.getInstance().addPromoterObject(accountRecommendModel.getAccount_id(), accountRecommendModel.getTarget_account_id());
			// 入库
			DataThreadPool.getInstance()
					.addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.INSERT, "insertAccountRecommendModel", accountRecommendModel));
			account.getAccountRecommendModelMap().put(accountRecommendModel.getTarget_account_id(), accountRecommendModel);

			// ========同步到中心========
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			//
			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			rsAccountResponseBuilder.setAccountId(account.getAccount_id());
			//
			RsAccountRecommendResponse.Builder rsAccountRecommendResponse = RsAccountRecommendResponse.newBuilder();
			rsAccountRecommendResponse.setAccountId(accountRecommendModel.getAccount_id());
			rsAccountRecommendResponse.setTargetAccountId(accountRecommendModel.getTarget_account_id());
			rsAccountRecommendResponse.setCreateTime(accountRecommendModel.getCreate_time().getTime());
			rsAccountRecommendResponse.setGoldNum(accountRecommendModel.getGold_num());
			rsAccountRecommendResponse.setTargetName(accountRecommendModel.getTarget_name());
			rsAccountRecommendResponse.setTargetIcon(accountRecommendModel.getTarget_icon());
			rsAccountResponseBuilder.addRsAccountRecommendResponseList(rsAccountRecommendResponse);
			//
			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
					ERedisTopicType.topicProxy.getId() + account.getLastProxyIndex());
			return true;

		} catch (Exception e) {
			logger.error("error", e);
			return false;
		}
	}

	/**
	 * 新增邀请记录
	 *
	 * @param process
	 *            修改渠道0自己默认的，1管理后台修改
	 * @param accountRecommendModel
	 */
	public boolean addAgentRecommendModel(AgentRecommendModel agentRecommendModel, int process) {
		try {
			long account_id = agentRecommendModel.getAccount_id();
			Account account = this.getAccount(account_id);
			if (account == null)
				return false;
			if (account.getAgentRecommendModelMap() != null) {
				AgentRecommendModel model = account.getAgentRecommendModelMap().get(agentRecommendModel.getTarget_account_id());
				if (model != null && process == 0) {
					logger.warn("玩家:{} 已经推荐过 玩家:{},不能重复推荐!!", account_id, agentRecommendModel.getTarget_account_id());
					return false;
				}
			}
			if (agentRecommendModel.getTarget_account_id() == account_id) {
				logger.warn("玩家:{} ，不可以推荐自己!!", account_id);
				return false;
			}
			// 入库
			DataThreadPool.getInstance()
					.addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.INSERT, "insertAgentRecommendModel", agentRecommendModel));
			account.getAgentRecommendModelMap().put(agentRecommendModel.getTarget_account_id(), agentRecommendModel);
			return true;

		} catch (Exception e) {
			logger.error("error", e);
			return false;
		}
	}

	public boolean addHallRecommendModel(HallRecommendModel hallRecommendModel) {
		try {
			if (ZZPromoterService.getInstance().getAccountZZPromoterModel(hallRecommendModel.getTarget_account_id()) != null) {
				return false;// 株洲协会的推广对象不能设置上级推广员
			}
			long account_id = hallRecommendModel.getAccount_id();
			if (account_id != 0) {
				Account account = this.getAccount(account_id);
				if (account == null)
					return false;
				if (account.getHallRecommendModelMap() != null) {
					HallRecommendModel model = account.getHallRecommendModelMap().get(hallRecommendModel.getTarget_account_id());
					if (model != null) {
						logger.warn("玩家:{} 已经推荐过 玩家:{},不能重复推荐!!", account_id, hallRecommendModel.getTarget_account_id());
						return false;
					}
				}
				account.getHallRecommendModelMap().put(hallRecommendModel.getTarget_account_id(), hallRecommendModel);
				account.getRecommendRelativeModel().incre();// 推荐人数+1
				// SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT,
				// account_id + "", account);// 更新redis缓存
			}
			if (hallRecommendModel.getTarget_account_id() == account_id) {
				logger.warn("玩家:{} ，不可以推荐自己!!", account_id);
				return false;
			}
			// 入库
			DataThreadPool.getInstance()
					.addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.INSERT, "insertHallRecommendModel", hallRecommendModel));
			Account targetAccount = this.getAccount(hallRecommendModel.getTarget_account_id());
			targetAccount.setHallRecommendModel(hallRecommendModel);
			// SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT,
			// hallRecommendModel.getTarget_account_id() + "", targetAccount);
			// ========同步到中心========
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			rsAccountResponseBuilder.setAccountId(hallRecommendModel.getTarget_account_id());
			RsHallRecommendResponse.Builder rsHallRecommendResponse = RsHallRecommendResponse.newBuilder();
			rsHallRecommendResponse.setAccountId(hallRecommendModel.getAccount_id());
			rsHallRecommendResponse.setTargetAccountId(hallRecommendModel.getTarget_account_id());
			// rsHallRecommendResponse.setCreateTime(hallRecommendModel.getCreate_time().getTime());
			rsHallRecommendResponse.setProxyLevel(hallRecommendModel.getProxy_level());
			rsAccountResponseBuilder.setRsHallRecommendResponse(rsHallRecommendResponse);
			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
					ERedisTopicType.topicProxy.getId() + targetAccount.getLastProxyIndex());
			return true;

		} catch (Exception e) {
			logger.error("error", e);
			return false;
		}
	}

	//
	public boolean updateHallRecommendModel(HallRecommendModel hallRecommendModel) {
		try {
			// 更新
			DataThreadPool.getInstance()
					.addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.UPDATE, "updateHallRecommendLevel", hallRecommendModel));
			Account targetAccount = this.getAccount(hallRecommendModel.getTarget_account_id());
			targetAccount.setHallRecommendModel(hallRecommendModel);
			// 更新上级缓存
			if (hallRecommendModel.getAccount_id() != 0) {
				Account upAccount = this.getAccount(hallRecommendModel.getAccount_id());
				if (upAccount != null) {
					upAccount.getHallRecommendModelMap().put(hallRecommendModel.getTarget_account_id(), hallRecommendModel);
				}
			}
			// SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT,
			// hallRecommendModel.getTarget_account_id() + "", targetAccount);
			// ========同步到中心========
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			rsAccountResponseBuilder.setAccountId(hallRecommendModel.getTarget_account_id());
			RsHallRecommendResponse.Builder rsHallRecommendResponse = RsHallRecommendResponse.newBuilder();
			rsHallRecommendResponse.setAccountId(hallRecommendModel.getAccount_id());
			rsHallRecommendResponse.setTargetAccountId(hallRecommendModel.getTarget_account_id());
			rsHallRecommendResponse.setCreateTime(hallRecommendModel.getCreate_time().getTime());
			rsHallRecommendResponse.setProxyLevel(hallRecommendModel.getProxy_level());
			rsAccountResponseBuilder.setRsHallRecommendResponse(rsHallRecommendResponse);
			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
					ERedisTopicType.topicProxy.getId() + targetAccount.getLastProxyIndex());
			return true;

		} catch (Exception e) {
			logger.error("error", e);
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.cai.common.rmi.ICenterRMIServer#addAccountRMB(long, int,
	 * boolean, java.lang.String, com.cai.common.define.ERmbOperateType)
	 */
	@Override
	public AddGoldResultModel addAccountRMB(long account_id, double rmb, boolean isExceed, String desc, EGoldOperateType egoldOperateType) {

		egoldOperateType = egoldOperateType == null ? EGoldOperateType.UNKOWN_TYPE : egoldOperateType;

		AddGoldResultModel addGoldResultModel = new AddGoldResultModel();
		addGoldResultModel.setSuccess(false);

		Account account = this.getAccount(account_id);
		if (account == null) {
			addGoldResultModel.setMsg("账号不存在");
			return addGoldResultModel;
		}
		ReentrantLock lock = account.getRedisLock();
		lock.lock();
		try {
			AccountModel accountModel = account.getAccountModel();
			AccountWeixinModel wxModel = account.getAccountWeixinModel();
			addGoldResultModel.setAccountModel(accountModel);
			if (wxModel != null) {
				addGoldResultModel.setWxNickName(wxModel.getNickname());
			}
			double oldValue = accountModel.getRmb();

			if (rmb > 0) {
				BigDecimal bd = new BigDecimal(accountModel.getRmb() + rmb).setScale(2, BigDecimal.ROUND_HALF_UP);
				accountModel.setRmb(bd.doubleValue());
				BigDecimal history = new BigDecimal(accountModel.getHistory_rmb() + rmb).setScale(2, BigDecimal.ROUND_HALF_UP);
				accountModel.setHistory_rmb(history.doubleValue());
				addGoldResultModel.setSuccess(true);

			} else {
				BigDecimal bd = new BigDecimal(accountModel.getRmb() + rmb).setScale(2, BigDecimal.ROUND_HALF_UP);
				double k = bd.doubleValue();
				if (k < 0) {
					addGoldResultModel.setMsg("库存不足");
					return addGoldResultModel;
				} else {
					accountModel.setRmb(k);
				}

				BigDecimal receive = new BigDecimal(accountModel.getReceive_rmb() - rmb).setScale(2, BigDecimal.ROUND_HALF_UP);
				accountModel.setReceive_rmb(receive.doubleValue());
				addGoldResultModel.setSuccess(true);

			}

			// 房卡操作直接入库
			DataThreadPool.getInstance().addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.UPDATE, "updateAccountModel", accountModel));
			double change = new BigDecimal((accountModel.getRmb() - oldValue) * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();// 元换分入mongodb
			double newValue = new BigDecimal(accountModel.getRmb() * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
			// 日志
			StringBuffer buf = new StringBuffer();
			buf.append("|");
			if (change > 0) {
				buf.append("增加分[" + change + "]");
			} else {
				buf.append("减少分[" + change + "]");
			}
			buf.append(",值变化:[").append(oldValue).append("]->[").append(newValue).append("]分");
			desc = desc + buf.toString();
			MongoDBServiceImpl.getInstance().log(account_id, ELogType.rmb, desc, (long) change, (long) egoldOperateType.getId(), null,
					(long) oldValue, (long) newValue);

			// ========同步到中心========
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			//
			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			rsAccountResponseBuilder.setAccountId(account_id);
			//
			RsAccountModelResponse.Builder rsAccountModelResponseBuilder = RsAccountModelResponse.newBuilder();
			rsAccountModelResponseBuilder.setRmb(accountModel.getRmb());
			rsAccountModelResponseBuilder.setHistoryRmb(accountModel.getHistory_rmb());
			rsAccountModelResponseBuilder.setReceiveRmb(accountModel.getReceive_rmb());
			rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponseBuilder);
			//
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

	/*
	 * (non-Javadoc)
	 *
	 * @see com.cai.common.rmi.ICenterRMIServer#reloadVailGoodsList()
	 */
	@Override
	public void reloadVailGoodsList() {
		GoodsDict.getInstance().load();
		// ========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
		//
		RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
		rsDictUpdateResponseBuilder.setRsDictType(RsDictType.GOODS);
		//
		redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topProxAndLogic);

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.cai.common.rmi.ICenterRMIServer#reLoadIPModelDictionary()
	 */
	@Override
	public boolean reLoadIPModelDictionary() {
		IPGroupDict.getInstance().load();
		// ========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
		//
		RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
		rsDictUpdateResponseBuilder.setRsDictType(RsDictType.IP_LIST);
		//
		redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.cai.common.rmi.ICenterRMIServer#payCenterCallIOS(java.lang.String,
	 * java.lang.String)
	 */

	// public boolean addAccountPrxoyPlayerRoomModel(PrxoyPlayerRoomModel
	// prxoyPlayerRoomModel){
	// long account_id = prxoyPlayerRoomModel.getCreate_account_id();
	// Account account = this.getAccount(account_id);
	// if(account==null)
	// return false;
	// account.getProxRoomMap().put(prxoyPlayerRoomModel.getRoom_id(),
	// prxoyPlayerRoomModel);
	// return true;
	// }
	@Override
	public AddGoldResultModel doRecommendIncome(long account_id, double income, long level, String desc, EGoldOperateType eGoldOperateType,
			long targetId) {
		AddGoldResultModel addGoldResultModel = new AddGoldResultModel();
		addGoldResultModel.setSuccess(false);
		Account account = this.getAccount(account_id);
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
				MongoDBServiceImpl.getInstance().log_recommend(account_id, ELogType.recommendIncome, desc, (long) change, level, null, targetId, 0);
			} else {
				buf.append("减少[" + change + "]");
				buf.append(",值变化:[").append(oldValue * 100.0).append("]->[").append(newValue).append("]分");
				desc = desc + buf.toString();
				MongoDBServiceImpl.getInstance().log_recommend(account_id, ELogType.recommendOutcome, desc, (long) change, level, null, targetId, 0);
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

	@Override
	public AddGoldResultModel doRecommendIncome(long account_id, double income, long level, String desc, EGoldOperateType eGoldOperateType,
			long targetId, long money) {
		AddGoldResultModel addGoldResultModel = new AddGoldResultModel();
		addGoldResultModel.setSuccess(false);
		Account account = this.getAccount(account_id);
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
				MongoDBServiceImpl.getInstance().log_recommend(account_id, ELogType.recommendIncome, desc, (long) change, level, null, targetId,
						money);
			} else {
				buf.append("减少[" + change + "]");
				buf.append(",值变化:[").append(oldValue * 100.0).append("]->[").append(newValue).append("]分");
				desc = desc + buf.toString();
				MongoDBServiceImpl.getInstance().log_recommend(account_id, ELogType.recommendOutcome, desc, (long) change, level, null, targetId,
						money);
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

	@Override
	public AddGoldResultModel updateYesterdayRecommendIncome(long account_id, double yesterday_income) {

		AddGoldResultModel addGoldResultModel = new AddGoldResultModel();
		addGoldResultModel.setSuccess(false);
		if (yesterday_income < 0) {
			addGoldResultModel.setMsg("收益不能为负数");
			return addGoldResultModel;
		}
		Account account = this.getAccount(account_id);
		if (account == null) {
			addGoldResultModel.setMsg("账号不存在");
			return addGoldResultModel;
		}
		ReentrantLock lock = account.getRedisLock();
		lock.lock();
		try {
			AccountModel accountModel = account.getAccountModel();
			addGoldResultModel.setAccountModel(accountModel);
			addGoldResultModel.setSuccess(true);
			double oldYesterdayIncome = accountModel.getRecommend_yesterday_income();
			if (oldYesterdayIncome == yesterday_income) {
				return addGoldResultModel;
			} else {
				accountModel.setRecommend_yesterday_income(yesterday_income);
			}
			// 昨日收益直接入库
			DataThreadPool.getInstance().addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.UPDATE, "updateAccountModel", accountModel));
			// SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT,
			// account_id + "", account);// 更新缓存
			// ========同步到中心========
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			rsAccountResponseBuilder.setAccountId(account_id);
			RsAccountModelResponse.Builder rsAccountModelResponseBuilder = RsAccountModelResponse.newBuilder();
			rsAccountModelResponseBuilder.setRecommendYesterdayIncome(yesterday_income);
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
		Account account = this.getAccount(account_id);
		if (account == null) {
			return null;
		}
		AccountModel accountModel = account.getAccountModel();
		int level = accountModel.getRecommend_level();
		if (level == 0) {
			return null;
		}
		Map<Long, AccountRecommendModel> recommendMap = account.getAccountRecommendModelMap();
		if (recommendMap == null || recommendMap.size() == 0) {
			return null;
		}
		Map<String, Object> map = Maps.newConcurrentMap();
		map.put("remainIncome", accountModel.getRecommend_remain_income());// 可提现
		map.put("receiveIncome", accountModel.getRecommend_receive_income());// 已经提现
		map.put("historyIncome", accountModel.getRecommend_history_income());// 总收益记录
		map.put("level", level);// 玩家的等级
		if (level == 1) {
			int dsecondRecommendSize = 0;// 一级推广员人数
			int thirdSize = 0;// 二级推广员人数
			int agentCount = 0;
			int playCount = 0;
			int agentRecommender = 0;// 推广代理
			for (AccountRecommendModel model : recommendMap.values()) {
				Account targetAccount = this.getAccount(model.getTarget_account_id());
				AccountModel targetAccountModel = targetAccount.getAccountModel();
				if (targetAccountModel.getRecommend_level() == 0) {
					if (targetAccountModel.getIs_agent() > 0) {
						agentCount++;
					} else {
						playCount++;
					}
				} else
				// 下级玩家是否为推广员
				if (model.getRecommend_level() > 0) {
					if (targetAccountModel.getIs_agent() > 0) {
						agentRecommender++;
					} else {
						dsecondRecommendSize++;
					}
					// 需要判断是否为下级推广员
					if (targetAccount.getAccountRecommendModelMap() != null) {
						Map<Long, AccountRecommendModel> recommendDownMap = targetAccount.getAccountRecommendModelMap();
						for (AccountRecommendModel downmmodel : recommendDownMap.values()) {
							if (downmmodel.getRecommend_level() > 0) {
								thirdSize++;
							}
						}
					}
				}
			}
			Map<String, Object> recommendDetailMap = this.getRecommendTotal(account);
			map.putAll(recommendDetailMap);
			map.put("secondRecommendCount", dsecondRecommendSize);
			map.put("thirdRecommendCount", thirdSize);
			map.put("totalCount", dsecondRecommendSize + thirdSize + agentCount + playCount + agentRecommender);
			map.put("agentCount", agentCount);
			map.put("playCount", playCount);
			map.put("recommendAgentCount", agentRecommender);
			return map;
		} else if (level == 2) {
			int thirdSize = 0;// 二级推广员人数
			int agentCount = 0;// 代理人数
			int playCount = 0;// 玩家人数
			int agentRecommender = 0;// 推广代理
			for (AccountRecommendModel model : recommendMap.values()) {
				Account targetAccount = this.getAccount(model.getTarget_account_id());
				AccountModel targetAccountModel = targetAccount.getAccountModel();
				if (targetAccountModel.getIs_agent() > 0) {
					if (targetAccountModel.getRecommend_level() > 0) {
						agentRecommender++;
					} else {
						agentCount++;
					}
				} else {
					if (targetAccountModel.getRecommend_level() == 0) {
						playCount++;
					} else {
						thirdSize++;
					}
				}
			}
			Map<String, Object> recommendDetailMap = this.getRecommendTotal(account);
			map.putAll(recommendDetailMap);
			map.put("thirdRecommendCount", thirdSize);
			map.put("totalCount", thirdSize + agentCount + playCount + agentRecommender);
			map.put("agentCount", agentCount);
			map.put("playCount", playCount);
			map.put("recommendAgentCount", agentRecommender);
			return map;
		} else if (level == 3) {
			int agentCount = 0;
			int playCount = 0;
			for (AccountRecommendModel model : recommendMap.values()) {
				Account targetAccount = this.getAccount(model.getTarget_account_id());
				AccountModel targetAccountModel = targetAccount.getAccountModel();
				if (targetAccountModel.getRecommend_level() == 0) {
					if (targetAccountModel.getIs_agent() > 0) {
						agentCount++;
					} else {
						playCount++;
					}
				}
			}
			Map<String, Object> recommendDetailMap = this.getRecommendTotal(account);
			map.putAll(recommendDetailMap);
			map.put("totalCount", agentCount + playCount);
			map.put("agentCount", agentCount);
			map.put("playCount", playCount);
			return map;
		}
		return null;
	}

	public Map<String, Object> getRecommendTotal(Account targetAccount) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("account_id").is(targetAccount.getAccount_id()));
		query.addCriteria(Criteria.where("log_type").is(ELogType.recommendIncome.getId()));// 推广员获得的收益
		List<GameRecommend> recommendIncomeList = mongoDBService.getMongoTemplate().find(query, GameRecommend.class);
		double secondIncome = 0.0;// 一级推广员获利
		double secondReceive = 0.0;// 一级推广员返利
		double thirdIncome = 0.0;// 二级推广员获利
		double thirdReceive = 0.0;// 二级推广员返利
		double agentIncome = 0.0;// 代理获利
		// double agentRechargeReceive = 0;// 代理充值返利
		double playIncome = 0;// 玩家获利
		AccountModel accountModel = targetAccount.getAccountModel();
		if (accountModel.getRecommend_level() == 1) {
			for (GameRecommend model : recommendIncomeList) {
				if (model.getV2() == 0l) {// 自己发展的玩家获利
					playIncome += model.getV1();
				} else if (model.getV2() == 2l) {// 下级推广员发展玩家获利
					secondIncome += model.getV1();
				} else if (model.getV2() == 3l) {// 下下级推广员发展玩家获利
					thirdIncome += model.getV1();
				} else if (model.getV2() == 4l) {// 自己的代理充值返利
					agentIncome += model.getV1();
				} else if (model.getV2() == 5l) {// 下级推广员充值返利
					secondReceive += model.getV1();
				} else if (model.getV2() == 6l) {// 下下级推广员充值返利
					thirdReceive += model.getV1();
				}
			}
		} else if (accountModel.getRecommend_level() == 2) {
			for (GameRecommend model : recommendIncomeList) {
				if (model.getV2() == 0l) {// 自己发展的玩家获利
					playIncome += model.getV1();
				} else if (model.getV2() == 3l) {// 下级发展玩家获利
					thirdIncome += model.getV1();
				} else if (model.getV2() == 4l) {// 自己的代理充值返利
					agentIncome += model.getV1();
				} else if (model.getV2() == 6l) {// 下级代理充值返利
					thirdReceive += model.getV1();
				}
			}
		} else if (accountModel.getRecommend_level() == 3) {
			for (GameRecommend model : recommendIncomeList) {
				if (model.getV2() == 0l) {// 自己发展的玩家获利
					playIncome += model.getV1();
				} else if (model.getV2() == 4l) {// 自己的代理充值返利
					agentIncome += model.getV1();
				}
			}
		}
		Map<String, Object> map = Maps.newConcurrentMap();
		map.put("secondIncome", secondIncome / 100.0);
		map.put("secondReceive", secondReceive / 100.0);
		map.put("thirdIncome", thirdIncome / 100.0);
		map.put("thirdReceive", thirdReceive / 100.0);
		map.put("agentIncome", agentIncome / 100.0);// 我的代理收益
		map.put("playIncome", playIncome / 100.0);// 我的玩家收益
		map.put("totalReturnBalance", (secondReceive + thirdReceive + agentIncome) / 100.0);// 总的返利
		map.put("totalGetBalance", (secondIncome + thirdIncome + playIncome) / 100.0);// 总的获益
		return map;
	}

	// 查询下级推广员详情
	public Map<String, Object> queryDownRecommendByMonth(long account_id, String startTime, String endTime) {
		Date startDate = null;
		Date endDate = null;
		try {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
			startDate = dateFormat.parse(startTime);
			endDate = dateFormat.parse(endTime);
			// startDate = MyDateUtil.getMinMonthDate(startTime);
			// endDate = MyDateUtil.getMaxMonthDate(endTime);
		} catch (Exception e) {
			return null;
		}

		Account account = this.getAccount(account_id);
		if (account == null) {
			return null;
		}

		String start = DateFormatUtils.format(startDate, "yyyy-MM-dd");
		String end = DateFormatUtils.format(endDate, "yyyy-MM-dd");
		int level = account.getAccountModel().getRecommend_level();
		if (level == 0) {
			return null;
		}
		Map<Long, AccountRecommendModel> recommendMap = account.getAccountRecommendModelMap();
		if (recommendMap == null || recommendMap.size() == 0) {
			return null;
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd");
		SimpleDateFormat timeStr = new SimpleDateFormat("mm-dd");
		// 在从缓存中取值的时候，就根据日期进行排序
		List<AccountRecommendModel> lisRecommend = new ArrayList<AccountRecommendModel>(recommendMap.values());// 所有的推广员
		Collections.sort(lisRecommend, new Comparator<AccountRecommendModel>() {
			public int compare(AccountRecommendModel o1, AccountRecommendModel o2) {
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
		// Map<String, Object> details = null;
		if (level == 1) {
			int downRecommendSize = 0;
			for (AccountRecommendModel model : lisRecommend) {
				// details = Maps.newConcurrentMap();
				// 下级玩家是否在当前查询月份内
				if (model.getRecommend_level() > 0) {
					if (model.getUpdate_time().compareTo(start) >= 0 && model.getUpdate_time().compareTo(end) <= 0) {
						Map<String, Object> details = null;
						details = Maps.newConcurrentMap();
						downRecommendSize++;
						long targetId = model.getTarget_account_id();
						Account targetAccount = this.getAccount(targetId);
						// 需要判断是否为下级推广员
						String newTimeStr = null;
						try {
							Date update = sdf.parse(model.getUpdate_time());
							newTimeStr = timeStr.format(update);
						} catch (ParseException e) {
							e.printStackTrace();
						}
						details.put("date", newTimeStr);
						details.put("accountId", model.getAccount_id());// 推荐人Id
						Account subAccount = this.getAccount(model.getTarget_account_id());
						if (subAccount.getAccountWeixinModel() != null) {
							details.put("nickName", subAccount.getAccountWeixinModel().getNickname());
						} else {
							details.put("nickName", "-");
						}
						details.put("targetAccountId", model.getTarget_account_id());// 游戏Id
						if (targetAccount.getAccountRecommendModelMap() == null) {
							details.put("recommendCount", 0);
						} else {
							Map<Long, AccountRecommendModel> recommendDownMap = targetAccount.getAccountRecommendModelMap();
							int size = 0;
							int agentCount = 0;
							for (AccountRecommendModel downModel : recommendDownMap.values()) {
								Account downAccount = this.getAccount(downModel.getTarget_account_id());
								if (downModel.getRecommend_level() > 0) {
									size++;
								} else if (downAccount.getAccountModel().getIs_agent() > 0) {
									agentCount++;
								}
							}
							details.put("agentCount", agentCount);
							details.put("recommendCount", size);
						}
						list.add(details);
					}

					// list.add(details);
				}
			}
			detailsParam.put("tatalRecommendCount", downRecommendSize);
			detailsParam.put("data", list);
		}
		if (level == 2) {
			int downRecommendSize = 0;
			for (AccountRecommendModel model : lisRecommend) {
				// details = Maps.newConcurrentMap();
				// 下级玩家是否在当前查询月份内
				if (model.getRecommend_level() > 0) {
					if (model.getUpdate_time().compareTo(start) >= 0 && model.getUpdate_time().compareTo(end) <= 0) {
						Map<String, Object> details = null;
						details = Maps.newConcurrentMap();
						downRecommendSize++;
						// 需要判断是否为下级推广员
						String newTimeStr = null;
						try {
							Date update = sdf.parse(model.getUpdate_time());
							newTimeStr = timeStr.format(update);
						} catch (ParseException e) {
							e.printStackTrace();
						}
						details.put("date", newTimeStr);
						details.put("accountId", model.getAccount_id());
						Account subAccount = this.getAccount(model.getTarget_account_id());
						if (subAccount.getAccountWeixinModel() != null) {
							details.put("nickName", subAccount.getAccountWeixinModel().getNickname());
						} else {
							details.put("nickName", "-");
						}
						details.put("targetAccountId", model.getTarget_account_id());
						Account targetAccount = this.getAccount(model.getTarget_account_id());
						details.put("recommendCount", 0);
						int agentCount = 0;
						if (targetAccount.getAccountRecommendModelMap() != null) {
							Map<Long, AccountRecommendModel> recommendDownMap = targetAccount.getAccountRecommendModelMap();
							for (AccountRecommendModel downModel : recommendDownMap.values()) {
								if (this.getAccount(downModel.getTarget_account_id()).getAccountModel().getIs_agent() > 0) {
									agentCount++;
								}
							}
						}
						details.put("agentCount", agentCount);
						list.add(details);
					}

				}
				// list.add(details);
			}
			detailsParam.put("tatalRecommendCount", downRecommendSize);
			detailsParam.put("data", list);
		}

		return detailsParam;
	}

	/**
	 * 我的会员玩家
	 *
	 * @param account_id
	 * @param date
	 * @return
	 */
	public Map<String, Object> queryMyMemberByMonth(long account_id, String date) {
		Date startDate = null;
		Date endDate = null;
		try {
			startDate = MyDateUtil.getMinMonthDate(date);
			endDate = MyDateUtil.getMaxMonthDate(date);
		} catch (Exception e) {
			return null;
		}

		Account account = this.getAccount(account_id);
		if (account == null) {
			return null;
		}

		Map<String, Object> map = Maps.newConcurrentMap();

		// 获取玩家所有的下级推广员
		Map<Long, AccountRecommendModel> recommendMap = account.getAccountRecommendModelMap();
		List<AccountRecommendModel> list = new ArrayList<AccountRecommendModel>(recommendMap.values());
		List<AccountRecommendModel> newList = new ArrayList<AccountRecommendModel>();// 用来存放新的集合
		List<AccountRecommendModel> everyMonthList = new ArrayList<AccountRecommendModel>();// 用来存放每个月的查询数据
		for (AccountRecommendModel model : list) {
			AccountModel targetAccountModel = this.getAccount(model.getTarget_account_id()).getAccountModel();
			// 满足的条件是玩家的推广级别为0并且不是代理
			System.out.println("下级推官员的ID为：" + targetAccountModel.getAccount_id());
			System.out.println("下级推官员级别为：" + targetAccountModel.getRecommend_level());
			if (targetAccountModel.getRecommend_level() == 0 && targetAccountModel.getIs_agent() == 0) {
				newList.add(model);
			}
		}
		// map.put("totalCount", newList.size());

		// 过滤后的进行日期的查询
		String start = DateFormatUtils.format(startDate, "yyyy-MM-dd");
		String end = DateFormatUtils.format(endDate, "yyyy-MM-dd");
		for (AccountRecommendModel model : newList) {
			String createTime = DateFormatUtils.format(model.getCreate_time(), "yyyy-MM-dd");
			if (createTime.compareTo(start) >= 0 && createTime.compareTo(end) <= 0) {
				everyMonthList.add(model);
			}
		}
		map.put("totalCount", everyMonthList.size());

		Collections.sort(everyMonthList, new Comparator<AccountRecommendModel>() {
			public int compare(AccountRecommendModel o1, AccountRecommendModel o2) {
				// 按照时间进行排列
				Long time1 = o1.getCreate_time().getTime() / 1000;
				Long time2 = o2.getCreate_time().getTime() / 1000;
				if (time1 < time2) {
					return 1;
				}
				if (time1 == time2) {
					return 0;
				}
				return -1;
			}
		});

		// 封装成需要的数据
		List<Map<String, Object>> defineList = new ArrayList<Map<String, Object>>();
		Map<String, Object> newMap = null;
		for (AccountRecommendModel model : everyMonthList) {
			newMap = Maps.newConcurrentMap();
			newMap.put("accountId", model.getAccount_id());
			newMap.put("targetAccount", model.getTarget_account_id());

			String re_StrTime = null;
			SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");
			long lcc_time = Long.valueOf(model.getCreate_time().getTime() / 1000);
			re_StrTime = sdf.format(new Date(lcc_time * 1000L));
			newMap.put("createTime", re_StrTime);
			defineList.add(newMap);
		}
		map.put("details", defineList);
		return map;

	}

	/**
	 * 我的会员代理
	 *
	 * @param account_id
	 * @param date
	 * @return
	 */
	public Map<String, Object> queryMyProxyByMonth(long account_id, String date) {

		Date startDate = null;
		Date endDate = null;
		try {
			startDate = MyDateUtil.getMinMonthDate(date);
			endDate = MyDateUtil.getMaxMonthDate(date);
		} catch (Exception e) {
			return null;
		}

		Account account = this.getAccount(account_id);
		if (account == null) {
			return null;
		}

		Map<String, Object> map = Maps.newConcurrentMap();

		// 获取玩家所有的下级推广员
		Map<Long, AccountRecommendModel> recommendMap = account.getAccountRecommendModelMap();
		List<AccountRecommendModel> list = new ArrayList<AccountRecommendModel>(recommendMap.values());// 所有的推广员
		List<AccountRecommendModel> newList = new ArrayList<AccountRecommendModel>();// 用来存放新的集合
		List<AccountRecommendModel> everyMonthList = new ArrayList<AccountRecommendModel>();// 用来存放每个月的查询数据
		for (AccountRecommendModel model : list) {
			AccountModel targetAccountModel = this.getAccount(model.getTarget_account_id()).getAccountModel();
			// 满足的条件是玩家的推广级别为0并且是代理
			System.out.println("下级推官员的ID为：" + model.getTarget_account_id());
			System.out.println("下级推官员级别为：" + model.getRecommend_level());
			if (targetAccountModel.getRecommend_level() == 0 && targetAccountModel.getIs_agent() == 1) {
				newList.add(model);
			}
		}
		// map.put("totalCount", newList.size());

		// 过滤后的进行日期的查询
		String start = DateFormatUtils.format(startDate, "yyyy-MM-dd");
		String end = DateFormatUtils.format(endDate, "yyyy-MM-dd");
		for (AccountRecommendModel model : newList) {
			String createTime = DateFormatUtils.format(model.getCreate_time(), "yyyy-MM-dd");
			if (createTime.compareTo(start) >= 0 && createTime.compareTo(end) <= 0) {
				everyMonthList.add(model);
			}
		}
		map.put("totalCount", everyMonthList.size());

		Collections.sort(everyMonthList, new Comparator<AccountRecommendModel>() {
			public int compare(AccountRecommendModel o1, AccountRecommendModel o2) {
				// 按照时间进行排列
				Long time1 = o1.getCreate_time().getTime() / 1000;
				Long time2 = o2.getCreate_time().getTime() / 1000;
				if (time1 < time2) {
					return 1;
				}
				if (time1 == time2) {
					return 0;
				}
				return -1;
			}
		});
		// 封装成需要的数据
		List<Map<String, Object>> defineList = new ArrayList<Map<String, Object>>();
		Map<String, Object> newMap = null;
		for (AccountRecommendModel model : everyMonthList) {
			newMap = Maps.newConcurrentMap();
			newMap.put("accountId", model.getAccount_id());
			newMap.put("targetAccount", model.getTarget_account_id());
			String re_StrTime = null;
			SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");
			long lcc_time = Long.valueOf(model.getCreate_time().getTime() / 1000);
			re_StrTime = sdf.format(new Date(lcc_time * 1000L));
			newMap.put("createTime", re_StrTime);
			defineList.add(newMap);
		}
		map.put("details", defineList);
		return map;

	}

	/**
	 * 我的下线，type=0包含会员玩家、代理与下级推广员,1:推广员,2代理,3玩家,4推广代理
	 *
	 * @param account_id
	 * @param date
	 * @return
	 */
	public Map<String, Object> queryDownPlayerByMonth(long account_id, String startTime, String endTime, int type) {

		Date startDate = null;
		Date endDate = null;
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		try {
			startDate = dateFormat.parse(startTime);
			endDate = dateFormat.parse(endTime);
		} catch (Exception e) {
			return null;
		}

		Account account = this.getAccount(account_id);
		if (account == null) {
			return null;
		}

		Map<String, Object> map = Maps.newConcurrentMap();

		// 获取玩家所有的下级推广员
		Map<Long, AccountRecommendModel> recommendMap = account.getAccountRecommendModelMap();
		List<AccountRecommendModel> list = new ArrayList<AccountRecommendModel>(recommendMap.values());// 所有的推广员
		List<AccountRecommendModel> newList = new ArrayList<AccountRecommendModel>();// 用来存放新的集合
		List<AccountRecommendModel> everyMonthList = new ArrayList<AccountRecommendModel>();// 用来存放每个月的查询数据
		for (AccountRecommendModel model : list) {
			AccountModel targetAccountModel = this.getAccount(model.getTarget_account_id()).getAccountModel();
			// 满足的条件是玩家的推广级别为0并且是代理
			if (type == 0) {
				newList.add(model);
			} else if (type == 1) {
				if (targetAccountModel.getRecommend_level() > 0 && targetAccountModel.getIs_agent() == 0) {
					newList.add(model);
				}
			} else if (type == 2) {
				if (targetAccountModel.getIs_agent() > 0 && targetAccountModel.getRecommend_level() == 0) {
					newList.add(model);
				}
			} else if (type == 3) {
				if (targetAccountModel.getIs_agent() == 0 && targetAccountModel.getRecommend_level() == 0) {
					newList.add(model);
				}
			} else if (type == 4) {
				if (targetAccountModel.getIs_agent() > 0 && targetAccountModel.getRecommend_level() > 0) {
					newList.add(model);
				}
			}
		}
		// map.put("totalCount", newList.size());

		// 过滤后的进行日期的查询
		String start = DateFormatUtils.format(startDate, "yyyy-MM-dd");
		String end = DateFormatUtils.format(endDate, "yyyy-MM-dd");
		for (AccountRecommendModel model : newList) {
			String createTime = DateFormatUtils.format(model.getCreate_time(), "yyyy-MM-dd");
			if (createTime.compareTo(start) >= 0 && createTime.compareTo(end) <= 0) {
				everyMonthList.add(model);
			}
		}
		map.put("totalCount", everyMonthList.size());

		Collections.sort(everyMonthList, new Comparator<AccountRecommendModel>() {
			public int compare(AccountRecommendModel o1, AccountRecommendModel o2) {
				// 按照时间进行排列
				Long time1 = o1.getCreate_time().getTime() / 1000;
				Long time2 = o2.getCreate_time().getTime() / 1000;
				if (time1 < time2) {
					return 1;
				}
				if (time1 == time2) {
					return 0;
				}
				return -1;
			}
		});
		// 封装成需要的数据
		List<Map<String, Object>> defineList = new ArrayList<Map<String, Object>>();
		Map<String, Object> newMap = null;
		HashMap<Long, Long> rechargeMap = getTotalGroupBySubAccountId(account_id, 1);
		for (AccountRecommendModel model : everyMonthList) {
			newMap = Maps.newConcurrentMap();
			Account targetAccount = this.getAccount(model.getTarget_account_id());
			AccountModel accountModel = targetAccount.getAccountModel();
			if (accountModel.getIs_agent() > 0) {
				if (accountModel.getRecommend_level() > 0) {
					newMap.put("license", 3);// 推广员代理
				} else {
					newMap.put("license", 2);// 代理
				}
			} else {
				if (accountModel.getRecommend_level() > 0) {
					newMap.put("license", 1);// 推广员
				} else {
					newMap.put("license", 0);// 普通玩家
				}
			}
			if (targetAccount.getAccountWeixinModel() != null) {
				newMap.put("nickName", targetAccount.getAccountWeixinModel().getNickname());
			} else {
				newMap.put("nickName", "-");
			}
			newMap.put("accountId", model.getAccount_id());
			newMap.put("targetAccount", model.getTarget_account_id());
			String re_StrTime = null;
			SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");
			long lcc_time = Long.valueOf(model.getCreate_time().getTime() / 1000);
			re_StrTime = sdf.format(new Date(lcc_time * 1000L));
			newMap.put("createTime", re_StrTime);
			if (rechargeMap.containsKey(model.getTarget_account_id())) {
				newMap.put("totalRecharge", rechargeMap.get(model.getTarget_account_id()) / 100.00);
			} else {
				newMap.put("totalRecharge", 0);
			}
			defineList.add(newMap);
		}
		map.put("details", defineList);
		return map;

	}

	public HashMap<Long, Long> getTotalGroupBySubAccountId(long accountId, int type) {
		AggregationOperation match = Aggregation
				.match(Criteria.where("account_id").is(accountId).and("log_type").is(ELogType.recommendIncome.getId()));
		AggregationOperation group = null;
		if (type == 1) {
			group = Aggregation.group("target_id").sum("recharge_money").as("count").count().as("line");
		} else {
			group = Aggregation.group("target_id").sum("v1").as("count").count().as("line");
		}
		Aggregation aggregation = Aggregation.newAggregation(match, group);
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "game_recommend", GiveCardModel.class);
		List<GiveCardModel> sumLlist = result.getMappedResults();
		HashMap<Long, Long> map = new HashMap<Long, Long>();
		if (sumLlist != null && sumLlist.size() > 0) {
			for (GiveCardModel giveCardModel : sumLlist) {
				map.put(giveCardModel.get_id(), giveCardModel.getCount());
			}
		}
		return map;
	}

	public long getTotalRechargeByAccountId(long accountId, long targetId) {
		AggregationOperation match = Aggregation
				.match(Criteria.where("account_id").is(accountId).and("target_id").is(targetId).and("log_type").is(ELogType.recommendIncome.getId()));
		AggregationOperation group = Aggregation.group().sum("recharge_money").as("count").count().as("line");
		Aggregation aggregation = Aggregation.newAggregation(match, group);
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate().aggregate(aggregation, "game_recommend", GiveCardModel.class);
		List<GiveCardModel> sumLlist = result.getMappedResults();
		long totalBalance = 0;// 累计充值金额
		if (sumLlist != null && sumLlist.size() > 0) {
			GiveCardModel giveCardModel = sumLlist.get(0);
			totalBalance = giveCardModel.getCount();
		}
		return totalBalance;
	}

	/*
	 * (non-Javadoc) 获取中心服 最新数据
	 */
	@Override
	public AccountParamModel getAccountParamModelLoginReward(long account_id, EAccountParamType type) {
		return PublicServiceImpl.getInstance().getAccountParamModel(account_id, type);
	}

	/**
	 * 该方法只用于湖南推广员提现发送红包进行数据同步使用的 仅仅判断能否提现，不做入库的操作
	 *
	 * @param account_id
	 * @param income
	 * @return
	 */
	public AddGoldResultModel judgeWithdraw(long account_id, double income) {
		AddGoldResultModel addGoldResultModel = new AddGoldResultModel();
		addGoldResultModel.setSuccess(false);
		Account account = this.getAccount(account_id);
		if (account == null) {
			addGoldResultModel.setMsg("账号不存在");
			return addGoldResultModel;
		}
		try {
			AccountModel accountModel = account.getAccountModel();
			addGoldResultModel.setAccountModel(accountModel);
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
			}
			addGoldResultModel.setSuccess(true);

		} catch (Exception e) {
			logger.error("error", e);
		}
		return addGoldResultModel;

	}

	@Override
	public List<AppItem> getAllAppItemList() {
		if (AppItemDict.getInstance().getAppItemList() == null) {
			AppItemDict.getInstance().load();
		}
		return AppItemDict.getInstance().getAppItemList();
	}

	@Override
	public List<AppItem> getAppItemListByAppId(int appId) {
		return SpringService.getBean(PublicService.class).getPublicDAO().getAppItemListByAppId(appId);
	}

	@Override
	public void insertAppItem(AppItem appItem) {
		SpringService.getBean(PublicService.class).getPublicDAO().insertAppItem(appItem);
	}

	@Override
	public void updateAppItem(AppItem appItem) {
		SpringService.getBean(PublicService.class).getPublicDAO().updateAppItem(appItem);
	}

	@Override
	public List<RankModel> queryRank(int type) {
		ERankType rankType = ERankType.of(type);
		if (null == rankType) {
			logger.error("Proxy --> Center rmi,请求排行榜类型错误。type:{}", type);
			return Collections.emptyList();
		}

		return RankServiceImp.getInstance().getRank(ERankType.of(type));
	}

	@Override
	public int modifySigntrue(long accountId, String newSign) {

		Account account = this.getAccount(accountId);
		if (account == null) {
			return -1;
		}

		AccountModel accountModel = account.getAccountModel();
		if (null == accountModel) {
			return -2;
		}

		accountModel.setSignature(newSign);

		// 入库
		DataThreadPool.getInstance().addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.UPDATE, "updateAccountModel", accountModel));

		// 通知代理服
		RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
		rsAccountResponseBuilder.setAccountId(account.getAccount_id());
		RsAccountModelResponse.Builder accountBuilder = RsAccountModelResponse.newBuilder();
		accountBuilder.setSignature(newSign);
		rsAccountResponseBuilder.setRsAccountModelResponse(accountBuilder);

		RedisResponse.Builder redisRspBuilder = RedisResponse.newBuilder();
		redisRspBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
		redisRspBuilder.setRsAccountResponse(rsAccountResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisRspBuilder.build(),
				ERedisTopicType.topicProxy.getId() + account.getLastProxyIndex());
		return 0;
	}

	@Override
	public PlayerViewVO getPlayerViewVo(long accountId) {
		Account account = getAccount(accountId);
		if (account == null) {
			return null;
		}
		AccountModel accountModel = account.getAccountModel();
		if (null == accountModel) {
			return null;
		}
		PlayerViewVO vo = AccountUtil.getVo(account);

		return vo;
	}

	@Override
	public ClubRoomModel createClubRoom(long account, int club_id, ClubRuleModel roomRule, long club_account, String clubName, boolean ruleRepair,
			int tableIndex, int clubMemberSize, int debugLogicId) {
		// 是否在停服维护倒计时
		GlobalModel globalModel = PublicServiceImpl.getInstance().getGlobalModel();
		if (globalModel.isSystemStopReady()) {
			return new ClubRoomModel(-2, null, -1).setDesc("服务器进入停服倒计时,不能创建房间,请等待服务器停机维护完成再登录!");
		}

		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);

		// 禁止开房，内部玩家可以开房测试
		SysParamModel sysParamModel1000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(1000);
		if (sysParamModel1000.getVal3() == 0) {
			Account enterAccount = centerRMIServer.getAccount(account);
			if (enterAccount.getAccountModel().getIs_inner() == 0) {
				return new ClubRoomModel(-2, null, -1).setDesc("服务器临时维护,不能创建房间,请等待服务器停机维护完成再登录!");
			}
		}

		Account proxyAccount = centerRMIServer.getAccount(club_account);
		int game_id = SysGameTypeDict.getInstance().getGameIDByTypeIndex(roomRule.getGame_type_index());
		SysParamModel sysParamModel = null;
		try {
			sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id)
					.get(SysGameTypeDict.getInstance().getGameGoldTypeIndex(roomRule.getGame_type_index()));
		} catch (Exception e) {
			return new ClubRoomModel(-7, null, -1).setDesc("即将开放,敬请期待!");
		}
		// 还没开放
		if (sysParamModel != null && sysParamModel.getVal1() != 1) {

			return new ClubRoomModel(-7, null, -1).setDesc(Strings.isNullOrEmpty(sysParamModel.getStr2()) ? "即将开放,敬请期待!" : sysParamModel.getStr2());
		}

		// 还没开放
		if (GameGroupRuleDict.getInstance().get(roomRule.getGame_type_index()) == null) {
			return new ClubRoomModel(-7, null, -1).setDesc("游戏即将开放,敬请期待!");
		}

		int[] roundGoldArray = SysGameTypeDict.getInstance().getGoldIndexByTypeIndex(roomRule.getGame_type_index());

		SysParamModel findParam = null;
		for (int index : roundGoldArray) {
			SysParamModel tempParam = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(index);// 扑克类30
			if (tempParam == null) {
				logger.error("不存在的参数game_id" + SysGameTypeDict.getInstance().getGameIDByTypeIndex(roomRule.getGame_type_index()) + "index=" + index);
				continue;
			}
			if (tempParam.getVal1() == roomRule.getGame_round()) {
				findParam = tempParam;
				break;
			}
		}

		if (findParam == null) {
			return new ClubRoomModel(-7, null, -1).setDesc("包间异常，请重新保存玩法!");
		}

		// 最终确定扣款数量
		int finalCost = findParam.getVal4() == 1 ? findParam.getVal5().intValue() : findParam.getVal2().intValue();

		// add 20180205,临时需求-俱乐部人数区间扣豆
		if (ClubRangeCostUtil.INSTANCE.isActive()) {
			long value = ClubRangeCostUtil.INSTANCE.getValue(Pair.of(roomRule.getGame_type_index(), findParam.getVal1()), clubMemberSize);
			if (value >= 0 && value != Long.MIN_VALUE) {
				finalCost = (int) value;
			}
		}

		// 语音房补充扣豆
		if (roomRule.getRuleParams().getRuleValue(GameConstants.GAME_RULE_VOICE_ROOM) > 0) {
			finalCost += findParam.getVal3();
		}
		// int finalCost = findParam.getVal2();
		// 游戏id
		int gameId = SysGameTypeDict.getInstance().getGameIDByTypeIndex(roomRule.getGame_type_index());

		if (tableIndex != -1) { // 亲友圈自建赛tableIndex为-1,建赛时已扣豆这里不再检查豆够不够
			// 判断房卡是否免费
			if (sysParamModel != null && sysParamModel.getVal2() == 1) {

				do {
					// 1 专属豆满足
					if (ExclusiveGoldCfg.get().isUseExclusiveGold() && ClubExclusiveService.getInstance().check(club_account, gameId, finalCost)) {
						break;
					}
					// 2 专属豆不够的情况下判断房卡
					long gold = proxyAccount.getAccountModel().getGold();
					if (gold < finalCost) {
						return new ClubRoomModel(-6, null, -1).setDesc(SysParamServerDict.getInstance().replaceGoldTipsWord(("亲友圈创始人闲逸豆不足！")));
					}
				} while (false);

			}
		}

		int logicId = 0;
		if (debugLogicId > 0) {
			logicId = debugLogicId;
		} else {
			logicId = centerRMIServer.allotLogicId(game_id);
		}

		if (logicId <= 0) {
			return new ClubRoomModel(-2, null, -1).setDesc("服务器进入停服倒计时,不能创建房间,请等待服务器停机维护完成再登录!");
		}

		int room_id = centerRMIServer.randomRoomId(1, logicId);// 随机房间号
		if (room_id == -1) {
			return new ClubRoomModel(-1, null, -1).setDesc("创建房间失败!");
		}
		if (room_id == -2) {
			return new ClubRoomModel(-2, null, -1).setDesc("服务器进入停服倒计时,不能创建房间,请等待服务器停机维护完成再登录!");
		}

		String desc = null;
		int maxPlayer = 0;
		if (ruleRepair) {
			ILogicRMIServer logicRmiServer = RMIServiceImpl.getInstance().getLogicRMIByIndex(logicId);
			RmiDTO rmiDTO = logicRmiServer.getGameDescAndPeopleNumber(
					GameDescUtil.params.get().setGameType(roomRule.getGame_type_index()).setGameRule(roomRule.getRuleParams()._game_rule_index));

			desc = rmiDTO.getDesc();
			maxPlayer = rmiDTO.getValue();
		} else {
			maxPlayer = RoomComonUtil.getMaxNumber(roomRule.getRuleParams());
			desc = roomRule.getGameDesc(GameGroupRuleDict.getInstance().get(roomRule.getGame_type_index()));
		}

		// redis房间记录 代理开房间，人不需要进去
		RoomRedisModel roomRedisModel = new RoomRedisModel();
		roomRedisModel.setRoom_id(room_id);
		roomRedisModel.setLogic_index(logicId);
		// roomRedisModel.getPlayersIdSet().add(account);
		roomRedisModel.setCreate_time(System.currentTimeMillis());
		roomRedisModel.setGame_round(roomRule.getGame_round());
		roomRedisModel.setGame_rule_index(roomRule.getRuleParams()._game_rule_index);
		roomRedisModel.setGameRuleIndexEx(roomRule.getRuleParams().game_rules);
		roomRedisModel.setGame_type_index(roomRule.getGame_type_index());
		roomRedisModel.setPlayer_max(maxPlayer);
		roomRedisModel.setGame_id(game_id);
		roomRedisModel.setClub_id(club_id);
		roomRedisModel.setClubName(clubName);
		roomRedisModel.setProxy_room(false);
		roomRedisModel.setCreate_account_id(club_account);
		roomRedisModel.setRule_id(roomRule.getId());
		roomRedisModel.setTable_index(tableIndex);
		roomRedisModel.setCreateType(GameConstants.CREATE_ROOM_CLUB);
		SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM, room_id + "", roomRedisModel);
		SysParamModel sysParamModel2000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(2000);
		if (sysParamModel2000 == null) {
			sysParamModel2000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2000);
		}
		String url = sysParamModel2000.getStr2() + "?game=" + game_id + "&roomid=" + room_id;

		return new ClubRoomModel(1, roomRule, room_id, logicId, club_account, url).setDesc("成功").setMaxNumber(roomRedisModel.getPlayer_max())
				.setGameDesc(desc);
	}

	@Override
	public boolean checkIsInClub(long accountId, int club_id) {
		ClubAccountModel clubGroup = new ClubAccountModel();
		clubGroup.setAccount_id(accountId);
		clubGroup.setClub_id(club_id);
		ClubAccountModel accountModel = SpringService.getBean(PublicService.class).getPublicDAO().getClubAccount(clubGroup);
		return accountModel != null;
	}

	@Override
	public List<String> getGroupsByClub(int club_id) {
		return SpringService.getBean(PublicService.class).getPublicDAO().getClubGroup(club_id);
	}

	@Override
	public long allocateId(IDType idType, int serverIdx) {
		return IDServiceImpl.getInstance().next(idType);
	}

	@Override
	public boolean serverStatusUpdate(EServerType serverType, EServerStatus status, int serverIndex) {
		RMIServiceImpl.getInstance().serverStatusUpdate(serverType, status, serverIndex);
		return true;
	}

	@Override
	public boolean serverPing(EServerType serverType, int serverIndex) {
		RMIServiceImpl.getInstance().serverPing(serverType, serverIndex);
		return true;
	}

	@Override
	public void doAgentRecommendReceived(Account account, int money) {
		if (account.getHallRecommendModel().getAccount_id() > 0) {// 钻石黄金白银推广员返利
			SysParamModel sysParamModel2224 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2224);
			int limitMoney = 30000;
			if (sysParamModel2224 != null && sysParamModel2224.getVal4() > 0) {
				limitMoney = sysParamModel2224.getVal4();
			}
			if (money >= limitMoney) {
				RecommendService.getInstance().doHallRecommendReceived(account, money);
			}
		}
		// else {// 钻石黄金代理推广员返利
		// RecommendService.getInstance().doAgentReceived(account, money);
		// }
	}

	public void doRecommendPlayerReceive(Account account, int money) {
		SysParamModel sysParamModel5000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(5000);
		long gameId = sysParamModel5000.getVal1();
		if (gameId != EGameType.JS.getId()) {
			return;
		}
		RecommendService.getInstance().doRecommenPlayerReceived(account, money);
	}

	@Override
	public AddGoldResultModel doAgentIncome(long account_id, double income, long level, String desc, EGoldOperateType eGoldOperateType, long targetId,
			long rechargeMoney) {
		return RecommendService.getInstance().doAgentIncome(account_id, income, level, desc, eGoldOperateType, targetId, rechargeMoney);
	}

	@Override
	public AddGoldResultModel doHallRecommendIncome(long account_id, double income, long level, String desc, EGoldOperateType eGoldOperateType,
			long targetId, long rechargeMoney) {
		return RecommendService.getInstance().doHallRecommendIncome(account_id, income, level, desc, eGoldOperateType, targetId, rechargeMoney, 0);
	}

	@Override
	public Map<String, Object> queryAgentRecommendAll(long account_id) {
		return RecommendService.getInstance().queryRecommendAll(account_id);
	}

	@Override
	public Map<String, Object> queryDownRecommend(long account_id, Date startDate, Date endDate) {
		return RecommendService.getInstance().queryDownRecommend(account_id, startDate, endDate);
	}

	@Override
	public Map<String, Object> queryHallDownRecommend(long account_id, Date startDate, Date endDate) {
		return RecommendService.getInstance().queryHallDownRecommend(account_id, startDate, endDate);
	}

	@Override
	public Map<String, Object> queryHallDownAgent(long account_id, Date startDate, Date endDate) {
		return RecommendService.getInstance().queryHallDownAgent(account_id, startDate, endDate);
	}

	@Override
	public Map<String, Object> queryMyPlayers(long account_id, Date startDate, Date endDate) {
		return RecommendService.getInstance().queryMyPlayers(account_id, startDate, endDate);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.cai.common.rmi.ICenterRMIServer#getSimpleAccount(long)
	 */
	@Override
	public AccountSimple getSimpleAccount(long account_id) {
		AccountSimple accountsimple = PublicServiceImpl.getInstance().getAccountSimpe(account_id);
		return accountsimple;
	}

	@Override
	public boolean reloadGameRecommendIndexDict() {
		GameRecommendDict.getInstance().load();
		// ========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
		RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
		rsDictUpdateResponseBuilder.setRsDictType(RsDictType.GAME_RECOMMEND);
		redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
		// ===================
		return true;
	}

	/**
	 * @param money
	 *            操作金额
	 * @param operateType
	 *            1表示发送红包，2表示提现红包
	 * @param activity_id
	 *            活动主键唯一ID
	 * @return
	 */
	public boolean operateRedActivityModel(long account_id, int money, int operateType, int activity_id) {
		try {
			if (account_id != 0) {
				Account account = this.getAccount(account_id);
				if (account == null)
					return false;
				RedActivityModel oldModel = account.getRedActivityModel();
				if (operateType == 1) {
					if (oldModel.getAccount_id() == 0) {// 未有红包数据则新入库
						oldModel.setAccount_id(account_id);
						oldModel.setAll_money(money);
						oldModel.setReceive_money(0);
						DataThreadPool.getInstance()
								.addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.INSERT, "insertRedActivityModel", oldModel));
						// 入库
					} else {
						oldModel.setAll_money(oldModel.getAll_money() + money);
						DataThreadPool.getInstance()
								.addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.UPDATE, "updateRedActivityModel", oldModel));
					}

					// 记录玩家领取红包次数
					Multiset<Integer> person = PublicServiceImpl.getInstance().getAccount(account_id).getRED_PACK_COUNT();
					if (person == null) {
						person = HashMultiset.create();
					}
					person.add(activity_id);
					// 记录玩家领取红包次数结束
				} else if (operateType == 2) {
					if (oldModel.getAccount_id() == 0) {// 未有红包数据不能提现
						return false;
					} else {
						// 提现金额必须大于余额
						if ((oldModel.getAll_money() - oldModel.getReceive_money()) < money) {
							return false;
						}
						oldModel.setReceive_money(oldModel.getReceive_money() + money);
						DataThreadPool.getInstance()
								.addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.UPDATE, "updateRedActivityModel", oldModel));
					}
				}
				// SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT,
				// account_id + "", account);// 更新redis缓存
				RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
				redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
				RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
				rsAccountResponseBuilder.setAccountId(account_id);
				rsAccountResponseBuilder.setAllMoney(oldModel.getAll_money());
				rsAccountResponseBuilder.setReceiveMoney(oldModel.getReceive_money());
				redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
				RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
				return true;
			}

		} catch (Exception e) {
			logger.error("error", e);
			return false;
		}
		return false;
	}

	public ConcurrentHashMap<Integer, SysGameType> loadSysGameTypeMapToSSH() {
		SysGameTypeDict sysGameTypeDict = SysGameTypeDict.getInstance();
		return sysGameTypeDict.getSysGameTypeDictionary();
	}

	@Override
	public boolean reloadServerBalanceDict() {
		LogicServerBalanceDict.getInstance().load();
		return true;
	}

	@Override
	public List<ClubStatusModel> getCLubStatusList() {
		return null;
	}

	@Override
	public void dispatchRedPackage() {
		RedPackageServiceImp.getInstance().dispatchRedPackage();
	}

	@Override
	public int takeRedPackage(int type, int activeId) {
		return RedPackageServiceImp.getInstance().takeRedPackage(type);
	}

	@Override
	public TurntableSystemModel startTurntableRound() {
		return TurntableService.getInstance().startRound();
	}

	@Override
	public List<TurntableRewardModel> tokenTurntableRewardRank() {
		return TurntableService.getInstance().getTurntableRewardRank();
	}

	@Override
	public int addDayPlayRound(long accountId) {
		try {
			Account account = PublicServiceImpl.getInstance().getAccount(accountId);
			// WalkerGeek 测试
			increTotalRound(account);// 总局数入库
			return account.getDay_all_round().incrementAndGet();
		} catch (Exception e) {
			logger.error("Proxy --> Center rmi,增加玩家玩局数失败。accountId:{}", e);
		}
		return -1;
	}

	public void increTotalRound(Account account) {
		AccountParamModel accountParamModel = account.getAccountParamModelMap().get(EAccountParamType.TOTAL_ROUND.getId());
		if (accountParamModel != null) {
			accountParamModel.setVal1(accountParamModel.getVal1() + 1);
			accountParamModel.setNeedDB(true);
		} else {
			// 新加的值，标识一下
			accountParamModel = new AccountParamModel();
			accountParamModel.setAccount_id(account.getAccount_id());
			accountParamModel.setType(EAccountParamType.TOTAL_ROUND.getId());
			accountParamModel.setNeedDB(false);
			accountParamModel.setVal1(1);
			accountParamModel.setStr1("");
			accountParamModel.setLong1(0l);
			accountParamModel.setDate1(new Date());
			accountParamModel.setNewAddValue(true);
			account.getAccountParamModelMap().put(accountParamModel.getType(), accountParamModel);
		}
	}

	/**
	 * 账号id换靓号辅助方法，修改上下级推荐关系等等，不修改旧账号的id
	 */
	public boolean updateAccountId(long accountId, long newAccountId) {
		return PublicServiceImpl.getInstance().updateAccountId(accountId, newAccountId);
	}

	@Override
	public void addTurntableRewardLog(long accountId, int goodsId, String goodsDesc) {
		TurntableService.getInstance().addTurntableRewardLog(accountId, goodsId, goodsDesc);
	}

	/**
	 * (non-Javadoc)
	 */
	@Override
	public Multiset<Integer> getRedPackReceiveCount(long newAccountId) {
		return PublicServiceImpl.getInstance().getAccount(newAccountId).getRED_PACK_COUNT();
	}

	@Override
	public void increRecommenderCount(long recommendId) {
		Account account = this.getAccount(recommendId);
		account.getRecommendRelativeModel().incre();
	}

	public int setRecommender(String unionId) {
		if (!RecommendService.getInstance().containsKey(unionId)) {
			return -1;// 无需设置推荐人
		}
		Account account = PublicServiceImpl.getInstance().getAccountByWxUnionid(unionId);
		if (account == null || account.getHallRecommendModel().getTarget_account_id() > 0) {
			return -2;// 用户不存在或者已经设置过推荐人
		}
		HallRecommendModel model = RecommendService.getInstance().reduceRecommendMap(unionId);
		if (model == null) {
			return -3;// 缓存对象不存在
		}
		if (ZZPromoterService.getInstance().getAccountZZPromoterModel(account.getAccount_id()) != null) {
			return -2;// 株洲协会的推广对象不能设置上级推广员
		}
		model.setProxy_level(account.getAccountModel().getProxy_level());
		model.setTarget_account_id(account.getAccount_id());
		SysParamModel sysParamModel2004 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(account.getGame_id()).get(2004);
		int addGold = 50;
		if (sysParamModel2004 != null) {
			addGold = sysParamModel2004.getVal4();
		}
		addAccountGold(account.getAccount_id(), addGold, false, "填写推广员推荐人送豆，推广员account_id:" + model.getAccount_id(),
				EGoldOperateType.PADDING_RECOMMEND_ID);
		this.addHallRecommendModel(model);
		// try {
		// addRecommendPreReceive(account, model.getAccount_id());
		// } catch (Exception e) {
		// }
		return 0;
	}

	public void addRecommendPreReceive(Account account, long recommend_id) {
		if (account.getGame_id() != EGameType.JS.getId()) {
			// 江苏棋牌才有推荐返利
			return;
		}
		AccountParamModel accountParamModel = getAccountParamModel(account, EAccountParamType.RECOMMEND_PLAYER_RECEIVE);
		// val1=1获得返利资格，2表示已经返利
		if (accountParamModel.getVal1() != null && accountParamModel.getVal1() == 2) {
			return;
		}
		accountParamModel.setVal1(1);

	}

	public AccountParamModel getAccountParamModel(Account account, EAccountParamType eAccountParamType) {
		AccountParamModel accountParamModel = account.getAccountParamModelMap().get(eAccountParamType.getId());
		if (accountParamModel == null) {
			accountParamModel = new AccountParamModel();
			accountParamModel.setAccount_id(account.getAccount_id());
			accountParamModel.setType(eAccountParamType.getId());
			accountParamModel.setNeedDB(false);
			accountParamModel.setVal1(1);
			accountParamModel.setStr1("");
			accountParamModel.setLong1(0l);
			accountParamModel.setDate1(new Date());
			accountParamModel.setNewAddValue(true);
			account.getAccountParamModelMap().put(accountParamModel.getType(), accountParamModel);
		}
		return accountParamModel;
	}

	/*
	 * (non-Javadoc) 红包雨分享红包时间更新
	 *
	 * @see com.cai.common.rmi.ICenterRMIServer#setShareTime()
	 */
	@Override
	public void setShareTime(long account_id) {
		Account account = PublicServiceImpl.getInstance().getAccount(account_id);
		account.setShareTime(System.currentTimeMillis());
	}

	@Override
	public int getLastAccountProxyIndex(long account_id) {
		Account account = PublicServiceImpl.getInstance().getAccount(account_id);
		if (account == null) {
			return -1;
		}
		return account.getLastProxyIndex();
	}

	@Override
	public AddGoldResultModel addAccountGoldAndMoney(long account_id, int gold, boolean isExceed, String desc, EGoldOperateType eGoldOperateType,
			int money, EMoneyOperateType eMoneyOperateType) {
		AddGoldResultModel addGoldResultModel = new AddGoldResultModel();
		addGoldResultModel.setSuccess(false);

		Account account = this.getAccount(account_id);
		if (account == null) {
			addGoldResultModel.setMsg("账号不存在");
			return addGoldResultModel;
		}

		ReentrantLock lock = account.getRedisLock();
		try {
			lock.lock();

			AccountModel accountModel = account.getAccountModel();
			AccountWeixinModel wxModel = account.getAccountWeixinModel();
			addGoldResultModel.setAccountModel(accountModel);
			if (wxModel != null) {
				addGoldResultModel.setWxNickName(wxModel.getNickname());
			}

			if (money != 0) {

				long moneyTemp = accountModel.getMoney() + money;
				if (!isExceed && moneyTemp < 0) {
					addGoldResultModel.setMsg("金币不足");
					return addGoldResultModel;
				}
				addAccountMoney(account_id, money, isExceed, desc, eMoneyOperateType);
			}

			// 如果不需要扣逗
			if (gold == 0) {
				addGoldResultModel.setSuccess(true);
				return addGoldResultModel;
			}

			long oldValue = accountModel.getGold();

			if (gold > 0) {
				accountModel.setGold(accountModel.getGold() + gold);
				accountModel.setHistory_pay_gold(accountModel.getHistory_pay_gold() + gold);
				addGoldResultModel.setSuccess(true);

				AccountParamModel accountParamModel = PublicServiceImpl.getInstance().getAccountParamModel(account_id,
						EAccountParamType.TODAY_ADD_GOLD);
				if (accountParamModel != null) {
					accountParamModel.setLong1(accountParamModel.getLong1() + gold);
					accountParamModel.setNeedDB(true);
				}
				if (eGoldOperateType.getId() == EGoldOperateType.FAILED_ROOM.getId()) {
					accountModel.setConsum_total(accountModel.getConsum_total() - gold);// gold<0
				}
			} else {
				long k = accountModel.getGold() + gold;
				if (!isExceed) {
					if (k < 0) {
						addGoldResultModel.setMsg(SysParamServerDict.getInstance().replaceGoldTipsWord("闲逸豆不足"));
						return addGoldResultModel;
					}
				}

				if (k < 0) {
					accountModel.setGold(0L);
				} else {
					accountModel.setGold(k);
				}
				AccountParamModel accountParamModel = PublicServiceImpl.getInstance().getAccountParamModel(account_id,
						EAccountParamType.TODAY_CONSUM_GOLD);
				if (accountParamModel != null) {
					accountParamModel.setLong1(accountParamModel.getLong1() - gold);
					accountParamModel.setNeedDB(true);
				}
				if (eGoldOperateType.getId() == EGoldOperateType.PROXY_GIVE.getId()
						|| eGoldOperateType.getId() == EGoldOperateType.OPEN_ROOM.getId()) {
					accountModel.setConsum_total(accountModel.getConsum_total() - gold);// gold<0
				}
				addGoldResultModel.setSuccess(true);

			}

			// 房卡操作直接入库
			DataThreadPool.getInstance().addTask(new DBUpdateDto(DbStoreType.PUBLIC, DbOpType.UPDATE, "updateAccountModel", accountModel));
			// // 处理充值返利
			// if (eGoldOperateType.getId() ==
			// EGoldOperateType.OSS_OPERATE.getId()) {
			// int price = 0;
			// String[] descList = desc.split(":");
			// int shopId = 0;
			// if (descList.length > 1) {
			// try {
			// shopId = Integer.parseInt(descList[descList.length - 1]);
			// } catch (Exception e) {
			// }
			// }
			// if (shopId > 0) {
			// ShopModel shop = ShopDict.getInstance().getShopModel(shopId);
			// if (shop != null) {
			// price = shop.getPrice();
			// }
			// }
			// if (price > 0) {
			// doAgentReceived(account, price / 100);
			// }
			//
			// }
			long change = accountModel.getGold() - oldValue;
			long newValue = accountModel.getGold();
			// 日志
			StringBuffer buf = new StringBuffer();
			buf.append("|");
			if (change > 0) {
				buf.append("增加[" + change + "]");
			} else {
				buf.append("减少[" + change + "]");
				// MongoDBServiceImpl.getInstance().log(account_id,
				// ELogType.addGold, desc, change,
				// (long) EAccountParamType.TODAY_CONSUM_GOLD.getId(), null);
			}
			buf.append(",值变化:[").append(oldValue).append("]->[").append(newValue).append("]");
			desc = desc + buf.toString();
			MongoDBServiceImpl.getInstance().log(account_id, ELogType.addGold, desc, change, (long) eGoldOperateType.getId(), null, oldValue,
					newValue);
			// ========同步到中心========
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			//
			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			rsAccountResponseBuilder.setAccountId(account_id);
			//
			RsAccountModelResponse.Builder rsAccountModelResponseBuilder = RsAccountModelResponse.newBuilder();
			rsAccountModelResponseBuilder.setGold(accountModel.getGold());
			rsAccountModelResponseBuilder.setHistoryPayGold(accountModel.getHistory_pay_gold());
			rsAccountModelResponseBuilder.setConsumTotal(accountModel.getConsum_total());
			rsAccountModelResponseBuilder.setGoldChangeType(eGoldOperateType.getId());
			rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponseBuilder);
			//
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

	// 退单退返利
	public void paybackReceive(long account_id, int money) {
		Account account = getAccount(account_id);
		RecommendService.getInstance().doPaybackReceived(account, money);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.cai.common.rmi.ICenterRMIServer#updateEmailId(long, int)
	 */
	@Override
	public void updateEmailId(long accountId, int emailId) {
		PublicServiceImpl.getInstance().getAccount(accountId).getAccountModel().setEmail_id(emailId);
	}

	public int getMatchMaxSeqByItemId(int itemId) {
		return MatchServiceImp.getInstance().takeSeqByItemId(itemId);
	}

	@Override
	public boolean checkMoney(long account_id, int money) {
		Account account = this.getAccount(account_id);
		if (account == null) {
			return false;
		}

		return account.getAccountModel().getMoney() >= money;
	}

	@Override
	public boolean isLessRoundNum(long account_id, int bigRoundNum) {
		Account account = this.getAccount(account_id);
		if (account == null) {
			return true;
		}
		if (account.getDay_all_round().get() < bigRoundNum) {
			return true;
		}
		return false;
	}

	@Override
	public RedActivityModel getRedActivityModel(long account_id) {
		Account account = this.getAccount(account_id);
		if (account == null) {
			return null;
		}
		return account.getRedActivityModel();
	}

	@Override
	public boolean isExistAccount(long account_id) {
		Account account = this.getAccount(account_id);
		if (account == null) {
			return false;
		}
		return true;
	}

	@Override
	public AccountModel getAccountModel(long account_id) {
		Account account = this.getAccount(account_id);
		if (account == null) {
			return null;
		}
		return account.getAccountModel();
	}

	@Override
	public long getRecommendId(long account_id) {
		// TODO Auto-generated method stub
		Account account = getAccount(account_id);
		if (account == null) {
			return 0;
		}
		long recommend_id = account.getHallRecommendModel().getAccount_id();
		return recommend_id;
	}

	@Override
	public <T, R> R rmiInvoke(int cmd, T message) {
		IRMIHandler<T, R> handler = RMIHandlerServiceImp.getInstance().getHandler(cmd);
		if (null != handler) {
			return handler.apply(message);
		}
		return null;
	}

	/**
	 * @param dictTypeStr
	 *            --后台传递过来 最好在 DictStringType定义
	 */
	public void reloadDistributeDict(String dictTypeStr) {

		roloadCenterDict(dictTypeStr);// 中心服根据需要，补充加载的方法;一般是
										// 数据库取出，写入redis，尽量由其它服务器进行，最好别依赖中心服,各服务器管理自己的数据。

		// ========分发到其它的服务器=======
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
		//
		RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
		rsDictUpdateResponseBuilder.setRsDictType(RsDictType.COMMON_DICT);// 通用的加载
		rsDictUpdateResponseBuilder.setDictTypeStr(dictTypeStr);
		//
		redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicAll);
	}

	// 中心服根据需要，是否根据类型加载
	public void roloadCenterDict(String dictTypeStr) {
		switch (dictTypeStr) {
		case DictStringType.TESTDICT:
			// load();--执行具体的加载
			break;
		}
	}

	///////////////////////////////////////// 不在这里继续加方法了，考虑rmiHandler,可以参考RMICmd#HELLO
	///////////////////////////////////////// ///////////////////////////////
}
