/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.constant;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.LongFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.S2SCmd;
import com.cai.common.define.ELifecycle;
import com.cai.common.domain.ClubActivityModel;
import com.cai.common.util.Lifecycle;
import com.cai.common.util.NamedThreadFactory;
import com.cai.common.util.PBUtil;
import com.cai.common.util.TimeUtil;
import com.cai.config.ClubCfg;
import com.cai.service.ClubService;
import com.cai.service.SessionService;
import com.cai.utils.Utils;
import com.google.common.collect.Maps;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import protobuf.clazz.ClubMsgProto.ClubActivityProto;

/**
 * 
 * 活动包装
 * 
 * 
 * @author wu_hc date: 2018年01月22日 上午11:15:45 <br/>
 */
public final class ClubActivityWrap implements Lifecycle {

	/**
	 * 日志
	 */
	private static final Logger logger = LoggerFactory.getLogger(ClubActivityWrap.class);

	/**
	 * 七天
	 */
	public static final long SEVEN_DATE_MILLIS = (7 * 24 * 60 * 60 * 1000L);

	/**
	 * 活动调度器
	 */
	private static final Map<Long, ScheduledFuture<?>> activityTrigger = Maps.newConcurrentMap();

	/**
	 * 活动调度器
	 */
	private static final ScheduledExecutorService activitySchedule = Executors.newScheduledThreadPool(1,
			new NamedThreadFactory("club-activity-scheduled-thread"));

	/**
	 * 俱乐部id
	 */
	private final ClubActivityModel actModel;

	public ClubActivityWrap(ClubActivityModel actModel) {
		this.actModel = actModel;
		initTrigger();
	}

	@Override
	public void start() throws Exception {
		logger.info("--------俱乐部[{}]->活动[id:{},name:{}]开始 --------", actModel.getClubId(), actModel.getId(), actModel.getActivityName());
	}

	@Override
	public void stop() throws Exception {
		logger.info("--------俱乐部[{}]->活动[id:{},name:{}]结束 --------", actModel.getClubId(), actModel.getId(), actModel.getActivityName());
		Club club = ClubService.getInstance().getClub(getClubId());
		if (null != club) {
			entrustProxyBuildSnapshot();
			Utils.notifyActivityEvent(actModel.getCreatorId(), club, getId(), ClubActivityCode.END);
		}
		cancelSchule();
	}

	public void cancelSchule() {
		ScheduledFuture<?> futrue = activityTrigger.remove(getId());
		if (null != futrue) {
			futrue.cancel(false);
			logger.info("--------俱乐部[{}]->活动[id:{},name:{}]移除调度 --------", actModel.getClubId(), actModel.getId(), actModel.getActivityName());
		}
	}

	@Override
	public boolean isRunning() {
		return inActing();
	}

	public int getClubId() {
		return actModel.getClubId();
	}

	public long getId() {
		return actModel.getId();
	}

	public final ClubActivityModel model() {
		return actModel;
	}

	public long startMillis() {
		return actModel.getActivityStartDate().getTime();
	}

	public long endMillis() {
		return actModel.getActivityEndDate().getTime();
	}

	/**
	 * 在活动
	 * 
	 * @return
	 */
	public boolean inActing() {
		return isNotExpire(actModel.getActivityStartDate(), actModel.getActivityEndDate());
	}

	/**
	 * 活动触发调度器
	 */
	private void initTrigger() {

		long triggerTime = actModel.getActivityEndDate().getTime() - System.currentTimeMillis();
		if (triggerTime > 0) {
			activityTrigger.putIfAbsent(actModel.getId(), newTrigger().apply(triggerTime));
			logger.warn("--------俱乐部[{}]->活动[id:{},name:{}]加入调度中 --------", actModel.getClubId(), actModel.getId(), actModel.getActivityName());
		}
	}

	/**
	 * 生成调度器
	 * 
	 * @return
	 */
	private LongFunction<ScheduledFuture<?>> newTrigger() {
		return (delay) -> {
			return activitySchedule.schedule(() -> {
				try {
					this.stop();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}, delay, TimeUnit.MILLISECONDS);
		};
	}

	/**
	 * 委托代理服生成俱乐部活动快照
	 */
	public int entrustProxyBuildSnapshot() {

		long current = System.currentTimeMillis();
		if (inActing() || current > actModel.getActivityEndDate().getTime() + ClubActivityWrap.SEVEN_DATE_MILLIS) {
			return Club.FAIL;
		}
		Optional<C2SSession> proxy = SessionService.getInstance().randomProxy();
		// 是否考虑在当前进程内生成??? GAME-TODO
		if (!proxy.isPresent()) {
			logger.warn("--------俱乐部[{}]->活动[id:{},name:{}]结束 ,但没有活跃代理服帮忙生成活动快照！", actModel.getClubId(), actModel.getId(),
					actModel.getActivityName());
			return Club.FAIL;
		}

		proxy.get().send(PBUtil.toS2SResponse(S2SCmd.CLUB_ACTIVITY_SNAPSHOT_BUILD, toActivityBuilder()));
		return Club.SUCCESS;
	}

	/**
	 * 
	 * @return
	 */
	public final ClubActivityProto.Builder toActivityBuilder() {
		ClubActivityProto.Builder builder = ClubActivityProto.newBuilder();
		builder.setActivityId(actModel.getId());
		builder.setActivityName(actModel.getActivityName());
		builder.setActivityType(actModel.getActivityType());
		builder.setClubId(actModel.getClubId());
		builder.setCreatorId(actModel.getCreatorId());
		builder.setStartDate((int) (actModel.getActivityStartDate().getTime() / 1000L));
		builder.setEndDate((int) (actModel.getActivityEndDate().getTime() / 1000L));
		return builder;
	}

	/**
	 * 
	 * @param b
	 * @param e
	 * @return
	 */
	static final boolean isNotExpire(final Date b, final Date e) {
		// 时间无效，不可以使用
		if (null == b || null == e) {
			return false;
		}
		long c = System.currentTimeMillis();
		return c >= b.getTime() && c < e.getTime();
	}

	/**
	 * 
	 * @return
	 */
	public ELifecycle status() {
		long current = System.currentTimeMillis();
		if (current < actModel.getActivityStartDate().getTime()) {
			return ELifecycle.BEFORE;
		} else if (isRunning()) {
			return ELifecycle.ING;
		} else {
			return ELifecycle.AFTER;
		}
	}

	/**
	 * 
	 * @return
	 */
	public boolean show(long current) {
		return (current - endMillis()) <= ClubCfg.get().getShowHistoryTime() * TimeUtil.HOUR;
	}
}
