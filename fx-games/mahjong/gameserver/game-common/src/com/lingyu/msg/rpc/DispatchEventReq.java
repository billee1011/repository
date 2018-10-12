package com.lingyu.msg.rpc;

import java.util.ArrayList;
import java.util.List;

public class DispatchEventReq<T> {

	private long roleId;
	private List<String> handlerList = new ArrayList<>();
	private T entity;
	private transient int worldId;
	
	public int getWorldId() {
		return worldId;
	}
	public void setWorldId(int worldId) {
		this.worldId = worldId;
	}
	public void addHandle(String name) {
		handlerList.add(name);
	}
	public boolean isEmpty(){
		return handlerList.isEmpty();
	}

	public long getRoleId() {
		return roleId;
	}

	public void setRoleId(long roleId) {
		this.roleId = roleId;
	}

	public List<String> getHandlerList() {
		return handlerList;
	}

	public void setHandlerList(List<String> handlerList) {
		this.handlerList = handlerList;
	}

	public T getEntity() {
		return entity;
	}

	public void setEntity(T entity) {
		this.entity = entity;
	}

}
