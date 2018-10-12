/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.activity;

/**
 * 
 * 活动类型枚举
 * @author WalkerGeek 
 * date: 2017年9月7日 上午11:56:36 <br/>
 */
public enum ActivityType {

	YUM_CAI_TON_ZI(2,"运财童子"),
	
	RED_PACK_MORE(1,"红包雨")
	;
	
	private int type;	 //活动类型
	
	private String name; //活动名称
	
	
	/**
	 * @param type
	 * @param name
	 */
	private ActivityType(int type, String name) {
		this.type = type;
		this.name = name;
	}

	/**
	 * @return the type
	 */
	public int getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	
	
}
