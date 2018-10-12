/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.config;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.cai.common.config.EnumConfig;
import com.cai.common.config.IConfig;
import com.cai.common.config.struct.Config;
import com.cai.common.config.struct.HostNode;

/**
 *
 * @author wu_hc
 */
@IConfig(tag = EnumConfig.REDIS)
@XmlRootElement(name = "game")
public final class GameConfig implements Config {

	private boolean debug;
	private int port;
	private int index;

	private List<HostNode> logicServers;

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
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

	@javax.xml.bind.annotation.XmlElement(name = "logicServers")
	public List<HostNode> getLogicServers() {
		return logicServers;
	}

	public void setLogicServers(List<HostNode> logicServers) {
		this.logicServers = logicServers;
	}

	@Override
	public String toString() {
		return "GameStruct [debug=" + debug + ", port=" + port + ", index=" + index + ", logicServers=" + logicServers
				+ "]";
	}

}
