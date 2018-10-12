package com.cai.common.domain;

import java.io.Serializable;

import io.netty.channel.Channel;

public class Player implements Serializable{
	
	private long account_id;
	
	private long gold;
	
	private int proxy_index;
	
	private long proxy_session_id;
	
	private String account_icon;
	
	private String account_ip;
	
	private String account_ip_addr;
	
	private String nick_name;
	
	private int room_id;
	
	/**
	 * 性别 1=男  2=女
	 */
	private int sex;
	
	/**
	 * 每个会话所承载的网络会话实例
	 */
	private transient Channel channel;

	/**
	 * 位置索引
	 */
	private int _seat_index;
	
	/**
	 * 是否在线
	 */
	private boolean isOnline = true;
	
	public long getAccount_id() {
		return account_id;
	}

	public void setAccount_id(long account_id) {
		this.account_id = account_id;
	}

	public long getGold() {
		return gold;
	}

	public void setGold(long gold) {
		this.gold = gold;
	}

	public int getProxy_index() {
		return proxy_index;
	}

	public void setProxy_index(int proxy_index) {
		this.proxy_index = proxy_index;
	}

	public long getProxy_session_id() {
		return proxy_session_id;
	}

	public void setProxy_session_id(long proxy_session_id) {
		this.proxy_session_id = proxy_session_id;
	}

	public String getAccount_icon() {
		return account_icon;
	}

	public void setAccount_icon(String account_icon) {
		this.account_icon = account_icon;
	}

	public String getAccount_ip() {
		return account_ip;
	}

	public void setAccount_ip(String account_ip) {
		this.account_ip = account_ip;
	}

	public String getNick_name() {
		return nick_name;
	}

	public void setNick_name(String nick_name) {
		this.nick_name = nick_name;
	}

	public int getRoom_id() {
		return room_id;
	}

	public void setRoom_id(int room_id) {
		this.room_id = room_id;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}


	public int get_seat_index() {
		return _seat_index;
	}

	public void set_seat_index(int _seat_index) {
		this._seat_index = _seat_index;
	}

	public boolean isOnline() {
		return isOnline;
	}

	public void setOnline(boolean isOnline) {
		this.isOnline = isOnline;
	}

	public int getSex() {
		return sex;
	}

	public void setSex(int sex) {
		this.sex = sex;
	}

	public String getAccount_ip_addr() {
		return account_ip_addr;
	}

	public void setAccount_ip_addr(String account_ip_addr) {
		this.account_ip_addr = account_ip_addr;
	}

	
	
	
	

}
