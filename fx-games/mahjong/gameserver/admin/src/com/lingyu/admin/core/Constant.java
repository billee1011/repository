package com.lingyu.admin.core;

public class Constant {

	public final static String USER_KEY = "USER";
	public final static String AREA_ID_KEY = "AREAR_ID";

	/** Session中的Key：验证码 */
	public final static String SESSION_KEY_VERIFY_CODE = "VERIFY_CODE";

	/** 最多尝试登录失败的次数，连错3次出验证码 */
	public final static int MAX_LOGIN_FAILED_VERIFY_CODE = 3;
	/** 最多尝试登录失败的次数，连错十次封号1小时 */
	public final static int MAX_LOGIN_FAILED_NO_LOGIN = 10;
	
	/** 服务器维护操作 */
	public final static int SERVER_OPT_MAINTAIN = 1;
	/** 服务器开放操作 */
	public final static int SERVER_OPT_OPEN = 0;
	
	
	//版本常量
	/** 服务器版本 */
	public final static int VERSION_TYPE_SERVER = 0;
	/** 策划数据版本 */
	public final static int VERSION_TYPE_DATA = 1;
	
	/** 异常类别-状态不正确 */
	public final static int WEIRED_TYPE_STATUS = 1 << 0;
	/** 异常类别-服务器版本不正确 */
	public final static int WEIRED_TYPE_SERVER_VERSION = 1 << 1;
	/** 异常类别-策划数据版本不正确 */
	public final static int WEIRED_TYPE_DATA_VERSION = 1 << 2;
}
