package com.cai.rmi.handler;

import org.apache.commons.lang.StringUtils;
import com.cai.common.constant.RMICmd;
import com.cai.common.domain.Account;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.service.PublicServiceImpl;

/**
 * 验证是否绑定手机号码
 * @author chansonyan
 * 2018年5月7日
 */
@IRmi(cmd = RMICmd.IS_ACCOUNT_PHONE_BIND, desc = "账号是否绑定手机号码")
public final class AccountCheckPhoneBindHandler extends IRMIHandler<Long, Boolean> {
	@Override
	public Boolean execute(Long account_id) {
		if(null == account_id || account_id.longValue() <= 0) {
			return false;
		}
		Account account = PublicServiceImpl.getInstance().getAccount(account_id);
		if(null != account) {
			if(StringUtils.isNotBlank(account.getAccountModel().getMobile_phone()) && !account.getAccountModel().getMobile_phone().startsWith("0")) {
				return true;
			}
		}
		return false;
	}
}
