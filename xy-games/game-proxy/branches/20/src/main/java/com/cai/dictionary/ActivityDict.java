package com.cai.dictionary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.ActivityModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

import javolution.util.FastMap;

/**
 * 活动字典
 *
 */
public class ActivityDict {

	private Logger logger = LoggerFactory.getLogger(ActivityDict.class);

	private FastMap<Integer, FastMap<Integer, ActivityModel>> activityDictionary;

	/**
	 * 单例
	 */
	private static ActivityDict instance;

	/**
	 * 私有构造
	 */
	private ActivityDict() {
		activityDictionary = new FastMap<Integer, FastMap<Integer, ActivityModel>>();
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
		PerformanceTimer timer = new PerformanceTimer();
		try {
			RedisService redisService = SpringService.getBean(RedisService.class);
			activityDictionary = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_ACTIVITY, FastMap.class);
		} catch (Exception e) {
			logger.error("error", e);
		}
		logger.info("redis缓存加载字典ActivityDict" + timer.getStr());
	}

	public FastMap<Integer, FastMap<Integer, ActivityModel>> getActivityDictionary() {
		return activityDictionary;
	}

}
