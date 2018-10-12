package com.cai.common.define;

/**
 * 玩家参数值,数据库用的
 * @author run
 *
 */
public enum EAccountParamType {
	
	LAST_LOGIN_NOTICE(1,"最后查看登录公告的时间"),
	
	;
	
	private int id;
	
	private String desc;
	
	EAccountParamType(int id,String desc){
		this.id = id;
		this.desc = desc;
	}
	
	
	public static EAccountParamType getEMsgType(int id)
	{
		for (EAccountParamType c : EAccountParamType.values())
		{
			if(c.id==id)
				return c;
		}
		return null;
	}


	public int getId() {
		return id;
	}


	public void setId(int id) {
		this.id = id;
	}


	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
	
	
	
}
