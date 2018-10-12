package com.lingyu.admin.dao;

import java.util.Date;
import java.util.List;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.lingyu.common.entity.User;
import com.lingyu.common.orm.SimpleHibernateTemplate;

@Repository
public class UserDao {
	private SimpleHibernateTemplate<User, Integer> template;

	@Autowired
	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new SimpleHibernateTemplate<User, Integer>(sessionFactory, User.class);
	}

	/**
	 * 根据用户名字查询用户实体信息
	 * 
	 * @param name
	 * @return
	 */
	public User getUser(String name) {
		// template.findUnique("from User where name = ?0",name);
		User ret = template.findUniqueByProperty("name", name);
		if(ret != null){
			ret.deserialize();
		}
		return ret;
	}

	/**
	 * 根据id查询用户信息
	 * 
	 * @param id
	 * @return
	 */
	public User getUser(int id) {
		User ret = template.get(id);
		if(ret != null){
			ret.deserialize();
		}
		return ret;
	}

	/**
	 * 更新用户信息
	 * 
	 * @param user
	 * @return
	 */
	public void updateUser(User user) {
		Date now=new Date();
		user.setModifyTime(now);
		user.serialize();
		template.update(user);
	}

	/**
	 * 添加用户
	 * 
	 * @param user
	 * @return
	 */
	public void saveUser(User user) {
		user.serialize();
		template.save(user);
	}

	/**
	 * 查看所有用户信息
	 * 
	 * @return
	 */
	public List<User> getUserList() {
		List<User> list = template.findAll();
		for (User user : list) {
			user.deserialize();
		}
		return list;
	}

	/**
	 * 删除用户
	 * 
	 * @param user
	 */
	public void delete(int id) {
		 template.delete(id);
	}

}
