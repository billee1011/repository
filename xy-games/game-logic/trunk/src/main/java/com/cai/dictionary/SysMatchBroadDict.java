package com.cai.dictionary;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.MatchBroadModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

/**
 * 比赛场公告字典
 * @author run
 *
 */
public class SysMatchBroadDict {

	private Logger logger = LoggerFactory.getLogger(SysMatchBroadDict.class);
	
	/**
	 * 系统参数缓存 match_id(id,model)
	 */
	private Map<Integer,MatchBroadModel> matchBroadModelDictionary;

	/**
	 * 单例
	 */
	private static SysMatchBroadDict instance;

	/**
	 * 私有构造
	 */
	private SysMatchBroadDict() {
		matchBroadModelDictionary = new ConcurrentHashMap<Integer,MatchBroadModel>();
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static SysMatchBroadDict getInstance() {
		if (null == instance) {
			instance = new SysMatchBroadDict();
		}

		return instance;
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		try{
			RedisService redisService = SpringService.getBean(RedisService.class);
			matchBroadModelDictionary = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_MATCH_BROAD, ConcurrentHashMap.class);
		}catch(Exception e){
			logger.error("error",e);
		}
		logger.info("redis缓存加载字典sys_notice"  + timer.getStr());
	}

	public Map<Integer, MatchBroadModel> getMatchBroadModelDictionary() {
		return matchBroadModelDictionary;
	}
	
	public MatchBroadModel getMatchBroad(int matchId){
		return matchBroadModelDictionary.get(matchId);
	}
}
