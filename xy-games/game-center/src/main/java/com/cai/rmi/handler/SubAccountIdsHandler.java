package com.cai.rmi.handler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.cai.common.constant.RMICmd;
import com.cai.common.domain.Account;
import com.cai.common.domain.HallRecommendModel;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.service.PublicServiceImpl;

/**
 *
 * @author tang date: 2018年08月03日 下午3:11:24 <br/>
 */
@IRmi(cmd = RMICmd.GET_ALL_SUB, desc = "获取所有的下级id")
public final class SubAccountIdsHandler extends IRMIHandler<Long, Set<Long>> {

	@Override
	public Set<Long> execute(Long account_id) {
		Set<Long> set = new HashSet<>();
		Account account = PublicServiceImpl.getInstance().getAccount(account_id);
		if (account == null || account.getHallRecommendModel().getRecommend_level() == 0) {
			return set;
		}
		getSubId(account_id, set);
		return set;
	}

	private void getSubId(long accountId, Set<Long> set) {
		Account account = PublicServiceImpl.getInstance().getAccount(accountId);
		if (account == null || account.getHallRecommendModel().getRecommend_level() == 0) {
			return;
		}
		Map<Long, HallRecommendModel> map = account.getHallRecommendModelMap();
		for (HallRecommendModel model : map.values()) {
			if (model.getRecommend_level() > 0) {
				getSubId(model.getTarget_account_id(), set);
			}
			set.add(model.getTarget_account_id());
		}
	}
}
