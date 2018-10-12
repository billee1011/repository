package com.lingyu.admin.dao;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.lingyu.admin.core.ErrorCode;
import com.lingyu.common.entity.Role;
import com.lingyu.common.orm.SimpleHibernateTemplate;

@Repository
public class RoleDao {
	private static final Logger logger = LogManager.getLogger(RoleDao.class);
	private SimpleHibernateTemplate<Role, Integer> template;

	@Autowired
	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new SimpleHibernateTemplate<Role, Integer>(sessionFactory, Role.class);
	}

	/**
	 * 根据id查询权限角色信息
	 * 
	 * @param id
	 * @return
	 */
	public Role getRole(int id) {
		Role role= template.get(id);
		role.deserialize();
		return role;
	}

	/**
	 * 根据角色名字查询角色实体信息
	 * 
	 * @param name
	 * @return
	 */
	public Role getRole(String name) {
		Role role= template.findUniqueByProperty("name", name);
		if(role != null){
			role.deserialize();
		}
		return role;
	}

	/**
	 * 添加权限角色信息
	 * 
	 * @param role
	 * @return
	 */
	public String save(Role role) {
		try {
			role.serialize();
			template.save(role);
			return ErrorCode.EC_OK;
		} catch (Exception e) {
			logger.error(e.toString());
			return ErrorCode.EC_FAILED;
		}
	}

	/**
	 * 更新权限角色信息
	 * 
	 * @param role
	 * @return
	 */
	public String updateRole(Role role) {
		try {
			role.serialize();
			template.update(role);
			return ErrorCode.EC_OK;
		} catch (Exception e) {
			logger.error(e.toString());
			return ErrorCode.EC_FAILED;
		}
	}

	/**
	 * 查询所有的权限角色
	 * 
	 * @return
	 */
	public List<Role> getAllRole() {
		List<Role> ret = template.findAll();
		for(Role role:ret){
			role.deserialize();
		}
		return ret;
	}

	/**
	 * 删除角色信息
	 * 
	 * @param role
	 * @return
	 */
	public String deleteRole(Role role) {
		try {
			template.delete(role);
			return ErrorCode.EC_OK;
		} catch (Exception e) {
			logger.error(e.toString());
			return ErrorCode.EC_FAILED;
		}
	}

	/**
	 * 删除用户
	 * 
	 * @param user
	 */
	public void delete(int roleId) {
		template.delete(roleId);
	}
}
