package com.cai.common.define;

/**
 * 房卡操作类型
 * @author run
 *
 */
public enum EMoneyOperateType {
	
	//不要用 1 -1
	
	LOGIN_ACTIVITY(10,"登录活动"),
	USE_PROP(11,"使用道具"),
	DUIHUAN_PROP(12,"兑换金币"),
	ROOM_COST(13,"牌桌内消耗"),
	QUIT_ROOM_COST(14,"牌桌内逃跑扣费"),
	ADD_MONEY(15,"后台加金币"),
	DEC_MONEY(16,"后台减金币"),
	;
	private int id;
	
	private String idstr;
	
	
	
	EMoneyOperateType(int id,String idstr){
		this.id = id;
		this.idstr = idstr;
	}
	
	
	public static EMoneyOperateType ELogType(int id)
	{
		for (EMoneyOperateType c : EMoneyOperateType.values())
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


	public String getIdstr() {
		return idstr;
	}


	public void setIdstr(String idstr) {
		this.idstr = idstr;
	}


	

	
	
}
