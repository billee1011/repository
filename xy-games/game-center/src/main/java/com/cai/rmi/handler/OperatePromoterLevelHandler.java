package com.cai.rmi.handler;

import org.apache.commons.lang.StringUtils;

import com.cai.common.constant.RMICmd;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.core.TaskThreadPool;
import com.cai.future.runnable.DownRecommendLevelRunnble;

/**
 * 
 * 推广员降级操作
 *
 * @author tang date: 2018年05月31日<br/>
 */
@IRmi(cmd = RMICmd.DOWN_PROMOTER_LEVEL, desc = "推广员降级操作")
public final class OperatePromoterLevelHandler extends IRMIHandler<String, Integer> {

	@Override
	protected Integer execute(String accountIds) {
		if (StringUtils.isBlank(accountIds)) {
			return 0;
		}
		String[] accounts = accountIds.split(",");
		for (String accountIdStr : accounts) {
			long accountId = Long.parseLong(accountIdStr);
			// RecommendReceiveModel model =
			// RecommenderReceiveService.getInstance().getRecommendReceiveModel(accountId);
			// int money = model.getReceive();
			DownRecommendLevelRunnble runnlble = new DownRecommendLevelRunnble(accountId, 0);
			TaskThreadPool.getInstance().addTask(runnlble);
			// model.resetModel();
		}
		return 1;

	}

}
