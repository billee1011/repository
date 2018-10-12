package com.cai.constant;

/**
 * 
 *
 * @author zhanglong date: 2018年6月25日 下午8:01:29
 */
public interface ClubMatchCode {
	/**
	 * 创建比赛
	 */
	int CREATE = 1;

	/**
	 * 删除比赛
	 */
	int DEL = 2;

	/**
	 * 自主参赛
	 */
	int ATTEND = 3;

	/**
	 * 退赛
	 */
	int EXIT = 4;

	/**
	 * 设置参赛成员
	 */
	int SET_ATTEND = 5;

	/**
	 * 开赛失败
	 */
	int START_FAIL = 6;

	/**
	 * 开赛前十分钟通知
	 */
	int WILL_START = 7;

	/**
	 * 牌桌游戏结束
	 */
	int TABLE_GAME_END = 8;
	
	/**
	 * 被设置为参赛成员
	 */
	int BE_SET_ATTEND = 9;
	
	/**
	 * 俱乐部解散导致比赛解散
	 */
	int DISBAND_MATCH = 10;
	
	/**
	 * 管理员设置的退赛
	 */
	int EXIT_BY_MANAGER_SET = 11;

	/**
	 * 比赛开赛
	 */
	int MATCH_START = 12;

	/**
	 * 管理员解散自建赛牌桌
	 */
	int MANAGER_DISBAND_TABLE = 13;

}
