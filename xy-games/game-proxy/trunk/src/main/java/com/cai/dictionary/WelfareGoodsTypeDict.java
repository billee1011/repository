package com.cai.dictionary;

import java.util.HashMap;
import java.util.Map;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.WelfareGoodsTypeModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.google.common.collect.Maps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 * @author zhanglong 2018/8/16 16:10
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

	@SuppressWarnings("unchecked")
	public void load() {
		PerformanceTimer timer = new PerformanceTimer();

		RedisService redisService = SpringService.getBean(RedisService.class);
		welfareGoodsTypeMap = redisService.hGet(RedisConstant.DICT, RedisConstant.WELFARE_GOODS_TYPE, HashMap.class);
		logger.info("redis缓存加载加载字典welfare_goods_type" + timer.getStr());
	}

	public Map<Integer, WelfareGoodsTypeModel> getWelfareGoodsTypeMap() {
		return welfareGoodsTypeMap;
	}

	/**
	 * 类型描述
	 */
	public String getTypeDesc(int goodsType) {
		if (welfareGoodsTypeMap.containsKey(goodsType)) {
			return welfareGoodsTypeMap.get(goodsType).getType_desc();
		}
		return "";
	}

	/**
	 * 类型上架状态
	 */
	public boolean isGoodsTypeOnSale(int goodsType) {
		if (welfareGoodsTypeMap.containsKey(goodsType)) {
			return welfareGoodsTypeMap.get(goodsType).getOnline() == 1;
		}
		return false;
	}
}
