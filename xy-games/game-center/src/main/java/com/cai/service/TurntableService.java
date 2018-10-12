package com.cai.service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang.StringUtils;

import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AccountWeixinModel;
import com.cai.common.domain.Event;
import com.cai.common.domain.TurntableRewardModel;
import com.cai.common.domain.TurntableSystemModel;
import com.cai.core.MonitorEvent;
import com.cai.domain.Session;

public class TurntableService extends AbstractService {

	private List<TurntableSystemModel> rewardPool;

	private List<TurntableRewardModel> rewardLogList;

	private static TurntableService instance = null;

	private TurntableService() {
		rewardPool = new CopyOnWriteArrayList<>();

	}

	public synchronized static TurntableService getInstance() {
		if (null == instance) {
			instance = new TurntableService();
		}
		return instance;
	}

	public void initRewardPool(List<TurntableSystemModel> goodsList) {
		if (goodsList == null || goodsList.size() == 0) {
			return;
		}
		rewardPool.clear();

		int weight = 0; // 所有份额
		for (TurntableSystemModel model : goodsList) {
			weight += model.getGoodsWeight();
		}

		for (TurntableSystemModel model : goodsList) {
			// 百分比
			int num = (int) ((float) model.getGoodsWeight() / weight * 100);
			for (int i = 0; i < num; i++) {
				rewardPool.add(model);
			}
		}

		Collections.shuffle(rewardPool);
	}

	public void initRewardLogList(List<TurntableRewardModel> list) {
		if (list == null) {
			rewardLogList = new CopyOnWriteArrayList<>();
		} else {
			rewardLogList = new CopyOnWriteArrayList(list);
		}
	}

	/**
	 * 开奖
	 * 
	 * @return
	 */
	public TurntableSystemModel startRound() {
		if (rewardPool == null && rewardPool.size() == 0) {
			return null;
		}
		Collections.shuffle(rewardPool);
		return rewardPool.get(0);
	}

	public void addTurntableRewardLog(long accountId, int goodsId, String goodsDesc) {
		Account account = PublicServiceImpl.getInstance().getAccount(accountId);
		AccountModel accountModel = account.getAccountModel();
		AccountWeixinModel accountWeixinModel = account.getAccountWeixinModel();

		String name = StringUtils.isBlank(accountWeixinModel.getNickname()) ? accountModel.getAccount_name() : accountWeixinModel.getNickname();
		TurntableRewardModel rewardLog = new TurntableRewardModel();
		rewardLog.setCreateTime(new Date());
		rewardLog.setAccountId(accountId);
		rewardLog.setAccountName(name);
		rewardLog.setHeadImgUrl(accountWeixinModel.getHeadimgurl());
		rewardLog.setRewardGoodsId(goodsId);
		rewardLog.setRewardGoodsDesc(goodsDesc);

		// 只保最近20名
		if (rewardLogList.size() >= 20)
			rewardLogList.remove(19);
		rewardLogList.add(0, rewardLog);

		MongoDBServiceImpl.getInstance().log_turntable_reward(rewardLog);
	}

	public List<TurntableRewardModel> getTurntableRewardRank() {
		return rewardLogList;
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

}
