package com.cai.game.hh.handler.hcphz;

import com.cai.common.constant.GameConstants;

public class GameConstants_HeChi extends GameConstants {

	public static final int GAME_PLAYER_NUMBER_2 = 2; // 2人字牌
	public static final int GAME_PLAYER_NUMBER_3 = 3; // 3人字牌

	// ---------rules----------
	public static final int GAME_RULE_PLAYER_2 = 1; // 2人字牌
	public static final int GAME_RULE_PLAYER_3 = 2; // 3人字牌
	public static final int GAME_RULE_QI_HU_10 = 3; // 10油起胡
	public static final int GAME_RULE_QI_HU_15 = 4; // 15油起胡
	public static final int GAME_RULE_ZI_3 = 5; // 3油一子
	public static final int GAME_RULE_ZI_5 = 6; // 5油一子
	public static final int GAME_RULE_ZI_MO_ADD_FAN = 7; // 自摸加番
	public static final int GAME_RULE_ZI_MO_ADD_ZI = 8; // 自摸加子
	public static final int GAME_RULE_PAO_HU = 9; // 接炮胡
	public static final int GAME_RULE_ZI_MO = 10; // 自摸胡
	public static final int GAME_RULE_PAO_HU_MORE = 11; // 多人接跑胡
	public static final int GAME_RULE_XING_SHANG = 12; // 上醒
	public static final int GAME_RULE_XING_XIA = 13; // 下醒
	public static final int GAME_RULE_XING_BEN = 14; // 本醒
	public static final int GAME_RULE_SI_XING_LIAN_FAN = 15; // 四醒连番
	public static final int GAME_RULE_BI_HU = 16; // 有胡必胡

	// ------------------胡型--------------
	public static final int CHR_TIAN_HU = 0x00000001; // 天胡
	public static final int CHR_SHI_SAN_HONG = 0x00000002; // 十三红
	public static final int CHR_SHI_BA_DA = 0x00000004; // 十八大
	public static final int CHR_SHI_BA_XIAO = 0x00000008; // 十八小
	public static final int CHR_SHI_BA_AND_SHI_SAN = 0x00000010; // 十三红+十八大
	public static final int CHR_ALL_HEI = 0x00000020; // 全黑
}
