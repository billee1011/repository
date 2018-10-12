package com.lingyu.msg.http;

import java.util.Date;

public class GetServerInfo_S2C_Msg extends HttpMsg {

	private int status;
	
	private int times;//重启次数
	
	private Date startTime;
	
	private Date maintainTime;
	
	private Date combineTime;
	
	private int MaxConcurrentUser;
	private int gray;
	

	public int getGray() {
		return gray;
	}

	public void setGray(int gray) {
		this.gray = gray;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getTimes() {
		return times;
	}

	public void setTimes(int times) {
		this.times = times;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getMaintainTime() {
		return maintainTime;
	}

	public void setMaintainTime(Date maintainTime) {
		this.maintainTime = maintainTime;
	}

	public Date getCombineTime() {
		return combineTime;
	}

	public void setCombineTime(Date combineTime) {
		this.combineTime = combineTime;
	}

	public int getMaxConcurrentUser() {
		return MaxConcurrentUser;
	}

	public void setMaxConcurrentUser(int maxConcurrentUser) {
		MaxConcurrentUser = maxConcurrentUser;
	}
	
	
	
}
