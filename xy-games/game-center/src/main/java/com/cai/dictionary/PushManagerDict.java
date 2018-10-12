/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.dictionary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.PushInfoModel;
import com.cai.common.util.SpringService;
import com.cai.dao.PublicDAO;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;

/**
 * 推送管理字典
 * 
 * @author tang
 */
public class PushManagerDict {
	private Logger logger = LoggerFactory.getLogger(PushManagerDict.class);
	/**
	 * 单例
	 */
	private final static PushManagerDict instance = new PushManagerDict();
	
	private Map<Integer,PushInfoModel> pushInfoMap = null;
	/**
	 * 私有构造
	 */
	private PushManagerDict() {
		pushInfoMap = new HashMap<>();
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static PushManagerDict getInstance() {
		return instance;
	}

	
	/**
	 * 
	 */
	public void load() {
		PublicService publicService = SpringService.getBean(PublicService.class);
		PublicDAO dao = publicService.getPublicDAO();
		List<PushInfoModel> list = dao.getPushInfoModelList();
		pushInfoMap.clear();
		for(PushInfoModel model:list){
			pushInfoMap.put(model.getId(), model);
		}
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hSet(RedisConstant.DICT, RedisConstant.PUSH_MANAGER_DICT, pushInfoMap);
		logger.info("加载字典PushManagerDict,count=" + pushInfoMap.size());
	}

	
}
