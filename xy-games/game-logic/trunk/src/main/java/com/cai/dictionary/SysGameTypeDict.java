/**
 * 
 */
package com.cai.dictionary;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.SysGameType;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

/**
 * 扣豆描述
 *
 * @author tang
 */
public class SysGameTypeDict {

	private Logger logger = LoggerFactory.getLogger(SysGameTypeDict.class);

	private static ConcurrentHashMap<Integer, SysGameType> sysGameTypeMap;
	private Map<Integer, Set<Integer>> gameTypeListMap = new HashMap<>(); //key bigGameType value:key gameId 

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

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		try {
			RedisService redisService = SpringService.getBean(RedisService.class);
			Object object = redisService.hGet(RedisConstant.DICT, RedisConstant.DIR_SYS_GAME_TYPE, Object.class);
			if (object instanceof HashMap) {
				HashMap<Integer, SysGameType> map = (HashMap<Integer, SysGameType>) object;
				for (Integer key : map.keySet()) {
					sysGameTypeMap.put(key, map.get(key));
				}
			} else if (object instanceof ConcurrentHashMap) {
				sysGameTypeMap = (ConcurrentHashMap<Integer, SysGameType>) object;
			}
			// sysGameTypeMap = redisService.hGet(RedisConstant.DICT,
			// RedisConstant.DIR_SYS_GAME_TYPE, ConcurrentHashMap.class);
			intGameTypeMap();
		} catch (Exception e) {
			logger.error("error", e);
		}
		logger.info("redis缓存加载字典SysGameTypeDict" + timer.getStr());

	}
	
	private void intGameTypeMap(){
		if(sysGameTypeMap == null){
			return;
		}
		Map<Integer, Set<Integer>> tempGameTypeListMap = new HashMap<>();
		Set<Integer> tempSet = null;
		for(SysGameType sType : sysGameTypeMap.values()){
			tempSet = tempGameTypeListMap.get(sType.getGameBigType());
			if(tempSet == null){
				tempSet = new HashSet<>();
				tempGameTypeListMap.put(sType.getGameBigType(), tempSet);
			}
			tempSet.add(sType.getGame_type_index());
		}
		gameTypeListMap = tempGameTypeListMap;
	}
	
	public Set<Integer> getGameTypeIndexSet(int gameType){
		Set<Integer> set = gameTypeListMap.get(gameType);
		if(set == null){
			set = new HashSet<>();
			return set;
		}
		Set<Integer> newSet = new HashSet<>();
		newSet.addAll(set);
		return newSet;
	}

	public ConcurrentHashMap<Integer, SysGameType> getSysGameTypeDictionary() {
		return sysGameTypeMap;
	}
	
	public int getGameBigType(int game_type_index){
		SysGameType sType = sysGameTypeMap.get(game_type_index);
		if(sType == null){
			return -1;
		}
		return sType.getGameBigType();
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
		return sysGameTypeMap.get(game_type_index).getGameID();
	}
	
	/**
	 * 获取游戏ID--根据typeIndex
	 * 
	 * @param game_type_index
	 * @return
	 */
	public String getGameDescByTypeIndex(int game_type_index) {
		SysGameType gameType = sysGameTypeMap.get(game_type_index);
		if(gameType==null) return "";
		return gameType.getDesc();
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
