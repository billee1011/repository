package com.cai.dictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.GameResourceModel;
import com.cai.common.domain.HallGuideModel;
import com.cai.common.domain.HallMainViewBackModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

/**
 * 大厅指引字典
 *
 */
@SuppressWarnings("unchecked")
public class HallGuideDict {

	private Logger logger = LoggerFactory.getLogger(HallGuideDict.class);

	private List<HallGuideModel> hallGuideDictionary;					//大厅指引图
	private Map<Integer, GameResourceModel> gameResourceMap;			//游戏资源(子游戏印章、背景、标题资源)
	private Map<Integer, HallMainViewBackModel> hallMainViewBackMap;		//大厅主界面背景资源
	/**
	 * 单例
	 */
	private static HallGuideDict instance;

	/**
	 * 私有构造
	 */
	private HallGuideDict() {
		hallGuideDictionary = new ArrayList<HallGuideModel>();
		gameResourceMap = new HashMap<>();
		hallMainViewBackMap = new HashMap<>();
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static HallGuideDict getInstance() {
		if (null == instance) {
			instance = new HallGuideDict();
		}

		return instance;
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		try {
			RedisService redisService = SpringService.getBean(RedisService.class);
			hallGuideDictionary = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_HALL_GUIDE, ArrayList.class);
		} catch (Exception e) {
			logger.error("error", e);
		}
		logger.info("redis缓存加载字典hallGuideDictionary" + timer.getStr());
	}

	public void loadResource() {
		PerformanceTimer timer = new PerformanceTimer();
		try {
			RedisService redisService = SpringService.getBean(RedisService.class);
			gameResourceMap = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_GAME_RESOURCE, HashMap.class);
		} catch (Exception e) {
			logger.error("error", e);
		}
		logger.info("redis缓存加载字典gameResourceMap" + timer.getStr());
	}
	
	public void loadMainViewBack() {
		PerformanceTimer timer = new PerformanceTimer();
		try {
			RedisService redisService = SpringService.getBean(RedisService.class);
			hallMainViewBackMap = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_HALL_MAIN_VIEW_BACK, HashMap.class);
		} catch (Exception e) {
			logger.error("error", e);
		}
		logger.info("redis缓存加载字典hallMainViewBackMap" + timer.getStr());
	}

	public List<HallGuideModel> getHallGuideDictionary() {
		return hallGuideDictionary;
	}

	public Map<Integer, GameResourceModel> getGameResourceMap() {
		return gameResourceMap;
	}

	public GameResourceModel getResourceByAppId(int appId) {
		return gameResourceMap.get(appId);
	}

	public HallMainViewBackModel getHallMainViewBackModel(int cityCode) {
		if (hallMainViewBackMap.size() == 0 || hallMainViewBackMap.get(cityCode) == null) {
			return new HallMainViewBackModel();
		}
		return hallMainViewBackMap.get(cityCode);
	}

}
