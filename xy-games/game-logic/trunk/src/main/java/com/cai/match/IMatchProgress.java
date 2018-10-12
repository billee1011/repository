package com.cai.match;

import com.cai.game.AbstractRoom;

public interface IMatchProgress {

	public static final int SCORE_OUT = 1; // 打立出局
	public static final int FIXED = 2; // 定局赛
	public static final int SWISS_SHIFT = 3; // 瑞士移位

	/**
	 * @param room
	 * @param table
	 * @param player
	 * @return 是否被淘汰
	 */
	boolean onGameOver(AbstractRoom room, MatchTable table, MatchPlayer player, long ctime);

	// 每秒update
	void onUpdate(long ctime, MatchTable table);

	// 当前进度结束
	void overProgress(MatchTable table);

	// 初始化进度 牌局倍数， 弱化上局分数
	void onInitProgress(MatchTable table, MatchProgressInfo curProgress);

	// 添加并返回当前轮数
	int addMatchTypeRound(MatchPlayer player, MatchTable table, MatchProgressInfo curProgress);

	int getMatchTypeRound(MatchPlayer player, MatchTable table, MatchProgressInfo curProgress);

	int getType();

	// 是否进入下一轮
	boolean isNeedChangeProgress(MatchTable table);

	// 是否已打完当前轮，等待下一轮排行
	boolean isWaitRank(MatchTable table, MatchPlayer player);

	public static IMatchProgress create(int type) {
		switch (type) {
		case SCORE_OUT:

			return MatchScoreOutProgress.getInstance();
		case FIXED:

			return MatchFixProgress.getInstance();
		case SWISS_SHIFT:

			return MatchSwissProgress.getInstance();

		default:
			throw new NullPointerException("找不到该比赛赛制类型");
		}
	}

}
