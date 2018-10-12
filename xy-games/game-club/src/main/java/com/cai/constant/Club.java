package com.cai.constant;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import com.alibaba.fastjson.JSON;
import com.cai.common.ClubRecordDataType;
import com.cai.common.ClubTireLogType;
import com.cai.common.constant.AccountConstant;
import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.define.EClubEventType;
import com.cai.common.define.EClubSettingStatus;
import com.cai.common.define.ERedHeartCategory;
import com.cai.common.define.ESysMsgType;
import com.cai.common.define.EWealthCategory;
import com.cai.common.domain.ClubAccountModel;
import com.cai.common.domain.ClubBanPlayerModel;
import com.cai.common.domain.ClubBulletinModel;
import com.cai.common.domain.ClubDailyCostModel;
import com.cai.common.domain.ClubDataModel;
import com.cai.common.domain.ClubEventLogModel;
import com.cai.common.domain.ClubMatchModel;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.domain.ClubMemberRecordModel;
import com.cai.common.domain.ClubModel;
import com.cai.common.domain.ClubRoomModel;
import com.cai.common.domain.ClubRuleModel;
import com.cai.common.domain.ClubRuleRecordModel;
import com.cai.common.domain.GameGroups;
import com.cai.common.domain.StatusModule;
import com.cai.common.domain.SysGameType;
import com.cai.common.domain.log.ClubApplyLogModel;
import com.cai.common.domain.log.ClubScoreMsgLogModel;
import com.cai.common.type.ClubApplyType;
import com.cai.common.type.ClubRecordDayType;
import com.cai.common.util.FilterUtil;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.LimitQueue;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.MyStringUtil;
import com.cai.common.util.PBUtil;
import com.cai.common.util.Pair;
import com.cai.common.util.RuntimeOpt;
import com.cai.common.util.SpringService;
import com.cai.common.util.TimeUtil;
import com.cai.config.ClubCfg;
import com.cai.constant.ClubMatchWrap.ClubMatchStatus;
import com.cai.dictionary.GameGroupRuleDict;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.manager.ClubDataLogManager;
import com.cai.service.ClubCacheService;
import com.cai.service.ClubDaoService;
import com.cai.service.ClubService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerService;
import com.cai.service.SessionService;
import com.cai.tasks.CheckPlayerStatusTask;
import com.cai.tasks.ClubWelfareLotteryTask;
import com.cai.tasks.db.ClubMemberRecordCreateDBTask;
import com.cai.tasks.db.ClubMemberRecordDelDBTask;
import com.cai.tasks.db.ClubRecordDBTask;
import com.cai.tasks.db.ClubRuleRecordDelDBTask;
import com.cai.utils.ClubEventLog;
import com.cai.utils.ClubRoomUtil;
import com.cai.utils.Utils;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.protobuf.InvalidProtocolBufferException;
import com.xianyi.framework.core.concurrent.DefaultWorkerLoopGroup;
import com.xianyi.framework.core.concurrent.WorkerLoop;
import com.xianyi.framework.core.concurrent.WorkerLoopGroup;
import com.xianyi.framework.core.concurrent.disruptor.TaskDispatcher;
import com.xianyi.framework.core.concurrent.selfDriver.AutoDriverQueue;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protobuf.clazz.ClubMsgProto;
import protobuf.clazz.ClubMsgProto.ClubAccountProto;
import protobuf.clazz.ClubMsgProto.ClubActivityListProto;
import protobuf.clazz.ClubMsgProto.ClubApplyExitProto;
import protobuf.clazz.ClubMsgProto.ClubApplyJoinProto;
import protobuf.clazz.ClubMsgProto.ClubBulletinProto;
import protobuf.clazz.ClubMsgProto.ClubDailyCostProto;
import protobuf.clazz.ClubMsgProto.ClubDailyMatchDataProto;
import protobuf.clazz.ClubMsgProto.ClubDataProto;
import protobuf.clazz.ClubMsgProto.ClubJoinQuitMsgProto;
import protobuf.clazz.ClubMsgProto.ClubMemberRecordBatchModifyResponse;
import protobuf.clazz.ClubMsgProto.ClubMemberRecordModifyProto;
import protobuf.clazz.ClubMsgProto.ClubMemberRecordModifyResponse;
import protobuf.clazz.ClubMsgProto.ClubModifyTireMsgProto;
import protobuf.clazz.ClubMsgProto.ClubModifyTireMsgResponse;
import protobuf.clazz.ClubMsgProto.ClubProto;
import protobuf.clazz.ClubMsgProto.ClubRedHeartRsp;
import protobuf.clazz.ClubMsgProto.ClubReqRsp;
import protobuf.clazz.ClubMsgProto.ClubRuleProto;
import protobuf.clazz.ClubMsgProto.ClubServerProto;
import protobuf.clazz.ClubMsgProto.ClubTablePlayerProto;
import protobuf.clazz.s2s.ClubServerProto.ClubGameRecordProto.ClubGameOverProto;
import protobuf.clazz.s2s.ClubServerProto.ClubRoomStatusProto;
import protobuf.clazz.s2s.S2SProto.ClubDisbandSendMailProto;
import protobuf.clazz.s2s.S2SProto.S2STransmitProto;

import static java.util.stream.Collectors.toList;

public final class Club {

	private static Logger logger = LoggerFactory.getLogger(Club.class);

	// 桌子状态同步放在这里处理
	private static final WorkerLoopGroup workGroup = DefaultWorkerLoopGroup.newGroup("club-status-worker", RuntimeOpt.availableProcessors() << 2);

	// 请求线程，处理请求任务
	private static final TaskDispatcher reqDispatcher = TaskDispatcher
			.newDispatcher(RuntimeOpt.availableProcessors() << 2, "club-task-dispatcher", 262144, RuntimeOpt.availableProcessors());
	private final AutoDriverQueue taskQueue = AutoDriverQueue.newQueue(reqDispatcher);

	// DB落地
	private static final TaskDispatcher dbDispatcher = TaskDispatcher
			.newDispatcher(RuntimeOpt.availableProcessors() << 1, "club-db-dispatcher", 131072, RuntimeOpt.availableProcessors());
	private final AutoDriverQueue dbQueue = AutoDriverQueue.newQueue(dbDispatcher);

	public final ClubModel clubModel;

	private ClubDataModel clubDataModel;

	public final Map<Long, ClubApplyJoinProto> requestMembers;

	public final LimitQueue<ClubApplyJoinProto> agreeRequestMembers;

	// 每日消耗的闲逸豆
	public List<ClubDailyCostProto> dailyCosts;

	// 新每日消耗
	public List<ClubDailyCostModel> dailyCostModels;

	// 战绩最后刷新时间 过12点数据清0
	private long nextRefreshTime;

	// 单日消耗闲逸豆
	public long costGold;

	// 单日消耗专属豆
	public long costExclusiveGold;

	// 单日对局数
	public int gameCount;

	// 当日消耗数据
	private ClubDailyCostModel dailyCostModel;

	public final Map<Long, ClubMemberModel> members; // 成员列表

	// 规则/玩法->房间列表
	public final ConcurrentMap<Integer, ClubRuleTable> ruleTables = Maps.newConcurrentMap();

	// 设置相关
	public final StatusModule setsModel;

	// 亲友圈消息
	public final LimitQueue<ClubJoinQuitMsgProto> joinQuitMsgQueueProto;

	// 上一次改名时间
	private Date lastChangeNameDate;

	// 亲友圈聊天
	public final ClubChatWrap chatWrap;

	public Set<String> groupSet;

	// 亲友圈工作线程-GAME-TODO考虑用于优化亲友圈
	private final WorkerLoop worker;

	// 亲友圈创始人财富[account:category]
	public final static Map<Pair<Long, EWealthCategory>, Long> wealth = Maps.newConcurrentMap();

	// 亲友圈活动
	public final Map<Long, ClubActivityWrap> activitys;

	// 亲友圈公告
	public final Map<Long, ClubBulletinWrap> bulletins;

	private final ClubSeat currentSeat;

	private ClubScoreMsgWrap clubScoreMsgWrap;

	public ClubMatchLogWrap clubMatchLogWrap;

	public final Map<Long, ClubApplyLogModel> requestQuitMembers = new ConcurrentHashMap<>();

	private AtomicLong chatAtomicId;

	private List<ClubMemberRecordModel> delMemberRecordList;

	// 比赛
	public Map<Long, ClubMatchWrap> matchs;

	// 删除的比赛
	public Map<Long, ClubMatchWrap> delMatchs = Maps.newConcurrentMap();

	// 自建赛数据
	private int dailyMatchCreateNum;
	private int dailyMatchEndNum;
	private long dailyMatchGold;
	private long dailyMatchExclusiveGold;
	private List<ClubDailyMatchDataProto> matchDailyDatas;

	//循环次数
	private int loopCounter;

	public ClubWelfareWrap clubWelfareWrap;

	public Club(ClubModel clubModel, ClubDataModel clubDataModel) {
		this(clubModel, Collections.emptyList(), new ClubScoreMsgWrap(clubModel.getClub_id()), clubDataModel);
	}

	public Club(ClubModel clubModel, List<ClubMemberModel> clubMembers, ClubScoreMsgWrap clubScoreMsgWrap, ClubDataModel clubDataModel) {
		worker = workGroup.next();
		requestMembers = new ConcurrentSkipListMap<>();
		agreeRequestMembers = new LimitQueue<>(30);
		joinQuitMsgQueueProto = new LimitQueue<>(100);
		members = new ConcurrentHashMap<>();
		this.clubModel = clubModel;
		this.clubDataModel = clubDataModel;
		nextRefreshTime = MyDateUtil.getTomorrowZeroDate(System.currentTimeMillis()).getTime();
		dailyCosts = new ArrayList<>();
		matchDailyDatas = new ArrayList<>();
		this.setsModel = StatusModule.newWithStatus(clubModel.getSettingStatus());
		this.chatWrap = new ClubChatWrap(clubModel.getClub_id(), clubModel.getChatMsgs());
		this.groupSet = new HashSet<>();
		activitys = Maps.newConcurrentMap();
		bulletins = Maps.newConcurrentMap();
		currentSeat = ClubSeat.newSeat(clubModel.getClub_id());
		dailyCostModels = Lists.newArrayList();
		this.clubScoreMsgWrap = clubScoreMsgWrap;
		this.matchs = Maps.newConcurrentMap();
		this.initialClubTables();
		this.clubWelfareWrap = new ClubWelfareWrap(this);

		if (clubModel.getDatas() != null && clubModel.getDatas().length > 0) {
			ClubServerProto proto;
			try {
				long ctime = System.currentTimeMillis();
				proto = ClubServerProto.parseFrom(clubModel.getDatas());
				nextRefreshTime = proto.getNextRefreshTime();
				costGold = proto.getDailyGameCost();
				costExclusiveGold = proto.getDailyGameExclusiveCost();
				gameCount = proto.getDailyGameCount();

				joinQuitMsgQueueProto.addAll(proto.getJoinQuitMsgProtoList());

				proto.getRequestMembersList().forEach((member) -> {
					if (member.getCreateTime() < ctime + 7 * DateUtils.MILLIS_PER_DAY) {
						requestMembers.put(member.getAccountId(), member);
					}
				});
				dailyCosts.addAll(proto.getDailysCostList());

				if (clubDataModel != null && clubDataModel.getDatas() != null && clubDataModel.getDatas().length > 0) {
					ClubDataProto clubDataProto = ClubDataProto.parseFrom(clubDataModel.getDatas());
					dailyMatchCreateNum = clubDataProto.getMatchCreateCount();
					dailyMatchEndNum = clubDataProto.getMatchEndCount();
					dailyMatchExclusiveGold = clubDataProto.getMatchExclusiveGold();
					dailyMatchGold = clubDataProto.getMatchGold();
					matchDailyDatas.addAll(clubDataProto.getMatchDailysList());
				}

				update(ctime);

			} catch (InvalidProtocolBufferException e) {
				logger.error(e.toString());
				e.printStackTrace();
			}
		}

		clubMembers.forEach((member) -> {

			String name = member.getNickname();
			if (name == null) {
				return;
			}
			if (name.indexOf("SELF_") != -1) {
				String name2 = name.split("SELF_")[1];
				name2 = MyStringUtil.substringByLength(name2, AccountConstant.NICK_NAME_LEN);
				member.setNickname(name2);
			}

			members.put(member.getAccount_id(), member);
		});
	}

	public void setClubMatchLogWrap(ClubMatchLogWrap clubMatchLogWrap) {
		this.clubMatchLogWrap = clubMatchLogWrap;
	}

	public String getGameDescs() {
		StringBuilder sb = new StringBuilder("玩法描述:");
		if (clubModel.getRules() == null || clubModel.getRules().size() == 0) {
			return sb.toString();
		}
		int i = 1;
		for (ClubRuleModel model : clubModel.getRules().values()) {
			String gameName = SysGameTypeDict.getInstance().getMJname(model.getGame_type_index());
			sb.append("\n玩法" + i + ": " + gameName + " " + model.getGameDesc(GameGroupRuleDict.getInstance().get(model.getGame_type_index())));
			i++;
		}
		return sb.toString();
	}

	/**
	 * 初始化现有规则/玩法房间
	 */
	private void initialClubTables() {
		this.clubModel.getRules().forEach((id, rule) -> {
			ruleTables.putIfAbsent(rule.getId(), new ClubRuleTable(rule, true));
		});
	}

	/**
	 * 落地前赋值
	 */
	public void encodeSave() {

		clubModel.getRules().forEach((id, rule) -> {
			rule.encodeRule();
		});

		ClubServerProto.Builder b = ClubServerProto.newBuilder();
		b.setDailyGameCost(costGold);
		b.setDailyGameExclusiveCost(costExclusiveGold);
		b.setDailyGameCount(gameCount);
		b.setNextRefreshTime(nextRefreshTime);
		b.addAllDailysCost(dailyCosts);
		b.addAllJoinQuitMsgProto(joinQuitMsgQueueProto);
		requestMembers.forEach((account_id, member) -> {
			b.addRequestMembers(member);
		});

		clubModel.setDatas(b.build().toByteArray());
		clubModel.setSettingStatus(setsModel.getStatus());
		clubModel.setChatMsgs(chatWrap.serializeToDB());

		// 自建赛数据
		if (clubDataModel != null) {
			ClubDataProto.Builder clubData = ClubDataProto.newBuilder();
			clubData.setMatchCreateCount(dailyMatchCreateNum);
			clubData.setMatchEndCount(dailyMatchEndNum);
			clubData.setMatchExclusiveGold(dailyMatchExclusiveGold);
			clubData.setMatchGold(dailyMatchGold);
			clubData.addAllMatchDailys(matchDailyDatas);
			clubDataModel.setDatas(clubData.build().toByteArray());
		}
	}

	/**
	 * 全部玩法详情
	 */
	public ClubProto.Builder encode(long requestAccountId, boolean isDetailRule) {
		EClubIdentity identity = getIdentify(requestAccountId);

		// 给管理员视角
		if (identity == EClubIdentity.OBSERVER) {
			identity = EClubIdentity.MANAGER;
		}
		ClubMemberModel memberModel = members.get(requestAccountId);
		return encode(clubModel.getRules().keySet(), isDetailRule).addAllSetStatus(Utils.toClubStatusBuilder(setsModel))
				.setReqAccountIdentity(identity.identify()).setIsPartner(memberModel != null && memberModel.isPartner());
	}

	/**
	 * 指定玩法规则详情
	 */
	public ClubProto.Builder encode(Collection<Integer> ruleIds, boolean isDetailRule) {
		ClubProto.Builder b = ClubProto.newBuilder();
		b.setClubId(clubModel.getClub_id());
		b.setAccountId(clubModel.getAccount_id());
		b.setClubName(clubModel.getClub_name());
		b.setClubCount(members.size());
		b.setDesc(clubModel.getDesc());
		b.setCreateAt(clubModel.getDate().getTime());
		String mOwnerName = "";
		String mAvatar = "";
		ClubMemberModel owner = getOwner();
		if (null != owner) {
			mAvatar = owner.getAvatar();
			mOwnerName = owner.getNickname();
		}
		b.setAvatar(mAvatar);
		b.setOwenerName(mOwnerName);
		if (null != clubModel.getNotice()) {
			b.setClubNotice(clubModel.getNotice());
		}
		b.setPlayingTableCount(this.getHasPlayerTableCount());

		ruleIds.forEach(ruleId -> {

			try {// 保护一下
				ClubRuleProto.Builder ruleB = ClubRuleProto.newBuilder();
				ruleB.setId(ruleId);
				ClubRuleModel rule = clubModel.getRule(ruleId);
				if (null == rule) {
					return;
				}
				ClubRuleTable ruleTable = ruleTables.get(ruleId);
				if (null == ruleTable) {
					return;
				}

				SysGameType gameType = SysGameTypeDict.getInstance().getSysGameType(rule.getGame_type_index());
				if (null == gameType) {
					logger.error("亲友圈玩法[{}]对应的配置找不到，请查看配置！", rule.getGame_type_index());
					return;
				}
				ruleB.setGameTypeIndex(rule.getGame_type_index());
				String appName = gameType.getAppName();
				if (StringUtils.isEmpty(appName)) {
					appName = gameType.getDesc();
				}
				ruleB.setGameName(appName);
				ruleB.setAppId(gameType.getGameID());
				String subGameName = SysGameTypeDict.getInstance().getMJname(rule.getGame_type_index());
				if (StringUtils.isNotEmpty(subGameName)) {
					ruleB.setSubName(subGameName);
				}
				if (!isDetailRule) {
					b.addClubRule(ruleB);
					return;
				}
				ruleB.setGameType(gameType.getGameBigType());

				if (Strings.isNullOrEmpty(ruleTable.getRuleDesc())) {
					GameGroups gameGroups = GameGroupRuleDict.getInstance().get(rule.getGame_type_index());
					if (null == gameGroups) {
						ruleB.setRuleDesc("无效游戏玩法");
					} else {
						ruleTable.setRuleDesc(GameDescUtil.getGameDesc(rule, gameGroups));
					}
				}
				ruleB.setRuleDesc(Strings.isNullOrEmpty(ruleTable.getRuleDesc()) ? "无效游戏玩法" : ruleTable.getRuleDesc());

				ruleB.setRules(rule.getRules());
				ruleB.setGameRound(rule.getGame_round());
				if (StringUtils.isNotEmpty(rule.getRemark())) {
					ruleB.setRemark(rule.getRemark());
				}
				ruleB.addAllSetStatus(Utils.toRuleStatusBuilder(rule.getSetsModel()));
				ruleB.setTireValue(rule.getTireValue());
				ruleB.setLimitGameRound(rule.getLimitRound());
				ruleB.setTableMaxPlayer(ruleTable.getPlayerLimit());
				ruleB.setShowType(rule.getShowType());
				ruleB.setLotteryCost(rule.getLotteryCost());
				ruleB.setLimitWelfare(rule.getLimitWelfare());
				b.addClubRule(ruleB);
			} catch (Exception e) {
				logger.error("亲友圈encode错误，clubId:{}", clubModel.getClub_id(), e);
			}
		});
		return b;
	}

	/**
	 * 亲友圈成员
	 */
	public List<ClubAccountProto> encodeMemberProto() {
		List<ClubAccountProto> r = Lists.newArrayListWithCapacity(members.size());
		members.forEach((account_id, member) -> {
			boolean online = false;
			if (PlayerService.getInstance().isPlayerOnline(account_id)) {
				online = true;
			}
			if (member.getIdentity() == EClubIdentity.OBSERVER.identify()) {
				return;
			}
			r.add(member.encode(online).build());
		});
		return r;
	}

	public void updateGameCount(int costGold, long createTime, int wealthCategory) {
		if (wealthCategory == (int) EWealthCategory.EXCLUSIVE_GOLD.category()) {
			clubModel.setExclusiveGold(clubModel.getExclusiveGold() + costGold);
		} else {
			clubModel.setConsumeGold(clubModel.getConsumeGold() + costGold);
		}

		clubModel.setGameCount(clubModel.getGameCount() + 1);

		// 检查是否可开启亲友圈福卡功能
		clubWelfareWrap.checkOpenClubWelfare();

		// 这是上一天的数据 判断 > 0 是防止有些逻辑服没更新，没传值过来
		if (createTime > 0 && nextRefreshTime - createTime * 1000 > TimeUtil.DAY) {
			// 逻辑服是秒数
			createTime = createTime * 1000;
			ClubDailyCostProto.Builder b = null;

			if (dailyCosts.size() <= 0) {
				// 如果前一天没有记录数据 容错
				b = ClubDailyCostProto.newBuilder();
				b.setCreateAt(MyDateUtil.getZeroDate(new Date(createTime)).getTime());
				dailyCosts.add(b.build());
			} else {
				b = dailyCosts.get(dailyCosts.size() - 1).toBuilder();
			}
			b.setDailyCount(b.getDailyCount() + 1);
			if (wealthCategory == (int) EWealthCategory.EXCLUSIVE_GOLD.category()) {
				b.setDailyExclusiveGold(b.getDailyExclusiveGold() + costGold);
			} else {
				b.setDailyGold(b.getDailyGold() + costGold);
			}
			dailyCosts.set(dailyCosts.size() - 1, b.build());

		} else {
			if (wealthCategory == (int) EWealthCategory.EXCLUSIVE_GOLD.category()) {
				this.costExclusiveGold += costGold;
			} else {
				this.costGold += costGold;
				ClubDataLogManager.addClubCostGold(costGold);
			}
			this.gameCount += 1;
		}
	}

	public void update(long ctime) {

		if (isMultiClubRuleTableMode() && loopCounter++ % 10 == 0) {
			runInReqLoop(new CheckPlayerStatusTask(this, ctime));
		}

		if (clubWelfareWrap.isOpenClubWelfare() && loopCounter++ % 5 == 0) {
			runInReqLoop(new ClubWelfareLotteryTask(this, ctime));
		}

		if (ctime >= nextRefreshTime) {
			synchronized (this) {
				runInReqLoop(() -> {
					if (ctime >= nextRefreshTime) {

						memberRecordDailyReset();

						ClubDailyCostProto.Builder b = ClubDailyCostProto.newBuilder();
						b.setCreateAt(MyDateUtil.getYesterdayZeroDate(nextRefreshTime).getTime());
						b.setDailyGold(costGold);
						b.setDailyCount(gameCount);
						b.setDailyExclusiveGold(costExclusiveGold);
						this.costGold = 0;
						this.gameCount = 0;
						this.costExclusiveGold = 0;

						// 自建赛数据
						if (clubDataModel != null) {
							ClubDailyMatchDataProto.Builder clubData = ClubDailyMatchDataProto.newBuilder();
							clubData.setCreateTime(MyDateUtil.getYesterdayZeroDate(nextRefreshTime).getTime());
							clubData.setMatchCreateNum(dailyMatchCreateNum);
							clubData.setMatchEndNum(dailyMatchEndNum);
							clubData.setMatchExclusiveGold(dailyMatchExclusiveGold);
							clubData.setMatchGold(dailyMatchGold);
							dailyMatchCreateNum = 0;
							dailyMatchEndNum = 0;
							dailyMatchExclusiveGold = 0;
							dailyMatchGold = 0;
							List<ClubDailyMatchDataProto> tmpMatchData = new ArrayList<>(matchDailyDatas);
							if (tmpMatchData.size() > 60) {
								tmpMatchData = tmpMatchData.subList(1, tmpMatchData.size());
							}
							tmpMatchData.add(clubData.build());
							matchDailyDatas = new ArrayList<>(tmpMatchData);
						}

						nextRefreshTime = MyDateUtil.getTomorrowZeroDate(ctime).getTime();

						List<ClubDailyCostProto> temp = new ArrayList<>(dailyCosts);
						if (temp.size() > 60) {
							temp = temp.subList(1, temp.size());
						}

						temp.add(b.build());
						dailyCosts = new ArrayList<>(temp);

						ClubDailyCostModel cost = new ClubDailyCostModel();
						cost.setAccount_id(clubModel.getAccount_id());
						cost.setClubId(clubModel.getClub_id());
						cost.setCost(b.getDailyGold());
						cost.setGameCount(b.getDailyCount());
						cost.setAccountId(clubModel.getAccount_id());
						cost.setClubName(clubModel.getClub_name());
						cost.setCreate_time(new Date(b.getCreateAt()));
						cost.setClubMemberCount(members.size());
						String ruleNames = "";
						if (clubModel.getRules() != null) {
							StringBuffer buf = new StringBuffer();
							for (ClubRuleModel model : clubModel.getRules().values()) {
								buf.append(model.getGame_name() + "#");
							}
							ruleNames = buf.toString();
						}
						cost.setGameNames(ruleNames);
						MongoDBServiceImpl.getInstance().getLogQueue().add(cost);

						clubMatchDailyReset();

						clubWelfareWrap.clubWelfareDailyReset();

						//包间消耗统计相关
						ruleTables.forEach((ruleId, ruleTable) -> ClubRoomUtil
								.saveRuleCostModel(ruleTable, getMemberCount(), MyDateUtil.getYesterdayZeroDate(nextRefreshTime)));
					}

					//如果有围观，踢出
					testKickOutObserver();
				});
			}
		}
	}

	/**
	 * 剔出围观
	 */
	private void testKickOutObserver() {
		Iterator<Map.Entry<Long, ClubMemberModel>> it = members.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Long, ClubMemberModel> entry = it.next();
			ClubMemberModel member = entry.getValue();
			if (null == member) {
				continue;
			}

			//强检测，身份
			if (member.getIdentity() == EClubIdentity.OBSERVER.identify() && Objects.equals(member.getNickname(), entry.getKey().toString())) {
				it.remove();
				ClubCacheService.getInstance().rmMemberClubId(entry.getKey(), getClubId());
				logger.warn("俱乐部[{}] 移出旁观者[ {} ]!", getClubId(), entry.getKey());
			}

		}
	}

	public void updateFiveMin(long ctime) {

	}

	public void save() {
		if (!ClubCfg.get().isUseOldRecordSaveWay()) {
			// 玩家记录数据落地
			List<ClubMemberRecordModel> recordModelList = new ArrayList<>();
			members.forEach((account_id, memberModel) -> {
				memberModel.getMemberRecordMap().forEach((day, recordModel) -> {
					if (recordModel.isNeedDB()) {
						recordModel.setNeedDB(false);
						recordModelList.add(recordModel);
					}
				});
			});
			if (!recordModelList.isEmpty()) {
				SpringService.getBean(ClubDaoService.class).batchUpdate("updateClubMemberRecord", recordModelList);
			}
		}

		// 玩家记录待删除数据
		if (delMemberRecordList != null && delMemberRecordList.size() > 0) {
			List<ClubMemberRecordModel> tmpList = new ArrayList<>();
			tmpList.addAll(delMemberRecordList);
			delMemberRecordList.clear();
			SpringService.getBean(ClubDaoService.class).batchDelete("deleteClubMemberRecordById", tmpList);
		}

		// 亲友圈普通公告落地
		List<ClubBulletinModel> bulletins_ = Lists.newArrayList();
		bulletins.forEach((id, wrap) -> {
			if (wrap.getBulletinModel().isNeedDB()) {
				bulletins_.add(wrap.getBulletinModel());
				wrap.getBulletinModel().setNeedDB(false);
			}
		});
		if (!bulletins_.isEmpty()) {
			SpringService.getBean(ClubDaoService.class).batchUpdate("updateClubBulletin", bulletins_);
		}
		// 亲友圈禁止同桌数据落地
		List<ClubBanPlayerModel> banPlayers = Lists.newArrayList();
		members.forEach((account_id, memberModel) -> {
			ClubBanPlayerModel model = memberModel.getClubBanPlayerModel();
			if (model != null && model.isNeedDB()) {
				if (memberModel.getMemberBanPlayerMap() != null) {
					StringBuilder sb = new StringBuilder();
					for (Long accountId : memberModel.getMemberBanPlayerMap().keySet()) {
						sb.append(accountId).append(",");
					}
					if (sb.length() > 0) {
						model.setBanPlayers(sb.deleteCharAt(sb.length() - 1).toString());
					} else {
						model.setBanPlayers(sb.toString());
					}
					banPlayers.add(model);
				}
				model.setNeedDB(false);
			}
		});
		if (!banPlayers.isEmpty()) {
			SpringService.getBean(ClubDaoService.class).batchUpdate("updateOrInsertClubMemberBanPlayer", banPlayers);
		}
		// 亲友圈自建赛数据落地
		List<ClubMatchModel> clubMatchs = new ArrayList<>();
		this.matchs.forEach((id, wrap) -> {
			ClubMatchModel model = wrap.getModel();
			if (model.isNeedDB()) {
				clubMatchs.add(model);
				model.setNeedDB(false);
			}
		});
		this.delMatchs.forEach((id, wrap) -> {
			ClubMatchModel model = wrap.getModel();
			if (model.isNeedDB()) {
				clubMatchs.add(model);
				model.setNeedDB(false);
			}
		});
		if (!clubMatchs.isEmpty()) {
			SpringService.getBean(ClubDaoService.class).batchUpdate("updateClubMatchModel", clubMatchs);
		}

		// 包间数据记录落地
		List<ClubRuleRecordModel> ruleRecordList = new ArrayList<>();
		this.ruleTables.forEach((id, ruleTable) -> {
			Map<Long, ClubRuleRecordModel> map = ruleTable.getRuleRecordMap();
			for (ClubRuleRecordModel model : map.values()) {
				if (model.isNeedDB()) {
					ruleRecordList.add(model);
					model.setNeedDB(false);
				}
			}
		});
		if (!ruleRecordList.isEmpty()) {
			SpringService.getBean(ClubDaoService.class).batchUpdate("updateClubRuleRecordModel", ruleRecordList);
		}
	}

	public ClubRoomModel requestJoinClub(long account, String avatar, String content, String nickname, long partnerId) {
		if (members.containsKey(account)) {
			return new ClubRoomModel(Club.FAIL).setDesc("你已经是该亲友圈成员!");
		}

		Supplier<ClubApplyJoinProto.Builder> supplier = () -> {
			long ctime = System.currentTimeMillis();
			ClubApplyJoinProto.Builder b = ClubApplyJoinProto.newBuilder();
			b.setAccountId(account);
			b.setCreateTime(ctime);
			b.setJoinTime(ctime);
			b.setAvatar(avatar);
			b.setNickname(nickname);
			b.setContent(content);
			b.setPartnerId(partnerId);
			return b;
		};

		// 审核设置相关
		if (!setsModel.isStatusTrue(EClubSettingStatus.CLUB_EXAMINED)) {
			requestMembers.put(account, supplier.get().build());
			ClubRoomModel examinedModel = ClubService.getInstance().agreeJoinClub(clubModel.getAccount_id(), clubModel.getClub_id(), account);
			return examinedModel.setDesc(Club.SUCCESS == examinedModel.getStatus() ? null : "服务器异常!");
		}

		if (requestMembers.containsKey(account)) {
			return new ClubRoomModel(Club.FAIL).setDesc("你已申请，请等待管理员审核!");
		}

		ClubApplyJoinProto.Builder b = supplier.get();
		requestMembers.put(account, b.build());

		// 推送消息给管理员
		ClubReqRsp.Builder builder = ClubReqRsp.newBuilder().addApplyList(b).setClubId(clubModel.getClub_id()).setReqType(ClubApplyType.JOIN);
		Utils.sendClient(getManagerIds(), S2CCmd.CLUB_APPLY_NOTIFY, builder);

		// 红点
		sendHaveNewMsg(ERedHeartCategory.CLUB_REQ_ENTER);
		return new ClubRoomModel(Club.SUCCESS).setDesc("申请成功，等待管理员审核!");
	}

	public ClubRoomModel requestQuitClub(long accountId, String avatar, String content, String nickname) {
		if (!members.containsKey(accountId)) {
			return new ClubRoomModel(Club.FAIL).setDesc("你已经不是该亲友圈成员!");
		}

		// 审核设置相关
		if (!setsModel.isStatusTrue(EClubSettingStatus.CLUB_QUIT_EXAMINED) || EClubIdentity.isManager(members.get(accountId).getIdentity())) {
			ClubRoomModel examinedModel = ClubService.getInstance().outClub(clubModel.getClub_id(), accountId);
			return examinedModel.setDesc(examinedModel.getDesc() != null ? examinedModel.getDesc() : "操作失败!");
		}

		if (requestQuitMembers.containsKey(accountId)) {
			return new ClubRoomModel(Club.FAIL).setDesc("你已申请，请等待管理员审核!");
		}

		ClubApplyLogModel applyModel = new ClubApplyLogModel();
		Date now = new Date();
		applyModel.setCreate_time(now);
		applyModel.setAccountId(accountId);
		applyModel.setClubId(this.getClubId());
		applyModel.setType(2);
		applyModel.setAvatar(avatar);
		applyModel.setNickname(nickname);
		applyModel.setContent(content);
		applyModel.setApplyTime(now);
		applyModel.setHandle(false);
		requestQuitMembers.put(accountId, applyModel);
		MongoDBServiceImpl.getInstance().statClubApplyLog(applyModel);

		// 推送消息给管理员
		ClubReqRsp.Builder builder = ClubReqRsp.newBuilder();
		builder.setClubId(clubModel.getClub_id());
		builder.setReqType(ClubApplyType.QUIT);
		ClubApplyExitProto.Builder exitBuilder = ClubApplyExitProto.newBuilder();
		exitBuilder.setCreateTime(applyModel.getCreate_time().getTime());
		exitBuilder.setAccountId(applyModel.getAccountId());
		exitBuilder.setAvatar(applyModel.getAvatar());
		exitBuilder.setNickname(applyModel.getNickname());
		exitBuilder.setApplyTime(applyModel.getApplyTime().getTime());
		ClubMemberRecordModel memberRecordModel = getMemberRecordModelByDay(1, members.get(accountId));
		exitBuilder.setTireValue(this.getMemberRealUseTire(memberRecordModel));
		builder.addExitList(exitBuilder);
		Utils.sendClient(getManagerIds(), S2CCmd.CLUB_APPLY_NOTIFY, builder);

		// 红点
		sendHaveNewMsg(ERedHeartCategory.CLUB_REQ_EXIT);
		return new ClubRoomModel(Club.APPLY_SUCCESS).setDesc("申请成功，等待管理员审核!");
	}

	public void sendHaveNewMsg(int type) {
		ClubRedHeartRsp.Builder builder = ClubRedHeartRsp.newBuilder().setClubId(clubModel.getClub_id()).setType(type);
		Utils.sendClient(getManagerIds(), S2CCmd.CLUB_RED_HEART, builder);
	}

	public int rejectJoinClub(long account, long operatorId) {
		ClubApplyJoinProto requestMember = requestMembers.remove(account);
		if (requestMember == null) {
			return NO_REQUEST;
		}
		// 返回推送
		Utils.notifyJoinResult(account, operatorId, this, ClubJoinCode.REJECT);
		agreeRequestMembers.offer(requestMember.toBuilder().setIsAgree(false).setJoinTime(System.currentTimeMillis()).build());

		return SUCCESS;
	}

	/**
	 * 解散调用
	 */
	public void disband() {

		// 告知在线玩家,放在前面只是为了保证消息能发到房间内的玩家
		Utils.notifyUpdateRule(clubModel.getClub_id(), 0, this, EClubOperateCategory.CLUB_DEL);

		ruleTables.forEach((ruleId, ruleTable) -> {
			ruleTable.disband();
		});

		ClubMemberModel memberModel = members.get(clubModel.getAccount_id());

		ClubDisbandSendMailProto.Builder b = ClubDisbandSendMailProto.newBuilder();
		b.setClubName(clubModel.getClub_name());
		b.setNickName(memberModel.getNickname());
		members.forEach((accountId, member) -> {
			b.addAccounts(accountId);
			ClubCacheService.getInstance().removeWelfareLotteryMember(member.getAccount_id());
		});

		SessionService.getInstance().sendGate(1, PBUtil.toS2SRequet(S2SCmd.S_2_M,
				S2STransmitProto.newBuilder().setAccountId(0).setRequest(PBUtil.toS2SResponse(S2SCmd.CLUB_DISBAND_TO_MACTH_SERVER, b))).build());

		// logger.warn("玩家[{}] 解散亲友圈[{}]!", getOwnerId(), getClubId());

		// 解散亲友圈时未开赛的自建赛需要还豆
		matchs.forEach((id, wrap) -> {
			if (wrap.getModel().getStatus() == ClubMatchStatus.PRE.status()) {
				wrap.cancelSchule(true);
				wrap.sendBackGold();
				wrap.enrollAccountIds().forEach((targetId) -> {
					wrap.exitMatch(targetId);
					Utils.sendClubMatchEvent(targetId, this, wrap.id(), ClubMatchCode.DISBAND_MATCH);
				});
			}
		});
	}

	public Date getLastChangeNameDate() {
		return lastChangeNameDate;
	}

	public void setLastChangeNameDate(Date lastChangeNameDate) {
		this.lastChangeNameDate = lastChangeNameDate;
	}

	public int getClubId() {
		return clubModel.getClub_id();
	}

	public final String getClubName() {
		return clubModel.getClub_name();
	}

	public long getOwnerId() {
		return clubModel.getAccount_id();
	}

	public ClubMemberModel getOwner() {
		return members.get(getOwnerId());
	}

	public String getOwnerName() {
		ClubMemberModel member = getOwner();
		return null != member ? member.getNickname() : " ";
	}

	public int getMemberCount() {
		return members.size();
	}

	private int getHasPlayerTableCount() {
		int count = 0;
		for (Map.Entry<Integer, ClubRuleTable> entry : ruleTables.entrySet()) {
			count += entry.getValue().getHasPlayerTableCount();
		}
		return count;
	}

	public int getPlayingTableCount() {
		int count = 0;
		for (Map.Entry<Integer, ClubRuleTable> entry : ruleTables.entrySet()) {
			count += entry.getValue().getPlayingTableCount();
		}
		return count;
	}

	public int getManagerCount() {
		return (int) members.values().stream().filter((m) -> m.getIdentity() == EClubIdentity.MANAGER.identify() && !m.isPartner()).count();
	}

	private List<ClubMemberModel> getManagers() {
		return FilterUtil.filter(members.values(), (m) -> EClubIdentity.isManager(m.getIdentity()));
	}

	public List<Long> allMemberIds() {
		return members.values().stream().map(ClubMemberModel::getAccount_id).collect(toList());
	}

	public List<Long> getManagerIds() {
		return getManagers().stream().map(ClubMemberModel::getAccount_id).collect(toList());
	}

	/**
	 * 是否可以修改亲友圈
	 */
	public boolean isCanChangeName() {
		return null == lastChangeNameDate || !DateUtils.isSameDay(lastChangeNameDate, new Date());
	}

	/**
	 * 亲友圈处理线程
	 */
	public WorkerLoop worker() {
		return this.worker;
	}

	public void runInClubLoop(final Runnable task) {
		this.worker.runInLoop(task);
	}

	/**
	 * 请求队列
	 */
	public void runInReqLoop(final Runnable task) {
		taskQueue.addTask(task);
	}

	/**
	 * DB操作
	 */
	public void runInDBLoop(final Runnable task) {
		dbQueue.addTask(task);
	}

	public final AutoDriverQueue taskQueue() {
		return taskQueue;
	}

	/**
	 * 活动
	 */
	public ClubActivityListProto.Builder toActivityListBuilder() {
		return toActivityListBuilder(activitys.keySet());
	}

	/**
	 * 活动
	 */
	public ClubActivityListProto.Builder toActivityListBuilder(Collection<Long> ids) {
		ClubActivityListProto.Builder builder = ClubActivityListProto.newBuilder();
		builder.setClubId(getClubId());
		final long current = System.currentTimeMillis();
		ids.forEach((id) -> {
			ClubActivityWrap actWrap = activitys.get(id);
			if (null != actWrap && actWrap.show(current)) {
				builder.addActs(actWrap.toActivityBuilder().build());
			}
		});
		return builder;
	}

	/**
	 * 是否有某项设置
	 */
	public boolean hasSetting(EClubSettingStatus sets) {
		return setsModel.isStatusTrue(sets);
	}

	public long getNextRefreshTime() {
		return nextRefreshTime;
	}

	public void setNextRefreshTime(long nextRefreshTime) {
		this.nextRefreshTime = nextRefreshTime;
	}

	/**
	 * 亲友圈迷你数据
	 */
	public ClubProto.Builder encodeMini() {
		ClubProto.Builder cb = ClubProto.newBuilder();
		cb.setAccountId(getOwnerId());
		cb.setClubId(getClubId());
		cb.setClubName(getClubName());
		cb.setCreateAt(clubModel.getDate().getTime());
		ClubMemberModel owner = getOwner();
		if (null != owner) {
			String avatar = owner.getAvatar();
			if (!Strings.isNullOrEmpty(avatar)) {
				cb.setAvatar(owner.getAvatar());
			}
		}
		cb.setOwenerName(owner.getNickname());
		return cb;
	}

	/**
	 * 亲友圈活动
	 */
	public List<ClubBulletinProto.Builder> toAllBulletinBuilder() {
		List<ClubBulletinProto.Builder> bulletinPBs = Lists.newArrayList();
		long cur = System.currentTimeMillis();
		bulletins.forEach((id, wrap) -> {
			if (wrap.isDone(cur)) {
				return;
			}
			bulletinPBs.add(wrap.toBuilder());
		});

		ClubService.getInstance().getSharedbulletins().forEach((id, model) -> {
			if (model.isDone(cur)) {
				return;
			}
			bulletinPBs.add(model.toBuilder());
		});
		return bulletinPBs;
	}

	public List<ClubBulletinProto.Builder> toBulletinBuilder(Collection<Long> ids) {
		List<ClubBulletinProto.Builder> bulletinPBs = Lists.newArrayList();
		long cur = System.currentTimeMillis();
		ids.forEach((id) -> {
			ClubBulletinWrap wrap = bulletins.get(id);
			if (null == wrap) {
				wrap = ClubService.getInstance().getSharedbulletins().get(id);
			}

			if (null == wrap || wrap.isDone(cur)) {
				return;
			}
			bulletinPBs.add(wrap.toBuilder());
		});
		return bulletinPBs;
	}

	public ClubSeat currentSeat() {
		return currentSeat;
	}

	/**
	 * 新活动
	 */
	public List<ClubDailyCostProto> toNewDailyCostBuilder() {
		List<ClubDailyCostProto> dailyCostPB = Lists.newArrayList();
		dailyCostModels.forEach((model) -> {
			dailyCostPB.add(ClubDailyCostProto.newBuilder().setCreateAt(model.getCreate_time().getTime()).setDailyCount(model.getGameCount())
					.setDailyCount((int) model.getCost()).build());
		});
		return dailyCostPB;
	}

	public ClubDailyCostModel getDailyCostModel() {
		return dailyCostModel;
	}

	public void setDailyCostModel(ClubDailyCostModel dailyCostModel) {
		this.dailyCostModel = dailyCostModel;
	}

	public static final int SUCCESS = 1;// 成功
	public static final int FAIL = 0;// 失败

	public static final int CLUB_NOT_FIND = -1; // 找不到亲友圈

	public static final int PERM_DENIED = -2; // 权限不够

	public static final int ACCOUNT_NOT_FIND = -3; // 不在该亲友圈里

	public static final int IS_PARTNER = -4; // 是该亲友圈合伙人

	public static final int HAS_JOIN = -13; // 已加入该亲友圈

	public static final int HAS_REQUEST = -14; // 已经申请加入该亲友圈

	public static final int NO_REQUEST = -15; // 没有申请加入该亲友圈

	public static final int GROUP_ISBIND = -27; // 微信群已经绑定了亲友圈
	// 加入亲友圈达到上限
	public static final int CLUB_ENTER_MAX = -16;

	public static final int ACCOUNT_IS_NULL = -21; // 找不到该用户

	public static final int CLUB_NAME_ERROR = -22;

	public static final int CLUB_DESC_ERROR = -23;
	// 创建上限
	public static final int CLUB_CREATE_MAX = -24;

	// 创建玩法上限
	public static final int CLUB_CREATE_RULE_MAX = -25;

	public static final int CLUB_MEMBER_MAX = -26;

	public static final int CLUB_CREATE_ERROR = -4; // 亲友圈创建失败

	public static final int CLUB_SERVER_ERROR = -500; // 服务器异常

	private static final int APPLY_SUCCESS = -501;// 申请成功

	static final String RELESE_TIP = "房间已被管理员解散";

	public static final String DEFRIEND_TIP = "您已被亲友圈暂停娱乐，请联系管理员进行操作";

	static final String MATCH_TIME_OUT_TIP = "您亲友圈自建赛的比赛时长超过120分钟，将自动判定您本场比赛为“弃赛”。请保持良好的比赛行为，便于大家愉快的参与比赛！";

	public ClubMemberRecordModel getMemberRecordModelByDay(int day, ClubMemberModel clubMemberModel) {
		// day 0-全部 1-今天 2-昨天 3-前天...
		Map<Integer, ClubMemberRecordModel> recordMap = clubMemberModel.getMemberRecordMap();
		if (!recordMap.containsKey(day)) {
			ClubMemberRecordModel model = new ClubMemberRecordModel();
			model.setClubId(clubMemberModel.getClub_id());
			model.setAccountId(clubMemberModel.getAccount_id());
			model.setDay(day);
			model.setGameCount(0);
			model.setBigWinCount(0);
			if (day == 0) {
				model.setGameCount(clubMemberModel.getGame_count());
				model.setBigWinCount(clubMemberModel.getWinCount());
			}
			model.setIsLike(0);
			model.setTireValue(0);

			if (ClubCfg.get().isUseOldRecordInsertWay()) {
				SpringService.getBean(ClubDaoService.class).getDao().insertClubMemberRecordModel(model);
			} else {
				this.runInDBLoop(new ClubMemberRecordCreateDBTask(model));
			}

			recordMap.put(day, model);
		}
		return recordMap.get(day);
	}

	/**
	 * 管理员修改亲友圈成员记录
	 */
	public void managerModifyMemberRecordModelByDay(ClubMemberRecordModifyProto proto, ClubMemberModel opeMember, C2SSession session) {
		int opeType = proto.getOpType();
		int day = proto.getRequestType();
		long accountId = proto.getAccountId();
		if (opeType == 1) { // 修改疲劳值
			if (!this.isTireSwitchOpen()) {
				Utils.sendTip(opeMember.getAccount_id(), "疲劳值功能已关闭,不能进行该操作！", ESysMsgType.INCLUDE_ERROR, session);
				return;
			}
			if (accountId > 0) { // 单个修改
				ClubMemberModel memberModel = members.get(accountId);
				if (memberModel == null) {
					return;
				}
				ClubMemberRecordModel recordModel = getMemberRecordModelByDay(day, memberModel);
				ClubMemberRecordModifyResponse.Builder builder = ClubMemberRecordModifyResponse.newBuilder();
				builder.setClubId(this.getClubId());
				builder.setAccountId(accountId);
				builder.setTireValue(proto.getTireValue());
				builder.setOpType(1);
				builder.setRequestType(day);
				int oldTireValue = this.getMemberRealUseTire(recordModel);
				if (oldTireValue == proto.getTireValue()) {
					return;
				}
				modifyPlayerTire(opeMember, memberModel, oldTireValue, proto.getTireValue(), day);

				Utils.sendClubAllMembers(builder, S2CCmd.MODIFY_CLUB_MEMBER_RECORD_RSP, this);
			} else if (proto.getTargetIdsCount() > 0) { // 批量修改
				ClubMemberRecordBatchModifyResponse.Builder builder = ClubMemberRecordBatchModifyResponse.newBuilder();
				builder.setClubId(this.getClubId());
				builder.setRequestType(day);
				builder.setTireValue(proto.getTireValue());
				for (Long targetId : proto.getTargetIdsList()) {
					ClubMemberModel memberModel = members.get(targetId);
					if (memberModel == null) {
						continue;
					}
					ClubMemberRecordModel recordModel = getMemberRecordModelByDay(day, memberModel);
					int oldTireValue = this.getMemberRealUseTire(recordModel);
					if (oldTireValue == proto.getTireValue()) {
						continue;
					}
					builder.addAccountIds(targetId);
					modifyPlayerTire(opeMember, memberModel, oldTireValue, proto.getTireValue(), day);
				}
				Utils.sendClubAllMembers(builder, S2CCmd.CLUB_MEMBER_RECORD_BATCH_MODIFY_RSP, this);
			}
		} else if (opeType == 2) { // 点心操作
			ClubMemberModel memberModel = members.get(accountId);
			if (memberModel == null) {
				return;
			}
			ClubMemberRecordModel recordModel = getMemberRecordModelByDay(day, memberModel);
			ClubMemberRecordModifyResponse.Builder builder = ClubMemberRecordModifyResponse.newBuilder();
			builder.setClubId(this.getClubId());
			builder.setAccountId(accountId);
			recordModel.setIsLike((recordModel.getIsLike() == 1) ? 0 : 1);
			builder.setIsLike(recordModel.getIsLike());
			builder.setOpType(2);
			builder.setRequestType(day);
			Utils.sendClubAllMembers(builder, S2CCmd.MODIFY_CLUB_MEMBER_RECORD_RSP, this);
		}
	}

	/**
	 * 修改玩家疲劳值
	 */
	private void modifyPlayerTire(ClubMemberModel opeMember, ClubMemberModel memberModel, int oldTireValue, int newTireValue, int day) {
		int changeValue = newTireValue - oldTireValue;
		long accountId = memberModel.getAccount_id();
		updateMemberRecordModelByDay(accountId, day, ClubRecordDataType.TIRE_VALUE, changeValue);
		//		if (day > 1) { // 如果修改的是昨天或者前天的累计疲劳值
		//			for (int i = day - 1; i >= 1; i--) {
		//				ClubMemberRecordModel tmp = getMemberRecordModelByDay(i, memberModel);
		//				if (tmp != null && tmp.getTireValue() != tmp.getAccuTireValue()) {
		//					tmp.setAccuTireValue(tmp.getAccuTireValue() + changeValue);
		//				} else {
		//					break;
		//				}
		//			}
		//		}

		ClubScoreMsgLogModel model = statModifyRecord(opeMember, memberModel, oldTireValue, newTireValue, day);
		notifyModifyRecord(accountId, model);

		ClubEventLog.event(new ClubEventLogModel(this.getClubId(), opeMember.getAccount_id(), EClubEventType.TIRE_U)
				.setTargetId(memberModel.getAccount_id()).setVal1(oldTireValue).setVal2(newTireValue).setVal3(day));
	}

	public void notifyModifyRecord(long accountId, ClubScoreMsgLogModel model) {
		Set<Long> notifyIds = Sets.newHashSet(getManagerIds());
		notifyIds.add(accountId);

		sendHaveNewMsg(ERedHeartCategory.CLUB_TIRE_MODIFY_NOTIFY);
		ClubModifyTireMsgResponse.Builder builder = ClubModifyTireMsgResponse.newBuilder();
		builder.setClubId(this.getClubId());
		builder.setType(2);
		ClubModifyTireMsgProto.Builder msgBuilder = ClubModifyTireMsgProto.newBuilder();
		msgBuilder.setOpeAccountId(model.getAccountId());
		msgBuilder.setOpeNickname(model.getAccountName());
		msgBuilder.setTargetAccountId(model.getTargetAccountId());
		msgBuilder.setTargetNickname(model.getTargetAccountName() == null ? "" : model.getTargetAccountName());
		msgBuilder.setOldValue(model.getOldValue());
		msgBuilder.setNewValue(model.getNewValue());
		msgBuilder.setTime(model.getCreate_time().getTime());
		msgBuilder.setRecordTime(model.getRecordTime());
		msgBuilder.setMsgType(model.getMsgType());
		msgBuilder.setSwitchStatus(model.getSwitchStatus());
		builder.addMsgs(msgBuilder);
		Utils.sendClient(notifyIds, S2CCmd.CLUB_MODIFY_TIRE_MSG, builder);
	}

	private ClubScoreMsgLogModel statModifyRecord(ClubMemberModel opeMember, ClubMemberModel memberModel, int oldTireValue, int newTireValue,
			int day) {
		ClubScoreMsgLogModel model = new ClubScoreMsgLogModel();
		model.setCreate_time(new Date());
		model.setAccountId(opeMember.getAccount_id());
		model.setAccountName(opeMember.getNickname());
		model.setTargetAccountId(memberModel.getAccount_id());
		model.setTargetAccountName(memberModel.getNickname());
		model.setOldValue(oldTireValue);
		model.setNewValue(newTireValue);
		model.setClubId(this.getClubId());
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		day = day - 1;
		cal.add(Calendar.DAY_OF_MONTH, -day);
		model.setRecordTime(cal.getTimeInMillis());
		model.setRecordDate(cal.getTime());
		clubScoreMsgWrap.addScoreMsg(model);
		MongoDBServiceImpl.getInstance().logClubScoreMsg(model);
		return model;
	}

	public ClubScoreMsgLogModel statModifyClubTireSwitchRecord(int clubId, ClubMemberModel opeMember, int switchStatus, int msgType) {
		ClubScoreMsgLogModel model = new ClubScoreMsgLogModel();
		model.setCreate_time(new Date());
		model.setAccountId(opeMember.getAccount_id());
		model.setAccountName(opeMember.getNickname());
		model.setClubId(clubId);
		model.setMsgType(msgType);
		model.setSwitchStatus(switchStatus);
		clubScoreMsgWrap.addScoreMsg(model);
		MongoDBServiceImpl.getInstance().logClubScoreMsg(model);
		return model;
	}

	public void updateMemberRecordModelByDay(long accountId, int day, int type, int value) {
		if (day < 1 || day > 8) {
			return;
		}
		ClubMemberModel memberModel = members.get(accountId);
		if (memberModel == null) {
			return;
		}
		ClubMemberRecordModel totalRecord = getMemberRecordModelByDay(0, memberModel);
		ClubMemberRecordModel dayRecord = getMemberRecordModelByDay(day, memberModel);
		if (type == ClubRecordDataType.GAME_COUNT) { // 牌局数
			totalRecord.setGameCount(totalRecord.getGameCount() + value);
			dayRecord.setGameCount(dayRecord.getGameCount() + value);
		} else if (type == ClubRecordDataType.BIG_WIN_COUNT) { // 大赢家数
			totalRecord.setBigWinCount(totalRecord.getBigWinCount() + value);
			dayRecord.setBigWinCount(dayRecord.getBigWinCount() + value);
		} else if (type == ClubRecordDataType.TIRE_VALUE) { // 疲劳值
			totalRecord.setTireValue(totalRecord.getTireValue() + value);
			dayRecord.setTireValue(dayRecord.getTireValue() + value);
			dayRecord.setAccuTireValue(dayRecord.getAccuTireValue() + value);
		}
	}

	public ClubScoreMsgWrap getClubScoreMsgWrap() {
		return clubScoreMsgWrap;
	}

	/**
	 * 亲友圈成员记录重置
	 */
	private void memberRecordDailyReset() {
		List<ClubMemberModel> memlist = new ArrayList<>();
		members.forEach((account_id, memberModel) -> {
			List<ClubMemberRecordModel> tempList = new ArrayList<>();
			Map<Integer, ClubMemberRecordModel> recordMap = memberModel.getMemberRecordMap();
			for (ClubMemberRecordModel recordModel : recordMap.values()) {
				if (recordModel.getDay() != 0) {
					int day = (recordModel.getDay() + 1) % 9;
					if (day == 0) {
						day = 1;
						recordModel.setGameCount(0);
						recordModel.setBigWinCount(0);
						recordModel.setIsLike(0);
						recordModel.setTireValue(0);
						recordModel.setAccuTireValue(0);
					}
					recordModel.setDay(day);
				}
				tempList.add(recordModel);
			}
			recordMap.clear();
			for (ClubMemberRecordModel model : tempList) {
				recordMap.put(model.getDay(), model);
			}
			if (!isTireDailyReset()) { // 疲劳值不清零
				if (recordMap.containsKey(ClubRecordDayType.YESTERDAY)) {
					int yesterdayAccuTire = recordMap.get(ClubRecordDayType.YESTERDAY).getAccuTireValue();
					if (yesterdayAccuTire != 0) {
						ClubMemberRecordModel recordModel = getMemberRecordModelByDay(ClubRecordDayType.TODAY, memberModel);
						recordModel.setAccuTireValue(yesterdayAccuTire);
						// 玩家疲劳值累计记录
						statPlayerTireAccuLog(memberModel, yesterdayAccuTire);
					}
				}
			}

			// 玩家打过的局数数据重置
			boolean result = memberModel.dailyResetPayRound();
			if (result) {
				memlist.add(memberModel);
			}
		});
		runInDBLoop(new ClubRecordDBTask(memlist));

		// 包间数据
		Date now = new Date();
		List<ClubRuleRecordModel> delRuleRecordList = new ArrayList<>();
		this.ruleTables.forEach((id, ruleTable) -> {
			Map<Long, ClubRuleRecordModel> map = ruleTable.getRuleRecordMap();
			List<Long> tmpList = new ArrayList<>();
			for (Map.Entry<Long, ClubRuleRecordModel> entry : map.entrySet()) {
				ClubRuleRecordModel model = entry.getValue();
				if (model.getIsTotal() == 0 && MyDateUtil.getIntervalDay(now, model.getRecordDate()) >= 8) {
					delRuleRecordList.add(model);
					tmpList.add(entry.getKey());
				}
			}
			for (Long key : tmpList) {
				map.remove(key);
			}
		});
		if (delRuleRecordList.size() > 0) {
			this.runInDBLoop(new ClubRuleRecordDelDBTask(delRuleRecordList));
		}
	}

	/**
	 * 玩家疲劳值累计日志
	 */
	private void statPlayerTireAccuLog(ClubMemberModel memberModel, int accuTireValue) {
		ClubScoreMsgLogModel model = new ClubScoreMsgLogModel();
		Date now = new Date();
		model.setCreate_time(now);
		model.setAccountId(memberModel.getAccount_id());
		model.setAccountName(memberModel.getNickname());
		model.setTargetAccountId(memberModel.getAccount_id());
		model.setTargetAccountName(memberModel.getNickname());
		model.setOldValue(accuTireValue);
		model.setNewValue(accuTireValue);
		model.setClubId(memberModel.getClub_id());
		model.setMsgType(ClubTireLogType.TIRE_ACCU);
		model.setRecordTime(now.getTime());
		clubScoreMsgWrap.addScoreMsg(model);
		MongoDBServiceImpl.getInstance().logClubScoreMsg(model);

	}

	public void roomPlayerLog(ClubGameOverProto clubGameOverProto, long createTime) {

	}

	public long getChatUniqueId() {
		initChatUniqueId();
		return chatAtomicId.incrementAndGet();
	}

	public void initChatUniqueId() {
		if (chatAtomicId == null) {
			chatAtomicId = new AtomicLong();
		}
		if (chatAtomicId.get() == 0) {
			// 初始化
			long maxId = 0;
			ClubChatMsg chatMsg = null;
			List<ClubChatMsg> list = new ArrayList<>();
			for (String str : chatWrap.getChatMsgList()) {
				try {
					chatMsg = JSON.parseObject(str, ClubChatMsg.class);
					list.add(chatMsg);
					long id = chatMsg.getUniqueId();
					if (id > maxId) {
						maxId = id;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			chatAtomicId.set(maxId);
			if (maxId == 0 && list.size() > 0) {
				// 功能上线时兼容下旧的聊天记录
				for (ClubChatMsg msg : list) {
					msg.setUniqueId(chatAtomicId.incrementAndGet());
				}
			}
		}
	}

	public void addDelMemberRecordData(ClubMemberRecordModel model) {
		if (delMemberRecordList == null) {
			delMemberRecordList = new ArrayList<>();
		}
		delMemberRecordList.add(model);
	}

	public boolean isTireSwitchOpen() {
		// 此开关1代表关闭 0代表开启(需求默认开启,为了方便处理)
		return !this.setsModel.isStatusTrue(EClubSettingStatus.CLUB_TIRE_SWITCH);
	}

	public boolean isTireDailyReset() {
		// 此开关0代表重置清零 1代表不重置清零(默认清零)
		return !this.setsModel.isStatusTrue(EClubSettingStatus.CLUB_TIRE_DAILY_RESET_SWITCH);
	}

	/**
	 * 玩家真正使用的疲劳值(增加开关,可以支持在新旧疲劳值模式之间切换)
	 */
	public int getMemberRealUseTire(ClubMemberRecordModel model) {
		if (ClubCfg.get().isUseOldTireWay()) {
			return model.getTireValue();
		} else {
			return model.getAccuTireValue();
		}
	}

	public void delPlayerLimitRound(ClubRuleModel clubRuleModel) {
		clubRuleModel.setLimitRound(ClubService.DEFAULT_LIMIT_ROUND);
		clubRuleModel.getSetsModel().statusDel(ERuleSettingStatus.GAME_ROUND_LIMIT_SWITCH);
		List<ClubMemberModel> list = new ArrayList<>();
		for (ClubMemberModel member : members.values()) {
			boolean result = member.delLimitRound(clubRuleModel.getId());
			if (result) {
				list.add(member);
			}
		}
		runInDBLoop(new ClubRecordDBTask(list));
	}

	/**
	 * 玩家离开亲友圈时退出参加的自建赛
	 */
	public void exitClubMatch(ClubMemberModel member) {
		matchs.forEach((id, matchWrap) -> {
			if (matchWrap.getEnrollAccountIds().contains(member.getAccount_id())) {
				matchWrap.exitMatch(member.getAccount_id());
				Utils.notifyClubMatchEvent(member.getAccount_id(), this, matchWrap.id(), ClubMatchCode.EXIT);
			}
		});
	}

	/**
	 * 检查自建赛结束
	 */
	public void checkMatchEnd(ClubRoomStatusProto req) {
		long matchId = req.getClubMatchId();
		if (matchId > 0) {
			ClubMatchWrap matchWrap = matchs.get(matchId);
			if (matchWrap != null) {
				List<Long> tablePlayerList = new ArrayList<>();
				for (ClubTablePlayerProto proto : req.getPlayersList()) {
					tablePlayerList.add(proto.getAccountId());
				}
				matchWrap.checkMatchEnd(tablePlayerList, req.getRoomId());
			}
		}
	}

	private void clubMatchDailyReset() {
		List<ClubMatchWrap> list = new ArrayList<>();
		Date now = new Date();
		matchs.forEach((id, wrap) -> {
			if (!DateUtils.isSameDay(now, wrap.getModel().getStartDate())) {
				if (wrap.getModel().getStatus() != ClubMatchStatus.PRE.status() && wrap.getModel().getStatus() != ClubMatchStatus.ING.status()) {
					list.add(wrap);
				}
			}
		});
		for (ClubMatchWrap wrap : list) {
			matchs.remove(wrap.id());
		}
	}

	public ClubDataModel getClubDataModel() {
		return clubDataModel;
	}

	private void createClubDataModelData() {
		clubDataModel = new ClubDataModel(this.getClubId());
		SpringService.getBean(ClubDaoService.class).getDao().insertClubDataModel(clubDataModel);
	}

	public void addMatchCreateNum() {
		if (clubDataModel == null) {
			createClubDataModelData();
		}
		this.dailyMatchCreateNum += 1;
		this.clubDataModel.setMatchCreateNum(this.clubDataModel.getMatchCreateNum() + 1);
	}

	void addMatchEndNum() {
		if (clubDataModel == null) {
			createClubDataModelData();
		}
		this.dailyMatchEndNum += 1;
		this.clubDataModel.setMatchEndNum(this.clubDataModel.getMatchEndNum() + 1);
	}

	void addGoldCost(int num) {
		if (clubDataModel == null) {
			createClubDataModelData();
		}
		this.dailyMatchGold += num;
		this.clubDataModel.setGoldCost(this.clubDataModel.getGoldCost() + num);
	}

	void addExclusiveGoldCost(int num) {
		if (clubDataModel == null) {
			createClubDataModelData();
		}
		this.dailyMatchExclusiveGold += num;
		this.clubDataModel.setExclusiveCost(this.clubDataModel.getExclusiveCost() + num);
	}

	/**
	 * 是否成员
	 */
	public boolean isMember(long accountId) {
		return members.containsKey(accountId);
	}

	public EClubIdentity getIdentify(long accountId) {
		ClubMemberModel member = members.get(accountId);
		return null != member ? EClubIdentity.of(member.getIdentity()) : null;
	}

	public void initRuleRecord(List<ClubRuleRecordModel> list) {
		if (list != null && list.size() > 0) {
			Date now = new Date();
			for (ClubRuleRecordModel model : list) {
				if (model.getIsTotal() == 0 && MyDateUtil.getIntervalDay(now, model.getRecordDate()) >= 8) {
					continue;
				}
				ClubRuleTable ruleTable = this.ruleTables.get(model.getRuleId());
				if (ruleTable != null) {
					ruleTable.getRuleRecordMap().put(model.getRecordDate().getTime(), model);
				}
			}
		}
	}

	public void removeRule(int ruleId) {
		ClubRuleTable ruleTable = ruleTables.remove(ruleId);
		if (ruleTable != null) {
			List<ClubRuleRecordModel> list = new ArrayList<>(ruleTable.getRuleRecordMap().values());
			if (list.size() > 0) {
				this.runInDBLoop(new ClubRuleRecordDelDBTask(list));
			}
		}
	}

	/**
	 * @param newOwnerId 新玩管理员
	 * @return true:成功  false:不是成员
	 */
	public final boolean transferOwner(long newOwnerId) {
		// 1权限验证
		ClubMemberModel currentOwner = getOwner();

		ClubMemberModel newOwner = members.get(newOwnerId);
		if (null == newOwner) {
			return false;
		}

		// 3设置，落地
		clubModel.setAccount_id(newOwnerId);
		clubModel.setAvatar(newOwner.getAvatar());
		currentOwner.setIdentity(EClubIdentity.COMMONER.identify());
		newOwner.setIdentity(EClubIdentity.CREATOR.identify());

		runInDBLoop(() -> {
			SpringService.getBean(ClubDaoService.class).getDao().updateClubAccountIdentity(currentOwner);
			SpringService.getBean(ClubDaoService.class).getDao().updateClubAccountIdentity(newOwner);
		});

		return true;
	}

	/**
	 * 接受合伙人邀请直接加入亲友圈
	 */
	public boolean fastJoin(long accountId, ClubMsgProto.ClubPartnerCommon req) {
		int clubId = getClubId();
		ClubMemberModel member = new ClubMemberModel();
		member.setAccount_id(accountId);
		member.setAvatar(req.getTargetAvatar());
		member.setNickname(req.getTargetName());
		member.setClubName(getClubName());
		member.setDate(new Date());
		member.setClub_id(clubId);

		members.put(accountId, member);

		ClubAccountModel requestClubAccount = new ClubAccountModel();
		requestClubAccount.setAccount_id(accountId);
		requestClubAccount.setClub_id(clubId);
		try {
			SpringService.getBean(ClubDaoService.class).getDao().insertClubAccount(requestClubAccount);
		} catch (Exception e) {
			logger.error("fastJoinClub error", e);
			members.remove(accountId);
			return false;
		}

		//绑定合伙人关系
		member.setParentId(req.getPartnerId());

		// 事件
		ClubMemberModel operator = getOwner();
		ClubMsgProto.ClubJoinQuitMsgProto.Builder eventMsg = Utils
				.newEventMsg(clubId, ClubMsgProto.ClubJoinQuitMsgProto.MsgType.JOIN, operator, member);

		joinQuitMsgQueueProto.offer(eventMsg.build());
		Utils.sendClubEventMsg(getManagerIds(), clubId, eventMsg.build());

		ClubCacheService.getInstance().addMemberClubId(accountId, clubId);

		ClubEventLog.event(new ClubEventLogModel(clubId, getOwnerId(), EClubEventType.JOIN).setTargetId(accountId));
		ClubDataLogManager.addNewJoinPlayer(accountId);

		Utils.sendJoinMemberInfo(getManagerIds(), clubId, member);

		runInDBLoop(() -> {
			SpringService.getBean(ClubDaoService.class).getDao().updateClubAccountPartner(member);
		});

		//清除之前玩家退出亲友圈时没有正常移除的玩家记录数据
		runInDBLoop(new ClubMemberRecordDelDBTask(clubId, member.getAccount_id()));
		return true;
	}

	public void sendClubPartnerEventMsgToClient(ClubMemberModel source, ClubMemberModel target, ClubMemberModel partner,
			ClubJoinQuitMsgProto.MsgType type) {
		ClubJoinQuitMsgProto.Builder eventMsg = Utils.newEventMsg(getClubId(), type, source, target);
		if (partner != null) {
			eventMsg.setPartnerId(partner.getAccount_id());
			eventMsg.setPartnerName(partner.getNickname());
		}
		joinQuitMsgQueueProto.offer(eventMsg.build());
		Utils.sendClubEventMsg(getManagerIds(), getClubId(), eventMsg.build());
		Utils.notifyRedHeart(this, ERedHeartCategory.CLUB_EVENT_NOTIFY);
	}

	/**
	 * 多包间模式
	 */
	public boolean isMultiClubRuleTableMode() {
		return setsModel.isStatusTrue(EClubSettingStatus.CLUB_RULE_TABLE_MODE);
	}
}
