package com.cai.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSON;
import com.cai.common.ClubGameInfoData;
import com.cai.common.ClubLogPlayer;
import com.cai.common.ClubMemberCountSectionData;
import com.cai.common.ClubMemberCountSectionType;
import com.cai.common.constant.RMICmd;
import com.cai.common.domain.PlayerViewVO;
import com.cai.common.domain.log.ClubDataLogModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.NamedThreadFactory;
import com.cai.common.util.SpringService;
import com.cai.constant.Club;
import com.cai.constant.ClubRuleTable;
import com.cai.service.ClubService;
import com.cai.service.MongoDBServiceImpl;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protobuf.clazz.s2s.ClubServerProto.ClubGameRecordProto.ClubGameOverProto;
import protobuf.clazz.s2s.ClubServerProto.ClubGameRecordProto.ClubGameOverProto.GamePlayerProto;

public class ClubDataLogManager {

	private static Logger logger = LoggerFactory.getLogger(ClubDataLogManager.class);

	private static final ScheduledExecutorService schExecutorService = Executors.newScheduledThreadPool(1,
			new NamedThreadFactory("club-datalog-schedule-thread"));

	private static Map<Long, ClubLogPlayer> clubLogPlayerMap = new HashMap<>();
	private static ClubDataLogModel logModel;
	private static Set<Long> activePlayers = Sets.newHashSet();
	private static Map<Integer, ClubGameInfoData> gameInfoDataMap = new HashMap<>();
	private static Map<Integer, Set<Long>> gamePlayerNumMap = new HashMap<>();
	private static Set<Integer> activeClubs = Sets.newHashSet();

	public static void init() {
		schExecutorService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				saveLog();
			}
		}, 5, 5, TimeUnit.MINUTES);
	}

	public static void saveLog() {
		Set<Long> clubDistinctUser = Sets.newHashSet();
		Map<Integer, Integer> clubRuleCountMap = new HashMap<>();
		for (int i = 0; i <= 5; i++) {
			clubRuleCountMap.put(i, 0);
		}
		Map<Integer, ClubMemberCountSectionData> clubMemCountSectionMap = new HashMap<>();
		for (int i = 1; i <= 9; i++) {
			ClubMemberCountSectionData data = new ClubMemberCountSectionData();
			clubMemCountSectionMap.put(i, data);
		}

		int totalClubCount = 0;
		int totalSuccessGameCount = 0;
		Map<Integer, Integer> clubGameIdCountMap = new HashMap<>();
		for (Club club : ClubService.getInstance().clubs.values()) {
			club.members.forEach((accountId, member) -> {
				clubDistinctUser.add(accountId);
			});
			int size = club.ruleTables.size();
			if (clubRuleCountMap.containsKey(size)) {
				clubRuleCountMap.put(size, clubRuleCountMap.get(size) + 1);
			}
			totalSuccessGameCount += club.gameCount;
			totalClubCount++;
			// 俱乐部子游戏统计
			statClubGameIdCount(clubGameIdCountMap, club);
			// 俱乐部人数区间统计
			statClubMemberCountSection(clubMemCountSectionMap, club);
		}
		StringBuilder ruleCountBuffer = new StringBuilder();
		for (int i = 0; i <= 5; i++) {
			ruleCountBuffer.append(i).append(",").append(clubRuleCountMap.get(i)).append("|");
		}
		if (ruleCountBuffer.length() > 0) {
			ruleCountBuffer.deleteCharAt(ruleCountBuffer.length() - 1);
		}
		StringBuilder gameIdCountBuffer = new StringBuilder();
		for (Integer gameId : clubGameIdCountMap.keySet()) {
			gameIdCountBuffer.append(gameId).append(",").append(clubGameIdCountMap.get(gameId)).append("|");
		}
		if (gameIdCountBuffer.length() > 0) {
			gameIdCountBuffer.deleteCharAt(gameIdCountBuffer.length() - 1);
		}
		StringBuilder clubSectionDataBuffer = new StringBuilder();
		for (int i = 1; i <= 9; i++) {
			ClubMemberCountSectionData data = clubMemCountSectionMap.get(i);
			clubSectionDataBuffer.append(data.getClubCount()).append(",").append(data.getGameCount()).append("|");
		}
		if (clubSectionDataBuffer.length() > 0) {
			clubSectionDataBuffer.deleteCharAt(clubSectionDataBuffer.length() - 1);
		}

		// 亲友圈数据统计
		ClubDataLogModel tmpModel = new ClubDataLogModel();
		tmpModel.setTotalClubCount(totalClubCount);
		tmpModel.setDistinct_user(clubDistinctUser.size());
		tmpModel.setSetRuleCountData(ruleCountBuffer.toString());
		tmpModel.setSuccessGameCount(totalSuccessGameCount);
		tmpModel.setClubGameCountData(gameIdCountBuffer.toString());
		tmpModel.setClubSectionData(clubSectionDataBuffer.toString());
		updateClubDataLog(tmpModel);
	}

	protected static void statClubGameIdCount(Map<Integer, Integer> clubGameIdCountMap, Club club) {
		for (ClubRuleTable ruleTable : club.ruleTables.values()) {
			int gameId = ruleTable.getClubRuleModel().getGame_id();
			if (gameId <= 0) {
				continue;
			}
			if (!clubGameIdCountMap.containsKey(gameId)) {
				clubGameIdCountMap.put(gameId, 0);
			}
			clubGameIdCountMap.put(gameId, clubGameIdCountMap.get(gameId) + 1);
		}
	}

	protected static void updateClubDataLog(ClubDataLogModel data) {
		logModel = getDataLogModel();
		logModel.setDistinct_user(data.getDistinct_user());
		logModel.setSetRuleCountData(data.getSetRuleCountData());
		logModel.setSuccessGameCount(data.getSuccessGameCount());
		logModel.setClubGameCountData(data.getClubGameCountData());
		logModel.setTotalClubCount(data.getTotalClubCount());
		logModel.setClubSectionData(data.getClubSectionData());

		try {
			for (ClubGameInfoData infoData : gameInfoDataMap.values()) {
				infoData.encodeGameRoundCountData();
			}
			logModel.setGameInfoData(JSON.toJSONString(gameInfoDataMap.values()));
		} catch (Exception e) {
			logger.error("stat game data error", e);
		}

		MongoDBServiceImpl.getInstance().updateClubDataLog(logModel);
	}

	public static void addNewJoinPlayer(long account_id) {
		ClubLogPlayer logPlayer = clubLogPlayerMap.get(account_id);
		if (logPlayer != null && logPlayer.getFirstJoinClubDate() != null) {
			return;
		}
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		List<Long> list = new ArrayList<>();
		list.add(account_id);
		list.add(1L);
		PlayerViewVO vo = centerRMIServer.rmiInvoke(RMICmd.CLUB_LOG_PLAYER_INFO, list);
		if (vo != null) {
			if (vo.isFirstJoinClub() && MyDateUtil.isSameDay(vo.getCreate_time())) {
				logModel = getDataLogModel();
				logModel.setNewJoinCount(logModel.getNewJoinCount() + 1);
			}

			if (!clubLogPlayerMap.containsKey(account_id)) {
				logPlayer = new ClubLogPlayer();
				logPlayer.setAccountId(account_id);
				clubLogPlayerMap.put(account_id, logPlayer);
			}
			logPlayer.setRegisterDate(vo.getCreate_time());
			logPlayer.setFirstJoinClubDate(vo.getFirstJoinClubTime());
		}
	}

	public static void addActivePlayer(long account_id) {
		if (!activePlayers.contains(account_id)) {
			logModel = getDataLogModel();
			activePlayers.add(account_id);
			logModel.setActivePlayerCount(logModel.getActivePlayerCount() + 1);

			ClubLogPlayer logPlayer = clubLogPlayerMap.get(account_id);
			if (logPlayer == null) {
				ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
				List<Long> list = new ArrayList<>();
				list.add(account_id);
				list.add(0L);
				PlayerViewVO vo = centerRMIServer.rmiInvoke(RMICmd.CLUB_LOG_PLAYER_INFO, list);
				if (vo != null) {
					logPlayer = new ClubLogPlayer();
					logPlayer.setAccountId(account_id);
					clubLogPlayerMap.put(account_id, logPlayer);
					logPlayer.setRegisterDate(vo.getCreate_time());
					logPlayer.setFirstJoinClubDate(vo.getFirstJoinClubTime());
				}
			}
			if (logPlayer != null && MyDateUtil.isSameDay(logPlayer.getRegisterDate())) {
				logModel.setRegistAndPlayNum(logModel.getRegistAndPlayNum() + 1);
			}
		}
	}

	public static void addActiveTableCount() {
		logModel = getDataLogModel();
		logModel.setClubActiveTableCount(logModel.getClubActiveTableCount() + 1);
	}

	public static void addActiveClubNum(int clubId) {
		if (!activeClubs.contains(clubId)) {
			activeClubs.add(clubId);
			logModel = getDataLogModel();
			logModel.setActiveClubNum(logModel.getActiveClubNum() + 1);
		}
	}

	public static void addClubCompleteParentBrandCount() {
		logModel = getDataLogModel();
		logModel.setCompleteParentBrandCount(logModel.getCompleteParentBrandCount() + 1);
	}

	public static void addClubChildBrandCount(int count) {
		logModel = getDataLogModel();
		logModel.setChildBrandCount(logModel.getChildBrandCount() + count);
	}

	public static void addClubCostGold(int count) {
		logModel = getDataLogModel();
		logModel.setTotalCostGold(logModel.getTotalCostGold() + count);
	}

	public static void addNewClubCount() {
		logModel = getDataLogModel();
		logModel.setNewClubCount(logModel.getNewClubCount() + 1);
	}

	private static ClubDataLogModel getDataLogModel() {
		if (logModel == null) {
			logModel = MongoDBServiceImpl.getInstance().getClubDataLog();
			if (!Strings.isNullOrEmpty(logModel.getGameInfoData())) {
				try {
					List<ClubGameInfoData> list = JSON.parseArray(logModel.getGameInfoData(), ClubGameInfoData.class);
					if (list != null) {
						for (ClubGameInfoData data : list) {
							gameInfoDataMap.put(data.getGameId(), data);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		// 跨天时
		if (!MyDateUtil.isSameDay(logModel.getCreate_time())) {
			logModel = MongoDBServiceImpl.getInstance().getClubDataLog();
		}
		return logModel;
	}

	/**
	 * 重置每日统计类的数据
	 */
	public static void resetDailyData() {
		activePlayers.clear();
		gameInfoDataMap.clear();
		gamePlayerNumMap.clear();
		activeClubs.clear();
	}

	protected static void statClubMemberCountSection(Map<Integer, ClubMemberCountSectionData> clubMemCountSectionMap, Club club) {
		int count = club.members.size();
		int key = -1;
		if (count >= 1 && count <= 10) {
			key = ClubMemberCountSectionType.ONE;
		} else if (count <= 20) {
			key = ClubMemberCountSectionType.TWO;
		} else if (count <= 30) {
			key = ClubMemberCountSectionType.THREE;
		} else if (count <= 40) {
			key = ClubMemberCountSectionType.FOUR;
		} else if (count <= 50) {
			key = ClubMemberCountSectionType.FIVE;
		} else if (count <= 100) {
			key = ClubMemberCountSectionType.SIX;
		} else if (count <= 200) {
			key = ClubMemberCountSectionType.SEVEN;
		} else if (count <= 500) {
			key = ClubMemberCountSectionType.EIGHT;
		} else {
			key = ClubMemberCountSectionType.NINE;
		}
		ClubMemberCountSectionData data = clubMemCountSectionMap.get(key);
		if (data != null) {
			data.setClubCount(data.getClubCount() + 1);
			data.setGameCount(data.getGameCount() + club.gameCount);
		}
	}

	/**
	 * 子游戏数据统计
	 */
	public static void statGameInfoData(ClubGameOverProto clubGameOverProto) {
		int gameId = clubGameOverProto.getGameId();
		ClubGameInfoData data = gameInfoDataMap.get(gameId);
		if (data == null) {
			data = new ClubGameInfoData();
			data.setGameId(gameId);
			gameInfoDataMap.put(gameId, data);
		}
		data.setGameCount(data.getGameCount() + 1);
		data.setPlayerCount(data.getPlayerCount() + clubGameOverProto.getPlayersCount());
		Set<Long> players = gamePlayerNumMap.get(gameId);
		if (players == null) {
			players = Sets.newHashSet();
			gamePlayerNumMap.put(gameId, players);
		}
		int count = 0;
		for (GamePlayerProto proto : clubGameOverProto.getPlayersList()) {
			if (!players.contains(proto.getAccountId())) {
				players.add(proto.getAccountId());
				count++;
			}
		}
		data.setPlayerNum(data.getPlayerNum() + count);
		data.addGameRoundCount(clubGameOverProto.getGameRound());
	}
}
