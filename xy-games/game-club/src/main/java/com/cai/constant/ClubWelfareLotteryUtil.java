package com.cai.constant;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.cai.common.domain.ClubWelfareLotteryInfo;
import com.cai.common.domain.ClubWelfareRewardModel;
import com.cai.common.domain.ClubWelfareSwitchModel;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.TimeUtil;
import com.cai.common.util.WeightBuilder;
import com.cai.dao.ClubDao;
import com.cai.dictionary.ClubWelfareRewardDict;
import com.cai.dictionary.ClubWelfareSwitchModelDict;
import com.cai.service.ClubDaoService;
import com.google.common.collect.Lists;
import com.xianyi.framework.core.concurrent.DefaultWorkerLoopGroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/9/21 16:58
 */
public class ClubWelfareLotteryUtil {

	private static Logger logger = LoggerFactory.getLogger(ClubWelfareLotteryUtil.class);

	private static Map<Integer, ClubWelfareLotteryInfo> clubWelfareLotteryMap = new ConcurrentHashMap<>();

	private static final DefaultWorkerLoopGroup dbWorker = DefaultWorkerLoopGroup.newGroup("club-lottery-db-work-thread", 1);

	public static void init() {
		List<ClubWelfareLotteryInfo> temps = SpringService.getBean(ClubDaoService.class).getDao().getClubWelfareLotteryInfoList();
		for (ClubWelfareLotteryInfo info : temps) {
			clubWelfareLotteryMap.put(info.getRewardId(), info);
		}
	}

	public static List<ClubWelfareRewardModel> randomRealReward(int count) {
		List<ClubWelfareRewardModel> rewards = Lists.newArrayList();
		List<ClubWelfareRewardModel> normalRealRewardList = ClubWelfareRewardDict.getInstance().getNormalRealRewardList();
		List<ClubWelfareRewardModel> assistRealRewardList = ClubWelfareRewardDict.getInstance().getAssistRealRewardList();
		for (int i = 0; i < count; i++) {
			ClubWelfareRewardModel reward = getRealReward(normalRealRewardList, assistRealRewardList);
			if (reward != null) {
				rewards.add(reward);
			}
		}

		return rewards;
	}

	private static synchronized ClubWelfareRewardModel getRealReward(List<ClubWelfareRewardModel> normalRealRewardList,
			List<ClubWelfareRewardModel> assistRealRewardList) {
		ClubDao clubDao = SpringService.getBean(ClubDaoService.class).getDao();
		// 是否跨天重置已抽取次数
		ClubWelfareSwitchModel switchModel = ClubWelfareSwitchModelDict.getInstance().getClubWelfareSwitchModel();
		if (switchModel != null) {
			if (switchModel.getLastLotteryTime() != null && !TimeUtil.isSameDay(switchModel.getLastLotteryTime())) {
				List<ClubWelfareLotteryInfo> tmpList = Lists.newArrayList();
				for (ClubWelfareLotteryInfo info : clubWelfareLotteryMap.values()) {
					info.setLotteryCount(0);
					tmpList.add(info);
				}
				dbWorker.next().runInLoop(() -> clubDao.batchUpdate("updateClubWelfareLotteryInfo", tmpList));
			}
			switchModel.setLastLotteryTime(new Date());
			//更新抽奖时间
			dbWorker.next().runInLoop(() -> clubDao.updateClubWelfareLastLotteryTime(switchModel));
		}
		ClubWelfareRewardModel reward = null;
		if (checkHasNormalRealReward(normalRealRewardList)) { //随机常规奖励
			WeightBuilder<ClubWelfareRewardModel> weightBuilder = WeightBuilder.newBuilder();
			for (ClubWelfareRewardModel model : normalRealRewardList) {
				int lotteryNum = 0;
				if (clubWelfareLotteryMap.containsKey(model.getId())) {
					lotteryNum = clubWelfareLotteryMap.get(model.getId()).getLotteryCount();
				}
				int num = model.getDailyLotteryNum() - lotteryNum;
				if (num > 0) {
					weightBuilder.append(num, model);
				}
			}
			reward = weightBuilder.calculateAndGet();
			if (reward == null) {
				logger.error("clubwelfare lottery normalRealReward error!!!");
				return null;
			}
			int rewardId = reward.getId();
			if (clubWelfareLotteryMap.containsKey(rewardId)) {
				ClubWelfareLotteryInfo info = clubWelfareLotteryMap.get(rewardId);
				info.setLotteryCount(info.getLotteryCount() + 1);
				clubWelfareLotteryMap.put(rewardId, info);
				dbWorker.next().runInLoop(() -> clubDao.updateClubWelfareLotteryInfo(clubWelfareLotteryMap.get(rewardId)));
			} else {
				ClubWelfareLotteryInfo info = new ClubWelfareLotteryInfo();
				info.setRewardId(rewardId);
				info.setItemId(reward.getAwardId());
				info.setLotteryCount(1);
				clubWelfareLotteryMap.put(rewardId, info);
				//insert db
				dbWorker.next().runInLoop(() -> clubDao.insertClubWelfareLotteryInfo(clubWelfareLotteryMap.get(rewardId)));
			}
		} else { //随机辅助奖励
			int size = assistRealRewardList.size();
			if (size <= 0) {
				logger.error("clubwelfare lottery assistRealReward not config,please check!!!");
				return null;
			}
			reward = assistRealRewardList.get(RandomUtil.getRandomNumber(size));
		}

		return reward;
	}

	private static boolean checkHasNormalRealReward(List<ClubWelfareRewardModel> normalRealRewardList) {
		for (ClubWelfareRewardModel model : normalRealRewardList) {
			if (!clubWelfareLotteryMap.containsKey(model.getId())) {
				return true;
			}
			if (model.getDailyLotteryNum() > clubWelfareLotteryMap.get(model.getId()).getLotteryCount()) {
				return true;
			}
		}
		return false;
	}

	public static List<ClubWelfareRewardModel> randomShowReward(int count) {
		List<ClubWelfareRewardModel> rewards = Lists.newArrayList();
		WeightBuilder<ClubWelfareRewardModel> weightBuilder = WeightBuilder.newBuilder();
		List<ClubWelfareRewardModel> showRewardList = ClubWelfareRewardDict.getInstance().getShowRewardList();
		if (showRewardList.size() <= 0) {
			logger.error("clubwelfare lottery ShowReward not config,please check!!!");
			return rewards;
		}
		for (ClubWelfareRewardModel model : showRewardList) {
			weightBuilder.append(model.getShowRate(), model);
		}
		weightBuilder.calculate();
		for (int i = 0; i < count; i++) {
			rewards.add(weightBuilder.get());
		}
		return rewards;
	}

}
