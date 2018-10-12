package com.cai.dictionary;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.coin.CoinGameDetail;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

/**
 * 比赛场字典
 * 
 * @author run
 *
 */
public class CoinDict {

	private Logger logger = LoggerFactory.getLogger(CoinDict.class);
	private static CoinDict coinDict = new CoinDict();
	private CoinDict(){}
	
	private Map<Integer, List<CoinGameDetail>> gameDetailMap = new LinkedHashMap<>();
	
	public static CoinDict getInstance() {
		return coinDict;
	}

	@SuppressWarnings("unchecked")
	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		
		RedisService redisService = SpringService.getBean(RedisService.class);
		
		Map<Integer, List<CoinGameDetail>> detailMap = redisService.hGet(RedisConstant.DICT,
				RedisConstant.DICT_COIN_GAME_DETAILS, LinkedHashMap.class);
		if(detailMap != null){
			Map<Integer, List<CoinGameDetail>> tempMap = new LinkedHashMap<>();
			for(int gameId : detailMap.keySet()){
				List<CoinGameDetail> list = tempMap.get(gameId);
				if(list == null){
					list = new ArrayList<>();
					tempMap.put(gameId, list);
				}
				for(CoinGameDetail detail : detailMap.get(gameId)){
					if(!detail.isClose()){
						list.add(detail);
					}
				}
			}
			gameDetailMap = tempMap;
		}
		
		logger.info("load-> coin cache update success time:{} !!",timer.getStr());
	}
	
	
	public CoinGameDetail getGameDetail(int gameId,int detailId){
		List<CoinGameDetail> list = getGameDetails(gameId);
		if(list == null){
			return null;
		}
		for(CoinGameDetail detail : list){
			if(detail.getId() == detailId){
				return detail;
			}
		}
		logger.error("getGameDetail-> no find gameTypeDetail gameId:{} id:{} !!",gameId,detailId);
		return null;
	}
	
	public List<CoinGameDetail> getGameDetails(int gameId){
		List<CoinGameDetail> list = gameDetailMap.get(gameId);
		if(list == null){
			logger.error("getGameDetails-> no find game details gameId:{} !!",gameId);
		}
		return list;
	}
	
	public int getMinGameLimit(int gameId){
		int value = Integer.MAX_VALUE;
		List<CoinGameDetail> list = gameDetailMap.get(gameId);
		if(list == null){
			logger.error("getGameDetails-> no find game details gameId:{} !!",gameId);
			return value;
		}
		for(CoinGameDetail detail : list){
			if(detail.getLower_limit() < value){
				value = detail.getLower_limit();
			}
		}
		return value;
	}
	
}
