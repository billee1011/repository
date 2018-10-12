package com.cai.domain;

import java.io.Serializable;
import java.util.concurrent.locks.ReentrantLock;

import com.cai.common.domain.Account;

import io.netty.channel.Channel;

public class Session implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8196240430631276557L;

	/**
	 * 构造
	 */
	public Session() {

	}

	/**
	 * 编号
	 */
	private int sessionId;

	/**
	 * 创建时间
	 */
	private long createTime;

	/**
	 * 账号登录时间
	 */
	private long accountLoginTime;

	/**
	 * 每个会话所承载的网络会话实例
	 */
	private transient Channel channel;

	/**
	 * 帐号ID
	 */
	private long accountID;

	/**
	 * 帐号名
	 */
	private String account_name;

	/**
	 * 玩家编号
	 */
	private int userID;

	/**
	 * 最近一次会话刷新时间
	 */
	private long refreshTime;

	/**
	 * 客户端IP
	 */
	private String clientIP;

	private Account account;

	private final ReentrantLock mainLock = new ReentrantLock();

	public int getSessionId() {
		return sessionId;
	}

	public void setSessionId(int sessionId) {
		this.sessionId = sessionId;
	}

	public long getCreateTime() {
		return createTime;
	}

	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	public long getAccountID() {
		return accountID;
	}

	public void setAccountID(long accountID) {
		this.accountID = accountID;
	}

	public int getUserID() {
		return userID;
	}

	public void setUserID(int userID) {
		this.userID = userID;
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

	public String getAccount_name() {
		return account_name;
	}

	public void setAccount_name(String account_name) {
		this.account_name = account_name;
	}

	public long getAccountLoginTime() {
		return accountLoginTime;
	}

	public void setAccountLoginTime(long accountLoginTime) {
		this.accountLoginTime = accountLoginTime;
	}

	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

}
