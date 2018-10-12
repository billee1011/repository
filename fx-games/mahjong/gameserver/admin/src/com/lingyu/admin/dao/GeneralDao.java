package com.lingyu.admin.dao;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.lingyu.admin.core.ErrorCode;
import com.lingyu.common.orm.SimpleHibernateTemplate;

public class GeneralDao<T, PK extends Serializable> {

	private static final Logger logger = LogManager.getLogger(GeneralDao.class);
	protected SimpleHibernateTemplate<T, PK> template;
	
	private Class<T> entityClass;  
	  
    /** 
     * copy from http://blog.csdn.net/ykdsg/article/details/5472591
     * 这个通常也是hibernate的取得子类class的方法 
     *  
     * @author "yangk" 
     * @date 2010-4-11 下午01:51:28 
     */  
    public GeneralDao() {  
        Type genType = getClass().getGenericSuperclass();  
        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();  
        entityClass = (Class) params[0];  
    } 

	@Autowired
	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new SimpleHibernateTemplate<T, PK>(sessionFactory, entityClass);
	}
	
	/**
	 * 添加实体
	 * 
	 * @param entity
	 * @return
	 */
	public String add(T entity) {
		try {
			template.save(entity);
			return ErrorCode.EC_OK;
		} catch (Exception e) {
			logger.error(e.toString());
			return ErrorCode.EC_FAILED;
		}
	}

	/**
	 * 更改实体
	 * 
	 * @param entity
	 * @return
	 */
	public String update(T entity) {
		try {
			template.update(entity);
			return ErrorCode.EC_OK;
		} catch (Exception e) {
			logger.error(e.toString());
			return ErrorCode.EC_FAILED;
		}
	}
	
	/**
	 * 查询所有实体
	 * 
	 * @return
	 */
	public List<T> queryAll() {
		List<T> ret  =template.findAll();
		return ret;
	}
	
	/**
	 * @param T
	 * @return
	 */
	public String delete(T entity) {
		try {
			template.delete(entity);
			return ErrorCode.EC_OK;
		} catch (Exception e) {
			logger.error(e.toString());
			return ErrorCode.EC_FAILED;
		}
	}

}
