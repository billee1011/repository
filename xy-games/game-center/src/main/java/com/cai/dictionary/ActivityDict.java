package com.cai.dictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.ActivityMissionModel;
import com.cai.common.domain.ActivityModel;
import com.cai.common.domain.activity.ActivityDaysMission;
import com.cai.common.domain.activity.ActivityDaysMissionJson;
import com.cai.common.domain.activity.ActivityDaysRandomReward;
import com.cai.common.domain.activity.ActivityDaysReward;
import com.cai.common.domain.activity.ActivityEveryDayMission;
import com.cai.common.domain.activity.ActivityMission;
import com.cai.common.domain.activity.ActivityMissionGroupModel;
import com.cai.common.domain.activity.ActivityMissionRely;
import com.cai.common.domain.activity.ActivityTypeEnum;
import com.cai.common.domain.activity.MainPrizes;
import com.cai.common.domain.activity.NewActivityModel;
import com.cai.common.domain.activity.NewActivityPrizeModel;
import com.cai.common.domain.activity.RandomPrizes;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;
import com.google.common.collect.Maps;

import javolution.util.FastMap;

/**
 * 活动字典
 *
 */
public class ActivityDict {

	private Logger logger = LoggerFactory.getLogger(ActivityDict.class);

	private FastMap<Integer, FastMap<Integer, ActivityModel>> activityDictionary;
	private Map<Integer, NewActivityModel> newActivityDictionary;
	private Map<Integer, ActivityMissionModel> activityMissionDictionary;
	private Map<Integer, ActivityMissionGroupModel> activityMissionGroupMap;
	private Map<Integer, ActivityDaysMission> activityDaysMissionMap;

	/**
	 * 单例
	 */
	private static ActivityDict instance;

	/**
	 * 私有构造
	 */
	private ActivityDict() {
		activityDictionary = new FastMap<Integer, FastMap<Integer, ActivityModel>>();
		newActivityDictionary = Maps.newConcurrentMap();
		activityMissionDictionary = new HashMap<>();
		activityMissionGroupMap = Maps.newConcurrentMap();
		activityDaysMissionMap = new HashMap<>();
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static ActivityDict getInstance() {
		if (null == instance) {
			instance = new ActivityDict();
		}

		return instance;
	}

	public void load() {
		activityDictionary.clear();
		newActivityDictionary.clear();
		activityMissionDictionary.clear();
		activityMissionGroupMap.clear();
		activityDaysMissionMap.clear();
		RedisService redisService = SpringService.getBean(RedisService.class);
		PerformanceTimer timer = new PerformanceTimer();
		PublicService publicService = SpringService.getBean(PublicService.class);
		// 加载活动任务
		List<ActivityMissionModel> missionList = publicService.getPublicDAO().getActivityMissionModelList();
		for (ActivityMissionModel miModel : missionList) {
			if (StringUtils.isNotEmpty(miModel.getMission_type_rely())) {
				miModel.setMissionRely(JSONObject.parseObject(miModel.getMission_type_rely(), ActivityMissionRely.class));
			}
			activityMissionDictionary.put(miModel.getId(), miModel);
		}
		redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_ACTIVITY_MISSION, activityMissionDictionary);
		List<ActivityDaysMission> daysmissionList = publicService.getPublicDAO().getActivityDaysMissionList();
		for (ActivityDaysMission dm : daysmissionList) {
			if (StringUtils.isNotEmpty(dm.getMission_ids())) {
				String[] ids = dm.getMission_ids().split(",");
				for (String id : ids) {
					dm.getMissionList().add(Integer.parseInt(id));
				}
			}
			activityDaysMissionMap.put(dm.getId(), dm);
		}
		// 加载活动任务组
		List<ActivityMissionGroupModel> missionGroupModelList = publicService.getPublicDAO().getActivityMissionGroupModelList();
		for (ActivityMissionGroupModel activityMissionGroupModel : missionGroupModelList) {
			// 任务ID用list处理
			if (StringUtils.isNotEmpty(activityMissionGroupModel.getMission_ids())) {
				String[] ids = activityMissionGroupModel.getMission_ids().split(",");
				for (String id : ids) {
					activityMissionGroupModel.getMissionList().add(Integer.parseInt(id));
				}
			}
			this.activityMissionGroupMap.put(activityMissionGroupModel.getId(), activityMissionGroupModel);
		}
		redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_ACTIVITY_MISSION_GROUP, activityMissionGroupMap);

		// 活动
		List<ActivityModel> activityModelList = publicService.getPublicDAO().getActivityModelList();
		for (ActivityModel model : activityModelList) {
			if (model.getState() == 0) {
				continue;
			}
			String[] gameIdArray = model.getGame_id().split(",");
			for (String gameIdStr : gameIdArray) {
				int gameId = Integer.parseInt(gameIdStr);
				if (!activityDictionary.containsKey(gameId)) {
					FastMap<Integer, ActivityModel> map = new FastMap<Integer, ActivityModel>();
					activityDictionary.put(gameId, map);
				}
				activityDictionary.get(gameId).put(model.getId(), model);
			}
			NewActivityModel newModel = new NewActivityModel();
			activityModelTurns(model, newModel);
			newActivityDictionary.put(model.getId(), newModel);
		}

		// 查询活动奖品随机抽样项
		List<NewActivityPrizeModel> prizeList = publicService.getPublicDAO().getActivityPrizeModelList();
		for (NewActivityModel newActivityModel : newActivityDictionary.values()) {
			Map<Integer, List<NewActivityPrizeModel>> activityPrizeMap = newActivityModel.getActivityPrizeMap();
			for (NewActivityPrizeModel temp : prizeList) {
				if (temp.getActive_id() == newActivityModel.getId()) {
					List<NewActivityPrizeModel> dayPrizeList = activityPrizeMap.get(temp.getDay());
					if (null == dayPrizeList) {
						dayPrizeList = new ArrayList<>();
						activityPrizeMap.put(temp.getDay(), dayPrizeList);
					}
					dayPrizeList.add(temp);
				}
			}
		}

		// 放入redis缓存
		redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_ACTIVITY, activityDictionary);
		redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_ACTIVITY_NEW, newActivityDictionary);

		logger.info("加载字典ActivityDict,count=" + activityModelList.size() + timer.getStr());
	}

	public void activityModelTurns(ActivityModel model, NewActivityModel newModel) {
		newModel.setId(model.getId());
		newModel.setActivity_end_time(model.getActivity_end_time());
		newModel.setActivity_object(model.getActivity_object());
		newModel.setActivity_start_time(model.getActivity_start_time());
		newModel.setContent(model.getContent());
		newModel.setGame_id(model.getGame_id());
		newModel.setHref(model.getHref());
		newModel.setName(model.getName());
		newModel.setPlayer_limit_days(model.getPlayer_limit_days());
		newModel.setPrizes_count(model.getPrizes_count());
		newModel.setType(model.getType());
		newModel.setRemark(model.getRemark());
		newModel.setActivity_condition(model.getActivity_condition());
		newModel.setReset_time(model.getReset_time());
		newModel.setState(model.getState());
		newModel.setReload_state(model.getReload_state());
		newModel.setReset_prizes(model.getReset_prizes());
		newModel.setActivity_start_type(model.getActivity_start_type());
		newModel.setStart_avaliable_time(model.getStart_avaliable_time());
		newModel.setReward_random_type(model.getReward_random_type());
		newModel.setShare_icon(model.getShare_icon());
		newModel.setDisplay_target(model.getDisplay_target());
		newModel.setShare_mission_images(model.getShare_mission_images());
		newModel.setInclude_erweima(model.getInclude_erweima());
		if (StringUtils.isNotBlank(model.getMain_prizes())) {
			List<MainPrizes> list = JSON.parseArray(model.getMain_prizes(), MainPrizes.class);
			newModel.setMainPrizesList(list);
		}
		if (StringUtils.isNotBlank(model.getRandom_prizes())) {
			if (model.getType() != ActivityTypeEnum.NEW_USER_ACTIVITY.getId()) {
				List<RandomPrizes> list = JSON.parseArray(model.getRandom_prizes(), RandomPrizes.class);
				newModel.setRandomPrizesList(list);
			}
		}
		if (StringUtils.isNotEmpty(model.getActivity_mission())) {
			if (model.getActivity_mission().startsWith("[")) {
				if (model.getType() == ActivityTypeEnum.NEW_USER_ACTIVITY.getId()) {
					List<ActivityDaysMissionJson> activityDaysMissions = JSON.parseArray(model.getActivity_mission(), ActivityDaysMissionJson.class);
					Map<Integer, ActivityEveryDayMission> mMap = new HashMap<>();
					for (ActivityDaysMissionJson temp : activityDaysMissions) {
						int dmId = Integer.parseInt(temp.getDayMissionId());
						ActivityDaysMission activityDaysMission = activityDaysMissionMap.get(dmId);
						if (activityDaysMission != null) {
							ActivityEveryDayMission everyDayMission = new ActivityEveryDayMission();
							everyDayMission.setDay_index(activityDaysMission.getDay_index());
							// everyDayMission.setTaskName(activityDaysMission.getRemark());//设置任务组名字，两种方式
							everyDayMission.setTaskName(temp.getTaskName());
							List<ActivityMissionModel> dayMissions = new ArrayList<>();
							everyDayMission.setMissionIds(activityDaysMission.getMissionList());
							for (int missionId : activityDaysMission.getMissionList()) {
								ActivityMissionModel m = activityMissionDictionary.get(missionId);
								if (m != null) {
									dayMissions.add(m);
								}

							}
							everyDayMission.setDayMissions(dayMissions);
							mMap.put(activityDaysMission.getDay_index(), everyDayMission);
						}
					}
					newModel.setEverydayMissions(mMap);
				} else {
					List<ActivityMission> activityMissions = JSON.parseArray(model.getActivity_mission(), ActivityMission.class);
					List<ActivityMissionModel> list = new ArrayList<>();
					for (ActivityMission temp : activityMissions) {
						if (temp.getActivityMissionGroup() > 0) {
							ActivityMissionGroupModel activityMissionGroupModel = this.activityMissionGroupMap.get(temp.getActivityMissionGroup());
							if (null != activityMissionGroupModel) {
								newModel.getMissionGroupList().add(this.activityMissionGroupMap.get(temp.getActivityMissionGroup()));
								for (int missionId : activityMissionGroupModel.getMissionList()) {
									ActivityMissionModel m = activityMissionDictionary.get(missionId);
									if (m != null) {
										list.add(m);
									}
								}
							}

						}
					}
					newModel.setActivityMissionList(list);
				}
				// 以JSON数组开始，为新的任务类型，可以拓展任务组

			} else {
				// 旧的任务类型，只处理单个任务对象
				ActivityMission activityMission = JSON.parseObject(model.getActivity_mission(), ActivityMission.class);
				// newModel.setActivityMission(activityMission);
				List<ActivityMissionModel> list = new ArrayList<>();
				String miIds = activityMission.getActivityMissionIds();
				if (StringUtils.isNotEmpty(miIds)) {
					String[] ids = miIds.split(",");
					for (String id : ids) {
						ActivityMissionModel m = activityMissionDictionary.get(Integer.parseInt(id));
						if (m != null) {
							list.add(m);
						}
					}
				}
				newModel.setActivityMissionList(list);
			}
		}
		// 加载活动任务组奖励，某些任务组奖励会按天区分，需要做处理
		PublicService publicService = SpringService.getBean(PublicService.class);
		if (model.getType() == ActivityTypeEnum.NEW_USER_ACTIVITY.getId() && StringUtils.isNotBlank(model.getRandom_prizes())) {
			List<ActivityDaysRandomReward> activityDailyRewardList = JSON.parseArray(model.getRandom_prizes(), ActivityDaysRandomReward.class);
			Map<Integer, ActivityDaysReward> dailyRewardMap = new HashMap<>();
			for (ActivityDaysRandomReward dailyReward : activityDailyRewardList) {
				ActivityDaysReward reward = new ActivityDaysReward();
				reward.setDayIndex(dailyReward.getDayIndex());
				reward.setIcon(dailyReward.getIcon());
				reward.setRewardRemark(dailyReward.getRewardRemark());
				dailyRewardMap.put(dailyReward.getDayIndex(), reward);
			}
			List<NewActivityPrizeModel> daysMissionPrizeList = publicService.getPublicDAO().getEveryDayMissionPrizeModelList(newModel.getId());
			for (NewActivityPrizeModel temp : daysMissionPrizeList) {
				ActivityDaysReward activityDaysReward = newModel.getDailyMissionPrizeMap().get(temp.getDay());
				if (null == activityDaysReward) {
					activityDaysReward = dailyRewardMap.get(temp.getDay());
					if (activityDaysReward != null) {
						newModel.getDailyMissionPrizeMap().put(temp.getDay(), activityDaysReward);
					}
				}
				activityDaysReward.getRewardList().add(temp);
			}
		} else {
			List<NewActivityPrizeModel> missionGroupPrizeList = publicService.getPublicDAO().getActivityMissionGroupPrizeModelList(newModel.getId());
			for (NewActivityPrizeModel temp : missionGroupPrizeList) {
				Map<Integer, List<NewActivityPrizeModel>> groupDailyPrizeMap = newModel.getMissionGroupPrizeModel().get(temp.getActive_group());
				if (null == groupDailyPrizeMap) {
					groupDailyPrizeMap = new HashMap<Integer, List<NewActivityPrizeModel>>();
					newModel.getMissionGroupPrizeModel().put(temp.getActive_group(), groupDailyPrizeMap);
				}
				List<NewActivityPrizeModel> dayGroupPrizeList = groupDailyPrizeMap.get(temp.getDay());
				if (null == dayGroupPrizeList) {
					dayGroupPrizeList = new ArrayList<>();
					groupDailyPrizeMap.put(temp.getDay(), dayGroupPrizeList);
				}
				dayGroupPrizeList.add(temp);
			}
		}

	}

	public FastMap<Integer, FastMap<Integer, ActivityModel>> getActivityDictionary() {
		return activityDictionary;
	}

}
