package com.cai.game.util;

public class GameUtilConstants {
	public static final int TABLE_COUNT = 9; // 9种表格数据
	public static final int MAX_MAGIC_COUNT = 8; // 最多8个癞子
	public static final int MAX_CARD_VALUE = 9; // 万条筒最大点数9
	public static final int MAX_FENG_CARD_VALUE = 7; // 风牌最大点数7
	public static final int MAX_WEAVE_COUNT = 4; // 3*n中，n最大为4
	public static final int MAX_LOOPS = 16; // 插入刻子、顺子时，刻子轮询9次，顺子轮询7次
	public static final int MAX_FENG_LOOPS = 12; // 风牌，插入刻子、(东南西北)或(中发白)任意三种不同风牌组成的组合，7+4+1
	public static final int MAX_FENG_LOOPS_DFL = 11; // 有东风令时，只能轮询11次
	public static final int MIN_LOOPS = 7; // 风牌插入刻子时，轮询7次
	public static final int CARD_COUNT_PER_CARD = 4; // 每种牌最多四张
	public static final int CRITICAL_CARD_COUNT_PER_CARD = 5; // 加顺子时，牌数目不能过5
	public static final int MAX_CARD_TYPE = 34; // 万条筒加风牌，一共34种
	public static final int MAX_HAND_CARD_COUNT = 14; // 胡牌时手里的牌最多14张
	public static final int MAX_MAGIC_INDEX_COUNT = 2; // 最多2个癞子牌索引，最多8个癞子

	public static final int COLOR_FOR_WAN = 0; // 万的颜色值设置
	public static final int COLOR_FOR_TIAO = 1; // 条的颜色值设置
	public static final int COLOR_FOR_TONG = 2; // 筒的颜色值设置
	public static final int COLOR_FOR_FENG = 3; // 风的颜色值设置

	public static final int CARD_COLOR_COUNT = 4; // 4种花色的牌
	public static final int MAX_COMBINE_COUNT = 6; // 最多八张癞子时，一个5连的牌型，组合成3n/3+2时，可以有5次

	public static final int MAX_YAO_JIU_LOOPS = 4; // 19，刻子两次，顺子两次

	public static final int MAX_JI_XIAN_LOOPS = 32; // 插入刻子、顺子时，刻子轮询9次，顺子轮询7次；加入19夹的时候，轮训9次，加入28夹的时候，轮训7次
	public static final int MIN_SINGLE_INDEX = 0; // 单色牌最新下标0
	public static final int MAX_SINGLE_INDEX = 8; // 单色牌最大下标8
}
