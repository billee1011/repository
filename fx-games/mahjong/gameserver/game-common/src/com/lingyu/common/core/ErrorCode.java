package com.lingyu.common.core;

public abstract class ErrorCode {
	public static final String CODE = "code";
	public static final String RESULT = "result";
	/** 失败 */
	public static final int EC_FAILED = 0;
	public static final int FAILED = 0;

	/** 成功 */
	public static final int EC_OK = 1;
	public static final int OK = 1;

	// ***********************************************通用模块*********************************
	/** 用户已存在 */
	public static final int USER_EXIST = 1001;
	/** 用户不存在 */
	public static final int USER_NOT_EXIST = 1002;
	/** 角色不存在 */
	public static final int ROLE_NOT_EXIST = 1003;
	/** 房间不存在 */
	public static final int ROOM_NOT_EXIST = 1004;
	/** 游戏已开始 */
	public static final int GAME_AREADY_START = 1005;
	/** 超出房间人数 */
	public static final int OVER_ROOM_NUM = 1006;
	/** 你没有房间。怎么打的牌？ */
	public static final int ROLE_NOT_IN_ROOM = 1007;
	/** 操作异常 */
	public static final int OPERATE_ERROR = 1008;
	/** 没有这张牌。怎么打的 */
	public static final int ROLE_NOT_THIS_EVERY = 1009;
	/** 不该你打牌啊亲~ */
	public static final int ROLE_ID_NOT_SAME = 1010;
	/** 你已经有房间里。不能再创建 */
	public static final int ROOM_EXIST_NOT_CREATE = 1011;
	/** 你没有房间，不能解散 */
	public static final int ROOM_NOT_EXIST_NOT_DIS = 1012;
	/** 房间已被房主{0}解散 */
	public static final int Error_1013 = 1013;
	/** 你不是房间的创建者，不能解散 */
	public static final int NOT_CREATE_LEADER = 1014;
	/** 你已经在房间里，不能再加入 */
	public static final int ROLE_IN_ROOM_NOT_JOIN = 1015;
	/** 你走错协议了 */
	public static final int ERROR_MSGTYPE = 1016;
	/** 消耗房卡全局配置异常 */
	public static final int COST_CAR_ERROR = 1017;
	/** 您的账户在其他地方登陆，请注意账户安全 */
	public static final int USERID_RELOAD = 1018;
	/** 当前局数已经打完了。 */
	public static final int CUR_JUSHU_FINISH = 1019;
	/** 现在还不能查看总战绩 */
	public static final int NOT_LOOK_SUM_SCORE = 1020;

	/** 玩家{0}申请解散房间，请等待其他玩家选择(超过{1}分钟未做选择，则默认同意) */
	public static final int Error_1021 = 1021;
	/** 玩家{0}申请解散房间，请选择是否同意(超过{1}分钟未做选择，则默认同意) */
	public static final int Error_1022 = 1022;
	/** 玩家{0}拒绝解散房间，牌局继续 */
	public static final int Error_1023 = 1023;
	/** 玩家{0}同意，解散房间成功 */
	public static final int Error_1024 = 1024;
	/** 房间{0}超过{1}分钟未全部做出选择，已默认解散 */
	public static final int Error_1025 = 1025;

	/** 有玩家解散房间,你怎么还点的开始游戏? */
	public static final int START_GAME_ERROR_OPERATE = 1026;
	/** 房间已存在 */
	public static final int ROOM_EXIST = 1027;
	/** 你有标签操作，请先操作标签 */
	public static final int ROLECACHE_EXIST_SIGN = 1028;
	/** 查看战绩没有数据 */
	public static final int LOOK_ZHANJI_NOT_DATA = 1029;
	/** 找不到邮件信息 */
	public static final int NOT_FIND_MAIL_INFO = 1030;
	/** 钻石为0.领个毛线啊 */
	public static final int DIAMOND_ZERO = 1031;
	/** 消耗货币类型错误 */
	public static final int ACCOUNT_TYPE_ERROR = 1032;
	/** 很抱歉,角色钻石不足 */
	public static final int DIAMOND_NOT_ENOUGH = 1033;
	/** 找不到回放记录 */
	public static final int PLAY_BACK_NOT_FIND = 1034;
	/** 数据错误（碰杠胡过路杠。signMap不为空的） */
	public static final int DATA_ERROR = 1035;
	/** 标签操作数据异常（signMap找不到roleid） */
	public static final int SIGN_OPERATE_ERROR = 1036;
	/** 错误的登陆类型 */
	public static final int LOGIN_TYPE_ERROR = 1037;
	/** 找不到session */
	public static final int NOT_FIND_SESSION = 1038;
	/** 游客登陆一个设备只能有一个账号，账号为空了 */
	public static final int YOUKE_USER_ID_NULL = 1039;
	/** 服务器升级维护中，请耐心等待 */
	public static final int SERVER_MAINTAINING = 1040;
	/** 拉取微信信息的时候。openid为null */
	public static final int WEIXIN_OPENID_NULL = 1041;
	/** 用两张相同的牌来吃，傻逼么 */
	public static final int EAT_USE_IDENTICAL = 1042;
	/** 自己打的牌自己不能吃 */
	public static final int SLEF_EAT = 1043;
	/** 不是代理 */
	public static final int IS_NOT_AGENT = 1044;
	/** 无权限开授权代理 */
	public static final int NO_JUR_OPEN_AGENT = 1045;
	/** 格式错误 */
	public static final int PATTEN_ERROR = 1046;
	/** 转账房卡必须大于0 */
	public static final int TRANSFER_DIAMOUD_ERROR = 1046;
	/** 权限不足 */
	public static final int JURISDICTION_INSUF = 1047;
}
