package com.lingyu.admin.config;

public class SyncConfig {

	private String userName;

	private String password;
	private String path;
	private int port;

	public SyncConfig(String userName, String password, String path, int port) {
		this.userName = userName;
		this.password = password;
		this.path = path;
		this.port = port;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

}
