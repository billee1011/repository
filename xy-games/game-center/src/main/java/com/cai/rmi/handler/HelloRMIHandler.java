package com.cai.rmi.handler;

import com.cai.common.constant.RMICmd;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;

/**
 * 
 * 
 *
 * @author wu_ch date: 2017年11月20日 下午5:48:24 <br/>
 */
@IRmi(cmd = RMICmd.HELLO, desc = "------test")
public final class HelloRMIHandler extends IRMIHandler<Void, String> {

	@Override
	public String execute(Void message) {
		return "hello,i'm center server";
	}

}
