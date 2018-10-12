package com.cai.dictionary;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.info.CardSecretInfo;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;

public class CardSecretDict {
	private Logger logger = LoggerFactory.getLogger(CardSecretDict.class);

	private static CardSecretDict instance;

	public synchronized static CardSecretDict getInstance() {
		if (instance == null) {
			instance = new CardSecretDict();
		}
		return instance;
	}

	public CardSecretDict() {

	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<CardSecretInfo> list = publicService.getPublicDAO().getCardSecretInfoList();

		// 放入redis缓存
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hSet(RedisConstant.DICT, RedisConstant.CARD_SECRET, list);
		logger.info("加载字典cardSecretList,count=" + list.size() + timer.getStr());
	}
}
