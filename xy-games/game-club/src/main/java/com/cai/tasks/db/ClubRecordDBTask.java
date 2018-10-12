/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.tasks.db;

import java.util.List;

import com.cai.common.domain.ClubMemberModel;
import com.cai.common.util.SpringService;
import com.cai.service.ClubDaoService;
import com.cai.tasks.AbstractClubTask;

/**
 * 
 * 俱乐部统计局统计局数
 *
 * @author wu_hc date: 2018年5月21日 上午10:28:46 <br/>
 */
public class ClubRecordDBTask extends AbstractClubTask {

	private final List<ClubMemberModel> members;

	/**
	 * @param members
	 */
	public ClubRecordDBTask(List<ClubMemberModel> members) {
		this.members = members;
	}

	@Override
	protected void exe() {
		SpringService.getBean(ClubDaoService.class).batchUpdate("updateClubAccountGameCount", members);
	}
}
