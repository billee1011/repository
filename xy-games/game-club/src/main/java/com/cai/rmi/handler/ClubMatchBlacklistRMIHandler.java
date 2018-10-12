package com.cai.rmi.handler;

import java.util.ArrayList;
import java.util.List;

import com.cai.common.constant.RMICmd;
import com.cai.common.define.EClubEventType;
import com.cai.common.define.EClubSettingStatus;
import com.cai.common.domain.ClubEventLogModel;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.rmi.vo.ClubMatchBlacklistVo;
import com.cai.constant.Club;
import com.cai.service.ClubService;
import com.cai.utils.ClubEventLog;
import com.cai.utils.Utils;

/**
 * @author zhanglong date: 2018年7月3日 下午6:20:03
 */
@IRmi(cmd = RMICmd.CLUB_MATCH_BLACKLIST, desc = "亲友圈自建赛黑名单/白名单")
public class ClubMatchBlacklistRMIHandler extends IRMIHandler<ClubMatchBlacklistVo, ClubMatchBlacklistVo> {

	@Override
	protected ClubMatchBlacklistVo execute(ClubMatchBlacklistVo req) {
		ClubMatchBlacklistVo vo = new ClubMatchBlacklistVo();
		// 黑名单操作
		if (req.getOpType() == 1) {
			List<Integer> list = new ArrayList<>();
			ClubService.getInstance().clubs.forEach((id, club) -> {
				if (club.setsModel.isStatusTrue(EClubSettingStatus.CLUB_MATCH_BLACK_LIST)) {
					list.add(club.getClubId());
				}
			});
			vo.setResult(true);
			vo.setClubList(list);
			return vo;
		} else if (req.getOpType() == 2) {
			Club club = ClubService.getInstance().clubs.get(req.getClubId());
			if (club != null) {
				if (club.setsModel.isStatusTrue(EClubSettingStatus.CLUB_MATCH_WHITE_LIST)) { //如果已经在白名单中不能再添加到黑名单
					vo.setResult(false);
					return vo;
				}
				club.runInReqLoop(() -> {
					if (!club.setsModel.isStatusTrue(EClubSettingStatus.CLUB_MATCH_BLACK_LIST)) {
						club.setsModel.statusAdd(EClubSettingStatus.CLUB_MATCH_BLACK_LIST);
						club.clubModel.setSettingStatus(club.setsModel.getStatus());
						Utils.notityClubSetsUpdate(club);

						ClubEventLog
								.event(new ClubEventLogModel(club.getClubId(), club.getOwnerId(), EClubEventType.CLUB_MATCH_BLACKLIST_U).setVal1(1));
					}
				});
				vo.setResult(true);
				return vo;
			}
		} else if (req.getOpType() == 3) {
			Club club = ClubService.getInstance().clubs.get(req.getClubId());
			if (club != null) {
				club.runInReqLoop(() -> {
					if (club.setsModel.isStatusTrue(EClubSettingStatus.CLUB_MATCH_BLACK_LIST)) {
						club.setsModel.statusDel(EClubSettingStatus.CLUB_MATCH_BLACK_LIST);
						club.clubModel.setSettingStatus(club.setsModel.getStatus());

						ClubEventLog
								.event(new ClubEventLogModel(club.getClubId(), club.getOwnerId(), EClubEventType.CLUB_MATCH_BLACKLIST_U).setVal1(0));
						Utils.notityClubSetsUpdate(club);
					}
				});
				vo.setResult(true);
				return vo;
			}
		}
		// 白名单操作
		if (req.getOpType() == 4) {
			List<Integer> list = new ArrayList<>();
			ClubService.getInstance().clubs.forEach((id, club) -> {
				if (club.setsModel.isStatusTrue(EClubSettingStatus.CLUB_MATCH_WHITE_LIST)) {
					list.add(club.getClubId());
				}
			});
			vo.setResult(true);
			vo.setClubList(list);
			return vo;
		} else if (req.getOpType() == 5) {
			Club club = ClubService.getInstance().clubs.get(req.getClubId());
			if (club != null) {
				if (club.setsModel.isStatusTrue(EClubSettingStatus.CLUB_MATCH_BLACK_LIST)) { //如果已经在黑名单中不能再添加到白名单
					vo.setResult(false);
					return vo;
				}
				club.runInReqLoop(() -> {
					if (!club.setsModel.isStatusTrue(EClubSettingStatus.CLUB_MATCH_WHITE_LIST)) {
						club.setsModel.statusAdd(EClubSettingStatus.CLUB_MATCH_WHITE_LIST);
						club.clubModel.setSettingStatus(club.setsModel.getStatus());
						Utils.notityClubSetsUpdate(club);

						ClubEventLog
								.event(new ClubEventLogModel(club.getClubId(), club.getOwnerId(), EClubEventType.CLUB_MATCH_WHITELIST_U).setVal1(1));
					}
				});
				vo.setResult(true);
				return vo;
			}
		} else if (req.getOpType() == 6) {
			Club club = ClubService.getInstance().clubs.get(req.getClubId());
			if (club != null) {
				club.runInReqLoop(() -> {
					if (club.setsModel.isStatusTrue(EClubSettingStatus.CLUB_MATCH_WHITE_LIST)) {
						club.setsModel.statusDel(EClubSettingStatus.CLUB_MATCH_WHITE_LIST);
						club.clubModel.setSettingStatus(club.setsModel.getStatus());

						ClubEventLog
								.event(new ClubEventLogModel(club.getClubId(), club.getOwnerId(), EClubEventType.CLUB_MATCH_WHITELIST_U).setVal1(0));
						Utils.notityClubSetsUpdate(club);
					}
				});
				vo.setResult(true);
				return vo;
			}
		}
		return vo;
	}

}
