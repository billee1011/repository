package com.cai.dictionary;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.GoodsModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.google.common.collect.Lists;

import javolution.util.FastMap;

/**
 * 道具字典
 */
public class GoodsDict {

	private Logger logger = LoggerFactory.getLogger(GoodsDict.class);

	/**
	 * 商品缓存 game_id(id,model)
	 */
	private FastMap<Integer, FastMap<Integer, GoodsModel>> goodsDictionary;

	/**
	 * 商品缓存 (id,model)
	 */
	private FastMap<Integer, GoodsModel> goodMap;
	
    /**
     * 商品缓存 (id,model)
     */
    private FastMap<Integer, GoodsModel> unionGoodsMap;

	/**
	 * 单例
	 */
	private static GoodsDict instance;

	/**
	 * 私有构造
	 */
	private GoodsDict() {
		goodsDictionary = new FastMap<Integer, FastMap<Integer, GoodsModel>>();
		goodMap = new FastMap<Integer, GoodsModel>();
		unionGoodsMap = new FastMap<Integer, GoodsModel>();
	}

	/**
	 * 单例模式
	 *
	 * @return 字典单例
	 */
	public static GoodsDict getInstance() {
		if (null == instance) {
			instance = new GoodsDict();
		}

		return instance;
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		try {

			goodMap.clear();
			goodsDictionary.clear();
			if(unionGoodsMap!=null) unionGoodsMap.clear();

			RedisService redisService = SpringService.getBean(RedisService.class);
			goodMap = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_GOODS, FastMap.class);
			
			unionGoodsMap = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_GOODS_UNION, FastMap.class);
			
			
			if(unionGoodsMap!=null && !unionGoodsMap.isEmpty()) {//保持代码的兼容性--主键作为key，包含子游戏的所有商品
				for (GoodsModel model : unionGoodsMap.values()) {
					if (!goodsDictionary.containsKey(model.getGame_type())) {
						FastMap<Integer, GoodsModel> map = new FastMap<Integer, GoodsModel>();
						goodsDictionary.put(model.getGame_type(), map);
					}
					goodsDictionary.get(model.getGame_type()).put(model.getGoodID(), model);
				}
				
			}else {
				for (GoodsModel model : goodMap.values()) {
					if (!goodsDictionary.containsKey(model.getGame_type())) {
						FastMap<Integer, GoodsModel> map = new FastMap<Integer, GoodsModel>();
						goodsDictionary.put(model.getGame_type(), map);
					}
					goodsDictionary.get(model.getGame_type()).put(model.getGoodID(), model);
				}
				
			}

		} catch (Exception e) {
			logger.error("error", e);
		}
		logger.info("redis缓存加载字典goodMap" + timer.getStr());
	}

	/**
	 *
	 * @param game_id
	 * @return
	 */
	public List<GoodsModel> getGoodsModelByGameIdAndShopType(int game_id) {
		List<GoodsModel> shopModelList = Lists.newArrayList();
		
		if(unionGoodsMap!=null && !unionGoodsMap.isEmpty()){
			 FastMap<Integer, GoodsModel> map = goodsDictionary.get(game_id);
			 if(map==null || map.isEmpty()) {
				 map = goodsDictionary.get(0);//取不到再取大厅的
			 }
			 if(map!=null && !map.isEmpty()) {
				 shopModelList.addAll(map.values());
			 }
		}else {
			for (GoodsModel shopModel : goodMap.values()) {
				if (shopModel.getGame_type() == 0 || shopModel.getGame_type() == game_id) {
					if (shopModel.getState() == 1) {
						shopModelList.add(shopModel);
					}

				}
			}
		}
		return shopModelList;
		

		
		
	}
	
	public GoodsModel getGoodsModelByGameIdAndGoodsId(int game_id, int goods_id){
		
		if(unionGoodsMap!=null && !unionGoodsMap.isEmpty()){
			FastMap<Integer, GoodsModel> map = goodsDictionary.get(game_id);
			 if(map==null || map.isEmpty()) {
				 map = goodsDictionary.get(0);//取不到再取大厅的
			 }
			 if(map!=null && !map.isEmpty()) {
				 return map.get(goods_id);
			 }
		}
		
		
		for (GoodsModel shopModel : goodMap.values()) {//以下方式后面废弃
			if (shopModel.getGame_type() == 0 || shopModel.getGame_type() == game_id) {
				if (shopModel.getState() == 1 && shopModel.getGoodID() == goods_id) {
					return shopModel;
				}

			}
		}	
		return null;
	}

	public FastMap<Integer, FastMap<Integer, GoodsModel>> getGoodsModelDictionary() {
		return goodsDictionary;
	}
	
	
	public FastMap<Integer, GoodsModel> getGoodsModelMapByGameID(int gameID) {
		FastMap<Integer, GoodsModel> map = goodsDictionary.get(gameID);
		return map==null?new FastMap<Integer, GoodsModel>():map;
	}

	public FastMap<Integer, GoodsModel> getGoodsShopMap() {
		return goodMap;
	}

}
