package com.cai.rmi.handler;

import java.util.Map;

import com.cai.common.constant.RMICmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.domain.ClubMemberRecordModel;
import com.cai.common.domain.ClubRuleModel;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.rmi.vo.SSHEClubTireInfoVO;
import com.cai.constant.Club;
import com.cai.constant.ClubRuleTable;
import com.cai.constant.ERuleSettingStatus;
import com.cai.service.ClubService;

/**
 * @author zhanglong date: 2018年6月4日 下午6:13:50
 */
@IRmi(cmd = RMICmd.CLUB_TIRE_INFO_QUERY, desc = "查询俱乐部疲劳值信息 ")
public class SSHEClubTireInfoQueryRMIHandler extends IRMIHandler<SSHEClubTireInfoVO, String> {

	@Override
	protected String execute(SSHEClubTireInfoVO infoVO) {
		StringBuilder infoBuffer = new StringBuilder();
		if (infoVO.getClubId() > 0) { // 查询单个俱乐部疲劳值信息
			Club club = ClubService.getInstance().getClub(infoVO.getClubId());
			if (club != null) {
				infoBuffer.append("俱乐部疲劳值开关状态:").append(club.isTireSwitchOpen()).append(";");
				infoBuffer.append("俱乐部疲劳值每日清零开关状态:").append(club.isTireDailyReset()).append(";");
				Map<Integer, ClubRuleTable> ruleTables = club.ruleTables;
				for (ClubRuleTable ruleTable : ruleTables.values()) {
					ClubRuleModel ruleModel = ruleTable.getClubRuleModel();
					if (ruleModel == null) {
						continue;
					}
					infoBuffer.append("包间:").append(ruleModel.getGame_name());
					infoBuffer.append(", 疲劳值开关:").append(ruleModel.getSetsModel().isStatusTrue(ERuleSettingStatus.TIRE_VALUE_SWITCH)).append(", 疲劳值:")
							.append(ruleModel.getTireValue()).append(";");
				}
				if (infoVO.getAccountId() > 0) {
					ClubMemberModel memModel = club.members.get(infoVO.getAccountId());
					if (memModel != null) {
						Map<Integer, ClubMemberRecordModel> recordMap = memModel.getMemberRecordMap();
						infoBuffer.append("玩家疲劳值信息:");
						for (ClubMemberRecordModel model : recordMap.values()) {
							infoBuffer.append(" day=").append(model.getDay()).append(",疲劳值=").append(model.getTireValue()).append(",局数=")
									.append(model.getGameCount()).append(",大赢家数=").append(model.getBigWinCount()).append(",点心状态=")
									.append((model.getIsLike() == 1)).append(",累计疲劳值=").append(model.getAccuTireValue()).append(";");
						}
					}
				}
			}
		} else { // 查询打开疲劳值开关的俱乐部数量
			int count_1 = 0;
			int count_2 = 0;
			int count_3 = 0;
			int count_4 = 0;
			int count_5 = 0;
			Map<Integer, Club> clubMap = ClubService.getInstance().clubs;
			for (Club club : clubMap.values()) {
				if (club.isTireSwitchOpen()) {
					count_1++;
				}
				if (!club.isTireDailyReset()) {
					count_4++;
				}
				boolean isHave = false;
				Map<Integer, ClubRuleTable> ruleTables = club.ruleTables;
				for (ClubRuleTable ruleTable : ruleTables.values()) {
					if (ruleTable.getClubRuleModel().getSetsModel().isStatusTrue(ERuleSettingStatus.TIRE_VALUE_SWITCH)) {
						isHave = true;
						if (ruleTable.getClubRuleModel().getTireValue() != -200) {
							count_3++;
							break;
						}
					}
				}
				if (isHave) {
					count_2++;
				}
				if (club.isMultiClubRuleTableMode()) {
					count_5++;
				}
			}
			infoBuffer.append("俱乐部疲劳值开关打开的数量:").append(count_1);
			infoBuffer.append("; 有包间疲劳值开关打开的俱乐部数量:").append(count_2);
			infoBuffer.append("; 有包间疲劳值开关打开且疲劳值门槛值不是默认值(-200)的俱乐部数量:").append(count_3);
			infoBuffer.append("; 疲劳值设置为每日不重置的俱乐部数量:").append(count_4);
			infoBuffer.append("; 切换多包间模式的俱乐部数量:").append(count_5);
		}
		return infoBuffer.toString();
	}

}
