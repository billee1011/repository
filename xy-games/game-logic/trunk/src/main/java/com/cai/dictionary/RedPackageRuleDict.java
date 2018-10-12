/**
 * 
 */
package com.cai.dictionary;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.RedPackageActivityModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

/**
 * 扣豆描述
 *
 * @author tang
 */
public class RedPackageRuleDict {

	private Logger logger = LoggerFactory.getLogger(RedPackageRuleDict.class);

	private static ConcurrentHashMap<Integer, RedPackageActivityModel> redPackageRuleMap;

	/**
	 * 单例
	 */
	private static RedPackageRuleDict instance;

	/**
	 * 私有构造
	 */
	private RedPackageRuleDict() {
		redPackageRuleMap = new ConcurrentHashMap<Integer, RedPackageActivityModel>();
	}

	/**
	 * 单例模式
	 *
	 * @return 字典单例
	 */
	public static RedPackageRuleDict getInstance() {
		if (null == instance) {
			instance = new RedPackageRuleDict();
		}

		return instance;
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		try {
			RedisService redisService = SpringService.getBean(RedisService.class);
			redPackageRuleMap = redisService.hGet(RedisConstant.DICT, RedisConstant.DIR_RED_PACKAGE_RULE, ConcurrentHashMap.class);
		} catch (Exception e) {
			logger.error("error", e);
		}
		logger.info("redis缓存加载字典redPackageRuleDict" + timer.getStr());
	}

	public  ConcurrentHashMap<Integer, RedPackageActivityModel> getRedPackageRuleMap() {
		return redPackageRuleMap;
	}

}
