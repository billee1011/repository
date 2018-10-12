package com.cai.rmi.handler;

import com.cai.common.constant.RMICmd;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.rmi.vo.ClubActivitySnapshotBuildRMIVo;
import com.cai.constant.Club;
import com.cai.constant.ClubActivityWrap;
import com.cai.service.ClubService;

/**
 * 
 *
 * @author wu_hc date: 2018年1月24日 下午12:15:54 <br/>
 */
@IRmi(cmd = RMICmd.CLUB_ACTIVITY_SNAPSHOT_BUILD, desc = "")
public final class ClubActivitySnapshotRMIHandler extends IRMIHandler<ClubActivitySnapshotBuildRMIVo, Integer> {
	@Override
	protected Integer execute(ClubActivitySnapshotBuildRMIVo vo) {

		logger.warn("俱乐部服接受到来自后台操作!{}", vo);

		int clubId = vo.getClubId();
		if (clubId <= 0) {
			logger.error("后台操作，俱乐部id[{}] 不合理！", clubId);
			return Club.FAIL;
		}
		Club club = ClubService.getInstance().getClub(clubId);
		if (null == club) {
			logger.error("后台操作，不存在俱乐部[{}]！", clubId);
			return Club.FAIL;
		}

		ClubActivityWrap wrap = club.activitys.get(vo.getActivityId());
		if (null == wrap) {
			return Club.FAIL;
		}

		return wrap.entrustProxyBuildSnapshot();
	}
}
