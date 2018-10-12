package com.lingyu.admin.config;

import java.io.File;

public class MoneyMonitorConfig {
	private String moneyMonitorDetailDir;
	private String serverIp;
	private int serverPort;
	
	public MoneyMonitorConfig(){}
	
	public MoneyMonitorConfig(String moneyMonitorDetailDir, String serverIp, int serverPort){
		this.moneyMonitorDetailDir = moneyMonitorDetailDir;
		this.serverIp = serverIp;
		this.serverPort = serverPort;
		init();
	}
	
	private void init(){
		File dir = new File(moneyMonitorDetailDir);
		if(!dir.exists()){
			dir.mkdirs();
		}
	}

	public String getMoneyMonitorDetailDir() {
		return moneyMonitorDetailDir;
	}

	public void setMoneyMonitorDetailDir(String moneyMonitorDetailDir) {
		this.moneyMonitorDetailDir = moneyMonitorDetailDir;
	}

	public String getServerIp() {
		return serverIp;
	}

	public void setServerIp(String serverIp) {
		this.serverIp = serverIp;
	}

	public int getServerPort() {
		return serverPort;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}
}
