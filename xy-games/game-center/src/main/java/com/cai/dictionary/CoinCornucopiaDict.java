/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.dictionary;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.ClubDataModel;
import com.cai.common.domain.CoinCornucopiaModel;
import com.cai.common.domain.CoinExciteModel;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.groupingBy;

/**
 * @author wu date: 2018年08月13日 上午9:27:23 <br/>
 */
public final class CoinCornucopiaDict {

	private static Logger logger = LoggerFactory.getLogger(CoinCornucopiaDict.class);

	private static final CoinCornucopiaDict M = new CoinCornucopiaDict();

	public static CoinCornucopiaDict getInstance() {
		return M;
	}

	public void load() {
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<CoinCornucopiaModel> coinCornucopiaModel = publicService.getPublicDAO().getCoinCornucopiaModel();
		if (null == coinCornucopiaModel || coinCornucopiaModel.isEmpty()) {
			return;
		}

		Map<Integer, List<CoinCornucopiaModel>> modelMap = coinCornucopiaModel.stream().collect(groupingBy(CoinCornucopiaModel::getGameTypeIndex));

		// 放入redis缓存
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_CORNUCOPIA, modelMap);
		logger.info("CoinCornucopiaDict,count=" + modelMap.size());
	}

}
