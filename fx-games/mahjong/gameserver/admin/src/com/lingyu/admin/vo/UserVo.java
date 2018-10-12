package com.lingyu.admin.vo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UserVo {
	private int id;
	
	private String name;
	
	private String nickName;
	
	private String email;
	
	private String lastAreaName;
	
	private String roleName;
	
	private Date addTime;
	
	private transient List<String> platformIdList = new ArrayList<>();

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

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getLastAreaName() {
		return lastAreaName;
	}

	public void setLastAreaName(String lastAreaName) {
		this.lastAreaName = lastAreaName;
	}

	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	public Date getAddTime() {
		return addTime;
	}

	public void setAddTime(Date addTime) {
		this.addTime = addTime;
	}

	public List<String> getPlatformIdList() {
		return platformIdList;
	}

	public void setPlatformIdList(List<String> platformIdList) {
		this.platformIdList = platformIdList;
	}
}
