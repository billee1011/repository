package com.cai.common;

public interface ClubTireLogType {
	// 2-俱乐部疲劳值开关操作消息 3-俱乐部疲劳值清零开关操作信息 4-俱乐部疲劳值每日累计信息
	/**
	 * 俱乐部疲劳值开关操作消息
	 */
	int TIRE_SWITCH = 2;
	/**
	 * 俱乐部疲劳值清零开关操作信息
	 */
	int TIRE_RESET_SWITCH = 3;
	/**
	 * 俱乐部疲劳值每日累计信息
	 */
	int TIRE_ACCU = 4;
}
