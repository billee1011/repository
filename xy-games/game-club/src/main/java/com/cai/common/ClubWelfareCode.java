package com.cai.common;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/9/14 14:39
 */
public interface ClubWelfareCode {
	//=========================================
	/**
	 * 奖励类型(1-真实奖励 2-展示奖励)
	 */
	int REWARD_TYPE_REAL = 1;
	/**
	 * 奖励类型(1-真实奖励 2-展示奖励)
	 */
	int REWARD_TYPE_SHOW = 2;

	//=========================================
	/**
	 * 奖励权重(1-常规奖励 2-辅助奖励)
	 */
	int REWARD_WEIGHT_TYPE_NORMAL = 1;
	/**
	 * 奖励权重(1-常规奖励 2-辅助奖励)
	 */
	int REWARD_WEIGHT_TYPE_ASSIST = 2;

	//=========================================
	/**
	 * 玩家福卡变动类型-抽奖
	 */
	int MEMBER_WELFARE_CHANGE_LOTTERY = 1;
	/**
	 * 玩家福卡变动类型-修改
	 */
	int MEMBER_WELFARE_CHANGE_MODIFY = 2;
}
