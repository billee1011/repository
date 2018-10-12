package com.cai.common.define;

/**
 * 玩家参数值,数据库用的
 * @author run
 *
 */
public enum EAccountParamType {
	
	LAST_LOGIN_NOTICE(1,"最后查看登录公告的时间",0),
	TODAY_ADD_MONEY(2,"今天新增加的铜钱数量",1),
	TODAY_ADD_GOLD(3,"今日新增金币数量 ",1),
	HISTORY_SAMLL_BRAND_TIMES(4,"历史小牌局数",0),
	HISTORY_BIG_BRAND_TIMES(5,"历史大牌局数",0),
	DAY_SHARE_TIME(6,"每日分享的时间",0),
	DRAW_SHARE_DOWN(7,"是否领取分享下载金币",0),
	;
	
	private int id;
	
	private String desc;
	
	/**
	 * 类型 0=普通  1今日(每天重置)
	 */
	private int type;
	
	EAccountParamType(int id,String desc,int type){
		this.id = id;
		this.desc = desc;
		this.type = type;
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


	public int getType() {
		return type;
	}


	public void setType(int type) {
		this.type = type;
	}
	
	
	
	
	
}
