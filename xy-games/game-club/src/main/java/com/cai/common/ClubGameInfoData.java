package com.cai.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ClubGameInfoData implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 子游戏Id
	 */
	private int gameId;
	/**
	 * 牌局数
	 */
	private int gameCount;

	/**
	 * 玩牌人数
	 */
	private int playerNum;

	/**
	 * 玩牌人次
	 */
	private int playerCount;

	/**
	 * 开局时选择的局数统计(格式：选择的局数,该局数的次数|...)
	 */
	private String gameRoundCountData;

	private transient Map<Integer, Integer> gameRoundCountDataMap = new HashMap<>();

	public int getGameId() {
		return gameId;
	}

	public void setGameId(int gameId) {
		this.gameId = gameId;
	}

	public int getGameCount() {
		return gameCount;
	}

	public void setGameCount(int gameCount) {
		this.gameCount = gameCount;
	}

	public int getPlayerNum() {
		return playerNum;
	}

	public void setPlayerNum(int playerNum) {
		this.playerNum = playerNum;
	}

	public int getPlayerCount() {
		return playerCount;
	}

	public void setPlayerCount(int playerCount) {
		this.playerCount = playerCount;
	}

	public String getGameRoundCountData() {
		return gameRoundCountData;
	}

	public void setGameRoundCountData(String gameRoundCountData) {
		this.gameRoundCountData = gameRoundCountData;
	}

	public void addGameRoundCount(int round) {
		if (!this.gameRoundCountDataMap.containsKey(round)) {
			gameRoundCountDataMap.put(round, 0);
		}
		gameRoundCountDataMap.put(round, gameRoundCountDataMap.get(round) + 1);
	}

	public void encodeGameRoundCountData() {
		StringBuilder buffer = new StringBuilder();
		for (Integer key : gameRoundCountDataMap.keySet()) {
			buffer.append(key).append(",").append(gameRoundCountDataMap.get(key)).append("|");
		}
		if (buffer.length() > 0) {
			buffer.deleteCharAt(buffer.length() - 1);
		}
		this.gameRoundCountData = buffer.toString();
	}
}
