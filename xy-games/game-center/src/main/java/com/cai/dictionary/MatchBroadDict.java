package com.cai.dictionary;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.MatchBroadModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;

/**
 * 主界面公告字典
 * @author run
 *
 */
public class MatchBroadDict {

	private Logger logger = LoggerFactory.getLogger(MatchBroadDict.class);
	
	/**
	 * 系统参数缓存 match_id(id,model)
	 */
	private Map<Integer,MatchBroadModel> matchBroadModelDictionary;

	/**
	 * 单例
	 */
	private static MatchBroadDict instance;

	/**
	 * 私有构造
	 */
	private MatchBroadDict() {
		matchBroadModelDictionary = new ConcurrentHashMap<Integer,MatchBroadModel>();
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static MatchBroadDict getInstance() {
		if (null == instance) {
			instance = new MatchBroadDict();
		}

		return instance;
	}
	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		PublicService publicService = SpringService.getBean(PublicService.class);
		Date now = new Date();
		List<MatchBroadModel> matchBroadModelList = publicService.getPublicDAO().getMatchBroadModelList(now);
		Map<Integer,MatchBroadModel> matchBroadModelDictionary = new ConcurrentHashMap<Integer,MatchBroadModel>();
		for(MatchBroadModel model : matchBroadModelList){
			matchBroadModelDictionary.put(model.getMatch_id(), model);
		}
		this.matchBroadModelDictionary = matchBroadModelDictionary;
		//放入redis缓存
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_MATCH_BROAD, this.matchBroadModelDictionary);
		logger.info("redis缓存加载字典matchBroadModelDictionary"  + timer.getStr());
	}

	public Map<Integer, MatchBroadModel> getMatchBroadModelDictionary() {
		return matchBroadModelDictionary;
	}

}
