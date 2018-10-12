package com.cai.dictionary;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.ItemModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.MatchServiceImp;
import com.cai.service.PublicService;
import com.google.common.collect.Maps;

/**
 * 转盘字典
 * 
 * @author yu
 *
 */
public class ItemDict {

	private Logger logger = LoggerFactory.getLogger(ActivityDict.class);

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

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<ItemModel> itemList = publicService.getPublicDAO().getItemModelList();

		for (ItemModel item : itemList)
			itemMap.put(item.getItemId(), item);

		// 放入redis缓存
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_PACKAGE_ITEM, itemMap);
		MatchServiceImp.getInstance().dispatchMaxSeqMap();
		logger.info("加载字典goodsList,count=" + itemList.size() + timer.getStr());
	}
}
