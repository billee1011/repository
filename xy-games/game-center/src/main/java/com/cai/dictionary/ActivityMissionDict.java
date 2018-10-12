package com.cai.dictionary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.ActivityMissionModel;
import com.cai.common.domain.activity.ActivityMissionRely;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;

/**
 * 活动任务
 * @author chansonyan
 * 2018年5月18日
 */
public class ActivityMissionDict {

	private Logger logger = LoggerFactory.getLogger(ActivityMissionDict.class);
	
	private Map<Integer,ActivityMissionModel> activityMissionDictionary ;
	

	/**
	 * 单例
	 */
	private static ActivityMissionDict instance;

	/**
	 * 私有构造
	 */
	private ActivityMissionDict() {
		activityMissionDictionary = new HashMap<>();
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static ActivityMissionDict getInstance() {
		if (null == instance) {
			instance = new ActivityMissionDict();
		}

		return instance;
	}

	public void load() {
		RedisService redisService = SpringService.getBean(RedisService.class);
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<ActivityMissionModel> missionList = publicService.getPublicDAO().getActivityMissionModelList();
		for(ActivityMissionModel miModel:missionList){
			if(StringUtils.isNotEmpty(miModel.getMission_type_rely())) {
				miModel.setMissionRely(JSONObject.parseObject(miModel.getMission_type_rely(), ActivityMissionRely.class));
			}
			activityMissionDictionary.put(miModel.getId(), miModel);
		}
		redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_ACTIVITY_MISSION, activityMissionDictionary);
		
		logger.info("加载字典ActivityMissionDict,count=" + activityMissionDictionary.size());
	}

}
