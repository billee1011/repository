/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import com.cai.common.constant.RMICmd;
import com.cai.common.constant.Symbol;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.ExclusiveSettingStatus;
import com.cai.common.define.XYCode;
import com.cai.common.domain.AccountSimple;
import com.cai.common.domain.ClubExclusiveActivityModel;
import com.cai.common.domain.ClubExclusiveGoldModel;
import com.cai.common.domain.ClubExclusiveResultModel;
import com.cai.common.domain.Event;
import com.cai.common.domain.StatusModule;
import com.cai.common.rmi.vo.ClubExclusiveRMIVo;
import com.cai.common.util.Bits;
import com.cai.common.util.Pair;
import com.cai.common.util.SpringService;
import com.cai.core.MonitorEvent;
import com.cai.domain.Session;
import com.cai.util.ClubExclusiveLogUtil;
import com.cai.util.RMIMsgSender;
import com.google.common.collect.Lists;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

import protobuf.clazz.s2s.S2SProto.ExclusiveGoldPB;

/**
 * 放在中心服，防止特殊情况下需要重启俱乐部服
 *
 * @author wu_hc date: 2017年12月5日 下午4:18:09 <br/>
 */
public final class ClubExclusiveService extends AbstractService {

	/**
	 * 账号专属豆 accountid,gameid
	 */
	private final Map<Pair<Long, Integer>, ClubExclusiveGoldModel> exclusive = Maps.newConcurrentMap();

	/**
	 * 活动专属豆
	 */
	private volatile Map<Integer, ClubExclusiveActivityModel> activity = Maps.newConcurrentMap();

	/**
	 * 活动调度器
	 */
	private final Map<Integer, ScheduledFuture<?>> activityScheduled = Maps.newConcurrentMap();

	/**
	 * 最小活动开启时间，活动结束时间必须要大于当前时间30分钟
	 */
	protected static final long MINIMUM_ACTIVITY_TIME = 30 * 60 * 1000L;

	private static final ClubExclusiveService m = new ClubExclusiveService();

	/**
	 * 落地
	 */
	private ScheduledExecutorService dbService = Executors.newScheduledThreadPool(1);

	/**
	 * @return
	 */
	public static final ClubExclusiveService getInstance() {
		return m;
	}

	private ClubExclusiveService() {
	}

	@Override
	public void startService() {
		List<ClubExclusiveGoldModel> models = SpringService.getBean(PublicService.class).getPublicDAO().getClubExclusiveGoldModelList();
		models.forEach((model) -> {
			if (model.getUsedCount() < 0) {
				model.setUsedCount(0L);
			}
			exclusive.putIfAbsent(key(model.getAccountId(), model.getGameId()), model);
		});

		// 加载活动配置
		activityCfgReload(Boolean.FALSE);

		dbService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try {
					save();
				} catch (Exception e) {
					logger.error("俱乐部专属币服务落地失败!!!", e);
					e.printStackTrace();
				}
			}
		}, 5L, 5L, TimeUnit.MINUTES);
	}

	@Override
	public void stopService() {
		try {
			save();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @throws Exception
	 */
	private void save() throws Exception {
		List<ClubExclusiveGoldModel> exclusiveGoldModels = Lists.newArrayList();
		exclusive.forEach((k, model) -> {
			if (model.isNeedDB()) {
				exclusiveGoldModels.add(model);
				model.setNeedDB(false);
			}
		});
		if (exclusiveGoldModels.isEmpty()) {
			return;
		}
		SpringService.getBean(PublicService.class).batchUpdate("updateExclusiveGoldModel", exclusiveGoldModels);
	}

	/**
	 * @param accountId
	 * @param gameId
	 * @param gold
	 * @return
	 */
	public boolean check(long accountId, int gameId, long gold) {
		Optional<ClubExclusiveGoldModel> modelOpt = get(accountId, gameId);
		Optional<ClubExclusiveActivityModel> actOpt = getAct(gameId);

		AccountSimple lock = PublicServiceImpl.getInstance().getAccountSimpe(accountId);
		synchronized (lock) {
			if (actOpt.isPresent()) {
				ClubExclusiveActivityModel actModel = actOpt.get();

				// 如果有活动，但活动过期，直接不能用
				if (!inActivity(actModel)) {
					return false;
				}

				if (modelOpt.isPresent()) {
					ClubExclusiveGoldModel model = modelOpt.get();
					return (gold + model.getUsedCount()) <= (model.getExclusiveGold() + actModel.getExclusiveGold());
				} else {
					return gold <= actModel.getExclusiveGold();
				}
			} else {
				if (modelOpt.isPresent()) {
					ClubExclusiveGoldModel model = modelOpt.get();
					return model.getUsedCount() <= model.getExclusiveGold() && isNotExpire(model.getExclusiveBeginDate(),
							model.getExclusiveEndDate());
				}
			}
		}

		return false;
	}

	/**
	 * 扣款
	 *
	 * @param vo
	 * @return
	 */
	public ClubExclusiveResultModel cost(ClubExclusiveRMIVo vo) {

		AccountSimple lock = PublicServiceImpl.getInstance().getAccountSimpe(vo.getAccountId());
		synchronized (lock) {
			Optional<ClubExclusiveGoldModel> opt = get(vo.getAccountId(), vo.getGameId());
			Optional<ClubExclusiveActivityModel> actOpt = getAct(vo.getGameId());

			long costGold = vo.getValue();

			if (actOpt.isPresent()) {
				ClubExclusiveActivityModel actModel = actOpt.get();
				// 如果有活动，但活动过期，直接不能用
				if (!inActivity(actModel)) {
					return ClubExclusiveResultModel.newModel(XYCode.FAIL);
				}

				ClubExclusiveGoldModel model = null;
				if (opt.isPresent()) {
					model = opt.get();
				} else {
					model = create(vo.getAccountId(), vo.getGameId(), actModel.getActivityBeginDate(), actModel.getActivityEndDate(), 0L, 0);
				}

				long all = model.getExclusiveGold() + actModel.getExclusiveGold();
				long oldV = all - model.getUsedCount();
				if (costGold <= oldV) {
					model.setUsedCount(model.getUsedCount() + costGold);
					long newV = all - model.getUsedCount();
					return ClubExclusiveResultModel.newModel(XYCode.SUCCESS).setNewValue(Math.max(newV, 0L)).setOldValue(oldV)
							.setGameId(vo.getGameId()).setExclusiveEndDate(actModel.getActivityEndDate()).setAccountId(vo.getAccountId());
				}
			} else {
				if (opt.isPresent()) {
					ClubExclusiveGoldModel model = opt.get();
					long oldV = model.getExclusiveGold() - model.getUsedCount();
					if (costGold <= oldV && isNotExpire(model.getExclusiveBeginDate(), model.getExclusiveEndDate())) {
						model.setUsedCount(model.getUsedCount() + costGold);
						long newV = model.getExclusiveGold() - model.getUsedCount();
						return ClubExclusiveResultModel.newModel(XYCode.SUCCESS).setNewValue(Math.max(0L, newV)).setOldValue(oldV)
								.setGameId(vo.getGameId()).setExclusiveEndDate(model.getExclusiveEndDate()).setAccountId(vo.getAccountId());
					}
				}
			}
		}

		return ClubExclusiveResultModel.newModel(XYCode.FAIL);
	}

	/**
	 * 还款--GAME-TODO
	 *
	 * @param vo
	 * @return
	 */
	public ClubExclusiveResultModel repay(ClubExclusiveRMIVo vo) {
		AccountSimple lock = PublicServiceImpl.getInstance().getAccountSimpe(vo.getAccountId());
		synchronized (lock) {
			Optional<ClubExclusiveGoldModel> opt = get(vo.getAccountId(), vo.getGameId());
			if (opt.isPresent()) {
				ClubExclusiveGoldModel model = opt.get();

				long actExclusiveGold = activityExclusiveGold(vo.getGameId());
				long oldV = (model.getExclusiveGold() + actExclusiveGold) - model.getUsedCount();
				model.setUsedCount(Math.max(0L, model.getUsedCount() - vo.getValue())); // 防止在清0后还豆造成负值
				long newV = (model.getExclusiveGold() + actExclusiveGold) - model.getUsedCount();
				return ClubExclusiveResultModel.newModel(XYCode.SUCCESS).setGameId(vo.getGameId()).setNewValue(Math.max(newV, 0L)).setOldValue(oldV);
			}
		}

		return null;
	}

	/**
	 * 创建
	 *
	 * @param vo
	 * @return
	 */
	private ClubExclusiveGoldModel create(ClubExclusiveRMIVo vo) {
		int settings = vo.getSettings();
		return create(vo.getAccountId(), vo.getGameId(), vo.getExclusiveBeginDate(), vo.getExclusiveEndDate(), vo.getValue(), settings);
	}

	/**
	 * 创建 model
	 *
	 * @param accountId
	 * @param gameId
	 * @param b
	 * @param e
	 * @param exclusiveGold
	 * @return
	 */
	private synchronized ClubExclusiveGoldModel create(long accountId, int gameId, Date b, Date e, long exclusiveGold, int settings) {
		ClubExclusiveGoldModel model = new ClubExclusiveGoldModel();
		model.setAccountId(accountId);
		model.setGameId(gameId);
		model.setUsedCount(0L);
		model.setExclusiveBeginDate(b);
		model.setExclusiveEndDate(e);
		model.setExclusiveGold(exclusiveGold);
		model.setSettings(settings);
		Object o = exclusive.putIfAbsent(Pair.of(model.getAccountId(), model.getGameId()), model);
		if (null != o) {
			logger.warn("#### [{}] create exclusive model,but game[{}] has in cache yet!", accountId, gameId);
		}
		SpringService.getBean(PublicService.class).getPublicDAO().insertExclusiveGoldModel(model);

		return model;
	}

	/**
	 * 更新，只有从后台发放
	 *
	 * @param vo
	 * @return
	 */
	public ClubExclusiveResultModel update(ClubExclusiveRMIVo vo) {

		if (null != vo.getExclusiveEndDate() && vo.getExclusiveEndDate().getTime() <= System.currentTimeMillis() + 1000L) {
			return ClubExclusiveResultModel.newModel(XYCode.FAIL).setGameId(vo.getGameId()).setAccountId(vo.getAccountId()).setDesc("有效期不合理!");
		}

		AccountSimple lock = PublicServiceImpl.getInstance().getAccountSimpe(vo.getAccountId());
		if (null == lock) {
			return ClubExclusiveResultModel.newModel(XYCode.FAIL).setGameId(vo.getGameId()).setAccountId(vo.getAccountId()).setDesc("玩家不存在!");
		}

		synchronized (lock) {
			Optional<ClubExclusiveGoldModel> opt = get(vo.getAccountId(), vo.getGameId());

			long actExclusiveGold = activityExclusiveGold(vo.getGameId());
			Date b = vo.getExclusiveBeginDate(), e = vo.getExclusiveEndDate();
			Optional<ClubExclusiveActivityModel> actOpt = getAct(vo.getGameId());
			if (actOpt.isPresent()) {
				b = actOpt.get().getActivityBeginDate();
				e = actOpt.get().getActivityEndDate();
			}
			if (opt.isPresent()) {
				// 修改如果旧值小于零，从零开始计[跟活动中途下线相关]
				ClubExclusiveGoldModel model = opt.get();

				boolean isFromSSHE = false;
				long oldV = actExclusiveGold + model.getExclusiveGold() - model.getUsedCount();

				// 如果此次修改的是来自后台
				if (vo.getType() == EGoldOperateType.OSS_ADD_EXCLUSIVE_GOLD || vo.getType() == EGoldOperateType.OSS_DESC_EXCLUSIVE_GOLD) {
					model.setSettings(vo.getSettings());
					isFromSSHE = true;
				} else {
					// 如果有设置，并且专属豆没有过期---[送豆/转豆]
					if (model.getSettings() > 0 && oldV > 0 && (model.getExclusiveEndDate().getTime() > System.currentTimeMillis())) {
						StatusModule curModel = StatusModule.newWithStatus(model.getSettings());
						if (curModel.statusOR(ExclusiveSettingStatus.NOT_OFFER, ExclusiveSettingStatus.NOT_SELL)) {
							return ClubExclusiveResultModel.newModel(XYCode.FAIL).setGameId(vo.getGameId()).setAccountId(vo.getAccountId())
									.setDesc("玩家当前专属豆不可以赠送,可以转让!");
						}
					}
				}

				//是否过期，插入过期日志
				boolean isExpire = model.getExclusiveEndDate().getTime() < System.currentTimeMillis() && actExclusiveGold == 0L;
				if (isExpire) {

					//过期日志
					ClubExclusiveRMIVo copy = ClubExclusiveLogUtil.clone(vo, EGoldOperateType.EXCLUSIVE_GOLD_EXPIRE);
					copy.setDesc("专属豆过期，");
					copy.setValue(oldV);
					ClubExclusiveLogUtil.exclusiveLog(copy, ClubExclusiveResultModel.newModel(1).setOldValue(oldV).setNewValue(0), false);
				}
				if (oldV <= 0 || isExpire) {
					oldV = 0;

					model.setExclusiveGold(Math.max(0L, vo.getValue()));
					model.setUsedCount(0L);

					//来自后台的在前面已经设置过了
					if (!isFromSSHE) {
						model.setSettings(0);
					}

				} else {

					model.setExclusiveGold(Math.max(0L, vo.getValue() + model.getExclusiveGold()));
				}

				long newV = actExclusiveGold + model.getExclusiveGold() - model.getUsedCount();
				if (null != vo.getExclusiveBeginDate()) {
					model.setExclusiveBeginDate(vo.getExclusiveBeginDate());
				}
				if (null != vo.getExclusiveEndDate()) {
					model.setExclusiveEndDate(vo.getExclusiveEndDate());
				}
				return ClubExclusiveResultModel.newModel(XYCode.SUCCESS).setGameId(vo.getGameId()).setNewValue(Math.max(0L, newV))
						.setOldValue(Math.max(0L, oldV)).setAccountId(vo.getAccountId()).setExclusiveBeginDate(b).setExclusiveEndDate(e);

			} else {
				ClubExclusiveGoldModel model = create(vo);
				return ClubExclusiveResultModel.newModel(XYCode.SUCCESS).setGameId(vo.getGameId())
						.setNewValue(model.getExclusiveGold() + actExclusiveGold).setOldValue(0L).setAccountId(vo.getAccountId())
						.setExclusiveBeginDate(b).setExclusiveEndDate(e);
			}
		}

	}

	/**
	 * 账号专属豆
	 *
	 * @param accountId
	 * @return
	 */
	public List<ExclusiveGoldPB> accountExclusiveGold(long accountId) {
		List<ExclusiveGoldPB> r = Lists.newArrayList();

		final Set<Pair<Long, Integer>> set = Sets.newHashSet();
		activity.forEach((gameId, actModel) -> {
			set.add(Pair.of(accountId, gameId));
		});
		set.addAll(exclusive.keySet());
		set.forEach((k) -> {
			final Pair<Long, Integer> pair = k;
			if (pair.getFirst().longValue() == accountId) {
				r.add(accountExclusiveGold(accountId, pair.getSecond()));
			}
		});
		return r;
	}

	/**
	 * @param accountId
	 * @return
	 */
	public ExclusiveGoldPB accountExclusiveGold(long accountId, int gameId) {

		Optional<ClubExclusiveGoldModel> opt = get(accountId, gameId);
		Optional<ClubExclusiveActivityModel> actOpt = getAct(gameId);
		if (actOpt.isPresent()) {

			ClubExclusiveActivityModel actModel = actOpt.get();

			int expire = (int) (actModel.getActivityEndDate().getTime() / 1000L);

			// 如果有活动，但活动过期，直接不能用
			if (!inActivity(actModel)) {
				return ExclusiveGoldPB.newBuilder().setGameId(gameId).setValue(0L).setExpireE(expire).build();
			}

			if (opt.isPresent()) {
				ClubExclusiveGoldModel model = opt.get();
				long n = (actModel.getExclusiveGold() + model.getExclusiveGold()) - model.getUsedCount();
				return ExclusiveGoldPB.newBuilder().setGameId(gameId).setValue(Math.max(n, 0L)).setExpireE(expire).setSettings(model.getSettings())
						.build();
			} else {
				return ExclusiveGoldPB.newBuilder().setGameId(gameId).setValue(actModel.getExclusiveGold()).setExpireE(expire).build();
			}
		} else {
			if (opt.isPresent()) {
				ClubExclusiveGoldModel model = opt.get();
				int expire = (int) (model.getExclusiveEndDate().getTime() / 1000L);
				long v = model.getExclusiveGold() - model.getUsedCount();
				return ExclusiveGoldPB.newBuilder().setGameId(gameId).setValue(Math.max(v, 0L)).setExpireE(expire).setSettings(model.getSettings())
						.build();
			} else {
				return ExclusiveGoldPB.newBuilder().setGameId(gameId).setValue(0L).build();
			}
		}

	}

	/**
	 * 专属豆活动配置重载
	 */
	public synchronized void activityCfgReload(boolean notify) {
		List<ClubExclusiveActivityModel> models = SpringService.getBean(PublicService.class).getPublicDAO().getClubExclusiveActivityModelList();

		Map<Integer, ClubExclusiveActivityModel> act = Maps.newConcurrentMap();
		models.forEach((model) -> {
			if (model.getStatus() == Bits.byte_1) {
				act.put(model.getGameId(), model);
			}
		});

		if (notify) {
			MapDifference<Integer, ClubExclusiveActivityModel> diff = Maps.difference(act, activity);
			List<Integer> diffAct = Lists.newArrayList();
			diff.entriesInCommon().forEach((gameId, m) -> {
				diffAct.add(gameId);
			});
			RMIMsgSender.callClub(RMICmd.CLUB_EXCLUSIVE_ACTIVITY_UPDATE, diffAct);
		}
		// 交换引用
		activity = act;

		rebuildScheduled();
	}

	/**
	 * 重建活动调度器
	 */
	private void rebuildScheduled() {
		// 清除活动调度器
		activityScheduled.forEach((game, future) -> {
			future.cancel(false);
		});

		BiFunction<Integer, Long, ScheduledFuture<?>> future = (game_id, delay) -> {
			return dbService.schedule(() -> {
				logger.warn("--------俱乐部->子游戏[{}]专属豆活动结束 --------", game_id);

				exclusive.forEach((pair, exclusiveModel) -> {
					if (pair.getSecond().intValue() == game_id) {
						exclusiveModel.setExclusiveGold(0L);
						exclusiveModel.setUsedCount(0L);
						RMIMsgSender.callClub(RMICmd.CLUB_EXCLUSIVE_ACTIVITY_UPDATE, Arrays.asList(game_id));
					}
				});
			}, delay, TimeUnit.MILLISECONDS);
		};

		activityScheduled.clear();
		long current = System.currentTimeMillis();
		activity.forEach((game, actModel) -> {
			if (actModel.getActivityEndDate().getTime() > (current /* + MINIMUM_ACTIVITY_TIME */)) {
				long delay = actModel.getActivityEndDate().getTime() - current;
				activityScheduled.put(game, future.apply(game, delay));
			}
		});
	}

	/**
	 * 该游戏在活动中
	 *
	 * @param model
	 * @return
	 */
	private boolean inActivity(ClubExclusiveActivityModel model) {
		return null != model && isNotExpire(model.getActivityBeginDate(), model.getActivityEndDate());
	}

	/**
	 * 活动可以用的专属豆上限
	 *
	 * @param gameId
	 * @return
	 */
	public long activityExclusiveGold(int gameId) {
		Optional<ClubExclusiveActivityModel> opt = getAct(gameId);
		if (opt.isPresent() && isNotExpire(opt.get().getActivityBeginDate(), opt.get().getActivityEndDate())) {
			return opt.get().getExclusiveGold();
		}
		return 0L;
	}

	/**
	 * @param key
	 * @return
	 */
	final Pair<Long, Integer> splitKey(String key) {
		String[] tmp = key.split(Symbol.COLON);
		return Pair.of(Longs.tryParse(tmp[0]), Ints.tryParse(tmp[1]));
	}

	public Optional<ClubExclusiveGoldModel> get(long accountId, int gameId) {
		return Optional.ofNullable(exclusive.get(key(accountId, gameId)));
	}

	final Optional<ClubExclusiveActivityModel> getAct(int gameId) {
		return Optional.ofNullable(activity.get(gameId));
	}

	final static Pair<Long, Integer> key(long accountId, int gameId) {
		return Pair.of(accountId, gameId);
	}

	final static boolean isNotExpire(final Date b, final Date e) {
		// 时间无效，不可以使用
		if (null == b || null == e) {
			return false;
		}
		long c = System.currentTimeMillis();
		return c >= b.getTime() && c < e.getTime();
	}

	@Override
	public MonitorEvent montior() {
		return null;
	}

	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {

	}

	@Override
	public void sessionCreate(Session session) {

	}

	@Override
	public void sessionFree(Session session) {

	}

	@Override
	public void dbUpdate(int _userID) {
	}
}
