package com.cai.dictionary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.sdk.SdkApp;
import com.cai.common.domain.sdk.SdkShop;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;

import javolution.util.FastMap;

/**
 * 商品字典
 * @author xwy
 * @date 2016年11月21日 -- 上午10:00:28
 *
 */
public class SdkAppDict {
	
	
	private Logger logger = LoggerFactory.getLogger(SdkAppDict.class);

	private Map<Long, SdkApp> sdkAppMap;
	
	private Map<Long, Map<String, SdkShop>> sdkShopMap;

	private static SdkAppDict instance = new SdkAppDict();

	/**
	 * 私有构造
	 */
	private SdkAppDict() {
		sdkAppMap = new FastMap<>();
		sdkShopMap = new FastMap<>();
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static SdkAppDict getInstance() {
		if (null == instance) {
			instance = new SdkAppDict();
		}
		return instance;
	}

	public void load() {
		sdkAppMap.clear();//把之前的clear掉
		sdkShopMap.clear();
		PerformanceTimer timer = new PerformanceTimer();
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<SdkApp> shopModelList = publicService.getPublicDAO().getAllSdkAppList();
		for(SdkApp temp : shopModelList) {
			this.sdkAppMap.put(temp.getAppId(), temp);
		}
		
		//加载APP商品
		List<SdkShop> sdkShopList = publicService.getPublicDAO().getAllSdkAppShopList();
		for(SdkShop sdkShop : sdkShopList) {
			Map<String, SdkShop> shopMap = this.sdkShopMap.get(sdkShop.getAppId());
			if(null == shopMap) {
				shopMap = new HashMap<>();
				this.sdkShopMap.put(sdkShop.getAppId(), shopMap);
			}
			shopMap.put(sdkShop.getItemId(), sdkShop);
		}
		
		//放入redis缓存
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_SDK_APP, sdkAppMap);
		redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_SDK_APP_SHOP, sdkShopMap);
		
		logger.info("加载字典SdkAppDict,count=" + shopModelList.size() + timer.getStr());
	}
	
	public SdkShop getShopById(long appId, String itemId) {
		Map<String, SdkShop> map = sdkShopMap.get(appId);
		if(null == map) {
			return null;
		}
		return map.get(itemId);
	}
	

}
