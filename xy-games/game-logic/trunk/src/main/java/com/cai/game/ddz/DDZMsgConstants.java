package com.cai.game.ddz;

public class DDZMsgConstants {
	// 临汾斗地主
	public static final int RESPONSE_DDZ_GAME_START_LF = 1001;//// 1001
	//// 开始(gameStart)
	public static final int RESPONSE_DDZ_REFRESH_PLAYER_CARDS = 1002;// 刷新玩家的牌
	public static final int RESPONSE_DDZ_OUT_CARD = 1003;// 发送玩家出牌 201
	public static final int RESPONSE_DDZ_GAME_END = 1004;// 结束
	public static final int RESPONSE_DDZ_RECONNECT_DATA = 1005; // 断线重连
	public static final int RESPONSE_DDZ_SEND_CARD = 1006;
	public static final int RESPONSE_DDZ_CALL_BANKER_RESPONSE = 1007; // 操作回复
	public static final int RESPONSE_DDZ_CALL_BANKER_RESULT = 1008; // 叫地主结果
	public static final int RESPONSE_DDZ_SEND_DI_CARD = 1009;// 底牌
	public static final int RESPONSE_DDZ_SEND_SPECIAL_CARD = 1010;// 特殊牌
	public static final int RESPONSE_DDZ_END_DRAW = 1011;// 流局
	// 踢小五
	public static final int RESPONSE_TXW_GAME_START = 1001;//// 1001
	public static final int RESPONSE_TXW_RECONNECT_DATA = 1002; // 断线重连
	public static final int RESPONSE_TXW_OUT_CARD = 1003;// 发送玩家出牌 201
	public static final int RESPONSE_TXW_GAME_END = 1004;// 结束
	public static final int RESPONSE_TXW_SEND_CARD = 1006;
	public static final int RESPONSE_TXW_ROUND_END = 1007;// 回合结束
	public static final int RESPONSE_TXW_CALL_BANKER_RESPONSE = 1008; // 操作回复
	public static final int RESPONSE_TXW_CALL_BANKER_RESULT = 1009; // 叫地主结果
	public static final int RESPONSE_TXW_PLAYER_TIMES = 1010; // 玩家倍数
}
