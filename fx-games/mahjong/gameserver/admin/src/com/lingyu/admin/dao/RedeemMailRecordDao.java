package com.lingyu.admin.dao;

import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import com.lingyu.common.entity.RedeemMailRecord;
import com.lingyu.common.orm.Page;

@Repository
public class RedeemMailRecordDao extends GeneralDao<RedeemMailRecord, Integer> {
	private static final Logger logger = LogManager.getLogger(RedeemMailRecordDao.class);

	public List<RedeemMailRecord> getRecords(int pageNo, int rows) {
		Page<RedeemMailRecord> page = new Page<>();
		page.setPageNo(pageNo);
		page.setPageSize(rows);
		page.setOrderBy("addTime");
		page.setOrder("desc");

		List<RedeemMailRecord> ret = null;

		Page<RedeemMailRecord> pageRet = null;
		try {
			pageRet = template.findByCriteria(page);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		if (pageRet == null) {
			ret = Collections.emptyList();
		} else {
			ret = pageRet.getResult();
		}
		return ret;
	}

	public RedeemMailRecord getRedeemRecordById(int id) {
		return template.findUniqueByProperty("id", id);
	}

	public int size() {
		return template.getSize();
	}
}
