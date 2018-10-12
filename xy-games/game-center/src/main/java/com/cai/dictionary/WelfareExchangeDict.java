package com.cai.dictionary;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.WelfareExchangeModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;
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

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<WelfareExchangeModel> list = publicService.getPublicDAO().getWelfareExchangeModelList();

		for (WelfareExchangeModel model : list)
			welfareExchangeMap.put(model.getItemId(), model);

		// 放入redis缓存
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hSet(RedisConstant.DICT, RedisConstant.WELFARE_EXCHANGE, welfareExchangeMap);
		logger.info("加载字典welfareExchangeList,count=" + list.size() + timer.getStr());
	}
}
