package com.lingyu.game.service.user;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.lingyu.common.entity.Role;
import com.lingyu.common.entity.User;
import com.lingyu.game.service.role.RoleRepository;
import com.lingyu.noark.data.repository.UniqueCacheRepository;

@Repository
public class UserRepository extends UniqueCacheRepository<User, Long> {

	private Map<String, User> map = new ConcurrentHashMap<>();
	/** key=machingId val=userId*/
	private Map<String, String> machingMap = new ConcurrentHashMap<>();
	
	@Autowired
	private RoleRepository roleRepository;
	
	@Override
	public void cacheUpdate(User entity) {
		entity.setModifyTime(new Date());
		super.cacheUpdate(entity);
		map.put(entity.getPid() + ":" + entity.getUserId(), entity);
	}
	
	public User getUserByRoleId(long roleId) {
		Role role = roleRepository.cacheLoad(roleId);
		return this.getUser(role.getPid(), role.getUserId());
	}
	
	public User getUser(String pid, String userId) {
		return map.get(pid + ":" + userId);
	}
	
	/**
	 * 根据机器码 看是否能找到userid
	 * @param machingId
	 * @return
	 */
	public String getUserIdByMachingId(String machingId){
		return machingMap.get(machingId);
	}
	
	public List<User> loadAll() {
		List<User> list = super.cacheLoadAll();
		for (User e : list) {
			map.put(e.getPid() + ":" + e.getUserId(), e);
			machingMap.put(e.getMachingId(), e.getUserId());
		}
		return list;
	}
	
	@Override
	public void cacheInsert(User user) {
		super.cacheInsert(user);
		map.put(user.getPid() + ":" + user.getUserId(), user);
		machingMap.put(user.getMachingId(), user.getUserId());
	}
	
	@Override
	public void cacheDelete(User user) {
		super.cacheDelete(user);
		map.remove(user.getPid() + ":" + user.getUserId());
		machingMap.remove(user.getMachingId());
	}
}