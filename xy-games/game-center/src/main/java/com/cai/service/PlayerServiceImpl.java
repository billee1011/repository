package com.cai.service;

import java.util.Map;
import java.util.SortedMap;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.cai.common.define.EGameType;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.domain.AccountGamesModel;
import com.cai.common.domain.AddCardLog;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.Event;
import com.cai.common.domain.ShopModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.SpringService;
import com.cai.core.MonitorEvent;
import com.cai.dictionary.ShopDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.domain.Session;
import com.google.common.collect.Maps;

public class PlayerServiceImpl extends AbstractService {

	private static PlayerServiceImpl instance = null;

	private PlayerServiceImpl() {
	}

	public static PlayerServiceImpl getInstance() {
		if (null == instance) {
			instance = new PlayerServiceImpl();
		}
		return instance;
	}

	private Map<Long, AccountGamesModel> accountGamesMap = Maps.newConcurrentMap();

	public Map<Long, AccountGamesModel> getAccountGamesMap() {
		return accountGamesMap;
	}

	public AccountGamesModel getAccountGamesModelByAccountId(long accountId) {
		return accountGamesMap.get(accountId);
	}

	public void addOrUpdateAccountGames(long accountId, AccountGamesModel model) {
		accountGamesMap.put(accountId, model);
	}

	@Override
	protected void startService() {

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
		// TODO Auto-generated method stub

	}

	@Override
	public void sessionFree(Session session) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dbUpdate(int _userID) {
		// TODO Auto-generated method stub

	}

	/**
	 * 金币场充值(充值豆直接兑换为金币)
	 * 
	 */
	public void rechargeForCoin(int rechargeType, String gameOrderId) {
		if (rechargeType == 1) {
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
			Query query = new Query();
			query.addCriteria(Criteria.where("orderID").is(gameOrderId));
			AddCardLog addCardLog = mongoDBService.getMongoTemplate().findOne(query, AddCardLog.class);
			if (addCardLog != null && addCardLog.getOrderStatus() == 0) {
				ShopModel shop = ShopDict.getInstance().getShopModel(addCardLog.getShopId());
				AddGoldResultModel t = centerRMIServer.addAccountGold(addCardLog.getAccountId(), -(shop.getGold() + shop.getSend_gold()), true,
						"金币场充值兑换金币扣除", EGoldOperateType.COIN_PAY_EXCHANGE);
				if (t == null || !t.isSuccess()) {
					logger.error("金币场充值,兑换金币时扣豆失败!!!");
					return;
				}
				SysParamModel sysParamModel = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(EGameType.DT.getId()).get(2301);
				if (sysParamModel == null) {
					return;
				}
				int addNum = (shop.getGold() + shop.getSend_gold()) * sysParamModel.getVal1();
				centerRMIServer.addAccountMoney(addCardLog.getAccountId(), addNum, false, "充值闲逸豆兑换金币", EMoneyOperateType.RECHARGE_EXCHANGE_COIN);
			}
		}
	}
}
