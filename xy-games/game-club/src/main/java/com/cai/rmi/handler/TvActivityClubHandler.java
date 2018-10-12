package com.cai.rmi.handler;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.cai.common.constant.RMICmd;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.service.ClubCacheService;
import com.cai.service.ClubService;

/**
 * 
 *
 * @author wu_hc date: 2017年8月02日 上午16:11:00 <br/>
 */
@IRmi(cmd = RMICmd.ACCOUNT_HAS_CLUB, desc = "指定的玩家中有多少加入了亲友圈")
public final class TvActivityClubHandler extends IRMIHandler<List<Long>, Integer> {
	@Override
	protected Integer execute(List<Long> ids) {
		if (ids == null || ids.size() == 0) {
			return 0;
		}
		int count = 0;
		for (long accountId : ids) {
			Optional<Set<Integer>> clubIdsOpt = ClubCacheService.getInstance().optMemberClubs(accountId);
			if(clubIdsOpt.isPresent() && clubIdsOpt.get().size() > 0){
				count++;
			}
		}
		return count;
	}
}
