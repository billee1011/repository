package com.cai.rmi.handler;

import java.util.HashMap;

import com.cai.common.constant.RMICmd;
import com.cai.common.define.EBonusPointsType;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.util.GlobalExecutor;
import com.cai.service.BonusPointsService;

/**
 * 
 * 积分操作
 *
 * @author tang date: 2018年07月05日 <br/>
 */
@IRmi(cmd = RMICmd.BONUS_POINTS, desc = "积分操作")
public final class BonusPointsHandler extends IRMIHandler<HashMap<String, String>, Integer> {
	// private static Logger logger =
	// LoggerFactory.getLogger(BonusPointsHandler.class);

	@Override
	protected Integer execute(HashMap<String, String> map) {
		GlobalExecutor.asyn_execute(new Runnable() {
			@Override
			public void run() {
				try {
					int type = Integer.parseInt(map.get("type"));// 充值送积分
					if (type == 0) {
						long accountId = Long.parseLong(map.get("accountId"));
						int isFirstOpenAgent = Integer.parseInt(map.get("isFirstOpenAgent"));// 是否首次开通代理
						int money = Integer.parseInt(map.get("money"));
						if (isFirstOpenAgent == 0) {
							BonusPointsService.getInstance().rechargeSendBonusPoints(accountId, money, EBonusPointsType.RECHARGE_SEND_BP);
						} else {
							BonusPointsService.getInstance().rechargeSendBonusPoints(accountId, money, EBonusPointsType.FIRST_RECHARGE_BP);
						}
					} else if (type == 1) {// 手动操作增减积分
						String accountIds = map.get("accountIds");
						int operateType = Integer.parseInt(map.get("operateType"));
						int score = Integer.parseInt(map.get("score"));
						String remark = map.get("remark");
						if (operateType == EBonusPointsType.BACKUP_ADD.getId()) {
							BonusPointsService.getInstance().operateBonusPoints(accountIds, score, EBonusPointsType.BACKUP_ADD, remark);
						} else if (operateType == EBonusPointsType.BACKUP_DEC.getId()) {
							BonusPointsService.getInstance().operateBonusPoints(accountIds, score, EBonusPointsType.BACKUP_DEC, remark);
						}
					} else if (type == 2) {
						long accountId = Long.parseLong(map.get("accountId"));
						int money = Integer.parseInt(map.get("money"));
						BonusPointsService.getInstance().payBackDecreaseBonusPoints(accountId, money);
					}
				} catch (Exception e) {
					logger.error("BonusPointsHandler error", e);
				}
			}
		});
		return 1;
	}

}
