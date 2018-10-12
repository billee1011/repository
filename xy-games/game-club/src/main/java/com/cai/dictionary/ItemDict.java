package com.cai.dictionary;

import java.util.HashMap;
import java.util.Map;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.ItemModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.google.common.collect.Maps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemDict {
	private Logger logger = LoggerFactory.getLogger(ItemDict.class);

	private Map<Integer, ItemModel> itemMap;

	private static ItemDict instance;

	public synchronized static ItemDict getInstance() {
		if (instance == null) {
			instance = new ItemDict();
		}
		return instance;
	}

	private ItemDict() {
		itemMap = Maps.newHashMap();
	}

	@SuppressWarnings("unchecked")
	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		try {
			RedisService redisService = SpringService.getBean(RedisService.class);
			itemMap = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_PACKAGE_ITEM, HashMap.class);
		} catch (Exception e) {
			logger.error("error", e);
		}
		logger.info("redis缓存加载字典ItemDict" + timer.getStr());
	}

	public ItemModel token4ItemId(int itemId) {
		ItemModel itemModel = itemMap.get(itemId);
		if (itemModel == null) {
			logger.info("背包物品ID 【" + itemId + "】 不存在，清相应配置重新配置");
		}
		return itemModel;
	}

	public String getNameByItemId(int itemId) {
		String name = "";
		ItemModel itemModel = itemMap.get(itemId);
		if (itemModel == null) {
			logger.info("背包物品ID 【" + itemId + "】 不存在，清相应配置重新配置");
		} else {
			name = itemModel.getName();
		}
		return name;
	}
}
