package com.lingyu.admin.vo;


public class DisplayGameAreaEntryVo {
	private int worldId;
	private String worldName;
	private String followerId;
	private int areaId;
	private String areaName;
	private String adminAddress;
	private String gameAddress;
	private String openTime;
	private String combineTime;
	private String restartTime;
	private int status;
	private String serverVersion;
	private String dataVersion;
	private String platformName;
	
	
	public String getPlatformName() {
		return platformName;
	}
	public void setPlatformName(String platformName) {
		this.platformName = platformName;
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
	public String getFollowerId() {
		return followerId;
	}
	public void setFollowerId(String followerId) {
		this.followerId = followerId;
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
	public String getAdminAddress() {
		return adminAddress;
	}
	public void setAdminAddress(String adminAddress) {
		this.adminAddress = adminAddress;
	}
	public String getGameAddress() {
		return gameAddress;
	}
	public void setGameAddress(String gameAddress) {
		this.gameAddress = gameAddress;
	}
	public String getOpenTime() {
		return openTime;
	}
	public void setOpenTime(String openTime) {
		this.openTime = openTime;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public String getServerVersion() {
		return serverVersion;
	}
	public void setServerVersion(String serverVersion) {
		this.serverVersion = serverVersion;
	}
	public String getDataVersion() {
		return dataVersion;
	}
	public void setDataVersion(String dataVersion) {
		this.dataVersion = dataVersion;
	}
	public String getCombineTime() {
		return combineTime;
	}
	public void setCombineTime(String combineTime) {
		this.combineTime = combineTime;
	}
	public String getRestartTime() {
		return restartTime;
	}
	public void setRestartTime(String restartTime) {
		this.restartTime = restartTime;
	}
}
