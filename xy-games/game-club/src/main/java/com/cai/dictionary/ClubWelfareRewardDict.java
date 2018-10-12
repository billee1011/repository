package com.cai.dictionary;

import java.util.List;

import com.cai.common.ClubWelfareCode;
import com.cai.common.domain.ClubWelfareRewardModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.dao.ClubDao;
import com.cai.service.ClubDaoService;
import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/9/21 18:03
 */
public class ClubWelfareRewardDict {
	private Logger logger = LoggerFactory.getLogger(ClubWelfareRewardDict.class);

	private static ClubWelfareRewardDict instance = new ClubWelfareRewardDict();

	private List<ClubWelfareRewardModel> normalRealRewardList = Lists.newArrayList();

	private List<ClubWelfareRewardModel> assistRealRewardList = Lists.newArrayList();

	private List<ClubWelfareRewardModel> showRewardList = Lists.newArrayList();

	public static ClubWelfareRewardDict getInstance() {
		return instance;
	}

	public ClubWelfareRewardDict() {
	}

	public void load() {
		normalRealRewardList.clear();
		assistRealRewardList.clear();
		showRewardList.clear();
		PerformanceTimer timer = new PerformanceTimer();
		ClubDao dao = SpringService.getBean(ClubDaoService.class).getDao();
		List<ClubWelfareRewardModel> list = dao.getClubWelfareRewardList();
		for (ClubWelfareRewardModel model : list) {
			if (model.getRewardType() == ClubWelfareCode.REWARD_TYPE_REAL) {
				if (model.getRewardWeightType() == ClubWelfareCode.REWARD_WEIGHT_TYPE_NORMAL) {
					normalRealRewardList.add(model);
				} else if (model.getRewardWeightType() == ClubWelfareCode.REWARD_WEIGHT_TYPE_ASSIST) {
					assistRealRewardList.add(model);
				}
			} else if (model.getRewardType() == ClubWelfareCode.REWARD_TYPE_SHOW) {
				showRewardList.add(model);
			}
		}
		logger.info("加载字典ClubWelfareRewardDict" + timer.getStr());
		if (normalRealRewardList.size() <= 0) {
			logger.error("clubwelfare lottery normalRealReward not config,please check!!!");
		}
		if (assistRealRewardList.size() <= 0) {
			logger.error("clubwelfare lottery assistRealReward not config,please check!!!");
		}
		if (showRewardList.size() <= 0) {
			logger.error("clubwelfare lottery showRealReward not config,please check!!!");
		}
	}

	public List<ClubWelfareRewardModel> getShowRewardList() {
		return showRewardList;
	}

	public List<ClubWelfareRewardModel> getNormalRealRewardList() {
		return normalRealRewardList;
	}

	public List<ClubWelfareRewardModel> getAssistRealRewardList() {
		return assistRealRewardList;
	}
}

