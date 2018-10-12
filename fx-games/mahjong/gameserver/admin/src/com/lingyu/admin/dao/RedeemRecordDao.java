package com.lingyu.admin.dao;

import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import com.lingyu.common.entity.RedeemRecord;
import com.lingyu.common.entity.User;
import com.lingyu.common.orm.Page;

@Repository
public class RedeemRecordDao extends GeneralDao<RedeemRecord, Integer>{
	private static final Logger logger = LogManager.getLogger(RedeemRecordDao.class);
	public List<RedeemRecord> getRecords(int pageNo, int rows) {
		Page<RedeemRecord> page = new Page<>();
		page.setPageNo(pageNo);
		page.setPageSize(rows);
		page.setOrderBy("addTime");
		page.setOrder("desc");
		
		List<RedeemRecord> ret = null;
		
		Page<RedeemRecord> pageRet = null;
		try {
			pageRet = template.findByCriteria(page);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		if(pageRet == null){
			ret = Collections.emptyList();
		}else{
			ret = pageRet.getResult();
		}
		return ret;
	}
	
	public RedeemRecord getRedeemRecordById(int id){
		return template.findUniqueByProperty("id", id);
	}

	public int size(){
		return template.getSize();
	}
}
