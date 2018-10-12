package com.cai.constant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.cai.common.domain.log.ClubWelfareLotteryMsgLogModel;
import com.cai.common.util.LimitQueue;
import com.cai.common.util.MyDateUtil;

import org.apache.commons.lang.time.DateUtils;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/9/25 19:58
 */
public class ClubWelfareLotteryLogWrap {

	private static final int MAX_MSG_COUNT = 200;

	private final LimitQueue<ClubWelfareLotteryMsgLogModel> logQueue = new LimitQueue<>(MAX_MSG_COUNT);

	public void addLotteryLog(ClubWelfareLotteryMsgLogModel model) {
		// 移除超过15天的记录
		Date beginDate = DateUtils.addDays(MyDateUtil.getZeroDate(new Date()), -14);
		List<ClubWelfareLotteryMsgLogModel> list = new ArrayList<>(logQueue);
		for (int i = 0; i < list.size(); i++) {
			ClubWelfareLotteryMsgLogModel logModel = list.get(i);
			if (logModel.getCreate_time().getTime() < beginDate.getTime()) {
				logQueue.poll();
			}
		}
		logQueue.offer(model);
	}

	public void initData(List<ClubWelfareLotteryMsgLogModel> list) {
		if (list == null) {
			return;
		}
		list.sort(Comparator.comparing(ClubWelfareLotteryMsgLogModel::getCreate_time).reversed());
		List<ClubWelfareLotteryMsgLogModel> tempList = list.subList(0, list.size() > MAX_MSG_COUNT ? MAX_MSG_COUNT : list.size());
		Collections.reverse(tempList);
		for (int i = 0; i < tempList.size(); i++) {
			logQueue.offer(tempList.get(i));
		}
	}

	public List<ClubWelfareLotteryMsgLogModel> getLogList() {
		return Arrays.asList(logQueue.toArray(new ClubWelfareLotteryMsgLogModel[logQueue.size()]));
	}
}
