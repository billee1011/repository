package com.cai.dictionary;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.AppShopModel;
import com.cai.common.domain.ShopModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.google.common.collect.Lists;

import javolution.util.FastMap;

/**
 * 商城字典
 * @author run
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
		try{
			
			shopMap.clear();
			shopModelDictionary.clear();
			
			appshopMap.clear();
			appshopModelDictionary.clear();
			
			RedisService redisService = SpringService.getBean(RedisService.class);
			shopMap = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_STORE, FastMap.class);
			for (ShopModel model : shopMap.values()) {
				if (!shopModelDictionary.containsKey(model.getGame_type())) {
					FastMap<Integer, ShopModel> map = new FastMap<Integer, ShopModel>();
					shopModelDictionary.put(model.getGame_type(), map);
				}
				shopModelDictionary.get(model.getGame_type()).put(model.getId(), model);
			}
			
			appshopMap = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_STORE_APP, FastMap.class);
			for (AppShopModel model : appshopMap.values()) {
				if (!appshopModelDictionary.containsKey(model.getGame_type())) {
					FastMap<Integer, AppShopModel> map = new FastMap<Integer, AppShopModel>();
					appshopModelDictionary.put(model.getGame_type(), map);
				}
				appshopModelDictionary.get(model.getGame_type()).put(model.getShop_id(), model);
			}
			
		}catch(Exception e){
			logger.error("error",e);
		}
		logger.info("redis缓存加载字典shop"  + timer.getStr());
	}
	
	
	/**
	 * 获取不同商品类型的商品列表
	 * @param game_id
	 * @param shop_type --商品类型
	 * @return
	 */
	public List<ShopModel> getShopModelByGameIdAndShopType(int game_id,int shop_type){
		
		List<ShopModel> shopModelList = Lists.newArrayList();
		for(ShopModel shopModel : shopMap.values()){
			if(shopModel.getGame_type()==0 || shopModel.getGame_type()==game_id){
				if(shopModel.isStatusEffect() && shop_type==shopModel.getShop_type()){
					shopModelList.add(shopModel);
				}
				
			}
		}
		//重新排序
		Collections.sort(shopModelList, new Comparator<ShopModel>() {
			public int compare(ShopModel p1, ShopModel p2) {
				// id 大到小
				Integer k1 = p1.getDisplay_order();
				Integer k2 = p2.getDisplay_order();
				return k2.compareTo(k1);
			}
		});
		return shopModelList;
	}
	
	/**
	 * 获取不同商品类型的商品列表
	 * @param game_id
	 * @param shop_type --商品类型
	 * @return
	 */
	public List<AppShopModel> getAppShopModelByGameIdAndShopType(int game_id,int shop_type){
		
		List<AppShopModel> shopModelList = Lists.newArrayList();
		for(AppShopModel shopModel : appshopMap.values()){
			if(shopModel.getGame_type()==0 || shopModel.getGame_type()==game_id){
				if(shopModel.isStatusEffect() && shop_type==shopModel.getShop_type()){
					shopModelList.add(shopModel);
				}
				
			}
		}
		//重新排序
		Collections.sort(shopModelList, new Comparator<AppShopModel>() {
			public int compare(AppShopModel p1, AppShopModel p2) {
				// id 大到小
				Integer k1 = p1.getDisplay_order();
				Integer k2 = p2.getDisplay_order();
				return k2.compareTo(k1);
			}
		});
		return shopModelList;
	}

	public FastMap<Integer, FastMap<Integer, ShopModel>> getShopModelDictionary() {
		return shopModelDictionary;
	}

	public FastMap<Integer, ShopModel> getShopMap() {
		return shopMap;
	}
	
	/**
	 * 根据商品Id 获取商品
	 * @param shopID
	 * @return
	 */
	public ShopModel getShopModel(int shopID) {
		return shopMap.get(shopID);
	}
}
