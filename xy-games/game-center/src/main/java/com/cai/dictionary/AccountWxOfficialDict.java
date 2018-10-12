/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.dictionary;

import java.util.List;
import java.util.Map;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.cai.common.domain.AccountWxOfficalModel;
import com.cai.common.util.SpringService;
import com.cai.service.MongoDBService;
import com.google.common.collect.Maps;

/**
 * 城市处理
 * 
 * @author tang
 */
public class AccountWxOfficialDict {

	/**
	 * 单例
	 */
	private final static AccountWxOfficialDict instance = new AccountWxOfficialDict();

	/**
	 * 私有构造
	 */
	private AccountWxOfficialDict() {

	}

	private Map<Long, AccountWxOfficalModel> accountWxOfficalMap = Maps.newConcurrentMap();
	
	private Map<String,Long> openIdToAccountMap = Maps.newConcurrentMap();

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static AccountWxOfficialDict getInstance() {
		return instance;
	}

	/**
	 * 
	 */
	public void load() {
		List<AccountWxOfficalModel> list = SpringService.getBean(MongoDBService.class).getMongoTemplate().findAll(AccountWxOfficalModel.class);
		for (AccountWxOfficalModel model : list) {
			accountWxOfficalMap.put(model.getAccount_id(), model);
			openIdToAccountMap.put(model.getOpenId(), model.getAccount_id());
		}
	}

	public AccountWxOfficalModel put(AccountWxOfficalModel model) {
		openIdToAccountMap.put(model.getOpenId(), model.getAccount_id());
		return accountWxOfficalMap.putIfAbsent(model.getAccount_id(), model);
	}

	public boolean isExistAccount(long accountId) {
		return accountWxOfficalMap.containsKey(accountId);
	}
	
	public boolean isExistOpenId(String openId) {
		return openIdToAccountMap.containsKey(openId);
	}
	
	public long getAccountIdByOpenId(String openId) {
		return openIdToAccountMap.get(openId);
	}
	public boolean unBindOpenId(String openId,long accountId,String mobile){
		AccountWxOfficalModel model = accountWxOfficalMap.get(accountId);
		if(model!=null&&model.getMobile().equals(mobile)){
			openIdToAccountMap.remove(openId);
			accountWxOfficalMap.remove(accountId);
			Query query = Query.query(Criteria.where("account_id").is(accountId));
			SpringService.getBean(MongoDBService.class).getMongoTemplate().remove(query, AccountWxOfficalModel.class);
			return true;
		}else{
			return false;
		}
		
	}
	
	public AccountWxOfficalModel getWxOfficalModel(long accountId) {
		return accountWxOfficalMap.get(accountId);
	}
}
