package com.cai.game.hh.handler.yyzhz;

import com.cai.common.constant.GameConstants;

/**
 * 
 * 岳阳捉红字常量类
 * @author WalkerGeek 
 */
public class GameConstants_YYZHZ extends GameConstants {
	
	public static final int CARD_COUNT_PHZ_YYZHZ = 80; // 岳阳捉红字默认数量
	public static final int YYZHZ_MAGIC_CARD = 0x21 ;  //万能牌

	//游戏玩法
	public static final int GAME_RULE_PLAYER_YYZGZ_THREE = 1; // 三人玩法
	public static final int GAME_RULE_PLAYER_YYZGZ_WANG_1 = 2; //带1张王牌
	public static final int GAME_RULE_PLAYER_YYZGZ_WANG_2 = 3; //带2张王牌
	public static final int GAME_RULE_PLAYER_YYZGZ_WANG_3 = 4; //带3张王牌
	public static final int GAME_RULE_PLAYER_YYZGZ_WANG_4 = 5; //带4张王牌
	public static final int GAME_RULE_PLAYER_YYZHZ_DAHU_DIE_JIA = 6; //大胡叠加
	
	//胡牌动画定义
	public static final long CHR_HU_BAN_BAN_HU = 0x00000001; // 板板胡
	public static final long CHR_HU_DIAN_HU= 0x00000002; // 点胡
	public static final long CHR_HU_YI_GUA_BIAN = 0x00000004; // 一挂匾
	public static final long CHR_HU_PENG_PENG_HU = 0x00000008; // 碰碰胡
	public static final long CHR_HU_XIAO_YI_SE = 0x00000010; // 小一色
	public static final long CHR_HU_DA_YI_SE = 0x00000020; //大一色
	public static final long CHR_HU_HEI_HU = 0x00000040; // 黑胡
	public static final long CHR_HU_SHI_HONG = 0x00000080; // 十红
	public static final long CHR_HU_QI_XIAO_DUI = 0x00000100; // 七小对
	public static final long CHR_HU_MANG_TANG_HONG = 0x00000200; // 满堂红
	public static final long CHR_HU_DANG_DIAO = 0x00000400; // 四碰单吊
	public static final long CHR_HU_JU_JU_HONG = 0x00000800; // 句句红
	
	public static final int MAX_YYZHZ_INDEX = 21; // 最大索引数据（湘阴字牌）
	public static final int MAX_YYZHZ_COUNT = 14; // 最大数目手牌（字牌）
	public static final int MAX_WEAVE_YYZHZ = 4; // 最大组合
	
	public static final int CARD_DATA_PHZ_DEFAULT[] = new int[] { 
			0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, // 小一到十
			0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, // 小一到十
			0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, // 小一到十
			0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, // 小一到十
			0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, // 大壹到拾
			0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, // 大壹到拾
			0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, // 大壹到拾
			0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, // 大壹到拾
	};
}
