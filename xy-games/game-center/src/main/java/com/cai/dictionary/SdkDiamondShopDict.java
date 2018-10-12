package com.cai.dictionary;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.sdk.SdkDiamondShopModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;

import javolution.util.FastMap;

/**
 * SDK钻石商城
 * @author chansonyan
 * 2018年9月25日
 */
public class SdkDiamondShopDict {
	
	
	private Logger logger = LoggerFactory.getLogger(SdkDiamondShopDict.class);

	private Map<Integer, SdkDiamondShopModel> sdkDiamondShopMap;
	
	private static SdkDiamondShopDict instance = new SdkDiamondShopDict();

	/**
	 * 私有构造
	 */
	private SdkDiamondShopDict() {
		sdkDiamondShopMap = new FastMap<>();
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static SdkDiamondShopDict getInstance() {
		if (null == instance) {
			instance = new SdkDiamondShopDict();
		}
		return instance;
	}

	public void load() {
		sdkDiamondShopMap.clear();//把之前的clear掉
		PerformanceTimer timer = new PerformanceTimer();
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<SdkDiamondShopModel> shopModelList = publicService.getPublicDAO().getAllSdkDiamondShopList();
		for(SdkDiamondShopModel temp : shopModelList) {
			this.sdkDiamondShopMap.put(temp.getId(), temp);
		}
		
		//放入redis缓存
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_SDK_DIAMOND_SHOP, sdkDiamondShopMap);
		
		logger.info("加载字典sdkDiamondShop,count=" + shopModelList.size() + timer.getStr());
	}

	public Map<Integer, SdkDiamondShopModel> getSdkDiamondShopMap() {
		return sdkDiamondShopMap;
	}

	public void setSdkDiamondShopMap(Map<Integer, SdkDiamondShopModel> sdkDiamondShopMap) {
		this.sdkDiamondShopMap = sdkDiamondShopMap;
	}

}
