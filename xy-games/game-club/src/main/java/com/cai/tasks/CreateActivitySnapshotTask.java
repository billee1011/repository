/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.tasks;

import com.cai.constant.Club;
import com.cai.constant.ClubActivityWrap;
import com.cai.service.ClubService;

/**
 * 生成俱乐部活动快照任务，尝试三次
 *
 * @author wu_hc date: 2018年1月31日 下午8:10:39 <br/>
 */
public final class CreateActivitySnapshotTask implements Runnable {

	private final int clubId;
	private final long activityId;

	/**
	 * @param clubId
	 * @param activityId
	 */
	public CreateActivitySnapshotTask(int clubId, long activityId) {
		this.clubId = clubId;
		this.activityId = activityId;
	}

	@Override
	public void run() {
		Club club = ClubService.getInstance().getClub(clubId);
		if (null == club) {
			return;
		}

		ClubActivityWrap wrap = club.activitys.get(activityId);
		if (null == wrap) {
			return;
		}

		wrap.entrustProxyBuildSnapshot();
	}
}
