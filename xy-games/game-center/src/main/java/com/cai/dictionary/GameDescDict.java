package com.cai.dictionary;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.GameDescModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;

import javolution.util.FastMap;

/**
 * 游戏玩法说明字典
 * @author run
 *
 */
public class GameDescDict {

	private Logger logger = LoggerFactory.getLogger(GameDescDict.class);
	
	/**
	 * 系统参数缓存 game_id(id,model)
	 */
	private FastMap<Integer,FastMap<Integer,GameDescModel>> gameDescModelDictionary;

	/**
	 * 单例
	 */
	private static GameDescDict instance;

	/**
	 * 私有构造
	 */
	private GameDescDict() {
		gameDescModelDictionary = new FastMap<Integer,FastMap<Integer,GameDescModel>>();
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static GameDescDict getInstance() {
		if (null == instance) {
			instance = new GameDescDict();
		}

		return instance;
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		gameDescModelDictionary.clear();
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<GameDescModel> gameDescModelList = publicService.getPublicDAO().getGameDescModelList();
		for(GameDescModel model : gameDescModelList){
			if(!gameDescModelDictionary.containsKey(model.getGame_id())){
				gameDescModelDictionary.put(model.getGame_id(), new FastMap<Integer,GameDescModel>());
			}
			gameDescModelDictionary.get(model.getGame_id()).put(model.getId(), model);
		}
		
		//放入redis缓存
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_GAMEDESC, gameDescModelDictionary);
		
		logger.info("加载字典GameDescDict,count=" + gameDescModelList.size() + timer.getStr());
	}

	public FastMap<Integer, FastMap<Integer, GameDescModel>> getGameDescModelDictionary() {
		return gameDescModelDictionary;
	}

	public void setGameDescModelDictionary(FastMap<Integer, FastMap<Integer, GameDescModel>> gameDescModelDictionary) {
		this.gameDescModelDictionary = gameDescModelDictionary;
	}

	

}
