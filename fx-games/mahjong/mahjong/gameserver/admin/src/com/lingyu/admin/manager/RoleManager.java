package com.lingyu.admin.manager;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.lingyu.admin.core.ErrorCode;
import com.lingyu.admin.dao.RoleDao;
import com.lingyu.admin.util.SessionUtil;
import com.lingyu.admin.vo.RoleVO;
import com.lingyu.common.entity.Role;
import com.lingyu.common.entity.User;

@Service
@Transactional(propagation = Propagation.REQUIRED)
public class RoleManager {
	private static final Logger logger = LogManager.getLogger(RoleManager.class);
	@Autowired
	private RoleDao roleDao;

	@Cacheable(value = "getRole", key = "'id'+#id")
	public Role getRole(int id) {
		return roleDao.getRole(id);
	}

	public Role getRole(String name) {
		return roleDao.getRole(name);
	}

	public List<Role> getRoleList() {
		return roleDao.getAllRole();
	}

	public List<RoleVO> getRoleVOList() {
		List<RoleVO> ret = new ArrayList<>();
		List<Role> list = roleDao.getAllRole();
		for (Role e : list) {
			ret.add(e.toVO());
		}
		return ret;
	}

	public String create(String name, String description, List<Integer> list) {
		String ret = ErrorCode.EC_OK;
		Role role = roleDao.getRole(name);
		if (role != null) {
			return ErrorCode.ROLE_NAME_EXIST;
		}

		role = new Role();
		role.setName(name);
		role.setDescription(description);
		role.setPrivilegeList(list);
		roleDao.save(role);
		User user = SessionUtil.getCurrentUser();
		logger.info("createRole: admin={}, role={}", user.getName(), role.toString());
		return ret;
	}

	public String update(int id, String name, String description, List<Integer> list) {
		String ret = ErrorCode.EC_OK;
		Role role = roleDao.getRole(id);
		if (role != null) {
			role.setName(name);
			role.setDescription(description);
			role.setPrivilegeList(list);
			roleDao.updateRole(role);
			User user = SessionUtil.getCurrentUser();
			logger.info("updateRole: admin={}, role={}", user.getName(), role.toString());
		} else {
			ret = ErrorCode.ROLE_NAME_NOT_EXIST;
		}
		return ret;
	}

	public String remove(Role role) {
		User user = SessionUtil.getCurrentUser();
		logger.info("removeRole: admin={}, role={}", user.getName(), role.toString());
		return roleDao.deleteRole(role);
	}

	public void removeRole(int roleId) {
		roleDao.delete(roleId);
	}

}
