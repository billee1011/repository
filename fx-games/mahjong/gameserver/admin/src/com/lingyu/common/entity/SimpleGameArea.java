package com.lingyu.common.entity;

import com.alibaba.fastjson.annotation.JSONField;

public class SimpleGameArea {
	@JSONField(name = "pid")
	private String pid;
	@JSONField(name = "pname")
	private String platformName;
	@JSONField(name = "sid")
	private int id;
	@JSONField(name = "sname")
	private String name;
	@JSONField(name = " worldId")
	private int worldId;
	@JSONField(name = "worldName")
	private String worldName;
	@JSONField(name = "status")
	private int status;
	

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getPid() {
		return pid;
	}

	public void setPid(String pid) {
		this.pid = pid;
	}

	public String getPlatformName() {
		return platformName;
	}

	public void setPlatformName(String platformName) {
		this.platformName = platformName;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getWorldId() {
		return worldId;
	}

	public void setWorldId(int worldId) {
		this.worldId = worldId;
	}

	public String getWorldName() {
		return worldName;
	}

	public void setWorldName(String worldName) {
		this.worldName = worldName;
	}

	
}
