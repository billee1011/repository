package com.lingyu.noark.data.accessor.redis.cmd;

import java.io.Serializable;

public class Set {

	private Serializable roleId;
	private String key;
	private String value;

	public Set() {
	}

	public Set(Serializable roleId, String key, String value) {
		this.roleId = roleId;
		this.key = key;
		this.value = value;
	}

	public final Serializable getRoleId() {
		return roleId;
	}

	public final String getKey() {
		return key;
	}

	public final void setKey(String key) {
		this.key = key;
	}

	public final String getValue() {
		return value;
	}

	public final void setValue(String value) {
		this.value = value;
	}

	public final void setRoleId(Serializable roleId) {
		this.roleId = roleId;
	}
}