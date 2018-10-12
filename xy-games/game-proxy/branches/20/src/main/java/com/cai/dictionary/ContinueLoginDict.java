package com.cai.dictionary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.ContinueLoginModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

import javolution.util.FastMap;

/**
 * 连续登录
 *
 */
public class ContinueLoginDict {

	private Logger logger = LoggerFactory.getLogger(ContinueLoginDict.class);

	private FastMap<Integer, FastMap<Integer, ContinueLoginModel>> continueLoginDictionary;

	/**
	 * 单例
	 */
	private static ContinueLoginDict instance;

	/**
	 * 私有构造
	 */
	private ContinueLoginDict() {
		continueLoginDictionary = new FastMap<Integer, FastMap<Integer, ContinueLoginModel>>();
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static ContinueLoginDict getInstance() {
		if (null == instance) {
			instance = new ContinueLoginDict();
		}

		return instance;
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		try {
			RedisService redisService = SpringService.getBean(RedisService.class);
			continueLoginDictionary = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_CONTINUE_LOGIN, FastMap.class);
		} catch (Exception e) {
			logger.error("error", e);
		}
		logger.info("redis缓存加载字典continueLoginDict" + timer.getStr());
	}

	public FastMap<Integer, FastMap<Integer, ContinueLoginModel>> getContinueLoginDictionary() {
		return continueLoginDictionary;
	}

}
