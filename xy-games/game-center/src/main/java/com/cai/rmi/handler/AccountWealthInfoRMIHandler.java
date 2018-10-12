package com.cai.rmi.handler;

import com.cai.common.constant.RMICmd;
import com.cai.common.define.EWealthCategory;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.rmi.vo.AccountWealthVo;
import com.cai.common.util.Pair;
import com.cai.service.PublicServiceImpl;

/**
 * 
 * 
 *
 * @author wu_hc date: 2017年12月25日 上午11:58:53 <br/>
 */
@IRmi(cmd = RMICmd.ACCOUNT_WEALTH_INFO, desc = "个人财富")
public final class AccountWealthInfoRMIHandler extends IRMIHandler<Pair<Long, EWealthCategory>, AccountWealthVo> {

	@Override
	public AccountWealthVo execute(final Pair<Long, EWealthCategory> key) {
		final long accountId = key.getFirst().longValue();
		final EWealthCategory category = key.getSecond();

		long value = 0L;
		if (category == EWealthCategory.GOLD) {
			Account account = PublicServiceImpl.getInstance().getAccount(accountId);
			if (null == account) {
				logger.error("查询个人[{}]财富,但没找到玩家->account is null!!", accountId);
			} else {
				AccountModel accountModel = account.getAccountModel();
				if (null == accountModel) {
					logger.error("查询个人[{}]财富,但没找到玩家->accountModel is null!!", accountId);
				} else {
					value = accountModel.getGold();
				}
			}
		}
		return AccountWealthVo.newVo(accountId, value, category);
	}

}
