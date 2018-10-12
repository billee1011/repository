/**
 * 
 */
package com.cai.dictionary;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.GameGroups;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

/**
 * 
 */
public class GameGroupRuleDict {

	private Logger logger = LoggerFactory.getLogger(GameGroupRuleDict.class);

	// 每款子游戏的数据
	private Map<Integer, GameGroups> groups;

	/**
	 * 单例
	 */
	private static GameGroupRuleDict instance;

	/**
	 * 私有构造
	 */
	private GameGroupRuleDict() {
		groups = new ConcurrentHashMap<Integer, GameGroups>();
	}

	/**
	 * 单例模式
	 *
	 * @return 字典单例
	 */
	public static GameGroupRuleDict getInstance() {
		if (null == instance) {
			instance = new GameGroupRuleDict();
		}

		return instance;
	}

	@SuppressWarnings("unchecked")
	public void load() {
		try {
			RedisService redisService = SpringService.getBean(RedisService.class);
			groups = redisService.hGet(RedisConstant.DICT, RedisConstant.DIR_GAME_GROUP_RULE, HashMap.class);
		} catch (Exception e) {
			logger.error("error", e);
		}

	}
	
	public GameGroups get(int game_type_index){
		return groups.get(game_type_index);
	}

}
