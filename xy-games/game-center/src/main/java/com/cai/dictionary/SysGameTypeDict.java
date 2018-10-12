package com.cai.dictionary;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.GameTypeDBModel;
import com.cai.common.domain.SysGameType;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;

/**
 * 游戏类型对应收费索引 游戏类型 描述字典
 *
 */
public class SysGameTypeDict {

	private Logger logger = LoggerFactory.getLogger(SysGameTypeDict.class);

	private static ConcurrentHashMap<Integer, SysGameType> sysGameTypeMap;

	private static ConcurrentHashMap<Integer, SysGameType> goldGameTypeMap;

	/**
	 * 单例
	 */
	private static SysGameTypeDict instance;

	/**
	 * 私有构造
	 */
	private SysGameTypeDict() {
		sysGameTypeMap = new ConcurrentHashMap<Integer, SysGameType>();
		goldGameTypeMap = new ConcurrentHashMap<Integer, SysGameType>();
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

	public boolean isGoldGameType(int goldType) {
		return goldGameTypeMap.get(goldType) == null ? false : true;
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<GameTypeDBModel> gameTypeDBModelList = publicService.getPublicDAO().getSysGameTypeDBModelList();
		for (GameTypeDBModel model : gameTypeDBModelList) {
			String goldIndexStr = model.getGoldIndex().substring(1, model.getGoldIndex().length() - 1);
			String[] goldIndexs = goldIndexStr.split(",");
			int[] ia = new int[goldIndexs.length];
			for (int i = 0; i < goldIndexs.length; i++) {
				if (StringUtils.isNotBlank(goldIndexs[i]))
					ia[i] = Integer.parseInt(goldIndexs[i].trim());
			}
			SysGameType sysGameType = new SysGameType();
			sysGameType.setGameID(model.getGameID());
			sysGameType.setDesc(model.getDescription());
			sysGameType.setGameBigType(model.getGame_big_type());
			sysGameType.setGold_type(model.getGold_type());
			sysGameType.setGame_type_index(model.getGame_type_index());
			sysGameType.setGoldIndex(ia);
			sysGameType.setAppName(model.getApp_name());
			sysGameTypeMap.put(sysGameType.getGame_type_index(), sysGameType);

			goldGameTypeMap.put(model.getGold_type(), sysGameType);
		}
		// com.cai.dictionary.SysGameTypeDict.sysGameTypeMap = sysGameTypeMap;
		// 放入redis缓存
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hSet(RedisConstant.DICT, RedisConstant.DIR_SYS_GAME_TYPE, sysGameTypeMap);
		logger.info("加载字典sysGameTypeMap,count=" + sysGameTypeMap.size() + timer.getStr());

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

	/**
	 * 获取游戏ID--根据typeIndex
	 * 
	 * @param game_type_index
	 * @return
	 */
	public int getGameIDByTypeIndex(int game_type_index) {
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
