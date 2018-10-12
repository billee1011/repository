package com.cai.dictionary;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.ContinueLoginModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;

import javolution.util.FastMap;

/**
 * 连续登录配置
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
		continueLoginDictionary.clear();
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<ContinueLoginModel> continueLoginModelList = publicService.getPublicDAO().getContinueLoginModelList();
		for (ContinueLoginModel model : continueLoginModelList) {
			if (!continueLoginDictionary.containsKey(model.getGame_id())) {
				FastMap<Integer, ContinueLoginModel> map = new FastMap<Integer, ContinueLoginModel>();
				continueLoginDictionary.put(model.getGame_id(), map);
			}
			continueLoginDictionary.get(model.getGame_id()).put(model.getId(), model);
		}

		// 放入redis缓存
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_CONTINUE_LOGIN, continueLoginDictionary);

		logger.info("加载字典ContinueLoginDict,count=" + continueLoginModelList.size() + timer.getStr());
	}

	public FastMap<Integer, FastMap<Integer, ContinueLoginModel>> getContinueLoginDictionary() {
		return continueLoginDictionary;
	}

}
