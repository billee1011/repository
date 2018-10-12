package com.lingyu.msg.rpc;

import com.lingyu.noark.data.OperateType;

public class WriteDataReq<T> {
	private String klassName;
	private OperateType type;
	private T entity;

	public String getKlassName() {
		return klassName;
	}

	public void setKlassName(String klassName) {
		this.klassName = klassName;
	}

	public OperateType getType() {
		return type;
	}

	public void setType(OperateType type) {
		this.type = type;
	}

	public T getEntity() {
		return entity;
	}

	public void setEntity(T entity) {
		this.entity = entity;
	}
}
