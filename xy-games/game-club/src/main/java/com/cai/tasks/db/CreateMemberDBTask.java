/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.tasks.db;

import java.util.List;

import com.cai.common.domain.ClubAccountModel;
import com.cai.common.util.SpringService;
import com.cai.service.ClubDaoService;
import com.cai.tasks.AbstractClubTask;

/**
 * 
 *
 * @author wu_hc date: 2018年5月22日 下午3:52:03 <br/>
 */
public class CreateMemberDBTask extends AbstractClubTask {

	private List<ClubAccountModel> accounts;

	/**
	 * @param accounts
	 */
	public CreateMemberDBTask(List<ClubAccountModel> accounts) {
		this.accounts = accounts;
	}

	@Override
	protected void exe() {
		SpringService.getBean(ClubDaoService.class).batchInsert("insertClubAccount", accounts);
	}
}
