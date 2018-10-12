
/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.cai.common.define.EGameType;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.GameRoomRecord;
import com.cai.common.domain.Player;
import com.cai.common.util.AbstractServer;
import com.cai.common.util.Pair;
import com.cai.common.util.PropertiesUtil;
import com.cai.common.util.TimeUtil;
import com.cai.service.MongoDBServiceImpl;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.xianyi.framework.core.transport.event.IOEventListener;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

/**
 * 
 * 生成俱乐部大赢家次数
 *
 * @author wu_hc date: 2018年5月4日 下午3:39:20 <br/>
 */
public final class ClubRecordMain extends AbstractServer {

	static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public static void main(String[] args) {
		ClubRecordMain server = new ClubRecordMain();
		try {

			////////// 需要生成的游戏id ,0表示不限制游戏类型//////////////

			final String extFileName = ".txt"; // xlsx
			int gameId = 0; // @see
							// com.cai.common.define.EGameType

			/////////// 需要生成的俱乐部id列表 ///////////
			List<Integer> clubids = Lists.newArrayList(9268818);

			// 1基础组件启动
			server.initConfig();
			server.startSpring();

			final Date curDate = new Date();

			clubids.forEach((clubId) -> {
				StringBuilder sb = new StringBuilder();

				sb.append(String.format("------------------clubId:%d--------生成时间:%s---------", clubId, format.format(curDate))).append("\n");
				List<Pair<Long, Long>> dates = Lists.newArrayList();

				// 0：当前天，-N：往后N天
				for (int i = 0; i <= 2; i++) {
					dates.add(Pair.of(TimeUtil.getTimeStart(curDate, 0 - i), TimeUtil.getTimeEnd(curDate, 0 - i)));
				}
				dates.forEach(date -> {
					server.dataBuild(clubId, date.getFirst().longValue(), date.getSecond().longValue(), gameId, sb);
				});

				try {
					FileUtils.writeStringToFile(new File("record/clubId_" + clubId + extFileName), sb.toString(), Charset.defaultCharset(), false);
				} catch (IOException e) {
					e.printStackTrace();
				}

				System.out.println(String.format("-------- clubId:%d ---------- finishi!!!", clubId));
				try {
					Thread.sleep(1 * 1000L);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			});

			System.exit(1);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public void stop() throws Exception {
	}

	@Override
	protected void config(PropertiesUtil prop) {

	}

	@Override
	protected Class<? extends IOEventListener<C2SSession>> acceptorListener() {
		return null;
	}

	@Override
	protected void debugCmdAccept(String cmd) {
	}

	/**
	 * 
	 * @param clubId
	 * @param startTime
	 * @param endTime
	 */
	private void dataBuild(int clubId, long startTime, long endTime, int gameId, StringBuilder sb) {

		final boolean outrecord = true;
		final boolean outwinCount = true;
		final boolean outgameCount = true;
		final boolean outTire = true;
		long accountId = 0L;
		List<Record> recordMap = Lists.newArrayList();
		Map<Long, Entry> winMap = Maps.newHashMap();
		Map<Long, Entry> timeMap = Maps.newHashMap();
		Map<Long, Entry> tireMap = Maps.newHashMap();

		int goldCostCount = 0, exclusiveCostCount = 0, gameCount = 0;

		// 俱乐部每日消耗数据

		Map<String, Object> param = null;

		if (gameId > 0) {
			param = Maps.newHashMap();
			param.put("gameId", gameId);
		}
		List<BrandLogModel> brandLogModels = MongoDBServiceImpl.getInstance().getClubParentBrandList(null, clubId, startTime, endTime, param);

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
					timeMap.put(player.getAccount_id(), new Entry(player.getAccount_id(), player.getNick_name(), 1));
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
					winMap.put(player.getAccount_id(), new Entry(player.getAccount_id(), player.getNick_name(), 1));
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
							new Entry(player.getAccount_id(), player.getNick_name(), (int) (grr.get_player().game_score[i])));
				} else {
					tireMap.get(player.getAccount_id()).value += (int) (grr.get_player().game_score[i]);
				}
			}
		}
		if (gameId > 0) {
			sb.append("游戏[").append(EGameType.getEGameType(gameId).getName()).append("]");
		} else {
			sb.append("游戏[全部]");
		}
		sb.append(String.format("起始时间:%s,结束时间:%s", format.format(startTime), format.format(endTime))).append("\n");
		sb.append(String.format("[ 房卡:%-6d 专属豆:%-6d 局数:%d ]\n", goldCostCount, exclusiveCostCount, gameCount));

		if (outrecord) {
			sb.append("战绩:\n");
			recordMap.forEach((record) -> {
				sb.append(record).append("\n");
			});
			sb.append("\n");
		}

		if (outgameCount) {
			sb.append("局数:\n");
			// 局数
			for (Map.Entry<Long, Entry> entry : timeMap.entrySet()) {
				sb.append(entry.getValue()).append("\n");
			}

			sb.append("\n");
		}

		if (outwinCount) {
			sb.append("大赢家\n");
			// 大赢家
			for (Map.Entry<Long, Entry> entry : winMap.entrySet()) {
				sb.append(entry.getValue()).append("\n");
			}
		}

		if (outTire) {
			// 疲劳值
			sb.append("\n");
			sb.append("疲劳值\n");
			for (Map.Entry<Long, Entry> entry : tireMap.entrySet()) {
				sb.append(entry.getValue()).append("\n");
			}
		}

		sb.append("--------------------- 这是一条分界线-----------------------\n\n");

	}

	static final class Entry {
		public long accountId;
		public String accountName;
		public int value;

		/**
		 * @param accountId
		 * @param accountName
		 * @param value
		 */
		public Entry(long accountId, String accountName, int value) {
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
			sb.append("时间:").append(format.format(model.getCreate_time())).append("\t房间号:").append(grr.getRoom_id()).append("\n");

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
