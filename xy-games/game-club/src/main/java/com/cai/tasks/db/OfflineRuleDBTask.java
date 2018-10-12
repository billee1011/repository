/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.tasks.db;

import java.util.List;

import com.cai.common.util.SpringService;
import com.cai.service.ClubDaoService;
import com.cai.tasks.AbstractClubTask;

/**
 * 
 * 下线的时候用
 * 
 * @author wu_hc date: 2018年5月23日 下午9:02:11 <br/>
 */
public class OfflineRuleDBTask extends AbstractClubTask {

	private List<Integer> ruleIds = null;

	/**
	 * @param ruleIds
	 */
	public OfflineRuleDBTask(List<Integer> ruleIds) {
		this.ruleIds = ruleIds;
	}

	@Override
	protected void exe() {
		SpringService.getBean(ClubDaoService.class).batchUpdate("deleteClubRuleWithRuleId", ruleIds);
	}

}
