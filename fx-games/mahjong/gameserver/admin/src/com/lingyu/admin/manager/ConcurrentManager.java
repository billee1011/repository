package com.lingyu.admin.manager;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.lingyu.admin.AdminServerContext;
import com.lingyu.admin.dao.StatOnlineNumDao;
import com.lingyu.common.entity.MonitorLogin;
import com.lingyu.common.entity.StatOnlineNum;

@Service
@Transactional(propagation = Propagation.REQUIRED)
public class ConcurrentManager {
	private static final Logger logger = LogManager.getLogger(ConcurrentManager.class);
	@Autowired
	private StatOnlineNumDao statOnlineNumDao;

	private Map<String, Map<Integer, Integer>> onlineStore = new HashMap<>();
	private Map<Long, MonitorLogin> loginStore = new HashMap<>();
	private Date startTime = new Date();
	// 跨服随机副本的在线
	private Map<Integer, Map<Integer, Integer>> crossStore = new ConcurrentHashMap<>();

	public void init() {
		logger.info("初始化在线数据开始");
//		loginStore = redisManager.getOnlineList();
		logger.info("初始化在线数据完毕");
	}


	public static ConcurrentManager getInstance() {
		return InstanceHolder.INSTANCE;
	}

	private static class InstanceHolder {
		private static final ConcurrentManager INSTANCE = AdminServerContext.getBean(ConcurrentManager.class);
	}

	/** 跨服在线 */
	public void addCrossOnlineQueue(int subType, int worldId, int num) {
		Map<Integer, Integer> map = crossStore.get(subType);
		if (map == null) {
			map = new ConcurrentHashMap<>();
			crossStore.put(subType, map);
		}
		map.put(worldId, num);
	}
	
	public Map<Integer, Map<Integer, Integer>> getCrossStore() {
		return crossStore;
	}

	public void addLogin(MonitorLogin login) {
		loginStore.put(login.getRoleId(), login);
	}

	public void removeLogin(MonitorLogin logout) {
		loginStore.remove(logout.getRoleId());
	}

	public boolean isOnline(long roleId) {
		return loginStore.containsKey(roleId);
	}

	public void createStatOnlineNum(String pid, int worldId, int areaId, int num, Date date) {
		StatOnlineNum stat = new StatOnlineNum();
		stat.setPid(pid);
		stat.setAreaId(areaId);
		stat.setNum(num);
		stat.setWorldId(worldId);
		stat.setAddTime(date);
		this.createStatOnlineNum(stat);
	}

	public void createStatOnlineNum(StatOnlineNum stat) {
		Map<Integer, Integer> map = onlineStore.get(stat.getPid());
		if (map == null) {
			map = new ConcurrentHashMap<Integer, Integer>();
			onlineStore.put(stat.getPid(), map);
		}
		map.put(stat.getWorldId(), stat.getNum());
		statOnlineNumDao.saveOnlineNum(stat);
	}

	// {5:4354443,6:4}
	public Map<Integer, Integer> getOnlineNumList(String pid) {
		Map<Integer, Integer> ret = onlineStore.get(pid);
		if (ret == null) {
			ret = new ConcurrentHashMap<>();
		}
		return ret;

	}

	public void clear(String pid, int worldId) {
		Map<Integer, Integer> ret = onlineStore.get(pid);
		if (ret != null) {
			ret.put(worldId, 0);
		}
	}

	public void clearAll(String pid) {
		Map<Integer, Integer> ret = onlineStore.get(pid);
		if (ret != null) {
			ret.clear();
		}
	}

	public List<StatOnlineNum> getConcurrentNumList4Area(int areaId, String pid, Date baseTime, Date endTime) {
		return statOnlineNumDao.getOnlineNumList4Area(areaId, pid, baseTime, endTime);
	}

	public List<StatOnlineNum> getConcurrentNumListTimerArea(int areaId, String pid, Date baseTime, Date endTime) {
		return statOnlineNumDao.getConcurrentNumListTimerArea(areaId, pid, baseTime, endTime);
	}

	public List<StatOnlineNum> getConcurrentNumList(int areaId, String pid, Date day) {
		return statOnlineNumDao.getOnlineNumList(areaId, pid, day);
	}

	/** 获取当前在线 */
	public int getOnlineNum(int areaId) {
		return statOnlineNumDao.getConcurrentNum(areaId);
	}

	/**
	 * 获取XX时间的在线人数明细.
	 */
	public List<StatOnlineNum> getOnlineNumDetailed(int areaId, Date startDate, Date endDate) {
		return statOnlineNumDao.getConcurrentNum(areaId, startDate, DateUtils.addDays(endDate, 1));
	}
}