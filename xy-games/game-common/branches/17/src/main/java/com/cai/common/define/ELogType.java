package com.cai.common.define;

/**
 * 消息类型
 * @author run
 *
 */
public enum ELogType {
	
	//=======玩家的============
	login("login","登录",1),
	register("register","注册",1),
	request("rquest","请求消息",1),
	addGold("addGold","房卡操作",1),
	response("response","消息听响应",1),
	
	
	
	
	//======系统的============
	dbBatch("dbBatch","批量入库",2),
	dbTime("dbTime","数据入库时间",2), 
	onlinePlayer("onlinePlayer","在线玩家数量",2),
	socketConnect("socketConnect","socket链接数",2),
	gameNotice("gameNotice","游戏公告",2),
	gameOnlinePlayer("gameOnlinePlayer","游戏在线人数",2),
	accountOnline("accountOnline","账号在线人数",2),
	
	redisTopicCenter("redisTopicCenter","topicCenter每分钟数量",2),//只有中心有,name=
	
	socketMsgNum("socketMsgNum","服务器socket每分钟消息量",2),//代理，逻辑有
	
	sysFreeRoom("sysFreeRoom","系统释放房间",2),
	
	socketStatePack("socketStatePack","socket流量包统计",2),//v1=入口包数量  v2=出口包数量
	socketStateBytes("socketStateBytes","socket流量字节统计",2),//v1=入口流量字节  v2=出口流量字节
	rmiTest("rmiTest","RMI链接测试",2),//v1=失败的数量
	startJvm("startJvm","开启服务器",2),
	stopJvm("stopJvm","关闭服务器",2),
	requestPool("requestPool","请求消息队列情况",2),
	releaseRedisRoom("releaseRedisRoom","释放redis房间",2),
	jvmMemory("jvmMemory","jvm内存",2),
	kickOnlineAccount("kickOnlineAccount","踢下线",2),
	webRequest("webRequest","web请求",2),
	inRoomWay("inRoomWay","进入房间方式",2),
	inRoomWayStat("inRoomWayStat","进入 房间方式统计",2),
	cardTypdCacheStat("cardTypdCacheStat","牌型缓存统计",2),
	
	
	
	
	//牌局
	parentBrand("parentBrand","大局",3),
	childBrand("childBrand","小局",3),
	accountBrand("accountBrand","玩家牌局索引",3),
	
	
	//代理转卡记录
	proxy_gold("proxy_gold","代理转卡记录",4),
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	;
	private String id;
	
	private String desc;
	
	/**
	 * 类型  1玩家  2系统  3牌局
	 */
	private int type;
	
	ELogType(String id,String desc,int type){
		this.id = id;
		this.desc = desc;
		this.type = type;
	}
	
	
	public static ELogType ELogType(String id)
	{
		for (ELogType c : ELogType.values())
		{
			if(c.id==id)
				return c;
		}
		return null;
	}


	public String getId() {
		return id;
	}


	public void setId(String id) {
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
