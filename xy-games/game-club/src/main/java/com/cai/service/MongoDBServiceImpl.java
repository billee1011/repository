package com.cai.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.LinkedBlockingQueue;

import com.cai.common.define.ELogType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.ClubCreateMatchLogModel;
import com.cai.common.domain.ClubDailyCostModel;
import com.cai.common.domain.ClubIdUpdateLogModel;
import com.cai.common.domain.ClubMatchLogModel;
import com.cai.common.domain.EveryDayRobotModel;
import com.cai.common.domain.GiveCardModel;
import com.cai.common.domain.ItemLogModel;
import com.cai.common.domain.Page;
import com.cai.common.domain.ServerErrorLogModel;
import com.cai.common.domain.SystemLogQueueModel;
import com.cai.common.domain.VoiceChatLogModel;
import com.cai.common.domain.log.ClubApplyLogModel;
import com.cai.common.domain.log.ClubDataLogModel;
import com.cai.common.domain.log.ClubMemberWelfareChangeLogModel;
import com.cai.common.domain.log.ClubScoreMsgLogModel;
import com.cai.common.domain.log.ClubWelfareLotteryMsgLogModel;
import com.cai.common.type.VoiceChatType;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.SpringService;
import com.cai.constant.ClubChatMsg;
import com.cai.constant.ClubMatchWrap;
import com.cai.constant.ClubMatchWrap.ClubMatchStatus;
import com.cai.constant.ServiceOrder;
import com.cai.manager.ClubDataLogManager;
import com.cai.timer.MogoDBTimer;
import com.xianyi.framework.core.service.AbstractService;
import com.xianyi.framework.core.service.IService;

import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import protobuf.clazz.ClubMsgProto.ClubRequest;

/**
 * mogodb服务类
 *
 * @author run
 */
@IService(order = ServiceOrder.MONGODB, desc = "mongo服务器")
public class MongoDBServiceImpl extends AbstractService {

	@SuppressWarnings("unused") private static final Logger logger = LoggerFactory.getLogger(MongoDBServiceImpl.class);
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

	public void useItemLog(long account_id, int itemId, Date use_time) {
		ItemLogModel logModel = new ItemLogModel();
		logModel.setCreate_time(use_time);
		logModel.setAccount_id(account_id);
		logModel.setItem_id(itemId);

		logQueue.add(logModel);
	}

	public List<ItemLogModel> getItemLog(long accountId) {
		try {
			Query query = new Query();
			query.addCriteria(Criteria.where("account_id").is(accountId));
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			return mongoDBService.getMongoTemplate().find(query, ItemLogModel.class);
		} catch (Exception e) {
		}
		return null;
	}

	public List<ClubDailyCostModel> getClubDailyCostModelList(long accountId, Date startDate, Date endDate, long minGold, int receive) {
		try {
			Query query = new Query();
			query.addCriteria(Criteria.where("accountId").is(accountId));
			if (receive >= 0) {
				query.addCriteria(Criteria.where("receive").is(receive));
			}
			query.addCriteria(Criteria.where("create_time").lt(endDate).gte(startDate));
			query.addCriteria(Criteria.where("cost").gte(minGold));
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			return mongoDBService.getMongoTemplate().find(query, ClubDailyCostModel.class);
		} catch (Exception e) {
		}
		return new ArrayList<ClubDailyCostModel>();
	}

	/**
	 * 获取俱乐部消耗日志
	 *
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public List<ClubDailyCostModel> getClubDailyCostModelList(Date startDate, Date endDate) {
		try {
			Query query = new Query();
			query.addCriteria(Criteria.where("create_time").lt(endDate).gte(startDate));
			query.addCriteria(Criteria.where("cost").gt(0L));
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			return mongoDBService.getMongoTemplate().find(query, ClubDailyCostModel.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Collections.emptyList();
	}

	public long getTotalReceiveClubDailyCount(long accountId) {
		long sum = 0;
		try {
			AggregationOperation match = Aggregation.match(Criteria.where("accountId").is(accountId).and("receive").is(1));
			AggregationOperation group = Aggregation.group().sum("sendGold").as("count").count().as("line");
			Aggregation aggregation = Aggregation.newAggregation(match, group);
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			AggregationResults<GiveCardModel> result = mongoDBService.getMongoTemplate()
					.aggregate(aggregation, "club_daily_log", GiveCardModel.class);
			List<GiveCardModel> list = result.getMappedResults();
			if (list != null && list.size() > 0) {
				GiveCardModel giveCardModel = list.get(0);
				sum = giveCardModel.getCount();
			}
		} catch (Exception e) {
		}
		return sum;
	}

	public void updateClubDailyCostModel(ClubDailyCostModel model, MongoTemplate mongoTemplate) {
		Query query = new Query();
		query.addCriteria(Criteria.where("_id").is(model.get_id()));
		Update update = Update.update("receive", 1).set("sendGold", model.getSendGold());
		mongoTemplate.updateFirst(query, update, ClubDailyCostModel.class);
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
		systemLogModel.setLog_type(eLogType.getId());
		systemLogModel.setMsg(msg);
		systemLogModel.setV1(v1);
		systemLogModel.setV2(v2);
		systemLogModel.setLevel(eSysLogLevelType.getId());
		logQueue.add(systemLogModel);
	}

	/**
	 * 服务器报错日志
	 */
	public void server_error_log(int roomId, ELogType eLogType, String msg, Long accountID, String extractMsg, int appId) {
		try {
			ServerErrorLogModel gameLogModel = new ServerErrorLogModel();
			gameLogModel.setCreate_time(new Date());
			gameLogModel.setLog_type(eLogType.getId());
			gameLogModel.setMsg(msg);
			gameLogModel.setRoomId(roomId);
			gameLogModel.setExtractMsg(extractMsg);
			gameLogModel.setAppId(appId);
			logQueue.add(gameLogModel);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void start() throws Exception {
		// TODO Auto-generated method stub

		mogoDBTimer = new MogoDBTimer();
		timer.schedule(mogoDBTimer, 1000, 1000);

	}

	@Override
	public void stop() throws Exception {
		// TODO Auto-generated method stub
		mogoDBTimer.handle();
	}

	public LinkedBlockingQueue<Object> getLogQueue() {
		return logQueue;
	}

	/**
	 * 修改俱乐部id日志，考虑到这个修改量很少，暂放在俱乐部服
	 *
	 * @param accountId
	 * @param oldClubId
	 * @param newClubId
	 * @param accountIds
	 */
	public void updateClubIdLog(long accountId, int oldClubId, int newClubId, String accountIds) {
		ClubIdUpdateLogModel model = new ClubIdUpdateLogModel();
		model.setAccount_id(accountId);
		model.setAccount_ids(accountIds);
		model.setCreate_time(new Date());
		model.setNew_club_id(newClubId);
		model.setOld_club_id(oldClubId);
		logQueue.add(model);
	}

	public List<ClubScoreMsgLogModel> getClubScoreMsgLogModelList() {
		Query query = new Query();
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		return mongoDBService.getMongoTemplate().find(query, ClubScoreMsgLogModel.class);
	}

	public List<ClubMatchLogModel> getClubMatchLogModelList() {
		Query query = new Query();
		query.addCriteria(Criteria.where("status").is(ClubMatchStatus.AFTER.status()));
		//最近15天
		query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(MyDateUtil.getZeroDate(new Date()), -14)));

		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		return mongoDBService.getMongoTemplate().find(query, ClubMatchLogModel.class);
	}

	public void logClubScoreMsg(ClubScoreMsgLogModel model) {
		logQueue.add(model);
	}

	public void statClubApplyLog(ClubApplyLogModel applyModel) {
		logQueue.add(applyModel);
	}

	public List<ClubApplyLogModel> getClubApplyLogModelList() {
		Query query = new Query();
		query.addCriteria(Criteria.where("isHandle").is(false));
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		return mongoDBService.getMongoTemplate().find(query, ClubApplyLogModel.class);
	}

	public void updateClubApplyLogModel(ClubApplyLogModel applyQuitModel) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("clubId").is(applyQuitModel.getClubId()));
		query.addCriteria(Criteria.where("accountId").is(applyQuitModel.getAccountId()));
		query.addCriteria(Criteria.where("isHandle").is(false));

		Update update = new Update();
		update.set("isHandle", applyQuitModel.isHandle());

		mongoDBService.getMongoTemplate().updateFirst(query, update, ClubApplyLogModel.class);

	}

	/**
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

		query.addCriteria(Criteria.where("isRealKouDou").is(true));
		query.addCriteria(new Criteria().orOperator(Criteria.where("clubMatchId").is(null), Criteria.where("clubMatchId").lte(0)));
		query.with(new Sort(Direction.DESC, "create_time"));
		List<BrandLogModel> brandLogModelList = mongoDBService.getMongoTemplate().find(query, BrandLogModel.class);

		return brandLogModelList;
	}

	/**
	 * 查询修改日志
	 *
	 * @param clubId
	 * @param createTime
	 * @param endTime
	 * @return
	 */
	public List<ClubScoreMsgLogModel> getClubScoreMsgLogModelList(int clubId, long createTime, long endTime) {
		Query query = new Query();

		query.addCriteria(Criteria.where("recordTime").gte(createTime).lte(endTime));
		query.addCriteria(Criteria.where("clubId").is(clubId));
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		return mongoDBService.getMongoTemplate().find(query, ClubScoreMsgLogModel.class);
	}

	/**
	 * 语音文件日志
	 *
	 * @param request
	 * @param chatMsg
	 */
	public void logVoiceMsg(ClubRequest request, ClubChatMsg chatMsg) {
		VoiceChatLogModel model = new VoiceChatLogModel();
		model.setCreate_time(new Date());
		model.setType(VoiceChatType.CLUB);
		model.setClubId(request.getClubId());
		model.setUniqueId(chatMsg.getUniqueId());
		model.setAccountId(chatMsg.getAccountId());
		model.setContent(request.getChatMsg().getVoiceMsg().toByteArray());
		logQueue.add(model);
	}

	public synchronized ClubDataLogModel getClubDataLog() {
		Date logCreateDate = MyDateUtil.getZeroDate(new Date());
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("create_time").is(logCreateDate));
		ClubDataLogModel logModel = mongoDBService.getMongoTemplate().findOne(query, ClubDataLogModel.class);
		if (logModel == null) {
			ClubDataLogModel model = new ClubDataLogModel();
			model.setCreate_time(logCreateDate);
			mongoDBService.getMongoTemplate().insert(model);

			logModel = model;
			ClubDataLogManager.resetDailyData();
		}

		return logModel;
	}

	public void updateClubDataLog(ClubDataLogModel logModel) {
		Date logCreateDate = MyDateUtil.getZeroDate(new Date());
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("create_time").is(logCreateDate));

		Update update = new Update();
		update.set("totalClubCount", logModel.getTotalClubCount());
		update.set("distinct_user", logModel.getDistinct_user());
		update.set("setRuleCountData", logModel.getSetRuleCountData());
		update.set("successGameCount", logModel.getSuccessGameCount());
		update.set("activePlayerCount", logModel.getActivePlayerCount());
		update.set("clubGameCountData", logModel.getClubGameCountData());
		update.set("newJoinCount", logModel.getNewJoinCount());
		update.set("clubSectionData", logModel.getClubSectionData());
		update.set("completeParentBrandCount", logModel.getCompleteParentBrandCount());
		update.set("childBrandCount", logModel.getChildBrandCount());
		update.set("totalCostGold", logModel.getTotalCostGold());
		update.set("gameInfoData", logModel.getGameInfoData());
		update.set("registAndPlayNum", logModel.getRegistAndPlayNum());
		update.set("activeTableCount", logModel.getClubActiveTableCount());
		update.set("newClubCount", logModel.getNewClubCount());
		update.set("activeClubNum", logModel.getActiveClubNum());

		mongoDBService.getMongoTemplate().updateFirst(query, update, ClubDataLogModel.class);
	}

	public BrandLogModel getClubParentBrand(long brandId) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);

		Query query = new Query();
		query.addCriteria(Criteria.where("brand_id").is(brandId));

		List<BrandLogModel> brandLogModelList = mongoDBService.getMongoTemplate().find(query, BrandLogModel.class);
		if (brandLogModelList != null && brandLogModelList.size() > 0) {
			return brandLogModelList.get(0);
		}

		return null;
	}

	public void logClubCreateMatch(ClubMatchWrap wrap) {
		ClubCreateMatchLogModel model = new ClubCreateMatchLogModel();
		model.setCreate_time(new Date());
		model.setCreatorId(wrap.getModel().getCreatorId());
		model.setId(wrap.id());
		model.setClubId(wrap.getModel().getClubId());
		model.setMatchName(wrap.getModel().getMatchName());
		model.setGameName(wrap.getRuleBuilder().getGameName());
		model.setMatchType(wrap.getModel().getMatchType());
		model.setReward(wrap.getModel().getReward());
		model.setStartDate(wrap.getModel().getStartDate());
		logQueue.add(model);
	}

	public List<ClubWelfareLotteryMsgLogModel> getClubWelfareLotteryMsgLogList() {
		Query query = new Query();
		//最近15天
		query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(MyDateUtil.getZeroDate(new Date()), -14)));

		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		return mongoDBService.getMongoTemplate().find(query, ClubWelfareLotteryMsgLogModel.class);
	}

	public List<ClubMemberWelfareChangeLogModel> getClubMemberWelfareChangeLog(int clubId, long accountId) {
		Query query = new Query();
		//最近15天
		query.addCriteria(Criteria.where("clubId").is(clubId));
		query.addCriteria(Criteria.where("accountId").is(accountId));
		query.addCriteria(Criteria.where("create_time").gte(DateUtils.addDays(MyDateUtil.getZeroDate(new Date()), -14)));
		query.with(new Sort(Direction.DESC, "create_time"));
		query.limit(100);

		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		return mongoDBService.getMongoTemplate().find(query, ClubMemberWelfareChangeLogModel.class);
	}
}
