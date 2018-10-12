package com.lingyu.admin.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 */
public abstract class ErrorCode {
	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface Comment {
		public String text();
	}

	@Comment(text = "失败")
	public static final String EC_FAILED = "0";
	@Comment(text = "成功")
	public static final String EC_OK = "1";
	@Comment(text = "Cache出错")
	public static final String EC_ACCESS_CACHE_ERROR = "2";
	@Comment(text = "数据库出错")
	public static final String EC_ACCESS_DB_ERROR = "3";
	@Comment(text = "不允许调用Debug指令")
	public static final String EC_DEBUG_NOT_ALLOWED = "4";
	@Comment(text = "C2S协议为空")
	public static final String MESSAGE_NULL = "5";
	@Comment(text = "参数为空")
	public static final String PARAMATER_NULL = "6";
	@Comment(text = "平台不存在该玩家账号，可能连接平台有误")
	public static final String PLATFORM_ACCOUNT_NOT_EXISTED = "7";
	
	
	/**
	 * 登录模块的ErrorCode(900-1000)
	 */
	@Comment(text = "验证码错误，请重新输入.")
	public static final String LOGIN_VERIFYCODE_ERROR = "900";
	@Comment(text = "用户名不存在，请重新输入.")
	public static final String LOGIN_USERNAME_ERROR = "901";
	@Comment(text = "你已连续登录错误3次，请输入验证码.")
	public static final String LOGIN_PASSWORD_ERROR_3 = "902";
	@Comment(text = "你已连续登录错误10次，请1小时后再登录.")
	public static final String LOGIN_PASSWORD_ERROR_10 = "903";
	@Comment(text = "密码错误，请重新输入.")
	public static final String LOGIN_PASSWORD_ERROR = "904";
	@Comment(text = "参数不全.")
	public static final String EC_NEED_PARA = "-1";
	@Comment(text = "加密签名错误.")
	public static final String EC_SIGN = "-2";
	@Comment(text = "请求超时(链接已过期).")
	public static final String EC_TIME_OUT = "-3";
	@Comment(text = "非法帐号.")
	public static final String EC_ILLEGAL_ACCOUNT = "-4";
	@Comment(text = "登陆失败.")
	public static final String EC_LOGIN_FAIL = "-5";
	
	/**
	 * 模块1 服务器内部处理
	 */
	@Comment(text = "找不到用户")
	public static final String NO_USER = "1001";
	@Comment(text = "获取Token超时")
	public static final String CANT_GET_TOKEN = "1002";
	@Comment(text = "没有该用户名")
	public static final String LOGIN_NO_USRE = "1003";
	@Comment(text = "密码错误")
	public static final String LOGIN_ERROR_PASSWORD = "1004";
	@Comment(text = "角色加载失败")
	public static final String ROLE_LOAD_FAIL = "1005";
	@Comment(text = "用户名长度不合法(3-32个字符)")
	public static final String NAME_LENGTH_ERROR = "1006";
	@Comment(text = "密码长度不合法(3-32个字符)")
	public static final String PASSWORD_LENGTH_ERROR = "1007";
	@Comment(text = "用户名已存在")
	public static final String USER_NAME_EXIST = "1008";
	@Comment(text = "该游戏区还存在服务器信息,不能删除")
	public static final String DELETE_AREA_SERVER_EXIST = "1009";
	@Comment(text = "还有用户分配在该游戏区,不能删除")
	public static final String DELETE_AREA_USRE_EXIT = "1010";
	@Comment(text = "服务器信息加载失败")
	public static final String GAME_SERVER_LOAD_FAIL = "1011";
	@Comment(text = "还有管理员是该角色,不能删除")
	public static final String DELETE_ROLE_USER_EXIT = "1012";
	@Comment(text = "不能重复选择游戏区")
	public static final String GAME_AREA_EXIT = "1013";
	@Comment(text = "选择游戏区不存在")
	public static final String GAME_AREA_DATA_WRONG = "1014";
	@Comment(text = "用户名为空")
	public static final String USER_NAME_EMPTY = "1015";
	@Comment(text = "用户ID为0")
	public static final String USER_ID_ZERO = "1016";
	@Comment(text = "同一用户不许重复创建角色")
	public static final String CREATE_ROLE_AGAIN = "1017";
	@Comment(text = "角色名已存在")
	public static final String ROLE_NAME_EXIST = "1018";
	@Comment(text = "选择平台不存在")
	public static final String PLATFORM_DATA_WRONG = "1019";
	@Comment(text = "角色不存在")
	public static final String ROLE_NAME_NOT_EXIST = "1020";
	/**
	 * 模块10 登录
	 */
	@Comment(text = "非法sid")
	public static final String ILLEGAL_SID = "2001";
	@Comment(text = "角色名重复")
	public static final String DUPLICATE_NAME = "2002";
	@Comment(text = "验证sid失败")
	public static final String VALIDATE_TOKEN_FAILED = "2003";
	@Comment(text = "sid已失效或登陆验证超时,请重新登陆")
	public static final String VALIDATE_TOKEN_INVALID = "2004";
	@Comment(text = "服务器升级维护中，请耐心等待")
	public static final String SERVER_MAINTAINING = "2005";
	@Comment(text = "注册人数已达上限")
	public static final String ROLE_CREATE_UP_LIMIT = "2007";
	@Comment(text = "同时在线人数达上限")
	public static final String ROLE_ONLINE_UP_LIMIT = "2008";
	@Comment(text = "由于\"{0}\"该账号被封")
	public static final String ROLE_PUNISH_BAN = "2009";
	@Comment(text = "对不起,服务器开始维护,请喝杯咖啡先 : )")
	public static final String KICK_OFF = "2010";
	@Comment(text = "对不起,你可能使用了非法外挂,请自觉关闭,小心账号被盗")
	public static final String ACCELERATOR = "2011";
	@Comment(text = "角色名称不合法，请检查长度规范及是否包含敏感字符")
	public static final String ROLE_NAME_IS_NOT_RIGHTFUL = "2012";
	
	/**
	 * 查询系统
	 */
	@Comment(text = "不能执行插入，更新，删除等违法SQL，您的操作已被记录")
	public static final String SQL_NOT_PERMITION = "30001";
	@Comment(text = "对不起，数据不存在，请更换查询条件")
	public static final String NO_DATA = "30002";
	@Comment(text = "对不起，数据不合法")
	public static final String NO_LEGAL = "30003";
	/**运营系统*/
	@Comment(text = "对不起,该战区已被注册")
	public static final String BATTLE_AREA_REGISTERED = "40001";
	@Comment(text = "对不起,战区不存在")
	public static final String BATTLE_AREA_NOT_EXISTED = "40002";
	@Comment(text = "对不起,目标跨服服务器维护中，不能创建战区")
	public static final String BATTLE_AREA_MAINTAIN = "40003";
	@Comment(text = "对不起,国家不存在")
	public static final String COUNTRY_NOT_EXISTED = "40004";
	@Comment(text = "对不起,有关联服务器，国家不能删除")
	public static final String COUNTRY_CANT_DELETED = "40005";
	@Comment(text = "对不起,国家没有绑定战区")
	public static final String COUNTRY_NOT_ATTACHED = "40006";
	@Comment(text = "对不起,最多允许n个国家")
	public static final String TOO_MANY_COUNTRIES = "40007";
	
	/**
	 * 公告系统
	 */
	@Comment(text = "发布的公告时间已过")
	public static final String ANNOUNCE_TIME_PASSED = "40100";
	
	/**
	 * 官方商城
	 */
	@Comment(text = "官方商城道具不存在")
	public static final String GLOBAL_MALL_ITEM_NOT_EXISTS = "40200";
	@Comment(text = "官方商城道具没有上架")
	public static final String GLOBAL_MALL_ITEM_NOT_ON_SHELF = "40201";
	
	/**
	 * 策划数据管理
	 */
	@Comment(text = "策划数据不存在")
	public static final String GAME_DATA_NOT_EXISTS = "40300";
	@Comment(text = "策划数据无需同步")
	public static final String GAME_DATA_SYNC_NOTHING = "40301";
}
