package com.lingyu.noark.data.accessor.redis.cmd;

import java.io.Serializable;
import java.util.Map;

public class Hmset {

	private Serializable roleId;
	private String key;
	private Map<String, String> hash;

	public Hmset() {
	}

	public Hmset(Serializable roleId, String key, Map<String, String> hash) {
		this.roleId = roleId;
		this.key = key;
		this.hash = hash;
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

	public final Map<String, String> getHash() {
		return hash;
	}

	public final void setHash(Map<String, String> hash) {
		this.hash = hash;
	}

	public final void setRoleId(Serializable roleId) {
		this.roleId = roleId;
	}
}