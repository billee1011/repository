package com.cai.service;

import java.util.Map;
import java.util.SortedMap;

import com.cai.common.define.EAccountParamType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountParamModel;
import com.cai.common.domain.Event;
import com.cai.core.MonitorEvent;
import com.cai.dictionary.ActivityDict;
import com.cai.dictionary.AppItemDict;
import com.cai.dictionary.CardCategoryDict;
import com.cai.dictionary.ChannelModelDict;
import com.cai.dictionary.CoinDict;
import com.cai.dictionary.CoinExciteDict;
import com.cai.dictionary.ContinueLoginDict;
import com.cai.dictionary.CustomerSerNoticeDict;
import com.cai.dictionary.GameDescDict;
import com.cai.dictionary.GameGroupRuleDict;
import com.cai.dictionary.GameRecommendDict;
import com.cai.dictionary.GoodsDict;
import com.cai.dictionary.HallGuideDict;
import com.cai.dictionary.IPGroupDict;
import com.cai.dictionary.InviteActiveDict;
import com.cai.dictionary.ItemDict;
import com.cai.dictionary.ItemExchangeDict;
import com.cai.dictionary.LoginNoticeDict;
import com.cai.dictionary.MainUiNoticeDict;
import com.cai.dictionary.MatchDict;
import com.cai.dictionary.MoneyShopDict;
import com.cai.dictionary.SdkAppDict;
import com.cai.dictionary.SdkDiamondShopDict;
import com.cai.dictionary.ServerDict;
import com.cai.dictionary.ShopDict;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysNoticeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.dictionary.TurntableDict;
import com.cai.dictionary.WelfareExchangeDict;
import com.cai.dictionary.WelfareGoodsTypeDict;
import com.google.common.collect.Maps;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.server.AbstractService;

import protobuf.clazz.Protocol.Response;

public class PublicServiceImpl extends AbstractService {

	private static PublicServiceImpl instance = null;

	/**
	 * 最后公告缓存
	 */
	private Map<Integer, Response> lastNoticeCache = Maps.newConcurrentMap();

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

		// System.out.println("start........");

		loadCache();
	}

	/**
	 * 远程加载缓存
	 */
	public void loadCache() {
		SysParamDict.getInstance().load();// 系统参数
		SysNoticeDict.getInstance().load();// 系统公告
		GameDescDict.getInstance().load();// 游戏玩法说明
		ShopDict.getInstance().load();// 商城字典
		MainUiNoticeDict.getInstance().load();// 主界面公告
		LoginNoticeDict.getInstance().load();// 登录公告
		MoneyShopDict.getInstance().load();// 金币商城字典
		ActivityDict.getInstance().load();// 活动
		ContinueLoginDict.getInstance().load();// 连续登录
		GoodsDict.getInstance().load();
		IPGroupDict.getInstance().load();
		AppItemDict.getInstance().load();
		ServerDict.getInstance().load();
		GameRecommendDict.getInstance().load();
		SysGameTypeDict.getInstance().load();// 游戏类型对应收费索引 游戏类型 描述
		GameGroupRuleDict.getInstance().load();// 游戏类型对应收费索引 游戏类型 描述
		SysParamServerDict.getInstance().load();// 服务端系统参数
		TurntableDict.getInstance().load();
		CustomerSerNoticeDict.getInstance().load();// 客服界面公告
		MatchDict.getInstance().load();//
		InviteActiveDict.getInstance().load();
		CardCategoryDict.getInstance().load();
		CoinExciteDict.getInstance().load();
		CoinDict.getInstance().load();
		ItemExchangeDict.getInstance().load();
		WelfareExchangeDict.getInstance().load();
		WelfareGoodsTypeDict.getInstance().load();
		ItemDict.getInstance().load();
		ChannelModelDict.getInstance().load();
		HallGuideDict.getInstance().load();
		HallGuideDict.getInstance().loadResource();
		HallGuideDict.getInstance().loadMainViewBack();
		SdkAppDict.getInstance().load();
		SdkDiamondShopDict.getInstance().load();
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
	public void sessionCreate(C2SSession session) {

	}

	@Override
	public void sessionFree(C2SSession session) {

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
	 * 
	 * @param account
	 * @param eAccountParamType
	 * @return
	 */
	public AccountParamModel getAccountParamModel(Account account, EAccountParamType eAccountParamType) {
		AccountParamModel accountParamModel = account.getAccountParamModelMap().get(eAccountParamType.getId());
		if (accountParamModel == null) {
			accountParamModel = new AccountParamModel();
			accountParamModel.setAccount_id(account.getAccount_id());
			accountParamModel.setType(eAccountParamType.getId());
			account.getAccountParamModelMap().put(eAccountParamType.getId(), accountParamModel);
		}
		return accountParamModel;
	}

}
