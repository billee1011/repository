package com.cai.domain;

import com.cai.service.FirewallServiceImpl;

public class IpFirewallModel {
	
	/**
	 * IP
	 */
	private String ip;
	
	/**
	 * 当前链接数量
	 */
	private int linkCount;
	
	/**
	 * 最后的频率刷新时间
	 */
	private long lastHzFlushTime;
	
	/**
	 * 当前频率内的链接次数
	 */
	private int hzLinkTimes;

	
	/**
	 * 黑名单过期时间
	 */
	private long blackExpirationTime;
	
	
	public boolean verifyIP(){
		if(ip ==null || "".equals(ip))
			return true;
		
		
		long nowTime = System.currentTimeMillis();
		if(blackExpirationTime>nowTime)
			return false;
		
		if(linkCount>FirewallServiceImpl.IP_MAX_LINK){
			return false;
		}
		
		return true;
	}
	
	public IpFirewallModel(String ip){
		this.ip = ip;
	}


	public String getIp() {
		return ip;
	}


	public void setIp(String ip) {
		this.ip = ip;
	}


	public int getLinkCount() {
		return linkCount;
	}


	public void setLinkCount(int linkCount) {
		this.linkCount = linkCount;
	}


	public long getLastHzFlushTime() {
		return lastHzFlushTime;
	}


	public void setLastHzFlushTime(long lastHzFlushTime) {
		this.lastHzFlushTime = lastHzFlushTime;
	}


	public int getHzLinkTimes() {
		return hzLinkTimes;
	}


	public void setHzLinkTimes(int hzLinkTimes) {
		this.hzLinkTimes = hzLinkTimes;
	}


	public long getBlackExpirationTime() {
		return blackExpirationTime;
	}


	public void setBlackExpirationTime(long blackExpirationTime) {
		this.blackExpirationTime = blackExpirationTime;
	}


	
	

	
	
	
	
}
