package com.cai.dictionary;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.coin.CoinGameType;
import com.cai.common.domain.coin.CoinGameDetail;
import com.cai.common.domain.coin.CoinGame;
import com.cai.common.domain.coin.CoinRelief;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.dao.PublicDAO;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;

/**
 * 比赛场字典
 * 
 * @author run
 *
 */
public class CoinDict {

	private Logger logger = LoggerFactory.getLogger(CoinDict.class);
	private static final CoinDict coinDict = new CoinDict();
	
	private CoinDict() {
	}
	
	public static CoinDict getInstance() {
		return coinDict;
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		PublicService publicService = SpringService.getBean(PublicService.class);
		PublicDAO dao = publicService.getPublicDAO();
		// 放入redis缓存
		RedisService redisService = SpringService.getBean(RedisService.class);
		
		try{
			initGameType(dao, redisService);
			initGames(dao, redisService);
			initGameDetails(dao, redisService);
			initGameRelief(dao, redisService);
		}catch (Exception e) {
			logger.error("load->load error !!",e);
		}
		
		logger.info("load-> coin cache update success time:{} !!",timer.getStr());
	}
	
	private void initGameType(PublicDAO dao,RedisService redis){
		List<CoinGameType> list = dao.getCoinGameTypeList();
		redis.hSet(RedisConstant.DICT, RedisConstant.DICT_COIN_GAME_TYPE, list);
	}
	
	private void initGames(PublicDAO dao,RedisService redis){
		List<CoinGame> list = dao.getCoinGameTypeIndexList();
		List<CoinGame> indexList = null;
		Map<Integer, List<CoinGame>> gameTypeIndexMap = new LinkedHashMap<>();
		for(CoinGame index : list){
			indexList = gameTypeIndexMap.get(index.getGame_big_type_id());
			if(indexList == null){
				indexList = new ArrayList<>();
				gameTypeIndexMap.put(index.getGame_big_type_id(), indexList);
			}
			indexList.add(index);
		}
		redis.hSet(RedisConstant.DICT, RedisConstant.DICT_COIN_GAMES, gameTypeIndexMap);
	}
	
	private void initGameDetails(PublicDAO dao,RedisService redis){
		List<CoinGameDetail> list = dao.getCoinGameTypeDetailList();
		Map<Integer, List<CoinGameDetail>> gameTypeDetailMap = new LinkedHashMap<>();
		List<CoinGameDetail> detailList = null;
		for(CoinGameDetail detail : list){
			detailList = gameTypeDetailMap.get(detail.getGame_type_index());
			if(detailList == null){
				detailList = new ArrayList<>();
				gameTypeDetailMap.put(detail.getGame_type_index(), detailList);
			}
			detailList.add(detail);
		}
		redis.hSet(RedisConstant.DICT, RedisConstant.DICT_COIN_GAME_DETAILS, gameTypeDetailMap);
	}
	
	private void initGameRelief(PublicDAO dao,RedisService redis){
		List<CoinRelief> list = dao.getCoinReliefList();
		Map<Integer, CoinRelief> reliefMap = new LinkedHashMap<>();
		for(CoinRelief relief : list){
			reliefMap.put(relief.getDaily_receive_time(), relief);
		}
		redis.hSet(RedisConstant.DICT, RedisConstant.DICT_COIN_RELIEF, reliefMap);
	}
}
