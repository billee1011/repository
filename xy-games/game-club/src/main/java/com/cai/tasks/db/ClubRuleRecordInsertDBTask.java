package com.cai.tasks.db;

import java.util.List;

import com.cai.common.domain.ClubRuleRecordModel;
import com.cai.common.util.SpringService;
import com.cai.service.ClubDaoService;
import com.cai.tasks.AbstractClubTask;

public class ClubRuleRecordInsertDBTask extends AbstractClubTask {

	private List<ClubRuleRecordModel> insertList;

	public ClubRuleRecordInsertDBTask(List<ClubRuleRecordModel> list) {
		this.insertList = list;
	}

	@Override
	protected void exe() {
		SpringService.getBean(ClubDaoService.class).batchInsert("insertClubRuleRecordModel", insertList);
	}

}
