package com.cai.json;

import java.io.Serializable;

import com.cai.common.domain.WeaveItem;


public class GameEndJson implements Serializable {
	private int card_count[]; //
	private int cards_data[][]; //
	// 结束信息
	private int provide_player[]; // 供应用户
	private int chi_hu_right[]; // 胡牌类型
	private int start_hu_right[]; // 起手胡牌类型
	private int start_hu_score[]; // 起手胡牌分数

	// 积分信息
	private int game_score[]; // 游戏积分
	private int game_tax[]; // 抽水？？？

	private int win_order[]; // 胡牌排名

	// 详细得分
	private int gang_score[];
	private int gang_count[];
	private int lost_fan_shu[][];

	private int left_player;// 逃跑的玩家？？？

	// 组合扑克
	private int weave_count[]; // 组合数目
	private WeaveItem weave_items[][]; // 组合扑克
	int cards_data_niao[]; // 鸟牌
	int count_niao; // 鸟牌个数
	int count_pick_niao; // 中鸟个数
}
