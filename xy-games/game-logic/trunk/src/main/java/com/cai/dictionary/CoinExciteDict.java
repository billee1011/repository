/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.dictionary;

import java.util.Map;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.CoinExciteModel;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wu date: 2018年08月13日 上午9:27:23 <br/>
 */
public final class CoinExciteDict {

	private Logger logger = LoggerFactory.getLogger(CoinExciteDict.class);

	private static final CoinExciteDict M = new CoinExciteDict();


	private volatile Map<Integer,CoinExciteModel> exciteModelMap;

	public static CoinExciteDict getInstance() {
		return M;
	}

	public void load() {
		RedisService redisService = SpringService.getBean(RedisService.class);
		Map<Integer,CoinExciteModel> modelMap = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_COIN_EXCITE, Map.class);
		exciteModelMap = modelMap;
		if(null != exciteModelMap){
			logger.info("CoinExciteDict,count=" + modelMap.size());
		}

	}

	public CoinExciteModel getExciteModel(int id){
		return null != exciteModelMap ? exciteModelMap.get(id) : null;
	}
}
