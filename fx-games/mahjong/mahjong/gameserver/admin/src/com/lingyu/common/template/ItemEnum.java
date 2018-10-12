package com.lingyu.common.template;

/**
 * 
 * @Description 物品类型
 * @author majiayun
 * @date 2014年2月28日 下午4:25:57
 * 
 */
public enum ItemEnum {

	EQUIP_ITEM(1, "装备"), STAGE_ITEM(2, "道具(可消耗)");

	private int id;// ID标识
	private String description;// 描述

	private ItemEnum(int id, String description) {
		this.id = id;
		this.description = description;
	}

	public int getId() {
		return id;
	}

	public String getDescription() {
		return description;
	}

}
