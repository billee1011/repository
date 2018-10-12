package com.lingyu.admin.manager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerManager {
	private static final Logger logger = LogManager.getLogger(ServerManager.class);


	public static ServerManager getInstance() {
		return InstanceHolder.INSTANCE;
	}

	private static class InstanceHolder {
		private static final ServerManager INSTANCE = new ServerManager();
	}

	public void init() {
		
	}
}
