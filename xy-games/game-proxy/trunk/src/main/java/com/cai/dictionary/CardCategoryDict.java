/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.dictionary;

import java.util.Map;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.CardCategoryModel;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wu date: 2018年08月13日 上午9:27:23 <br/>
 */
public final class CardCategoryDict {

	private Logger logger = LoggerFactory.getLogger(CardCategoryDict.class);

	private static final CardCategoryDict M = new CardCategoryDict();


	private volatile Map<Integer,CardCategoryModel> modelMap;


	public static CardCategoryDict getInstance() {
		return M;
	}


	public void load() {
		RedisService redisService = SpringService.getBean(RedisService.class);
		Map<Integer, CardCategoryModel> modelMap = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_CARD_CATEGORY, Map.class);

		this.modelMap = modelMap;


		if(null != modelMap){
			logger.info("CoinExciteDict,count=" + modelMap.size());
		}
	}

	public CardCategoryModel getModel(int id){
		return null != modelMap ? modelMap.get(id) : null;
	}

}
