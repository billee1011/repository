package com.lingyu.noark.data.accessor.network;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NetworkManager {
	// RoleId-NetworkDataSource
	private ConcurrentMap<Serializable, NetworkDataSource> dataSourcesByRoleId = new ConcurrentHashMap<>();
	private ConcurrentMap<Serializable, NetworkDataSource> dataSourcesByUserId = new ConcurrentHashMap<>();

	void register(Serializable roleId, NetworkDataSource dataSource) {
		this.dataSourcesByRoleId.put(roleId, dataSource);
		this.dataSourcesByUserId.put(dataSource.getUserId(), dataSource);
	}

	boolean contains(Serializable roleId) {
		return dataSourcesByRoleId.containsKey(roleId);
	}

	NetworkDataSource getNetworkDataSource(Serializable roleId) {
		return dataSourcesByRoleId.get(roleId);
	}

	NetworkDataSource getNetworkDataSourceByUserId(Serializable userId) {
		return dataSourcesByUserId.get(userId);
	}

	public void remove(Serializable roleId) {
		NetworkDataSource ds = dataSourcesByRoleId.remove(roleId);
		if (ds != null) {
			dataSourcesByUserId.remove(ds.getUserId());
		}
	}
}
