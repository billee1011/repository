package com.lingyu.noark.data.accessor.redis.cmd;

import java.io.Serializable;

public class Zrem {

	private Serializable roleId;
	private String key;
	private String member;

	public Zrem() {

	}

	public Zrem(Serializable roleId, String key, String member) {
		this.roleId = roleId;
		this.key = key;
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

	public final void setMember(String member) {
		this.member = member;
	}

	public final void setRoleId(Serializable roleId) {
		this.roleId = roleId;
	}
}