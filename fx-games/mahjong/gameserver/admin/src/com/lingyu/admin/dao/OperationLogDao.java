package com.lingyu.admin.dao;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import com.lingyu.common.entity.OperationLog;
import com.lingyu.common.orm.Page;
import com.lingyu.common.orm.SimpleHibernateTemplate;

@Repository
public class OperationLogDao {
	// private static final Logger logger = LogManager.getLogger(OperationLogDao.class);
	private SimpleHibernateTemplate<OperationLog, Integer> template;

	@Autowired
	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new SimpleHibernateTemplate<OperationLog, Integer>(sessionFactory, OperationLog.class);
	}

	@Cacheable(value = "operationLog", key = "'start'+#startTime.time+'end'+#endTime.time+#pageNo+#pageSize")
	public List<OperationLog> getAllLogList(Date startTime, Date endTime, int pageNo, int pageSize) {
		Page<OperationLog> page = new Page<OperationLog>();
		page.setPageNo(pageNo);
		page.setPageSize(pageSize);
		//template.findAll(page);
		String hql = " from OperationLog ";
		String hqlDate = getHqlDate(startTime, endTime);
		if (hqlDate.length() > 0) {
			hql = hql + " where " + hqlDate;
		}
		template.find(page, hql);
		List<OperationLog> ret = page.getResult();
		return ret;
	}

	@Cacheable(value = "operationLog", key = "#userName+'start'+#startTime.time+'end'+#endTime.time+#pageNo+#pageSize")
	public List<OperationLog> getLogList(String userName, Date startTime, Date endTime, int pageNo, int pageSize) {
		Page<OperationLog> page = new Page<OperationLog>();
		page.setPageNo(pageNo);
		page.setPageSize(pageSize);
		String hql = " from OperationLog where userName= '" + userName + "'";
		String hqlDate = getHqlDate(startTime, endTime);
		if (hqlDate.length() > 0) {
			hql = hql + " and " + hqlDate;
		}
		template.find(page, hql);
		List<OperationLog> ret = page.getResult();
		return ret;
	}

	public int getLogNum(String userName, Date startDate, Date endDate) {
		String hql = "select count(*) from OperationLog where userName= '" + userName + "'";
		String hqlDate = getHqlDate(startDate, endDate);
		if (hqlDate.length() > 0) {
			hql = hql + " and " + hqlDate;
		}
		return template.findInt(hql);
	}

	public int getAllLogNum(Date startDate, Date endDate) {
		String hql = "select count(*) from OperationLog ";
		String hqlDate = getHqlDate(startDate, endDate);
		if (hqlDate.length() > 0) {
			hql = hql + " where " + hqlDate;
		}
		return template.findInt(hql);
	}

	private String getHqlDate(Date startDate, Date endDate) {
		String hqlDate = "";
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		if (startDate != null) {
			hqlDate = " addTime >= '" + format.format(startDate) + "' ";
		}
		if (endDate != null) {
			if (hqlDate.length() > 0) {
				hqlDate = hqlDate + " and ";
			}
			hqlDate = hqlDate + " addTime <= '" + format.format(endDate) + "' ";
		}
		return hqlDate;
	}

	public void createLog(int userId, String userName,String lastLoginIp, String fun, String value) {
		OperationLog log = new OperationLog();
		log.setUserId(userId);
		log.setUserName(userName);
		log.setType(1);
		log.setFun(fun);
		log.setValue(value);
		log.setLastLoginIp(lastLoginIp);
		Date now = new Date();
		log.setAddTime(now);
		template.save(log);
	}
}
