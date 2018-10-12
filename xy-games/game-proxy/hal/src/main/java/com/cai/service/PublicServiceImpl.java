package com.cai.service;

import java.util.Map;
import java.util.SortedMap;

import com.cai.common.define.EAccountParamType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountParamModel;
import com.cai.common.domain.Event;
import com.cai.core.MonitorEvent;
import com.cai.dictionary.*;
import com.cai.domain.Session;
import com.google.common.collect.Maps;
import com.xianyi.framework.server.AbstractService;

import protobuf.clazz.Protocol.Response;

public class PublicServiceImpl extends AbstractService {

	private static PublicServiceImpl instance = null;
	
	/**
	 * 最后公告缓存
	 */
	private Map<Integer,Response> lastNoticeCache = Maps.newConcurrentMap();

	private PublicServiceImpl() {
	}

	public static PublicServiceImpl getInstance() {
		if (null == instance) {
			instance = new PublicServiceImpl();
		}
		return instance;
	}

	@Override
	protected void startService() {
		// TODO Auto-generated method stub
		
		//System.out.println("start........");

		loadCache();
	}
	
	
	/**
	 * 远程加载缓存
	 */
	public void loadCache(){
		SysParamDict.getInstance().load();//系统参数
		SysNoticeDict.getInstance().load();//系统公告
		GameDescDict.getInstance().load();//游戏玩法说明
		ShopDict.getInstance().load();//商城字典
		MainUiNoticeDict.getInstance().load();//主界面公告
		LoginNoticeDict.getInstance().load();//登录公告
		MoneyShopDict.getInstance().load();//金币商城字典
		ActivityDict.getInstance().load();//活动
		ContinueLoginDict.getInstance().load();//连续登录
		GoodsDict.getInstance().load();
		IPGroupDict.getInstance().load();
	}

	@Override
	public MonitorEvent montior() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sessionCreate(Session session) {
		
	}

	@Override
	public void sessionFree(Session session) {

	}

	@Override
	public void dbUpdate(int _userID) {
		// TODO Auto-generated method stub

	}

	public Map<Integer, Response> getLastNoticeCache() {
		return lastNoticeCache;
	}

	public void setLastNoticeCache(Map<Integer, Response> lastNoticeCache) {
		this.lastNoticeCache = lastNoticeCache;
	}

	
	/**
	 * 获取玩家参数值
	 * @param account
	 * @param eAccountParamType
	 * @return
	 */
	public AccountParamModel getAccountParamModel(Account account,EAccountParamType eAccountParamType){
		AccountParamModel accountParamModel = account.getAccountParamModelMap().get(eAccountParamType.getId());
		if(accountParamModel==null){
			accountParamModel = new AccountParamModel();
			accountParamModel.setAccount_id(account.getAccount_id());
			accountParamModel.setType(eAccountParamType.getId());
			account.getAccountParamModelMap().put(eAccountParamType.getId(), accountParamModel);
		}
		return accountParamModel;
	}

	
	

}
