/**
 * 
 */
package com.cai.dictionary;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.GoodsModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;
import javolution.util.FastMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 金币商品字典
 *
 * @author xwy
 */
public class GoodsDict {


    private Logger logger = LoggerFactory.getLogger(GoodsDict.class);

    /**
     * 道具缓存 game_id(id,model)
     */
    private FastMap<Integer, FastMap<Integer, GoodsModel>> goodsDictionary;

    /**
     * 商品缓存 (id,model) -----之后要废弃
     */
    private FastMap<Integer, GoodsModel> goodsMap;
    
    
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
    	goodsMap = new FastMap<Integer, GoodsModel>();
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
        try {
			PerformanceTimer timer = new PerformanceTimer();
			PublicService publicService = SpringService.getBean(PublicService.class);
			List<GoodsModel> goodsList = publicService.getPublicDAO().getValidGoodsModelList();
			goodsDictionary.clear();//把之前的clear掉
			goodsMap.clear();
			
			if(unionGoodsMap!=null) unionGoodsMap.clear();
			
			for (GoodsModel model : goodsList) {
			    if (!goodsDictionary.containsKey(model.getGame_type())) {
			        FastMap<Integer, GoodsModel> map = new FastMap<Integer, GoodsModel>();
			        goodsDictionary.put(model.getGame_type(), map);
			    }
			    goodsDictionary.get(model.getGame_type()).put(model.getGoodID(), model);
			    goodsMap.put(model.getGoodID(), model);
			    
			    
			    unionGoodsMap.put(model.getId(), model);
			}


			//放入redis缓存
			RedisService redisService = SpringService.getBean(RedisService.class);
			redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_GOODS, goodsMap);
			
			redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_GOODS_UNION, unionGoodsMap);


			logger.info("加载字典goodsList,count=" + goodsList.size() + timer.getStr());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public FastMap<Integer, FastMap<Integer, GoodsModel>> getGoodsModelDictionary() {
        return goodsDictionary;
    }

    public void setMoneyShopModelDictionary(FastMap<Integer, FastMap<Integer, GoodsModel>> shopModelDictionary) {
        this.goodsDictionary = shopModelDictionary;
    }

    public FastMap<Integer, GoodsModel> getGoodsModelMapByGameId(int game_id) {
        return goodsDictionary.get(game_id);
    }

    /**
     * 根据商品Id 获取商品
     *
     * @param shopID
     * @return
     */
    public GoodsModel getGoodsShopModel(int shopID) {
        return goodsMap.get(shopID);
    }

}
