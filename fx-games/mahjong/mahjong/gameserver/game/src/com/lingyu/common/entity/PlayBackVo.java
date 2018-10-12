package com.lingyu.common.entity;

/**
 * 回放vo
 * @author wangning
 * @date 2017年2月21日 下午6:26:40
 */
public class PlayBackVo {
	private int type; // 回放类型
	private long roleId;// 玩家的id
	private int index; // 玩家当前操作的索引
	private Object obj; // 具体干了啥
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public Object getObj() {
		return obj;
	}
	public void setObj(Object obj) {
		this.obj = obj;
	}
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public long getRoleId() {
		return roleId;
	}
	public void setRoleId(long roleId) {
		this.roleId = roleId;
	}
}
