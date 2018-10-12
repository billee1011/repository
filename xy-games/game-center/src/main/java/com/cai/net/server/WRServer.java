package com.cai.net.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.IoStatisticsModel;

public abstract class WRServer {

	private static Logger logger = LoggerFactory.getLogger(WRServer.class);
	
	protected IoStatisticsModel statistics;

	public abstract void start();

	public abstract void shutdown();

	public IoStatisticsModel getStatistics() {
		return statistics;
	}

	public void setStatistics(IoStatisticsModel statistics) {
		this.statistics = statistics;
	}
	
	

}
