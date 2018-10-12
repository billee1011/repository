package com.cai.service;

import java.util.List;

import com.cai.common.domain.AccountModel;


public interface IPublicService {
	
	
	public Object queryForObject(String statementName, Object id);
	public Object queryForObject(String statementName);
	public List queryForList(String statementName);
	public List queryForList(String statementName, Object parameterObject);
	public void updateObject(String statementName);
	public int updateObject(String statementName, Object parameterObject);
	public void batchUpdate( final String statementName, final List list);
	public void batchInsert( final String statementName, final List list);
	public Object insertObject(String statementName, Object parameterObject);
	public int deleteObject(String statementName, Object parameterObject);
	public int deleteObject(String statementName);
	public void batchDelete( final String statementName, final List list);
	

	public void sayHello();
	
	public void insertAccountModel(AccountModel accoutModel);
	
	public AccountModel insertAccountModel(String pt_flag,String pt_name,String ip,String client_flag,String last_client_flag);
}
