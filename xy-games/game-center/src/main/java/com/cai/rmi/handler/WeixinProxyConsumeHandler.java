package com.cai.rmi.handler;

import java.util.HashMap;

import com.cai.common.constant.RMICmd;
import com.cai.common.domain.Account;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.util.GlobalExecutor;
import com.cai.service.PublicServiceImpl;
import com.cai.service.WeiXinProxyConsumeService;

/**
 * 调用记录代理消耗的rmi
 * 
 * @author wuhaoran
 */
@IRmi(cmd = RMICmd.WEIXIN_PROXY_CONSUME, desc = "代理消耗rmi")
public final class WeixinProxyConsumeHandler extends IRMIHandler<HashMap<String, String>, Integer> {

	@Override
	protected Integer execute(HashMap<String, String> map) {
		GlobalExecutor.asyn_execute(new Runnable() {
			@Override
			public void run() {
				try {
					long accountId = Long.valueOf(map.get("accountId").toString());
					Account account = PublicServiceImpl.getInstance().getAccount(accountId);
					if (account == null || account.getAccountModel().getIs_agent() != 1) {
						return;
					}
					String reduceType = map.get("reduceType").toString(); // 扣豆类型(0普通豆1专属豆)
					int gold = 0;
					int exclusiveGold = 0;
					if ("0".equals(reduceType)) {
						gold = Integer.valueOf(map.get("gold").toString());
					} else if ("1".equals(reduceType)) {
						exclusiveGold = Integer.valueOf(map.get("exclusiveGold").toString());
					}
					// String brand = map.get("brand");
					int gameTypeIndex = Integer.valueOf(map.get("gameTypeIndex").toString());
					WeiXinProxyConsumeService.getInstance().addProxyConsumeStatistics(accountId, gameTypeIndex, 1, gold, exclusiveGold); // 每次默认是添加1局
				} catch (Exception e) {
					logger.error("addProxyConsumeStatistics error", e);
				}
			}
		});
		return 1;
	}

}
