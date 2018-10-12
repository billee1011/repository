package com.cai.rmi.handler;

import java.util.HashMap;

import com.cai.common.constant.RMICmd;
import com.cai.common.define.ESmsSignType;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.util.GlobalExecutor;
import com.cai.util.TempSmsService;

/**
 * 
 *
 * @author tang date: 2018年07月27日 <br/>
 */
@IRmi(cmd = RMICmd.SEND_MSG, desc = "发送短信")
public final class SendMsgHandler extends IRMIHandler<HashMap<String, String>, Integer> {

	@Override
	protected Integer execute(HashMap<String, String> map) {
		GlobalExecutor.asyn_execute(new Runnable() {
			@Override
			public void run() {
				String mobile = map.get("mobile");
				String content = map.get("content");
				TempSmsService.sendDefineContent(mobile, content, ESmsSignType.XYYX_SIGN);
			}
		});
		return 1;
	}

}
