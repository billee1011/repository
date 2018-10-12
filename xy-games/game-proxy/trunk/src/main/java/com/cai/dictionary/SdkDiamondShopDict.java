package com.cai.dictionary;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.sdk.SdkDiamondShopModel;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;


/**
 * SDK 钻石商城
 * @author chansonyan
 * 2018年9月25日
 */
@SuppressWarnings("unchecked")
public class SdkDiamondShopDict {

	private Logger logger = LoggerFactory.getLogger(SdkDiamondShopDict.class);
	
	private Map<Integer, SdkDiamondShopModel> sdkDiamondShopMap;
	
	private static SdkDiamondShopDict instance;
	
	/**
	 * 私有构造
	 */
	private SdkDiamondShopDict() {
		sdkDiamondShopMap = new HashMap<>();
	}

	/**
	 * 单例模式
	 * 
	 */
	public static SdkDiamondShopDict getInstance() {
		if (null == instance) {
			instance = new SdkDiamondShopDict();
		}
		return instance;
	}

	public void load() {
		RedisService redisService = SpringService.getBean(RedisService.class);
		this.sdkDiamondShopMap = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_SDK_DIAMOND_SHOP, HashMap.class);
		logger.info("加载字典SdkDiamondShopDict,count=" + this.sdkDiamondShopMap.size());
	}

	public Map<Integer, SdkDiamondShopModel> getSdkDiamondShopMap() {
		return sdkDiamondShopMap;
	}
	
}
