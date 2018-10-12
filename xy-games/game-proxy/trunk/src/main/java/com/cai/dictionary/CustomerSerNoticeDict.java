package com.cai.dictionary;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.CustomerSerNoticeModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

import javolution.util.FastMap;

/**
 * 主界面公告字典
 * @author run
 *
 */
public class CustomerSerNoticeDict {

	private Logger logger = LoggerFactory.getLogger(CustomerSerNoticeDict.class);
	
	/**
	 * 系统参数缓存 game_id(id,model)
	 */
	private ConcurrentHashMap<Integer,FastMap<Integer,CustomerSerNoticeModel>> customerSerDictionary;

	/**
	 * 单例
	 */
	private static CustomerSerNoticeDict instance;

	/**
	 * 私有构造
	 */
	private CustomerSerNoticeDict() {
		customerSerDictionary = new ConcurrentHashMap<Integer,FastMap<Integer,CustomerSerNoticeModel>>();
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static CustomerSerNoticeDict getInstance() {
		if (null == instance) {
			instance = new CustomerSerNoticeDict();
		}

		return instance;
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		try{
			RedisService redisService = SpringService.getBean(RedisService.class);
			customerSerDictionary = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_CUSTOMER_SER_NOTICE, ConcurrentHashMap.class);
		}catch(Exception e){
			logger.error("error",e);
		}
		logger.info("redis缓存加载字典customerSerDictionary"  + timer.getStr());
	}

	public ConcurrentHashMap<Integer, FastMap<Integer, CustomerSerNoticeModel>> getCustomerSerNoticeDictionary() {
		return customerSerDictionary;
	}



	

	
	

}
