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
import com.cai.common.domain.SysGameType;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

/**
 * app版本管理
 *
 * @author xwy
 */
public class SysGameTypeDict {

	private Logger logger = LoggerFactory.getLogger(SysGameTypeDict.class);

	private static Map<Integer, SysGameType> sysGameTypeMap;

	/**
	 * 单例
	 */
	private static SysGameTypeDict instance;

	/**
	 * 私有构造
	 */
	private SysGameTypeDict() {
		sysGameTypeMap = new ConcurrentHashMap<Integer, SysGameType>();
	}

	/**
	 * 单例模式
	 *
	 * @return 字典单例
	 */
	public static SysGameTypeDict getInstance() {
		if (null == instance) {
			instance = new SysGameTypeDict();
		}

		return instance;
	}

	@SuppressWarnings("unchecked")
	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		try {
			RedisService redisService = SpringService.getBean(RedisService.class);
			sysGameTypeMap = redisService.hGet(RedisConstant.DICT, RedisConstant.DIR_SYS_GAME_TYPE, HashMap.class);
			// com.cai.dictionary.SysGameTypeDict.sysGameTypeMap =
			// sysGameTypeMap;
		} catch (Exception e) {
			logger.error("error", e);
		}
		logger.info("redis缓存加载字典SysGameTypeDict" + timer.getStr());
	}

	public Map<Integer, SysGameType> getSysGameTypeDictionary() {
		return sysGameTypeMap;
	}

	/**
	 * 获取游戏收费索引--用于判断是否开放
	 * 
	 * @param game_type_index
	 * @return
	 */
	public Integer getGameGoldTypeIndex(int game_type_index) {
		return sysGameTypeMap.get(game_type_index).getGold_type();
	}

	/**
	 * 获取收费的索引 --根据局数判断
	 * 
	 * @param game_type_index
	 * @return
	 */
	public int[] getGoldIndexByTypeIndex(int game_type_index) {
		return sysGameTypeMap.get(game_type_index).getGoldIndex();
	}

	public SysGameType getSysGameType(int game_type_index) {
		return sysGameTypeMap.get(game_type_index);
	}

	/**
	 * 获取游戏ID--根据typeIndex
	 * 
	 * @param game_type_index
	 * @return
	 */
	public int getGameIDByTypeIndex(int game_type_index) {
		SysGameType gameType = sysGameTypeMap.get(game_type_index);
		return null != gameType ? gameType.getGameID() : -1;
	}

	// 麻将小类型
	public String getMJname(int v2) {
		return sysGameTypeMap.get(v2).getDesc();
	}

	// 游戏大类型
	public int getBigType(int game_type_index) {
		return sysGameTypeMap.get(game_type_index).getGameBigType();
	}

}
