/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.dictionary;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.GameRecommendIndexModel;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;

/**
 * 
 *
 * @author wu date: 2017年8月23日 上午10:40:02 <br/>
 */
public class GameRecommendDict {
	private Logger logger = LoggerFactory.getLogger(GameRecommendDict.class);

	private static final GameRecommendDict INST = new GameRecommendDict();

	public static GameRecommendDict getInstance() {
		return INST;
	}

	public void load() {

		List<GameRecommendIndexModel> gameRecommendModelList = SpringService.getBean(PublicService.class).getPublicDAO().getGameRecommendModelList();
		if (null == gameRecommendModelList || gameRecommendModelList.isEmpty()) {
			logger.warn("app推荐位置的配置为空，请确认!!");
			return;
		}

		Map<Integer, GameRecommendIndexModel> gameRecommendIndexModelDict = gameRecommendModelList.stream()
				.collect(Collectors.toMap(GameRecommendIndexModel::getAppId, GameRecommendIndexModel -> GameRecommendIndexModel));
		// 放入redis缓存
		SpringService.getBean(RedisService.class).hSet(RedisConstant.DICT, RedisConstant.DIR_GAME_RECOMMEND, gameRecommendIndexModelDict);
		logger.info("加载字典GameRecommendDict成功！");
	}
}
