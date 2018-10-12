/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.dictionary;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.CardCategoryModel;
import com.cai.common.domain.CoinExciteModel;
import com.cai.common.util.FilterUtil;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wu date: 2018年08月13日 上午9:27:23 <br/>
 */
public final class CardCategoryDict {

	private Logger logger = LoggerFactory.getLogger(CardCategoryDict.class);

	private static final CardCategoryDict M = new CardCategoryDict();

	public static CardCategoryDict getInstance() {
		return M;
	}

	public void load() {
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<CardCategoryModel> coinExciteModelList = publicService.getPublicDAO().getCardCategoryModelList();
		coinExciteModelList = FilterUtil.filter(coinExciteModelList, m -> m.getStatus() == 1);
		
		Map<Integer, CardCategoryModel> modelMap = coinExciteModelList.stream()
				.collect(Collectors.toMap(CardCategoryModel::getId, Function.identity()));

		// 放入redis缓存
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_CARD_CATEGORY, modelMap);
		logger.info("CardCategoryDict,count=" + modelMap.size());
	}
}
