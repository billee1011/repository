package com.cai.rmi.handler;

import com.cai.common.constant.RMICmd;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountWxOfficalModel;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.dictionary.AccountWxOfficialDict;
import com.cai.service.PublicServiceImpl;

/**
 * 
 *
 * @author zhanglong date: 2018年4月20日 上午10:20:29
 */
@IRmi(cmd = RMICmd.GET_WX_OFFICAL_MODEL, desc = "获取绑定微信公众号信息")
public final class GetWxOfficalModelHandler extends IRMIHandler<Long, AccountWxOfficalModel> {

	@Override
	protected AccountWxOfficalModel execute(Long accountId) {
		Account account = PublicServiceImpl.getInstance().getAccount(accountId);
		if (account == null || account.getAccountModel() == null) {
			return null;
		}
		return AccountWxOfficialDict.getInstance().getWxOfficalModel(accountId);
	}
}
