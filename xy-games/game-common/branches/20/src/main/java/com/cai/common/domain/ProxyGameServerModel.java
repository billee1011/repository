package com.cai.common.domain;

import java.util.Date;

/**
 * 代理服
 * 
 * @author run
 *
 */
public class ProxyGameServerModel extends DBBaseModel {

	/**
	 * 代理服id
	 */
	private int proxy_game_id;
	/**
	 * 代理服名字
	 */
	private String proxy_game_name;
	/**
	 * 描述
	 */
	private String proxy_desc;
	/**
	 * 外网ip
	 */
	private String public_ip;
	/**
	 * 内网ip
	 */
	private String inner_ip;
	/**
	 * socket端口
	 */
	private int socket_port;
	
	/**
	 * rmi端口
	 */
	private int rmi_port;
	
	
	/**
	 * 安全码
	 */
	private String safe_code;
	/**
	 * 是否开放
	 */
	private int open;
	/**
	 * 程序位置
	 */
	private String software_location;
	
	
	//===========扩展===========
	
	/**
	 * 是否为有效
	 */
	private boolean isValid;
	
	/**
	 * 最后检测的时间
	 */
	private Date lastCheckTime;
	
	

	public int getProxy_game_id() {
		return proxy_game_id;
	}

	public void setProxy_game_id(int proxy_game_id) {
		this.proxy_game_id = proxy_game_id;
	}

	public String getProxy_game_name() {
		return proxy_game_name;
	}

	public void setProxy_game_name(String proxy_game_name) {
		this.proxy_game_name = proxy_game_name;
	}

	public String getProxy_desc() {
		return proxy_desc;
	}

	public void setProxy_desc(String proxy_desc) {
		this.proxy_desc = proxy_desc;
	}

	public String getPublic_ip() {
		return public_ip;
	}

	public void setPublic_ip(String public_ip) {
		this.public_ip = public_ip;
	}

	public String getInner_ip() {
		return inner_ip;
	}

	public void setInner_ip(String inner_ip) {
		this.inner_ip = inner_ip;
	}


	public int getSocket_port() {
		return socket_port;
	}

	public void setSocket_port(int socket_port) {
		this.socket_port = socket_port;
	}

	public int getRmi_port() {
		return rmi_port;
	}

	public void setRmi_port(int rmi_port) {
		this.rmi_port = rmi_port;
	}

	public String getSafe_code() {
		return safe_code;
	}

	public void setSafe_code(String safe_code) {
		this.safe_code = safe_code;
	}

	public int getOpen() {
		return open;
	}

	public void setOpen(int open) {
		this.open = open;
	}

	public String getSoftware_location() {
		return software_location;
	}

	public void setSoftware_location(String software_location) {
		this.software_location = software_location;
	}

	public boolean isValid() {
		return isValid;
	}

	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}

	public Date getLastCheckTime() {
		return lastCheckTime;
	}

	public void setLastCheckTime(Date lastCheckTime) {
		this.lastCheckTime = lastCheckTime;
	}

}
