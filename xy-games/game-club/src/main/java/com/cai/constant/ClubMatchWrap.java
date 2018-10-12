/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.constant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSON;
import com.cai.common.constant.RMICmd;
import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.constant.Symbol;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.ERedHeartCategory;
import com.cai.common.define.ESysMsgType;
import com.cai.common.domain.AccountMatchRedis;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.ClubMatchLogModel;
import com.cai.common.domain.ClubMatchModel;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.domain.ClubRoomModel;
import com.cai.common.domain.ClubRuleModel;
import com.cai.common.domain.CoinPlayerMatchRedis;
import com.cai.common.domain.CoinPlayerRedis;
import com.cai.common.domain.RankModel;
import com.cai.common.domain.SysGameType;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.rmi.vo.ClubExclusiveRMIVo;
import com.cai.common.rmi.vo.ClubMatchCreateRoomVo;
import com.cai.common.util.Bits;
import com.cai.common.util.ClubUitl;
import com.cai.common.util.NamedThreadFactory;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.RoomComonUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.StringUtil;
import com.cai.common.util.VoidFunction;
import com.cai.config.ClubCfg;
import com.cai.config.SystemConfig;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.redis.service.RedisService;
import com.cai.service.ClubCacheService;
import com.cai.service.ClubService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerService;
import com.cai.service.SessionService;
import com.cai.utils.ClubRoomUtil;
import com.cai.utils.LogicMsgSender;
import com.cai.utils.RoomUtil;
import com.cai.utils.Utils;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.protobuf.InvalidProtocolBufferException;
import com.googlecode.protobuf.format.JsonFormat;
import com.googlecode.protobuf.format.JsonFormat.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protobuf.clazz.ClubMsgProto.ClubMatchEventNotify;
import protobuf.clazz.ClubMsgProto.ClubMatchGameEndNotify;
import protobuf.clazz.ClubMsgProto.ClubMatchInfoProto;
import protobuf.clazz.ClubMsgProto.ClubMatchRankProto;
import protobuf.clazz.ClubMsgProto.ClubMatchScoreProto;
import protobuf.clazz.ClubMsgProto.ClubRedHeartRsp;
import protobuf.clazz.ClubMsgProto.ClubRuleProto;
import protobuf.clazz.ClubMsgProto.ScoreProto;
import protobuf.clazz.Common.CommonII;
import protobuf.clazz.Common.CommonILI;
import protobuf.clazz.s2s.S2SProto.ClubMatchStartFailSendMailProto;
import protobuf.clazz.s2s.S2SProto.S2STransmitProto;

import static java.util.Comparator.comparingLong;

/**
 * 俱乐部自建赛
 *
 * @author wu_hc date: 2018年6月20日 上午10:05:04 <br/>
 */
public class ClubMatchWrap {

	/**
	 * 日志
	 */
	private static final Logger logger = LoggerFactory.getLogger(ClubMatchWrap.class);

	/**
	 * 比赛调度器
	 */
	static final Map<Long, TriggerGroup> trigger = Maps.newConcurrentMap();

	/**
	 * 比赛调度器
	 */
	private static final ScheduledExecutorService schedule = Executors
			.newScheduledThreadPool(1, new NamedThreadFactory("club-match-scheduled-thread"));
	/**
	 * 比赛模块
	 */
	ClubMatchModel model;

	/**
	 * 报名成员列表
	 */
	Set<Long> enrollAccountIds;

	/**
	 * 分数
	 */
	private final Map<Long, ScoreEntry> memberScore = Maps.newHashMap();

	/**
	 * 奖励数量
	 */
	public final List<Integer> reward = Lists.newArrayList();

	/**
	 * 玩法
	 */
	private ClubRuleProto.Builder ruleBuilder;

	/**
	 * 玩法对应的规则model
	 */
	ClubRuleModel ruleModel;

	final Club club;

	private int totalGameCount;

	/**
	 * 禁止报名成员列表
	 */
	private final Set<Long> banPlayerIds;

	public Map<Integer, ClubMatchTable> clubMatchTables = new HashMap<>();

	public ClubMatchWrap(ClubMatchModel model, Club club) {
		this.model = model;
		this.club = club;
		this.enrollAccountIds = Sets.newHashSet();
		this.banPlayerIds = Sets.newHashSet();
		parseFromDB();
		initTrigger();
	}

	/**
	 * 比赛开始
	 */
	public void start() {
		// 开赛时间触发
		cancelSchule(false);

		Club club = ClubService.getInstance().getClub(model.getClubId());
		if (null == club) {
			return;
		}

		// 是否可以开赛
		if (!checkCanStart()) {
			// 开赛失败
			startMatchFail();
			return;
		}

		startMatch();

		memberScore.clear();
		enrollAccountIds.forEach(account_id -> {
			memberScore.put(account_id, new ScoreEntry());
		});
	}

	protected boolean checkCanStart() {
		boolean canStart = true;
		// 检查比赛人数
		//		if (this.model.getOpenType() == ClubMatchOpenType.TIME_MODE) {
		//			int tablePlayerNum = RoomComonUtil.getMaxNumber(this.ruleModel.getRuleParams());
		//			if (enrollAccountIds.size() <= 0 || enrollAccountIds.size() < model.getMinPlayerCount() || (enrollAccountIds.size() % tablePlayerNum) != 0) {
		//				canStart = false;
		//			}
		//		}
		// 检查比赛玩家是否还在亲友圈中，是否在线，是否已经在游戏中
		for (Long targetId : enrollAccountIds) {
			if (!club.members.containsKey(targetId)) {
				logger.warn("俱乐部[{}]，比赛[ {} ] 参赛玩家[{}] 已经不在俱乐部，开赛失败！", club.getClubId(), id(), targetId);
				canStart = false;
				break;
			}
			// if (SessionService.getInstance().getProxyByServerIndex(targetId)
			// == -1) {
			// logger.warn("俱乐部[{}]，比赛[ {} ] 参赛玩家[{}] 离线，开赛失败！",
			// club.getClubId(), id(), targetId);
			// canStart = false;
			// break;
			// }
			int roomId = RoomUtil.getRoomId(targetId);
			if (roomId > 0) {
				logger.warn("俱乐部[{}]，比赛[ {} ] 参赛玩家[{}] 有房间[{}]，开赛失败！", club.getClubId(), id(), targetId, roomId);
				canStart = false;
				break;
			}
			CoinPlayerMatchRedis redis = SpringService.getBean(RedisService.class)
					.hGet(RedisConstant.COIN_PLAYER_MATCH_INFO, targetId + "", CoinPlayerMatchRedis.class);
			if (redis != null) {
				logger.warn("俱乐部[{}]，比赛[ {} ] 参赛玩家[{}] 有金币场游戏正在匹配中，开赛失败！", club.getClubId(), id(), targetId);
				canStart = false;
				break;
			}
			CoinPlayerRedis coinRedis = SpringService.getBean(RedisService.class)
					.hGet(RedisConstant.COIN_PLAYER_INFO, targetId + "", CoinPlayerRedis.class);
			if (coinRedis != null) {
				logger.warn("俱乐部[{}]，比赛[ {} ] 参赛玩家[{}] 有未完成的金币场游戏，开赛失败！", club.getClubId(), id(), targetId);
				canStart = false;
				break;
			}
			AccountMatchRedis accountMatchRedis = SpringService.getBean(RedisService.class)
					.hGet(RedisConstant.MATCH_ROOM_ACCOUNT, targetId + "", AccountMatchRedis.class);
			if (accountMatchRedis != null && accountMatchRedis.isStart()) {
				logger.warn("俱乐部[{}]，比赛[ {} ] 参赛玩家[{}] 已经报名比赛场了，开赛失败！", club.getClubId(), id(), targetId);
				canStart = false;
				break;
			}
		}
		return canStart;
	}

	void startMatch() {
		//		if (this.model.getOpenType() == ClubMatchOpenType.COUNT_MODE) {
		//			TriggerGroup triggerGroup = trigger.get(model.getId());
		//			if (triggerGroup == null) {
		//				triggerGroup = new TriggerGroup();
		//			}
		//			triggerGroup.ensureStopTrigger = newTrigger(this::ensureStop, 2 * 60 * TimeUtil.MINUTE);
		//			trigger.put(model.getId(), triggerGroup);
		//			logger.warn("俱乐部[{}]，比赛[ {} ] 为满人赛，开赛时加入确保比赛结束调度！", club.getClubId(), id());
		//		}

		List<Long> playerList = new ArrayList<>(enrollAccountIds);
		Collections.shuffle(playerList);
		ClubRuleModel clubRuleModel = ruleModel();

		boolean repair = ClubCfg.get().getRuleUpdateSubGameIds().contains(clubRuleModel.getGame_type_index());
		if (SystemConfig.gameDebug == 1)
			repair = true;// 调试环境直接去对应的逻辑服取

		int debugLogicId = 0;
		List<Integer> clubMatchLogics = ClubCfg.get().getClubMatchLogicList();
		if (clubMatchLogics.size() > 0) {
			if (clubMatchLogics.size() == 1) {
				debugLogicId = clubMatchLogics.get(0);
			} else {
				int index = RandomUtil.getRandomNumber(clubMatchLogics.size());
				debugLogicId = clubMatchLogics.get(index);
			}
		}

		int tablePlayerNum = RoomComonUtil.getMaxNumber(clubRuleModel.getRuleParams());
		int roomCount = playerList.size() / tablePlayerNum;
		boolean result = true;
		long clubMatchId = id();
		ICenterRMIServer centerRmiServer = SpringService.getBean(ICenterRMIServer.class);
		ClubMatchCreateRoomVo vo = new ClubMatchCreateRoomVo();
		vo.setRoomNum(roomCount);
		vo.setClubId(club.getClubId());
		vo.setClubName(club.clubModel.getClub_name());
		vo.setClubMemSize(club.getMemberCount());
		vo.setClubOwnerId(club.clubModel.getAccount_id());
		vo.setClubRuleModel(clubRuleModel);
		vo.setTableIndex(-1);
		vo.setRepair(repair);
		vo.setLogicId(debugLogicId);
		List<ClubRoomModel> roomList = centerRmiServer.rmiInvoke(RMICmd.CLUB_MATCH_CREATE_ROOM, vo);
		String failDesc = "";
		if (roomList == null || roomList.size() <= 0) {
			result = false;
		} else {
			for (int i = 0; i < roomList.size(); i++) {
				ClubRoomModel clubRoomModel = roomList.get(i);
				if (clubRoomModel.getStatus() != Club.SUCCESS) {
					result = false;
					failDesc = clubRoomModel.getDesc();
					break;
				}
				List<Long> tmpList = playerList.subList(i * tablePlayerNum, (i + 1) * tablePlayerNum);
				clubRoomModel.setClub_name(club.clubModel.getClub_name());
				clubRoomModel.setClubRule(clubRuleModel);

				// 创建房间
				result = LogicMsgSender.sendCreateClubMatchRoom(club, clubRoomModel, tmpList, clubMatchId);
				if (!result) {
					break;
				}
			}
		}

		if (!result) { // 建房失败开始比赛也失败
			startMatchFail();
			logger.warn("俱乐部[{}]，比赛[ {} ] 开赛失败，创建房间失败={}！", club.getClubId(), id(), failDesc);
			return;
		}
		this.model.setStatus(ClubMatchStatus.ING.status());
		this.model.setStartDate(new Date());

		//开赛成功事件
		Utils.notifyClubMatchEvent(club.getOwnerId(), club, id(), ClubMatchCode.MATCH_START);

		for (int i = 0; i < roomList.size(); i++) {
			ClubRoomModel clubRoomModel = roomList.get(i);
			int roomId = clubRoomModel.getRoomId();
			List<Long> tmpList = playerList.subList(i * tablePlayerNum, (i + 1) * tablePlayerNum);
			for (Long targetId : tmpList) {
				ClubCacheService.getInstance().updateMemberOngoingClubMatch(targetId, this.id(), roomId, 1);
			}
			ClubMatchTable clubMatchTable = new ClubMatchTable(roomId, tmpList);
			clubMatchTables.put(roomId, clubMatchTable);
		}
	}

	/**
	 * 即将开始
	 */
	void willStart() {
		Club club = ClubService.getInstance().getClub(model.getClubId());
		if (null == club) {
			return;
		}

		ClubMatchEventNotify.Builder b = ClubMatchEventNotify.newBuilder().setOperatorId(club.getOwnerId()).setClubId(club.getClubId())
				.setEventCode(ClubMatchCode.WILL_START).setMatchId(id());
		b.setMatchName(model.getMatchName());
		b.setStartTime((int) (model.getStartDate().getTime() / 1000));
		Utils.sendClient(enrollAccountIds, S2CCmd.CLUB_MATCH_EVENT, b);
	}

	/**
	 * 确保比赛停止(逻辑服直接停服导致比赛无法结束的保底处理)
	 */
	void ensureStop() {
		stop(true);
	}

	/**
	 * 结束
	 */
	public void stop(boolean isEnsure) {

		cancelSchule(true);

		this.model.setStatus(ClubMatchStatus.AFTER.status());
		this.model.setEndDate(new Date());

		if (isEnsure) {
			// 将还在牌桌里的参赛玩家设置为弃赛并提示比赛结束
			for (Long target : this.enrollAccountIds) {
				ScoreEntry entry = memberScore.get(target);
				if (entry != null && !entry.isEnd) {
					updateScore(target, 0, false, false);
					Utils.sendTip(target, Club.MATCH_TIME_OUT_TIP, ESysMsgType.NONE);
				}
			}
		}

		// 管理员解散的房间玩家标记为弃赛
		Set<Long> disbandPlayers = Sets.newHashSet();
		clubMatchTables.forEach((roomId, table) -> {
			if (table.isDisband()) {
				table.getPlayers().forEach((playerId) -> {
					updateScore(playerId, 0, false, false);
					disbandPlayers.add(playerId);
				});
			}
		});

		// 1 生成比赛战报
		String temp = "";
		SysGameType gameType = SysGameTypeDict.getInstance().getSysGameType(this.ruleBuilder.getGameTypeIndex());
		if (gameType != null) {
			temp = gameType.getAppName();
		}
		String gameName = temp;
		String subGameName = SysGameTypeDict.getInstance().getMJname(this.ruleBuilder.getGameTypeIndex());
		ClubMatchLogModel logModel = ClubUitl.matchLogModel(model, JSON.toJSONString(rank()), gameName, subGameName);
		MongoDBServiceImpl.getInstance().getLogQueue().add(logModel);

		Club club = ClubService.getInstance().getClub(model.getClubId());
		if (null == club) {
			return;
		}

		club.runInReqLoop(() -> {
			club.matchs.remove(this.id());
			club.delMatchs.put(this.id(), this);
			club.clubMatchLogWrap.addMatchLog(logModel);

			club.addMatchEndNum();
			String costGold = this.model.getCostGold();
			if (!Strings.isNullOrEmpty(costGold)) {
				List<Integer> list = StringUtil.toIntList(costGold, Symbol.COMMA);
				if (list != null && list.size() >= 2) {
					if (list.get(0) == 1) {
						club.addExclusiveGoldCost(list.get(1));
					} else if (list.get(0) == 0) {
						club.addGoldCost(list.get(1));
					}
				}
			}

			// 比赛记录红点通知
			sendRedHeart(ERedHeartCategory.CLUB_MATCH_NEW_LOG);

			// 2通知参赛玩家
			ClubMatchGameEndNotify.Builder b = ClubMatchGameEndNotify.newBuilder();
			b.setClubId(club.getClubId());
			b.setMatchId(this.id());
			if (this.model.getMatchName() != null) {
				b.setMatchName(this.model.getMatchName());
			}
			if (subGameName != null) {
				b.setGameName(subGameName);
			}
			if (gameName != null) {
				b.setGameName(gameName);
			}

			int i = 1;
			for (Integer v : reward) {
				CommonII.Builder tmp = CommonII.newBuilder();
				tmp.setK(i++);
				tmp.setV(v);
				b.addRewards(tmp);
			}
			b.addAllRankList(ClubUitl.toRankModelListProto(rank()));
			for (Long accountId : enrollAccountIds) {
				if (disbandPlayers.contains(accountId)) {
					continue;
				}
				b.setAccountId(accountId);
				Utils.sendClient(accountId, S2CCmd.CLUB_MATCH_GAME_END_NOTIFY, b);
			}

			for (Long targetId : enrollAccountIds) {
				ClubCacheService.getInstance().updateMemberOngoingClubMatch(targetId, this.id(), 0, 0);
			}
		});
	}

	/**
	 * 生成排名
	 */
	private List<RankModel> rank() {
		List<RankModel> rankList = Lists.newArrayList();
		List<RankModel> disbandList = Lists.newArrayList();

		memberScore.forEach((account_id, entry) -> {
			ClubMemberModel member = club.members.get(account_id);
			if (null == member) {
				return;
			}
			RankModel m = new RankModel();
			m.setAccountId(account_id);
			if (ClubCfg.get().isDefendCheating() && this.model.getStatus() == ClubMatchStatus.ING.status()) {
				m.setNickName("***");
			} else {
				m.setNickName(member.getNickname());
			}

			m.setValue(entry.score);
			m.setDisband(entry.isDisband);
			m.setHead("");
			m.setV1((int) (member.getDate().getTime() / 1000L));
			if (m.isDisband()) {
				disbandList.add(m);
			} else {
				rankList.add(m);
			}
		});
		rankList.sort(comparingLong(RankModel::getValue).reversed().thenComparingLong(RankModel::getV1));
		//rankList.sort(comparingLong(RankModel::getValue).reversed());
		rankList.addAll(disbandList);

		for (int i = 0; i < rankList.size(); i++) {
			rankList.get(i).setRank(i + 1);
		}
		return rankList;
	}

	/**
	 * 报名
	 */
	public boolean enroll(long accountId) {
		if (this.model.getStatus() != ClubMatchStatus.PRE.status) {
			return false;
		}
		if (enrollAccountIds.size() >= model.getMaxPlayerCount()) {
			return false;
		}
		enrollAccountIds.add(accountId);
		serialEnrollAccountIds();

		if (this.model.getOpenType() == ClubMatchOpenType.COUNT_MODE) {
			if (enrollAccountIds.size() == model.getMaxPlayerCount()) { // 满人即开赛
				start();
			}
		}
		return true;
	}

	/**
	 * 退赛
	 */
	public boolean exitMatch(long accountId) {
		if (this.model.getStatus() != ClubMatchStatus.PRE.status) {
			return false;
		}
		boolean result = enrollAccountIds.remove(accountId);
		serialEnrollAccountIds();
		return result;
	}

	/**
	 * 更新分数
	 */
	public void updateScore(long accountId, int score, boolean isComplete, boolean isRound) {
		ScoreEntry entry = memberScore.get(accountId);
		if (entry != null) {
			if (isRound) {
				if (!entry.isDisband) {
					entry.score = score;
				}
			} else {
				entry.score = score;
				if (!isComplete) {
					entry.score = 0;
					entry.isDisband = true;
				}
			}
			serialScoreData();
		}
	}

	public void setGameEnd(long accountId) {
		ScoreEntry entry = memberScore.get(accountId);
		if (entry != null) {
			entry.isEnd = true;
		}
	}

	/**
	 * 比赛id
	 */
	public long id() {
		return model.getId();
	}

	public final ClubMatchModel getModel() {
		return this.model;
	}

	/**
	 * 从DB解析
	 */
	private void parseFromDB() {

		// 报名成员数据解析
		if (null != model.getEnrollAccountIds()) {
			try {
				List<Long> enrollAccountIds_ = JSON.parseArray(model.getEnrollAccountIds(), Long.class);
				enrollAccountIds.addAll(enrollAccountIds_);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// 奖励数据解析
		parseReward(model.getReward());

		// 玩法
		if (null != model.getGameRuleJson() && null == ruleBuilder) {

			try {
				ruleBuilder = ClubRuleProto.newBuilder();
				JsonFormat.merge(model.getGameRuleJson(), ruleBuilder);

				ClubRuleModel clubRuleModel = new ClubRuleModel();
				ClubRuleProto ruleProto = ruleBuilder.build();
				clubRuleModel.setGame_type_index(ruleProto.getGameTypeIndex());
				clubRuleModel.setRules(ruleProto.getRules());
				clubRuleModel.setGame_round(ruleProto.getGameRound());
				clubRuleModel.init();
				this.ruleModel = clubRuleModel;
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		// 积分
		if (model.getDatas() != null && model.getDatas().length > 0) {
			try {
				ClubMatchScoreProto matchScoreProto = ClubMatchScoreProto.parseFrom(model.getDatas());
				memberScore.clear();
				for (ScoreProto proto : matchScoreProto.getScoresList()) {
					ScoreEntry entry = new ScoreEntry();
					entry.accountId = proto.getAccountId();
					entry.score = proto.getScore();
					entry.isDisband = proto.getIsDisband();
					entry.isEnd = proto.getIsEnd();
					memberScore.put(proto.getAccountId(), entry);
				}
			} catch (InvalidProtocolBufferException e) {
				logger.error("clubMatchScore parse error", e);
				e.printStackTrace();
			}
		} else {
			memberScore.clear();
			enrollAccountIds.forEach(account_id -> {
				memberScore.put(account_id, new ScoreEntry());
			});
		}

		// 禁止参赛玩家
		if (!Strings.isNullOrEmpty(model.getBanPlayers())) {
			try {
				List<Long> banPlayerIds_ = JSON.parseArray(model.getBanPlayers(), Long.class);
				banPlayerIds.addAll(banPlayerIds_);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 解析奖励
	 */
	private void parseReward(String reward) {
		if (!Strings.isNullOrEmpty(reward)) {
			List<Integer> reward_ = StringUtil.toIntList(model.getReward(), Symbol.COMMA);
			this.reward.clear();
			if (reward_ != null) {
				this.reward.addAll(reward_);
			}
		}
	}

	private void serialEnrollAccountIds() {
		try {
			getModel().setEnrollAccountIds(JSON.toJSON(enrollAccountIds).toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void serialBanPlayerIds() {
		try {
			getModel().setBanPlayers(JSON.toJSON(banPlayerIds).toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void serialScoreData() {
		try {
			ClubMatchScoreProto.Builder builder = ClubMatchScoreProto.newBuilder();
			memberScore.forEach((accountId, entry) -> {
				ScoreProto.Builder scoreBuilder = ScoreProto.newBuilder();
				scoreBuilder.setAccountId(accountId);
				scoreBuilder.setScore(entry.score);
				scoreBuilder.setIsDisband(entry.isDisband);
				scoreBuilder.setIsEnd(entry.isEnd);
				builder.addScores(scoreBuilder);
			});
			getModel().setDatas(builder.build().toByteArray());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 触发调度器
	 */
	protected void initTrigger() {
		//		if (this.model.getOpenType() == ClubMatchOpenType.TIME_MODE) {
		//			long triggerTime = model.getStartDate().getTime() - System.currentTimeMillis();
		//			if (triggerTime > 0) {
		//				TriggerGroup triggerGroup = new TriggerGroup();
		//				triggerGroup.startTrigger = newTrigger(this::start, triggerTime);
		//
		//				if (triggerTime > ClubCfg.get().getClubMatchWillStartMinute() * TimeUtil.MINUTE)
		//					triggerGroup.willStartTrigger = newTrigger(this::willStart,
		//							triggerTime - ClubCfg.get().getClubMatchWillStartMinute() * TimeUtil.MINUTE);
		//
		//				triggerGroup.ensureStopTrigger = newTrigger(this::ensureStop, 2 * 60 * TimeUtil.MINUTE + triggerTime);
		//
		//				trigger.put(model.getId(), triggerGroup);
		//				logger.warn("--------俱乐部[{}]->比赛[id:{},name:{}]【加入】调度 --------", model.getClubId(), model.getId(), model.getMatchName());
		//			} else {
		//				if (this.model.getStatus() == ClubMatchStatus.PRE.status) { // 创建状态的比赛因中途停服错过开赛的情况,作为开赛失败处理
		//					// 延迟5分钟处理,以防在起服时服务器间连接未建立好导致逻辑执行失败
		//					TriggerGroup triggerGroup = new TriggerGroup();
		//					triggerGroup.startTrigger = newTrigger(this::startMatchFail, 5 * TimeUtil.MINUTE);
		//					trigger.put(model.getId(), triggerGroup);
		//					logger.warn("俱乐部[{}]，比赛[ {} ] 错过开始时间，将会自动还豆处理 ，开赛失败！", club.getClubId(), id());
		//				} else if (this.model.getStatus() == ClubMatchStatus.ING.status) {
		//					TriggerGroup triggerGroup = trigger.get(model.getId());
		//					if (triggerGroup == null) {
		//						triggerGroup = new TriggerGroup();
		//					}
		//					triggerGroup.ensureStopTrigger = newTrigger(this::ensureStop, 2 * 60 * TimeUtil.MINUTE + triggerTime);
		//					trigger.put(model.getId(), triggerGroup);
		//					logger.warn("俱乐部[{}]，比赛[ {} ] 正在进行中，启服后加入确保比赛结束调度！", club.getClubId(), id());
		//				}
		//			}
		//		} else if (this.model.getOpenType() == ClubMatchOpenType.COUNT_MODE) {
		//			if (this.model.getStatus() == ClubMatchStatus.ING.status) {
		//				TriggerGroup triggerGroup = trigger.get(model.getId());
		//				if (triggerGroup == null) {
		//					triggerGroup = new TriggerGroup();
		//				}
		//				triggerGroup.ensureStopTrigger = newTrigger(this::ensureStop, 2 * 60 * TimeUtil.MINUTE);
		//				trigger.put(model.getId(), triggerGroup);
		//				logger.warn("俱乐部[{}]，比赛[ {} ] 正在进行中，启服后加入确保比赛结束调度！", club.getClubId(), id());
		//			}
		//		}
	}

	public void cancelSchule(boolean cancelAll) {
		TriggerGroup triggerGroup = trigger.get(model.getId());
		if (null != triggerGroup) {
			if (null != triggerGroup.willStartTrigger) {
				triggerGroup.willStartTrigger.cancel(false);
				triggerGroup.willStartTrigger = null;
			}

			if (null != triggerGroup.startTrigger) {
				triggerGroup.startTrigger.cancel(false);
				triggerGroup.startTrigger = null;
			}

			logger.warn("--------俱乐部[{}]->比赛[id:{},name:{}]【移除】调度 --------", model.getClubId(), model.getId(), model.getMatchName());
		}

		if (cancelAll) {
			triggerGroup = trigger.remove(model.getId());
			if (null != triggerGroup) {
				if (null != triggerGroup.ensureStopTrigger) {
					triggerGroup.ensureStopTrigger.cancel(false);
				}

				logger.warn("--------俱乐部[{}]->比赛[id:{},name:{}]【移除】确保比赛结束调度 --------", model.getClubId(), model.getId(), model.getMatchName());
			}
		}
	}

	/**
	 * 生成调度器
	 */
	ScheduledFuture<?> newTrigger(VoidFunction call, final long delay) {
		return schedule.schedule(call, delay, TimeUnit.MILLISECONDS);
	}

	public ClubRuleProto.Builder getRuleBuilder() {
		return ruleBuilder;
	}

	public void setRuleBuilder(ClubRuleProto.Builder ruleBuilder) {
		this.ruleBuilder = ruleBuilder;
	}

	/**
	 * 获得已经参赛人数
	 */
	public Set<Long> enrollAccountIds() {
		return enrollAccountIds;
	}

	public Set<Long> getEnrollAccountIds() {
		return enrollAccountIds;
	}

	/**
	 * 比赛数据
	 */
	public final ClubMatchInfoProto.Builder toBuilder() {
		ClubMatchInfoProto.Builder builder = ClubMatchInfoProto.newBuilder();
		builder.setId(id());
		builder.setClubId(club.getClubId());
		builder.setMatchName(model.getMatchName());
		builder.setMatchType(model.getMatchType());
		builder.setOpenMatchType(model.getOpenType());
		builder.setMaxPlayerCount(model.getMaxPlayerCount());
		builder.setStartDate((int) (model.getStartDate().getTime() / 1000L));
		if (null != model.getEndDate()) {
			builder.setEndDate((int) (model.getEndDate().getTime() / 1000L));
		}
		builder.setCreatorId(model.getCreatorId());
		builder.setStatus(model.getStatus());
		builder.addAllEnrollAccountIds(enrollAccountIds);
		builder.setRule(ruleBuilder);
		int i = 1;
		for (Integer v : reward) {
			CommonII.Builder tmp = CommonII.newBuilder();
			tmp.setK(i++);
			tmp.setV(v);
			builder.addRewards(tmp);
		}
		String costGold = this.model.getCostGold();
		if (!Strings.isNullOrEmpty(costGold)) {
			List<Integer> list = StringUtil.toIntList(costGold, Symbol.COMMA);
			if (list != null && list.size() >= 2) {
				builder.setCostGold(list.get(1));
			}
		}
		builder.setAttendCondition(model.getAttendCondition());
		builder.setConditionValue(model.getConditionValue());
		builder.setMinPlayerCount(model.getMinPlayerCount());
		builder.setIsBanEnroll(isBanEnroll());
		builder.addAllBanPlayerIds(banPlayerIds);

		return builder;
	}

	/**
	 * 排名数据
	 */
	public final ClubMatchRankProto.Builder toRankBuilder() {
		ClubMatchRankProto.Builder builder = ClubMatchRankProto.newBuilder();
		builder.setClubId(club.getClubId());
		builder.setMatchId(id());
		builder.addAllRankList(ClubUitl.toRankModelListProto(rank()));
		return builder;
	}

	public boolean isBanEnroll() {
		return this.model.getIsBanEnroll() == 1;
	}

	public void banPlayer(long accountId) {
		banPlayerIds.add(accountId);
		serialBanPlayerIds();
	}

	public Set<Long> getBanPlayerIds() {
		return banPlayerIds;
	}

	public void updateMatch() {
		cancelSchule(true);
		parseFromDB();
		initTrigger();
	}

	public void updateClubMatchTableRound(int roomId, int roomCurRound) {
		ClubMatchTable clubMatchTable = clubMatchTables.get(roomId);
		if (clubMatchTable != null) {
			clubMatchTable.setCurRound(roomCurRound);
		}
	}

	public void updateClubMatchTableEnd(int roomId) {
		ClubMatchTable clubMatchTable = clubMatchTables.get(roomId);
		if (clubMatchTable != null) {
			clubMatchTable.setEnd(true);
		}
	}

	static final class ScoreEntry {
		int score;
		boolean isDisband;
		boolean isEnd;
		public long accountId;
	}

	static final class TriggerGroup {
		// 开赛触发器
		ScheduledFuture<?> startTrigger;

		// 即将开赛触发器
		ScheduledFuture<?> willStartTrigger;

		// 确保结束比赛触发器
		ScheduledFuture<?> ensureStopTrigger;
	}

	// 比赛状态描述
	public static enum ClubMatchStatus {

		PRE(Bits.byte_0), // 创建

		ING(Bits.byte_1), // 进行中

		AFTER(Bits.byte_2), // 正常结束

		CANCEL(Bits.byte_negative_1), // 取消

		FAILED(Bits.byte_negative_2); // 开赛失败

		byte status;

		private ClubMatchStatus(byte status) {
			this.status = status;
		}

		public byte status() {
			return this.status;
		}

		public static ClubMatchStatus of(int value) {
			for (ClubMatchStatus stus : ClubMatchStatus.values()) {
				if (value == stus.status()) {
					return stus;
				}
			}
			return null;
		}
	}

	/**
	 * 比赛开赛失败后还豆
	 */
	public void sendBackGold() {
		StringBuilder buf = new StringBuilder();
		int game_type_index = ruleBuilder.getGameTypeIndex();
		int gameId = SysGameTypeDict.getInstance().getGameIDByTypeIndex(game_type_index);
		String costGold = this.model.getCostGold();
		if (Strings.isNullOrEmpty(costGold)) {
			logger.error("亲友圈自建赛还豆失败,没有扣豆数据,clubId={},matchId={},gameId={},gameTypeIndex={}", club.getClubId(), this.id(), gameId, game_type_index);
			return;
		}

		List<Integer> list = StringUtil.toIntList(costGold, Symbol.COMMA);
		if (list == null || list.size() < 2) {
			logger.error("亲友圈自建赛还豆失败,扣豆数据不对,clubId={},matchId={},gameId={},gameTypeIndex={}", club.getClubId(), this.id(), gameId, game_type_index);
			return;
		}
		boolean isExclusive = list.get(0) == 1;
		int costNum = list.get(1);
		buf.append("亲友圈比赛开赛失败,clubId=" + ":").append(club.getClubId()).append("matchId:").append(this.id()).append("game_id:").append(gameId)
				.append(",game_type_index:").append(game_type_index).append(",game_round:").append(ruleBuilder.getGameRound()).append(",豆+:")
				.append(costNum);
		if (costNum == 0) {
			logger.warn(buf.toString());
			return;
		}

		// 如果是扣了专属豆，还专属豆
		if (isExclusive) {
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			ClubExclusiveRMIVo vo = ClubExclusiveRMIVo.newVo(club.getOwnerId(), gameId, costNum, EGoldOperateType.CLUB_MATCH_FAILED)
					.setGameTypeIndex(game_type_index).setClubId(club.getClubId());

			vo.setDesc(buf.toString());
			AddGoldResultModel addresult = centerRMIServer.rmiInvoke(RMICmd.CLUB_EXCLUSIVE_REPAY, vo);
			if (null != addresult && addresult.isSuccess()) {
				Object attament = addresult.getAttament();
				if (attament instanceof CommonILI) {
					Utils.sendExclusiveGoldUpdate(club.getOwnerId(), Arrays.asList((CommonILI) attament));
				}
				logger.warn("亲友圈自建赛专属豆还豆成功,clubId={},matchId={},gameId={},gameTypeIndex={},cost={}", club.getClubId(), this.id(), gameId,
						game_type_index, costNum);
			} else {
				logger.error("亲友圈自建赛专属豆还豆失败,clubId={},matchId={},gameId={},gameTypeIndex={},cost={}", club.getClubId(), this.id(), gameId,
						game_type_index, costNum);
			}
		} else {
			// 把豆还给玩家
			AddGoldResultModel addresult = ClubRoomUtil
					.addGold(club.getOwnerId(), costNum, false, buf.toString(), EGoldOperateType.CLUB_MATCH_FAILED);
			if (!addresult.isSuccess()) {
				logger.error("亲友圈自建赛闲逸豆还豆失败,clubId={},matchId={},gameId={},gameTypeIndex={},cost={}", club.getClubId(), this.id(), gameId,
						game_type_index, costNum);
			}
			logger.warn("亲友圈自建赛闲逸豆还豆成功,clubId={},matchId={},gameId={},gameTypeIndex={},cost={}", club.getClubId(), this.id(), gameId, game_type_index,
					costNum);
		}
	}

	void checkMatchEnd(List<Long> tablePlayerList, int roomId) {
		for (Long targetId : tablePlayerList) {
			ClubCacheService.getInstance().updateMemberOngoingClubMatch(targetId, this.id(), 0, 0);
		}

		this.model.setGameCount(this.model.getGameCount() + 1);
		if (totalGameCount == 0) {
			int tablePlayerNum = RoomComonUtil.getMaxNumber(this.ruleModel.getRuleParams());
			totalGameCount = enrollAccountIds.size() / tablePlayerNum;
		}
		if (this.model.getGameCount() >= this.totalGameCount) { // 比赛完成
			stop(false);
		} else { // 比赛还未完成时
			ClubMatchTable table = clubMatchTables.get(roomId);
			if (table != null && !table.isDisband()) {
				ClubMatchEventNotify.Builder b = ClubMatchEventNotify.newBuilder().setOperatorId(club.getOwnerId()).setClubId(club.getClubId())
						.setEventCode(ClubMatchCode.TABLE_GAME_END).setMatchId(id());
				Utils.sendClient(tablePlayerList, S2CCmd.CLUB_MATCH_EVENT, b);
			}
		}
	}

	/**
	 * 开赛失败
	 */
	void startMatchFail() {

		cancelSchule(true);

		this.model.setStatus(ClubMatchStatus.FAILED.status());
		sendBackGold();
		// 通知参赛玩家及所有管理员(在房间内的玩家发邮件通知)
		Set<Long> allPlayers = Sets.newHashSet();
		Set<Long> emailManagers = Sets.newHashSet();
		Set<Long> emailPlayers = Sets.newHashSet();
		Set<Long> otherPlayers = Sets.newHashSet();
		allPlayers.addAll(enrollAccountIds);
		allPlayers.addAll(club.getManagerIds());
		for (Long accountId : allPlayers) {
			if (RoomUtil.getRoomId(accountId) > 0) { // 在房间内
				if (club.getManagerIds().contains(accountId)) {
					emailManagers.add(accountId);
				} else {
					emailPlayers.add(accountId);
				}
			} else {
				otherPlayers.add(accountId);
			}
		}
		if (emailPlayers.size() > 0 || emailManagers.size() > 0) {
			ClubMatchStartFailSendMailProto.Builder b = ClubMatchStartFailSendMailProto.newBuilder();
			b.setType(1);
			b.addAllManagerIds(emailManagers);
			b.addAllPlayerIds(emailPlayers);
			b.setMatchName(this.model.getMatchName());
			b.setClubName(club.getClubName());
			SessionService.getInstance().sendGate(1, PBUtil.toS2SRequet(S2SCmd.S_2_M,
					S2STransmitProto.newBuilder().setAccountId(0).setRequest(PBUtil.toS2SResponse(S2SCmd.CLUB_MATCH_START_FAIL_TO_MATCH_SERVER, b)))
					.build());
		}
		if (otherPlayers.size() > 0) {
			ClubMatchEventNotify.Builder b = ClubMatchEventNotify.newBuilder().setOperatorId(club.getOwnerId()).setClubId(club.getClubId())
					.setEventCode(ClubMatchCode.START_FAIL).setMatchId(id()).setMatchName(this.model.getMatchName()).setClubName(club.getClubName());
			for (Long target : otherPlayers) {
				b.setTargetIsManager(club.getManagerIds().contains(target));
				Utils.sendClient(target, S2CCmd.CLUB_MATCH_EVENT, b);
			}
		}

		String gameName = "";
		SysGameType gameType = SysGameTypeDict.getInstance().getSysGameType(this.ruleBuilder.getGameTypeIndex());
		if (gameType != null) {
			gameName = gameType.getAppName();
		}
		String subGameName = SysGameTypeDict.getInstance().getMJname(this.ruleBuilder.getGameTypeIndex());
		ClubMatchLogModel logModel = ClubUitl.matchLogModel(model, "", gameName, subGameName);
		MongoDBServiceImpl.getInstance().getLogQueue().add(logModel);
	}

	private void sendRedHeart(int type) {
		ClubRedHeartRsp.Builder builder = ClubRedHeartRsp.newBuilder().setClubId(club.getClubId()).setType(type);
		for (Long target : this.enrollAccountIds) {
			if (PlayerService.getInstance().isPlayerOnline(target)) {
				Utils.sendClient(target, S2CCmd.CLUB_RED_HEART, builder);
			} else {
				ClubMemberModel memberModel = club.members.get(target);
				if (memberModel != null) {
					memberModel.setHaveNewClubMatchLog(true);
				}
			}
		}
	}

	public ClubRuleModel ruleModel() {
		return this.ruleModel;
	}
}
