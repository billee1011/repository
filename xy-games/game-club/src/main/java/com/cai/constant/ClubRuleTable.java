/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.constant;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.S2SCmd;
import com.cai.common.define.ERoomSettingStatus;
import com.cai.common.define.ERoomStatus;
import com.cai.common.define.EServerType;
import com.cai.common.define.EWealthCategory;
import com.cai.common.domain.ClubRuleCostLogModel;
import com.cai.common.domain.ClubRuleModel;
import com.cai.common.domain.ClubRuleRecordModel;
import com.cai.common.domain.GameGroups;
import com.cai.common.domain.RmiDTO;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.type.ClubRecordDayType;
import com.cai.common.util.FilterUtil;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RedisKeyUtil;
import com.cai.common.util.RoomComonUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.TimeUtil;
import com.cai.config.ClubCfg;
import com.cai.config.SystemConfig;
import com.cai.dictionary.GameGroupRuleDict;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.redis.service.RedisService;
import com.cai.service.ClubService;
import com.cai.service.SessionService;
import com.cai.tasks.db.ClubRuleRecordInsertDBTask;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import protobuf.clazz.ClubMsgProto.ClubRuleTableGroupProto;
import protobuf.clazz.s2s.ClubServerProto.ClubRoomStatusProto;

import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;

/**
 * @author wu_hc date: 2017年10月30日 下午3:00:19 <br/>
 */
public final class ClubRuleTable {

	/**
	 * 规则/玩法
	 */
	private final ClubRuleModel ruleModel;

	/**
	 * 人数上限
	 */
	private int playerLimit;

	/**
	 * 玩法描述
	 */
	private String ruleDesc;

	/**
	 * 索引->桌子
	 */
	private final List<ClubTable> tables = Lists.newArrayList();

	/**
	 * 隐藏桌子中，当前正在展示的桌子，配合隐藏桌子设置使用
	 */
	private final Map<Integer, ClubTable> showTables = Maps.newHashMap();

	// 当前场景的位置
	private final ClubSeat sceneTag;

	private Map<Long, ClubRuleRecordModel> ruleRecordMap = Maps.newHashMap();

	private ClubRuleCostLogModel costLogModel;

	public ClubRuleTable(ClubRuleModel ruleModel, boolean fromDBInit) {
		this.ruleModel = ruleModel;
		if (fromDBInit) {
			this.playerLimit = RoomComonUtil.getMaxNumber(ruleModel.getRuleParams());
		} else {
			updatePlayerLimit();
		}

		this.initialTables();
		sceneTag = ClubSeat.newSeat(ruleModel.getClub_id(), ruleModel.getId());
	}

	/**
	 * 有玩法变化
	 */
	public ClubRuleTable updateRule() {
		updatePlayerLimit();
		this.roomStatusUpdate(ERoomStatus.UPDATE_RULE);
		return this;
	}

	/**
	 * 删除玩法/包间
	 */
	public ClubRuleTable deleteRule() {
		this.roomStatusUpdate(ERoomStatus.DELETE_RULE);
		return this;
	}

	/**
	 * 添加包间
	 */
	public ClubRuleTable addRule() {
		this.roomStatusUpdate(ERoomStatus.ADD_RULE);
		return this;
	}

	/**
	 * 刷新玩法最大人数
	 */
	private void updatePlayerLimit() {
		if (ClubCfg.get().getRuleUpdateSubGameIds().contains(ruleModel.getGame_type_index()) || (SystemConfig.gameDebug == 1)) {
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			int gameId = SysGameTypeDict.getInstance().getGameIDByTypeIndex(ruleModel.getGame_type_index());
			RmiDTO dto = centerRMIServer.getMaxNumberAllotLogicId(gameId, ruleModel.getRuleParams());
			if (dto != null) {
				this.playerLimit = dto.getValue();
				this.ruleDesc = dto.getDesc();
			}
		} else {
			this.playerLimit = RoomComonUtil.getMaxNumber(ruleModel.getRuleParams());
			GameGroups gameGroups = GameGroupRuleDict.getInstance().get(getGameTypeIndex());
			if (null == gameGroups) {
				setRuleDesc(null);
			} else {
				setRuleDesc(GameDescUtil.getGameDesc(this.getClubRuleModel(), gameGroups));
			}
		}
	}

	private void initialTables() {
		for (int i = 0; i < ClubCfg.get().getRuleTableMax(); i++) {
			tables.add(new ClubTable(ruleModel.getClub_id(), ruleModel.getId(), i, ruleModel.getTablePassport(i)));
		}
	}

	public int getRuleId() {
		return ruleModel.getId();
	}

	public List<ClubTable> getTables() {
		return tables;
	}

	public ClubTable getTable(int index) {
		if (index < 0 || index >= ClubCfg.get().getRuleTableMax()) {
			return null;
		}
		return tables.get(index);
	}

	public ClubPlayer getPlayer(int tableIndex, int seatIndex) {
		ClubTable table = getTable(tableIndex);
		return null == table ? null : table.getPlayerBySeat(seatIndex);
	}

	public ClubRuleTableGroupProto.Builder toTablesBuilder(int clubId) {
		ClubRuleTableGroupProto.Builder builder = ClubRuleTableGroupProto.newBuilder();
		builder.setClubId(clubId);
		builder.setRuleId(ruleModel.getId());
		builder.setMaxPlayer(playerLimit);
		for (final ClubTable table : tables) {
			builder.addClubTables(table.toClubTableBuilder());
		}
		return builder;
	}

	/**
	 * @param clubId
	 * @param playerAttrInterestOps 感兴趣的玩家属性
	 * @param hideStartedTable
	 * @return
	 */
	public ClubRuleTableGroupProto.Builder toTablesBuilder(int clubId, int playerAttrInterestOps, boolean hideStartedTable) {
		ClubRuleTableGroupProto.Builder builder = ClubRuleTableGroupProto.newBuilder();
		builder.setClubId(clubId);
		builder.setRuleId(ruleModel.getId());
		builder.setMaxPlayer(playerLimit);
		for (final ClubTable table : tables) {
			if (hideStartedTable && table.isGameStart()) {
				continue;
			}
			builder.addClubTables(table.toClubTableBuilder(playerAttrInterestOps));
		}
		return builder;
	}

	/**
	 * 不满桌的八桌，如果已经达上限，自动拓展
	 */
	public ClubRuleTableGroupProto.Builder toNOTSTARTTablesBuilder(int clubId) {
		ClubRuleTableGroupProto.Builder builder = ClubRuleTableGroupProto.newBuilder();
		builder.setClubId(clubId);
		builder.setRuleId(ruleModel.getId());
		builder.setMaxPlayer(playerLimit);

		ensureShowingTable();

		showTables.forEach((id, table) -> {
			builder.addClubTables(table.toClubTableBuilder());
		});

		return builder;
	}

	public int getPlayerLimit() {
		return playerLimit;
	}

	public int getPlayerCount() {
		int size = 0;
		for (final ClubTable table : tables) {
			size += table.getPlayerSize();
		}
		return size;
	}

	/**
	 * 只要桌子有人就算
	 */
	public int getHasPlayerTableCount() {
		return (int) tables.stream().filter(t -> t.getPlayerSize() > 0).count();
	}

	/**
	 * 开打才算
	 */
	public int getPlayingTableCount() {
		return (int) tables.stream().filter(ClubTable::isGameStart).count();
	}

	public int getGameTypeIndex() {
		return this.ruleModel.getGame_type_index();
	}

	public void roomStatusUpdate(ERoomStatus status) {

		final SessionService sender = SessionService.getInstance();
		tables.forEach((table) -> {

			// 告诉相应的逻辑服[目前就修改&删除 需要通知逻辑服，其他操作暂可不发]
			if ((status == ERoomStatus.UPDATE_RULE || status == ERoomStatus.DELETE_RULE || status == ERoomStatus.DEL) && table.isGameStart()) {
				ClubRoomStatusProto.Builder builder = ClubRoomStatusProto.newBuilder();
				builder.setType(status.status());
				builder.setRoomId(table.getRoomId());
				sender.sendMsg(EServerType.LOGIC, table.getLogicIndex(), PBUtil.toS2SResponse(S2SCmd.C_2_LOGIC_ROOM_STATUS, builder));
			}

			if (table.getRoomId() > 0 && table.getCurRound() > 0) {
				// 删除俱乐部房间redis数据
				RedisService redisService = SpringService.getBean(RedisService.class);
				redisService.hDel(RedisKeyUtil.clubRoomKey(ruleModel.getClub_id(), ruleModel.getId()), Integer.toString(table.getIndex()));
			} else {
				// 释放未开始的桌子
				table.release();
			}
			table.resetTable();
		});
	}

	/**
	 * 俱乐部解散调用
	 */
	public void disband() {
		roomStatusUpdate(ERoomStatus.DEL);
	}

	/**
	 * 最优选择，用于快速加入桌子
	 */
	public Optional<ClubTable> optimalTable(final String ip, boolean isConceal) {
		List<ClubTable> tbs = new ArrayList<>(isConceal ? showTables.values() : tables);

		Predicate<ClubTable> predicate = (t) -> {
			if (t.getPlayerSize() >= playerLimit) {
				return false;
			}
			if (t.isGameStart() && t.hasSets(ERoomSettingStatus.ROOM_FORBID_HALF_WAY_ENTER)) {
				return false;
			}

			// ip限制新规则
			Integer ipLimitRuleValue = ruleModel.getRuleParams().getRuleValue(GameConstants.GAME_RULE_IP);
			if (null != ipLimitRuleValue && ipLimitRuleValue.intValue() == 1) {
				if (t.hasSameIP(ip)) {
					return false;
				}
			}
			if (t.getPassport() > 0) {
				return false;
			}
			return true;
		};
		tbs = tbs.stream().filter(predicate).sorted(comparingInt(ClubTable::readyCount).thenComparingInt(ClubTable::playerSize).reversed()).limit(1)
				.collect(toList());

		return (null == tbs || tbs.isEmpty()) ? Optional.empty() : Optional.of(tbs.get(0));
	}

	/**
	 * 自动拓展桌子容量,步长
	 */
	private synchronized final void ensureCapacity(int incre) {
		int newCapacity = tables.size() + ((incre & 1) == 0 ? incre : incre + 1);
		for (int i = tables.size(); i < newCapacity; i++) {
			tables.add(new ClubTable(ruleModel.getClub_id(), ruleModel.getId(), i));
		}
	}

	/**
	 * 确保展示桌子的正确性
	 */
	private final void ensureShowingTable() {
		int showCount = ClubCfg.get().getTableCountWhenHideSetting();

		// 确保需要扩容
		List<ClubTable> notStartTables = FilterUtil.filter(tables, (t) -> !t.isGameStart());
		if (notStartTables.size() < showCount) {
			// 补够，多分配两桌配用
			ensureCapacity((showCount - notStartTables.size()) + 2);
		}

		// 初始化或者校正
		if (showTables.isEmpty() || showTables.size() != showCount) {
			showTables.clear();

			Predicate<ClubTable> p = (tb) -> !tb.isGameStart() && tb.getCurRound() == 0;
			List<ClubTable> tbs = tables.stream().filter(p).sorted(comparingInt(ClubTable::playerSize).reversed()).limit(showCount).collect(toList());
			tbs = tbs.stream().sorted(comparingInt(ClubTable::getIndex)).collect(toList());
			for (final ClubTable table : tbs) {
				if (!table.isGameStart()) {
					showTables.put(table.getIndex(), table);
					table.setShow(true);
				}
			}
		}
	}

	/**
	 * 牌桌
	 */
	public synchronized void reloadTable() {
		if (tables.size() >= ClubCfg.get().getRuleTableMax()) {
			return;
		}

		for (int i = tables.size(); i < ClubCfg.get().getRuleTableMax(); i++) {
			tables.add(new ClubTable(ruleModel.getClub_id(), ruleModel.getId(), i));
		}
	}

	/**
	 * 空桌子
	 */
	public Optional<ClubTable> emptyAndNotInShowTable() {
		for (final ClubTable table : tables) {
			if (!showTables.containsKey(table.getIndex()) && table.getPlayerSize() == 0 && table.getCurRound() == 0) {
				return Optional.of(table);
			}
		}
		return Optional.empty();
	}

	/**
	 * 桌子是否要展示
	 */
	public void willShow(ClubTable table, boolean show) {
		table.setShow(show);
		if (show) {
			showTables.put(table.getIndex(), table);
		} else {
			showTables.remove(table.getIndex());
		}
	}

	/**
	 * 隐藏桌子有设置，清除显示牌桌
	 */
	public void setConcealTable(boolean conceal) {
		showTables.clear();

		if (conceal) {
			this.ensureShowingTable();
		}
	}

	public ClubSeat currentSeat() {
		return sceneTag;
	}

	/**
	 * 玩法下的所有成员id
	 */
	public Set<Long> allInTablePlayerIds() {
		Set<Long> ids = Sets.newHashSet();
		tables.forEach(table -> {
			ids.addAll(table.playerIds());
		});
		return ids;
	}

	public Map<Long, ClubRuleRecordModel> getRuleRecordMap() {
		return ruleRecordMap;
	}

	public void setTablePassport(int index, int passport) {
		ClubTable table = getTable(index);
		table.setPassport(passport);
		this.ruleModel.setTablePassport(index, passport);
	}

	public ClubRuleModel getClubRuleModel() {
		return this.ruleModel;
	}

	public String getRuleDesc() {
		return ruleDesc;
	}

	public void setRuleDesc(String ruleDesc) {
		this.ruleDesc = ruleDesc;
	}

	public void updateRuleTableRecord(int gold, long createTime, int wealthCategory) {
		// 判断是否跨天,同一天则更新到今天的数据里,跨天了则更新到昨天的数据里
		Club club = ClubService.getInstance().clubs.get(this.ruleModel.getClub_id());
		if (club != null) {
			List<ClubRuleRecordModel> insertList = new ArrayList<>();
			Date now = new Date();
			long nextRefreshTime = club.getNextRefreshTime();
			long timeKey = 0;
			// 逻辑服是秒数
			if (createTime > 0 && nextRefreshTime - createTime * 1000 > TimeUtil.DAY) { // 昨天
				timeKey = TimeUtil.getTimeStart(now, -1);
			} else { // 今天
				timeKey = TimeUtil.getTimeStart(now, 0);
			}
			if (timeKey > 0) {
				ClubRuleRecordModel model = ruleRecordMap.get(timeKey);
				if (model == null) {
					model = new ClubRuleRecordModel();
					model.setClubId(this.ruleModel.getClub_id());
					model.setRuleId(this.ruleModel.getId());
					model.setRecordDate(new Date(timeKey));
					ruleRecordMap.put(timeKey, model);
					// insert
					insertList.add(model);
				}
				model.setGameCount(model.getGameCount() + 1);
				if (wealthCategory == (int) EWealthCategory.EXCLUSIVE_GOLD.category()) {
					model.setExclusiveGold(model.getExclusiveGold() + gold);
				} else {
					model.setGold(model.getGold() + gold);
				}
			}
			ClubRuleRecordModel total = null;
			for (ClubRuleRecordModel model : ruleRecordMap.values()) {
				if (model.getIsTotal() == 1) {
					total = model;
					break;
				}
			}
			if (total == null) {
				Date defaultDate = TimeUtil.getParsedDate(TimeUtil.DEFAULT_TIME);
				timeKey = TimeUtil.getTimeStart(defaultDate, 0);
				total = new ClubRuleRecordModel();
				total.setClubId(this.ruleModel.getClub_id());
				total.setRuleId(this.ruleModel.getId());
				total.setRecordDate(new Date(timeKey));
				total.setIsTotal(1);
				ruleRecordMap.put(timeKey, total);
				// insert db
				insertList.add(total);
			}
			total.setGameCount(total.getGameCount() + 1);
			if (wealthCategory == (int) EWealthCategory.EXCLUSIVE_GOLD.category()) {
				total.setExclusiveGold(total.getExclusiveGold() + gold);
			} else {
				total.setGold(total.getGold() + gold);
			}

			club.runInDBLoop(new ClubRuleRecordInsertDBTask(insertList));

			//统计包间消耗
			if (null == costLogModel) {
				newCostLogModel();
			}
			if (wealthCategory == (int) EWealthCategory.EXCLUSIVE_GOLD.category()) {
				costLogModel.setExclusiveGold(costLogModel.getExclusiveGold() + gold);
			} else {
				costLogModel.setGold(costLogModel.getGold() + gold);
			}
			costLogModel.setGameCount(costLogModel.getGameCount() + 1);
		}
	}

	public ClubRuleRecordModel getRuleRecordKeyByDay(int day) {
		ClubRuleRecordModel record = null;
		if (day == ClubRecordDayType.ALL) {
			for (ClubRuleRecordModel model : ruleRecordMap.values()) {
				if (model.getIsTotal() == 1) {
					record = model;
					break;
				}
			}
		} else {
			if (day != ClubRecordDayType.EIGHT) {
				Date now = new Date();
				long timeKey = TimeUtil.getTimeStart(now, 1 - day);
				record = ruleRecordMap.get(timeKey);
			}
		}
		return record;
	}

	/**
	 * 创建消耗对象
	 */
	public ClubRuleCostLogModel newCostLogModel() {
		ClubRuleCostLogModel clubRuleCostLogModel = new ClubRuleCostLogModel();
		clubRuleCostLogModel.setClubId(ruleModel.getClub_id());
		clubRuleCostLogModel.setGameTypeIndex(ruleModel.getGame_type_index());
		clubRuleCostLogModel.setGameId(ruleModel.getGame_id());
		clubRuleCostLogModel.setRuleId(ruleModel.getId());
		costLogModel = clubRuleCostLogModel;
		return costLogModel;
	}

	public void setNullCostLogModel() {
		costLogModel = null;
	}

	public ClubRuleCostLogModel getCostLogModel() {
		return costLogModel;
	}
}
