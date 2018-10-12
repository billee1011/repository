package com.cai.dictionary;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.ItemExchangeModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;
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

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<ItemExchangeModel> list = publicService.getPublicDAO().getItemExchangeModelList();

		for (ItemExchangeModel model : list)
			itemExchangeMap.put(model.getItemId(), model);

		// 放入redis缓存
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hSet(RedisConstant.DICT, RedisConstant.ITEM_EXCHANGE, itemExchangeMap);
		logger.info("加载字典itemExchangeList,count=" + list.size() + timer.getStr());
	}
}
