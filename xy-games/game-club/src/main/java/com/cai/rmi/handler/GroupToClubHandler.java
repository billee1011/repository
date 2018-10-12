package com.cai.rmi.handler;

import java.util.HashMap;

import com.cai.common.constant.RMICmd;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.type.ClubExitType;
import com.cai.constant.Club;
import com.cai.service.ClubService;

/**
 * 
 * 
 *
 * @author tang date: 2017年11月30日 下午3:41:24 <br/>
 */
@IRmi(cmd = RMICmd.GROUP_TO_CLUB, desc = "微信群同步俱乐部操作")
public final class GroupToClubHandler extends IRMIHandler<HashMap<String, String>, Integer> {

	@Override
	public Integer execute(HashMap<String, String> paramsMap) {
		String accountId = paramsMap.get("accountId");
		String groupId = paramsMap.get("groupId");
		Integer clubId = ClubService.getInstance().groupClubMaps.get(groupId);
		if (clubId == null || clubId == 0) {
			return Club.CLUB_NOT_FIND;
		}
		Club club = ClubService.getInstance().getClub(clubId);
		if (club == null) {
			return Club.CLUB_NOT_FIND;
		}
		int result = ClubService.getInstance().kickClub(club.getClubId(), club.getOwnerId(), Long.parseLong(accountId), ClubExitType.KICK);
		return result;
	}

}
