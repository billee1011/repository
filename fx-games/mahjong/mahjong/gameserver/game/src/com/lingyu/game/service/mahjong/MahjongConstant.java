package com.lingyu.game.service.mahjong;

public class MahjongConstant {
	public static final String CLIENT_DATA = "data";
	// ===========麻将玩法================================
	// 是否是 例如：是否是自摸胡 是否是风牌 是否是红中赖子
	public static final int TYPE_YES = 1;
	public static final int TYPE_NO = 2;
	// ===========麻将玩法end================================

	/** 麻将总数，1-9条、饼、万=108张，加上风牌 中西南北中发白28张 */
	public static final int MAHJONG_PAI_SUM = 136;
	/** 不要风牌的总数 */
	public static final int MAHJONG_PAI_NO_FENG_SUM = 108;

	/** 1 代表万 ，2 代表筒， 3 代表 条 4代表风 5代表赖子 */
	public static final int MAHJONG_WAN = 1;
	public static final int MAHJONG_TONG = 2;
	public static final int MAHJONG_TIAO = 3;
	public static final int MAHJONG_FENG = 4;
	public static final int MAHJONG_LZ = 5;

	/** 依次代表 东西南北中发白 */
	public static final int FENG_TYPE_DONG = 10;
	public static final int FENG_TYPE_XI = 11;
	public static final int FENG_TYPE_NAN = 12;
	public static final int FENG_TYPE_BEI = 13;
	public static final int FENG_TYPE_ZHONG = 14;
	public static final int FENG_TYPE_FA = 15;
	public static final int FENG_TYPE_BAI = 16;
	/** 赖子的值 */
	public static final int FENG_TYPE_LZ = 17;

	/** 麻将初始化的时候。一人13张牌 */
	public static final int MAHJONG_INIT_THIRTEENTH = 13;

	// ====================麻将标签操作======================
	/** 麻将 碰 */
	public static final int MAHJONG_PENG = 1;
	/** 麻将 暗杠 */
	public static final int MAHJONG_ANGANG = 2;
	/** 麻将 明杠 */
	public static final int MAHJONG_MINGGANG = 3;
	/** 麻将 过路杠 */
	public static final int MAHJONG_GUOLUGANG = 4;
	/** 麻将 胡 */
	public static final int MAHJONG_HU = 5;
	/** 麻将 自摸 */
	public static final int MAHJONG_ZIMO = 6;
	/** 麻将 过 */
	public static final int MAHJONG_GUO = 7;
	/** 麻将 吃 */
	public static final int MAHJONG_EAT = 8;

	// ==================胡牌类型用于算番数========================
	/** 麻将 平胡 */
	public static final int MAHJONG_HU_TYPE_PING = 1;
	/** 麻将 对对胡 */
	public static final int MAHJONG_HU_TYPE_DUIDUI = 2;
	/** 麻将 豪华对对胡 */
	public static final int MAHJONG_HU_TYPE_HAOHUADUIDUI = 3;
	/** 麻将 杠开 */
	public static final int MAHJONG_HU_TYPE_GANGKAI = 4;
	/** 碰碰胡 */
	public static final int MAHJONG_HU_TYPE_PENGPENG = 5;
	/** 清一色 */
	public static final int MAHJONG_HE_TYPE_PUR_COLOR = 6;
	/** 全求人 */
	public static final int MAHJONG_HE_TYPE_ALL_CLAIMANT = 7;
	/** 门前请 */
	public static final int MAHJONG_HU_TYPE_NOT_SPEAK = 8;
	/** 麻将 抢杠 */
	public static final int MAHJONG_HU_TYPE_QIANGGANG = 9;

	// ====================解散房间操作====================
	/** 申请房间解散-同意 */
	public static final int DISMISS_AGREE = 1;
	/** 申请房间解散-拒绝 */
	public static final int DISMISS_FAIL = 2;
	/** 申请房间解散-全部同意 */
	public static final int DISMISS_ALL_AGREE = 3;
	/** 申请房间解散-时间已到 */
	public static final int DISMISS_TIME_END = 4;

	// ====================当前局结束的类型=================
	/** 1.自摸 */
	public static final int CUR_JU_END_ZIMO = 1;
	/** 2.胡 */
	public static final int CUR_JU_END_HU = 2;
	/** 3.摸牌摸完了 */
	public static final int CUR_JU_END_MOPAIFINISH = 3;
	/** 4.解散房间 3个人点了同意 */
	public static final int CUR_JU_END_DISSROOM_THREE = 4;
	/** 5.解散房间 时间到了 */
	public static final int CUR_JU_END_DISSROOM_TIME_END = 5;

	/** 每页显示条数 */
	public static final int ZHANJI_RESULT_PAGE_SIZE = 20;

	// ====================回放记录类型=================
	/** 回放类型1=牌的初始化 */
	public static final int PLAY_BACK_TYPE_INIT = 1;
	/** 回放类型2=摸牌 */
	public static final int PLAY_BACK_TYPE_MOCHESS = 2;
	/** 回放类型3=标签提示 */
	public static final int PLAY_BACK_TYPE_SHOW_SIGN = 3;
	/** 回放类型4=标签操作 */
	public static final int PLAY_BACK_TYPE_SIGN_OPERATE = 4;
	/** 回放类型5=打牌 */
	public static final int PLAY_BACK_TYPE_PLAY = 5;
}
