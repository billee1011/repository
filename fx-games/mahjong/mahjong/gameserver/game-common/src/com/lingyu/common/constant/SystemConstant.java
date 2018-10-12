package com.lingyu.common.constant;

public class SystemConstant {
	// 加密解密的ticket
	public static final String TICKET = "LEYOUKEJI_MAJHONG_SUPER";

	// 服务器类型
	/** 游戏服务器 */
	public final static int SERVER_TYPE_GAME = 1;

	/** 服务器处于正常运行状态 */
	public final static int SERVER_STATUS_OPENING = 0;
	/** 服务器处于运维维护状态,只能允许GM进来 */
	public final static int SERVER_STATUS_MAINTAIN = 1;
	/** 服务器处于停服状态 */
	public final static int SERVER_STATUS_STOPED = 2;
	/** 对不起,你可能使用了非法外挂,请自觉关闭,小心账号被盗 */
	public final static String REASON_PLUGIN = "对不起,你可能使用了非法外挂,请自觉关闭,小心账号被盗";
	/** 对不起,同一账号重复登陆，踢掉前一个连接 */
	public final static String REASON_SAME_USER_LOGIN = "对不起,同一账号重复登陆，踢掉前一个连接";
	/** server RPC超时设置 单位毫秒 */
	public final static int TIMEOUT = 5000;
	/***/
	public static int serverType = SERVER_TYPE_GAME;

	// session的类型
	/** 玩家直连生成的session 绑定1个玩家 */
	public final static int SESSION_TYPE_PLAYER = 1;

	/** 服务器中转RPC生成的session,绑定多个玩家 */
	public final static int SESSION_TYPE_RPC = 2;
	// 服务器系统分组
	/** 业务逻辑组 */
	public static final byte GROUP_BUS_CACHE = 1;
	/** 业务初始化组 */
	public static final byte GROUP_BUS_INIT = 2;
	/** 场景组 */
	public static final byte GROUP_STAGE = 3;
	/** 公共组 */
	public static final byte GROUP_PUBLIC = 4;

	public static final String NAME_BUS_CACHE = "bus_cache";
	public static final String NAME_BUS_INIT = "bus_init";
	public static final String NAME_STAGE = "stage";
	public static final String NAME_PUBLIC = "public";

	/**
	 * 正常下线
	 */
	public static final byte LOGOUT_TYPE_NOMRAL = 0;
	/**
	 * 关闭服务器下线
	 */
	public static final byte LOGOUT_TYPE_SHUTDOWN = 1;

	/** 微信登陆 */
	public final static int LOGIN_TYPE_WEIXIN = 1;
	/** 游客登陆 */
	public final static int LOGIN_TYPE_YOUKE = 2;

	/** 微信登陆的appid的值 */
	public final static String APPID_VAL = "wx359fb80bf39eb40a";
	/** 微信登陆的secret的值 */
	public final static String SECRET_VAL = "bb109e1f7366ab35e8e3d137fa1eba34";

	/** 游客登陆userId的前缀 */
	public final static String YOUKE = "youke";
	/** 游客登陆名字的前缀 */
	public final static String YOUKE_NAME = "游客";

	public static String getGroupName(byte group) {
		switch (group) {
		case GROUP_BUS_CACHE: {

			return NAME_BUS_CACHE;
		}
		case GROUP_BUS_INIT: {

			return NAME_BUS_INIT;
		}
		case GROUP_STAGE: {
			return NAME_STAGE;
		}
		case GROUP_PUBLIC: {
			return NAME_PUBLIC;
		}
		}
		return null;
	}

}
