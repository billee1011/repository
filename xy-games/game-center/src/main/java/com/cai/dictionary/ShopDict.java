package com.cai.dictionary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.AppShopModel;
import com.cai.common.domain.ShopModel;
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
public class ShopDict {
	
	
	private Logger logger = LoggerFactory.getLogger(ShopDict.class);

	/**
	 * 商品缓存 game_id(id,model)
	 */
	private FastMap<Integer, FastMap<Integer, ShopModel>> shopModelDictionary;
	
	/**
	 * 商品缓存 (id,model)
	 */
	private FastMap<Integer, ShopModel> shopMap;
	
	/**
	 *app 商品缓存 game_id(id,model)
	 */
	private FastMap<Integer, FastMap<Integer, AppShopModel>> appshopModelDictionary;
	
	/**
	 * app商品缓存 (id,model)
	 */
	private FastMap<Integer, AppShopModel> appshopMap;
	

	/**
	 * 单例
	 */
	private static ShopDict instance;

	/**
	 * 私有构造
	 */
	private ShopDict() {
		shopModelDictionary = new FastMap<Integer, FastMap<Integer, ShopModel>>();
		shopMap = new FastMap<Integer, ShopModel>();
		
		appshopModelDictionary = new FastMap<Integer, FastMap<Integer, AppShopModel>>();
		appshopMap = new FastMap<Integer, AppShopModel>();
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static ShopDict getInstance() {
		if (null == instance) {
			instance = new ShopDict();
		}

		return instance;
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<ShopModel> shopModelList = publicService.getPublicDAO().getValidShopModelList();
		shopModelDictionary.clear();//把之前的clear掉
		shopMap.clear();
		for (ShopModel model : shopModelList) {
			if (!shopModelDictionary.containsKey(model.getGame_type())) {
				FastMap<Integer, ShopModel> map = new FastMap<Integer, ShopModel>();
				shopModelDictionary.put(model.getGame_type(), map);
			}
			shopModelDictionary.get(model.getGame_type()).put(model.getId(), model);
			shopMap.put(model.getId(), model);
		}
		
		//放入redis缓存
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_STORE, shopMap);
		
		
		
		List<AppShopModel> appshopModelList = publicService.getPublicDAO().getValidAppShopModelList();
		appshopModelDictionary.clear();//把之前的clear掉
		appshopMap.clear();
		for (AppShopModel model : appshopModelList) {
			if (!appshopModelDictionary.containsKey(model.getGame_type())) {
				FastMap<Integer, AppShopModel> map = new FastMap<Integer, AppShopModel>();
				appshopModelDictionary.put(model.getGame_type(), map);
			}
			appshopModelDictionary.get(model.getGame_type()).put(model.getShop_id(), model);
			appshopMap.put(model.getShop_id(), model);
		}
		
		//放入redis缓存
		redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_STORE_APP, appshopMap);
		
		logger.info("加载字典ShopDict,count=" + shopModelList.size() + timer.getStr());
	}

	public FastMap<Integer, FastMap<Integer, ShopModel>> getShopModelDictionary() {
		return shopModelDictionary;
	}

	public void setShopModelDictionary(FastMap<Integer, FastMap<Integer, ShopModel>> ShopModelDictionary) {
		this.shopModelDictionary = ShopModelDictionary;
	}

	public FastMap<Integer, ShopModel> getShopModelMapByGameId(int game_id) {
		return shopModelDictionary.get(game_id);
	}
	
	/**
	 * 根据商品Id 获取商品
	 * @param shopID
	 * @return
	 */
	public ShopModel getShopModel(int shopID) {
		return shopMap.get(shopID);
	}
	
	
	/**
	 * 根据商品Id 获取商品appstore
	 * @param shopID
	 * @return
	 */
	public AppShopModel getAppShopModel(int shopID) {
		return appshopMap.get(shopID);
	}

}
