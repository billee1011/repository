package com.lingyu.noark.data.accessor.redis.cmd;

import java.io.Serializable;

public class Zincrby {

	private Serializable roleId;
	private String key;
	private double sorce;
	private String member;

	public Zincrby() {

	}

	public Zincrby(Serializable roleId, String key, double sorce, String member) {
		this.roleId = roleId;
		this.key = key;
		this.sorce = sorce;
		this.member = member;
	}

	public Serializable getRoleId() {
		return roleId;
	}

	public final String getKey() {
		return key;
	}

	public final void setKey(String key) {
		this.key = key;
	}

	public final String getMember() {
		return member;
	}

	public final double getSorce() {
		return sorce;
	}

	public final void setSorce(double sorce) {
		this.sorce = sorce;
	}

	public final void setMember(String member) {
		this.member = member;
	}

	public final void setRoleId(Serializable roleId) {
		this.roleId = roleId;
	}
}