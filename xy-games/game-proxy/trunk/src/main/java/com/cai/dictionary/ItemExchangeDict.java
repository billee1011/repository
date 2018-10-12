package com.cai.dictionary;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.ItemExchangeModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.google.common.collect.Maps;

public class ItemExchangeDict {
	private Logger logger = LoggerFactory.getLogger(ItemExchangeDict.class);

	private Map<Integer, ItemExchangeModel> itemExchangeMap;

	private static ItemExchangeDict instance;

	public synchronized static ItemExchangeDict getInstance() {
		if (instance == null) {
			instance = new ItemExchangeDict();
		}
		return instance;
	}

	private ItemExchangeDict() {
		itemExchangeMap = Maps.newHashMap();
	}

	@SuppressWarnings("unchecked")
	public void load() {
		PerformanceTimer timer = new PerformanceTimer();

		RedisService redisService = SpringService.getBean(RedisService.class);
		itemExchangeMap = redisService.hGet(RedisConstant.DICT, RedisConstant.ITEM_EXCHANGE, HashMap.class);
		logger.info("redis缓存加载加载字典item_exchange" + timer.getStr());
	}

	public Map<Integer, ItemExchangeModel> getItemExchangeMap() {
		return itemExchangeMap;
	}
}
