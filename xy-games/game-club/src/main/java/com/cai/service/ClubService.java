package com.cai.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.IntBinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;
import com.cai.common.ClubRecordDataType;
import com.cai.common.ClubTableKickOutType;
import com.cai.common.ClubTireLogType;
import com.cai.common.constant.AttributeKeyConstans;
import com.cai.common.constant.GameConstants;
import com.cai.common.constant.RMICmd;
import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.constant.Symbol;
import com.cai.common.define.EClubEventType;
import com.cai.common.define.EClubSettingStatus;
import com.cai.common.define.EPhoneIdentifyCodeType;
import com.cai.common.define.EPlayerStatus;
import com.cai.common.define.ERedHeartCategory;
import com.cai.common.define.ERoomSettingStatus;
import com.cai.common.define.ERoomStatus;
import com.cai.common.define.EServerType;
import com.cai.common.define.EWealthCategory;
import com.cai.common.define.IPhoneOperateType;
import com.cai.common.define.XYCode;
import com.cai.common.domain.ClubAccountModel;
import com.cai.common.domain.ClubActivityModel;
import com.cai.common.domain.ClubBanPlayerModel;
import com.cai.common.domain.ClubBulletinModel;
import com.cai.common.domain.ClubDailyCostModel;
import com.cai.common.domain.ClubDataModel;
import com.cai.common.domain.ClubEventLogModel;
import com.cai.common.domain.ClubGroupModel;
import com.cai.common.domain.ClubMatchLogModel;
import com.cai.common.domain.ClubMatchModel;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.domain.ClubMemberRecordModel;
import com.cai.common.domain.ClubModel;
import com.cai.common.domain.ClubRoomModel;
import com.cai.common.domain.ClubRoomRedisModel;
import com.cai.common.domain.ClubRuleModel;
import com.cai.common.domain.ClubRuleRecordModel;
import com.cai.common.domain.PlayerViewVO;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.domain.StatusModule;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.json.ClubRoomJsonModel;
import com.cai.common.domain.json.ClubRoomJsonModel.RoomJsonModel;
import com.cai.common.domain.log.ClubApplyLogModel;
import com.cai.common.domain.log.ClubScoreMsgLogModel;
import com.cai.common.domain.log.ClubWelfareLotteryMsgLogModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.rmi.vo.UpdateClubIdVo;
import com.cai.common.rmi.vo.UserPhoneRMIVo;
import com.cai.common.type.ClubApplyType;
import com.cai.common.type.ClubExitType;
import com.cai.common.util.EmojiFilter;
import com.cai.common.util.FilterUtil;
import com.cai.common.util.HttpClientUtils;
import com.cai.common.util.NamedThreadFactory;
import com.cai.common.util.PBUtil;
import com.cai.common.util.Pair;
import com.cai.common.util.RedisKeyUtil;
import com.cai.common.util.SessionUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.StringUtil;
import com.cai.common.util.TimeUtil;
import com.cai.config.ClubCfg;
import com.cai.config.SystemConfig;
import com.cai.constant.Club;
import com.cai.constant.ClubActivityWrap;
import com.cai.constant.ClubBulletinWrap;
import com.cai.constant.ClubChatMsg;
import com.cai.constant.ClubEventCode;
import com.cai.constant.ClubJoinCode;
import com.cai.constant.ClubMatchFactory;
import com.cai.constant.ClubMatchLogWrap;
import com.cai.constant.ClubMatchWrap;
import com.cai.constant.ClubMatchWrap.ClubMatchStatus;
import com.cai.constant.ClubPlayer;
import com.cai.constant.ClubRuleTable;
import com.cai.constant.ClubScoreMsgWrap;
import com.cai.constant.ClubSeat;
import com.cai.constant.ClubTable;
import com.cai.constant.ClubWelfareLotteryUtil;
import com.cai.constant.EClubIdentity;
import com.cai.constant.EClubOperateCategory;
import com.cai.constant.ERuleSettingStatus;
import com.cai.constant.ServiceOrder;
import com.cai.dictionary.DirtyWordDict;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.manager.ClubDataLogManager;
import com.cai.redis.service.RedisService;
import com.cai.tasks.db.ClubMemberRecordDelDBTask;
import com.cai.tasks.db.ClubMemberUpdateIdentityDBTask;
import com.cai.tasks.db.ClubRecordDBTask;
import com.cai.tasks.db.DelClubDBTask;
import com.cai.tasks.db.DelMemberDBTask;
import com.cai.tasks.db.OfflineRuleDBTask;
import com.cai.timer.DataStatTimer;
import com.cai.utils.ClubEventLog;
import com.cai.utils.ClubRoomUtil;
import com.cai.utils.LogicMsgSender;
import com.cai.utils.Utils;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;
import com.xianyi.framework.core.service.AbstractService;
import com.xianyi.framework.core.service.IService;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import protobuf.clazz.ClubMsgProto;
import protobuf.clazz.ClubMsgProto.ClubApplyJoinProto;
import protobuf.clazz.ClubMsgProto.ClubBanSwitchResponse;
import protobuf.clazz.ClubMsgProto.ClubChatHistory;
import protobuf.clazz.ClubMsgProto.ClubChatRsp;
import protobuf.clazz.ClubMsgProto.ClubCommonLIIProto;
import protobuf.clazz.ClubMsgProto.ClubJoinQuitMsgProto;
import protobuf.clazz.ClubMsgProto.ClubJoinQuitMsgProto.MsgType;
import protobuf.clazz.ClubMsgProto.ClubMemberListProto;
import protobuf.clazz.ClubMsgProto.ClubMemberRemarkProto;
import protobuf.clazz.ClubMsgProto.ClubNoticeProto;
import protobuf.clazz.ClubMsgProto.ClubOnlineMemeberRsp;
import protobuf.clazz.ClubMsgProto.ClubProto;
import protobuf.clazz.ClubMsgProto.ClubRedHeartRsp;
import protobuf.clazz.ClubMsgProto.ClubRequest;
import protobuf.clazz.ClubMsgProto.ClubRuleProto;
import protobuf.clazz.ClubMsgProto.ClubRuleRemarkProto;
import protobuf.clazz.ClubMsgProto.ClubRuleTableGroupProto;
import protobuf.clazz.ClubMsgProto.ClubSimple;
import protobuf.clazz.ClubMsgProto.ClubTableNeedPassportResponse;
import protobuf.clazz.ClubMsgProto.ClubTableStatusUpdateRsp;
import protobuf.clazz.ClubMsgProto.ClubUpdateProto;
import protobuf.clazz.Common.ChatMsgRsp;
import protobuf.clazz.Common.CommonII;
import protobuf.clazz.Common.CommonLI;
import protobuf.clazz.Common.CommonLII;
import protobuf.clazz.s2s.ClubServerProto.ClubGameRecordProto;
import protobuf.clazz.s2s.ClubServerProto.ClubGameRecordProto.ClubGameOverProto;
import protobuf.clazz.s2s.ClubServerProto.ClubGameRecordProto.ClubGameOverProto.GamePlayerProto;
import protobuf.clazz.s2s.ClubServerProto.ClubRoomStatusProto;
import protobuf.clazz.s2s.ClubServerProto.ProxyClubRq;

import static java.util.stream.Collectors.groupingBy;

@IService(order = ServiceOrder.CLUB, desc = "亲友圈")
public final class ClubService extends AbstractService {

	private final ExecutorService services = Executors.newFixedThreadPool(3, new NamedThreadFactory("club-group-thread"));

	private final ScheduledExecutorService schExecutorService_update = Executors
			.newScheduledThreadPool(1, new NamedThreadFactory("club-schedule-update-thread"));

	private final ScheduledExecutorService schExecutorService_save = Executors
			.newScheduledThreadPool(1, new NamedThreadFactory("club-schedule-save-thread"));

	// 财富同步
	// private ScheduledExecutorService schWealthService =
	// Executors.newScheduledThreadPool(1, new
	// NamedThreadFactory("club-wealth-sync-thread"));

	private static final ClubService instance = new ClubService();

	public static final int DEFAULT_LIMIT_ROUND = 3;

	public final Map<Integer, Club> clubs;
	public final Map<String, Integer> groupClubMaps;

	// 公共亲友圈公告
	private final Map<Long, ClubBulletinWrap> sharedBulletins;

	// 表示在亲友圈列表界面
	public static final ClubSeat currentSeat = ClubSeat.newSeat();

	private Timer timer;

	private ClubService() {
		timer = new Timer("Timer-PublicServiceImpl");
		groupClubMaps = new ConcurrentSkipListMap<>();
		clubs = Maps.newConcurrentMap();
		sharedBulletins = Maps.newConcurrentMap();
	}

	public static ClubService getInstance() {
		return instance;
	}

	@Override
	public void start() throws Exception {
		timer.schedule(new DataStatTimer(), 60000L, 60000L);// 数据统计

		long cur = System.currentTimeMillis();

		List<ClubModel> temps = SpringService.getBean(ClubDaoService.class).getDao().getClubList();
		List<ClubDataModel> clubDataList = SpringService.getBean(ClubDaoService.class).getDao().getClubDataList();
		List<ClubGroupModel> clubGroupList = SpringService.getBean(ClubDaoService.class).getDao().getClubGroupList();
		List<ClubActivityModel> activityModels = SpringService.getBean(ClubDaoService.class).getDao().getClubActivityModelList();
		List<ClubBulletinModel> clubBulletinModes = SpringService.getBean(ClubDaoService.class).getDao().getClubBulletinModelList();
		List<ClubRuleRecordModel> clubRuleRecordModelList = SpringService.getBean(ClubDaoService.class).getDao().getAllClubRuleRecord();

		//亲友圈福卡抽奖数据
		ClubWelfareLotteryUtil.init();

		Map<Integer, List<ClubBulletinModel>> clubBulletinModesMaps = clubBulletinModes.stream().collect(groupingBy(ClubBulletinModel::getClubId));

		Map<Integer, List<ClubDataModel>> clubDataMap = clubDataList.stream().collect(groupingBy(ClubDataModel::getClubId));

		Map<Integer, Set<String>> clubGroupMaps = new ConcurrentSkipListMap<>();

		Map<Integer, List<ClubRuleRecordModel>> clubRuleRecordMap = clubRuleRecordModelList.stream()
				.collect(groupingBy(ClubRuleRecordModel::getClubId));

		for (ClubGroupModel model : clubGroupList) {
			groupClubMaps.put(model.getGroup_id(), model.getClub_id());
			Set<String> set = clubGroupMaps.get(model.getClub_id());
			if (set == null) {
				set = new HashSet<String>();
				set.add(model.getGroup_id());
				clubGroupMaps.put(model.getClub_id(), set);
			} else {
				set.add(model.getGroup_id());
			}
		}

		// 亲友圈成员
		List<ClubMemberModel> allmembers = SpringService.getBean(ClubDaoService.class).getDao().getAllClubMembers();
		Map<Integer, List<ClubMemberModel>> allMemberMaps = allmembers.stream().collect(groupingBy(ClubMemberModel::getClub_id));

		// 亲友圈成员记录
		Map<Integer, Map<Long, List<ClubMemberRecordModel>>> allMemberRecordMap = initAllMemberRecord();

		// 亲友圈禁止同桌玩家
		Map<Integer, Map<Long, String>> allMemberBanPlayerMap = initAllMemberBanPlayer();

		// 亲友圈玩法/包间数据
		List<ClubRuleModel> allClubRules = SpringService.getBean(ClubDaoService.class).getDao().getAllClubRule();
		Map<Integer, List<ClubRuleModel>> allRuleMaps = allClubRules.stream().collect(groupingBy(ClubRuleModel::getClub_id));

		// 亲友圈自建赛
		List<ClubMatchModel> expireClubMatchs = new ArrayList<>();
		Date now = new Date();
		List<ClubMatchModel> allClubMatchs = SpringService.getBean(ClubDaoService.class).getDao().getAllClubMatchs();
		for (ClubMatchModel matchModel : allClubMatchs) {
			if (matchModel.getStatus() != ClubMatchStatus.PRE.status() && matchModel.getStatus() != ClubMatchStatus.ING.status()) {
				if (matchModel.getStatus() != ClubMatchStatus.FAILED.status()) {
					expireClubMatchs.add(matchModel);
				} else {
					if (!DateUtils.isSameDay(now, matchModel.getStartDate())) {
						expireClubMatchs.add(matchModel);
					}
				}
			}
		}
		allClubMatchs.removeAll(expireClubMatchs);
		Map<Integer, List<ClubMatchModel>> allClubMatchMaps = allClubMatchs.stream().collect(groupingBy(ClubMatchModel::getClubId));

		// 亲友圈每日消耗数据
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, -10);
		List<ClubDailyCostModel> clubsDailyCostModel = MongoDBServiceImpl.getInstance().getClubDailyCostModelList(calendar.getTime(), new Date());
		Map<Integer, List<ClubDailyCostModel>> dailyCostMap = clubsDailyCostModel.stream().collect(groupingBy(ClubDailyCostModel::getClubId));

		// 申请记录
		List<ClubApplyLogModel> clubApplyModes = MongoDBServiceImpl.getInstance().getClubApplyLogModelList();
		Map<Integer, List<ClubApplyLogModel>> clubApplyLogMap = clubApplyModes.stream().collect(groupingBy(ClubApplyLogModel::getClubId));

		// 亲友圈修改疲劳值记录数据
		List<ClubScoreMsgLogModel> clubScoreMsgLogModelList = MongoDBServiceImpl.getInstance().getClubScoreMsgLogModelList();
		Map<Integer, List<ClubScoreMsgLogModel>> clubScoreMsgLogMap = clubScoreMsgLogModelList.stream()
				.collect(groupingBy(ClubScoreMsgLogModel::getClubId));

		// 亲友圈自建赛记录
		List<ClubMatchLogModel> clubMatchLogModelList = MongoDBServiceImpl.getInstance().getClubMatchLogModelList();
		Map<Integer, List<ClubMatchLogModel>> clubMatchLogMap = clubMatchLogModelList.stream().collect(groupingBy(ClubMatchLogModel::getClubId));

		// 亲友圈福卡抽奖记录
		List<ClubWelfareLotteryMsgLogModel> clubWelfareLotteryMsgLogList = MongoDBServiceImpl.getInstance().getClubWelfareLotteryMsgLogList();
		Map<Integer, List<ClubWelfareLotteryMsgLogModel>> clubWelfareLotteryLogMap = clubWelfareLotteryMsgLogList.stream()
				.collect(groupingBy(ClubWelfareLotteryMsgLogModel::getClubId));

		for (ClubModel temp : temps) {

			// 1玩法/包间
			List<ClubRuleModel> clubRules = allRuleMaps
					.get(temp.getClub_id());// SpringService.getBean(ClubDaoService.class).getDao().getClubRule(temp.getClub_id());
			if (null != clubRules) {
				Map<Integer, ClubRuleModel> ruleMap = clubRules.stream().collect(Collectors.toMap(ClubRuleModel::getId, c -> c));
				temp.setRules(ruleMap);

				clubRules.forEach((ruleModel) -> {
					try {
						String gameName = SysGameTypeDict.getInstance().getMJname(ruleModel.getGame_type_index());
						ruleModel.setGame_name(Strings.isNullOrEmpty(gameName) ? "未知" : gameName);
					} catch (Exception e) {
						logger.error("亲友圈[{}] 子游戏:{},玩法名称找不到", temp.getClub_id(), ruleModel.getGame_type_index(), e);
					}

					try {
						int gameId = SysGameTypeDict.getInstance().getGameIDByTypeIndex(ruleModel.getGame_type_index());
						ruleModel.setGame_id(gameId);
						if (-1 == gameId) {
							logger.error("亲友圈[{}] 子游戏:{},玩法gameId找不到", temp.getClub_id(), ruleModel.getGame_type_index());
						}
					} catch (Exception e) {
						logger.error("亲友圈[{}] 子游戏:{},玩法gameId找不到", temp.getClub_id(), ruleModel.getGame_type_index(), e);
					}
				});
			} else {
				temp.setRules(Maps.newHashMap());
			}

			// 2成员列表
			List<ClubMemberModel> members = allMemberMaps
					.get(temp.getClub_id());// SpringService.getBean(ClubDaoService.class).getDao().getClubMembers(temp.getClub_id());
			if (null == members) {
				logger.error("亲友圈:{} 成员数据为空！", temp.getClub_id());
				continue;
			}
			members.forEach((m) -> {
				if (m.getAccount_id() == temp.getAccount_id()) {
					m.setIdentity(EClubIdentity.CREATOR.identify());
				}

				// 优化---玩家-亲友圈id影射
				ClubCacheService.getInstance().addMemberClubId(m.getAccount_id(), temp.getClub_id());

				// 玩家记录
				if (allMemberRecordMap.get(temp.getClub_id()) != null) {
					if (allMemberRecordMap.get(temp.getClub_id()).get(m.getAccount_id()) != null) {
						m.initMemberRecord(allMemberRecordMap.get(temp.getClub_id()).get(m.getAccount_id()));
					}
				}
				// 禁止同桌玩家
				if (allMemberBanPlayerMap.get(temp.getClub_id()) != null) {
					if (allMemberBanPlayerMap.get(temp.getClub_id()).get(m.getAccount_id()) != null) {
						m.initMemberBanPlayers(allMemberBanPlayerMap.get(temp.getClub_id()).get(m.getAccount_id()));
					}
				}
			});
			ClubScoreMsgWrap clubScoreMsgWrap = new ClubScoreMsgWrap(temp.getClub_id());
			clubScoreMsgWrap.initData(clubScoreMsgLogMap.get(temp.getClub_id()));

			ClubMatchLogWrap clubMatchLogWrap = new ClubMatchLogWrap(temp.getClub_id());
			clubMatchLogWrap.initData(clubMatchLogMap.get(temp.getClub_id()));

			// 初始化
			ClubDataModel clubDataModel = null;
			List<ClubDataModel> dataList = clubDataMap.get(temp.getClub_id());
			if (dataList != null && dataList.size() > 0) {
				clubDataModel = dataList.get(0);
			}
			Club club = new Club(temp, members, clubScoreMsgWrap, clubDataModel);
			if (clubGroupMaps.containsKey(club.getClubId())) {
				club.groupSet.addAll(clubGroupMaps.get(club.getClubId()));
			}
			club.setClubMatchLogWrap(clubMatchLogWrap);
			club.initRuleRecord(clubRuleRecordMap.get(club.getClubId()));
			club.clubWelfareWrap.initLotteryLogMsg(clubWelfareLotteryLogMap.get(club.getClubId()));

			clubs.put(temp.getClub_id(), club);

			List<ClubApplyLogModel> applyList = clubApplyLogMap.get(temp.getClub_id());
			if (applyList != null) {
				for (ClubApplyLogModel model : applyList) {
					if (model.getType() == ClubApplyType.QUIT) {
						club.requestQuitMembers.put(model.getAccountId(), model);
					}
				}
			}

			// 3亲友圈公告
			List<ClubBulletinModel> clubBulletins = clubBulletinModesMaps.get(temp.getClub_id());
			if (null != clubBulletins) {
				clubBulletins.forEach((bullet) -> {
					club.bulletins.put(bullet.getId(), new ClubBulletinWrap(bullet));
				});
			}

			// 4亲友圈共享公告
			List<ClubBulletinModel> sharedBulletins_ = clubBulletinModesMaps.get(0);
			if (null != sharedBulletins_ && !sharedBulletins.isEmpty()) {
				sharedBulletins_.forEach((bullet) -> {
					sharedBulletins.put(bullet.getId(), new ClubBulletinWrap(bullet));
				});
			}

			List<ClubDailyCostModel> clubDailyCostModels = dailyCostMap.get(club.getClubId());
			if (null != clubDailyCostModels) {
				club.dailyCostModels.addAll(clubDailyCostModels);
			}

			// 亲友圈自建赛
			List<ClubMatchModel> clubMatchs = allClubMatchMaps.get(club.getClubId());
			if (clubMatchs != null) {
				clubMatchs.forEach((clubMatchModel) -> {
					ClubMatchWrap wrap = ClubMatchFactory.createClubMatchWrap(clubMatchModel.getOpenType(), clubMatchModel, club);
					club.matchs.put(clubMatchModel.getId(), wrap);
				});
			}
		}

		activityModels.forEach((activityModel) -> {
			Club club = clubs.get(activityModel.getClubId());
			if (null != club) {
				club.activitys.put(activityModel.getId(), new ClubActivityWrap(activityModel));
			} else {
				logger.warn("亲友圈活动[clubId:{},id:{}]，亲友圈可能已经删除了！ ", activityModel.getClubId(), activityModel.getId());
			}
		});

		logger.info("------ 亲友圈数据初始化完成，总耗时:{} ms", System.currentTimeMillis() - cur);

		schExecutorService_update.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				clubs.forEach((club_id, club) -> {
					long time = System.currentTimeMillis();
					club.update(time);
				});
			}
		}, 1, 1, TimeUnit.SECONDS);

		schExecutorService_save.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				clubs.forEach((club_id, club) -> {
					long time = System.currentTimeMillis();
					club.updateFiveMin(time);
				});
				ClubService.this.save();
			}
		}, 5, 5, TimeUnit.MINUTES);
	}

	private Map<Integer, Map<Long, String>> initAllMemberBanPlayer() {
		Map<Integer, Map<Long, String>> totalMap = new HashMap<>();
		List<ClubBanPlayerModel> list = SpringService.getBean(ClubDaoService.class).getDao().getAllClubMemberBanPlayer();
		for (ClubBanPlayerModel model : list) {
			Map<Long, String> tmpMap = totalMap.computeIfAbsent(model.getClubId(), k -> new HashMap<>());
			tmpMap.put(model.getAccountId(), model.getBanPlayers());
		}
		return totalMap;
	}

	private Map<Integer, Map<Long, List<ClubMemberRecordModel>>> initAllMemberRecord() {
		Map<Integer, Map<Long, List<ClubMemberRecordModel>>> map = new HashMap<>();
		List<ClubMemberRecordModel> allmemberRecord = SpringService.getBean(ClubDaoService.class).getDao().getAllClubMemberRecord();

		for (ClubMemberRecordModel model : allmemberRecord) {
			int clubId = model.getClubId();
			long accountId = model.getAccountId();
			if (!map.containsKey(clubId)) {
				Map<Long, List<ClubMemberRecordModel>> temp = new HashMap<>();
				map.put(model.getClubId(), temp);
			}
			Map<Long, List<ClubMemberRecordModel>> map1 = map.get(clubId);
			if (!map1.containsKey(accountId)) {
				List<ClubMemberRecordModel> temp = new ArrayList<>();
				map1.put(accountId, temp);
			}
			List<ClubMemberRecordModel> list = map1.get(accountId);
			list.add(model);
		}
		return map;
	}

	private void save() {
		try {
			List<ClubModel> clubModels = Lists.newArrayList();
			List<ClubDataModel> clubDataModels = Lists.newArrayList();

			clubs.forEach((club_id, club) -> {
				try {
					club.save();
				} catch (Exception e) {
					e.printStackTrace();
				}
				club.encodeSave();
				if (club.clubModel.isNeedDB()) {
					clubModels.add(club.clubModel);
					club.clubModel.setNeedDB(false);
				}
				ClubDataModel clubDataModel = club.getClubDataModel();
				if (clubDataModel != null && clubDataModel.isNeedDB()) {
					clubDataModels.add(clubDataModel);
					clubDataModel.setNeedDB(false);
				}
			});

			SpringService.getBean(ClubDaoService.class).batchUpdate("updateClub", clubModels);
			SpringService.getBean(ClubDaoService.class).batchUpdate("updateClubDataModel", clubDataModels);

			// 亲友圈系统公告
			List<ClubBulletinModel> bulletins_ = Lists.newArrayList();
			sharedBulletins.forEach((id, model) -> {
				if (model.getBulletinModel().isNeedDB()) {
					bulletins_.add(model.getBulletinModel());
					model.getBulletinModel().setNeedDB(false);
				}
			});
			if (!bulletins_.isEmpty()) {
				SpringService.getBean(ClubDaoService.class).batchUpdate("updateClubBulletin", bulletins_);
			}

			if (ClubCfg.get().isUseOldRecordSaveWay()) {
				// 亲友圈成员记录
				List<ClubMemberRecordModel> recordModelList = new ArrayList<>();
				clubs.forEach((club_id, club) -> {
					club.members.forEach((account_id, memberModel) -> {
						memberModel.getMemberRecordMap().forEach((day, recordModel) -> {
							if (recordModel.isNeedDB()) {
								recordModel.setNeedDB(false);
								recordModelList.add(recordModel);
							}
						});
					});
				});
				if (!recordModelList.isEmpty()) {
					SpringService.getBean(ClubDaoService.class).batchUpdate("updateClubMemberRecord", recordModelList);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("保存亲友圈数据报错", e);
		}
	}

	public ClubProto getClubEncodeDetail(int clubId, long accountId) {
		Club club = clubs.get(clubId);
		if (club == null) {
			return null;
		}
		return club.encode(accountId, false).build();
	}

	/**
	 * 解散亲友圈
	 */
	public ClubRoomModel deleteClub(int clubId, long account_ID, String field, boolean isFromSSHE) {
		Club club = clubs.get(clubId);
		if (club == null) {
			return new ClubRoomModel(Club.FAIL).setDesc("亲友圈不存在!");
		}

		// 手机验证
		if (!isFromSSHE && club.getMemberCount() >= 10) {
			if (Strings.isNullOrEmpty(field)) {
				return new ClubRoomModel(Club.FAIL).setDesc("手机验证信息不符合规定！");
			}

			String[] arr = field.split(Symbol.COLON);
			if (arr.length < 2) {
				return new ClubRoomModel(Club.FAIL).setDesc("手机验证信息不符合规定！！");
			}

			UserPhoneRMIVo vo = UserPhoneRMIVo.newVo(IPhoneOperateType.BIND_INFO, 0L, arr[0]);
			Pair<Integer, String> r = SpringService.getBean(ICenterRMIServer.class).rmiInvoke(RMICmd.ACCOUNT_PHONE, vo);

			if (r.getFirst().intValue() == 0 || Longs.tryParse(r.getSecond()) != account_ID) {
				return new ClubRoomModel(Club.FAIL).setDesc("手机没邦定！");
			}

			if (!Utils.identifyCodeVaild(arr[0], arr[1], EPhoneIdentifyCodeType.PHONE_CLUB_DISBAND)) {
				return new ClubRoomModel(Club.FAIL).setDesc("验证码有误，请重新输入！");
			}
		}

		if (club.getOwnerId() != account_ID) {
			return new ClubRoomModel(Club.FAIL).setDesc("只有创建者可以进行删除亲友圈操作！");
		}

		Club club_ = clubs.get(clubId);
		club_.runInDBLoop(new DelClubDBTask(club_));
		return new ClubRoomModel(Club.SUCCESS).setDesc("删除亲友圈成功!");
	}

	/**
	 *
	 */
	public ClubRoomModel requestClub(int clubId, long accountId, String avatar, String content, String nickname) {
		return requestClub(clubId, accountId, avatar, content, nickname, 0);
	}

	/**
	 *
	 */
	public ClubRoomModel requestClub(int clubId, long accountId, String avatar, String content, String nickname, long partnerId) {
		Club club = clubs.get(clubId);
		if (club == null) {
			return new ClubRoomModel(Club.FAIL).setDesc("找不到该亲友圈!");
		}

		if (club.members.containsKey(accountId)) {
			return new ClubRoomModel(Club.FAIL).setDesc("你已经是该亲友圈成员!");
		}

		// 亲友圈上限
		if (getMyEnterClubCount(accountId) >= ClubCfg.get().getOwnerClubMax()) {
			return new ClubRoomModel(Club.FAIL).setDesc("你的亲友圈数量已达上限!");
		}

		// 亲友圈人数上限
		if (club.getMemberCount() > ClubCfg.get().getClubMemberMax()) {
			return new ClubRoomModel(Club.FAIL).setDesc("你所申请的亲友圈人数数量已达上限!");
		}
		return club.requestJoinClub(accountId, avatar, content, nickname, partnerId);
	}

	public ClubRoomModel agreeJoinClub(long ownerId, int club_id, long targetId) {

		Club club = clubs.get(club_id);

		if (club == null) {
			return new ClubRoomModel(Club.FAIL).setDesc("亲友圈不存在!");
		}

		ClubMemberModel operator = club.members.get(ownerId);

		if (null == operator || !EClubIdentity.isManager(operator.getIdentity())) {
			return new ClubRoomModel(Club.FAIL).setDesc("权限不够!");
		}

		if (getMyEnterClubCount(targetId) >= ClubCfg.get().getOwnerClubMax()) {
			return new ClubRoomModel(Club.FAIL).setDesc("加入亲友圈数量已达上限!");
		}

		// 亲友圈人数上限
		if (club.getMemberCount() >= ClubCfg.get().getClubMemberMax()) {
			return new ClubRoomModel(Club.FAIL).setDesc("亲友圈人数已达上限!");
		}
		if (club.members.containsKey(targetId)) {
			club.requestMembers.remove(targetId);
			return new ClubRoomModel(Club.FAIL).setDesc("已经是亲友圈成员!");
		}
		ClubApplyJoinProto temp = club.requestMembers.remove(targetId);
		if (temp == null) {
			return new ClubRoomModel(Club.FAIL).setDesc("没有请求加入记录!");
		}

		ClubMemberModel member = new ClubMemberModel();
		member.setAccount_id(temp.getAccountId());
		member.setAvatar(temp.getAvatar());
		member.setNickname(temp.getNickname());
		member.setClubName(club.clubModel.getClub_name());
		member.setDate(new Date());
		member.setClub_id(club_id);

		if (club.members.putIfAbsent(targetId, member) != null) {
			return new ClubRoomModel(Club.FAIL).setDesc("其它管理员已经同意!");
		}

		//如果是合伙人邀请进来的绑定合伙人关系
		long partnerId = temp.getPartnerId();
		if (partnerId > 0) {
			ClubMemberModel partner = club.members.get(partnerId);
			if (partner != null && partner.isPartner()) {
				member.setParentId(partnerId);
				club.runInDBLoop(() -> {
					SpringService.getBean(ClubDaoService.class).getDao().updateClubAccountPartner(member);
				});
			}
		}

		// 事件
		ClubJoinQuitMsgProto.Builder eventMsg = Utils.newEventMsg(club_id, MsgType.JOIN, operator, member);

		club.joinQuitMsgQueueProto.offer(eventMsg.build());
		Utils.sendClubEventMsg(club.getManagerIds(), club_id, eventMsg.build());

		ClubCacheService.getInstance().addMemberClubId(targetId, club_id);// 返回推送
		Utils.notifyJoinResult(targetId, ownerId, club, ClubJoinCode.AGREE);

		club.agreeRequestMembers.offer(temp.toBuilder().setJoinTime(member.getDate().getTime()).setIsAgree(true).build());
		ClubAccountModel requestClubAccount = new ClubAccountModel();

		synchronized (club) {
			requestClubAccount.setAccount_id(targetId);
			requestClubAccount.setClub_id(club.clubModel.getClub_id());

			try {
				SpringService.getBean(ClubDaoService.class).getDao().insertClubAccount(requestClubAccount);
			} catch (Exception e) {
				logger.error("agreeJoinClub error", e);
				club.members.remove(targetId);
				return new ClubRoomModel(Club.FAIL).setDesc("系统异常，请联系客服!");
			}

		}

		ClubEventLog.event(new ClubEventLogModel(club_id, ownerId, EClubEventType.JOIN).setTargetId(targetId));
		ClubDataLogManager.addNewJoinPlayer(targetId);

		Utils.sendJoinMemberInfo(club.getManagerIds(), club_id, member);

		//清除之前玩家退出亲友圈时没有正常移除的玩家记录数据
		club.runInDBLoop(new ClubMemberRecordDelDBTask(club_id, member.getAccount_id()));

		// 检查是否可开启亲友圈福卡功能
		club.clubWelfareWrap.checkOpenClubWelfare();

		return new ClubRoomModel(Club.SUCCESS).setDesc(String.format("[%s]已经加入亲友圈!", temp.getNickname()));
	}

	public ClubRoomModel agreeJoinClubBatch(long clientSessionId, int clubId) {

		List<Club> clubs = new ArrayList<>();
		if (clubId > 0) {
			Club club = this.clubs.get(clubId);

			if (club == null) {
				return new ClubRoomModel(Club.FAIL).setDesc("亲友圈不存在!");
			}

			clubs.add(club);
		} else {
			this.clubs.forEach((id, club) -> {
				if (club.clubModel.getAccount_id() == clientSessionId) {
					clubs.add(club);
				}
			});
		}

		List<ClubAccountModel> accounts = new ArrayList<>();
		for (final Club club : clubs) {
			for (ClubApplyJoinProto apply : club.requestMembers.values()) {

				if (club.requestMembers.remove(apply.getAccountId()) == null) {
					continue;
				}
				// 如果申请人自己的亲友圈已达上限
				if (getMyEnterClubCount(apply.getAccountId()) > ClubCfg.get().getOwnerClubMax()) {
					continue;
				}

				// 当前亲友圈人数已达上限
				if (club.getMemberCount() > ClubCfg.get().getClubMemberMax()) {
					continue;
				}
				ClubMemberModel member = new ClubMemberModel();
				member.setAccount_id(apply.getAccountId());
				member.setAvatar(apply.getAvatar());
				member.setNickname(apply.getNickname());
				member.setClubName(club.clubModel.getClub_name());
				member.setDate(new Date());
				member.setClub_id(club.clubModel.getClub_id());
				if (club.members.putIfAbsent(apply.getAccountId(), member) == null) {
					club.agreeRequestMembers.offer(apply.toBuilder().setJoinTime(member.getDate().getTime()).setIsAgree(true).build());

					ClubAccountModel requestClubAccount = new ClubAccountModel();
					requestClubAccount.setAccount_id(apply.getAccountId());
					requestClubAccount.setClub_id(club.clubModel.getClub_id());
					accounts.add(requestClubAccount);

					// 事件
					ClubJoinQuitMsgProto.Builder eventMsg = Utils.newEventMsg(clubId, MsgType.JOIN, club.members.get(clientSessionId), member);
					club.joinQuitMsgQueueProto.offer(eventMsg.build());
					Utils.sendClubEventMsg(club.getManagerIds(), clubId, eventMsg.build());

					Utils.sendJoinMemberInfo(club.getManagerIds(), club.getClubId(), member);

					// 返回推送
					Utils.notifyJoinResult(apply.getAccountId(), clientSessionId, club, ClubJoinCode.AGREE);

					ClubCacheService.getInstance().addMemberClubId(apply.getAccountId(), club.getClubId());
					ClubDataLogManager.addNewJoinPlayer(apply.getAccountId());

					//清除之前玩家退出亲友圈时没有正常移除的玩家记录数据
					club.runInDBLoop(new ClubMemberRecordDelDBTask(clubId, member.getAccount_id()));
				}
			}
			// 检查是否可开启亲友圈福卡功能
			club.clubWelfareWrap.checkOpenClubWelfare();
		}

		SpringService.getBean(ClubDaoService.class).batchInsert("insertClubAccount", accounts);
		return new ClubRoomModel(Club.SUCCESS).setDesc(accounts.isEmpty() ? "没有需要操作的请求" : "操作完成!");
	}

	public int rejectClub(int club_id, long ownerId, long targetId) {
		Club club = clubs.get(club_id);

		if (club == null) {
			return Club.CLUB_NOT_FIND;
		}

		ClubMemberModel member = club.members.get(ownerId);
		if (null == member || !EClubIdentity.isManager(member.getIdentity())) {
			return Club.PERM_DENIED;
		}

		return club.rejectJoinClub(targetId, ownerId);
	}

	public int kickGroup(int clubId, long ownerId, long accountId) {
		Club club = clubs.get(clubId);
		if (club == null) {
			return Club.CLUB_NOT_FIND;
		}
		if (club.clubModel.getAccount_id() == accountId) {
			return Club.PERM_DENIED;
		}

		if (club.groupSet.size() > 0) {
			try {
				ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
				for (String groupId : club.groupSet) {
					HashMap<String, String> map = new HashMap<String, String>();
					map.put("groupId", groupId);
					map.put("accountId", accountId + "");
					map.put("type", "1");
					centerRMIServer.rmiInvoke(RMICmd.CLUB_TO＿GROUP, map);
				}
			} catch (Exception e) {
				logger.error("亲友圈踢人同步微信群踢人异常", e);
			}
		}
		return Club.SUCCESS;
	}

	public int kickClub(int clubId, long ownerId, long accountId, int exitType) {
		Club club = clubs.get(clubId);

		if (club == null) {
			return Club.CLUB_NOT_FIND;
		}
		ClubMemberModel operator = club.members.get(ownerId);
		if (null == operator || !EClubIdentity.isManager(operator.getIdentity())) {
			return Club.PERM_DENIED;
		}

		ClubMemberModel target = club.members.get(accountId);
		if (null == target) {
			return Club.ACCOUNT_NOT_FIND;
		}

		if (target.getIdentity() >= operator.getIdentity()) {
			return Club.PERM_DENIED;
		}

		// 判断是否是合伙人
		if (target.isPartner()) {
			return Club.IS_PARTNER;
		}

		ClubMemberModel memberModel = club.members.remove(accountId);
		if (memberModel != null) {
			// ClubAccountModel requestClubAccount = new ClubAccountModel();
			// requestClubAccount.setAccount_id(accountId);
			// requestClubAccount.setClub_id(clubId);
			// SpringService.getBean(ClubDaoService.class).getDao().deleteClubAccount(requestClubAccount);
			// SpringService.getBean(ClubDaoService.class).getDao().deleteClubMemberRecord(clubId,
			// accountId);

			club.runInDBLoop(new DelMemberDBTask(clubId, accountId, memberModel));

			int code = ClubJoinCode.KICK;
			if (exitType == ClubExitType.AGREE_QUIT) {
				code = ClubJoinCode.AGREE_QUIT;
			}
			// 提玩家推送
			Utils.notifyJoinResult(accountId, ownerId, club, code);

			ClubCacheService.getInstance().rmMemberClubId(accountId, clubId);

			// logger.warn("玩家[{}] 被[{}]踢出亲友圈[{}]!", accountId, ownerId,
			// clubId);
			ClubEventLog.event(new ClubEventLogModel(clubId, ownerId, EClubEventType.OUT).setTargetId(accountId));

			club.exitClubMatch(memberModel);

			club.clubWelfareWrap.outClub(memberModel);

			// 事件
			ClubJoinQuitMsgProto.Builder eventMsg = Utils.newEventMsg(clubId, MsgType.QUIT, null, target);
			club.joinQuitMsgQueueProto.offer(eventMsg.build());
			Utils.sendClubEventMsg(club.getManagerIds(), clubId, eventMsg.build());
			Utils.notifyRedHeart(club, ERedHeartCategory.CLUB_EVENT_NOTIFY);

			ClubApplyLogModel applyQuitModel = club.requestQuitMembers.remove(accountId);
			if (applyQuitModel != null && !applyQuitModel.isHandle()) {
				applyQuitModel.setHandle(true);
				MongoDBServiceImpl.getInstance().updateClubApplyLogModel(applyQuitModel);
			}

			return Club.SUCCESS;
		}
		return Club.ACCOUNT_NOT_FIND;
	}

	public ClubRoomModel outClub(int clubId, long accountId) {
		Club club = clubs.get(clubId);

		if (club == null) {
			return new ClubRoomModel(Club.FAIL).setDesc("亲友圈不存在!");
		}
		if (club.clubModel.getAccount_id() == accountId) {
			return new ClubRoomModel(Club.FAIL).setDesc("创始人不能退出亲友圈!");
		}
		ClubMemberModel targetModel = club.members.get(accountId);
		if (targetModel == null) {
			return new ClubRoomModel(Club.SUCCESS).setDesc("你已经不在亲友圈了!");
		}
		if (targetModel.isPartner()) {
			return new ClubRoomModel(Club.FAIL).setDesc("您是该亲友圈合伙人，请先解除合伙人关系");
		}

		synchronized (club) {
			ClubMemberModel memberModel = club.members.remove(accountId);
			if (memberModel != null) {
				// ClubAccountModel requestClubAccount = new ClubAccountModel();
				// requestClubAccount.setAccount_id(accountId);
				// requestClubAccount.setClub_id(clubId);
				//
				// SpringService.getBean(ClubDaoService.class).getDao().deleteClubAccount(requestClubAccount);
				// SpringService.getBean(ClubDaoService.class).getDao().deleteClubMemberRecord(clubId,
				// accountId);

				club.runInDBLoop(new DelMemberDBTask(club.getClubId(), accountId, memberModel));

				club.exitClubMatch(memberModel);

				club.clubWelfareWrap.outClub(memberModel);

				// 事件
				ClubJoinQuitMsgProto.Builder eventMsg = Utils.newEventMsg(clubId, MsgType.QUIT, null, memberModel);
				club.joinQuitMsgQueueProto.offer(eventMsg.build());
				Utils.sendClubEventMsg(club.getManagerIds(), clubId, eventMsg.build());
				Utils.notifyRedHeart(club, ERedHeartCategory.CLUB_EVENT_NOTIFY);

				ClubCacheService.getInstance().rmMemberClubId(accountId, clubId);

				// logger.warn("玩家[{}] 退出亲友圈[{}]!", accountId, clubId);

				ClubEventLog.event(new ClubEventLogModel(clubId, accountId, EClubEventType.OUT).setTargetId(accountId));

				ClubApplyLogModel applyQuitModel = club.requestQuitMembers.remove(accountId);
				if (applyQuitModel != null && !applyQuitModel.isHandle()) {
					applyQuitModel.setHandle(true);
					MongoDBServiceImpl.getInstance().updateClubApplyLogModel(applyQuitModel);
				}
			}
		}

		return new ClubRoomModel(Club.SUCCESS).setDesc("退出成功!");
	}

	public void updateAccountId(long new_id, long account_id) {
		SpringService.getBean(ClubDaoService.class).getDao().updateClubAccountId(new_id, account_id);
		SpringService.getBean(ClubDaoService.class).getDao().updateClubAccountId2(new_id, account_id);

		for (Club club : clubs.values()) {
			synchronized (club) {
				if (club.clubModel.getAccount_id() == account_id) {
					club.clubModel.setAccount_id(new_id);
				}

				ClubMemberModel member = club.members.get(account_id);
				if (member != null) {
					member.setAccount_id(new_id);
					Map<Integer, ClubMemberRecordModel> recordMap = member.getMemberRecordMap();
					for (ClubMemberRecordModel recordModel : recordMap.values()) {
						recordModel.setAccountId(new_id);
					}
					ClubBanPlayerModel clubBanPlayerModel = member.getClubBanPlayerModel();
					if (clubBanPlayerModel != null) {
						clubBanPlayerModel.setAccountId(new_id);
					}
					club.members.forEach((id, m) -> {
						Map<Long, Long> banMap = m.getMemberBanPlayerMap();
						if (banMap != null) {
							if (banMap.containsKey(account_id)) {
								banMap.remove(account_id);
								banMap.put(new_id, new_id);
							}
						}
					});

					club.members.put(new_id, member);
					club.members.remove(account_id);
					ClubCacheService.getInstance().rmMemberClubId(account_id, club.getClubId());
					ClubCacheService.getInstance().addMemberClubId(new_id, club.getClubId());
				}

				ClubApplyJoinProto join = club.requestMembers.get(account_id);

				if (join != null) {
					club.requestMembers.put(new_id, join.toBuilder().setAccountId(new_id).build());
					club.requestMembers.remove(account_id);
				}
			}

		}
	}

	public int bindGroup(int clubId, String groupId) {
		Club club = clubs.get(clubId);

		if (club == null) {
			return Club.CLUB_NOT_FIND;
		}

		if (SpringService.getBean(ClubDaoService.class).getDao().getClubGroup(groupId) != null) {
			return -2;
		}
		ClubGroupModel group = new ClubGroupModel();
		group.setGroup_id(groupId);
		group.setClub_id(clubId);
		SpringService.getBean(ClubDaoService.class).getDao().insertClubGroup(group);
		club.groupSet.add(groupId);
		return 1;
	}

	public ClubRoomModel createClubRoom(ClubRequest request, ProxyClubRq topRequest, C2SSession session) {

		if (!ClubCfg.get().isOpen()) {
			String tip = ClubCfg.get().getTip();

			return new ClubRoomModel(Club.FAIL).setDesc(Strings.isNullOrEmpty(tip) ? "亲友圈临时维护，暂不能开房，请稍后再试!" : tip);
		}

		int clubId = request.getClubId(), ruleId = request.getClubRuleId(), joinId = request.getJoinId();
		String accountIp = topRequest.getAccountIp(), groupIds = request.getGroupId();
		Club club = clubs.get(clubId);

		if (club == null) {
			return new ClubRoomModel(Club.FAIL).setDesc("亲友圈不存在！");
		}

		ClubModel clubModel = club.clubModel;

		long accountId = topRequest.getClientSessionId();
		ClubMemberModel member = club.members.get(accountId);
		if (null == member) {

			boolean isInClubGroup = false;
			String[] groupIdArray = StringUtils.split(groupIds, Symbol.COLON);
			if (null != groupIdArray && !club.groupSet.isEmpty()) {
				for (final String clubSubGroupId : club.groupSet) {
					for (final String accountGroupId : groupIdArray) {
						if (Objects.equals(accountGroupId, clubSubGroupId)) {
							isInClubGroup = true;
							break;
						}
					}
				}
			}

			if (!isInClubGroup) {
				return new ClubRoomModel(Club.FAIL).setDesc("不是亲友圈成员！");
			}
		}

		// 黑名单
		if (null != member && EClubIdentity.isDefriend(member.getIdentity())) {
			return new ClubRoomModel(Club.FAIL).setDesc(Club.DEFRIEND_TIP);
		}

		// 冻结?
		if (club.setsModel.isStatusTrue(EClubSettingStatus.CLUB_FREEZE)) {
			logger.info("亲友圈[{}]冻结了，不可以进行开房操作!", clubModel.getClub_id());
			return new ClubRoomModel(Club.FAIL).setDesc("亲友圈被管理员冻结，请联系管理员!");
		}

		ClubRuleTable clubRuleTable = club.ruleTables.get(ruleId);
		if (null == clubRuleTable) {
			return new ClubRoomModel(Club.FAIL).setDesc("包间不存在，请退出重试！");
		}

		ClubRuleModel ruleModel = clubModel.getRule(ruleId);
		if (ruleModel == null) {
			return new ClubRoomModel(Club.FAIL).setDesc("该亲友圈没有这个游戏类型");
		}

		int gameId = ruleModel.getGame_id();
		int gameTypeIndex = ruleModel.getGame_type_index();
		SysParamModel sysParamModel = null;
		// 开放判断
		try {
			sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
					.get(SysGameTypeDict.getInstance().getGameGoldTypeIndex(gameTypeIndex));
		} catch (Exception e) {
			logger.error("SysParamModel 获取失败,gameId={},game_type_index={}", gameId, gameTypeIndex, e);
			return new ClubRoomModel(Club.FAIL).setDesc("即将开放,敬请期待!");
		}

		if (sysParamModel != null && sysParamModel.getVal1() != 1) {
			return new ClubRoomModel(Club.FAIL).setDesc(Strings.isNullOrEmpty(sysParamModel.getStr2()) ? "即将开放,敬请期待!" : sysParamModel.getStr2());
		}

		// 下线
		if (ClubCfg.get().getOfflineGames().contains(ruleModel.getGame_id())) {
			return new ClubRoomModel(Club.FAIL).setDesc(ClubCfg.get().getOfflineGameTip());
		}

		int tableIndex = (joinId & 0xffff0000) >> 16;
		int seatIndex = joinId & 0x0000ffff;

		// 桌子索引和座位索引校验
		if (tableIndex < 0 || tableIndex > ClubCfg.get().getRuleTableMax() || seatIndex < 0 || seatIndex > clubRuleTable.getPlayerLimit()) {
			return new ClubRoomModel(Club.FAIL).setDesc("房间已满，不能加入!");
		}

		ClubTable table = clubRuleTable.getTable(tableIndex);
		if (table.getCurRound() > 0 && null != table.getSetsModel() && table.getSetsModel()
				.isStatusTrue(ERoomSettingStatus.ROOM_FORBID_HALF_WAY_ENTER)) {
			return new ClubRoomModel(Club.FAIL).setDesc("游戏已经开始,禁止中途加入!");
		}

		// 位置判断
		Optional<ClubSeat> seatOpt = ClubCacheService.getInstance().seat(accountId);
		if (ClubCfg.get().isCheckSeat() && seatOpt.isPresent() && seatOpt.get().isOnSeat() && !ClubSeat.eq(seatOpt.get(), table.sceneTag())) {
			return new ClubRoomModel(Club.FAIL).setDesc("您已经在其他桌落座，请先退出桌子后再进入!!");
		}

		// 疲劳值判断
		if (club.isTireSwitchOpen()) {
			if (ruleModel.getSetsModel().isStatusTrue(ERuleSettingStatus.TIRE_VALUE_SWITCH)) {
				ClubMemberRecordModel memberRecordModel = club.getMemberRecordModelByDay(1, member);
				if (club.getMemberRealUseTire(memberRecordModel) < ruleModel.getTireValue()) {
					return new ClubRoomModel(Club.FAIL).setDesc("疲劳值太低了,无法进行游戏,请联系管理员进行处理");
				}
			}
		}
		// 局数限制判断(观战类游戏在坐下时再判断,其他游戏在进入房间时就判断)
		if (!SysParamDict.getInstance().isObserverGameTypeIndex(ruleModel.getGame_type_index())) {
			if (ruleModel.getSetsModel().isStatusTrue(ERuleSettingStatus.GAME_ROUND_LIMIT_SWITCH)) {
				int leftRound = member.checkLimitRound(ruleModel);
				if (leftRound == 0) {
					return new ClubRoomModel(Club.FAIL).setDesc("今日您在本包间牌局数已用完，无法继续游戏");
				}
			}
		}
		// 亲友圈福卡限制判断
		if (club.clubWelfareWrap.isOpenClubWelfare()) {
			if (ruleModel.getSetsModel().isStatusTrue(ERuleSettingStatus.CLUB_WELFARE_SWITCH)) {
				if (member.getClubWelfare() < ruleModel.getLimitWelfare()) {
					return new ClubRoomModel(Club.FAIL).setDesc(String.format("您的福卡不足%d，无法进入牌桌", ruleModel.getLimitWelfare()));
				}
			}
		}

		// ip限制新规则
		Integer ipLimitRuleValue = ruleModel.getRuleParams().getRuleValue(GameConstants.GAME_RULE_IP);
		if (null != ipLimitRuleValue && ipLimitRuleValue.intValue() == 1) {
			if (table.hasSameIP(accountIp)) {
				return new ClubRoomModel(Club.FAIL).setDesc("牌桌内与你相同ip玩家,禁止加入!");
			}
		}

		if (table.isInRoom(accountId)) {
			return new ClubRoomModel(Club.FAIL).setDesc("你已经在当前牌桌内，不可以重复加入!");
		}

		// 密码验证
		int passport = table.getPassport();
		if (passport > 0 && passport != request.getTablePassport()) {
			if (request.getTablePassport() == 0) {
				// 需要输入密码
				ClubTableNeedPassportResponse.Builder rspBuilder = ClubTableNeedPassportResponse.newBuilder();
				rspBuilder.setClubId(clubId);
				rspBuilder.setRuleId(ruleId);
				rspBuilder.setTableIndex(tableIndex);
				session.send(PBUtil.toS_S2CRequet(accountId, S2CCmd.CLUB_TABLE_NEED_PASSPORT_RSP, rspBuilder));
				return null;
			} else {
				return new ClubRoomModel(Club.FAIL).setDesc("密码不正确!");
			}
		}
		// 禁止同玩玩家判断
		Map<Long, ClubPlayer> players = table.getPlayers();
		for (Long userId : players.keySet()) {
			if (userId != accountId) {
				ClubMemberModel memModel = club.members.get(userId);
				if (memModel == null) {
					member.removeBanPlayer(userId);
					continue;
				}
				Map<Long, Long> tmpMap = memModel.getMemberBanPlayerMap();
				if (tmpMap != null && tmpMap.containsKey(accountId)) {
					return new ClubRoomModel(Club.FAIL).setDesc(String.format("您暂时无法与 %s 玩家同桌进行游戏，如有疑问请联系亲友圈管理员!", players.get(userId).getUserName()));
				}
			}
		}

		// GAME-TODO座位判断
		if (seatIndex > 0) {
			ClubPlayer player = table.getPlayerBySeat(seatIndex);
			if (null != player) {
				logger.info("亲友圈[{}]，此坑有人[{},{},{}]", clubModel.getClub_name(), ruleId, tableIndex, seatIndex);
			}
		}

		if (ClubCfg.get().isAccessLimit() && ClubCacheService.getInstance().isBlockVisit(accountId)) {
			return new ClubRoomModel(Club.FAIL).setDesc("操作过于频繁了哦，请稍候再试 !");
		}

		if (club.isMultiClubRuleTableMode()) {
			if (System.currentTimeMillis() - member.getLastAutoKickTime() < ClubCfg.get().getPlayerEnterTableBanTime() * TimeUtil.SECOND) {
				return new ClubRoomModel(Club.FAIL).setDesc("您需要等待" + ClubCfg.get().getPlayerEnterTableBanTime() + "秒后，才能重新进入牌桌");
			}
		}

		// 链接信息
		final Pair<EServerType, Integer> serverInfo = SessionUtil.getAttr(session, AttributeKeyConstans.CLUB_SESSION);
		synchronized (club) {
			RedisService redisService = SpringService.getBean(RedisService.class);
			ClubRoomRedisModel room = redisService
					.hGet(RedisKeyUtil.clubRoomKey(clubId, ruleId), Integer.toString(tableIndex), ClubRoomRedisModel.class);

			do {
				if (null != room) {
					RoomRedisModel roomRedisModel = redisService.hGet(RedisConstant.ROOM, room.getRoom_id() + "", RoomRedisModel.class);

					if (roomRedisModel == null || roomRedisModel.getClub_id() != clubId) {
						if (ClubCfg.get().isDelRedisCache()) {
							table.delTableCache();
							return new ClubRoomModel(Club.FAIL).setDesc(ClubCfg.get().getDelRedisCacheTip());
						}
						break;
					}

					// 满人
					if (clubRuleTable.getPlayerLimit() > 0 && roomRedisModel.getPlayersIdSet().size() >= clubRuleTable.getPlayerLimit()) {
						boolean isObserverGame = SysParamDict.getInstance().isObserverGameTypeIndex(roomRedisModel.getGame_type_index());
						if (!isObserverGame) {
							return new ClubRoomModel(Club.FAIL).setDesc("房间人数已满，不能加入!");
						}
					}

					Supplier<ClubRoomModel> supplier = () -> {
						return new ClubRoomModel(Club.SUCCESS, ruleModel, roomRedisModel.getRoom_id()).setLogicId(roomRedisModel.getLogic_index());
					};
					// 创建房间
					LogicMsgSender.sendJoinRoom(topRequest, club, supplier.get(), serverInfo.getSecond().intValue(), joinId);

					return new ClubRoomModel(Club.SUCCESS);
				}

			} while (false);

			boolean repair = ClubCfg.get().getRuleUpdateSubGameIds().contains(clubRuleTable.getGameTypeIndex());
			if (SystemConfig.gameDebug == 1)
				repair = true;// 调试环境直接去对应的逻辑服取

			ICenterRMIServer centerRmiServer = SpringService.getBean(ICenterRMIServer.class);
			int debugLogicId = /*SystemConfig.gameDebug == 1 ? SystemConfig.club_index :*/ 0;
			ClubRoomModel clubRoomModel = centerRmiServer
					.createClubRoom(accountId, clubId, ruleModel, club.clubModel.getAccount_id(), club.clubModel.getClub_name(), repair, tableIndex,
							club.getMemberCount(), debugLogicId);

			clubRoomModel.setClub_name(club.clubModel.getClub_name());

			// 序列号后参数会被设置为null
			clubRoomModel.setClubRule(ruleModel);
			if (clubRoomModel.getStatus() != Club.SUCCESS) {
				return clubRoomModel;
			}

			// 亲友圈房间基本信息写入redis[后面改成本地缓存，不走redis，GAME-TODO]
			ClubRoomRedisModel roomRedisModel = new ClubRoomRedisModel();
			roomRedisModel.setRoom_id(clubRoomModel.getRoomId());
			redisService.hSet(RedisKeyUtil.clubRoomKey(clubId, ruleId), Integer.toString(tableIndex), roomRedisModel);

			sendUrlToClubGrup(club, clubRoomModel, ruleModel);

			clubRoomModel.setAttament(joinId);

			// 创建房间
			LogicMsgSender.sendCreateRoom(topRequest, club, clubRoomModel, serverInfo.getSecond().intValue(), joinId);
			ClubDataLogManager.addActiveTableCount();

			return clubRoomModel;
		}
	}

	/**
	 * 推送房间邀请到群
	 */
	private void sendUrlToClubGrup(final Club club, final ClubRoomModel clubRoomModel, ClubRuleModel roomRule) {
		int clubId = club.getClubId();
		try {
			boolean isSend = SysParamServerDict.getInstance().getSendGroupRoom();
			if (isSend) {
				services.execute(new Runnable() {
					@Override
					public void run() {
						try {
							// 亲友圈绑定的群已经存入缓存
							if (club.groupSet.size() > 0) {
								RoomJsonModel roomJson = new RoomJsonModel("link");
								roomJson.setUrl(clubRoomModel.getUrl());
								roomJson.setDesc("亲友圈ID[" + clubId + "]," + clubRoomModel.getGameDesc());
								roomJson.setTitle(
										SysGameTypeDict.getInstance().getMJname(roomRule.getGame_type_index()) + "亲友圈房间 [" + clubRoomModel.getRoomId()
												+ "]" + " 1_" + clubRoomModel.getMaxNumber());

								for (String groupId : club.groupSet) {
									ClubRoomJsonModel clubJson = new ClubRoomJsonModel(1, groupId, clubId, club.getClubName(), roomJson);
									HttpClientUtils.httpPostWithJSON(SystemConfig.clubToGroup, JSON.toJSONString(clubJson));
								}
							}
						} catch (Exception e) {
							logger.error("createRandomRoom broadcast group error", e);
						}
					}
				});
			}
		} catch (Exception e) {
			logger.error("推送房间异常", e);
		}
	}

	public Club getClub(int clubId) {
		return clubs.get(clubId);

	}

	public Club removeClub(int clubId) {
		return clubs.remove(clubId);
	}

	public ClubModel getClubModel(int clubId) {
		Club club = clubs.get(clubId);

		if (club == null) {
			return null;
		}

		club.clubModel.setMembersCount(club.members.size());
		return club.clubModel;
	}

	int bindClubAccount(int clubId, long accountId) {
		Club club = clubs.get(clubId);

		if (club == null) {
			return Club.CLUB_NOT_FIND;
		}

		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);

		PlayerViewVO bindAccount = centerRMIServer.getPlayerViewVo(accountId);
		if (bindAccount == null) {
			return -3;
		}

		// if (bindAccount.getNickName() != nickName) {
		// return -4;
		// }

		if (club.members.containsKey(accountId)) {
			return -2;
		}

		ClubMemberModel member = new ClubMemberModel();
		member.setAccount_id(bindAccount.getAccountId());
		member.setAvatar(bindAccount.getHead());
		member.setNickname(bindAccount.getNickName());
		member.setClubName(club.clubModel.getClub_name());
		member.setDate(new Date());
		member.setClub_id(clubId);

		if (club.members.putIfAbsent(accountId, member) != null) {
			return -2;
		}

		ClubAccountModel requestClubAccount = new ClubAccountModel();
		requestClubAccount.setAccount_id(accountId);
		requestClubAccount.setClub_id(clubId);
		requestClubAccount.setStatus(1);

		SpringService.getBean(ClubDaoService.class).getDao().insertClubAccount(requestClubAccount);

		ClubApplyJoinProto temp = club.requestMembers.remove(accountId);
		if (temp != null) {
			club.agreeRequestMembers.offer(temp.toBuilder().setJoinTime(System.currentTimeMillis()).build());
		}

		// 返回推送
		Utils.notifyJoinResult(accountId, 0L, club, ClubJoinCode.AGREE);
		return 1;

	}

	public void roomPlayerLog(ClubGameOverProto clubGameOverProto, long createTime) {
		Club club = clubs.get(clubGameOverProto.getClubId());

		if (club == null) {
			return;
		}

		// 得出最大分数，等于这个分数的都是大赢家
		int maxScore = 0;
		for (GamePlayerProto p : clubGameOverProto.getPlayersList()) {
			if (p.getScore() > maxScore) {
				maxScore = p.getScore();
			}
		}

		final int _maxScore = maxScore;
		club.runInReqLoop(() -> {
			List<ClubMemberModel> members = new ArrayList<>(clubGameOverProto.getPlayersCount());
			if (clubGameOverProto.getClubMatchId() > 0) { // 自建赛不统计到俱乐部战绩里
				return;
			}
			boolean needCostClubWelfare = false;
			int lotteryCost = 0;
			int endTime = (int) (System.currentTimeMillis() / 1000);
			int gameTypeIndex = 0;
			if (club.clubWelfareWrap.isOpenClubWelfare()) {
				ClubRuleModel ruleModel = club.clubModel.getRule(clubGameOverProto.getRuleId());
				if (ruleModel != null && ruleModel.getSetsModel().isStatusTrue(ERuleSettingStatus.CLUB_WELFARE_SWITCH)) {
					gameTypeIndex = ruleModel.getGame_type_index();
					needCostClubWelfare = true;
					int bigWinnerCount = 0;
					for (int i = 0; i < clubGameOverProto.getPlayersCount(); i++) {
						GamePlayerProto player = clubGameOverProto.getPlayers(i);
						if (player.getScore() == _maxScore) {
							bigWinnerCount++;
						}
					}
					lotteryCost = ruleModel.getLotteryCost() / bigWinnerCount;
				}
			}
			for (int i = 0; i < clubGameOverProto.getPlayersCount(); i++) {
				GamePlayerProto player = clubGameOverProto.getPlayers(i);
				ClubMemberModel member = club.members.get(player.getAccountId());
				if (member != null) {
					// 判断是否跨天,同一天则更新到今天的数据里,跨天了则更新到昨天的数据里
					int updateDay = 1;
					if (createTime != 0) {
						Date createDate = new Date(createTime * 1000);
						Date now = new Date();
						updateDay = DateUtils.isSameDay(now, createDate) ? 1 : 2;
						if (updateDay == 2) {
							// 跨天的情况还需要判断玩家记录每日重置是否执行过了
							long nextRefreshTime = club.getNextRefreshTime();
							if (now.getTime() >= nextRefreshTime) {
								updateDay = 1;
							}
						}
					}
					club.updateMemberRecordModelByDay(member.getAccount_id(), updateDay, ClubRecordDataType.GAME_COUNT, 1);
					if (player.getScore() == _maxScore) {
						member.setWinCount(member.getWinCount() + 1);
						club.updateMemberRecordModelByDay(member.getAccount_id(), updateDay, ClubRecordDataType.BIG_WIN_COUNT, 1);
						if (needCostClubWelfare) {
							ClubCacheService.getInstance()
									.addMemWelfareLotteryInfo(member.getAccount_id(), lotteryCost, endTime, club.getClubId(), gameTypeIndex);
							club.clubWelfareWrap.notifyLottery(member, lotteryCost);
						}
					}
					club.updateMemberRecordModelByDay(member.getAccount_id(), updateDay, ClubRecordDataType.TIRE_VALUE, player.getScore());
					if (updateDay == 2 && !club.isTireDailyReset()) { // 如果开启了累计,跨天的情况还要修改今日的累计疲劳值
						ClubMemberRecordModel recordModel = club.getMemberRecordModelByDay(1, member);
						recordModel.setAccuTireValue(recordModel.getAccuTireValue() + player.getScore());
					}
					member.setGame_count(member.getGame_count() + 1);

					// 更新玩家打过的局数
					if (updateDay == 1) {
						ClubRuleTable ruleTable = club.ruleTables.get(clubGameOverProto.getRuleId());
						if (ruleTable != null) {
							member.updatePlayRound(clubGameOverProto.getRuleId());
						}
					}

					members.add(member);

					ClubDataLogManager.addActivePlayer(member.getAccount_id());
				}
			}
			// SpringService.getBean(ClubDaoService.class).batchUpdate("updateClubAccountGameCount",
			// members);
			club.runInDBLoop(new ClubRecordDBTask(members));
			if (clubGameOverProto.getCurRound() != 0 && clubGameOverProto.getGameRound() == clubGameOverProto.getCurRound()) {
				ClubDataLogManager.addClubCompleteParentBrandCount();
			}
			ClubDataLogManager.addClubChildBrandCount(clubGameOverProto.getCurRound());
			ClubDataLogManager.statGameInfoData(clubGameOverProto);
			ClubDataLogManager.addActiveClubNum(club.getClubId());
		});
	}

	public void roomKouDou(ClubGameRecordProto req) {
		long clubMatchId = req.getGameOver().getClubMatchId();
		if (clubMatchId > 0) { // 自建赛数据不统计到亲友圈战绩中
			return;
		}

		int clubId = req.getKouDou().getClubId();
		int gold = req.getKouDou().getGold();
		long createTime = req.getKouDou().getCreateTime();
		int wealthCategory = req.getKouDou().getWealthCategory();
		Club club = clubs.get(clubId);

		if (club == null) {
			return;
		}

		club.runInClubLoop(() -> {
			club.updateGameCount(gold, createTime, wealthCategory);

			ClubRuleTable ruleTable = club.ruleTables.get(req.getGameOver().getRuleId());
			if (ruleTable != null) {
				ruleTable.updateRuleTableRecord(gold, createTime, wealthCategory);
			}
		});
	}

	/**
	 * 玩家上下线
	 */
	public void playerStatus(final long accountId, EPlayerStatus eStatus) {
		if (eStatus == EPlayerStatus.ONLINE) {
			notifyOfflineRedHeartIfHas(accountId);
		} else { // 离线
			memberOffline(accountId);
		}
	}

	/**
	 * 因为红点
	 */
	private void notifyOfflineRedHeartIfHas(long accountId) {

		Collection<Club> clubsTmp = getMyEnterClub(accountId);
		for (Club club : clubsTmp) {
			ClubMemberModel member = club.members.get(accountId);
			if (member == null) {
				continue;
			}
			if (EClubIdentity.isManager(member.getIdentity())) {
				if (club.requestMembers.size() > 0) {
					club.sendHaveNewMsg(ERedHeartCategory.CLUB_REQ_ENTER);
				}
				if (club.requestQuitMembers.size() > 0) {
					club.sendHaveNewMsg(ERedHeartCategory.CLUB_REQ_EXIT);
				}
			}
			if (member.isHaveNewClubMatchLog()) {
				member.setHaveNewClubMatchLog(false);
				ClubRedHeartRsp.Builder builder = ClubRedHeartRsp.newBuilder().setClubId(club.getClubId())
						.setType(ERedHeartCategory.CLUB_MATCH_NEW_LOG);
				Utils.sendClient(accountId, S2CCmd.CLUB_RED_HEART, builder);
			}
		}
	}

	private void memberOffline(long accountId) {
		Collection<Club> clubs = getMyEnterClub(accountId);
		clubs.forEach((club) -> {
			final ClubMemberModel member = club.members.get(accountId);
			if (null != member) {
				member.setLastLoginDate(new Date());
			}
		});
	}

	/**
	 * 创建亲友圈
	 */
	private synchronized int createClub(long accountId, ClubModel clubModel) {
		clubModel.setAccount_id(accountId);

		if (getMyCreateClubCount(accountId) >= ClubCfg.get().getOwnerClubMax()) {
			return Club.CLUB_CREATE_MAX;
		}

		int id = randomClubId();
		if (id == -1) {
			logger.error("随机亲友圈Id失败");
			return Club.CLUB_CREATE_ERROR;
		}
		clubModel.setDatas(new byte[0]);
		clubModel.setDate(new Date());
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);

		PlayerViewVO account = centerRMIServer.getPlayerViewVo(accountId);

		if (account == null) {
			logger.error("创建亲友圈找不到账号");
			return Club.ACCOUNT_IS_NULL;
		}

		clubModel.setAvatar(account.getHead());
		clubModel.setClub_id(id);

		SpringService.getBean(ClubDaoService.class).getDao().insertClub(clubModel);
		ClubAccountModel requestClubAccount = new ClubAccountModel();
		requestClubAccount.setAccount_id(accountId);
		requestClubAccount.setClub_id(id);
		requestClubAccount.setStatus(1);
		for (ClubRuleModel rule : clubModel.getRules().values()) {
			rule.setClub_id(id);
		}
		SpringService.getBean(ClubDaoService.class).getDao().insertClubAccount(requestClubAccount);
		// SpringService.getBean(ClubDaoService.class).getDao().insertClubRule(clubModel.getRules().values());
		clubModel.setMembersCount(1);

		ClubDataModel clubDataModel = new ClubDataModel(clubModel.getClub_id());
		SpringService.getBean(ClubDaoService.class).getDao().insertClubDataModel(clubDataModel);
		Club club = new Club(clubModel, clubDataModel);
		club.setClubMatchLogWrap(new ClubMatchLogWrap(club.getClubId()));
		club.clubWelfareWrap.initLotteryLogMsg(null);

		ClubMemberModel member = new ClubMemberModel();
		member.setAccount_id(accountId);
		member.setAvatar(account.getHead());
		member.setNickname(account.getNickName());
		member.setClubName(club.clubModel.getClub_name());
		member.setDate(new Date());
		member.setIdentity(EClubIdentity.CREATOR.identify());
		member.setClub_id(id);
		club.members.put(member.getAccount_id(), member);

		clubs.put(id, club);

		// 事件
		ClubJoinQuitMsgProto.Builder eventMsg = Utils.newEventMsg(id, MsgType.CREATE, null, member);
		club.joinQuitMsgQueueProto.offer(eventMsg.build());
		Utils.sendClubEventMsg(club.getManagerIds(), id, eventMsg.build());
		Utils.notifyRedHeart(club, ERedHeartCategory.CLUB_EVENT_NOTIFY);

		ClubCacheService.getInstance().addMemberClubId(accountId, id);

		// logger.warn("玩家:[{}]创建亲友圈[{}]!", accountId, id);
		ClubEventLog.event(new ClubEventLogModel(id, accountId, EClubEventType.CREATE));

		// 同步亲友圈财富
		reqAccountWealthSyncTask(club.getOwnerId(), accountId, EWealthCategory.GOLD);

		ClubDataLogManager.addNewJoinPlayer(accountId);
		ClubDataLogManager.addNewClubCount();

		return id;
	}

	public List<ClubProto> getMyClub(long accountId, boolean selfCreate) {
		Collection<Club> clubs_ = selfCreate ? getMyCreateClub(accountId) : getMyEnterClub(accountId);

		final List<ClubProto> list = Lists.newArrayListWithCapacity(clubs_.size());
		clubs_.forEach((club) -> {
			try {
				list.add(club.encode(accountId, false).build());
			} catch (Exception e) {
				logger.error("亲友圈[{}] encode报错", club.getClubId(), e);
			}
			reqAccountWealthSyncTask(club.getOwnerId(), accountId, EWealthCategory.GOLD);
		});
		return list;
	}

	private int getMyEnterClubCount(long accountId) {
		return getMyEnterClub(accountId).size();
	}

	private int getMyCreateClubCount(long accountId) {
		return getMyCreateClub(accountId).size();
	}

	/**
	 * 挑选出玩家创建的亲友圈
	 */
	public Collection<Club> getMyCreateClub(long accountId) {
		if (ClubCfg.get().isUseNewGetClubWay()) {// 新方式，列表获得，效率高，复杂度高，需要维护影射列表
			Optional<Set<Integer>> opt = ClubCacheService.getInstance().optMemberClubs(accountId);

			if (!opt.isPresent()) {
				return Collections.emptyList();
			}
			final List<Club> clubs_ = Lists.newArrayList();
			opt.get().forEach(id -> {
				Club club = clubs.get(id);
				if (null != club && club.getOwnerId() == accountId) {
					clubs_.add(club);
				}
			});

			return clubs_;
		} else {// 旧方式，遍历查找，效率低，复杂度低
			return FilterUtil.filter(clubs.values(), (club) -> club.getOwnerId() == accountId);
		}
	}

	/**
	 * 挑选出玩家所在的所有亲友圈
	 */
	public Collection<Club> getMyEnterClub(long accountId) {
		if (ClubCfg.get().isUseNewGetClubWay()) { // 新方式，列表获得，效率高，复杂度高，需要维护影射列表
			Optional<Set<Integer>> opt = ClubCacheService.getInstance().optMemberClubs(accountId);
			if (!opt.isPresent()) {
				return Collections.emptyList();
			}

			final List<Club> clubs_ = Lists.newArrayList();
			opt.get().forEach(id -> {
				Club club = clubs.get(id);
				if (null != club && club.members.containsKey(accountId)) {
					clubs_.add(club);
				}
			});
			return clubs_;
		} else {// 旧方式，遍历查找，效率低，复杂度低
			return FilterUtil.filter(clubs.values(), (club) -> club.members.containsKey(accountId));
		}
	}

	/**
	 * 随机生成亲友圈id
	 */
	private int randomClubId() {
		int randomIndex = 0;
		while (randomIndex < 100) {
			randomIndex++;
			int id = ThreadLocalRandom.current().nextInt(9000000) + 1000000;
			if (!clubs.containsKey(id)) {
				return id;
			}
		}

		return -1;
	}

	@Override
	public void stop() throws Exception {
		save();
	}

	public int createClub(long clientSessionId, ClubProto clubProto) {

		if (clubProto.getClubName().isEmpty() || StringUtil.getLength(clubProto.getClubName()) > 14) {
			return Club.CLUB_NAME_ERROR;
		}

		if (EmojiFilter.containsEmoji(clubProto.getClubName())) {
			return Club.CLUB_NAME_ERROR;
		}
		if (/* !StringUtil.isChinese(clubProto.getClubName()) || */DirtyWordDict.getInstance().checkDirtyWord(clubProto.getClubName())) {
			return Club.CLUB_NAME_ERROR;
		}

		if (DirtyWordDict.getInstance().checkDirtyWord(clubProto.getDesc()) || EmojiFilter.containsEmoji(clubProto.getDesc())) {
			return Club.CLUB_DESC_ERROR;
		}

		if (getMyEnterClubCount(clientSessionId) >= ClubCfg.get().getOwnerClubMax()) {
			return Club.CLUB_CREATE_MAX;
		}

		ClubModel clubModel = new ClubModel();
		clubModel.setAccount_id(clientSessionId);
		clubModel.setClub_name(clubProto.getClubName());
		clubModel.setDesc(Strings.isNullOrEmpty(clubProto.getDesc()) ? "" : clubProto.getDesc());
		clubModel.setRules(Maps.newHashMap());
		clubModel.setSettingStatus(2);

		return createClub(clientSessionId, clubModel);
	}

	/**
	 * 更新亲友圈相关
	 */
	public ClubRoomModel updateClub(long clientSessionId, ClubUpdateProto clubProto) {
		Club club = clubs.get(clubProto.getClubId());

		if (club == null) {
			return new ClubRoomModel(Club.FAIL).setDesc("亲友圈不存在!");
		}

		ClubMemberModel member = club.members.get(clientSessionId);
		if (null == member || !EClubIdentity.isManager(member.getIdentity())) {
			return new ClubRoomModel(Club.FAIL).setDesc("权限不够!");
		}

		synchronized (club) {

			ClubModel clubModel = club.clubModel;
			// 1修改描述 2.修改玩法 3.添加玩法 4.删除玩法 5.亲友圈名 6.设置状态 7.设置玩法相关状态
			if (clubProto.getType() == 1) {
				clubModel.setDesc(clubProto.getDesc());

			} else if (clubProto.getType() == 2) {
				for (ClubRuleProto ruleProto : clubProto.getClubRuleList()) {
					if (ruleProto.getId() <= 0) {
						continue;
					}
					ClubRuleModel clubRuleModel = clubModel.getRule(ruleProto.getId());
					if (clubRuleModel != null) {
						int oldGameId = clubRuleModel.getGame_id();
						int oldGameTypeIndex = clubRuleModel.getGame_type_index();
						clubRuleModel.setGame_type_index(ruleProto.getGameTypeIndex());
						clubRuleModel.setRules(ruleProto.getRules());
						clubRuleModel.setGame_round(ruleProto.getGameRound());
						clubRuleModel.setRules(ruleProto.getRules());
						clubRuleModel.encodeRule();
						clubRuleModel.init();
						clubRuleModel.setGame_name(SysGameTypeDict.getInstance().getMJname(ruleProto.getGameTypeIndex()));
						clubRuleModel.setGame_id(SysGameTypeDict.getInstance().getGameIDByTypeIndex(ruleProto.getGameTypeIndex()));
						clubRuleModel.setShowType(ruleProto.getShowType());

						// 玩法修改
						ClubRuleTable ruleTable = club.ruleTables.get(ruleProto.getId());
						if (null != ruleTable) {

							//包间消耗落地
							try {
								if (oldGameTypeIndex != ruleProto.getGameTypeIndex()) {
									ClubRoomUtil.saveRuleCostModel(ruleTable, club.getMemberCount(), new Date());
								}
							} catch (Exception e) {
								e.printStackTrace();
							}

							// 告知在线玩家
							Utils.notifyUpdateRule(clubModel.getClub_id(), ruleProto.getId(), club, EClubOperateCategory.RULE_UPDATE);

							ruleTable.updateRule();
							// 事件
							ClubJoinQuitMsgProto.Builder eventMsg = Utils.newEventMsg(clubModel.getClub_id(), MsgType.MODIFY_RULE, member, null);
							club.joinQuitMsgQueueProto.offer(eventMsg.build());
							Utils.sendClubEventMsg(club.getManagerIds(), clubModel.getClub_id(), eventMsg.build());

							ClubEventLog.event(new ClubEventLogModel(club.getClubId(), clientSessionId, EClubEventType.RULE_U)
									.setVal1(ruleTable.getGameTypeIndex()).setVal2(ruleTable.getRuleId()));
							if (oldGameId != clubRuleModel.getGame_id()) {
								club.delPlayerLimitRound(clubRuleModel);
							}
						}
						club.runInDBLoop(() -> {
							SpringService.getBean(ClubDaoService.class).getDao().updateClubRule(clubRuleModel);
						});
					} else {
						logger.warn("亲友圈[{}],玩家:[{}]修改玩法[{}],但找不到该玩法！", club.getClubId(), clientSessionId, ruleProto.getId());
					}

				}

			} else if (clubProto.getType() == 3) { // 添加玩法

				return createRule(club, clubProto, clientSessionId);

			} else if (clubProto.getType() == 4) { // 删除玩法

				return delRule(club, clubProto, clientSessionId);

			} else if (clubProto.getType() == 5) { // 修改亲友圈名称
				return updateClubName(club, clubProto.getClubName(), false);

			} else if (clubProto.getType() == 6) { // 设置亲友圈相关状态
				return clubSettings(club, clubProto, member);

			} else if (clubProto.getType() == 7) { // 设置玩法相关状态
				for (ClubRuleProto ruleProto : clubProto.getClubRuleList()) {
					if (ruleProto.getId() <= 0) {
						continue;
					}
					if (ruleProto.getLotteryCost() > 0) {
						if(ruleProto.getLimitWelfare() < ruleProto.getLotteryCost()) {
							return new ClubRoomModel(Club.FAIL).setDesc("福卡入桌门槛不能小于大赢家抽奖消耗!");
						}
					}

					updateRuleSets(ruleProto, club);
					// 告知在线玩家
					Utils.notifyUpdateRule(clubModel.getClub_id(), ruleProto.getId(), club, EClubOperateCategory.RULE_UPDATE);
				}
			}
		}

		return new ClubRoomModel(Club.SUCCESS).setDesc("操作成功!");

	}

	/**
	 * 添加玩法
	 */
	private ClubRoomModel createRule(final Club club, final ClubUpdateProto clubProto, long operatorId) {
		final ClubModel clubModel = club.clubModel;
		if (ClubCfg.get().getClubRuleMax() < clubModel.getRules().size() + clubProto.getClubRuleList().size()) {
			return new ClubRoomModel(Club.FAIL).setDesc("添加游戏已达上限!");
		}
		List<ClubRuleModel> clubRuleModels = Lists.newArrayList();
		for (ClubRuleProto ruleProto : clubProto.getClubRuleList()) {

			int gameId = SysGameTypeDict.getInstance().getGameIDByTypeIndex(ruleProto.getGameTypeIndex());

			if (-1 == gameId) {
				return new ClubRoomModel(Club.FAIL).setDesc("游戏还未上线，请联系客服!");
			}

			if (ClubCfg.get().getOfflineGames().contains(gameId)) {
				return new ClubRoomModel(Club.FAIL).setDesc(ClubCfg.get().getOfflineGameTip());
			}
			if (ruleProto.getLotteryCost() > 0) {
				if(ruleProto.getLimitWelfare() < ruleProto.getLotteryCost()) {
					return new ClubRoomModel(Club.FAIL).setDesc("福卡入桌门槛不能小于大赢家抽奖消耗!");
				}
			}

			ClubRuleModel clubRuleModel = new ClubRuleModel();
			clubRuleModel.setClub_id(club.clubModel.getClub_id());
			clubRuleModel.setGame_type_index(ruleProto.getGameTypeIndex());
			clubRuleModel.setRules(ruleProto.getRules());
			clubRuleModel.setGame_round(ruleProto.getGameRound());
			clubRuleModel.setSettingStatus(0);
			clubRuleModel.setSetsModel(StatusModule.newWithStatus(0));
			clubRuleModel.encodeRule();
			clubRuleModel.init();
			clubRuleModel.setGame_name(SysGameTypeDict.getInstance().getMJname(ruleProto.getGameTypeIndex()));
			clubRuleModel.setGame_id(gameId);
			clubRuleModel.setShowType(ruleProto.getShowType());
			clubRuleModel.setLotteryCost(ruleProto.getLotteryCost());
			clubRuleModel.setLimitWelfare(ruleProto.getLimitWelfare());
			clubRuleModels.add(clubRuleModel);
			initRuleSets(ruleProto, clubRuleModel, club.getClubId());
		}

		SpringService.getBean(ClubDaoService.class).getDao().insertClubRule(clubRuleModels);

		// 初始化房间
		clubRuleModels.forEach((rule) -> {

			clubModel.getRules().put(rule.getId(), rule);
			club.ruleTables.put(rule.getId(), new ClubRuleTable(rule, false).addRule());

			// 告知在线玩家
			Utils.notifyUpdateRule(clubModel.getClub_id(), rule.getId(), club, EClubOperateCategory.RULE_ADD);

			ClubEventLog.event(new ClubEventLogModel(club.getClubId(), operatorId, EClubEventType.RULE_A).setVal1(rule.getId())
					.setVal2(rule.getGame_type_index()));

		});

		List<Integer> ruleIds = clubRuleModels.stream().map(ClubRuleModel::getId).collect(Collectors.toList());

		return new ClubRoomModel(Club.SUCCESS).setDesc("添加游戏成功!!").setAttament(ruleIds);
	}

	/**
	 * 删除玩法
	 */
	private ClubRoomModel delRule(final Club club, final ClubUpdateProto clubProto, long operatorId) {
		final ClubModel clubModel = club.clubModel;
		if (clubProto.getClubRuleList().isEmpty()) {
			return new ClubRoomModel(Club.FAIL).setDesc("请选择要删除的游戏!");
		}
		for (ClubRuleProto ruleProto : clubProto.getClubRuleList()) {

			if (ruleProto.getId() <= 0) {
				continue;
			}
			ClubRuleModel clubRuleModel = clubModel.getRules().remove(ruleProto.getId());
			if (clubRuleModel != null) {
				ClubRuleTable ruleTable = club.ruleTables.get(ruleProto.getId());
				if (null != ruleTable) {

					// 告知在线玩家
					Utils.notifyUpdateRule(clubModel.getClub_id(), ruleProto.getId(), club, EClubOperateCategory.RULE_DEL);
					ruleTable.deleteRule();

					ClubEventLog.event(new ClubEventLogModel(club.getClubId(), operatorId, EClubEventType.RULE_D).setVal1(ruleTable.getRuleId())
							.setVal2(ruleTable.getGameTypeIndex()));

					club.removeRule(ruleProto.getId());

					club.delPlayerLimitRound(clubRuleModel);

					ClubRoomUtil.saveRuleCostModel(ruleTable, club.getMemberCount(), new Date());
				}
				SpringService.getBean(ClubDaoService.class).getDao().deleteClubRuleWithRuleId(clubRuleModel.getId());

			}
		}

		return new ClubRoomModel(Club.SUCCESS).setDesc("删除成功!!");
	}

	/**
	 * 修改亲友圈名称
	 *
	 * @param club
	 * @param newClubName
	 * @param fromSshe    是否通过后台操作
	 */
	public ClubRoomModel updateClubName(final Club club, final String newClubName, boolean fromSshe) {

		if (Strings.isNullOrEmpty(newClubName) || StringUtil.getLength(newClubName) > 14) {
			return new ClubRoomModel(Club.FAIL).setDesc("亲友圈名称不合理!");
		}

		if (!fromSshe && !club.isCanChangeName()) {
			return new ClubRoomModel(Club.FAIL).setDesc("今天已经修改过，明天再来！");
		}

		if (DirtyWordDict.getInstance().checkDirtyWord(newClubName) || EmojiFilter.containsEmoji(newClubName)) {
			return new ClubRoomModel(Club.FAIL).setDesc("亲友圈名称不可以包含敏感字符!");
		}
		synchronized (club) {
			club.clubModel.setClub_name(newClubName);
		}

		if (!fromSshe) {
			club.setLastChangeNameDate(new Date());
		}

		return new ClubRoomModel(Club.SUCCESS).setDesc("修改成功!");
	}

	/**
	 * 亲友圈设置
	 */
	private ClubRoomModel clubSettings(final Club club, final ClubUpdateProto clubProto, ClubMemberModel operator) {
		final ClubModel clubModel = club.clubModel;
		List<CommonII> sets = clubProto.getSetStatusList();
		final StatusModule model = club.setsModel;

		final int oldSets = model.getStatus();
		// 是否冻结
		final boolean isFreeze = model.isStatusTrue(EClubSettingStatus.CLUB_FREEZE);
		final boolean isConceal = model.isStatusTrue(EClubSettingStatus.CONCEAL_TABLE);
		final boolean oldTireSwitch = club.isTireSwitchOpen();
		final boolean oldTireResetSwitch = club.isTireDailyReset();
		final boolean oldRuleTableMode = club.isMultiClubRuleTableMode();

		if (null != sets && !sets.isEmpty()) {
			sets.forEach((set) -> {
				EClubSettingStatus eTypes = EClubSettingStatus.of(set.getK());
				if (EClubSettingStatus.NONE != eTypes) {
					if (set.getV() == 0)
						model.statusDel(eTypes);
					else
						model.statusAdd(eTypes);
				} else {
					logger.error("亲友圈[{}] 设置类型[{}]有误 @see EclubSettingStatus!", club.clubModel.getClub_id(), set.getK());
				}
			});
		} else {
			return new ClubRoomModel(Club.FAIL).setDesc("设置失败!");
		}

		clubModel.setSettingStatus(model.getStatus());

		//没有任何变化，下面的流程不必要执行
		if (model.getStatus() == oldSets) {
			return new ClubRoomModel(Club.SUCCESS).setDesc("设置成功，没有任何修改!!");
		}
		if (!isFreeze && model.isStatusTrue(EClubSettingStatus.CLUB_FREEZE)) {
			// club.freeze();
			// 告知在线玩家
			Utils.notifyUpdateRule(clubModel.getClub_id(), 0, club, EClubOperateCategory.CLUB_FREEZE);
		}

		if (isFreeze != model.isStatusTrue(EClubSettingStatus.CLUB_FREEZE)) {
			// 事件
			ClubJoinQuitMsgProto.Builder eventMsg = Utils
					.newEventMsg(club.getClubId(), model.isStatusTrue(EClubSettingStatus.CLUB_FREEZE) ? MsgType.FREEZE : MsgType.UNFREEZE, operator,
							null);
			club.joinQuitMsgQueueProto.offer(eventMsg.build());
			Utils.sendClubEventMsg(club.getManagerIds(), club.getClubId(), eventMsg.build());
			Utils.notifyRedHeart(club, ERedHeartCategory.CLUB_EVENT_NOTIFY);
		}

		// 隐藏桌子设置
		if (isConceal != model.isStatusTrue(EClubSettingStatus.CONCEAL_TABLE)) {
			club.ruleTables.forEach((id, ruleTb) -> {
				ruleTb.setConcealTable(model.isStatusTrue(EClubSettingStatus.CONCEAL_TABLE));
			});
			logger.warn("玩家[{}] 隐藏:[{}] 亲友圈[{}]!", operator.getAccount_id(), model.isStatusTrue(EClubSettingStatus.CONCEAL_TABLE), club.getClubId());
		}

		if (oldTireSwitch != club.isTireSwitchOpen()) {
			ClubScoreMsgLogModel logModel = club
					.statModifyClubTireSwitchRecord(club.getClubId(), operator, club.isTireSwitchOpen() ? 1 : 2, ClubTireLogType.TIRE_SWITCH);
			club.notifyModifyRecord(operator.getAccount_id(), logModel);
			ClubEventLog.event(new ClubEventLogModel(club.getClubId(), operator.getAccount_id(), EClubEventType.TIRE_SWITCH_U)
					.setVal1(club.isTireSwitchOpen() ? 1 : 2));
		}

		if (oldTireResetSwitch != club.isTireDailyReset()) {
			ClubScoreMsgLogModel logModel = club
					.statModifyClubTireSwitchRecord(club.getClubId(), operator, club.isTireDailyReset() ? 1 : 2, ClubTireLogType.TIRE_RESET_SWITCH);
			club.notifyModifyRecord(operator.getAccount_id(), logModel);
			ClubEventLog.event(new ClubEventLogModel(club.getClubId(), operator.getAccount_id(), EClubEventType.TIRE_RESET_SWITCH_U)
					.setVal1(club.isTireDailyReset() ? 1 : 2));
		}

		Utils.notityClubSetsUpdate(club);

		ClubEventLog.event(new ClubEventLogModel(club.getClubId(), operator.getAccount_id(), EClubEventType.SETS_U).setVal1(oldSets)
				.setVal2(model.getStatus()));

		if (oldRuleTableMode != club.isMultiClubRuleTableMode()) {
			clubRuleTableModeSwift(club);
		}

		return new ClubRoomModel(Club.SUCCESS).setDesc("设置成功!");
	}

	/**
	 * 亲友圈包间模式切换
	 */
	private void clubRuleTableModeSwift(Club club) {
		// 通知俱乐部成员包间模式已切换
		ClubMsgProto.ClubEventProto.Builder eventBuilder = ClubMsgProto.ClubEventProto.newBuilder();
		eventBuilder.setClubId(club.getClubId());
		eventBuilder.setEventCode(ClubEventCode.RULE_TABLE_SWITCH_MODE);
		Utils.sendClubAllMembers(eventBuilder, S2CCmd.CLUB_EVENT_RSP, club, false);
		// 踢出未开局的牌桌内玩家
		ProxyClubRq.Builder proxyClubRq = ProxyClubRq.newBuilder().setClientSessionId(club.getOwnerId());
		ClubRequest.Builder builder = ClubRequest.newBuilder().setClubId(club.getClubId());
		club.ruleTables.forEach((ruleId, ruleTable) -> {
			ruleTable.getTables().forEach((table) -> {
				if (!table.isGameStart()) {
					table.playerIds().forEach((playerId) -> {
						builder.setType(ClubRequest.ClubRequestType.CLUB_KICK_PLAYER);
						builder.setAccountId(playerId);
						builder.setJoinId(table.sceneTag().getJoinId());
						builder.setClubRuleId(ruleId);
						proxyClubRq.setClubRq(builder);
						clubKickPlayer(builder.build(), proxyClubRq.build(), ClubTableKickOutType.RULE_TABLE_MODE_SWIFT);
					});
				}
			});
		});

	}

	/**
	 * 玩法/规则设置
	 */
	private void updateRuleSets(final ClubRuleProto ruleProto, final Club club) {
		ClubRuleModel ruleModel = club.clubModel.getRule(ruleProto.getId());
		updateRuleSets(ruleProto, ruleModel, club.getClubId());
	}

	private void initRuleSets(ClubRuleProto ruleProto, ClubRuleModel ruleModel, int clubId) {
		List<CommonII> sets = ruleProto.getSetStatusList();
		final StatusModule model = ruleModel.getSetsModel();
		if (model == null) {
			return;
		}
		if (null != sets && !sets.isEmpty()) {
			sets.forEach((set) -> {
				ERuleSettingStatus eTypes = ERuleSettingStatus.of(set.getK());
				if (ERuleSettingStatus.NONE != eTypes) {
					if (set.getV() == 0)
						model.statusDel(eTypes);
					else
						model.statusAdd(eTypes);
				} else {
					logger.error("亲友圈[{},{}] 设置类型[{}]有误 @see ERuleSettingStatus!", clubId, ruleModel.getId(), set.getK());
				}
			});
		}
		ruleModel.setTireValue(ruleProto.getTireValue());
		ruleModel.setLimitRound(ruleProto.getLimitGameRound() <= 0 ? DEFAULT_LIMIT_ROUND : ruleProto.getLimitGameRound());
		ruleModel.setLotteryCost(ruleProto.getLotteryCost());
		ruleModel.setLimitWelfare(ruleProto.getLimitWelfare());
	}

	private void updateRuleSets(ClubRuleProto ruleProto, ClubRuleModel ruleModel, int clubId) {
		initRuleSets(ruleProto, ruleModel, clubId);
		Club club = clubs.get(clubId);
		if (club != null) {
			club.runInDBLoop(() -> {
				SpringService.getBean(ClubDaoService.class).getDao().updateClubRule(ruleModel);
			});
		}
	}

	/**
	 * 解散桌子
	 */
	public ClubRoomModel disbandTable(int clubId, int ruleId, int joinId, long reqAccountId) {
		Club club = ClubService.getInstance().getClub(clubId);
		if (null == club) {
			return new ClubRoomModel(Club.FAIL).setDesc("亲友圈不存在！");
		}

		ClubMemberModel operator = club.members.get(reqAccountId);
		if (null == operator || !EClubIdentity.isManager(operator.getIdentity())) {
			return new ClubRoomModel(Club.FAIL).setDesc("权限不够！");
		}

		synchronized (club) {
			ClubRuleTable clubRuleTable = club.ruleTables.get(ruleId);
			if (null == clubRuleTable) {
				return new ClubRoomModel(Club.FAIL).setDesc("不存在的游戏!");
			}

			int tableIndex = (joinId & 0xffff0000) >> 16;
			ClubTable table = clubRuleTable.getTable(tableIndex);
			if (null == table) {
				return new ClubRoomModel(Club.FAIL).setDesc("桌子不存在!");
			}

			if (!ClubCfg.get().isCanDelStartedRoom() && table.isGameStart()) {
				return new ClubRoomModel(Club.FAIL).setDesc("牌桌已经开始，不可以解散!");
			}

			// if (table.getPlayerSize() == 0) {
			// return new ClubRoomModel(Club.FAIL).setDesc("房间无人，不需要解散!");
			// }

			Map<Long, ClubPlayer> map = table.getPlayers();
			for (Long accountId : map.keySet()) {
				if (!PlayerService.getInstance().isPlayerOnline(accountId)) {
					ClubCacheService.getInstance().sit(accountId, ClubService.currentSeat);
				}
			}

			ClubTableStatusUpdateRsp.Builder builder = ClubTableStatusUpdateRsp.newBuilder();
			builder.setType(ERoomStatus.END.status());
			builder.setClubId(clubId);
			builder.setRuleId(ruleId);
			builder.setIndex(table.getIndex());
			builder.setRoomId(table.getRoomId());
			Utils.sendClubAllMembers(builder, S2CCmd.CLUB_TABLE_UPDATE, club);

			ClubEventLog.event(new ClubEventLogModel(clubId, reqAccountId, EClubEventType.DISBAND).setVal1(ruleId).setVal2(tableIndex)
					.setVal3(table.getRoomId()));

			if (ClubCfg.get().isDelRedisCache()) {
				table.delTableCache();
			}

			table.release();
		}

		return new ClubRoomModel(Club.SUCCESS).setDesc("解散成功！");
	}

	/**
	 * 规则详情
	 */
	public ClubRoomModel clubRuleDetail(ClubRequest request, ProxyClubRq topRequest) {
		int clubId = request.getClubId();
		Club club = ClubService.getInstance().getClub(clubId);

		if (null == club) {
			return new ClubRoomModel(Club.FAIL).setDesc("亲友圈不存在!");
		}

		if (!club.members.containsKey(topRequest.getClientSessionId())) {
			return new ClubRoomModel(Club.FAIL).setDesc("不是亲友圈成员!");
		}

		ClubUpdateProto clubProto = request.getClubUpdate();
		if (null == clubProto) {
			return new ClubRoomModel(Club.FAIL).setDesc("没有规则id !");
		}

		// 客户端不传ruleId，默认给全部，传则指定
		List<Integer> ruleIds = Lists.newArrayList();
		if (request.hasClubRuleId() || request.getClubRuleId() != 0) {
			ruleIds.add(request.getClubRuleId());
		} else {
			ruleIds.addAll(club.clubModel.getRules().keySet());
		}
		// clubProto.getClubRuleList().stream().map(ClubRuleProto::getId).collect(Collectors.toList());
		ClubProto.Builder clubProtoBuilder = club.encode(ruleIds, true);

		return new ClubRoomModel(Club.SUCCESS).setAttament(clubProtoBuilder);
	}

	/**
	 * 亲友圈聊天
	 */
	public ClubRoomModel clubChat(final ClubRequest request, ProxyClubRq topRequest) {
		int clubId = request.getClubId();
		Club club = ClubService.getInstance().getClub(clubId);
		if (null == club) {
			return new ClubRoomModel(Club.FAIL).setDesc("亲友圈不存在!");
		}

		ClubMemberModel member = club.members.get(topRequest.getClientSessionId());
		if (null == member) {
			return new ClubRoomModel(Club.FAIL).setDesc("不是亲友圈成员!");
		}
		// 黑名单
		if (EClubIdentity.isDefriend(member.getIdentity())) {
			return new ClubRoomModel(Club.FAIL).setDesc(Club.DEFRIEND_TIP);
		}

		String chat = request.getChatMsg().getChatMsg();
		if (StringUtils.isEmpty(chat)) {
			return new ClubRoomModel(Club.FAIL).setDesc("聊天信息不能为空！");
		}

		ClubChatMsg chatMsg = null;
		try {
			chatMsg = JSON.parseObject(chat, ClubChatMsg.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (null != chatMsg && EmojiFilter.containsEmoji(chatMsg.getContent())) {
			return new ClubRoomModel(Club.FAIL).setDesc("聊天信息含有敏感或特殊字符!");
		}

		club.chatWrap.appendChat(request.getChatMsg());
		ClubChatRsp.Builder builder = ClubChatRsp.newBuilder().setClubId(clubId).setClubName(club.getClubName());
		builder.setChatRsp(ChatMsgRsp.newBuilder().setChatMsg(chat));
		Utils.sendClubAllMembers(builder, S2CCmd.CLUB_CHAT_NOTIFY, club);
		if (request.getChatMsg().getChatType() == 2) {
			// 语音文件写入mongoDB
			MongoDBServiceImpl.getInstance().logVoiceMsg(request, chatMsg);
		}

		return new ClubRoomModel(Club.SUCCESS);
	}

	/**
	 * 在线成员
	 */
	public ClubRoomModel clubMemberStatus(final ClubRequest request, ProxyClubRq topRequest) {
		int clubId = request.getClubId();
		Club club = ClubService.getInstance().getClub(clubId);
		if (null == club) {
			return new ClubRoomModel(Club.FAIL).setDesc("亲友圈不存在!");
		}

		if (!club.members.containsKey(topRequest.getClientSessionId())) {
			return new ClubRoomModel(Club.FAIL).setDesc("不是亲友圈成员!");
		}

		ClubOnlineMemeberRsp.Builder builder = ClubOnlineMemeberRsp.newBuilder();
		builder.setClubId(clubId);
		synchronized (club) {
			final Date current = new Date();
			club.members.forEach((accountId, member) -> {
				CommonLI.Builder olBuilder = CommonLI.newBuilder().setK(accountId);
				if (PlayerService.getInstance().isPlayerOnline(accountId)) {
					olBuilder.setV(1);
				} else {
					Date lastDate = member.getLastLoginDate();
					if (member.getLastLoginDate() == null) {
						lastDate = current;
					}
					olBuilder.setV((int) (lastDate.getTime() / 1000L));
				}
				builder.addMemberOnlineStatus(olBuilder);
			});
		}

		return new ClubRoomModel(Club.SUCCESS).setAttament(builder);
	}

	/**
	 * 成员备注
	 */
	public ClubRoomModel clubMemberRemark(final ClubRequest request, ProxyClubRq topRequest) {
		int clubId = request.getClubId();
		Club club = ClubService.getInstance().getClub(clubId);
		if (null == club) {
			return new ClubRoomModel(Club.FAIL).setDesc("亲友圈不存在!");
		}

		ClubMemberModel operator = club.members.get(topRequest.getClientSessionId());

		if (null == operator) {
			return new ClubRoomModel(Club.FAIL).setDesc("不是亲友圈成员!");
		}

		if (!EClubIdentity.isManager(operator.getIdentity())) {
			return new ClubRoomModel(Club.FAIL).setDesc("不是管理员，权限不足!");
		}

		ClubMemberRemarkProto remarkProto = request.getRemark();
		String newRemark = remarkProto.getRemark(), newRemarkExt = remarkProto.getRemarkExt();

		if (EmojiFilter.containsEmoji(newRemark + newRemarkExt)) {
			return new ClubRoomModel(Club.FAIL).setDesc("备注名称包含敏感字符!");
		}

		ClubMemberModel member = club.members.get(remarkProto.getAccountId());
		if (null == member) {
			return new ClubRoomModel(Club.FAIL).setDesc("找不到成员!");
		}

		synchronized (club) {
			int r = SpringService.getBean(ClubDaoService.class).getDao()
					.updateClubAccountRemark(newRemark, newRemarkExt, remarkProto.getAccountId(), clubId);

			if (r > 0) {
				member.setRemark_ext(newRemarkExt);
				member.setRemark(newRemark);
			}
		}

		return new ClubRoomModel(Club.SUCCESS).setAttament(remarkProto.toBuilder()).setDesc("备注成功!");
	}

	/**
	 * 亲友圈公告设置
	 */
	public ClubRoomModel clubNoticeSets(final int clubId, long reqAccountId, ClubNoticeProto noticeProto) {
		Club club = ClubService.getInstance().getClub(clubId);
		if (null == club) {
			return new ClubRoomModel(Club.FAIL).setDesc("亲友圈不存在!");
		}

		ClubMemberModel operator = club.members.get(reqAccountId);
		if (null == operator || !EClubIdentity.isManager(operator.getIdentity())) {
			return new ClubRoomModel(Club.FAIL).setDesc("不是管理员，权限不足!");
		}

		String noticeText = noticeProto.getText();
		if (null == noticeText) {
			noticeText = "";
		}

		if (DirtyWordDict.getInstance().checkDirtyWord(noticeText) || EmojiFilter.containsEmoji(noticeText)) {
			return new ClubRoomModel(Club.FAIL).setDesc("公告包含敏感字符!");
		}
		synchronized (club) {
			club.clubModel.setNotice(noticeText);
		}

		Utils.sendClubAllMembers(noticeProto.toBuilder().setClubId(clubId), S2CCmd.CLUB_NOTICE_NOTIFY, club);
		return new ClubRoomModel(Club.SUCCESS).setDesc("设置成功!");
	}

	public ClubRoomModel clubReqRuleTables(final ClubRequest request, ProxyClubRq topRequest) {
		int clubId = request.getClubId();
		Club club = ClubService.getInstance().getClub(clubId);
		if (null == club) {
			return new ClubRoomModel(Club.FAIL).setDesc("亲友圈不存在!");
		}

		EClubIdentity identify = club.getIdentify(topRequest.getClientSessionId());
		if (null == identify) {
			return new ClubRoomModel(Club.FAIL).setDesc("不是亲友圈成员!");
		}
		int ruleId = request.getClubRuleId();
		final ClubRuleTable ruleTables = club.ruleTables.get(ruleId);
		if (null == ruleTables) {
			return new ClubRoomModel(Club.FAIL).setDesc("包间不存在，可能已经被管理员删除!");
		}

		ClubRuleTableGroupProto.Builder builder = null;
		// 不是管理员过滤已经打了的桌子
		if (club.hasSetting(EClubSettingStatus.CONCEAL_TABLE) && !EClubIdentity.isManager(identify.identify())) {
			builder = ruleTables.toNOTSTARTTablesBuilder(clubId);
		} else {
			builder = ruleTables.toTablesBuilder(clubId);
		}
		// send to client
		return new ClubRoomModel(Club.SUCCESS).setAttament(builder);
	}

	/**
	 * 亲友圈聊天记录
	 */
	public ClubRoomModel clubChatHistory(final ClubRequest request, ProxyClubRq topRequest) {
		int clubId = request.getClubId();
		Club club = ClubService.getInstance().getClub(clubId);
		if (null == club) {
			return new ClubRoomModel(Club.FAIL).setDesc("亲友圈不存在!");
		}
		if (!club.members.containsKey(topRequest.getClientSessionId())) {
			return new ClubRoomModel(Club.FAIL).setDesc("不是亲友圈成员!");
		}

		ClubChatHistory.Builder builder = ClubChatHistory.newBuilder().setClubId(clubId);
		club.initChatUniqueId();
		builder.addAllChatHistory(club.chatWrap.serializeToBuilder(request.getChatUniqueId()));

		return new ClubRoomModel(Club.SUCCESS).setAttament(builder);
	}

	/**
	 * 亲友圈快速加入
	 */
	public ClubRoomModel clubFastJoin(final ClubRequest request, ProxyClubRq topRequest, C2SSession session) {
		int clubId = request.getClubId();
		Club club = ClubService.getInstance().getClub(clubId);
		if (null == club) {
			return new ClubRoomModel(Club.FAIL).setDesc("亲友圈不存在!");
		}
		if (!club.members.containsKey(topRequest.getClientSessionId())) {
			return new ClubRoomModel(Club.FAIL).setDesc("不是亲友圈成员!");
		}

		ClubRuleTable ruleTable = club.ruleTables.get(request.getClubRuleId());
		if (null == ruleTable) {
			return new ClubRoomModel(Club.FAIL).setDesc("不存在该游戏!");
		}
		// 有少人模式，限制匹配
		ClubRuleModel roomRule = club.clubModel.getRule(ruleTable.getRuleId());
		if (null == roomRule) {
			return new ClubRoomModel(Club.FAIL).setDesc("包间不存在!");
		}

		int allowLessModel = roomRule.getRuleParams().getRuleValue(GameConstants.GAME_RULE_CAN_LESS);
		if (allowLessModel == 1) {
			return new ClubRoomModel(Club.FAIL).setDesc("此包间不允许快速组局，您可以选择桌子参加牌局!");
		}

		synchronized (club) {
			String accountIp = topRequest.getAccountIp();
			Optional<ClubTable> opt = ruleTable.optimalTable(accountIp, club.hasSetting(EClubSettingStatus.CONCEAL_TABLE));
			if (opt.isPresent()) {
				int joinId = (opt.get().getIndex() << 16) & 0xffff0000;
				return createClubRoom(request.toBuilder().setJoinId(joinId).build(), topRequest, session);
			} else {
				return new ClubRoomModel(Club.FAIL).setDesc("包间桌子已满或需要密码，请联系管理员！");
			}
		}
	}

	/**
	 * 包间备注
	 */
	public ClubRoomModel clubRuleRemark(final ClubRequest request, ProxyClubRq topRequest) {
		int clubId = request.getClubId();
		Club club = ClubService.getInstance().getClub(clubId);
		if (null == club) {
			return new ClubRoomModel(Club.FAIL).setDesc("亲友圈不存在!");
		}

		ClubMemberModel operator = club.members.get(topRequest.getClientSessionId());
		if (null == operator || !EClubIdentity.isManager(operator.getIdentity())) {
			return new ClubRoomModel(Club.FAIL).setDesc("不是管理员，权限不足!");
		}

		int ruleId = request.getClubRuleId();
		ClubRuleModel ruleModel = club.clubModel.getRule(ruleId);
		if (null == ruleModel) {
			return new ClubRoomModel(Club.FAIL).setDesc("不存在包间!");
		}

		ClubRuleRemarkProto remarkProto = request.getRuleRemark();
		final String newRemark = remarkProto.getRemark();
		if (StringUtil.getLength(newRemark) > 14) {
			return new ClubRoomModel(Club.FAIL).setDesc("备注长度不合法!");
		}

		if (DirtyWordDict.getInstance().checkDirtyWord(newRemark) || EmojiFilter.containsEmoji(newRemark)) {
			return new ClubRoomModel(Club.FAIL).setDesc("备注名称包含敏感字符!");
		}
		ruleModel.setRemark(newRemark);
		SpringService.getBean(ClubDaoService.class).getDao().updateClubRule(ruleModel);
		return new ClubRoomModel(Club.SUCCESS).setAttament(remarkProto.toBuilder().setClubId(clubId).setRuleId(ruleId)).setDesc("包间备注成功!");
	}

	/**
	 * 亲友圈添加成员
	 */
	public ClubRoomModel clubAddMember(final ClubRequest request, ProxyClubRq topRequest) {
		int clubId = request.getClubId();
		Club club = ClubService.getInstance().getClub(clubId);
		if (null == club) {
			return new ClubRoomModel(Club.FAIL).setDesc("亲友圈不存在!");
		}

		ClubMemberModel member = club.members.get(topRequest.getClientSessionId());
		if (null == member || !EClubIdentity.isManager(member.getIdentity())) {
			return new ClubRoomModel(Club.FAIL).setDesc("不是创建者，权限不足!");
		}

		long targetId = request.getAccountId();
		ClubMemberModel targetMember = club.members.get(targetId);
		if (null != targetMember) {
			return new ClubRoomModel(Club.FAIL).setDesc("玩家已经在亲友圈，不需要添加!");
		}

		// 通过RMI从中心服获得玩家数据
		PlayerViewVO vo = SpringService.getBean(ICenterRMIServer.class).getPlayerViewVo(request.getAccountId());
		if (null == vo) {
			return new ClubRoomModel(Club.FAIL).setDesc("玩家不存在，请确认是否输入正确 !");
		}

		if (getMyEnterClubCount(targetId) >= ClubCfg.get().getOwnerClubMax()) {
			return new ClubRoomModel(Club.FAIL).setDesc("玩家加入的亲友圈数量已达上限!");
		}

		// 亲友圈人数上限
		if (club.getMemberCount() >= ClubCfg.get().getClubMemberMax()) {
			return new ClubRoomModel(Club.FAIL).setDesc("亲友圈人数已达上限!");
		}

		ClubMemberModel newMember = new ClubMemberModel();
		newMember.setAccount_id(vo.getAccountId());
		newMember.setAvatar(vo.getHead());
		newMember.setNickname(vo.getNickName());
		newMember.setClubName(club.clubModel.getClub_name());
		newMember.setDate(new Date());
		newMember.setClub_id(clubId);

		synchronized (club) {
			if (null != club.members.putIfAbsent(targetId, newMember)) {
				return new ClubRoomModel(Club.FAIL).setDesc("其它管理员已经同意！");
			}

			ClubAccountModel requestClubAccount = new ClubAccountModel();
			requestClubAccount.setAccount_id(targetId);
			requestClubAccount.setClub_id(club.clubModel.getClub_id());

			try {
				SpringService.getBean(ClubDaoService.class).getDao().insertClubAccount(requestClubAccount);

				// 如果这个玩家在申请列表里，移出申请
				club.requestMembers.remove(request.getAccountId());
				ClubCacheService.getInstance().addMemberClubId(targetId, clubId);
				ClubDataLogManager.addNewJoinPlayer(targetId);
			} catch (Exception e) {
				logger.error("ADD MEMBER error", e);
				club.members.remove(targetId);
				return new ClubRoomModel(Club.FAIL).setDesc("系统异常，请联系客服!");
			}
		}

		// 事件
		ClubJoinQuitMsgProto.Builder eventMsg = Utils.newEventMsg(clubId, MsgType.JOIN, null, newMember);
		club.joinQuitMsgQueueProto.offer(eventMsg.build());
		Utils.sendClubEventMsg(club.getManagerIds(), clubId, eventMsg.build());

		Utils.sendJoinMemberInfo(club.getManagerIds(), clubId, newMember);
		return new ClubRoomModel(Club.SUCCESS).setDesc("操作成功！");
	}

	/**
	 * 设置管理员
	 */
	public ClubRoomModel clubSetManager(final ClubRequest request, ProxyClubRq topRequest) {
		int clubId = request.getClubId();
		Club club = ClubService.getInstance().getClub(clubId);
		if (null == club) {
			return new ClubRoomModel(Club.FAIL).setDesc("亲友圈不存在!");
		}
		ClubMemberModel member = club.members.get(topRequest.getClientSessionId());
		if (null == member || !EClubIdentity.isCreator(member.getIdentity())) {
			return new ClubRoomModel(Club.FAIL).setDesc("不是创建者，权限不足!");
		}

		if (request.getAccountId() == topRequest.getClientSessionId()) {
			return new ClubRoomModel(Club.FAIL).setDesc("创建者不能设置自己为管理员!");
		}

		ClubMemberModel targetMember = club.members.get(request.getAccountId());
		if (null == targetMember) {
			return new ClubRoomModel(Club.FAIL).setDesc("目标玩家不存在!");
		}

		if (request.getStatus() != 1) {
			if (targetMember.isPartner()) {
				return new ClubRoomModel(Club.FAIL).setDesc("该玩家是亲友圈合伙人，不能做此操作!");
			}
		}

		Set<Long> notifyIds = Sets.newHashSet(club.getManagerIds());
		synchronized (club) {
			if (request.getStatus() == 1 && club.getManagerCount() >= ClubCfg.get().getManagerMax()) {
				return new ClubRoomModel(Club.FAIL).setDesc("管理人数已达上限!");
			}
			if (request.getStatus() == 1) {
				targetMember.setIdentity(EClubIdentity.MANAGER.identify());
			} else {
				targetMember.setIdentity(EClubIdentity.COMMONER.identify());
			}

			SpringService.getBean(ClubDaoService.class).getDao().updateClubAccountIdentity(targetMember);
		}

		notifyIds.add(targetMember.getAccount_id());
		// 提玩家推送
		Utils.notityIdentityUpdate(notifyIds, request.getAccountId(), club.getClubId(), targetMember.getIdentity());

		// 事件
		if (request.getStatus() == 1) {
			ClubJoinQuitMsgProto.Builder eventMsg = Utils.newEventMsg(clubId, MsgType.SET_MANAGER, member, targetMember);
			club.joinQuitMsgQueueProto.offer(eventMsg.build());
			Utils.sendClubEventMsg(club.getManagerIds(), clubId, eventMsg.build());
		}

		return new ClubRoomModel(Club.SUCCESS).setAttament(ClubCommonLIIProto.newBuilder().setClubId(clubId)
				.setCommon(CommonLII.newBuilder().setK(request.getAccountId()).setV1(1).setV2(targetMember.getIdentity())));
	}

	/**
	 * 拉黑
	 */
	public ClubRoomModel clubDeFriend(final ClubRequest request, ProxyClubRq topRequest) {
		int clubId = request.getClubId();
		Club club = ClubService.getInstance().getClub(clubId);
		if (null == club) {
			return new ClubRoomModel(Club.FAIL).setDesc("亲友圈不存在!");
		}

		ClubMemberModel member = club.members.get(topRequest.getClientSessionId());
		if (null == member || !EClubIdentity.isManager(member.getIdentity())) {
			return new ClubRoomModel(Club.FAIL).setDesc("不是管理员，权限不足!");
		}

		ClubMemberModel targetMember = club.members.get(request.getAccountId());
		if (null == targetMember) {
			return new ClubRoomModel(Club.FAIL).setDesc("该玩家不是亲友圈成员!");
		}

		if (targetMember.getIdentity() >= member.getIdentity()) {
			return new ClubRoomModel(Club.FAIL).setDesc("权限不够，不能做此操作!");
		}

		if (targetMember.isPartner()) {
			return new ClubRoomModel(Club.FAIL).setDesc("该玩家是亲友圈合伙人，不能做此操作!");
		}

		Set<Long> notifyIds = Sets.newHashSet(club.getManagerIds());
		synchronized (club) {
			if (request.getStatus() == 1) {
				targetMember.setIdentity(EClubIdentity.DEFRIEND.identify());
			} else {
				targetMember.setIdentity(EClubIdentity.COMMONER.identify());
			}
			List<ClubMemberModel> list = new ArrayList<>();
			list.add(targetMember);
			club.runInDBLoop(new ClubMemberUpdateIdentityDBTask(list));
			//			SpringService.getBean(ClubDaoService.class).getDao().updateClubAccountIdentity(targetMember);
		}
		notifyIds.add(targetMember.getAccount_id());
		// 提玩家推送
		Utils.notityIdentityUpdate(notifyIds, request.getAccountId(), club.getClubId(), targetMember.getIdentity());

		// 事件
		ClubJoinQuitMsgProto.Builder eventMsg = Utils
				.newEventMsg(clubId, request.getStatus() == 1 ? MsgType.SET_DEFRIEND : MsgType.CANCEL_DEFRIEND, member, targetMember);
		club.joinQuitMsgQueueProto.offer(eventMsg.build());
		Utils.sendClubEventMsg(club.getManagerIds(), clubId, eventMsg.build());

		return new ClubRoomModel(Club.SUCCESS).setAttament(ClubCommonLIIProto.newBuilder().setClubId(clubId)
				.setCommon(CommonLII.newBuilder().setK(request.getAccountId()).setV1(1).setV2(targetMember.getIdentity())));
	}

	/**
	 * 亲友圈成员列表
	 */
	public ClubRoomModel clubMemberList(final ClubRequest request, ProxyClubRq topRequest) {
		int clubId = request.getClubId();
		Club club = ClubService.getInstance().getClub(clubId);
		if (null == club) {
			return new ClubRoomModel(Club.FAIL).setDesc("亲友圈不存在!");
		}

		EClubIdentity identify = club.getIdentify(topRequest.getClientSessionId());
		if (null == identify) {
			return new ClubRoomModel(Club.FAIL).setDesc("不是亲友圈成员，不可以操作!");
		}

		// 对普通成员隐藏成语列表
		if (club.setsModel.isStatusTrue(EClubSettingStatus.CONCEAL_MEMBER) && !EClubIdentity.isManager(identify.identify())) {
			return new ClubRoomModel(Club.FAIL, null, -1).setDesc("成员列表对普通成员隐藏，请联系管理员!");
		}

		return new ClubRoomModel(Club.SUCCESS)
				.setAttament(ClubMemberListProto.newBuilder().setClubId(clubId).addAllMemebers(club.encodeMemberProto()));
	}

	/**
	 * 踢出桌子
	 */
	public ClubRoomModel clubKickPlayer(final ClubRequest request, ProxyClubRq topRequest, int kickType) {
		int clubId = request.getClubId();
		Club club = ClubService.getInstance().getClub(clubId);
		if (null == club) {
			return new ClubRoomModel(Club.FAIL).setDesc("亲友圈不存在!");
		}

		ClubMemberModel member = club.members.get(topRequest.getClientSessionId());
		if (null == member || !EClubIdentity.isManager(member.getIdentity())) {
			return new ClubRoomModel(Club.FAIL).setDesc("没有权限!");
		}

		ClubRuleTable ruleTalbe = club.ruleTables.get(request.getClubRuleId());
		if (null == ruleTalbe) {
			return new ClubRoomModel(Club.FAIL).setDesc("操作的包间不存在!");
		}

		int tableIndex = (request.getJoinId() & 0xffff0000) >> 16;
		ClubTable table = ruleTalbe.getTable(tableIndex);
		if (null == table) {
			return new ClubRoomModel(Club.FAIL).setDesc("操作的桌子不存在!");
		}

		if (table.isGameStart()) {
			return new ClubRoomModel(Club.FAIL).setDesc("牌局已经开始，不可以踢人!");
		}

		final long accountId = request.getAccountId();
		ClubPlayer targetPlayer = table.playerExit(accountId);
		if (null == targetPlayer) {
			return new ClubRoomModel(Club.FAIL).setDesc("玩家已经离开!");
		}

		if (!PlayerService.getInstance().isPlayerOnline(accountId)) {
			ClubCacheService.getInstance().sit(accountId, ClubService.currentSeat);
		}

		ClubEventLog.event(new ClubEventLogModel(clubId, topRequest.getClientSessionId(), EClubEventType.KICK).setTargetId(accountId)
				.setVal1(ruleTalbe.getRuleId()).setVal2(tableIndex).setVal3(kickType));

		// 踢人逻辑，告诉相应的逻辑服
		ClubRoomStatusProto.Builder builder = ClubRoomStatusProto.newBuilder();
		builder.setType(ERoomStatus.KICK_PLAYER.status());
		builder.setPlayer(targetPlayer.toInteresPbBuilder(ClubPlayer.OP_JOINID));
		builder.setRoomId(table.getRoomId());
		SessionService.getInstance().sendMsg(EServerType.LOGIC, table.getLogicIndex(), PBUtil.toS2SResponse(S2SCmd.C_2_LOGIC_ROOM_STATUS, builder));

		return new ClubRoomModel(Club.SUCCESS).setDesc(String.format("成功将玩家[%s]踢出桌子!", targetPlayer.getUserName()));
	}

	/**
	 * 亲友圈简单消息
	 */
	public ClubSimple.Builder encodeSimpleClubs(long accountId, boolean selfCreate) {
		Collection<Club> clubs = selfCreate ? getMyCreateClub(accountId) : getMyEnterClub(accountId);

		ClubSimple.Builder builder = ClubSimple.newBuilder();
		clubs.forEach((club) -> {
			try {
				builder.addClubs(club.encodeMini());
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		return builder;
	}

	/**
	 * 配置有更新
	 */
	public void clubCfgReload() {
		clubs.forEach((cluId, club) -> {
			club.runInReqLoop(() -> {
				club.ruleTables.forEach((ruleId, ruleTable) -> {
					ruleTable.reloadTable();
				});
			});
		});
	}

	/**
	 * 更新亲友圈id
	 */
	public synchronized UpdateClubIdVo updateClubId(UpdateClubIdVo vo) {

		final int newClubId = vo.getNewClubId();
		final int oldClubId = vo.getOldClubId();

		if (newClubId == oldClubId) {
			return vo.setStatus(XYCode.FAIL).setDesc("你没有做任何修改!!");
		}
		if (newClubId > 9999999 || newClubId < 1000000) {
			return vo.setStatus(XYCode.FAIL).setDesc("新亲友圈id位数不合理!!");
		}
		if (clubs.containsKey(newClubId)) {
			return vo.setStatus(XYCode.FAIL).setDesc("亲友圈id已经存在！");
		}

		if (!clubs.containsKey(oldClubId)) {
			return vo.setStatus(XYCode.FAIL).setDesc("要修改的亲友圈不存在！");
		}

		/**
		 * 操作
		 */
		IntBinaryOperator updateOperatorFunc = (int old_id, int new_id) -> {

			Club club = clubs.remove(old_id);
			clubs.put(new_id, club);
			StringBuilder memberIds = new StringBuilder(club.getMemberCount() * 8);

			List<Map<String, Object>> params = Lists.newArrayList();
			club.members.forEach((accountId, model) -> {
				model.setClub_id(new_id);

				Map<String, Object> map = Maps.newHashMap();
				map.put("new_club_id", new_id);
				map.put("old_club_id", old_id);
				map.put("account_id", accountId);
				params.add(map);

				memberIds.append(accountId).append(',');
			});
			club.clubModel.setClub_id(new_id);
			for (ClubRuleModel rule : club.clubModel.getRules().values()) {
				rule.setClub_id(new_id);
			}
			SpringService.getBean(ClubDaoService.class).getDao().updateClubId(new_id, old_id);
			SpringService.getBean(ClubDaoService.class).getDao().batchUpdate("updateClubAccountClubId", params);

			MongoDBServiceImpl.getInstance().updateClubIdLog(club.getOwnerId(), old_id, new_id, memberIds.toString());
			return 1;
		};

		try {
			updateOperatorFunc.applyAsInt(oldClubId, newClubId);
		} catch (Exception e) {
			// 回滚
			updateOperatorFunc.applyAsInt(newClubId, oldClubId);
			return vo.setStatus(XYCode.FAIL).setDesc("数据库操作失败:" + e.getMessage());
		}

		return vo.setStatus(XYCode.SUCCESS).setDesc("修改成功！");
	}

	private void reqAccountWealthSyncTask(long clubOwenerId, long reqAccountId, EWealthCategory category) {
		// schWealthService.execute(AccountWealthSyncTask.newTask(Pair.of(clubOwenerId,
		// category), Arrays.asList(reqAccountId)));
	}

	public Map<Long, ClubBulletinWrap> getSharedbulletins() {
		return sharedBulletins;
	}

	public ClubRoomModel requestQuitClub(int clubId, long accountId, String avatar, String content, String nickname) {
		Club club = clubs.get(clubId);
		if (club == null) {
			return new ClubRoomModel(Club.FAIL).setDesc("找不到该亲友圈!");
		}
		return club.requestQuitClub(accountId, avatar, content, nickname);
	}

	/**
	 * 下架子游戏
	 */
	public synchronized void delClubGameTypeIndex(Set<Integer> gameIds) {
		clubs.forEach((id, club) -> {
			club.runInReqLoop(() -> {
				try {
					Map<Integer, ClubRuleModel> ruleMap = club.clubModel.getRules();
					if (null == ruleMap) {
						return;
					}
					// 操作的id
					List<Integer> ruleIds = null;
					Iterator<Map.Entry<Integer, ClubRuleModel>> it = ruleMap.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry<Integer, ClubRuleModel> entry = it.next();

						if (gameIds.contains(entry.getValue().getGame_id())) {

							it.remove();
							club.removeRule(entry.getKey());

							if (null == ruleIds) {
								ruleIds = Lists.newArrayList();
							}
							ruleIds.add(entry.getKey());

							logger.warn("俱乐部[{}] 游戏[ {} ],子游戏[ {} ], 包间[ {} ] 下架!", club.getClubId(), entry.getValue().getGame_id(),
									entry.getValue().getGame_type_index(), entry.getValue().getId());
						}
					}

					if (null != ruleIds && !ruleIds.isEmpty()) {
						club.runInDBLoop(new OfflineRuleDBTask(ruleIds));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		});
	}

	/**
	 * 聊天、公告、跑马灯 开关状态
	 */
	public void notifyBanSwitch() {
		ClubBanSwitchResponse.Builder builder = ClubBanSwitchResponse.newBuilder();
		builder.setCloseChat(ClubCfg.get().isBanChat());
		builder.setCloseBulletin(ClubCfg.get().isBanBulletin());
		builder.setCloseMarquee(ClubCfg.get().isBanMarquee());

		clubs.forEach((id, club) -> {
			Utils.sendClubAllMembers(builder, S2CCmd.CLUB_BAN_SWITCH_RSP, club);
		});
	}

	/**
	 * 变成围观者
	 */
	public void becomeObserver(int clubId, Collection<Long> accountIds, boolean enter) {
		Club club = getClub(clubId);
		if (null == club || null == accountIds) {
			return;
		}
		logger.warn("{}亲友圈[ {} ]管理员视角，玩家id[ {} ]", enter ? "设置" : "取消", clubId, accountIds);

		club.runInReqLoop(() -> accountIds.forEach(accountId -> {
			ClubMemberModel member = club.members.get(accountId);
			if (enter) {
				if (null == member) {
					// 临时存储于内存中，不可落地
					member = new ClubMemberModel();
					member.setClub_id(clubId);
					member.setAccount_id(accountId);
					member.setIdentity(EClubIdentity.OBSERVER.identify());
					member.setNickname(Long.toString(accountId));
					member.setDate(new Date());
					club.members.put(accountId, member);
					ClubCacheService.getInstance().addMemberClubId(accountId, clubId);
				}
			} else {
				if (null != member && member.getIdentity() == EClubIdentity.OBSERVER.identify()) {
					club.members.remove(accountId);
					ClubCacheService.getInstance().rmMemberClubId(accountId, clubId);
				}
			}
		}));
	}
}