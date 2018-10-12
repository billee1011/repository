/**
 * 
 */
package com.cai.dictionary;

import java.util.HashMap;
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

	private static ConcurrentHashMap<Integer, SysGameType> sysGameTypeMap;
	private static ConcurrentHashMap<Integer, String> appNameMap;

	/**
	 * 单例
	 */
	private static SysGameTypeDict instance;

	/**
	 * 私有构造
	 */
	private SysGameTypeDict() {
		sysGameTypeMap = new ConcurrentHashMap<Integer, SysGameType>();
		appNameMap  = new ConcurrentHashMap<Integer, String>();
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

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		try {
			RedisService redisService = SpringService.getBean(RedisService.class);
			Object object = redisService.hGet(RedisConstant.DICT, RedisConstant.DIR_SYS_GAME_TYPE, Object.class);
			if(object instanceof  HashMap){
				HashMap<Integer, SysGameType> map = (HashMap<Integer, SysGameType>) object;
				for(Integer key:map.keySet()){
					sysGameTypeMap.put(key, map.get(key));
					appNameMap.put(map.get(key).getGameID(), map.get(key).getAppName());
				}
			}else if(object instanceof  ConcurrentHashMap){
				sysGameTypeMap = (ConcurrentHashMap<Integer, SysGameType>) object;
			}
		} catch (Exception e) {
			logger.error("error", e);
		}
		logger.info("redis缓存加载字典SysGameTypeDict" + timer.getStr());
	}

	public ConcurrentHashMap<Integer, SysGameType> getSysGameTypeDictionary() {
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

	public String getAppNameByAppId(int appId){
		return appNameMap.containsKey(appId)?appNameMap.get(appId):appId+"";
	}
	/**
	 * 获取游戏ID--根据typeIndex
	 * 
	 * @param game_type_index
	 * @return
	 */
	public int getGameIDByTypeIndex(int game_type_index) {
		if(!sysGameTypeMap.containsKey(game_type_index)){
			logger.error("游戏没有配置sys_game_type请检查数据库配置,游戏Id"+game_type_index);
		}
		return sysGameTypeMap.get(game_type_index).getGameID();
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
