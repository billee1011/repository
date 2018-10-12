package com.cai.constant;

import com.cai.common.domain.ClubMatchModel;
import com.cai.common.util.RoomComonUtil;
import com.cai.common.util.TimeUtil;
import com.cai.config.ClubCfg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 * 亲友圈自建赛定时赛
 *
 * @author zhanglong 2018/9/6 17:09
 */
public class ClubMatchTimeWrap extends ClubMatchWrap {

	/**
	 * 日志
	 */
	private static final Logger logger = LoggerFactory.getLogger(ClubMatchTimeWrap.class);

	public ClubMatchTimeWrap(ClubMatchModel model, Club club) {
		super(model, club);
	}

	@Override
	protected boolean checkCanStart() {
		boolean canStart = true;
		// 检查比赛人数
		int tablePlayerNum = RoomComonUtil.getMaxNumber(this.ruleModel.getRuleParams());
		if (enrollAccountIds.size() <= 0 || enrollAccountIds.size() < model.getMinPlayerCount() || (enrollAccountIds.size() % tablePlayerNum) != 0) {
			canStart = false;
		}
		if (!canStart) {
			return canStart;
		}
		return super.checkCanStart();
	}

	@Override
	protected void initTrigger() {
		long triggerTime = model.getStartDate().getTime() - System.currentTimeMillis();
		if (triggerTime > 0) {
			TriggerGroup triggerGroup = new TriggerGroup();
			triggerGroup.startTrigger = newTrigger(this::start, triggerTime);

			if (triggerTime > ClubCfg.get().getClubMatchWillStartMinute() * TimeUtil.MINUTE)
				triggerGroup.willStartTrigger = newTrigger(this::willStart,
						triggerTime - ClubCfg.get().getClubMatchWillStartMinute() * TimeUtil.MINUTE);

			triggerGroup.ensureStopTrigger = newTrigger(this::ensureStop, 2 * 60 * TimeUtil.MINUTE + triggerTime);

			trigger.put(model.getId(), triggerGroup);
			logger.warn("--------俱乐部[{}]->比赛[id:{},name:{}]【加入】调度 --------", model.getClubId(), model.getId(), model.getMatchName());
		} else {
			if (this.model.getStatus() == ClubMatchStatus.PRE.status) { // 创建状态的比赛因中途停服错过开赛的情况,作为开赛失败处理
				// 延迟5分钟处理,以防在起服时服务器间连接未建立好导致逻辑执行失败
				TriggerGroup triggerGroup = new TriggerGroup();
				triggerGroup.startTrigger = newTrigger(this::startMatchFail, 5 * TimeUtil.MINUTE);
				trigger.put(model.getId(), triggerGroup);
				logger.warn("俱乐部[{}]，比赛[ {} ] 错过开始时间，将会自动还豆处理 ，开赛失败！", club.getClubId(), id());
			} else if (this.model.getStatus() == ClubMatchStatus.ING.status) {
				TriggerGroup triggerGroup = trigger.get(model.getId());
				if (triggerGroup == null) {
					triggerGroup = new TriggerGroup();
				}
				triggerGroup.ensureStopTrigger = newTrigger(this::ensureStop, 2 * 60 * TimeUtil.MINUTE + triggerTime);
				trigger.put(model.getId(), triggerGroup);
				logger.warn("俱乐部[{}]，比赛[ {} ] 正在进行中，启服后加入确保比赛结束调度！", club.getClubId(), id());
			}
		}
	}
}
