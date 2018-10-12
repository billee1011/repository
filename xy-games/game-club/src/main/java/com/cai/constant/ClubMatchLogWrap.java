package com.cai.constant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.cai.common.domain.ClubMatchLogModel;
import com.cai.common.util.LimitQueue;
import com.cai.common.util.MyDateUtil;

import org.apache.commons.lang.time.DateUtils;

public class ClubMatchLogWrap {
	private static final int MAX_MSG_COUNT = 50;
	/**
	 * 
	 */
	private final LimitQueue<ClubMatchLogModel> matchLogQueue = new LimitQueue<>(MAX_MSG_COUNT);

	/**
	 * 俱乐部id
	 */
	private final int clubId;

	/**
	 * @param clubId
	 */
	public ClubMatchLogWrap(int clubId) {
		this.clubId = clubId;
	}

	public int getClubId() {
		return clubId;
	}

	public void addMatchLog(ClubMatchLogModel model) {
		// 移除超过15天的记录
		Date beginDate = DateUtils.addDays(MyDateUtil.getZeroDate(new Date()), -14);
		List<ClubMatchLogModel> list = new ArrayList<>(matchLogQueue);
		for (int i = 0; i < list.size(); i++) {
			ClubMatchLogModel logModel = list.get(i);
			if (logModel.getCreate_time().getTime() < beginDate.getTime()) {
				matchLogQueue.poll();
			}
		}
		matchLogQueue.offer(model);
	}

	public void initData(List<ClubMatchLogModel> list) {
		if (list == null) {
			return;
		}
		list.sort(Comparator.comparing(ClubMatchLogModel::getCreate_time).reversed());
		List<ClubMatchLogModel> tempList = list.subList(0, list.size() > MAX_MSG_COUNT ? MAX_MSG_COUNT : list.size());
		Collections.reverse(tempList);
		for (int i = 0; i < tempList.size(); i++) {
			matchLogQueue.offer(tempList.get(i));
		}
	}

	public List<ClubMatchLogModel> getLogList() {
		return Arrays.asList(matchLogQueue.toArray(new ClubMatchLogModel[matchLogQueue.size()]));
	}
}
