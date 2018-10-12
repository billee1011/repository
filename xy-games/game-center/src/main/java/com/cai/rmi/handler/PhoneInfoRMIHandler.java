package com.cai.rmi.handler;

import com.cai.common.constant.RMICmd;
import com.cai.common.domain.AccountMobileModel;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.service.PhoneService;

/**
 * 
 * 
 *
 * @author wu_ch date: 2017年11月20日 下午5:48:24 <br/>
 */
@IRmi(cmd = RMICmd.PHONE_INFO, desc = "验证信息")
public final class PhoneInfoRMIHandler extends IRMIHandler<Long, AccountMobileModel> {

	@Override
	public AccountMobileModel execute(Long accountId) {
		return PhoneService.getInstance().getPhoneModel(accountId).get();
	}
}
