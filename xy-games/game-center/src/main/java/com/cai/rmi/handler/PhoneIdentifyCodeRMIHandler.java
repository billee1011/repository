package com.cai.rmi.handler;

import com.cai.common.constant.RMICmd;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.service.PhoneService;

/**
 * 
 * 
 *
 * @author wu_ch date: 2017年11月20日 下午5:48:24 <br/>
 */
@IRmi(cmd = RMICmd.PHONE_IDENTIFY_CODE, desc = "生成手机验证码")
public final class PhoneIdentifyCodeRMIHandler extends IRMIHandler<Void, Integer> {

	@Override
	public Integer execute(Void message) {
		return PhoneService.getInstance().randomIdentifyCode();
	}
}
