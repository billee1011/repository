package com.lingyu.noark.data.accessor.redis.cmd;

import java.io.Serializable;

public class Get {

	private Serializable roleId;
	private String key;

	public Get() {
	}

	public Get(Serializable roleId, String key) {
		this.roleId = roleId;
		this.key = key;
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

	public final void setRoleId(Serializable roleId) {
		this.roleId = roleId;
	}
}