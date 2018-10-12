package com.lingyu.common.config;

import java.util.List;

import com.lingyu.common.entity.Cache;

public interface IConfig {
	public String getServerVersion();

	public int getType();

	public int getSubType();

	public int getWorldId();

	public String getWorldName();

	public int getWebPort();

	public int getInnerPort();

	public int getTcpPort();

	public int getRpcTimeout();

	public List<Cache> getCacheList();
}
