package com.cai.dictionary;

import java.util.List;
import java.util.Map;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.WelfareGoodsTypeModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;
import com.google.common.collect.Maps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/8/16 15:19
 */
public class WelfareGoodsTypeDict {

	private Logger logger = LoggerFactory.getLogger(WelfareGoodsTypeDict.class);

	private static WelfareGoodsTypeDict instance = new WelfareGoodsTypeDict();

	private Map<Integer, WelfareGoodsTypeModel> welfareGoodsTypeMap;

	public static WelfareGoodsTypeDict getInstance() {
		return instance;
	}

	private WelfareGoodsTypeDict() {
		welfareGoodsTypeMap = Maps.newHashMap();
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<WelfareGoodsTypeModel> list = publicService.getPublicDAO().getWelfareGoodsTypeModelList();

		for (WelfareGoodsTypeModel model : list)
			welfareGoodsTypeMap.put(model.getId(), model);

		// 放入redis缓存
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hSet(RedisConstant.DICT, RedisConstant.WELFARE_GOODS_TYPE, welfareGoodsTypeMap);
		logger.info("加载字典welfareGoodsTypeModelList,count=" + list.size() + timer.getStr());
	}
}
