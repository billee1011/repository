/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.dictionary;

import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.GameRecommendIndexModel;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.google.common.collect.Maps;

/**
 * 
 *
 * @author wu date: 2017年8月23日 上午10:40:02 <br/>
 */
public class GameRecommendDict {
	private Logger logger = LoggerFactory.getLogger(GameRecommendDict.class);

	private Map<Integer, GameRecommendIndexModel> gameRecommendIndexModelDict;

	private static final GameRecommendDict INST = new GameRecommendDict();

	public static GameRecommendDict getInstance() {
		return INST;
	}

	@SuppressWarnings("unchecked")
	public void load() {
		// 放入redis缓存
		gameRecommendIndexModelDict = SpringService.getBean(RedisService.class).hGet(RedisConstant.DICT, RedisConstant.DIR_GAME_RECOMMEND, Map.class);
		if (null == gameRecommendIndexModelDict || gameRecommendIndexModelDict.isEmpty()) {
			logger.warn("app推荐位置的配置为空，请确认!!");
			return;
		}
	}

	public Map<Integer, GameRecommendIndexModel> getGameRecommendDict() {
		return null == gameRecommendIndexModelDict ? Maps.newHashMap() : Collections.unmodifiableMap(gameRecommendIndexModelDict);
	}
}
