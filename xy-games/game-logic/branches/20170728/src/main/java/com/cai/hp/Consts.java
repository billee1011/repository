package com.cai.hp;

public class Consts {

	// 参数定义
	public static final int INVALID_VALUE = 0; // 无效椅子
	public static final int INVALID_SEAT = -1; // 无效椅子
	public static final int BLACK_CARD = -2; // 牌背

	// 牌局结束
	// 结束原因
	public static final int GER_NORMAL = 0x00; // 常规结束
	public static final int GER_DISMISS = 0x01; // 游戏解散
	public static final int GER_USER_LEAVE = 0x02; // 用户离开
	public static final int GER_NETWORK_ERROR = 0x03; // 网络错误

	//////////////////////////////////////////////////////////////////////////////////////
	// 结束
	public static final int Game_End_NORMAL = 1; // 正常结束
	public static final int Game_End_DRAW = 2; // 流局
	public static final int Game_End_ROUND_OVER = 3; // 圈数用完了，结束
	public static final int Game_End_RELEASE_NO_BEGIN = 4; // 没开始就解散了
	public static final int Game_End_RELEASE_PLAY = 5; // 游戏过程中解散的
	public static final int Game_End_RELEASE_RESULT = 6; // 游戏过程中解散的
	public static final int Game_End_RELEASE_PLAY_TIME_OUT = 7; // 超时解散
	public static final int Game_End_RELEASE_WAIT_TIME_OUT = 8; // 超时解散
	public static final int Game_End_RELEASE_SYSTEM = 9; // 系统强制解散

	//////////////////////////////////////////////////////////////////////////
	// public static final int HU_RESULT_NULL = 0;
	// public static final int HU_RESULT_ZIMO = 1;
	// public static final int HU_RESULT_FANGPAO = 2;
	// public static final int HU_RESULT_JIEPAO = 3;
	public static final int HU_RESULT_FANG_KAN_QUAN_BAO = 4;
	////////////////////////////////////////////////////////////////////////////////////////
	// 效果定义
	public static final int EFFECT_ACTION_TYPE_HU = 1;
	public static final int EFFECT_ACTION_TYPE_ACTION = 2;
	public static final int Effect_Action_Other = 3;

	/// 其他类型效果
	public static final int EFFECT_LAST_FOUR_CARD = 1;// 最后4张牌

	public static final int Show_Card_HU = 1;
	public static final int Show_Card_XiaoHU = 2;
	public static final int Show_Card_Center = 3;
	/////////////////////////////////////////////////////////////////////

	public static final int CS_GANG_DRAW_COUNT = 2; //

	public static final int ZHUZHOU_GANG_DRAW_COUNT = 1; //

	public static final int OUT_CARD_TYPE_MID = 0;
	public static final int OUT_CARD_TYPE_LEFT = 1;

	/////////////////////////////////////////////////////////////////////
	public static final int CARD_STATUS_NORMAL = 0;
	public static final int CARD_STATUS_CS_GANG = 1;
	public static final int CARD_STATUS_BAO_TING = 2; // 报听

	/////////////////////////////////////////////////////////////////////
	public static final int HU_CARD_TYPE_ZIMO = 1;
	public static final int HU_CARD_TYPE_PAOHU = 2;
	public static final int HU_CARD_TYPE_QIANGGANG = 3;
	public static final int HU_CARD_TYPE_RE_CHONG = 4;
	public static final int HU_CARD_TYPE_TIAN_HU = 5;
	public static final int HU_CARD_TYPE_GANG_KAI = 6;// 判断杠上开花
	public static final int HU_CARD_TYPE_GANG_PAO = 7;// 判断杠上炮

	////////////////////////////////////////////////////////////////////////
	public static final int GANG_TYPE_AN_GANG = 1;
	public static final int GANG_TYPE_JIE_GANG = 2;
	public static final int GANG_TYPE_ADD_GANG = 3;

	//////////////////////////////////////////////////////////////////////// 吃碰牌的类型
	public static final int CHI_PENG_TYPE_OUT_CARD = 0;
	public static final int CHI_PENG_TYPE_GANG = 1;
	public static final int CHI_PENG_TYPE_CHAO_TIAN = 2;

	//////////////////////////////////////////////////////////////////////// 下跑最大个数
	public static final int PAO_MAX_COUNT = 2;

	public static final int PAO_MAX_COUNT_HENAN = 3;

	public static final int PAO_MAX_COUNT_PIAO_FLS = 5;// 福禄寿飘 最大5分

	// public static final int GANG_TYPE_MENG_XIAO = 4;
	// public static final int GANG_TYPE_DIAN_XIAO = 5;
	// public static final int GANG_TYPE_HUI_TOU_XIAO = 6;
	// public static final int GANG_TYPE_DA_CHAO_TIAN = 7;
	// public static final int GANG_TYPE_XIAO_CHAO_TIAN = 8;
	// 扑克定义

	// 0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09, //万子
	// 0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19, //索子
	// 0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29, //同子
	// 0x31,0x32,0x33,0x34,0x35,0x36,0x37, //番子,东南西北，中发白
	// 0x38,0x39,0x3A,0x3B,0x3C,0x3D,0x3E,0x3F, //花子

	//////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////
	// 静态变量

	// 全部的麻将数据
	public static final int CARD_DATA_MAX[] = new int[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, // 万子
			0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, // 万子
			0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, // 万子
			0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, // 万子
			0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, // 索子
			0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, // 索子
			0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, // 索子
			0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, // 索子
			0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, // 同子
			0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, // 同子
			0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, // 同子
			0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, // 同子

			0x31, 0x31, 0x31, 0x31, // 东风
			0x32, 0x32, 0x32, 0x32, // 西风
			0x33, 0x33, 0x33, 0x33, // 南风
			0x34, 0x34, 0x34, 0x34, // 北风
			0x35, 0x35, 0x35, 0x35, // 红中
			0x36, 0x36, 0x36, 0x36, // 绿发
			0x37, 0x37, 0x37, 0x37, // 白板

			0x38, 0x38, 0x38, 0x38, // 春
			0x39, 0x39, 0x39, 0x39, // 夏
			0x41, 0x41, 0x41, 0x41, // 秋
			0x42, 0x42, 0x42, 0x42, // 冬
			0x43, 0x43, 0x43, 0x43, // 梅
			0x44, 0x44, 0x44, 0x44, // 兰
			0x45, 0x45, 0x45, 0x45, // 竹
			0x46, 0x46, 0x46, 0x46, // 菊
	};

	public static final int CARD_DATA_FLS_LX[] = new int[] { 0x01, 0x01, 0x01, 0x01, // 上
			0x02, 0x02, 0x02, 0x02, // 大
			0x03, 0x03, 0x03, 0x03, // 人

			0x11, 0x11, 0x11, 0x11, // 丘
			0x12, 0x12, 0x12, 0x12, // 乙
			0x13, 0x13, 0x13, 0x13, // 己

			0x21, 0x21, 0x21, 0x21, // 化
			0x22, 0x22, 0x22, 0x22, // 三
			0x23, 0x23, 0x23, 0x23, // 千

			0x31, 0x31, 0x31, 0x31, // 七
			0x32, 0x32, 0x32, 0x32, // 十
			0x33, 0x33, 0x33, 0x33, // 土

			0x41, 0x41, 0x41, 0x41, // 尔
			0x42, 0x42, 0x42, 0x42, // 小
			0x43, 0x43, 0x43, 0x43, // 生

			0x51, 0x51, 0x51, 0x51, // 八
			0x52, 0x52, 0x52, 0x52, // 九
			0x53, 0x53, 0x53, 0x53, // 子

			0x61, 0x61, 0x61, 0x61, // 佳
			0x62, 0x62, 0x62, 0x62, // 作
			0x63, 0x63, 0x63, 0x63, // 亡

			0x71, 0x71, 0x71, 0x71, // 福
			0x72, 0x72, 0x72, 0x72, // 禄
			0x73, 0x73, 0x73, 0x73, // 寿
	};

	public static final int CARD_DATA_DDZ[] = new int[] { 
			0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0A,0x0B,0x0C,0x0D,	//方块 A - K
			0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,0x1A,0x1B,0x1C,0x1D,	//梅花 A - K
			0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,0x2A,0x2B,0x2C,0x2D,	//红桃 A - K
			0x31,0x32,0x33,0x34,0x35,0x36,0x37,0x38,0x39,0x3A,0x3B,0x3C,0x3D,	//黑桃 A - K
			0x4E,0x4F,
	};

	public static final int CARD_CAI_SHENG = 0x38; //

	public static final int Release_Room_Type_SEND = 1; // 发起解散
	public static final int Release_Room_Type_AGREE = 2; // 同意
	public static final int Release_Room_Type_DONT_AGREE = 3; // 不同意
	public static final int Release_Room_Type_CANCEL = 4; // 还没开始,房主解散房间
	public static final int Release_Room_Type_QUIT = 5; // 还没开始,普通玩家退出房间
	public static final int Release_Room_Type_PROXY = 6; // 代理解散房间

	public static final int CARD_ESPECIAL_TYPE_NIAO = 1000;
	public static final int CARD_ESPECIAL_TYPE_HU = 2000;

	public static final int CARD_ESPECIAL_TYPE_GUI = 3000;// 鬼
	public static final int CARD_ESPECIAL_TYPE_LAI_ZI = 3200;// ;//癞子

	public static final int CARD_ESPECIAL_TYPE_DING_GUI = 4000;// 定鬼
	public static final int CARD_ESPECIAL_TYPE_LAI_GEN = 4200;// ;//赖根

	public static final int CARD_ESPECIAL_TYPE_TING = 5000;// ;//听牌标示
	public static final int CARD_ESPECIAL_TYPE_BAO_TING = 5200;// ;//报听标示

	public static final int CARD_ESPECIAL_TYPE_HUN = 6000;// 混牌

	//////////
	public static final int WEAVE_SHOW_DIRECT = 1000;// ;//显示组合牌的 放坎人

	// 特殊描述类型

	// 特别显示的描述
	public static final int Especial_Txt_Type_NULL = 0;// 无,不显示
	public static final int Especial_Txt_Type_PIAO_LAI = 1;// 飘赖显示

	// 牌局过程结算
	public static final int Score_Type_NULL = 0;// 无,不显示
	public static final int Score_Type_PIAO_LAI = 1;// 飘赖分数
	public static final int Score_Type_GANG = 2;// 杠(笑)牌,分数

	// //发牌类型
	public static final int DispatchCard_Type_Noraml = 1;// 普通发牌
	public static final int DispatchCard_Type_Tian_Hu = 2;// 第一张牌
	//
	// //发牌类型
	public static final int OutCard_Type_Noraml = 1;// 普通出牌
	public static final int OutCard_Type_Di_Hu = 2;
	// public static final int OutCard_Type_Ming_Gang2 = 3;//明杠出牌

	/////////////////////////////////////////////////////////////////////////// 鸟
	public static final int DING_NIAO_VALID = 1000;
	public static final int DING_NIAO_INVALID = 200;
	public static final int FEI_NIAO_VALID = 1500;
	public static final int FEI_NIAO_INVALID = 500;
	//////////////////////////////////////////////////////////////////////////// 代理相关
	public static final int CREATE_ROOM_PROXY_TIME_GAP = 10; // 10分钟
	public static final int CREATE_ROOM_NORMAL = 0;
	public static final int CREATE_ROOM_PROXY = 1;

	public static final int PROXY_ROOM_RELEASE = 1;
	public static final int PROXY_ROOM_UPDATE = 2;
	public static final int PROXY_ROOM_PLAYER = 3;

}
