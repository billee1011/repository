package com.cai.dictionary;

import java.util.List;
import java.util.Set;

import com.cai.common.constant.Symbol;
import com.cai.common.domain.ClubWelfareSwitchModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.constant.ClubWelfareWrap;
import com.cai.dao.ClubDao;
import com.cai.service.ClubDaoService;
import com.cai.service.ClubService;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/9/20 17:09
 */
public class ClubWelfareSwitchModelDict {

	private Logger logger = LoggerFactory.getLogger(ClubWelfareSwitchModelDict.class);

	private static ClubWelfareSwitchModelDict instance = new ClubWelfareSwitchModelDict();

	public static ClubWelfareSwitchModelDict getInstance() {
		return instance;
	}

	private ClubWelfareSwitchModel clubWelfareSwitchModel;

	public void load(boolean reloadSwitchStatus) {
		PerformanceTimer timer = new PerformanceTimer();
		ClubDao dao = SpringService.getBean(ClubDaoService.class).getDao();
		List<ClubWelfareSwitchModel> list = dao.getClubWelfareSwitchModel();
		if (list != null && list.size() > 0) {
			clubWelfareSwitchModel = list.get(0);
		} else {
			logger.error("club_welfare_switch 没有配置数据，请检查！！！");
		}
		logger.info("加载字典ClubWelfareSwitchModelDict" + timer.getStr());
		if (reloadSwitchStatus) { // 后台修改开关状态后需要检查亲友圈的是否开启或关闭
			updateClubWelfareSwtichStatus();

		}
	}

	public ClubWelfareSwitchModel getClubWelfareSwitchModel() {
		return clubWelfareSwitchModel;
	}

	/**
	 * 更新亲友圈福卡功能开关状态
	 */
	private void updateClubWelfareSwtichStatus() {
		if (clubWelfareSwitchModel == null) {
			return;
		}
		boolean totalSwitch = clubWelfareSwitchModel.getTotalSwitch() == 1;
		boolean isConditionOpen = clubWelfareSwitchModel.getIsConditionOpen() == 1;
		boolean isAppointOpen = clubWelfareSwitchModel.getIsAppointOpen() == 1;
		String appointClubIdsStr = clubWelfareSwitchModel.getAppointClubIds();
		boolean hasMemCountCond = clubWelfareSwitchModel.getHasMemCountCond() == 1;
		int memCount = clubWelfareSwitchModel.getMemCount();
		boolean hasGameCountCond = clubWelfareSwitchModel.getHasGameCountCond() == 1;
		int gameCount = clubWelfareSwitchModel.getGameCount();

		Set<Integer> appointClubs = Sets.newHashSet();
		if (!Strings.isNullOrEmpty(appointClubIdsStr)) {
			String[] arr = appointClubIdsStr.split(Symbol.COMMA);
			for (String str : arr) {
				try {
					appointClubs.add(Integer.parseInt(str));
				} catch (Exception e) {
					logger.error("appointClubIdsStr parse error,{}", str, e);
				}
			}
		}

		ClubService.getInstance().clubs.forEach((clubId, club) -> {
			club.runInReqLoop(() -> {
				ClubWelfareWrap wrap = club.clubWelfareWrap;
				if (!totalSwitch) { //总开关关闭，关闭现在已开启的亲友圈福卡功能
					if (wrap.isOpenClubWelfare()) {
						wrap.closeClubWelfare();
					}
				} else {
					if (isConditionOpen) {
						boolean needClose = false;
						if (hasMemCountCond) {
							if (club.getMemberCount() <= memCount) {
								needClose = true;
							}
						}
						if (hasGameCountCond) {
							if (club.clubModel.getGameCount() <= gameCount) {
								needClose = true;
							}
						}
						if (wrap.isOpenClubWelfare() && needClose) { //已经开启但是不满足新开启条件则关闭
							wrap.closeClubWelfare();
						} else if (!wrap.isOpenClubWelfare() && !needClose) {//未开启但是满足了新开启条件则开启
							wrap.openClubWelfare();
						}
					} else if (isAppointOpen) {
						if (!appointClubs.contains(clubId) && wrap.isOpenClubWelfare()) { //已经开启但新指定列表里没有的则关闭
							wrap.closeClubWelfare();
						} else if (appointClubs.contains(clubId) && !wrap.isOpenClubWelfare()) { //未开启但新指定列表里有的则开启
							wrap.openClubWelfare();
						}
					}
				}
			});
		});
	}

}
