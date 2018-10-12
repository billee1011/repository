package com.cai.game.ddz;

public class DDZConstants {
	// 游戏状态
	public static final int GAME_STATUS_FREE = 0; // 空闲状态
	public static final int GAME_STATUS_PLAY = 100; // 游戏状态
	public static final int GAME_STATUS_WAIT = 200; // 等待状态

	// 扑克牌值
	public static final int CARD_DATA_DDZ_LF[] = new int[] { 0x01, 0x11, 0x21, 0x31, 0x02, 0x22, 0x03, 0x13, 0x23, 0x33,
			0x04, 0x14, 0x24, 0x34, 0x05, 0x15, 0x25, 0x35, 0x06, 0x16, 0x26, 0x36, 0x07, 0x17, 0x27, 0x37, 0x08, 0x18,
			0x28, 0x38, 0x09, 0x19, 0x29, 0x39, 0x0A, 0x1A, 0x2A, 0x3A, 0x0B, 0x1B, 0x2B, 0x3B, 0x0C, 0x1C, 0x2C, 0x3C,
			0x0D, 0x1D, 0x2D, 0x3D, 0x4E, 0x4F, };

	// 临汾斗地主
	public static final int LFDDZ_DI_PAI_COUNT_JD = 4; // 底牌数量
	public static final int CARD_COUNT_DDZ_LF = 52;
	public static final int DDZ_MAX_COUNT_LF = 16;

	public static final int DDZ_LF_CT_ERROR = -1; // 错误类型
	public static final int DDZ_LF_CT_PASS = 0; // 过
	public static final int DDZ_LF_CT_SINGLE = 1; // 单张
	public static final int DDZ_LF_CT_DOUBLE = 2; // 对子
	public static final int DDZ_LF_CT_SINGLE_LINE = 3; // 单张顺子
	public static final int DDZ_LF_CT_DOUBLE_LINE = 4; // 对子顺子
	public static final int DDZ_LF_CT_THREE = 5; // 三张
	public static final int DDZ_LF_CT_THREE_TAKE_ONE = 6; // 三带一
	public static final int DDZ_LF_CT_THREE_LINE_TAKE_ONE = 7; // 飞机
	public static final int DDZ_LF_CT_THREE_LINE_LOST = 8; // 飞机缺翅膀
	public static final int DDZ_LF_CT_FOUR_TAKE_TWO = 9; // 四带两
	public static final int DDZ_LF_CT_BOMB_CARD = 10; // 炸弹
	public static final int DDZ_LF_CT_MISSILE_CARD = 11; // 火箭
	// 规则
	public static final int GAME_RULE_LF_DDZ_CAN_TI = 1;// 能提
	public static final int GAME_RULE_LF_DDZ_NO_TI = 2;// 不能提
	public static final int GAME_RULE_LF_DDZ_THREE_BOOM_YES = 3;// 3张3为炸弹
	public static final int GAME_RULE_LF_DDZ_THREE_BOOM_NO = 4;// 3张3不为炸弹
	public static final int GAME_RULE_LF_DDZ_BOOM_LIMIT_FIVE = 5;// 5炸弹封顶
	public static final int GAME_RULE_LF_DDZ_BOOM_LIMIT_FOUR = 6;// 4炸弹封顶
	public static final int GAME_RULE_LF_DDZ_BOOM_LIMIT_NO = 7;// 不封顶

	// 踢小五
	// 扑克牌值
	public static final int CARD_DATA_TXW_TWO[] = new int[] { 0x01, 0x11, 0x21, 0x31, 0x09, 0x19, 0x29, 0x39, 0x0A,
			0x1A, 0x2A, 0x3A, 0x0B, 0x1B, 0x2B, 0x3B, 0x0C, 0x1C, 0x2C, 0x3C, 0x0D, 0x1D, 0x2D, 0x3D };
	public static final int CARD_DATA_TXW_FOUR[] = new int[] { 0x01, 0x11, 0x21, 0x31, 0x02, 0x12, 0x22, 0x32, 0x08,
			0x18, 0x28, 0x38, 0x09, 0x19, 0x29, 0x39, 0x0A, 0x1A, 0x2A, 0x3A, 0x0B, 0x1B, 0x2B, 0x3B, 0x0C, 0x1C, 0x2C,
			0x3C, 0x0D, 0x1D, 0x2D, 0x3D };
	public static final int CARD_DATA_TXW_FIVE[] = new int[] { 0x01, 0x11, 0x21, 0x31, 0x02, 0x12, 0x22, 0x32, 0x06,
			0x16, 0x26, 0x36, 0x07, 0x17, 0x27, 0x37, 0x08, 0x18, 0x28, 0x38, 0x09, 0x19, 0x29, 0x39, 0x0A, 0x1A, 0x2A,
			0x3A, 0x0B, 0x1B, 0x2B, 0x3B, 0x0C, 0x1C, 0x2C, 0x3C, 0x0D, 0x1D, 0x2D, 0x3D };
	public static final int CARD_COUNT_TXW_TWO = 24;
	public static final int CARD_COUNT_TXW_FOUR = 32;
	public static final int CARD_COUNT_TXW_FIVE = 40;
	public static final int TXW_MAX_COUT = 5;

	public static final int TXW_CT_ERROR = -1; // 错误类型
	public static final int TXW_CT_PASS = 0; // 过
	public static final int TXW_CT_SINGLE = 1; // 单张
	public static final int TXW_CT_DOUBLE = 2; // 对子
	public static final int TXW_CT_SINGLE_LINE = 3; // 单张顺子
	public static final int TXW_CT_BOMB_CARD = 4; // 炸弹
	public static final int TXW_CT_HONG = 5; // 轰
	// 规则
	public static final int GAME_RULE_TXW_CLASS = 1;// 经典玩法
	public static final int GAME_RULE_TXW_TI_TWO = 2;// 踢两圈
	public static final int GAME_RULE_TXW_BOOM_LIMIT_NO = 3;// 不封顶
	public static final int GAME_RULE_TXW_BOOM_LIMIT_32 = 4;// 32分封顶
	public static final int GAME_RULE_TXW_BOOM_LIMIT_64 = 5;// 64分封顶
	public static final int GAME_RULE_TXW_BOOM_LIMIT_128 = 6;// 128分封顶
	public static final int GAME_RULE_TXW_BOOM_DOUBLE = 7;// 轰炸翻倍
	public static final int GAME_RULE_TXW_TRUESS = 8;// 托管
	public static final int GAME_RULE_TXW_DUI_TI = 9;// 对踢
}
