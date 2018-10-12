/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.tasks.db;

import java.util.Map;

import com.cai.common.domain.ClubAccountModel;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.domain.ClubMemberRecordModel;
import com.cai.common.util.SpringService;
import com.cai.constant.Club;
import com.cai.service.ClubDaoService;
import com.cai.service.ClubService;
import com.cai.tasks.AbstractClubTask;

/**
 * 
 *
 * @author wu_hc date: 2018年5月25日 上午7:52:05 <br/>
 */
public final class DelMemberDBTask extends AbstractClubTask {
	private final int clubId;
	private final long accountId;
	private ClubMemberModel memberModel;

	/**
	 * @param clubId
	 * @param accountId
	 */
	public DelMemberDBTask(int clubId, long accountId, ClubMemberModel memberModel) {
		this.clubId = clubId;
		this.accountId = accountId;
		this.memberModel = memberModel;
	}

	@Override
	protected void exe() {

		Club club = ClubService.getInstance().getClub(clubId);
		ClubAccountModel requestClubAccount = new ClubAccountModel();
		requestClubAccount.setAccount_id(accountId);
		requestClubAccount.setClub_id(clubId);
		SpringService.getBean(ClubDaoService.class).getDao().deleteClubAccount(requestClubAccount);
		try {
			SpringService.getBean(ClubDaoService.class).getDao().deleteClubMemberRecord(clubId, accountId);
		} catch (Exception e) {
			logger.error("deleteClubMemberRecord error,clubId={},accountId={}", clubId, accountId, e);

			if (club != null && memberModel != null) {
				club.runInReqLoop(() -> {
					Map<Integer, ClubMemberRecordModel> recordMap = memberModel.getMemberRecordMap();
					for (ClubMemberRecordModel model : recordMap.values()) {
						club.addDelMemberRecordData(model);
					}
				});
			}
		}

		// 清除禁止同桌数据
		Map<Long, ClubMemberModel> members = club.members;
		for (ClubMemberModel model : members.values()) {
			Map<Long, Long> memberBanPlayerMap = model.getMemberBanPlayerMap();
			if (memberBanPlayerMap != null && memberBanPlayerMap.containsKey(accountId)) {
				model.removeBanPlayer(accountId);
			}
		}
		SpringService.getBean(ClubDaoService.class).getDao().deleteClubMemberBanPlayer(clubId, accountId);
	}

}
