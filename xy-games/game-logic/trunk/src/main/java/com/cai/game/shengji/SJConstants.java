package com.cai.game.shengji;

public class SJConstants {
	// 游戏状态
	public static final int GAME_STATUS_FREE = 0; // 空闲状态
	public static final int GAME_STATUS_PLAY = 100; // 游戏状态
	public static final int GAME_STATUS_WAIT = 200; // 等待状态
	// 吕梁打七
	public static final int GS_XPSJ_PLAY = GAME_STATUS_PLAY + 1; // 游戏状态,吕梁打七
	public static final int GS_XPSJ_SEND_CARD = GAME_STATUS_PLAY + 2;
	public static final int GS_XPSJ_CALL_BANKER = GAME_STATUS_PLAY + 3;
	// 溆浦240
	public static final int XP_SJ_MAX_INDEX = 16; // 信丰挂挡索引
	public static final int XP_SJ_MAX_COUT = 28;
	public static final int CARD_COUNT_XP_SJ = 84; // 信丰挂挡
	public static final int XP_SJ_CT_ERROR = -1; // 错误类型
	public static final int XP_SJ_CT_PASS = 0; // 过
	public static final int XP_SJ_CT_SINGLE = 1; // 单牌类型
	public static final int XP_SJ_CT_DOUBLE = 2; // 对子类型
	public static final int XP_SJ_CT_DOUBLE_LINK = 3; // 对连类型
	public static final int XP_SJ_CT_DIAN = 5;// 垫牌
	
	// 永州240
	public static final int YZ_SJ_MAX_INDEX = 16; //
	public static final int YZ_SJ_MAX_COUT_3 = 28; 
	public static final int YZ_SJ_MAX_COUT_4 = 23;   
	public static final int YZ_SJ_CARD_COUNT_3 = 84; // 三人
	public static final int YZ_SJ_CARD_COUNT_4 = 92; // 四人
	public static final int YZ_SJ_CT_ERROR = -1; // 错误类型
	public static final int YZ_SJ_CT_PASS = 0; // 过
	public static final int YZ_SJ_CT_SINGLE = 1; // 单牌类型
	public static final int YZ_SJ_CT_DOUBLE = 2; // 对子类型
	public static final int YZ_SJ_CT_DOUBLE_LINK = 3; // 对连类型
	public static final int YZ_SJ_CT_DIAN = 5;// 垫牌

	// 溆浦240
	public static final int RESPONSE_XPSJ_GAME_START = 1001;//// 1001////
															//// 开始(gameStart)
	public static final int RESPONSE_XPSJ_RECONNECT_DATA = 1002; // 断线重连
	public static final int RESPONSE_XPSJ_OUT_CARD = 1003;// 发送玩家出牌 201
	public static final int RESPONSE_XPSJ_GAME_END = 1004;
	public static final int RESPONSE_XPSJ_SEND_CARD = 1005;//// 发牌
	public static final int RESPONSE_XPSJCALL_BANKER = 1006;//// 叫主
	public static final int RESPONSE_XPSJ_MAIDI_BEIGN = 1007;// 埋底开始
	public static final int RESPONSE_XPSJ_MAIDI_OPREATE = 1008;// 埋底操作
	public static final int RESPONSE_XPSJ_SCORE = 1009;// 分数显示
	public static final int RESPONSE_XPSJ_ZHU_COUNT = 1010;// 牌型数量
	public static final int RESPONSE_XPSJ_SEND_CARD_END = 1011;// 发牌结束
	public static final int RESPONSE_XPSJ_DI_PAI_DATA = 1012;// 底牌数据
	public static final int RESPONSE_XPSJ_REFRES_CARD_DATA = 1013;// 刷新手牌
	public static final int RESPONSE_XPSJ_EFFECT_TYPE = 1014;// 动画特效
	public static final int RESPONSE_XPSJ_ZHU_CARD = 1015;// 主牌数据
	public static final int RESPONSE_XPSJ_LI_SHI_CARD = 1016;// 历史牌
	public static final int RESPONSE_XPSJ_PAI_SCORE = 1017;// 牌分

	// 永州包牌
	// 扑克牌值 一副牌16张
	public static final int CARD_DATA_YZBP[] = new int[] { 0x01, 0x02, 0x05, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, // 方块
			0x11, 0x12, 0x15, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, // 梅花
			0x21, 0x22, 0x25, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, // 红桃
			0x31, 0x32, 0x35, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, // 黑桃
			0x01, 0x02, 0x05, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, // 方块
			0x11, 0x12, 0x15, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x21, 0x22, 0x25, 0x27, 0x28, 0x29, 0x2A, 0x2B,
			0x2C, 0x2D, 0x31, 0x32, 0x35, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x4E, 0x4F, 0x4E, 0x4F, };
	public static final int CARD_COUNT_YZBP = 84;
	public static final int YZBP_MAX_COUT = 34;
	public static final int GAME_RULE_YZBP_TOU_XIANG_LOSE_ONE = 1;// 投降输1倍
	public static final int GAME_RULE_YZBP_TOU_XIANG_LOSE_TWO = 2;// 投降输2倍
	public static final int GAME_RULE_YZBP_TOU_XIANG_LOSE_THREE = 3;// 投降输3倍
	public static final int GAME_RULE_YZBP_HAN_LAI_SCORE = 4;// 允许喊来分
	public static final int GAME_RULE_YZBP_NO_LIMIT = 5;// 大倒不封顶
	public static final int GAME_RULE_YZBP_CAN_LOOK = 6;// 允许查牌
	public static final int GAME_RULE_YZBP_TOU_XIANG_ASK = 7;// 投降询问所有人
	public static final int GAME_RULE_YZBP_CALL_CALL_BO_ZHU = 8;// 允许叫无主
}
