package com.cai.tasks.db;

import com.cai.common.util.SpringService;
import com.cai.service.ClubDaoService;
import com.cai.tasks.AbstractClubTask;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/8/21 14:48
 */
public class ClubMemberRecordDelDBTask extends AbstractClubTask {

	private int clubId;

	private long accountId;

	public ClubMemberRecordDelDBTask(int clubId, long accountId) {
		this.clubId = clubId;
		this.accountId = accountId;
	}
	@Override
	protected void exe() {
		SpringService.getBean(ClubDaoService.class).getDao().deleteClubMemberRecord(clubId, accountId);
	}
}
