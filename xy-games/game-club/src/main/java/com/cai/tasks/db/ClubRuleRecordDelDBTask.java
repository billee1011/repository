package com.cai.tasks.db;

import java.util.List;

import com.cai.common.domain.ClubRuleRecordModel;
import com.cai.common.util.SpringService;
import com.cai.service.ClubDaoService;
import com.cai.tasks.AbstractClubTask;

public class ClubRuleRecordDelDBTask extends AbstractClubTask {

	private List<ClubRuleRecordModel> delList;

	public ClubRuleRecordDelDBTask(List<ClubRuleRecordModel> list) {
		this.delList = list;
	}

	@Override
	protected void exe() {
		SpringService.getBean(ClubDaoService.class).batchInsert("deleteClubRuleRecordModel", delList);
	}

}
