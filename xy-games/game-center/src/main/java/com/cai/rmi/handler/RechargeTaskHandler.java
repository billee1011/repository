package com.cai.rmi.handler;

import java.util.HashMap;

import org.apache.commons.lang.StringUtils;

import com.cai.common.constant.RMICmd;
import com.cai.common.rmi.IFoundationRMIServer;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.util.GlobalExecutor;
import com.cai.common.util.SpringService;

/**
 * 
 * 充值任务相关操作
 *
 * @author tang date: 2018年05月15日 下午16:43:45 <br/>
 */
@IRmi(cmd = RMICmd.RECHARGE_TASK, desc = "充值任务相关操作")
public final class RechargeTaskHandler extends IRMIHandler<HashMap<String, String>, Integer> {

	@Override
	protected Integer execute(HashMap<String, String> map) {
		String accountId = map.get("accountId");
		String moneyStr = map.get("money");
		String rechargeType = map.get("rechargeType");
		if (StringUtils.isBlank(accountId) || !StringUtils.isNumeric(moneyStr)) {
			return 0;
		}
		GlobalExecutor.asyn_execute(new Runnable() {
			@Override
			public void run() {
				try {
					long account_id = Long.parseLong(accountId);
					int money = Integer.parseInt(moneyStr);
					int type = 0;
					if (StringUtils.isNotBlank(rechargeType)) {
						type = Integer.parseInt(rechargeType);
					}
					IFoundationRMIServer iFoundationRMIServer = (IFoundationRMIServer) SpringService.getBean("foundationRMIServer");
					iFoundationRMIServer.rechargeMission(account_id, money, type);
				} catch (Exception e) {
					logger.error("iFoundationRMIServer.rechargeMission error", e);
				}

			}
		});
		return 1;

	}

}
