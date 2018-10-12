package com.cai.tasks.db;

import com.cai.common.domain.ClubMemberRecordModel;
import com.cai.common.util.SpringService;
import com.cai.service.ClubDaoService;
import com.cai.tasks.AbstractClubTask;

/**
 * 
 *
 * @author zhanglong date: 2018年6月8日 上午10:07:33
 */
public class ClubMemberRecordCreateDBTask extends AbstractClubTask {

	private ClubMemberRecordModel model;

	public ClubMemberRecordCreateDBTask(ClubMemberRecordModel model) {
		this.model = model;
	}

	@Override
	protected void exe() {
		SpringService.getBean(ClubDaoService.class).getDao().insertClubMemberRecordModel(model);
	}

}
