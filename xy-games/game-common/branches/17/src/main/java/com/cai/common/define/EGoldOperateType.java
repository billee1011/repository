package com.cai.common.define;

/**
 * 房卡操作类型
 * @author run
 *
 */
public enum EGoldOperateType {
	
	//不要用 1 -1
	
	OPEN_ROOM(10,"开房间"),
	OSS_OPERATE(11,"后台操作加卡"),
	PROXY_GIVE(12,"代理赠送"),
	SHOP_PAY(13,"商店"),
	PAY_CENTER(14,"充值中心"),
	OSS_OPERATE_DEC(11,"后台操作减卡"),
	;
	private int id;
	
	private String idstr;
	
	
	
	EGoldOperateType(int id,String idstr){
		this.id = id;
		this.idstr = idstr;
	}
	
	
	public static EGoldOperateType ELogType(int id)
	{
		for (EGoldOperateType c : EGoldOperateType.values())
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
