package com.lingyu.admin.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.lingyu.admin.dao.OperationLogDao;
import com.lingyu.common.entity.OperationLog;
import com.lingyu.common.entity.User;

@Service
@Transactional(propagation = Propagation.REQUIRED)
public class OperationLogManager {

	private static final Logger logger = LogManager.getLogger(OperationLogManager.class);
	@Autowired
	private OperationLogDao operationLogDao;

	/**
	 * 获取日志数量
	 * 
	 * @param userName 可以为""||null, 为""||null时查询全部user记录
	 * @return
	 */
	public int getLogNum(String userName, Date startDate, Date endDate) {
		int ret = 0;
		if (StringUtils.isEmpty(userName)) {
			ret = operationLogDao.getAllLogNum(startDate, endDate);

		} else {
			ret = operationLogDao.getLogNum(userName, startDate, endDate);

		}
		return ret;
	}

	/**
	 * 获取日志信息
	 * 
	 * @param userName 可以为""||null, 为""||null时查询全部user记录
	 * @param pageNo
	 * @param pageSize
	 * @return
	 */
	public List<OperationLog> getLogList(String userName, Date startDate, Date endDate, int pageNo, int pageSize) {
		List<OperationLog> ret = new ArrayList<OperationLog>();
		if (StringUtils.isEmpty(userName)) {
			ret = operationLogDao.getAllLogList(startDate, endDate, pageNo, pageSize);

		} else {
			ret = operationLogDao.getLogList(userName, startDate, endDate, pageNo, pageSize);

		}
		return ret;
	}

	public void createLog(User user, String funName, String param) {
		operationLogDao.createLog(user.getId(), user.getName(), user.getLastLoginIp(), funName, param);
	}
}
