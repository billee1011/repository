package com.lingyu.admin.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.lingyu.admin.core.Constant;
import com.lingyu.admin.core.ErrorCode;
import com.lingyu.admin.dao.UserDao;
import com.lingyu.admin.dao.UserPlatformDao;
import com.lingyu.admin.util.MD5Encrypt;
import com.lingyu.admin.util.SessionUtil;
import com.lingyu.common.entity.GameArea;
import com.lingyu.common.entity.Platform;
import com.lingyu.common.entity.Role;
import com.lingyu.common.entity.User;
import com.lingyu.common.entity.UserPlatform;

@Service
@Transactional(propagation = Propagation.REQUIRED)
public class UserManager {
	private static final Logger logger = LogManager.getLogger(UserManager.class);
	@Autowired
	private UserDao userDao;
	@Autowired
	private GameAreaManager gameAreaManager;
	@Autowired
	private PlatformManager platformManager;
	@Autowired
	private UserPlatformDao userPlatformDao;
	@Autowired
	private RoleManager roleManager;
	@Autowired
	private UserPlatformManager userPlatformManager;

	private Map<Integer, User> userCache;

	public void init() {
		logger.info("账号缓存化开始");
		userCache = new HashMap<>();
		List<User> userList = userDao.getUserList();
		if (CollectionUtils.isNotEmpty(userList)) {
			for (User user : userList) {
				userCache.put(user.getId(), user);
			}
		}
		logger.info("账号缓存化开始");
	}

	/**
	 * 根据用户名获得user
	 * 
	 * @param name
	 * @return
	 */
	public User getUserByName(String name) {
		User ret = null;
		for (User user : userCache.values()) {
			if (user.getName().equals(name)) {
				ret = user;
				break;
			}
		}
		return ret;
	}

	/**
	 * 根据用户名获得user
	 * 
	 * @param name
	 * @return
	 */
	public User getUser(int id) {
		return userCache.get(id);
	}

	/**
	 * 用户登录
	 * 
	 * @param user
	 * @return
	 */
	public void login(User user) {
		SessionUtil.setSessionValue(Constant.USER_KEY, user);

	}

	/**
	 * 添加用户
	 * 
	 * @param msg
	 * @return
	 */
	public User create(String name, String nickName, String password, String email, int roleId, String pid) {
		User user = new User();
		user.setName(name);
		user.setNickName(nickName);
		user.setRoleId(roleId);
		List<String> platformIdList = new ArrayList<>();
		platformIdList.add(pid);
		user.setPlatformIdList(platformIdList);
		user.setPassword(MD5Encrypt.encrypt(password));
		user.setEmail(email);
		user.setLastLoginIp("");
		Date now = new Date();
		user.setAddTime(now);
		user.setModifyTime(now);
		
		Platform platform = platformManager.getPlatform(pid);

		// 默认选择第一个区
		if (platform != null) {
			user.setLastPid(platform.getId());
			GameArea gameArea = gameAreaManager.getFirstGameArea(platform.getId());
			user.setLastAreaId(gameArea.getAreaId());
		}

		// 默认权限
		Role role = roleManager.getRole(roleId);
		user.setPrivilegeList(role.getPrivilegeList());
		userDao.saveUser(user);
		userCache.put(user.getId(), user);

		UserPlatform up = new UserPlatform();
		up.setPlatformId(pid);
		up.setUserId(user.getId());
		userPlatformDao.createUserplatform(up);

		logger.info("auto createUser: name={}, nickName={},password,email={},roleId={},pid={}", name, nickName, password, email, roleId, pid);
		return user;
	}

	/**
	 * 添加用户
	 * 
	 * @param msg
	 * @return
	 */
	public String create(String name, String nickName, String password, String email, int roleId, List<String> platformIdList) {
		String ret = ErrorCode.EC_OK;
		User user = userDao.getUser(name);
		if (user != null) {
			return ErrorCode.USER_NAME_EXIST;
		}
		user = new User();
		user.setName(name);
		user.setNickName(nickName);
		user.setRoleId(roleId);
		user.setPlatformIdList(platformIdList);
		user.setPassword(MD5Encrypt.encrypt(password));
		user.setEmail(email);
		user.setLastLoginIp("");
		Date now = new Date();
		user.setAddTime(now);
		user.setModifyTime(now);

		User adminUser = SessionUtil.getCurrentUser();

		List<Platform> list = platformManager.getPlatformListByIds(platformIdList);
		boolean pidAreaSeted = false;
		for (Platform platform : list) {
			if (platform.getId().equals(adminUser.getLastPid())) {
				pidAreaSeted = true;
				user.setLastPid(adminUser.getLastPid());
				user.setLastAreaId(adminUser.getLastAreaId());
				break;
			}
		}
		if (!pidAreaSeted) {
			// 默认选择第一个区
			Platform platform = list.get(0);
			if (platform != null) {
				user.setLastPid(platform.getId());

				GameArea gameArea = gameAreaManager.getFirstGameArea(platform.getId());
				user.setLastAreaId(gameArea.getWorldId());
			}
		}
		// 默认权限
		Role role = roleManager.getRole(roleId);
		user.setPrivilegeList(role.getPrivilegeList());
		userDao.saveUser(user);
		userCache.put(user.getId(), user);

		for (String e : platformIdList) {
			UserPlatform up = new UserPlatform();
			up.setPlatformId(e);
			up.setUserId(user.getId());
			userPlatformDao.createUserplatform(up);
		}
		User admin = SessionUtil.getCurrentUser();
		logger.info("createUser: admin={}, user={}, platformIds={}, role={}", admin.getName(), user.getName(), Arrays.toString(platformIdList.toArray()),
				role.toString());
		return ret;
	}

	/**
	 * 修改管理员信息
	 * 
	 * @param msg
	 * @return
	 */
	public String update(int id, String nickName, String email, List<String> platformIdList) {
		String ret = ErrorCode.EC_OK;
		User user = getUser(id);
		if (user == null) {
			return ErrorCode.NO_USER;
		}
		user.setEmail(email);
		user.setNickName(nickName);
		userDao.updateUser(user);
		User admin = SessionUtil.getCurrentUser();
		userPlatformDao.deleteByUserId(id);
		for (String e : platformIdList) {
			UserPlatform up = new UserPlatform();
			up.setPlatformId(e);
			up.setUserId(user.getId());
			userPlatformDao.createUserplatform(up);
		}
		logger.info("updateUser: admin={}, user={}, nickName={}, email={}", admin.getName(), user.getName(), nickName, email);
		return ret;
	}

	public String updatePassword(int id, String pwd) {
		String ret = ErrorCode.EC_OK;
		User user = getUser(id);
		if (user == null) {
			return ErrorCode.NO_USER;
		}
		if (StringUtils.isEmpty(pwd)) {

			return ErrorCode.PASSWORD_LENGTH_ERROR;

		}
		user.setPassword(MD5Encrypt.encrypt(pwd));
		userDao.updateUser(user);
		User admin = SessionUtil.getCurrentUser();
		logger.info("updatePassword: admin={}, user={}", admin.getName(), user.getName());
		return ret;

	}

	public String updatePrivilege(int id, List<Integer> privilegeList) {
		String ret = ErrorCode.EC_OK;
		User user = getUser(id);
		if (user == null) {
			return ErrorCode.NO_USER;
		}
		user.setPrivilegeList(privilegeList);
		userDao.updateUser(user);
		if (SessionUtil.getCurrentUser().getId() == id) {
			SessionUtil.setSessionValue(Constant.USER_KEY, user);
		}
		User admin = SessionUtil.getCurrentUser();
		logger.info("updateUserPrivilege: admin={}, user={}, privileges={}", admin.getName(), user.getName(), Arrays.toString(privilegeList.toArray()));
		return ret;

	}

	public void updateAreaId(User user, int areaId) {
		user.setLastAreaId(areaId);
		userDao.updateUser(user);
	}

	public void updatePlatformId(User user, String platformId) {
		user.setLastPid(platformId);
		userDao.updateUser(user);
	}

	/**
	 * 查看所有的管理员
	 * 
	 * @return
	 */
	public List<User> getUserList() {
		List<User> list = new ArrayList<>(userCache.values());
		Collections.sort(list, new Comparator<User>() {
			@Override
			public int compare(User o1, User o2) {
				return o1.getId() - o2.getId();
			}
		});
		return list;
	}

	/**
	 * 获取管理员缓存
	 * 
	 * @return
	 */
	public Map<Integer, User> getUserCache() {
		return userCache;
	}

	/**
	 * 删除用户
	 * 
	 * @param msg
	 * @return
	 */
	public void removeUser(int userId) {
		userDao.delete(userId);
		userCache.remove(userId);
		User admin = SessionUtil.getCurrentUser();
		logger.info("removeUser: admin={}, userId={}, privileges={}", admin.getName(), userId);
	}

	public void save(User user) {
		userDao.updateUser(user);
	}
}
