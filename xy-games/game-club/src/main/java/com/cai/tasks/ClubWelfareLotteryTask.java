package com.cai.tasks;

import java.util.List;

import com.cai.common.ClubMemWelfareLotteryInfo;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.util.ServerRandomUtil;
import com.cai.config.ClubCfg;
import com.cai.constant.Club;
import com.cai.service.ClubCacheService;
import com.google.common.collect.Lists;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/9/25 17:57
 */
public class ClubWelfareLotteryTask extends AbstractClubTask {
	private final Club club;
	private final int current;

	public ClubWelfareLotteryTask(Club club, long current) {
		this.club = club;
		this.current = (int) (current / 1000);
	}

	@Override
	protected void exe() {
		List<ClubMemWelfareLotteryInfo> targets = Lists.newArrayList();
		for (ClubMemWelfareLotteryInfo info : ClubCacheService.getInstance().lotteryMembers.values()) {
			if (info.getClubId() == club.getClubId() && current - info.getEndTime() > ClubCfg.get().getClubWelfareAotoLotteryTime()) {
				targets.add(info);
			}
		}
		for (ClubMemWelfareLotteryInfo info : targets) {
			ClubMemberModel memberModel = club.members.get(info.getAccountId());
			if (memberModel == null) {
				continue;
			}
			club.clubWelfareWrap.lotteryReward(memberModel, info, ServerRandomUtil.getRandomNumber(5));
		}
	}
}
