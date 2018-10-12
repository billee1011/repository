package com.cai.constant;

import com.cai.common.domain.ClubMatchModel;
import com.cai.common.util.TimeUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 * 亲友圈自建赛满人赛
 *
 * @author zhanglong 2018/9/6 17:10
 */
public class ClubMatchCountWrap extends ClubMatchWrap {

	/**
	 * 日志
	 */
	private static final Logger logger = LoggerFactory.getLogger(ClubMatchCountWrap.class);

	public ClubMatchCountWrap(ClubMatchModel model, Club club) {
		super(model, club);
	}

	@Override
	protected void initTrigger() {
		if (this.model.getStatus() == ClubMatchStatus.ING.status) {
			TriggerGroup triggerGroup = trigger.get(model.getId());
			if (triggerGroup == null) {
				triggerGroup = new TriggerGroup();
			}
			triggerGroup.ensureStopTrigger = newTrigger(this::ensureStop, 2 * 60 * TimeUtil.MINUTE);
			trigger.put(model.getId(), triggerGroup);
			logger.warn("俱乐部[{}]，比赛[ {} ] 正在进行中，启服后加入确保比赛结束调度！", club.getClubId(), id());
		}
	}

	@Override
	void startMatch() {
		TriggerGroup triggerGroup = trigger.get(model.getId());
		if (triggerGroup == null) {
			triggerGroup = new TriggerGroup();
		}
		triggerGroup.ensureStopTrigger = newTrigger(this::ensureStop, 2 * 60 * TimeUtil.MINUTE);
		trigger.put(model.getId(), triggerGroup);
		logger.warn("俱乐部[{}]，比赛[ {} ] 为满人赛，开赛时加入确保比赛结束调度！", club.getClubId(), id());
		super.startMatch();
	}
}
