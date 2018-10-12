package com.lingyu.admin.privilege;

import java.lang.reflect.Field;

public class PrivilegeConstant {
	
	/***************** 统计系统 *******************/
	@Resource(name = "统计系统", type = ResourceType.MODULE)
	public static final int MODULE_STAT = 1;
	@Resource(name = "在线人数", module = MODULE_STAT)
	public static final int MENU_ONLINE_NUM = 10001;
	
	/****************** 客服系统 ***********************/
	@Resource(name = "客服系统", type = ResourceType.MODULE)
	public static final int MODULE_PSS = 6;
	@Resource(name = "公告管理", module = MODULE_PSS)
	public static final int MENU_ANNOUNCEMENT = 60001;
	@Resource(name = "用户补偿申请", module = MODULE_PSS)
	public static final int MENU_PLAY_RECOUP = 60002;
	@Resource(name = "用户惩罚", module = MODULE_PSS)
	public static final int MENU_PLAYER_PUBNISH = 60003;
	@Resource(name = "用户管理", module = MODULE_PSS)
	public static final int MENU_PLAYER_USER_MANAGER = 60005;
	@Resource(name = "用户补偿审核", module = MODULE_PSS)
	public static final int MENU_PLAY_RECOUP_CHECK = 60017;
	@Resource(name = "发送邮件", module = MODULE_PSS)
	public static final int MENU_PLAY_SENDMAIL = 60019;
	
	/****************** 运维系统 ***********************/
	@Resource(name = "运维工具", type = ResourceType.MODULE)
	public static final int MODULE_PS = 4;
	@Resource(name = "服务器维护管理", module = MODULE_PS)
	public static final int MENU_SERVER_MANAGER = 40001;
	@Resource(name = "服务器列表", module = MODULE_PS)
	public static final int MENU_GAME_AREA_LIST = 40004;
	
	
	/********************* 后台管理 ****************/
	@Resource(name = "后台管理", type = ResourceType.MODULE)
	public static final int MODULE_BACK_STAGE = 8;
	@Resource(name = "平台管理", module = MODULE_BACK_STAGE)
	public static final int MENU_PLATFORM = 80001;
	@Resource(name = "账号管理", module = MODULE_BACK_STAGE)
	public static final int MENU_USER = 80003;
	@Resource(name = "用户权限管理", module = MODULE_BACK_STAGE)
	public static final int MENU_ROLE = 80004;
	@Resource(name = "操作日志", module = MODULE_BACK_STAGE)
	public static final int MENU_OPERATION_LOG = 80006;
	/*******************************************/
	
	/***/
	public static void main(String[] args) throws Exception {
		Field[] fs = PrivilegeConstant.class.getDeclaredFields();
		StringBuilder sb = new StringBuilder();
		int index = 0;
		for(Field f : fs){
			int value = (int)f.get(null);
			if(value >= 10000){
				if(index++ > 0){
					sb.append(",");
				}
				sb.append(value);
			}
		}
		System.out.println(sb);
	}
}
