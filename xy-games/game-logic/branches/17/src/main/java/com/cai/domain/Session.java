package com.cai.domain;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import io.netty.channel.Channel;

public class Session implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8196240430631276557L;

	/**
	 * 构造
	 */
	public Session()
	{
		this.refreshTime = System.currentTimeMillis();
	}

	/**
	 * 编号
	 */
	public long sessionId;

	/**
	 * 每个会话所承载的网络会话实例
	 */
	public transient Channel channel;

	/**
	 * 玩家编号
	 */
	public int userID;

	/**
	 * 帐号ID
	 */
	public int accountID;

	/**
	 * 最近一次会话刷新时间
	 */
	public long refreshTime;
	
	/**
	 * 客户端IP
	 */
	public String clientIP;

	private final ReentrantLock mainLock = new ReentrantLock();

	public long getSessionId() {
		return sessionId;
	}

	public void setSessionId(long sessionId) {
		this.sessionId = sessionId;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	public int getUserID() {
		return userID;
	}

	public void setUserID(int userID) {
		this.userID = userID;
	}

	public int getAccountID() {
		return accountID;
	}

	public void setAccountID(int accountID) {
		this.accountID = accountID;
	}

	public long getRefreshTime() {
		return refreshTime;
	}

	public void setRefreshTime(long refreshTime) {
		this.refreshTime = refreshTime;
	}

	public String getClientIP() {
		return clientIP;
	}

	public void setClientIP(String clientIP) {
		this.clientIP = clientIP;
	}

	public ReentrantLock getMainLock() {
		return mainLock;
	}
	

	

}
