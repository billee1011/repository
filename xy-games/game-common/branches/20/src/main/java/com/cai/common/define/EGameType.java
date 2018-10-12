package com.cai.common.define;

/**
 * 游戏类型
 * @author run
 *
 */
public enum EGameType {
	
	MJ(1,"MJ","湖南麻将"),
	PDK(2,"PDK","湖北麻将"),
	AY(3,"AY","安阳麻将"),
	SG(4,"SG","双鬼麻将"),
	FLS(5,"FLS","临湘福禄寿"),
	
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

	
	
	//麻将小类型
	public static String getMJname(int v2) {
		if(v2==0) {
			return "转转";
		}else if(v2==1) {
			return "长沙";
		}else if(v2==2) {
			return "红中";
		}else if(v2==3) {
			return "双鬼";
		}else if(v2==4) {
			return "株洲";
		}else if(v2==2001) {
			return "晃晃";
		}else if(v2==3001) {
			return "安阳";
		}else if(v2==3002) {
			return "林州";
		}else if(v2==3003) {
			return "河南";
		}else if(v2==3004) {
			return "河南红中";
		}else if(v2==4001) {
			return "双鬼";
		}else if(v2==5001) {
			return "临湘福禄寿";
		}
		return v2+"";
	}
	
}
