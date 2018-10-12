package com.lingyu.admin.vo;

import java.util.Date;

/**
 * 区服输出类
 * 
 * @author Wang Shuguang
 * 
 */
public class GameAreaVo {
	private int worldId;
	private String worldName;
	private int areaId;
	private String areaName;
	// private int type;
	private String externalIp;
	private int tcpPort;
	private String ip;
	private int port;
	// private String platformName;
	private int status;
	private boolean valid;
	private int followerId;
	private int followerAreaId;

	private Date addTime;

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

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getAreaId() {
		return areaId;
	}

	public void setAreaId(int areaId) {
		this.areaId = areaId;
	}

	public String getAreaName() {
		return areaName;
	}

	public void setAreaName(String areaName) {
		this.areaName = areaName;
	}

	public String getExternalIp() {
		return externalIp;
	}

	public void setExternalIp(String externalIp) {
		this.externalIp = externalIp;
	}

	public int getTcpPort() {
		return tcpPort;
	}

	public void setTcpPort(int tcpPort) {
		this.tcpPort = tcpPort;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public int getFollowerId() {
		return followerId;
	}

	public void setFollowerId(int followerId) {
		this.followerId = followerId;
	}

	public Date getAddTime() {
		return addTime;
	}

	public void setAddTime(Date addTime) {
		this.addTime = addTime;
	}

	public int getFollowerAreaId() {
		return followerAreaId;
	}

	public void setFollowerAreaId(int followerAreaId) {
		this.followerAreaId = followerAreaId;
	}

}
