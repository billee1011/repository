package com.cai.common.constant;

public class MsgConstants {

	// 1=创建房间请求(game_type_index,game_rule_index,game_round)
	// 2=加入房间(room_id,room_pw)
	// 6=小局数详细数据(brand_id)
	// 10 = 玩家准备
	// 101 = 玩家出牌(operate_card),
	// 102 = 玩家操作,吃碰杠等。(operate_card,operate_code)

	public static final int REQUST_PLAYER_READY = 10;

	public static final int REQUST_PLAYER_RELEASE_ROOM = 11; // 解散房间

	public static final int REQUST_PLAYER_BE_IN_ROOM = 12; // 进入房间

	public static final int REQUST_PLAYER_OUT_CARD = 101; // 101 =
															// 玩家出牌(operate_card),

	public static final int REQUST_PLAYER_OPERATE = 102; // 102 =
															// 玩家操作,吃碰杠等。(operate_card,operate_code)

	public static final int REQUST_CALL_BANKER = 110;		//用户叫庄
	public static final int REQUST_ADD_SCORE   =111;		//用户下注
	public static final int REQUST_OPEN_CARD   =112;        //用户摊牌
	public static final int REQUST_OPERATE_BUTTON = 113;	//操作按钮
	public static final int REQUST_PLAYER_OUT_CARD_MUL = 114;//出多张牌 （>=0)
	
	public static final int REQUST_AUDIO_CHAT = 13; // 13 = 聊天
	public static final int REQUST_EMJOY_CHAT = 14; // 14 = 表情

	public static final int REQUST_PAO_QIANG = 15; // 15 = 跑呛

	public static final int REQUST_LOCATION = 16; // 16 = 定位

	public static final int REQUST_OPEN_LESS = 17; // 17 = 允许少人模式

	public static final int REQUST_PROXY_RELEASE_ROOM = 18; // 代理解散房间

	public static final int REQUST_IS_TRUSTEE = 20; //是否托管
	
	public static final int REQUST_GOODS = 21; //道具

	public static final int REQUST_REFRESH_PLAYERS = 22; //道具
	
	public static final int REQUST_NAO_ZHUANG = 23; //闹庄
	
	public static final int REQUST_CALL_QIANG_ZHUANG = 50; //叫地主抢地主
	//////////////////////////////////////////////////////////////////////////////////////////////////
	public static final int RESPONSE_ENTER_ROOM = 10;

	public static final int RESPONSE_REFRESH_PLAYERS = 11;
	public static final int RESPONSE_REFRESH_PLAYER_CARDS = 12;// 刷新玩家的牌

	public static final int RESPONSE_SHOW_HAND_CARD = 14; // 14显示出牌

	public static final int RESPONSE_GAME_END = 15;

	public static final int RESPONSE_RECONNECT_DATA = 16;

	public static final int RESPONSE_REFRESH_DISCARD = 17;

	public static final int RESPONSE_OPERATE_XIAO_HU = 19;

	public static final int RESPONSE_FORCE_EXIT = 20;// 20系统级别的,收到这个消息,玩家强制退出客户端
	public static final int RESPONSE_PLAYER_READY = 21;// 玩家准备
	public static final int RESPONSE_PLAYER_RELEASE = 22;// 解散
	public static final int RESPONSE_AUDIO_CHAT = 23;// 聊天
	public static final int RESPONSE_EMJOY_CHAT = 24;
	public static final int RESPONSE_ADD_DISCARD = 25;// 添加牌到牌堆
	public static final int RESPONSE_EFFECT_ACTION = 26;// 26显示效果(effects_index,effect_time,effect_target)
	public static final int RESPONSE_PLAYER_ACTION = 27;// 通知玩家弹出操作
	public static final int RESPONSE_SHOW_CARD = 28;// 显示在玩家前面的牌,小胡牌,摸杠牌
	public static final int RESPONSE_PLAYER_STATUS = 29;// 基础状态
	public static final int RESPONSE_GAME_ROOM_RECORD_LIST = 30;// 大局记录列表
	public static final int RESPONSE_GAME_ROUND_RECORD_LIST = 31;// 小局记局表表
	public static final int RESPONSE_GAME_ROUND_RECORD = 32;// 小局记局(单个)
	public static final int RESPONSE_GAME_ESPECIAL_TXT = 33;// 刷新特别显示字符
	public static final int RESPONSE_GAME_PLAYER_SCORE = 34;// 牌局中分数结算
	public static final int RESPONSE_REMOVE_DISCARD = 35;// 删除出牌

	public static final int RESPONSE_REFRESH_PLAYER_DATA = 36;// 刷新玩家数据
	public static final int RESPONSE_PAO_QIANG_ACTION = 37;// 跑呛

	public static final int RESPONSE_REFRESH_ROOM_STATUS = 38;// 更新房间状态
	public static final int RESPONSE_NAO_ACTION = 39;// 闹庄状态

	public static final int RESPONSE_LOCATION = 40;// 定位
	public static final int RESPONSE_IS_TRUSTEE = 41;//托管
	public static final int RESPONSE_IS_XIANGGONG = 42;//是否是相公 
	
//	public static final int RESPONSE_EFFECT_ACTION_CENTER = 45;// 26显示效果(effects_index,effect_time,effect_target,operate_card)--带中心牌

	public static final int RESPONSE_GOODS = 43;//道具返回
	
	public static final int RESPONSE_REFRESH_PLAYER_WEAVE_CARDS = 44;// 刷新玩家的在组合牌
	
	
	public static final int RESPONSE_MY_ROOMS = 50;// 我的房间
	public static final int RESPONSE_CREATE_MY_ROOM = 51;// 创建我的房间（RoomInfo）
	public static final int RESPONSE_CREATE_RROXY_ROOM_SUCCESS = 52;// 创建成功
	public static final int RESPONSE_UPDATE_MY_ROOM = 53;// 更新我的房间
	public static final int RESPONSE_SELECT_HHTYPE = 54;	//选择攸县红黑胡类型
	public static final int RESPONSE_UPDATE_HU_XI = 80;		//胡息

	public static final int RESPONSE_OUT_CARD = 201;// 发送玩家出牌 201
													// =玩家出牌(CMD_OutCard:
													// operate_player,operate_card)
	public static final int RESPONSE_SEND_CARD = 204;// 204 =
														// 发牌（CMD_SendCard:operate_player,operate_card,operate_code）
	public static final int RESPONSE_GAME_START = 200;//// 200 开始(gameStart)
	public static final int RESPONSE_OPERATE_RESULT = 203;//// 203 =
															//// 操作结果(CMD_OperateResult:operate_player,provide_player,operate_code,operate_card)
	public static final int RESPONSE_OPERATE_NOTIFY = 202;////
	public static final int RESPONSE_CHI_HU_RESULT = 205;// 25 =
															// 发牌（CMD_SendCard:operate_player,operate_card,operate_code）
	public static final int RESPONSE_CHI_HU_CARDS = 206;// 显示吃胡牌
	public static final int RESPONSE_OPEN_CARD    =   207;//用户开牌
	public static final int RESPONSE_CALL_BANKER  = 	208;	//用户叫庄
	public static final int RESPONSE_ADD_JETTON	  =   209;	//用户下注
	public static final int RESPONSE_SELECT_BANKER = 210;	//选择庄家
	public static final int RESPONSE_OPEN_ALL_CARD = 211;  //全开
	public static final int RESPONSE_YAO_CARD      = 212;	//要牌
	public static final int RESPONSE_TOU_XIANG_OPERATE = 213;//是否投降
	public static final int RESPONSE_WAIT_RESPONSE = 214;	//等待用户
	public static final int RESPONSE_CANNOT_OUT_CARD = 215; // 不能出的牌
	
	//斗地主
	public static final int RESPONSE_DDZ_CALL_BANKER     = 300;	//叫地主
	public static final int RESPONSE_DDZ_JIAO_FEN	=301;	//叫分
	public static final int RESPONSE_DDZ_QIANG_BANKER=302;//抢地主
	
}
