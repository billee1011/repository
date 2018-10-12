package com.cai.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cai.dao.ClubDao;
import com.xianyi.framework.core.service.AbstractService;

@Service
public class ClubDaoService extends AbstractService {

	@Autowired
	private ClubDao publicDAO;
	
	@Override
	public void start() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop() throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	public Object queryForObject(String statementName, Object id) {
		return publicDAO.queryForObject(statementName, id);
	}

	public Object queryForObject(String statementName) {
		return publicDAO.queryForObject(statementName);
	}

	@SuppressWarnings("rawtypes")
	public List queryForList(String statementName) {
		return publicDAO.queryForList(statementName);
	}

	@SuppressWarnings("rawtypes")
	public List queryForList(String statementName, Object parameterObject) {
		return publicDAO.queryForList(statementName, parameterObject);
	}

	public void updateObject(String statementName) {
		publicDAO.updateObject(statementName);

	}

	public int updateObject(String statementName, Object parameterObject) {
		return publicDAO.updateObject(statementName, parameterObject);
	}

	@SuppressWarnings("rawtypes")
	public void batchUpdate(String statementName, List list) {
		publicDAO.batchUpdate(statementName, list);
	}

	@SuppressWarnings("rawtypes")
	public void batchInsert(String statementName, List list) {
		publicDAO.batchInsert(statementName, list);
	}

	public Object insertObject(String statementName, Object parameterObject) {
		return publicDAO.insertObject(statementName, parameterObject);
	}

	public int deleteObject(String statementName, Object parameterObject) {
		return publicDAO.deleteObject(statementName, parameterObject);
	}

	public int deleteObject(String statementName) {
		return publicDAO.deleteObject(statementName);
	}

	@SuppressWarnings("rawtypes")
	public void batchDelete(String statementName, List list) {
		publicDAO.batchDelete(statementName, list);

	}
	
	public ClubDao getDao(){
		return publicDAO;
	}

}
