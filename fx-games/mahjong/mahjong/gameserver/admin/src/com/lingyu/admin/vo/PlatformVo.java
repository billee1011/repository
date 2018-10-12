package com.lingyu.admin.vo;

public class PlatformVo {
	private String id;
	private String name;
	private int success;
//	private int areaId;
//	private int worldId;
//	private String areaName;
	
	public PlatformVo() {
	}
	public PlatformVo(String id, String name) {
		this.id = id;
		this.name = name;
	}
	
	

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getSuccess() {
		return success;
	}
	public void setSuccess(int success) {
		this.success = success;
	}
	
}
