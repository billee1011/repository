package com.cai.common.define;

/**
 * redis消息队列主题
 * @author run
 *
 */
public enum ERedisTopicType {
	
	topicCenter("topicCenter","仅中心处理"),
	topicAll("topicAll","所有应用处理"),
	topicProxy("topicProxy","仅代理服"),
	topicLogic("topicLogic","仅逻辑服"),
	topProxAndLogic("topProxAndLogic","代理服和逻辑服"),
	;
	
	
	
	private String id;
	
	private String desc;
	
	ERedisTopicType(String id,String desc){
		this.id = id;
		this.desc = desc;
	}
	
	
	public static ERedisTopicType getEMsgType(String id)
	{
		for (ERedisTopicType c : ERedisTopicType.values())
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

	
	
	
}
