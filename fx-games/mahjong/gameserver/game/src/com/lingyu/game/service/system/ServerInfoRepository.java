package com.lingyu.game.service.system;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Repository;

import com.lingyu.common.entity.ServerInfo;
import com.lingyu.noark.data.repository.OrmRepository;

@Repository
public class ServerInfoRepository extends OrmRepository<ServerInfo, Integer> {
	/**
	 * 获取数据库中的记录(直接从数据库获取记录)
	 * 
	 * @return
	 */
	public ServerInfo loadServerInfoFromDb() {
		List<ServerInfo> list = loadAllBySystem();
		return CollectionUtils.isNotEmpty(list) ? list.get(0) : null;
	}

}
