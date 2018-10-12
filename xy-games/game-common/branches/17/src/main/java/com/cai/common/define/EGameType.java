package com.cai.common.define;

/**
 * 游戏类型
 * @author run
 *
 */
public enum EGameType {
	
	MJ(1,"MJ","湖南麻将"),
	PDK(2,"PDK","湖北麻将"),

	
	;
	private int id;
	
	private String idstr;
	
	private String name;
	
	
	EGameType(int id,String idstr,String name){
		this.id = id;
		this.idstr = idstr;
		this.name = name;
	}
	
	
	public static EGameType ELogType(int id)
	{
		for (EGameType c : EGameType.values())
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


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}

	
	
}
