package com.cai.service;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cai.common.define.EPtType;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AccountWeixinModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.SpringService;
import com.cai.dao.PublicDAO;
import com.cai.dictionary.SysParamDict;

import javolution.util.FastMap;

@Service
public class PublicService implements IPublicService {

	@Autowired
	private PublicDAO publicDAO;

	@Override
	public Object queryForObject(String statementName, Object id) {
		return publicDAO.queryForObject(statementName, id);
	}

	@Override
	public Object queryForObject(String statementName) {
		return publicDAO.queryForObject(statementName);
	}

	@Override
	public List queryForList(String statementName) {
		return publicDAO.queryForList(statementName);
	}

	@Override
	public List queryForList(String statementName, Object parameterObject) {
		return publicDAO.queryForList(statementName, parameterObject);
	}

	@Override
	public void updateObject(String statementName) {
		publicDAO.updateObject(statementName);

	}

	@Override
	public int updateObject(String statementName, Object parameterObject) {
		return publicDAO.updateObject(statementName, parameterObject);
	}

	@Override
	public void batchUpdate(String statementName, List list) {
		publicDAO.batchUpdate(statementName, list);
	}

	@Override
	public void batchInsert(String statementName, List list) {
		publicDAO.batchInsert(statementName, list);
	}

	@Override
	public Object insertObject(String statementName, Object parameterObject) {
		return publicDAO.insertObject(statementName, parameterObject);
	}

	@Override
	public int deleteObject(String statementName, Object parameterObject) {
		return publicDAO.deleteObject(statementName, parameterObject);
	}

	@Override
	public int deleteObject(String statementName) {
		return publicDAO.deleteObject(statementName);
	}

	@Override
	public void batchDelete(String statementName, List list) {
		publicDAO.batchDelete(statementName, list);

	}

	@Override
	public void sayHello() {
		System.out.println("say hello");
	}

	/**
	 * 注册账号
	 */
	@Override
	public void insertAccountModel(AccountModel accoutModel) {
		publicDAO.insertAccountModel(accoutModel);
	}
	
	
	/**
	 * 创建账号
	 * @param pt_flag
	 * @param account_name
	 * @param ip
	 * @return
	 */
	@Override
	public AccountModel insertAccountModel(String pt_flag,String account_name,String ip,String last_client_flag,String client_version){
		
		FastMap<Integer, SysParamModel> sysParamMap = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1);
		
		//创建账号
		PublicService publicService = SpringService.getBean(PublicService.class);
		Date date = new Date();
		AccountModel accountModel = new AccountModel();
		accountModel.setPt(pt_flag);
		accountModel.setAccount_name(account_name);
		accountModel.setPassword("");
		accountModel.setLogin_times(0);
		accountModel.setCreate_time(date);
		accountModel.setLast_login_time(date);
//		accountModel.setMobile_phone(null);
		accountModel.setHistory_pay_gold(0L);
		accountModel.setGold(sysParamMap.get(1000).getVal1());
		accountModel.setMoney(sysParamMap.get(1000).getVal2());
		accountModel.setClient_ip(ip);
		accountModel.setLast_client_flag(last_client_flag);
		accountModel.setClient_version(client_version);
		accountModel.setRecommend_id(0);
		accountModel.setProxy_level(0);
		publicDAO.insertAccountModel(accountModel);
		
		if(pt_flag.equals(EPtType.WX.getId())){
			AccountWeixinModel accountWeixinModel = new AccountWeixinModel();
			accountWeixinModel.setAccount_id(accountModel.getAccount_id());
			publicDAO.insertAccountWeixinModel(accountWeixinModel);
		}
		
		
		
		return accountModel;
	}
	

	public PublicDAO getPublicDAO() {
		return publicDAO;
	}

}
