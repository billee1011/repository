/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.utils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.ClubMemberRecordModel;
import com.cai.common.domain.GameRoomRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.log.ClubScoreMsgLogModel;
import com.cai.common.util.XYRange;
import com.cai.common.util.GlobalExecutor;
import com.cai.common.util.TimeUtil;
import com.cai.constant.Club;
import com.cai.service.ClubService;
import com.cai.service.MongoDBServiceImpl;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * 
 * 俱乐部局数/大赢家/疲劳值统计修复工具类
 * 
 * @author wu_hc date: 2018年5月13日 下午2:14:16 <br/>
 */
public final class ClubRecordRepairUtil {

	/**
	 * 日志
	 */
	protected static final Logger logger = LoggerFactory.getLogger(ClubRecordRepairUtil.class);

	/**
	 * 
	 * @param clubId
	 * @param accountId
	 * @param dayRange
	 * @param fromBinlog
	 */
	public static void repair(int clubId, long accountId, XYRange dayRange, boolean fromBinlog) {

		for (int day = dayRange.getBegin(); day <= dayRange.getEnd(); day++) {
			final int day_ = day;
			GlobalExecutor.schedule(() -> {
				ClubRecordRepairUtil.repair_(clubId, accountId, day_, fromBinlog);
				logger.info("修复俱乐部[{}] 玩家[{}] 天数[ {} ]", clubId, accountId, day_);
			}, day_ * 2000L);
		}
		// 修复疲劳值累计值
		for (int day = dayRange.getEnd(); day >= dayRange.getBegin(); day--) {
			final int day_ = day;
			GlobalExecutor.schedule(() -> {
				ClubRecordRepairUtil.repairAccuTire(clubId, accountId, day_);
				logger.info("修复累计疲劳值 俱乐部[{}] 玩家[{}] 天数[ {} ]", clubId, accountId, day_);
			}, day_ * 2000L);
		}
	}

	private static void repairAccuTire(int clubId, long accountId, int day) {
		if (day < 1 || day > 8 || accountId < 0) {
			return;
		}
		Club club = ClubService.getInstance().getClub(clubId);
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {
			club.members.forEach((account_id, member) -> {
				if (accountId > 0 && accountId != account_id) {
					return;
				}

				ClubMemberRecordModel model = club.getMemberRecordModelByDay(day, member);
				if (null == model) {
					return;
				}
				model.setAccuTireValue(model.getTireValue());
				if (!club.isTireDailyReset()) {
					ClubMemberRecordModel yesterdayModel = club.getMemberRecordModelByDay(day + 1, member);
					if (yesterdayModel != null) {
						model.setAccuTireValue(yesterdayModel.getAccuTireValue() + model.getTireValue());
					}
				}
			});
		});
	}

	/**
	 * 
	 * @param clubId
	 * @param accountId
	 *            0表示所有成员
	 */
	private static void repair_(int clubId, long accountId, int day, boolean fromBinlog) {
		if (day < 1 || day > 8 || accountId < 0) {
			return;
		}
		Club club = ClubService.getInstance().getClub(clubId);
		if (null == club) {
			return;
		}
		List<Record> recordMap = Lists.newArrayList();
		final Map<Long, RecordEntry> winMap = Maps.newHashMap();
		final Map<Long, RecordEntry> timeMap = Maps.newHashMap();
		final Map<Long, RecordEntry> tireMap = Maps.newHashMap();

		// 修改记录中得到的值
		final Map<Long, RecordEntry> tireModifyMap = Maps.newHashMap();

		int goldCostCount = 0, exclusiveCostCount = 0, gameCount = 0;

		// 俱乐部每日消耗数据
		Date curDate = new Date();
		long startTime = TimeUtil.getTimeStart(curDate, 1 - day), endTime = TimeUtil.getTimeEnd(curDate, 1 - day);
		List<BrandLogModel> brandLogModels = MongoDBServiceImpl.getInstance().getClubParentBrandList(null, clubId, startTime, endTime, null);

		// 修改记录，类似于MYSQL的 binlog
		if (fromBinlog) {
			List<ClubScoreMsgLogModel> midifyLogList = MongoDBServiceImpl.getInstance().getClubScoreMsgLogModelList(clubId, startTime, endTime);
			if (null != midifyLogList && !midifyLogList.isEmpty()) {
				midifyLogList.forEach(log -> {

					long account = log.getTargetAccountId();

					RecordEntry entry = tireModifyMap.get(account);
					if (null == entry) {
						entry = new RecordEntry(account, "", (log.getNewValue() - log.getOldValue()));
						tireModifyMap.put(account, entry);
					} else {
						tireModifyMap.get(account).value += (log.getNewValue() - log.getOldValue());
					}
				});
			}
		}

		for (BrandLogModel branModel : brandLogModels) {
			GameRoomRecord grr = GameRoomRecord.to_Object(branModel.getMsg());

			gameCount++;
			if (branModel.isExclusiveGold()) {
				exclusiveCostCount += branModel.getGold_count();
			} else {
				goldCostCount += branModel.getGold_count();
			}

			// 战绩
			Record re = new Record(branModel, grr);
			if (accountId == 0L || (accountId > 0 && re.containPlayer(accountId))) {
				recordMap.add(re);
			}

			// 1轮数
			for (int i = 0; i < grr.getPlayers().length; i++) {
				Player player = grr.getPlayers()[i];
				if (null == player || (accountId > 0 && accountId != player.getAccount_id())) {
					continue;
				}
				if (!timeMap.containsKey(player.getAccount_id())) {
					timeMap.put(player.getAccount_id(), new RecordEntry(player.getAccount_id(), player.getNick_name(), 1));
				} else {
					timeMap.get(player.getAccount_id()).value++;
				}
			}
			// 2大赢家
			int bigWinIndex = 0;
			float scoreTmp = 0.0f;
			for (int i = 0; i < grr.get_player().game_score.length; i++) {
				if (grr.get_player().game_score[i] > scoreTmp) {
					scoreTmp = grr.get_player().game_score[i];
					bigWinIndex = i;
				}
			}

			// 多大赢家
			Set<Integer> bigWinIdxSet = Sets.newHashSet(bigWinIndex);
			for (int i = 0; i < grr.get_player().game_score.length; i++) {
				if (grr.get_player().game_score[i] == scoreTmp) {
					bigWinIdxSet.add(i);
				}
			}

			bigWinIdxSet.forEach((idx) -> {
				Player player = grr.getPlayers()[idx];
				if (null == player || (accountId > 0 && accountId != player.getAccount_id())) {
					return;
				}
				if (!winMap.containsKey(player.getAccount_id())) {
					winMap.put(player.getAccount_id(), new RecordEntry(player.getAccount_id(), player.getNick_name(), 1));
				} else {
					winMap.get(player.getAccount_id()).value++;
				}
			});

			// 3疲劳值
			for (int i = 0; i < grr.getPlayers().length; i++) {
				Player player = grr.getPlayers()[i];
				if (null == player || (accountId > 0 && accountId != player.getAccount_id())) {
					continue;
				}
				if (!tireMap.containsKey(player.getAccount_id())) {
					tireMap.put(player.getAccount_id(),
							new RecordEntry(player.getAccount_id(), player.getNick_name(), (int) (grr.get_player().game_score[i])));
				} else {
					tireMap.get(player.getAccount_id()).value += (int) (grr.get_player().game_score[i]);
				}
			}
		}

		// 修复逻辑
		club.runInReqLoop(() -> {

			club.members.forEach((account_id, member) -> {
				if (accountId > 0 && accountId != account_id) {
					return;
				}

				// 没有这个三个值，直接跳过
				if (!timeMap.containsKey(account_id) && !winMap.containsKey(account_id) && !tireMap.containsKey(account_id)
						&& !tireModifyMap.containsKey(account_id)) {
					return;
				}

				ClubMemberRecordModel model = club.getMemberRecordModelByDay(day, member);
				if (null == model) {
					return;
				}

				RecordEntry entry = null;

				// 局数修复
				entry = timeMap.get(account_id);
				if (null != entry) {
					model.setGameCount(entry.value);
				} else {
					model.setGameCount(0);
				}

				// 大赢家次数修复
				entry = winMap.get(account_id);
				if (null != entry) {
					model.setBigWinCount(entry.value);
				} else {
					model.setBigWinCount(0);
				}

				// 疲劳值修复
				entry = tireMap.get(account_id);

				RecordEntry binlogEntry = tireModifyMap.get(account_id);
				int binlogValue = 0;
				if (null != binlogEntry) {
					binlogValue = binlogEntry.value;
				}

				if (null != entry) {
					model.setTireValue(entry.value + binlogValue);
				} else {
					model.setTireValue(0 + binlogValue);
				}
			});

		});
		System.out.println(
				String.format("goldCostCount:%-10d exclusiveCostCount:%-10d gameCount:%-10d ", goldCostCount, exclusiveCostCount, gameCount));
	}

	static final class RecordEntry {
		public long accountId;
		public String accountName;
		public int value;

		/**
		 * @param accountId
		 * @param accountName
		 * @param value
		 */
		public RecordEntry(long accountId, String accountName, int value) {
			this.accountId = accountId;
			this.accountName = accountName;
			this.value = value;
		}

		@Override
		public String toString() {
			return desc();
		}

		public final String desc() {
			return String.format("玩家ID:%-20d 值:%-20d 玩家昵称:%-1s", accountId, value, accountName);
		}
	}

	static final class Record {
		public BrandLogModel model;
		public GameRoomRecord grr;

		/**
		 * @param model
		 * @param record
		 */
		public Record(BrandLogModel model, GameRoomRecord record) {
			this.model = model;
			this.grr = record;
		}

		public boolean containPlayer(long accountId) {
			for (int i = 0; i < grr.getPlayers().length; i++) {
				Player player = grr.getPlayers()[i];
				if (null != player && player.getAccount_id() == accountId) {
					return true;
				}
			}
			return false;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("时间:").append(model.getCreate_time()).append("\t房间号:").append(grr.getRoom_id()).append("\n");

			for (int i = 0; i < grr.getPlayers().length; i++) {
				Player player = grr.getPlayers()[i];
				if (null == player) {
					continue;
				}

				sb.append("ID:").append(player.getAccount_id()).append("\t昵称:").append(player.getNick_name()).append("\t").append("分数:")
						.append(grr.get_player().game_score[i]).append("\n");

			}
			return sb.toString();
		}

	}
}
