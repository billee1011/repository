package com.lingyu.admin.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.lingyu.admin.core.ErrorCode;
import com.lingyu.admin.dao.PlatformDao;
import com.lingyu.admin.util.SessionUtil;
import com.lingyu.common.entity.Platform;
import com.lingyu.common.entity.User;

@Service
@Transactional(propagation = Propagation.REQUIRED)
public class PlatformManager {
	private static final Logger logger = LogManager.getLogger(PlatformManager.class);
	@Autowired
	private PlatformDao platformDao;
	private Map<String, Platform> map = new HashMap<>();
	

	public void init() {
		logger.info("平台缓存化开始");
		List<Platform> list = platformDao.getPlatformList();
		for (Platform e : list) {
			map.put(e.getId(), e);
			logger.info("platform={}", e.toString());
		}
		logger.info("平台缓存化完毕");
	}

	/**
	 * 方法描述： 获取所有游戏服务器
	 * 
	 * @return
	 */
	public Collection<Platform> getPlatformList() {
		return map.values();
	}

	/**
	 * 添加游戏区
	 * 
	 * @param msg
	 * @return
	 */

	public String create(Platform platform) {
		Date now = new Date();
		platform.setAddTime(now);
		platform.setModifyTime(now);
		User user = SessionUtil.getCurrentUser();
		if (user == null) {
			logger.warn("createPlatformErr: platform={}, cause=adminIsNull", platform.toString());
			return ErrorCode.NO_USER;
		}
		String retCode = platformDao.add(platform);
		map.put(platform.getId(), platform);
		logger.info("createPlatform: admin={}, platform={}", user.getName(), platform.toString());
		return retCode;
	}

	/**
	 * 修改游戏区
	 * 
	 * @param msg
	 * @return
	 */
	public String updatePlatform(String id, String name) {
		User user = SessionUtil.getCurrentUser();
		if (user == null) {
			logger.warn("updatePlatformErr: platformId={}, cause=adminIsNull", id);
			return ErrorCode.NO_USER;
		}
		Platform platform = this.getPlatform(id);
		if (platform != null) {
			platform.setName(name);
			logger.info("updatePlatform: admin={}, platform={}", user.getName(), platform.toString());
		} else {
			logger.warn("updatePlatformErr:id={}, cause=platformNotExists", id);
		}
		return this.update(platform);
	}

	/**
	 * 修改游戏区
	 * 
	 * @param msg
	 * @return
	 */
	public String update(Platform platform) {
		User admin = SessionUtil.getCurrentUser();
		if (admin == null) {
			logger.warn("updatePlatformErr: platform={}, cause=adminIsNull", platform.toString());
			return ErrorCode.NO_USER;
		}
		Date now = new Date();
		platform.setModifyTime(now);
		String retCode = platformDao.update(platform);
		map.put(platform.getId(), platform);
		logger.warn("updatePlatform: admin={}, platform={}", admin.getName(), platform.toString());
		return retCode;
	}

	/**
	 * 删除游戏区
	 * 
	 * @param msg
	 * @return
	 */
	public String removePlatform(String platformId) {
		User user = SessionUtil.getCurrentUser();
		if (user == null) {
			logger.warn("removePlatformErr: platformId={}, cause=adminIsNull", platformId);
			return ErrorCode.NO_USER;
		}
		Platform platform = this.getPlatform(platformId);
		map.remove(platformId);
		logger.info("removePlatform: admin={}, platform={}", user.getName(), (platform != null ? platform.toString() : platformId));
		return platformDao.delete(platform);
	}

	// /**
	// * 查看所有的游戏区
	// *
	// * @return
	// */
	// public List<Platform> getPlatformList() {
	// return platformDao.queryAll();
	// }
	/**
	 * @param idList
	 * @return
	 */
	public List<Platform> getPlatformListByIds(List<String> idList) {
		List<Platform> ret = new ArrayList<>();
		for (String id : idList) {
			ret.add(map.get(id));
		}
		return ret;
	}

	public Platform getPlatform(String platformId) {
		return map.get(platformId);
	}

	/**
	 * 设定游戏区
	 * 
	 * @param platformId
	 */
	public String selectPlatform(String platformId) {
		logger.info("选择平台 platformId={}", platformId);
		Platform platform = this.getPlatform(platformId);
		if (platform == null) {
			return ErrorCode.PLATFORM_DATA_WRONG;
		} else {
			SessionUtil.setSessionValue(SessionUtil.PLATFORM_ID_KEY, platformId);
			return ErrorCode.EC_OK;
		}
	}

	/**
	 * 获取域名
	 * 
	 * @return
	 */
	public String loadLastPidDomain(String pid) {
		return platformDao.getPlatformDomain(pid);
	}
	// public int getNextPlatformId() {
	// return platformDao.getMaxPlatformId()+1;
	// }
}
