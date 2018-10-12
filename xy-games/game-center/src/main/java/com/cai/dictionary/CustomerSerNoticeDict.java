package com.cai.dictionary;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.CustomerSerNoticeModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;

import javolution.util.FastMap;

/**
 * 客服界面公告字典
 * @author tang
 *
 */
public class CustomerSerNoticeDict {

	private Logger logger = LoggerFactory.getLogger(CustomerSerNoticeDict.class);
	
	/**
	 * 
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
		customerSerDictionary.clear();
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<CustomerSerNoticeModel> mainUiNoticeModelList = publicService.getPublicDAO().getCustomerSerNoticeModelList();
		for(CustomerSerNoticeModel model : mainUiNoticeModelList){
			if(!customerSerDictionary.containsKey(model.getGame_id())){
				FastMap<Integer,CustomerSerNoticeModel> map = new FastMap<Integer,CustomerSerNoticeModel>();
				customerSerDictionary.put(model.getGame_id(), map);
			}
			customerSerDictionary.get(model.getGame_id()).put(model.getId(), model);
		}
		//放入redis缓存
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_CUSTOMER_SER_NOTICE, customerSerDictionary);
		logger.info("加载字典CustomerSerNoticeDict,count=" + mainUiNoticeModelList.size() + timer.getStr());
	}

	public ConcurrentHashMap<Integer, FastMap<Integer, CustomerSerNoticeModel>> getCustomerSerNoticeDictionary() {
		return customerSerDictionary;
	}


}
