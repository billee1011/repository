package com.lingyu.common.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class Cache {
	private int type;
	private String ip;
	private int port;
	private int index;
	public Cache(){
		
	}
	public Cache(int type, String ip, int port, int index) {
		this.type = type;
		this.ip = ip;
		this.port = port;
		this.index = index;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
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

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}
	public String toString(){
		return ToStringBuilder.reflectionToString(this);
	}

}
