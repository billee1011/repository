package com.cai.service;

import java.io.Serializable;
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
import java.util.Timer;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import com.cai.common.constant.GameConstants;
import com.cai.common.define.EGameType;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.ELogType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.domain.BrandChildLogModel;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.ClubActivityLogModel;
import com.cai.common.domain.ClubExclusiveGoldLogModel;
import com.cai.common.domain.ClubLogModel;
import com.cai.common.domain.Event;
import com.cai.common.domain.GameLogModel;
import com.cai.common.domain.GiveCardModel;
import com.cai.common.domain.InviteModel;
import com.cai.common.domain.InviteRedPacketModel;
import com.cai.common.domain.InviteResultModel;
import com.cai.common.domain.MatchPlayerLogModel;
import com.cai.common.domain.MobileLogModel;
import com.cai.common.domain.Page;
import com.cai.common.domain.PlayerLogClientIpModel;
import com.cai.common.domain.PlayerLogModel;
import com.cai.common.domain.PlayerLogNotifyIpModel;
import com.cai.common.domain.PlayerLogServerIpModel;
import com.cai.common.domain.ProxyGoldLogModel;
import com.cai.common.domain.RedPackageModel;
import com.cai.common.domain.RedPackageRecordModel;
import com.cai.common.domain.ReponseLogModel;
import com.cai.common.domain.RequestLogModel;
import com.cai.common.domain.ServerErrorLogModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.SystemLogModel;
import com.cai.common.domain.SystemLogOnlineModel;
import com.cai.common.domain.SystemLogQueueModel;
import com.cai.common.domain.TurntableLogModel;
import com.cai.common.domain.VoiceChatLogModel;
import com.cai.common.domain.log.StoreBuyLogModel;
import com.cai.common.type.VoiceChatType;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.SpringService;
import com.cai.core.MonitorEvent;
import com.cai.core.SystemConfig;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.timer.MogoDBTimer;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.server.AbstractService;

import org.apache.commons.lang.time.DateUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import protobuf.clazz.c2s.C2SProto.VoiceChatRequestProto;

/**
 * mogodb服务类
 * 
 * @author run
 *
 */
@SuppressWarnings({ "all" })
public class MongoDBServiceImpl extends AbstractService {

	/**
	 * 日志队列
	 */
	private LinkedBlockingQueue<Object> logQueue = new LinkedBlockingQueue<>();

	private Timer timer;

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
			gameLogModel.setProxy_id(SystemConfig.proxy_index);
			gameLogModel.setLog_type(eLogType.getId());
			gameLogModel.setMsg(msg);
			gameLogModel.setRoomId(roomId);
			gameLogModel.setExtractMsg(extractMsg);
			logQueue.add(gameLogModel);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		if (!SysParamServerDict.getInstance().getIsOpenLog())
			return;
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
		playerLogModel.setProxy_id(SystemConfig.proxy_index);
		logQueue.add(playerLogModel);
	}

	public void player_log_serverIP(long account_id, ELogType eLogType, String msg, Long v1, Long v2, String account_ip) {
		if (!SysParamServerDict.getInstance().getIsOpenLog())
			return;

		PlayerLogServerIpModel playerLogModel = new PlayerLogServerIpModel();
		playerLogModel.setCreate_time(new Date());
		playerLogModel.setAccount_id(account_id);
		playerLogModel.setCenter_id(1);
		playerLogModel.setLog_type(eLogType.getId());
		playerLogModel.setMsg(msg);
		playerLogModel.setV1(v1);
		playerLogModel.setV2(v2);
		playerLogModel.setLocal_ip(SystemConfig.localip);
		playerLogModel.setAccount_ip(account_ip);
		playerLogModel.setProxy_id(SystemConfig.proxy_index);
		logQueue.add(playerLogModel);
	}

	public void player_log_clientIP(long account_id, ELogType eLogType, String msg, Long v1, Long v2, String account_ip) {
		if (!SysParamServerDict.getInstance().getIsOpenLog())
			return;

		PlayerLogClientIpModel playerLogModel = new PlayerLogClientIpModel();
		playerLogModel.setCreate_time(new Date());
		playerLogModel.setAccount_id(account_id);
		playerLogModel.setCenter_id(1);
		playerLogModel.setLog_type(eLogType.getId());
		playerLogModel.setMsg(msg);
		playerLogModel.setV1(v1);
		playerLogModel.setV2(v2);
		playerLogModel.setLocal_ip(SystemConfig.localip);
		playerLogModel.setAccount_ip(account_ip);
		playerLogModel.setProxy_id(SystemConfig.proxy_index);
		logQueue.add(playerLogModel);
	}

	/**
	 * 废弃
	 * 
	 * @param account_id
	 * @param eLogType
	 * @param msg
	 * @param v1
	 * @param v2
	 * @param account_ip
	 */
	public void player_log_notifyIP(long account_id, ELogType eLogType, String msg, Long v1, Long v2, String account_ip) {
		if (!SysParamServerDict.getInstance().getIsOpenLog())
			return;

		PlayerLogNotifyIpModel playerLogModel = new PlayerLogNotifyIpModel();
		playerLogModel.setCreate_time(new Date());
		playerLogModel.setAccount_id(account_id);
		playerLogModel.setCenter_id(1);
		playerLogModel.setLog_type(eLogType.getId());
		playerLogModel.setMsg(msg);
		playerLogModel.setV1(v1);
		playerLogModel.setV2(v2);
		playerLogModel.setLocal_ip(SystemConfig.localip);
		playerLogModel.setAccount_ip(account_ip);
		playerLogModel.setProxy_id(SystemConfig.proxy_index);
		logQueue.add(playerLogModel);
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
	public void request_log(long account_id, ELogType eLogType, String msg, Long v1, Long v2, String account_ip, int roomID) {
		RequestLogModel playerLogModel = new RequestLogModel();
		playerLogModel.setCreate_time(new Date());
		playerLogModel.setAccount_id(account_id);
		playerLogModel.setCenter_id(1);
		playerLogModel.setRoomID(roomID);
		playerLogModel.setMsg(msg);
		playerLogModel.setV1(v1);
		playerLogModel.setV2(v2);
		playerLogModel.setLocal_ip(SystemConfig.localip);
		playerLogModel.setAccount_ip(account_ip);
		logQueue.add(playerLogModel);
	}

	/**
	 * 
	 * @param account_id
	 * @param eLogType
	 * @param msg
	 * @param v1
	 * @param v2
	 * @param account_ip
	 */
	public void response_log(long account_id, ELogType eLogType, String msg, Long v1, Long v2, String account_ip, int roomID) {
		ReponseLogModel playerLogModel = new ReponseLogModel();
		playerLogModel.setCreate_time(new Date());
		playerLogModel.setAccount_id(account_id);
		playerLogModel.setCenter_id(1);
		playerLogModel.setRoomID(roomID);
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
		systemLogModel.setProxy_id(SystemConfig.proxy_index);
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
	public void systemLog_online(ELogType eLogType, String msg, Long v1, Long v2, ESysLogLevelType eSysLogLevelType) {
		SystemLogOnlineModel systemLogModel = new SystemLogOnlineModel();
		systemLogModel.setCreate_time(new Date());
		systemLogModel.setProxy_id(SystemConfig.proxy_index);
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
		systemLogModel.setProxy_id(SystemConfig.proxy_index);
		systemLogModel.setLog_type(eLogType.getId());
		systemLogModel.setMsg(msg);
		systemLogModel.setV1(v1);
		systemLogModel.setV2(v2);
		systemLogModel.setLocal_ip(SystemConfig.localip);
		systemLogModel.setLevel(eSysLogLevelType.getId());
		logQueue.add(systemLogModel);
	}

	/**
	 * 玩家的最后10条大牌局记录(2天内的记录) (按大局分页)
	 * 
	 * @param account_id
	 */
	public List<BrandLogModel> getParentBrandListByAccountIdNew(Page page, long account_id, int game_id, long createTime, long endTime) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		if (createTime > 0) {
			Date startDate = new Date(createTime);

			Date endDate = null;
			if (endTime > 0) {
				endDate = new Date(endTime);
			} else {
				endDate = MyDateUtil.getTomorrowZeroDate(startDate);
			}
			query.addCriteria(Criteria.where("create_time").gte(MyDateUtil.getZeroDate(startDate)).lte(endDate));
		} else {
			query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(MyDateUtil.getZeroDate(new Date()), -1)));
		}
		if (game_id != 0) {
			query.addCriteria(Criteria.where("game_id").is(game_id));
		}
		query.addCriteria(Criteria.where("accountIds").is(account_id));
		query.addCriteria(Criteria.where("log_type").is(ELogType.parentBrand.getId()));
		query.addCriteria(new Criteria().orOperator(Criteria.where("clubMatchId").is(null), Criteria.where("clubMatchId").lte(0)));
		List<Integer> typeList = new ArrayList<>();
		typeList.add(GameConstants.CREATE_ROOM_MATCH);
		typeList.add(GameConstants.CREATE_ROOM_NEW_COIN);
		query.addCriteria(Criteria.where("createType").nin(typeList));
		query.with(new Sort(Direction.DESC, "create_time"));
		//
		int count = (int) mongoDBService.getMongoTemplate().count(query, BrandLogModel.class);
		page.setTotalSize(count);
		query.skip(page.getBeginNum());
		query.limit(page.getPageSize());// 15灞�
		List<BrandLogModel> brandLogModelList = mongoDBService.getMongoTemplate().find(query, BrandLogModel.class);
		return brandLogModelList;
	}

	/**
	 * 玩家的最后10条大牌局记录(2天内的记录) TODO 优化(未按大局分页)
	 * 
	 * @param account_id
	 */
	public List<BrandLogModel> getParentBrandListByAccountId(long account_id, int game_id, long createTime, long endTime) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		if (createTime > 0) {

			Date startDate = new Date(createTime);

			Date endDate = null;
			if (endTime > 0) {
				endDate = new Date(endTime);
			} else {
				endDate = MyDateUtil.getTomorrowZeroDate(startDate);
			}
			query.addCriteria(Criteria.where("create_time").gte(MyDateUtil.getZeroDate(startDate)).lte(endDate));
		} else {
			query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(new Date(), -2)));
		}
		if (game_id != 0) {
			query.addCriteria(Criteria.where("game_id").is(game_id));
		}
		query.addCriteria(Criteria.where("account_id").is(account_id));
		query.addCriteria(Criteria.where("log_type").is(ELogType.accountBrand.getId()));
		query.addCriteria(new Criteria().orOperator(Criteria.where("clubMatchId").is(null), Criteria.where("clubMatchId").lte(0)));
		List<Integer> typeList = new ArrayList<>();
		typeList.add(GameConstants.CREATE_ROOM_MATCH);
		typeList.add(GameConstants.CREATE_ROOM_NEW_COIN);
		query.addCriteria(Criteria.where("createType").nin(typeList));
		query.with(new Sort(Direction.DESC, "create_time"));
		query.limit(15);// 15局
		List<BrandLogModel> brandLogModelList = mongoDBService.getMongoTemplate().find(query, BrandLogModel.class);

		List<Long> brandIdList = Lists.newArrayList();
		for (BrandLogModel model : brandLogModelList) {
			if (model.getBrand_id() == 0L)
				continue;
			brandIdList.add(model.getBrand_id());
		}

		// 转成大局记录
		query = new Query();

		if (createTime > 0) {

			Date startDate = new Date(createTime);

			Date endDate = null;
			if (endTime > 0) {
				endDate = new Date(endTime);
			} else {
				endDate = MyDateUtil.getTomorrowZeroDate(startDate);
			}
			query.addCriteria(Criteria.where("create_time").gte(MyDateUtil.getZeroDate(startDate)).lte(endDate));
		} else {
			query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(new Date(), -2)));
		}

		query.addCriteria(Criteria.where("log_type").is(ELogType.parentBrand.getId()));
		query.addCriteria(Criteria.where("brand_id").in(brandIdList));
		brandLogModelList = mongoDBService.getMongoTemplate().find(query, BrandLogModel.class);

		return brandLogModelList;
	}

	/**
	 * 玩家的最后10条大牌局记录(2天内的记录) (按大局分页)
	 * 
	 * @param account_id
	 */
	public List<BrandLogModel> getProxyParentBrandListByAccountId(Page page, long create_account_id, int game_id, long createTime, long endTime) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();

		if (createTime > 0) {

			Date startDate = new Date(createTime);

			Date endDate = null;
			if (endTime > 0) {
				endDate = new Date(endTime);
			} else {
				endDate = MyDateUtil.getTomorrowZeroDate(startDate);
			}
			query.addCriteria(Criteria.where("create_time").gte(MyDateUtil.getZeroDate(startDate)).lte(endDate));
		} else {
			query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(new Date(), -3)));
		}
		if (game_id != 0) {
			query.addCriteria(Criteria.where("game_id").is(game_id));
		}
		query.addCriteria(Criteria.where("create_account_id").is(create_account_id));
		query.addCriteria(Criteria.where("log_type").is(ELogType.accountBrand.getId()));
		List<Integer> typeList = new ArrayList<>();
		typeList.add(GameConstants.CREATE_ROOM_MATCH);
		typeList.add(GameConstants.CREATE_ROOM_NEW_COIN);
		query.addCriteria(Criteria.where("createType").nin(typeList));
		query.with(new Sort(Direction.DESC, "create_time"));

		List<BrandLogModel> brandLogModelList = mongoDBService.getMongoTemplate().find(query, BrandLogModel.class);

		List<Long> brandIdList = Lists.newArrayList();
		for (BrandLogModel model : brandLogModelList) {
			if (model.getBrand_id() == 0L)
				continue;
			brandIdList.add(model.getBrand_id());
		}

		// 转成大局记录
		query = new Query();

		if (createTime > 0) {

			Date startDate = new Date(createTime);

			Date endDate = null;
			if (endTime > 0) {
				endDate = new Date(endTime);
			} else {
				endDate = MyDateUtil.getTomorrowZeroDate(startDate);
			}
			query.addCriteria(Criteria.where("create_time").gte(MyDateUtil.getZeroDate(startDate)).lte(endDate));
		} else {
			query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(new Date(), -3)));
		}

		query.addCriteria(Criteria.where("log_type").is(ELogType.parentBrand.getId()));
		query.addCriteria(Criteria.where("createType").is(GameConstants.CREATE_ROOM_PROXY));
		query.addCriteria(Criteria.where("brand_id").in(brandIdList));
		query.with(new Sort(Direction.DESC, "create_time"));
		// 总数
		int count = (int) mongoDBService.getMongoTemplate().count(query, BrandLogModel.class);
		page.setTotalSize(count);
		// 分页
		if (game_id != EGameType.NIUNIU.getId()) {
			query.skip(page.getBeginNum());
			query.limit(page.getPageSize());
		}
		brandLogModelList = mongoDBService.getMongoTemplate().find(query, BrandLogModel.class);

		return brandLogModelList;
	}

	/**
	 * 玩家的最后10条大牌局记录(2天内的记录) (未按大局分页)
	 * 
	 * @param account_id
	 */
	public List<BrandLogModel> getProxyParentBrandListByAccountId(long create_account_id, int game_id, long createTime, long endTime) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		if (createTime > 0) {

			Date startDate = new Date(createTime);

			Date endDate = null;
			if (endTime > 0) {
				endDate = new Date(endTime);
			} else {
				endDate = MyDateUtil.getTomorrowZeroDate(startDate);
			}
			query.addCriteria(Criteria.where("create_time").gte(MyDateUtil.getZeroDate(startDate)).lte(endDate));
		} else {
			query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(new Date(), -3)));
		}

		if (game_id != 0) {
			query.addCriteria(Criteria.where("game_id").is(game_id));
		}

		query.addCriteria(Criteria.where("create_account_id").is(create_account_id));
		query.addCriteria(Criteria.where("log_type").is(ELogType.accountBrand.getId()));
		List<Integer> typeList = new ArrayList<>();
		typeList.add(GameConstants.CREATE_ROOM_MATCH);
		typeList.add(GameConstants.CREATE_ROOM_NEW_COIN);
		query.addCriteria(Criteria.where("createType").nin(typeList));
		query.with(new Sort(Direction.DESC, "create_time"));
		if (game_id != EGameType.NIUNIU.getId()) {
			query.limit(15);// 15局
		}
		List<BrandLogModel> brandLogModelList = mongoDBService.getMongoTemplate().find(query, BrandLogModel.class);

		List<Long> brandIdList = Lists.newArrayList();
		for (BrandLogModel model : brandLogModelList) {
			if (model.getBrand_id() == 0L)
				continue;
			brandIdList.add(model.getBrand_id());
		}

		// 转成大局记录
		query = new Query();

		if (createTime > 0) {

			Date startDate = new Date(createTime);

			Date endDate = null;
			if (endTime > 0) {
				endDate = new Date(endTime);
			} else {
				endDate = MyDateUtil.getTomorrowZeroDate(startDate);
			}
			query.addCriteria(Criteria.where("create_time").gte(MyDateUtil.getZeroDate(startDate)).lte(endDate));
		} else {
			query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(new Date(), -3)));
		}
		query.addCriteria(Criteria.where("log_type").is(ELogType.parentBrand.getId()));
		query.addCriteria(Criteria.where("brand_id").in(brandIdList));
		brandLogModelList = mongoDBService.getMongoTemplate().find(query, BrandLogModel.class);

		return brandLogModelList;
	}

	public int getProxyParentBrandListByAccountIdCountNew(long create_account_id, int game_id) {
		Date now = new Date();
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(now, -2)));
		if (game_id != 0) {
			query.addCriteria(Criteria.where("game_id").is(game_id));
		}
		query.addCriteria(Criteria.where("create_account_id").is(create_account_id));
		query.addCriteria(Criteria.where("log_type").is(ELogType.accountBrand.getId()));
		List<Integer> typeList = new ArrayList<>();
		typeList.add(GameConstants.CREATE_ROOM_MATCH);
		typeList.add(GameConstants.CREATE_ROOM_NEW_COIN);
		query.addCriteria(Criteria.where("createType").nin(typeList));
		query.with(new Sort(Direction.DESC, "create_time"));
		SysParamModel sysParamModel1000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(1000);
		int value = 50;
		if (sysParamModel1000 != null && sysParamModel1000.getVal5() != 0) {
			value = sysParamModel1000.getVal5();
		}
		query.limit(value);
		List<BrandLogModel> brandLogModelList = mongoDBService.getMongoTemplate().find(query, BrandLogModel.class);

		List<Long> brandIdList = Lists.newArrayList();
		for (BrandLogModel model : brandLogModelList) {
			if (model.getBrand_id() == 0L)
				continue;
			brandIdList.add(model.getBrand_id());
		}

		// 转成大局记录
		query = new Query();

		query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(now, -2)));
		query.addCriteria(Criteria.where("log_type").is(ELogType.parentBrand.getId()));
		query.addCriteria(Criteria.where("brand_id").in(brandIdList));
		query.with(new Sort(Direction.DESC, "create_time"));
		long size = mongoDBService.getMongoTemplate().count(query, BrandLogModel.class);

		return (int) size;
	}

	/**
	 * 玩家的最后10条大牌局记录(2天内的记录) TODO 优化
	 * 
	 * @param account_id
	 */
	public List<BrandLogModel> getProxyParentBrandListByAccountIdNew(Page page, long create_account_id, int game_id) {
		Date now = new Date();
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(now, -2)));
		if (game_id != 0) {
			query.addCriteria(Criteria.where("game_id").is(game_id));
		}
		query.addCriteria(Criteria.where("create_account_id").is(create_account_id));
		query.addCriteria(Criteria.where("log_type").is(ELogType.accountBrand.getId()));
		List<Integer> typeList = new ArrayList<>();
		typeList.add(GameConstants.CREATE_ROOM_MATCH);
		typeList.add(GameConstants.CREATE_ROOM_NEW_COIN);
		query.addCriteria(Criteria.where("createType").nin(typeList));
		query.with(new Sort(Direction.DESC, "create_time"));
		SysParamModel sysParamModel1000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(1000);
		int value = 50;
		if (sysParamModel1000 != null && sysParamModel1000.getVal5() != 0) {
			value = sysParamModel1000.getVal5();
		}
		query.limit(value);
		List<BrandLogModel> brandLogModelList = mongoDBService.getMongoTemplate().find(query, BrandLogModel.class);
		List<Long> brandIdList = Lists.newArrayList();
		for (BrandLogModel model : brandLogModelList) {
			if (model.getBrand_id() == 0L)
				continue;
			brandIdList.add(model.getBrand_id());
		}

		// 转成大局记录
		query = new Query();
		query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(now, -2)));
		query.addCriteria(Criteria.where("log_type").is(ELogType.parentBrand.getId()));
		query.addCriteria(Criteria.where("brand_id").in(brandIdList));
		query.skip(page.getBeginNum());
		query.limit(page.getPageSize());
		query.with(new Sort(Direction.DESC, "create_time"));
		brandLogModelList = mongoDBService.getMongoTemplate().find(query, BrandLogModel.class);

		return brandLogModelList;
	}

	/**
	 * 开放助手· 玩家的最后10条大牌局记录(2天内的记录) TODO 优化
	 * 
	 * @param account_id
	 */
	public List<BrandLogModel> getAssistantParentBrandListByAccountId(Page page, long account_id, int game_id, long createTime, long endTime) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		if (createTime > 0) {

			Date startDate = new Date(createTime);

			Date endDate = null;
			if (endTime > 0) {
				endDate = new Date(endTime);
			} else {
				endDate = MyDateUtil.getTomorrowZeroDate(startDate);
			}
			query.addCriteria(Criteria.where("create_time").gte(MyDateUtil.getZeroDate(startDate)).lte(endDate));
		} else {
			query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(new Date(), -2)));
		}

		if (game_id != 0) {
			query.addCriteria(Criteria.where("game_id").is(game_id));
		}

		query.addCriteria(Criteria.where("groupID").ne(""));
		query.addCriteria(Criteria.where("account_id").is(account_id));
		query.addCriteria(Criteria.where("log_type").is(ELogType.accountBrand.getId()));
		List<Integer> typeList = new ArrayList<>();
		typeList.add(GameConstants.CREATE_ROOM_MATCH);
		typeList.add(GameConstants.CREATE_ROOM_NEW_COIN);
		query.addCriteria(Criteria.where("createType").nin(typeList));

		/*
		 * SysParamModel sysParamModel1000 =
		 * SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(
		 * 1000); int value = 50; if (sysParamModel1000 != null &&
		 * sysParamModel1000.getVal5() != 0) { value =
		 * sysParamModel1000.getVal5(); }
		 */
		/*
		 * query.skip(page.getBeginNum()); query.limit(page.getPageSize());
		 */
		List<BrandLogModel> brandLogModelList = mongoDBService.getMongoTemplate().find(query, BrandLogModel.class);
		List<Long> brandIdList = Lists.newArrayList();
		for (BrandLogModel model : brandLogModelList) {
			if (model.getBrand_id() == 0L)
				continue;
			brandIdList.add(model.getBrand_id());
		}

		// 转成大局记录
		query = new Query();

		if (createTime > 0) {

			Date startDate = new Date(createTime);

			Date endDate = null;
			if (endTime > 0) {
				endDate = new Date(endTime);
			} else {
				endDate = MyDateUtil.getTomorrowZeroDate(startDate);
			}
			query.addCriteria(Criteria.where("create_time").gte(MyDateUtil.getZeroDate(startDate)).lte(endDate));
		} else {
			query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(new Date(), -2)));
		}

		query.addCriteria(Criteria.where("log_type").is(ELogType.parentBrand.getId()));
		query.addCriteria(Criteria.where("brand_id").in(brandIdList));
		query.with(new Sort(Direction.DESC, "create_time"));
		// 总数
		int count = (int) mongoDBService.getMongoTemplate().count(query, BrandLogModel.class);
		page.setTotalSize(count);
		// 分页
		query.skip(page.getBeginNum());
		query.limit(page.getPageSize());
		brandLogModelList = mongoDBService.getMongoTemplate().find(query, BrandLogModel.class);

		return brandLogModelList;
	}

	
	public void insert_Model(Serializable card) {
		logQueue.add(card);
	}
	
	
	/**
	 * NEW 俱乐部大牌局记录
	 * 
	 * @param account_id
	 * 
	 */
	public List<BrandLogModel> getClubParentBrandList(Page page, int clubId, long createTime, long endTime) {
		return getClubParentBrandList(page, clubId, createTime, endTime, null);
	}

	/**
	 * NEW
	 * 
	 * @param page
	 * @param clubId
	 * @param createTime
	 * @param endTime
	 * @param conditionParam
	 * @return
	 */
	public List<BrandLogModel> getClubParentBrandList(Page page, int clubId, long createTime, long endTime, Map<String, Object> conditionParam) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);

		Query query = new Query();
		if (createTime > 0) {

			Date startDate = new Date(createTime);

			Date endDate = null;
			if (endTime > 0) {
				endDate = new Date(endTime);
			} else {
				endDate = MyDateUtil.getTomorrowZeroDate(startDate);
			}
			// query.addCriteria(Criteria.where("create_time").gte(MyDateUtil.getZeroDate(startDate)).lte(endDate));
			query.addCriteria(Criteria.where("create_time").gte(startDate).lte(endDate));
		} else {
			query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(new Date(), -3)));
		}
		query.addCriteria(Criteria.where("log_type").is(ELogType.parentBrand.getId()));
		query.addCriteria(Criteria.where("club_id").is(clubId));

		if (null != conditionParam) {
			conditionParam.forEach((k, v) -> {
				query.addCriteria(Criteria.where(k).is(v));
			});
		}

		if (null != page) {
			query.skip(page.getBeginNum());
			query.limit(page.getPageSize());
		}
		// 过滤俱乐部比赛场的战绩
		query.addCriteria(new Criteria().orOperator(Criteria.where("clubMatchId").is(null), Criteria.where("clubMatchId").lte(0)));
		query.addCriteria(Criteria.where("isRealKouDou").is(true));
		query.with(new Sort(Direction.DESC, "create_time"));

		if (null != page) {
			// 总数
			int count = (int) mongoDBService.getMongoTemplate().count(query, BrandLogModel.class);
			page.setTotalSize(count);
		}
		List<BrandLogModel> brandLogModelList = mongoDBService.getMongoTemplate().find(query, BrandLogModel.class);

		return brandLogModelList;
	}

	/**
	 * 俱乐部玩家的最后条大牌局记录(2天内的记录)
	 * 
	 * @param account_id
	 * 
	 */
	public List<BrandLogModel> getClubParentBrandList(Page page, int clubId, long targetAccountId, long createTime, long endTime, int ruleId) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		DBObject dbObject = new BasicDBObject();
		Query query = new BasicQuery(dbObject);

		if (createTime > 0) {

			Date startDate = new Date(createTime);

			Date endDate = null;
			if (endTime > 0) {
				endDate = new Date(endTime);
			} else {
				endDate = MyDateUtil.getTomorrowZeroDate(startDate);
			}
			query.addCriteria(Criteria.where("create_time").gte(MyDateUtil.getZeroDate(startDate)).lte(endDate));
		} else {
			query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(new Date(), -3)));
		}
		query.addCriteria(Criteria.where("account_id").is(targetAccountId));
		query.addCriteria(Criteria.where("club_id").is(clubId));
		query.addCriteria(new Criteria().orOperator(Criteria.where("clubMatchId").is(null), Criteria.where("clubMatchId").lte(0)));
		query.addCriteria(Criteria.where("log_type").is(ELogType.accountBrand.getId()));

		query.with(new Sort(Direction.DESC, "create_time"));

		List<BrandLogModel> brandLogModelList = mongoDBService.getMongoTemplate().find(query, BrandLogModel.class);
		List<Long> brandIdList = Lists.newArrayList();
		for (BrandLogModel brand : brandLogModelList) {
			if (brand.getBrand_id() == 0L)
				continue;

			brandIdList.add(brand.getBrand_id());
		}

		// 转成大局记录
		query = new Query();

		if (createTime > 0) {

			Date startDate = new Date(createTime);

			Date endDate = null;
			if (endTime > 0) {
				endDate = new Date(endTime);
			} else {
				endDate = MyDateUtil.getTomorrowZeroDate(startDate);
			}
			query.addCriteria(Criteria.where("create_time").gte(MyDateUtil.getZeroDate(startDate)).lte(endDate));
		} else {
			query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(new Date(), -3)));
		}

		query.addCriteria(Criteria.where("log_type").is(ELogType.parentBrand.getId()));
		query.addCriteria(Criteria.where("brand_id").in(brandIdList));
		query.addCriteria(Criteria.where("isRealKouDou").is(true));
		if (ruleId > 0) {
			query.addCriteria(Criteria.where("ruleId").is(ruleId));
		}
		query.with(new Sort(Direction.DESC, "create_time"));
		// 总数
		int count = (int) mongoDBService.getMongoTemplate().count(query, BrandLogModel.class);
		page.setTotalSize(count);
		// 分页
		query.skip(page.getBeginNum());
		query.limit(page.getPageSize());
		List<BrandLogModel> brandLogModels = mongoDBService.getMongoTemplate().find(query, BrandLogModel.class);

		return brandLogModels;
	}

	/**
	 * 根据随机码获取Id
	 * 
	 * @param account_id
	 * 
	 */
	public BrandLogModel getParentBrandByRandomNum(int randomNum) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);

		Query query = new Query();
		query.addCriteria(Criteria.where("randomNum").is(randomNum));
		query.addCriteria(Criteria.where("log_type").is(ELogType.parentBrand.getId()));
		// query.addCriteria(Criteria.where("isRealKouDou").is(true));

//		query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(new Date(), -3)));
		List<Integer> typeList = new ArrayList<>();
		typeList.add(GameConstants.CREATE_ROOM_MATCH);
		typeList.add(GameConstants.CREATE_ROOM_NEW_COIN);
		query.addCriteria(Criteria.where("createType").nin(typeList));
		query.with(new Sort(Direction.DESC, "create_time"));
		BrandLogModel brandLogModelList = mongoDBService.getMongoTemplate().findOne(query, BrandLogModel.class);

		return brandLogModelList;
	}

	/**
	 * 根据随机码获取Id
	 * 
	 * @param account_id
	 * 
	 */
	public BrandLogModel getParentBrandByRoomID(int roomId) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);

		Query query = new Query();
		query.addCriteria(Criteria.where("v3").is(roomId + ""));
		query.addCriteria(Criteria.where("log_type").is(ELogType.parentBrand.getId()));
		// query.addCriteria(Criteria.where("isRealKouDou").is(true));

		query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(new Date(), -2)));
		//		List<Integer> typeList = new ArrayList<>();
		//		typeList.add(GameConstants.CREATE_ROOM_MATCH);
		//		typeList.add(GameConstants.CREATE_ROOM_NEW_COIN);
		//		query.addCriteria(Criteria.where("createType").nin(typeList));//这里不能加
		query.with(new Sort(Direction.DESC, "create_time"));
		BrandLogModel brandLogModelList = mongoDBService.getMongoTemplate().findOne(query, BrandLogModel.class);

		return brandLogModelList;
	}

	/**
	 * 根据父牌局Id获取战绩
	 */
	public BrandLogModel getParentBrandByParentId(long parentId) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);

		Query query = new Query();
		query.addCriteria(Criteria.where("brand_id").is(parentId));
		query.addCriteria(Criteria.where("log_type").is(ELogType.parentBrand.getId()));
		// query.addCriteria(Criteria.where("isRealKouDou").is(true));

		//		query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(new Date(), -2)));
		//		List<Integer> typeList = new ArrayList<>();
		//		typeList.add(GameConstants.CREATE_ROOM_MATCH);
		//		typeList.add(GameConstants.CREATE_ROOM_NEW_COIN);
		//		query.addCriteria(Criteria.where("createType").nin(typeList));//这里不能加
		query.with(new Sort(Direction.DESC, "create_time"));
		BrandLogModel brandLogModelList = mongoDBService.getMongoTemplate().findOne(query, BrandLogModel.class);

		return brandLogModelList;
	}


	/**
	 * @param brand_parent_id
	 */
	public BrandLogModel getParentBrandByParentId(long brand_parent_id, int game_id) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		// query.addCriteria(Criteria.where("game_id").is(game_id));
		query.addCriteria(Criteria.where("brand_id").is(brand_parent_id));
		query.addCriteria(Criteria.where("log_type").is(ELogType.parentBrand.getId()));
		List<Integer> typeList = new ArrayList<>();
		typeList.add(GameConstants.CREATE_ROOM_MATCH);
		typeList.add(GameConstants.CREATE_ROOM_NEW_COIN);
		query.addCriteria(Criteria.where("createType").nin(typeList));
		BrandLogModel brandLogModel = mongoDBService.getMongoTemplate().findOne(query, BrandLogModel.class);
		return brandLogModel;
	}

	/**
	 * 根据大牌局获取所有子牌局(60天内的记录) TODO 优化 这里只能获取到video_record这个字段的数据
	 * 
	 * @param brand_parent_id
	 * @return
	 */
	public List<BrandChildLogModel> getChildVideoBrandList(long brand_parent_id, int game_id) {
		Date now = new Date();
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);

		Query query = new Query();
		// query.addCriteria(Criteria.where("game_id").is(game_id));
//		query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(now, -10)));
		query.addCriteria(Criteria.where("brand_parent_id").is(brand_parent_id));
		// query.addCriteria(Criteria.where("log_type").is(ELogType.childBrand.getId()));
		query.with(new Sort(Direction.ASC, "create_time"));
		query.limit(30);

		List<BrandChildLogModel> brandLogModelList = mongoDBService.getMongoTemplate().find(query, BrandChildLogModel.class);
		return brandLogModelList;
	}

	/**
	 * 查询指定的子牌局 这里只能获取到video_record这个字段的数据
	 * 
	 * @param brand_id
	 * @param game_id
	 * @return
	 */
	public BrandChildLogModel getChildBrandVideoByBrandId(long brand_id, int game_id) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);

		Query query = new Query();
		// query.addCriteria(Criteria.where("game_id").is(game_id));
		query.addCriteria(Criteria.where("brand_id").is(brand_id));
		// query.addCriteria(Criteria.where("log_type").is(ELogType.childBrand.getId()));
		BrandChildLogModel brandLogModel = mongoDBService.getMongoTemplate().findOne(query, BrandChildLogModel.class);
		return brandLogModel;
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
	 * 获取玩家最近一次比赛Id
	 * 
	 * @param accountId
	 * @param account_id
	 * 
	 */
	public MatchPlayerLogModel getMatchRankById(int id, long accountId) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);

		Query query = new Query();
		query.addCriteria(Criteria.where("unqueueId").is(id));
		query.addCriteria(Criteria.where("accountId").is(accountId));
		query.with(new Sort(Direction.DESC, "startTime"));
		MatchPlayerLogModel log = mongoDBService.getMongoTemplate().findOne(query, MatchPlayerLogModel.class);

		return log;
	}

	/**
	 * 获取玩家最近一次比赛Id
	 * 
	 * @param accountId
	 * @param account_id
	 * 
	 */
	public List<MatchPlayerLogModel> getMatchRankListById(long accountId) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);

		Query query = new Query();
		query.addCriteria(Criteria.where("accountId").is(accountId));
		query.addCriteria(Criteria.where("hasPrize").is(true));
		query.addCriteria(Criteria.where("isNoShow").is(false));
		Date date = DateUtils.addDays(MyDateUtil.getZeroDate(new Date()), -6);
		query.addCriteria(Criteria.where("startTime").gte(date));
		query.with(new Sort(Direction.DESC, "startTime"));

		List<MatchPlayerLogModel> log = mongoDBService.getMongoTemplate().find(query, MatchPlayerLogModel.class);

		return log;
	}

	/**
	 * @param accountId
	 * @param id
	 * @param account_id
	 * 
	 */
	public List<TurntableLogModel> getTurntableLogs(long accountId, int id) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);

		Query query = new Query();
		query.addCriteria(Criteria.where("activityId").is(id));
		query.with(new Sort(Direction.DESC, "create_time"));
		if (accountId > 0) {
			query.addCriteria(Criteria.where("accountId").is(accountId));
		} else {
			query.limit(20);
		}

		List<TurntableLogModel> log = mongoDBService.getMongoTemplate().find(query, TurntableLogModel.class);

		return log;
	}

	/**
	 * 代理转卡历史记录
	 * 
	 * @param page
	 * @param account_id
	 * @return
	 */
	public List<ProxyGoldLogModel> getProxyGoldLogModelList(Page page, long account_id, Long target_account_id) {
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
		// -15)));
		if (target_account_id != null) {
			query.addCriteria(Criteria.where("target_account_id").is(target_account_id));
		}
		query.with(new Sort(Direction.DESC, "create_time"));
		long count = mongoDBService.getMongoTemplate().count(query, ProxyGoldLogModel.class);
		return (int) count;
	}

	/**
	 * 红包发送记录
	 * 
	 * @param account_id
	 * @return
	 */
	public List<RedPackageModel> getRedPackageModelList(long account_id) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("account_id").is(account_id));
		query.with(new Sort(Direction.DESC, "create_time"));
		query.limit(20);
		List<RedPackageModel> list = mongoDBService.getMongoTemplate().find(query, RedPackageModel.class);
		return list;
	}

	public List<Entry<Long, Long>> getRedPackageRankByActiveId() {
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
		return list2;
	}

	public List<Entry<Long, Long>> getInviteRedPacketRank() {
		// DBObject groupFields = new BasicDBObject("_id", "$account_id");
		// groupFields.put("total", new BasicDBObject("$sum", 1));
		// DBObject group = new BasicDBObject("$group", groupFields);
		// // sort
		// DBObject sort = new BasicDBObject("$sort", new BasicDBObject("total",
		// -1));
		// // limit
		// DBObject limit = new BasicDBObject("$limit", 10);
		// MongoDBService mongoDBService =
		// SpringService.getBean(MongoDBService.class);
		// List<InviteModel> inModelList =
		// mongoDBService.getMongoTemplate().findAll(InviteModel.class);
		// AggregationOutput output =
		// mongoDBService.getMongoTemplate().getCollection("invite_red_packet").aggregate(group,
		// sort, limit);
		// Iterable<DBObject> list = output.results();
		// TreeMap<Long, Long> map = new TreeMap<Long, Long>();
		// for (DBObject dbObject : list) {
		// map.put(Long.parseLong(String.valueOf(dbObject.get("_id"))),
		// Long.parseLong(String.valueOf(dbObject.get("total"))));
		// }
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		List<InviteModel> inModelList = mongoDBService.getMongoTemplate().findAll(InviteModel.class);
		// AggregationOutput output =
		// mongoTemplate.getCollection("invite_red_packet").aggregate(group,
		// sort, limit);
		AggregationOperation match = Aggregation.match(Criteria.where("state").is(1));
		AggregationOperation sort = Aggregation.sort(new Sort(Sort.Direction.DESC, "total"));
		AggregationOperation limit = Aggregation.limit(10);
		AggregationOperation group = Aggregation.group("account_id").count().as("total");
		Aggregation aggregation = Aggregation.newAggregation(match, group, sort, limit);
		AggregationResults<HashMap> output = mongoDBService.getMongoTemplate().aggregate(aggregation, "invite_red_packet", HashMap.class);
		List<HashMap> list = output.getMappedResults();
		TreeMap<Long, Long> map = new TreeMap<Long, Long>();
		for (HashMap hashMap : list) {
			map.put(Long.parseLong(String.valueOf(hashMap.get("_id"))), Long.parseLong(String.valueOf(hashMap.get("total"))));
		}
		for (InviteModel mo : inModelList) {
			map.put(mo.getAccount_id(), mo.getTotal());
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

	/**
	 * 个人红包领取记录
	 * 
	 * @param accountId
	 * @return
	 */
	public List<RedPackageRecordModel> queryRedPackageRecordList(long accountId) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("account_id").is(accountId));
		query.with(new Sort(Direction.DESC, "create_time"));
		List<RedPackageRecordModel> list = mongoDBService.getMongoTemplate().find(query, RedPackageRecordModel.class);
		return list;

	}

	/**
	 * 
	 * 
	 */
	public Map<Integer, ClubLogModel> searchClubLogModel(int clubId, long createTime, long endTime) {

		Query query = new Query();
		query.addCriteria(Criteria.where("clubId").is(clubId));// .and("roomID").is(roomId));

		if (createTime > 0) {

			Date startDate = new Date(createTime);

			Date endDate = null;
			if (endTime > 0) {
				endDate = new Date(endTime);
			} else {
				endDate = MyDateUtil.getTomorrowZeroDate(startDate);
			}
			query.addCriteria(Criteria.where("create_time").gte(MyDateUtil.getZeroDate(startDate)).lte(endDate));
		} else {
			query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(new Date(), -3)));
		}

		List<ClubLogModel> logs = SpringService.getBean(MongoDBService.class).getMongoTemplate().find(query, ClubLogModel.class);
		if (null == logs || logs.isEmpty()) {
			return Maps.newHashMap();
		}

		BinaryOperator<ClubLogModel> binaryOperator = (a1, a2) -> {
			Long a1V = a1.getV1();
			Long a2V = a2.getV1();
			if (null != a1V && null != a2V) {
				a1.setV1(a1V.longValue() + a2V.longValue());
			}
			return a1; // 出现相同房间号的，直接累加返回第一个 GAME-TODO ，临时解决方案
		};
		return logs.stream().collect(Collectors.toMap(ClubLogModel::getRoomID, ClubLogModel -> ClubLogModel, binaryOperator));
	}

	/**
	 * 
	 * @param clubId
	 * @param brand_id
	 * @return
	 */
	public BrandLogModel searchBrandLogModel(int clubId, long brand_id) {

		Query query = new Query();
		query.addCriteria(Criteria.where("club_id").is(clubId));
		query.addCriteria(Criteria.where("brand_id").is(brand_id));
		query.addCriteria(Criteria.where("log_type").is("parentBrand"));
		return SpringService.getBean(MongoDBService.class).getMongoTemplate().findOne(query, BrandLogModel.class);
	}

	/**
	 * 统计时间段内闲逸豆消耗
	 * 
	 * @param openType
	 *            开房类型
	 * @param account_id
	 *            玩家ID
	 * @param game_id
	 *            游戏ID
	 * @param createTime
	 *            开始时间
	 * @param endTime
	 *            结束时间
	 * @return
	 */
	public int getOpenRoomConsume(int openType, long account_id, int game_id, long createTime, long endTime) {
		Criteria criteria = Criteria.where("account_id").is(account_id).and("v2").is(EGoldOperateType.REAL_OPEN_ROOM.getId());
		if (openType > -1) {
			criteria.and("openRoomWay").is(openType);
		}
		if (game_id > 0) {
			criteria.and("game_id").is(game_id);
		}

		criteria.and("create_time").gte(new Date(createTime)).lt(new Date(endTime));
		AggregationOperation match = Aggregation.match(criteria);
		AggregationOperation group = Aggregation.group().sum("v1").as("count");
		Aggregation aggregation = Aggregation.newAggregation(match, group);
		AggregationResults<GiveCardModel> result = SpringService.getBean(MongoDBService.class).getMongoTemplate().aggregate(aggregation, "game_log",
				GiveCardModel.class);
		List<GiveCardModel> list = result.getMappedResults();
		int count = 0;
		if (list.size() > 0) {
			for (GiveCardModel model : list) {
				if (model.getCount() == null) {
					continue;
				}
				count = model.getCount().intValue();
			}
		}
		return count;
	}

	/**
	 * 统计时间段内专属豆消耗
	 */
	public int getOpenRoomExclusiveConsume(int openType, long account_id, int game_id, long createTime, long endTime) {
		// 专属豆统计
		Criteria criteria = Criteria.where("account_id").is(account_id).and("operateType").is(EGoldOperateType.REAL_OPEN_ROOM.getId());
		if (openType > 0) {
			criteria.and("openRoomWay").is(openType);
		}
		if (game_id > 0) {
			criteria.and("appId").is(game_id);
		}

		criteria.and("create_time").gte(new Date(createTime)).lt(new Date(endTime));
		AggregationOperation match = Aggregation.match(criteria);
		AggregationOperation group = Aggregation.group().sum("v3").as("count");
		Aggregation aggregation = Aggregation.newAggregation(match, group);
		AggregationResults<GiveCardModel> result = SpringService.getBean(MongoDBService.class).getMongoTemplate()
				.aggregate(aggregation, "club_exclusive_gold_log", GiveCardModel.class);
		List<GiveCardModel> list = result.getMappedResults();
		int count = 0;
		if (list.size() > 0) {
			for (GiveCardModel model : list) {
				if (model.getCount() == null) {
					continue;
				}
				count = model.getCount().intValue();
			}
		}
		return count;
	}

	/**
	 * 统计时间段内闲逸豆消耗
	 * 
	 * @param openType
	 *            开房类型
	 * @param account_id
	 *            玩家ID
	 * @param game_id
	 *            游戏ID
	 * @param createTime
	 *            开始时间
	 * @param endTime
	 *            结束时间
	 * @return
	 */
	public int getOpenRoomConsumeClub(int clubId, long account_id, long createTime, long endTime) {
		Criteria criteria = Criteria.where("create_account_id").is(account_id).and("club_id").is(clubId).and("isRealKouDou").is(true).and("log_type")
				.is(ELogType.parentBrand.getId()).and("isExclusiveGold").is(false);
		criteria.and("create_time").gte(new Date(createTime)).lt(new Date(endTime));
		AggregationOperation match = Aggregation.match(criteria);
		AggregationOperation group = Aggregation.group().sum("gold_count").as("count");
		Aggregation aggregation = Aggregation.newAggregation(match, group);
		AggregationResults<GiveCardModel> result = SpringService.getBean(MongoDBService.class).getMongoTemplate()
				.aggregate(aggregation, "brand_log", GiveCardModel.class);
		List<GiveCardModel> list = result.getMappedResults();
		int count = 0;
		if (list.size() > 0) {
			for (GiveCardModel model : list) {
				if (model.getCount() == null) {
					continue;
				}
				count = model.getCount().intValue();
			}
		}
		return count;
	}

	public int getOpenRoomExclusiveConsumeClub(int clubId, long account_id, long createTime, long endTime) {
		Criteria criteria = Criteria.where("create_account_id").is(account_id).and("club_id").is(clubId).and("isRealKouDou").is(true).and("log_type")
				.is(ELogType.parentBrand.getId()).and("isExclusiveGold").is(true);
		criteria.and("create_time").gte(new Date(createTime)).lt(new Date(endTime));
		AggregationOperation match = Aggregation.match(criteria);
		AggregationOperation group = Aggregation.group().sum("gold_count").as("count");
		Aggregation aggregation = Aggregation.newAggregation(match, group);
		AggregationResults<GiveCardModel> result = SpringService.getBean(MongoDBService.class).getMongoTemplate().aggregate(aggregation, "brand_log",
				GiveCardModel.class);
		List<GiveCardModel> list = result.getMappedResults();
		int count = 0;
		if (list.size() > 0) {
			for (GiveCardModel model : list) {
				if (model.getCount() == null) {
					continue;
				}
				count = model.getCount().intValue();
			}
		}
		return count;
	}

	public double sumField(String collection, String filedName, Criteria criteria) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		double total = 0l;
		String reduce = "function(doc, aggr){" + "            aggr.total += parseFloat((Math.round((doc." + filedName + ")*100)/100).toFixed(2));"
				+ "       }";
		Query query = new Query();
		if (criteria != null) {
			query.addCriteria(criteria);
		}
		DBObject result = mongoDBService.getMongoTemplate().getCollection(collection).group(null, query.getQueryObject(),
				new BasicDBObject("total", total), reduce);

		Map<String, BasicDBObject> map = result.toMap();
		if (map.size() > 0) {
			BasicDBObject bdbo = map.get("0");
			if (bdbo != null && bdbo.get("total") != null)
				total = bdbo.getDouble("total");
		}
		return total;
	}

	/**
	 * 点赞
	 * 
	 * @param model
	 */
	public void updateBrandLogModel(BrandLogModel model) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("brand_id").is(model.getBrand_id()));
		Update update = Update.update("upvote", model.getUpvote());
		mongoDBService.getMongoTemplate().updateFirst(query, update, BrandLogModel.class);
	}

	// 获取邀请红包金额
	public InviteResultModel getInviteRedpacketReceive(long account_id) {
		Criteria criteria = Criteria.where("account_id").is(account_id);
		AggregationOperation match = Aggregation.match(criteria);
		AggregationOperation group = Aggregation.group().sum("receive").as("count");
		Aggregation aggregation = Aggregation.newAggregation(match, group);
		AggregationResults<InviteResultModel> result = SpringService.getBean(MongoDBService.class).getMongoTemplate().aggregate(aggregation,
				"invite_money", InviteResultModel.class);
		return result.getUniqueMappedResult();
	}

	// 获取邀请总人数
	public long getInvitePersonsCount(long account_id) {
		Query query = new Query();
		query.addCriteria(Criteria.where("account_id").is(account_id));
		long count = SpringService.getBean(MongoDBService.class).getMongoTemplate().count(query, InviteRedPacketModel.class);
		return count;
		// Criteria criteria = Criteria.where("account_id").is(account_id);
		// AggregationOperation match = Aggregation.match(criteria);
		// AggregationOperation group = Aggregation.group().count().as("total");
		// Aggregation aggregation = Aggregation.newAggregation(match, group);
		// AggregationResults<InviteResultModel> result =
		// SpringService.getBean(MongoDBService.class).getMongoTemplate().aggregate(aggregation,
		// "invite_red_packet",
		// InviteResultModel.class);
		// return result.getUniqueMappedResult();
	}

	public long getEffectiveInvitePersonsCount(long account_id) {
		Query query = new Query();
		query.addCriteria(Criteria.where("account_id").is(account_id).and("state").is(1));
		long count = SpringService.getBean(MongoDBService.class).getMongoTemplate().count(query, InviteRedPacketModel.class);
		return count;
		// Criteria criteria = Criteria.where("account_id").is(account_id);
		// AggregationOperation match = Aggregation.match(criteria);
		// AggregationOperation group = Aggregation.group().count().as("total");
		// Aggregation aggregation = Aggregation.newAggregation(match, group);
		// AggregationResults<InviteResultModel> result =
		// SpringService.getBean(MongoDBService.class).getMongoTemplate().aggregate(aggregation,
		// "invite_red_packet",
		// InviteResultModel.class);
		// return result.getUniqueMappedResult();
	}

	// 获取最后一个邀请用户的id
	public InviteRedPacketModel getLastInviteAccountId(long account_id) {
		Query query = new Query();
		query.addCriteria(Criteria.where("account_id").is(account_id));
		query.with(new Sort(Direction.DESC, "create_time")).limit(1);
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		List<InviteRedPacketModel> modelList = mongoDBService.getMongoTemplate().find(query, InviteRedPacketModel.class);
		return modelList.size() == 0 ? null : modelList.get(0);
	}

	// 获取被邀请的列表
	public List<InviteRedPacketModel> getInviteAccountList(long account_id, int pageSize, int limit) {
		Query query = new Query();
		query.addCriteria(Criteria.where("account_id").is(account_id));
		query.with(new Sort(Direction.DESC, "create_time")).skip((pageSize - 1) * limit).limit(limit);
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		List<InviteRedPacketModel> modelList = mongoDBService.getMongoTemplate().find(query, InviteRedPacketModel.class);
		return modelList;
	}

	/**
	 * 
	 * @param clubId
	 * @param brand_id
	 * @return
	 */
	public ClubActivityLogModel searchClubActivityModel(int clubId, long activityId) {

		Query query = new Query();
		query.addCriteria(Criteria.where("clubId").is(clubId));
		query.addCriteria(Criteria.where("id").is(activityId));
		return SpringService.getBean(MongoDBService.class).getMongoTemplate().findOne(query, ClubActivityLogModel.class);
	}

	@Override
	protected void startService() {
		mogoDBTimer = new MogoDBTimer();
		timer.schedule(mogoDBTimer, 1000, 1000);

	}

	/**
	 * 
	 * @param log
	 */
	public void mobileLog(MobileLogModel log) {
		logQueue.add(log);
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
	public void sessionCreate(C2SSession session) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sessionFree(C2SSession session) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dbUpdate(int _userID) {
		// TODO Auto-generated method stub
	}

	public LinkedBlockingQueue<Object> getLogQueue() {
		return logQueue;
	}

	public MogoDBTimer getMogoDBTimer() {
		return mogoDBTimer;
	}

	/**
	 * 商城购买日志
	 */
	public void storeBuyLog(long account_id, int storeType, int goodsId, int num, String goodsName, int gameType, int goodsType) {
		StoreBuyLogModel model = new StoreBuyLogModel();
		model.setCreate_time(new Date());
		model.setAccountId(account_id);
		model.setShopType(storeType);
		model.setItemId(goodsId);
		model.setItemNum(num);
		model.setItemName(goodsName);
		model.setGameType(gameType);
		model.setGoodsType(goodsType);
		logQueue.add(model);
	}

	/**
	 * 闲逸豆交易流水分页查询
	 * 
	 * @param account_id
	 * @param logType
	 * @param page
	 */
	public List<GameLogModel> goldcardTransLog(long account_id, ELogType logType, Page page) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("account_id").is(account_id));
		query.addCriteria(Criteria.where("log_type").is(logType.getId()));
		// 增加时间判断，只查询7天以内
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, -7);
		query.addCriteria(Criteria.where("create_time").gte(calendar.getTime()));
		// 过滤真实开房的记录，此条记录不需要显示
		query.addCriteria(Criteria.where("v2").nin(EGoldOperateType.REAL_OPEN_ROOM.getId()));
		// 查询总页数
		int totalSize = (int) mongoDBService.getMongoTemplate().count(query, GameLogModel.class);
		page.setTotalSize(totalSize);
		query.with(new Sort(Direction.DESC, "create_time"));
		query.with(new Sort(Direction.DESC, "id"));
		query.skip(page.getBeginNum());
		query.limit(page.getPageSize());
		List<GameLogModel> gameLogModelList = mongoDBService.getMongoTemplate().find(query, GameLogModel.class);
		return gameLogModelList;
	}
	
	/**
	 * 俱乐部专属豆交易流水分页查询
	 * @param account_id
	 * @param logType
	 * @param page
	 * @return
	 */
	public List<ClubExclusiveGoldLogModel> clubExclusiveTransLog(long account_id, Page page) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("account_id").is(account_id));
		// 增加时间判断，只查询7天以内
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, -7);
		query.addCriteria(Criteria.where("create_time").gte(calendar.getTime()));
		// 过滤真实开房的记录，此条记录不需要显示
		query.addCriteria(Criteria.where("operateType").nin(EGoldOperateType.REAL_OPEN_ROOM.getId()));
		// 查询总页数
		int totalSize = (int) mongoDBService.getMongoTemplate().count(query, ClubExclusiveGoldLogModel.class);
		page.setTotalSize(totalSize);
		query.with(new Sort(Direction.DESC, "create_time"));
		query.with(new Sort(Direction.DESC, "id"));
		query.skip(page.getBeginNum());
		query.limit(page.getPageSize());
		List<ClubExclusiveGoldLogModel> gameLogModelList = mongoDBService.getMongoTemplate().find(query, ClubExclusiveGoldLogModel.class);
		return gameLogModelList;
	}

	public VoiceChatLogModel getVoiceChat(VoiceChatRequestProto req) {
		int type = req.getType();
		long uniqueId = req.getUniqueId();
		int clubId = 0;
		if (type == VoiceChatType.CLUB) {
			clubId = req.getClubId();

		}
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("type").is(type));
		if (clubId > 0) {
			query.addCriteria(Criteria.where("clubId").is(clubId));
		}
		query.addCriteria(Criteria.where("uniqueId").is(uniqueId));
		List<VoiceChatLogModel> logModelList = mongoDBService.getMongoTemplate().find(query, VoiceChatLogModel.class);
		if (logModelList.size() > 0)
			return logModelList.get(0);
		return null;
	}

	/**
	 * 获取亲友圈自建赛战绩
	 */
	public List<BrandLogModel> getClubMatchParentBrandList(long clubMatchId, long accountId, int clubId) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("clubMatchId").is(clubMatchId));
		query.addCriteria(Criteria.where("log_type").is(ELogType.parentBrand.getId()));

		if (accountId > 0) {
			query.addCriteria(Criteria.where("accountIds").is(accountId));
		}
		query.addCriteria(Criteria.where("club_id").is(clubId));
		query.with(new Sort(Direction.DESC, "create_time"));
		List<BrandLogModel> brandLogModelList = mongoDBService.getMongoTemplate().find(query, BrandLogModel.class);

		return brandLogModelList;
	}

}
