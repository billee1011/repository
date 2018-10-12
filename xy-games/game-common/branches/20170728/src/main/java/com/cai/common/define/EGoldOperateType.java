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
	DAY_SHARE(15,"每日分享得卡"),
	FRIEND_DOWN(16,"好友下载"),
	SHARE_DOWN(17,"通过分享下载"),
	FAILED_ROOM(18,"开局失败"),
	REAL_OPEN_ROOM(19,"真实开房"),
	REAL_RMB_COUNT(20,"每日返利"),
	REAL_RMB_RECEIVER(21,"提现"),
	MONEY_DUIHUAN(22,"兑换金币"),
	REAL_INCOME_RECEIVER(23,"推广员提现"),
	RECOMMEND_PLAYER(24,"推广玩家返利"),
	RECOMMEND_AGENT(25,"推广代理返利"),
	AGENT_RECHARGE_RECEIVER(26,"代理充值返利"),
	LOGIN_ACTIVITY(27,"登录活动"),
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
