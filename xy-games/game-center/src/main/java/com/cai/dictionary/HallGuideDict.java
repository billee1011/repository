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
import com.cai.service.PublicService;

/**
 * 大厅指引图字典/子游戏印章、背景、标题资源
 *
 */
public class HallGuideDict {

	private Logger logger = LoggerFactory.getLogger(HallGuideDict.class);

	private List<HallGuideModel> hallGuideModelDictionary;				//大厅指引图

	private Map<Integer, GameResourceModel> gameResourceMap;			//游戏资源(子游戏印章、背景、标题资源)
	
	private Map<Integer, HallMainViewBackModel> hallMainViewBackMap;	//大厅主界面背景资源

	/**
	 * 单例
	 */
	private static HallGuideDict instance;

	/**
	 * 私有构造
	 */
	private HallGuideDict() {
		hallGuideModelDictionary = new ArrayList<>();
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
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<HallGuideModel> hallGuideModelList = publicService.getPublicDAO().getHallGuideModelList();
		List<HallGuideModel> list = new ArrayList<>();
		for (HallGuideModel model : hallGuideModelList) {
			list.add(model);
		}
		this.hallGuideModelDictionary = list;
		// 放入redis缓存
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_HALL_GUIDE, hallGuideModelDictionary);
		logger.info("load HallGuideDict success! " + timer.getStr());
	}

	public void loadResource() {
		PerformanceTimer timer = new PerformanceTimer();
		PublicService publicService = SpringService.getBean(PublicService.class);
		Map<Integer, GameResourceModel> gameResourceMap = new HashMap<>();
		List<GameResourceModel> gameResourceModelList = publicService.getPublicDAO().getGameResourceModelList();
		for (GameResourceModel model : gameResourceModelList) {
			gameResourceMap.put(model.getGame_id(), model);
		}
		this.gameResourceMap = gameResourceMap;
		// 放入redis缓存
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_GAME_RESOURCE, this.gameResourceMap);
		logger.info("load DICT_GAME_RESOURCE success! " + timer.getStr());
	}
	
	public void loadMainViewBack() {
		PerformanceTimer timer = new PerformanceTimer();
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<HallMainViewBackModel> hallMainViewBackList = publicService.getPublicDAO().getHallMainViewBackModelList();
		Map<Integer, HallMainViewBackModel> map = new HashMap<>();
		for (HallMainViewBackModel model : hallMainViewBackList) {
			map.put(model.getCity(), model);
		}
		this.hallMainViewBackMap = map;
		// 放入redis缓存
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_HALL_MAIN_VIEW_BACK, hallMainViewBackMap);
		logger.info("load DICT_HALL_MAIN_VIEW_BACK success! " + timer.getStr());
	}
}
