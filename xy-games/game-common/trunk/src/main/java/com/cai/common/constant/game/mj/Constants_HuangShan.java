package com.cai.common.constant.game.mj;

public interface Constants_HuangShan {
	int GAME_RULE_PLAYER_FOUR = 1; // 四人
	int GAME_RULE_PLAYER_THREE = 2; // 三人
	int GAME_RULE_PLAYER_ER = 3; // 二人
	int GAME_RULE_ZHI_XU_ZI_MU = 4; // 只能自摸
	int GAME_RULE_PI_HU = 5; // 屁胡
	int GAME_RULE_QUE_YI_MEN = 6; // 缺一门
	int GAME_RULE_DI_HU_YI_JIA_CHU = 7; // 地胡一家出
	int GAME_RULE_DI_HU_SAN_JIA_CHU = 8; // 地胡三家出

	int CHR_ZI_MO = 0x00000001; // 自摸
	int CHR_JIE_PAO = 0x00000002; // 接炮(收铳)
	int CHR_FANG_PAO = 0x00000004; // 放炮
	int CHR_SI_CAI_SHEN = 0x00000008; // 四财神
	int CHR_PI_HU = 0x00000020; // 屁胡
	int CHR_YOU_FEI_ZI_MO = 0x00000040; // 有飞自摸
	int CHR_WU_FEI_ZI_MO = 0x00000100;// 无飞自摸
	int CHR_PAO_FEI = 0x00000200; // 跑飞（飞单吊）
	int CHR_FEI_JI_GANG = 0x00000400; // 飞机杠
	int CHR_WU_FEI_GANG_KAI = 0x00000800; // 无飞杠开
	int CHR_WU_FEI_QIANG_GANG = 0x00001000; // 无飞抢杠
	int CHR_DI_HU = 0x00002000; // 地胡
	int CHR_TIAN_HU = 0x00004000; // 天胡
	int CHR_PENG_FEI = 0x00008000; // 碰飞
	// int CHR_PIAO_FEI_1 = 0x00008000; // 1飘飞
	// int CHR_PIAO_FEI_2 = 0x00008000; // 2飘飞
	// int CHR_PIAO_FEI_3 = 0x00008000; // 3飘飞
	// int CHR_PIAO_FEI_4 = 0x00008000; // 4飘飞

	int HU_CARD_TYPE_ZI_MO = 1; // 自摸
	int HU_CARD_TYPE_JIE_PAO = 2; // 接炮
	int HU_CARD_TYPE_QIANG_GANG = 3; // 抢杠
	int HU_CARD_TYPE_GANG_KAI = 4; // 杠开花
	int HU_CARD_TYPE_GANG_PAO = 5; // 杠上炮
	int HU_CARD_TIAN_HU = 6; // 天胡
	int HU_CARD_DI_HU = 7; // 地胡

	// 红中麻将数据
	public static final int CARD_DATA[] = new int[] {
			0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, // 索子
			0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, // 索子
			0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, // 索子
			0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, // 索子
			0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, // 同子
			0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, // 同子
			0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, // 同子
			0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, // 同子

			0x35, 0x35, 0x35, 0x35, // 红中
			0x37, 0x37, 0x37, 0x37, // 白班

	};

	public static final int WAN_ZI[] = new int[] { 
			0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, // 万子
			0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, // 万子
			0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, // 万子
			0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, // 万子
	};
}