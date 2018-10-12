package com.cai.dictionary;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.WelfareExchangeModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.google.common.collect.Maps;

public class WelfareExchangeDict {
	private Logger logger = LoggerFactory.getLogger(WelfareExchangeDict.class);

	private Map<Integer, WelfareExchangeModel> welfareExchangeMap;

	private static WelfareExchangeDict instance;

	public synchronized static WelfareExchangeDict getInstance() {
		if (instance == null) {
			instance = new WelfareExchangeDict();
		}
		return instance;
	}

	private WelfareExchangeDict() {
		welfareExchangeMap = Maps.newHashMap();
	}

	@SuppressWarnings("unchecked")
	public void load() {
		PerformanceTimer timer = new PerformanceTimer();

		RedisService redisService = SpringService.getBean(RedisService.class);
		welfareExchangeMap = redisService.hGet(RedisConstant.DICT, RedisConstant.WELFARE_EXCHANGE, HashMap.class);
		logger.info("redis缓存加载加载字典welfare_exchange" + timer.getStr());
	}

	public Map<Integer, WelfareExchangeModel> getWelfareExchangeMap() {
		return welfareExchangeMap;
	}
}
