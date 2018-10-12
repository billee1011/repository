package com.lingyu.admin.dao;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.lingyu.admin.core.ErrorCode;
import com.lingyu.common.entity.Platform;
import com.lingyu.common.orm.SimpleHibernateTemplate;

@Repository
public class PlatformDao {
	private static final Logger logger = LogManager.getLogger(PlatformDao.class);
	private SimpleHibernateTemplate<Platform, Integer> template;

	@Autowired
	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new SimpleHibernateTemplate<Platform, Integer>(sessionFactory, Platform.class);
	}

	/**
	 * 添加平台
	 * 
	 * @param platform
	 * @return
	 */
	public String add(Platform platform) {
		try {
			template.save(platform);
			return ErrorCode.EC_OK;
		} catch (Exception e) {
			logger.error(e.toString());
			return ErrorCode.EC_FAILED;
		}
	}

	/**
	 * 更改游戏区信息
	 * 
	 * @param platform
	 * @return
	 */
	public String update(Platform platform) {
		try {
			template.update(platform);
			return ErrorCode.EC_OK;
		} catch (Exception e) {
			logger.error(e.toString());
			return ErrorCode.EC_FAILED;
		}
	}

	/**
	 * 根据id查询游戏区
	 * 
	 * @param id
	 * @return
	 */
	public Platform getPlatform(int id) {
		Platform area = template.get(id);
		return area;
		// String hql = "from Platform where id = ?";
		// List<Platform> query = template.findT(hql, id);
		// return query.get(0);
	}
	
	/**
	 * 根据pid查询域名
	 * 
	 * @param id
	 * @return
	 */
	public String getPlatformDomain(String pid) {
		 String hql = "from Platform where id = ?0 ";
		 List<Platform> query = template.findT(hql, pid);
		 if(null==query){
			 return "";
		 }
		 return query.get(0).getDomain();
	}

	/**
	 * 查询所有游戏区信息
	 * 
	 * @return
	 */
	public List<Platform> getPlatformList() {
		List<Platform> ret = template.findAll();
		return ret;
		// List<Platform> ret = null;
		// String hql = "from Platform";
		// ret = template.findT(hql);
		// return ret;
	}

	/**
	 * 根据id拼成的字符串进行查询
	 * 
	 * @param ids
	 * @return
	 */
	public List<Platform> getPlatformListByIds(String ids) {
		String hql = "from Platform where id in(" + ids + ")";
		List<Platform> ret = template.findT(hql);
		return ret;
	}

	/**
	 * @param area
	 * @return
	 */
	public String delete(Platform area) {
		try {
			template.delete(area);
			return ErrorCode.EC_OK;
		} catch (Exception e) {
			logger.error(e.toString());
			return ErrorCode.EC_FAILED;
		}
	}

//	public String getMaxPlatformId() {
//		return template.findT("select max(id) from Platform");
//	}

}
