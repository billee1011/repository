package com.cai.dictionary;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.ActivityRedpacketPoolModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;

/**
 * 活动字典
 *
 */
public class ActivityRedpacketPoolDict {

	private Logger logger = LoggerFactory.getLogger(ActivityRedpacketPoolDict.class);

	/**
	 * 单例
	 */
	private static ActivityRedpacketPoolDict instance;

	private ActivityRedpacketPoolModel activityRedpacketPoolModel;
	/**
	 * 私有构造
	 */
	private ActivityRedpacketPoolDict() {
		activityRedpacketPoolModel = new ActivityRedpacketPoolModel();
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static ActivityRedpacketPoolDict getInstance() {
		if (null == instance) {
			instance = new ActivityRedpacketPoolDict();
		}

		return instance;
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<ActivityRedpacketPoolModel> list = publicService.getPublicDAO().getActivityRedpacketPoolModelList();
		for(ActivityRedpacketPoolModel model:list){
			activityRedpacketPoolModel = model;
			break;
		}
		//放入redis缓存
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hSet(RedisConstant.DICT, RedisConstant.REDPACKET_POOL_CLEAR, activityRedpacketPoolModel);
		logger.info("加载字典getActivityRedpacketPoolModel " + timer.getStr());
	}

}
