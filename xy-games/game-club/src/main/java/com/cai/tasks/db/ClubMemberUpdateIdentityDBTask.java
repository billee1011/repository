package com.cai.tasks.db;

import java.util.List;

import com.cai.common.domain.ClubMemberModel;
import com.cai.common.util.SpringService;
import com.cai.service.ClubDaoService;
import com.cai.tasks.AbstractClubTask;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/8/9 11:15
 */
public class ClubMemberUpdateIdentityDBTask extends AbstractClubTask {

	private List<ClubMemberModel> list;

	public ClubMemberUpdateIdentityDBTask(List<ClubMemberModel> list) {
		this.list = list;
	}

	@Override
	protected void exe() {
		SpringService.getBean(ClubDaoService.class).getDao().batchUpdate("updateClubAccountIdentity", this.list);
	}
}
