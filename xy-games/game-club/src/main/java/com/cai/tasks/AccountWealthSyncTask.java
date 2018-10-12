/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.tasks;

import java.util.List;

import com.cai.common.constant.RMICmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.define.EWealthCategory;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.rmi.vo.AccountWealthVo;
import com.cai.common.util.Pair;
import com.cai.common.util.SpringService;
import com.cai.constant.Club;
import com.cai.utils.Utils;

import protobuf.clazz.ClubMsgProto.ClubCreatorGoldUpdateProto;

/**
 * 
 *
 * @author wu_hc date: 2018年1月2日 上午10:17:03 <br/>
 */
public final class AccountWealthSyncTask implements Runnable {

	private final Pair<Long, EWealthCategory> key;
	private final List<Long> reqAccountIds;

	public static AccountWealthSyncTask newTask(Pair<Long, EWealthCategory> key, List<Long> reqAccountIds) {
		return new AccountWealthSyncTask(key, reqAccountIds);
	}

	private AccountWealthSyncTask(Pair<Long, EWealthCategory> key, List<Long> reqAccountIds) {
		this.key = key;
		this.reqAccountIds = reqAccountIds;
	}

	@Override
	public void run() {

		Long value = Club.wealth.get(key);
		if (null == value /*|| value.longValue() == 0L*/) {
			ICenterRMIServer rmiServer = SpringService.getBean(ICenterRMIServer.class);
			AccountWealthVo vo = rmiServer.rmiInvoke(RMICmd.ACCOUNT_WEALTH_INFO, this.key);
			Club.wealth.put(key, vo.getNewValue());
			value = vo.getNewValue();
		}

		ClubCreatorGoldUpdateProto.Builder b = ClubCreatorGoldUpdateProto.newBuilder();
		b.setCategory(key.getSecond().category());
		b.setValue(value.longValue());
		b.setAccountId(key.getFirst().longValue());
		Utils.sendClient(reqAccountIds, S2CCmd.CLUB_OWENER_GOLD_UPDATE, b);
	}

}
