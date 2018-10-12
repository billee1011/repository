package com.lingyu.common.entity;

public class MahjongResultDetailsData {
	private long roleId; // roleid
	private String name; // 名字
	private int jifen; // 对战的积分
	public long getRoleId() {
		return roleId;
	}
	public void setRoleId(long roleId) {
		this.roleId = roleId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getJifen() {
		return jifen;
	}
	public void setJifen(int jifen) {
		this.jifen = jifen;
	}
}
