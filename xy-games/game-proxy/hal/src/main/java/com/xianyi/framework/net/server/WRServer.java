package com.xianyi.framework.net.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WRServer {

	private static Logger logger = LoggerFactory.getLogger(WRServer.class);
	

	public abstract void start();

	public abstract void shutdown();



	

}
