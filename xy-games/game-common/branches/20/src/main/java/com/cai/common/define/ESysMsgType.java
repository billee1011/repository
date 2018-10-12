package com.cai.common.define;

/**
 * 系统消息
 * @author run
 *
 */
public enum ESysMsgType {
	
	NONE(1,"普通消息"),
	INCLUDE_ERROR(2,"包含错误码的提示信息"),
	TEXT_ERROR(3,"文本提示"); 
	
	
	private int id;
	
	private String desc;
	
	ESysMsgType(int id,String desc){
		this.id = id;
		this.desc = desc;
	}
	
	
	public static ESysMsgType getEMsgType(int id)
	{
		for (ESysMsgType c : ESysMsgType.values())
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
