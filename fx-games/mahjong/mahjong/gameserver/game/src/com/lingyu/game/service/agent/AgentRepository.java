package com.lingyu.game.service.agent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.lingyu.common.entity.Agent;
import com.lingyu.noark.data.repository.UniqueCacheRepository;

@Repository
public class AgentRepository extends UniqueCacheRepository<Agent, Long> {

	private Map<Long, Agent> map = new ConcurrentHashMap<>();

	public List<Agent> loadAll() {
		List<Agent> list = super.cacheLoadAll();
		for (Agent e : list) {
			map.put(e.getRoleId(), e);
		}
		return list;
	}

	@Override
	public void cacheInsert(Agent agent) {
		super.cacheInsert(agent);
		map.put(agent.getRoleId(), agent);
	}

	@Override
	public void cacheDelete(Agent agent) {
		// super.cacheDelete(agent);
		delete(agent);
		map.remove(agent.getRoleId());
	}

	public Agent getAgent(Long roleId) {
		return map.get(roleId);
	}

}