//package com.cai.service.activity;
//
//import java.util.List;
//import java.util.SortedMap;
//
//import org.apache.commons.lang.math.NumberUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.cai.common.define.EGoldOperateType;
//import com.cai.common.define.EMoneyOperateType;
//import com.cai.common.domain.Account;
//import com.cai.common.domain.AccountModel;
//import com.cai.common.domain.Event;
//import com.cai.common.domain.SysParamModel;
//import com.cai.common.domain.TurntableSystemModel;
//import com.cai.common.rmi.ICenterRMIServer;
//import com.cai.common.util.SpringService;
//import com.cai.core.MonitorEvent;
//import com.cai.dictionary.SysParamDict;
//import com.cai.dictionary.TurntableDict;
//import com.google.common.collect.Lists;
//import com.xianyi.framework.core.transport.netty.session.C2SSession;
//import com.xianyi.framework.server.AbstractService;
//
//public class TurntableService extends AbstractService {
//
//	private static final Logger LOGGER = LoggerFactory.getLogger(TurntableService.class);
//
//	private static TurntableService INSTANCE;
//	private List<Long> freeRewardedAccount;
//	private SysParamModel sysParamModel;
//
//	public static int TYPE_FREE_ROUND = 1; // 免费抽
//	public static int TYPE_ROUND_1 = 2; // 抽一次
//	public static int TYPE_ROUND_10 = 3; // 抽十次
//	public static int TYPE_ONCE_AGAIN = 4; // 再来一次
//
//	private TurntableService() {
//		freeRewardedAccount = Lists.newCopyOnWriteArrayList();
//		sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(1198);
//	}
//
//	public synchronized static TurntableService getInstance() {
//		if (INSTANCE == null) {
//			INSTANCE = new TurntableService();
//		}
//		return INSTANCE;
//	}
//
//	public List<TurntableSystemModel> getGoods() {
//		return TurntableDict.getInstance().getGoods();
//	}
//
//	public void clearFreeRewardedAccount() {
//		freeRewardedAccount.clear();
//	}
//
//	public boolean hasFree(long accountId) {
//		return freeRewardedAccount.contains(accountId);
//	}
//
//	public List<TurntableSystemModel> startRound(long accountId, int roundType) {
//		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
//		Account account = centerRMIServer.getAccount(accountId);
//		AccountModel accountModel = account.getAccountModel();
//
//		List<TurntableSystemModel> result = Lists.newArrayList();
//		TurntableSystemModel goods = null;
//		if (roundType == TYPE_FREE_ROUND) {
//			if (freeRewardedAccount.contains(accountId)) { // 已经抽取了免费
//				return null;
//			}
//			freeRewardedAccount.add(accountId);
//
//			goods = centerRMIServer.startTurntableRound();
//			reward(goods, accountId);
//
//			result.add(goods);
//		} else if (roundType == TYPE_ROUND_1) {
//			if (accountModel.getMoney() < sysParamModel.getVal1()) // 金币不足
//				return null;
//
//			centerRMIServer.addAccountMoney(accountId, 0 - sysParamModel.getVal1(), false, accountId + "转盘活动消耗" + sysParamModel.getVal1(),
//					EMoneyOperateType.TURNTABLE_ACTIVITY_COST);
//
//			goods = centerRMIServer.startTurntableRound();
//			reward(goods, accountId);
//
//			result.add(goods);
//		} else if (roundType == TYPE_ROUND_10) {
//			if (accountModel.getMoney() < sysParamModel.getVal2()) // 金币不足
//				return null;
//
//			centerRMIServer.addAccountMoney(accountId, 0 - sysParamModel.getVal2(), false, accountId + "转盘活动消耗" + sysParamModel.getVal1(),
//					EMoneyOperateType.TURNTABLE_ACTIVITY_COST);
//
//			for (int i = 0; i < 10; i++) {
//				goods = centerRMIServer.startTurntableRound();
//				reward(goods, accountId);
//
//				result.add(goods);
//			}
//		} else if (roundType == TYPE_ONCE_AGAIN) {
//			goods = centerRMIServer.startTurntableRound();
//			reward(goods, accountId);
//
//			result.add(goods);
//		}
//
//		return result;
//	}
//
//	private void reward(TurntableSystemModel goods, long accountId) {
//		if (goods == null) {
//			LOGGER.info("################ 转盘活动系统配置错误，无法抽取物品 ####################");
//			return;
//		}
//
//		StringBuffer buffer = new StringBuffer();
//		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
//		int goodsId = goods.getGoodsId();
//		switch (goodsId) {
//		case 1:
//		case 2:
//			break;
//		case 3:
//			int money = NumberUtils.toInt(goods.getRewardDetail());
//			buffer.append(accountId).append("转盘活动，抽到奖品ID：").append(goods.getGoodsId()).append(goods.getGoodsDesc());
//			centerRMIServer.addAccountMoney(accountId, money, false, buffer.toString(), EMoneyOperateType.TURNTABLE_ACTIVITY_REWARD);
//			break;
//		case 4:
//			int goldNum = NumberUtils.toInt(goods.getRewardDetail());
//			buffer.append(accountId).append("转盘活动，抽到奖品ID：").append(goods.getGoodsId()).append(goods.getGoodsDesc());
//			centerRMIServer.addAccountGold(accountId, goldNum, false, buffer.toString(), EGoldOperateType.ACTIVITY_TURNTABLE);
//			break;
//
//		default:
//			//////////////////////// 其他还未配置的物品
//			break;
//		}
//
//		if (goods.getRewradedShow() == 1) {
//			centerRMIServer.addTurntableRewardLog(accountId, goodsId, goods.getGoodsDesc());
//		}
//	}
//
//	@Override
//	protected void startService() {
//
//	}
//
//	@Override
//	public MonitorEvent montior() {
//		return null;
//	}
//
//	@Override
//	public void onEvent(Event<SortedMap<String, String>> event) {
//
//	}
//
//	@Override
//	public void sessionCreate(C2SSession session) {
//
//	}
//
//	@Override
//	public void sessionFree(C2SSession session) {
//		// TODO Auto-generated method stub
//
//	}
//
//	@Override
//	public void dbUpdate(int _userID) {
//
//	}
//}
