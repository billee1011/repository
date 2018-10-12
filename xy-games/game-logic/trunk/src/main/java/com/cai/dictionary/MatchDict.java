/**
 * 
 */
package com.cai.dictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.MatchRoundModel;
import com.cai.common.domain.MatchUnionModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.MatchTableService;

/**
 * 扣豆描述
 *
 * @author tang
 */
public class MatchDict {

	private Logger logger = LoggerFactory.getLogger(MatchDict.class);

	private ConcurrentHashMap<Integer, MatchRoundModel> matchDictionary = new ConcurrentHashMap<>();
	
	private Map<Integer,MatchUnionModel> matchUnions = new ConcurrentHashMap<>();
	/**
	 * 单例
	 */
	private static MatchDict instance;
	/**
	 * 私有构造
	 */
	private MatchDict() {
	}

	/**
	 * 单例模式
	 *
	 * @return 字典单例
	 */
	public static MatchDict getInstance() {
		if (null == instance) {
			instance = new MatchDict();
		}

		return instance;
	}

	@SuppressWarnings("unchecked")
	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		RedisService redisService = SpringService.getBean(RedisService.class);
		ConcurrentHashMap<Integer, MatchRoundModel> temp = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_MATCH, ConcurrentHashMap.class);
		if(temp != null){
			matchDictionary = temp;
		}
		
		Map<Integer,MatchUnionModel> tempMatchUnions = new ConcurrentHashMap<>();
		List<MatchUnionModel> list = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_MATCH_UNION, ArrayList.class);
		if(list != null){
			for (MatchUnionModel unionModel : list) {
				tempMatchUnions.put(unionModel.getId(), unionModel);
			}
		}
		matchUnions = tempMatchUnions;
		
		MatchTableService.getInstance().checkCloseMatch();
		logger.info("redis缓存加载字典matchDictionary" + timer.getStr());
	}

	public ConcurrentHashMap<Integer, MatchRoundModel> getMatchDictionary() {
		return matchDictionary;
	}
	public  MatchRoundModel getMatchModel(int matchId) {
		MatchRoundModel model = matchDictionary.get(matchId);
		if(model == null){
			logger.error("no find match round model matchId = " + matchId);
		}
		return model;
	}
	
	public MatchUnionModel getUnionModel(int unionId){
		return matchUnions.get(unionId);
	}
}
