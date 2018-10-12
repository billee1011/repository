/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.tasks.db;

import java.util.Date;
import java.util.Map;

import com.cai.common.define.EClubEventType;
import com.cai.common.domain.ClubEventLogModel;
import com.cai.common.domain.ClubModel;
import com.cai.common.domain.log.ClubApplyLogModel;
import com.cai.common.util.SpringService;
import com.cai.constant.Club;
import com.cai.service.ClubDaoService;
import com.cai.service.ClubService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.tasks.AbstractClubTask;
import com.cai.utils.ClubEventLog;
import com.cai.utils.ClubRoomUtil;

/**
 * 俱乐部删除任务
 *
 * @author wu_hc date: 2018年5月21日 上午9:31:01 <br/>
 */
public final class DelClubDBTask extends AbstractClubTask {
	private final Club club;

	public DelClubDBTask(Club club) {
		this.club = club;
	}

	@Override
	public void exe() {
		try {
			ClubModel clubModel = club.clubModel;
			SpringService.getBean(ClubDaoService.class).getDao().deleteClub(clubModel.getClub_id());
			SpringService.getBean(ClubDaoService.class).getDao().deleteClubRule(clubModel.getClub_id());
			SpringService.getBean(ClubDaoService.class).getDao().deleteClubAllGroup(clubModel.getClub_id());
			SpringService.getBean(ClubDaoService.class).getDao().deleteClubAllAccount(clubModel.getClub_id());
			SpringService.getBean(ClubDaoService.class).getDao().deleteAllClubActivity(clubModel.getClub_id());
			SpringService.getBean(ClubDaoService.class).getDao().deleteClubAllMemberRecord(clubModel.getClub_id());
			SpringService.getBean(ClubDaoService.class).getDao().deleteClubAllMemberBanPlayer(clubModel.getClub_id());
			SpringService.getBean(ClubDaoService.class).getDao().deleteClubAllMatch(clubModel.getClub_id());
			SpringService.getBean(ClubDaoService.class).getDao().deleteClubDataModel(clubModel.getClub_id());
			SpringService.getBean(ClubDaoService.class).getDao().deleteClubAllRuleRecordModel(clubModel.getClub_id());

			club.disband();
			ClubEventLog.event(new ClubEventLogModel(club.getClubId(), club.getOwnerId(), EClubEventType.DELETE).setVal1(club.getMemberCount()));
			Map<Long, ClubApplyLogModel> applyMap = club.requestQuitMembers;
			for (ClubApplyLogModel model : applyMap.values()) {
				if (model != null && !model.isHandle()) {
					model.setHandle(true);
					MongoDBServiceImpl.getInstance().updateClubApplyLogModel(model);
				}
			}

			//包间消耗统计相关
			club.ruleTables.forEach((ruleId, ruleTable) -> ClubRoomUtil.saveRuleCostModel(ruleTable, club.getMemberCount(), new Date()));

		} finally {
			ClubService.getInstance().removeClub(club.getClubId());
		}
	}
}
