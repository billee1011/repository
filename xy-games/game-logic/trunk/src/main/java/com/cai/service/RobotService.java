package com.cai.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;

import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.coin.CoinPlayer;
import com.cai.common.constant.GameConstants;
import com.cai.common.domain.Event;
import com.cai.common.domain.Player;
import com.cai.common.domain.SysParamModel;
import com.cai.common.type.SystemType;
import com.cai.common.util.CSVUtil;
import com.cai.common.util.WRSystem;
import com.cai.core.MonitorEvent;
import com.cai.dictionary.SysParamServerDict;
import com.cai.domain.Session;
import com.cai.match.MatchPlayer;

public class RobotService extends AbstractService {
	private static Logger logger = LoggerFactory.getLogger(RobotService.class);
	private final static int DEFAULT_MIN_ROBOT_ID = 145000 - 1; // 145000 -
																// 150400
																// 机器人ID范围
	private final static int DEFAULT_MAX_ROBOT_ID = 150400 - 1; // 145000 -
																// 150400
																// 机器人ID范围
	private final static RobotService instance = new RobotService();

	private static List<RobotBase> robotList = new ArrayList<>();
	private volatile static boolean isLoad = false; // 重新加载机器人

	private int curStartIndex = DEFAULT_MIN_ROBOT_ID;
	private int curEndIndex = DEFAULT_MAX_ROBOT_ID;

	public RobotService() {
	}

	public static RobotService getInstance() {
		return instance;
	}

	@Override
	protected void startService() {
	}

	/**
	 * 初始化机器人数据
	 */
	public void init() {
		int startIndex = 0;
		int endIndex = 0;
		try {
			SysParamModel sysModel = SysParamServerDict.getInstance()
					.getSysParamModelDictionaryByGameId(SystemType.ROBOT_ID).get(SystemType.ROBOT_ID);
			startIndex = sysModel.getVal1();
			endIndex = sysModel.getVal2();
			if (startIndex == curStartIndex && endIndex == curEndIndex) {
				return;
			}
		} catch (Exception e) {
			startIndex = DEFAULT_MIN_ROBOT_ID;
			endIndex = DEFAULT_MAX_ROBOT_ID;
		}
		initRobots(startIndex, endIndex);
	}

	public void initRobots(int startIndex, int endIndex) {
		try {
			List<RobotBase> tempRobotList = new ArrayList<>();
			List<String> temp = CSVUtil.readDirtyCSVFile(WRSystem.HOME + "../common/robot.txt");
			long accountId = 0;
			for (int i = 0; i < temp.size(); i++) {
				String name = temp.get(i);
				String icon = "http://game.51yeyou.cc/robot/";
				if (i < 10) {
					icon = icon + "0" + i + ".jpg";
				} else {
					icon = icon + i + ".jpg";
				}
				accountId = startIndex + i;
				if (accountId >= endIndex) {
					continue;
				}
				tempRobotList.add(new RobotBase(name, icon, accountId));
			}
			robotList = tempRobotList;
			isLoad = true;
			curStartIndex = startIndex;
			curEndIndex = endIndex;
		} catch (IOException e) {
			logger.error("初始化机器人失败");
		}
	}

	@Override
	public MonitorEvent montior() {
		return null;
	}

	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {

	}

	@Override
	public void sessionCreate(Session session) {

	}

	@Override
	public void sessionFree(Session session) {

	}

	@Override
	public void dbUpdate(int _userID) {
	}

	public RobotRandom getRobotRandom() {
		return new RobotRandom(this);
	}

	public static class RobotRandom {
		private List<RobotBase> names;

		public RobotRandom(RobotService robotService) {
			names = new ArrayList<>(robotList);
			Collections.shuffle(names);
		}

		public List<MatchPlayer> getRandomMatchPlayers(int count, int id) {
			List<MatchPlayer> players = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				MatchPlayer player = new MatchPlayer();
				RobotBase next = random();
				player.setAccount_id(next.accountId);
				player.setProxy_session_id(player.getAccount_id());
				player.setGold(RandomUtils.nextInt(2000));
				player.setEnter(true);
				player.setAccount_icon(next.icon);
				player.setAccount_ip("");
				player.setAccount_ip_addr("");
				player.setNick_name(next.name);
				player.setRobot(true);
				player.setId(id);
				player.setSex(RandomUtils.nextInt(2) + 1);
				player.set_seat_index(GameConstants.INVALID_SEAT);
				player.setRoom_id(0);
				player.setMoney(RandomUtils.nextInt(15000) + 5000);
				player.setMatch(true);
				players.add(player);
			}
			return players;
		}

		public List<CoinPlayer> getRandomCoinPlayers(int count, int id, int minLimit, int maxLimit) {
			List<CoinPlayer> players = new ArrayList<>();
			int midValue = maxLimit - minLimit;
			if (midValue > 130000) {
				midValue = 130000;
			}
			for (int i = 0; i < count; i++) {
				CoinPlayer player = new CoinPlayer();
				RobotBase next = random();
				player.setAccount_id(next.accountId);
				player.setProxy_session_id(player.getAccount_id());
				player.setGold(RandomUtils.nextInt(2000));
				player.setAccount_icon(next.icon);
				player.setAccount_ip("");
				player.setAccount_ip_addr("");
				player.setNick_name(next.name);
				player.setRobot(true);
				player.setSex(RandomUtils.nextInt(2) + 1);
				player.set_seat_index(GameConstants.INVALID_SEAT);
				player.setRoom_id(0);
				player.setMoney(RandomUtils.nextInt(midValue) + minLimit);
				players.add(player);
			}
			return players;
		}

		public Player getRandomNomalPlayer(int seat_index, int Room_id) {
			Player new_player = new Player();
			RobotBase next = random();
			new_player.setAccount_id(next.accountId);
			new_player.setProxy_session_id(new_player.getAccount_id());
			new_player.setGold(RandomUtils.nextInt(2000));
			new_player.setAccount_icon(next.icon);
			new_player.setAccount_ip("");
			new_player.setAccount_ip_addr("");
			new_player.setNick_name(next.name);
			new_player.setId(0);
			new_player.setRobot(true);
			new_player.setSex(RandomUtils.nextInt(2) + 1);
			new_player.set_seat_index(seat_index);
			new_player.setRoom_id(Room_id);
			return new_player;
		}

		public RobotBase random() {

			if (isLoad || names.size() <= 0) {
				isLoad = false;
				// 重复循环了
				names = new ArrayList<>(robotList);
				Collections.shuffle(names);
			}

			return names.remove(0);
		}
	}

	private static class RobotBase {
		public RobotBase(String name, String icon, long accountId) {
			this.name = name;
			this.icon = icon;
			this.accountId = accountId;
		}

		private String name;
		private String icon;
		private long accountId;
	}
}
