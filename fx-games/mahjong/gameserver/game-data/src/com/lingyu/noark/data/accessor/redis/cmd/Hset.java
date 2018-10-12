package com.lingyu.noark.data.accessor.redis.cmd;

import java.io.Serializable;

public class Hset {

	private Serializable roleId;
	private String key;
	private String field;
	private String value;

	public Hset() {
	}

	public Hset(Serializable roleId, String key, String field, String value) {
		this.roleId = roleId;
		this.key = key;
		this.field = field;
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

	public final String getField() {
		return field;
	}

	public final void setField(String field) {
		this.field = field;
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