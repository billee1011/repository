package com.cai.tasks.db;

import com.cai.common.domain.ClubRuleModel;
import com.cai.common.util.SpringService;
import com.cai.service.ClubDaoService;
import com.cai.tasks.AbstractClubTask;

public class ClubRuleDBTask extends AbstractClubTask {

	private ClubRuleModel model;

	public ClubRuleDBTask(ClubRuleModel model) {
		this.model = model;
	}

	@Override
	protected void exe() {
		SpringService.getBean(ClubDaoService.class).getDao().updateClubRule(this.model);
	}

}
