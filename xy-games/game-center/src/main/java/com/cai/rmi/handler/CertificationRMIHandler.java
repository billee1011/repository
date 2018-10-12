package com.cai.rmi.handler;

import com.cai.common.constant.RMICmd;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.rmi.vo.CertificationRMIVo;
import com.cai.service.PublicServiceImpl;

/**
 * 
 * 
 *
 * @author wu_ch date: 2017年11月20日 下午5:48:24 <br/>
 */
@IRmi(cmd = RMICmd.CERTIFICATION, desc = "验证信息")
public final class CertificationRMIHandler extends IRMIHandler<CertificationRMIVo, Void> {

	@Override
	public Void execute(CertificationRMIVo vo) {
		Account account = PublicServiceImpl.getInstance().getAccount(vo.getAccountId());
		if (null != account) {
			final AccountModel accountModel = account.getAccountModel();
			accountModel.setReal_name(vo.getRealName());
			accountModel.setIdentity_card(vo.getRealId());
			accountModel.setNeedDB(true);
		}
		return null;
	}
}
