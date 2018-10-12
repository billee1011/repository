package com.lingyu.common.io;

public abstract class MsgType {
	/** 与跨服服务器之间 */

	public static final int RPC_RelayMsgReq = 100;
	public static final int RPC_DispatchEventReq = 105;
	public static final int RPC_LoadDataReq = 106;
	public static final int RPC_WriteDataReq = 108;
	public static final int RPC_LoadUserDataReq = 114;

	/** 心跳 */
	public static final int Ping_Msg = 19000;
	/** 客户端主动心跳 */
	public static final int HEART_BEAT_Msg = 19003;

	// public static final int GetRoleList_C2S_Msg = 10001;
	// public static final int CreatRole_C2S_Msg = 10002;
	public static final int DELETE_ROLE_C2S_Msg = 10004;
	public static final int RESTORE_ROLE_C2S_Msg = 10005;

	/** 协议屏蔽通知(13910) */
	public static final int CommandForbidden_Msg = 13910;

	// ==================麻将协议======================
	/** 客户端心跳协议 */
	public static final int CLIENT_HEART_BEAT_MSG = 10001;
	// /** 验证角色信息*/
	// public static final int GetRoleList_C2S_Msg = 10002;
	/** 登陆游戏 */
	public static final int LoginGame_C2S_Msg = 10003;
	// /** 创建user */
	// public static final int CREATE_USER_MSG = 10004;
	// /** 创建role */
	// public static final int CREATE_ROLE_MSG = 10005;

	public static final int CREATE_MAHJONG_ROOM = 10007;

	/** 加入房间 */
	public static final int JOIN_MAHJONG_MSG = 10008;

	/** 给客户端推送初始化的牌 */
	public static final int MAHJONG_PAI_INIT_MSG = 10009;

	/** 客户端提示 碰，杠，胡 */
	public static final int MAHJONG_SHOW_MSG = 10010;

	/** 打牌 */
	public static final int MAHJONG_PLAY_MSG = 10011;

	/** 摸到的牌 */
	public static final int MAHJONG_MO_MSG = 10012;

	/** 麻将标签的操作 */
	public static final int MAHJONG_CHESS_SIGN_MSG = 10013;

	/** 解散房间 */
	public static final int DISSOLVED_ROOM_MSG = 10014;

	/** 退出房间 */
	public static final int QUIT_ROOM_MSG = 10015;

	/** 每局的结算面板 */
	public static final int MAHJONG_HU_MSG = 10016;

	/** 开始游戏 */
	public static final int MAHJONG_START_GAME_MSG = 10017;

	/** 断线重连-->在打牌界面 */
	public static final int MAHJONG_STATE_RELOGIN_MSG = 10018;

	/** 查看总战绩 */
	public static final int MAHJONG_SUM_SCORE_MSG = 10019;

	/** 申请解散房间 */
	public static final int MAHJONG_DISMISS_MSG = 10020;

	/** 申请解散房间 -同意or拒绝 */
	public static final int MAHJONG_DISMISS_OPERATE_MSG = 10021;

	/** 吃 */
	public static final int MAHJONG_EAT = 10022;

	/** 返回大厅 */
	public static final int MAHJONG_RETURN_HALL = 10023;

	/** 返回房间 */
	public static final int MAHJONG_RETURN_ROOM = 10024;

	// ===============================聊天指令==========================
	/** 聊天 */
	public static final int MAHJONG_CHAT_MSG = 11001;

	/** gm命令 */
	public static final int MAHJONG_GM_MSG = 11002;

	/** 屌翻老的连接 */
	public static final int Disconnet_S2C_Msg = 11003;

	// ===============================战绩=============================
	/** 拉去战绩面板 */
	public static final int MAHJONG_ZHANJI_INFO = 12001;
	/** 战绩详情 */
	public static final int MAHJONG_ZHANJI_DETAILS_INFO = 12002;
	/** 战绩回放 */
	public static final int MAHJONG_ZHANJI_PLAYBACK = 12003;

	// =============================公告===============================
	/** 拉取公告列表 */
	public static final int Announce_List_Msg = 13001;
	/** 增加/更新公告 */
	public static final int Announce_AddOrUpdate_Msg = 13002;
	/** 删除公告 */
	public static final int Announce_Delete_Msg = 13003;

	// =============================邮件===============================
	/** 推送给客户端 有邮件的通知 */
	public static final int MAIL_NEW = 14001;
	/** 拉取邮件信息 */
	public static final int MAIL_GETINFO = 14002;
	/** 打开一个邮件 */
	public static final int MAIL_OPEN = 14003;
	/** 删除邮件 */
	public static final int MAIL_REMOVE = 14004;
	/** 领取钻石 */
	public static final int MAIL_GAINDIAMOND = 14005;

	// =============================钻石变化===============================
	/** 钻石变化 */
	public static final int DIAMOND_CHANGE = 15001;

	// =============================版本公告拉取===============================
	/** 版本公告拉取 */
	public static final int VERSION_NOTICE_REFRESH = 16001;

	// ==============================代理=================================
	/** 代理出售钻石前操作 */
	public static final int AGENT_TRANSFER_READY = 17001;
	/** 代理出售钻石 */
	public static final int AGENT_TRANSFER = 17002;
	/** 代理出售记录 */
	public static final int AGENT_TRANSFER_LOG = 17003;
	/** 开通代理 */
	public static final int AGENT_OPEN = 17004;
	/** 代理解散房间 */
	public static final int AGENT_DIS_MISS_ROOM = 17005;
}
