package com.cai.rmi.handler;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.cai.common.constant.RMICmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.domain.ClubMemberRecordModel;
import com.cai.common.domain.ClubRuleModel;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.rmi.vo.ClubAndAccountVo;
import com.cai.common.util.TimeUtil;
import com.cai.config.ClubCfg;
import com.cai.constant.Club;
import com.cai.constant.ClubPlayer;
import com.cai.constant.ClubRuleTable;
import com.cai.constant.ClubTable;
import com.cai.constant.ERuleSettingStatus;
import com.cai.service.ClubService;

/**
 * @author wu_ch date: 2017年11月20日 下午5:48:24 <br/>
 */
@IRmi(cmd = RMICmd.CLUB_AND_ACCOUNT, desc = "查玩家是否在俱乐部里")
public final class ClubHasAccountRMIHandler extends IRMIHandler<ClubAndAccountVo, ClubAndAccountVo> {

	@Override
	public ClubAndAccountVo execute(ClubAndAccountVo vo) {

		Club club = ClubService.getInstance().getClub(vo.getClubId());
		if (null == club) {
			return vo.setInClub(false).setClubGroup(false);
		}

		ClubMemberModel member = club.members.get(vo.getAccountId());
		if (null != member) {
			vo.setInClub(true);
			vo.setIdentity(member.getIdentity());
		}

		Set<String> groupIds = vo.getAccountGroupIds();
		if (null != groupIds && !groupIds.isEmpty()) {
			club.groupSet.forEach((id) -> {
				for (final String accountGroupId : groupIds) {
					if (Objects.equals(id, accountGroupId)) {
						vo.setClubGroup(true);
						break;
					}
				}
			});
		}

		if (null != member) {
			ClubRuleModel ruleModel = club.clubModel.getRule(vo.getRuleId());
			vo.setIsTireEnough(true);
			// 疲劳值判断
			if (club.isTireSwitchOpen()) {
				if (ruleModel.getSetsModel().isStatusTrue(ERuleSettingStatus.TIRE_VALUE_SWITCH)) {
					ClubMemberRecordModel memberRecordModel = club.getMemberRecordModelByDay(1, member);
					if (club.getMemberRealUseTire(memberRecordModel) < ruleModel.getTireValue()) {
						vo.setIsTireEnough(false);
					}
				}
			}
			// 局数限制判断
			vo.setLeftGameLimitRound(-1);
			if (ruleModel.getSetsModel().isStatusTrue(ERuleSettingStatus.GAME_ROUND_LIMIT_SWITCH)) {
				int leftRound = member.checkLimitRound(ruleModel);
				vo.setLeftGameLimitRound(leftRound);
			}
			// 亲友圈福卡限制判断
			vo.setClubWelfareEnough(true);
			if (club.clubWelfareWrap.isOpenClubWelfare()) {
				if (ruleModel.getSetsModel().isStatusTrue(ERuleSettingStatus.CLUB_WELFARE_SWITCH)) {
					if (member.getClubWelfare() < ruleModel.getLimitWelfare()) {
						vo.setClubWelfareEnough(false);
						vo.setLimitWelfare(ruleModel.getLimitWelfare());
					}
				}
			}

			vo.setCanEnterTable(true);
			if (club.isMultiClubRuleTableMode()) {
				if (System.currentTimeMillis() - member.getLastAutoKickTime() < ClubCfg.get().getPlayerEnterTableBanTime() * TimeUtil.SECOND) {
					vo.setCanEnterTable(false);
					vo.setEnterBanTime(ClubCfg.get().getPlayerEnterTableBanTime());
				}
			}
		}
		ClubRuleTable clubRuleTable = club.ruleTables.get(vo.getRuleId());
		if (clubRuleTable != null) {
			ClubTable table = clubRuleTable.getTable(vo.getTableIndex());
			if (table != null) {
				vo.setTablePassport(table.getPassport());

				// 禁止同桌判断
				Map<Long, ClubPlayer> players = table.getPlayers();
				for (Long userId : players.keySet()) {
					if (userId != vo.getAccountId()) {
						ClubMemberModel memModel = club.members.get(userId);
						if (memModel == null) {
							member.removeBanPlayer(userId);
							continue;
						}
						Map<Long, Long> tmpMap = memModel.getMemberBanPlayerMap();
						if (tmpMap != null && tmpMap.containsKey(vo.getAccountId())) {
							vo.setBanPlayerName(players.get(userId).getUserName());
							break;
						}
					}
				}
			}
		}
		return vo;
	}

}
