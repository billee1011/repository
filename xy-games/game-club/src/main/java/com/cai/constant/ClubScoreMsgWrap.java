/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.cai.common.domain.log.ClubScoreMsgLogModel;
import com.cai.common.util.LimitQueue;

/**
 * 
 * 俱乐部设置牌技分日志
 *
 * @author wu_hc date: 2018年4月25日 下午12:07:21 <br/>
 */
public final class ClubScoreMsgWrap {

	private static final int MAX_MSG_COUNT = 1500;
	/**
	 * 
	 */
	private final LimitQueue<ClubScoreMsgLogModel> scoreMsgQueue = new LimitQueue<>(MAX_MSG_COUNT);

	/**
	 * 俱乐部id
	 */
	private final int clubId;

	/**
	 * @param clubId
	 */
	public ClubScoreMsgWrap(int clubId) {
		this.clubId = clubId;
	}

	public int getClubId() {
		return clubId;
	}

	public void addScoreMsg(ClubScoreMsgLogModel model) {
		scoreMsgQueue.offer(model);
	}

	public void initData(List<ClubScoreMsgLogModel> list) {
		if (list == null) {
			return;
		}
		list.sort(Comparator.comparing(ClubScoreMsgLogModel::getCreate_time).reversed());
		List<ClubScoreMsgLogModel> tempList = list.subList(0, list.size() > MAX_MSG_COUNT ? MAX_MSG_COUNT : list.size());
		Collections.reverse(tempList);
		for (int i = 0; i < tempList.size(); i++) {
			scoreMsgQueue.offer(tempList.get(i));
		}
	}

	public List<ClubScoreMsgLogModel> getMsgList() {
		return Arrays.asList(scoreMsgQueue.toArray(new ClubScoreMsgLogModel[scoreMsgQueue.size()]));
	}
}
