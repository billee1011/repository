package com.lingyu.admin.dao;
import java.util.List;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.lingyu.common.entity.UserPlatform;
import com.lingyu.common.orm.SimpleHibernateTemplate;
@Repository
public class UserPlatformDao
{
	private SimpleHibernateTemplate<UserPlatform, Integer> template;
	@Autowired
	public void setSessionFactory(SessionFactory sessionFactory)
	{
		template = new SimpleHibernateTemplate<UserPlatform, Integer>(
				sessionFactory, UserPlatform.class);
	}
	/**
	 * 根据id查询用户信息
	 * @param id
	 * @return
	 */
	public List<UserPlatform> getUserplatformListByplatformId(int platformId)
	{
		return template.findByProperty("platformId", platformId);
	}
	public List<UserPlatform> getUserplatformListByUserId(int userId)
	{
		return template.findByProperty("userId", userId);
		
	}
	/**
	 * 添加用户
	 * @param user
	 * @return
	 */
	public void createUserplatform(UserPlatform platform)
	{
		template.save(platform);
	}
	/**
	 * 删除用户
	 * @param user
	 * @return
	 */
	public void deleteByUserId(int userId)
	{
		template
				.bulkUpdate("delete from UserPlatform using where userId=?0", userId);
	}
	public void deleteByplatformId(int platformId)
	{
		template
				.bulkUpdate("delete from UserPlatform using where platformId=?0", platformId);
	}
	public void delete(UserPlatform userPlatform)
	{
		template.delete(userPlatform);
	}
}
