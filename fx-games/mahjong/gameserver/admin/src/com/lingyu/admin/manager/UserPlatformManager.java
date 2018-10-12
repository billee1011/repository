package com.lingyu.admin.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.lingyu.admin.dao.UserPlatformDao;
import com.lingyu.common.entity.UserPlatform;

@Service
@Transactional(propagation = Propagation.REQUIRED)
public class UserPlatformManager {

	@Autowired
	private UserPlatformDao userAreaDao;

	public List<String> getPlatformIdListByUserId(int userId) {
		List<String> ret = new ArrayList<>();
		List<UserPlatform> list = this.getUserPlatformList(userId);
		for (UserPlatform e : list) {
			ret.add(e.getPlatformId());
		}
		return ret;
	}

	public List<UserPlatform> getUserPlatformList(int userId) {
		return userAreaDao.getUserplatformListByUserId(userId);
	}

	public Map<String, UserPlatform> getUserPlatformStore(int userId) {
		Map<String, UserPlatform> ret = new HashMap<>();
		List<UserPlatform> list = this.getUserPlatformList(userId);
		for (UserPlatform e : list) {
			ret.put(e.getPlatformId(), e);
		}
		return ret;

	}

	public void removeUserPlatform(int userId) {
		userAreaDao.deleteByUserId(userId);

	}

}
