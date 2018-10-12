package com.cai.rmi.handler;

import org.apache.commons.lang.StringUtils;

import com.cai.common.constant.RMICmd;
import com.cai.common.domain.Account;
import com.cai.common.domain.PlayerSdkViewVo;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.vo.PlayerSdkVerifyDataVo;
import com.cai.service.PublicServiceImpl;
import com.cai.util.AccountUtil;

@IRmi(cmd = RMICmd.SDK_LOGIN_VERIFY, desc = "SDK登录校验handler")
public class SdkLoginVerifyRMIHandler extends IRMIHandler<PlayerSdkVerifyDataVo, PlayerSdkViewVo>{

	@Override
	protected PlayerSdkViewVo execute(PlayerSdkVerifyDataVo dataVo) {
		if(null == dataVo) {
			return null;
		}
		if(dataVo.getAccountId() <= 0 || StringUtils.isBlank(dataVo.getToken())) {
			return null;
		}
		//获取account和token，验证是否正确
		Account account = PublicServiceImpl.getInstance().getAccount(dataVo.getAccountId());
		if(null == account) {
			return null;
		}
		//生成的token校验失败，返回空
		if(!dataVo.getToken().equals(account.getThridToken())) {
			return null;
		}
		//返回正确的account数据
		PlayerSdkViewVo playerSdkViewVO = AccountUtil.getSdkVo(account);
		return playerSdkViewVO;
	}

}
