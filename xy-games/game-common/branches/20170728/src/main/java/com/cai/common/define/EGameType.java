package com.cai.common.define;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 游戏类型
 * @author run
 *
 */
public enum EGameType {
	
	MJ(1,"MJ","湖南麻将"),
	HBMJ(2,"HBMJ","湖北麻将"),
	AY(3,"AY","安阳麻将"),
	SG(4,"SG","双鬼麻将"),
	FLS(5,"FLS","临湘福禄寿"),
	DT(6,"DT","大厅"),
	PHZ(8,"PHZ","跑胡子"),
	NIUNIU(9,"NIUNIU","牛牛"),
	HJK(10,"HJK","21点"),
	PDK(11,"PDK","跑得快"),
	PHZCD(200,"PHZCD","常德跑胡子"),
	PHZXT(201,"PHZXT","湘潭跑胡子"),
	FPHZ(202,"FPHZ","四人跑胡子"),
	PHZYX(203,"PHZYX","攸县跑胡子"),
	LHQHD(204,"LHQHD","六胡抢"),
	THKHY(205,"THKHY","十胡卡"),
	ZJH(206,"ZJH","炸金花"),
	AXWMQ(207,"AXWMQ","偎麻雀"),
	;
	private int id;
	
	private String idstr;
	
	private String name;
	
	
	EGameType(int id,String idstr,String name){
		this.id = id;
		this.idstr = idstr;
		this.name = name;
	}
	
	private final static Map<Integer,EGameType> map = new HashMap<Integer,EGameType>();
	
	public static final Logger logger = LoggerFactory.getLogger(EGameType.class);
	
	static {
		for (EGameType c : EGameType.values()){
			EGameType type = map.get(c.getId());
			if(type!=null) {
				System.exit(-1);
				logger.error("EGameType定义了相同类型");
			}
			map.put(c.getId(), c);
		}
	}
	
	public static EGameType getEGameType(int id)
	{
		return map.get(id);
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
			return "福禄寿";
		}else if(v2==5002) {
			return "福禄寿20张";
		}else if(v2==8001) {
			return "红黑胡";
		}else if(v2==8002) {
			return "攸县跑胡子";
		}
		return v2+"";
	}
	
}
