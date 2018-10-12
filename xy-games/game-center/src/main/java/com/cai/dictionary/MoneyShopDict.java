package com.cai.dictionary;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.MoneyShopModel;
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
public class MoneyShopDict {


    private Logger logger = LoggerFactory.getLogger(MoneyShopDict.class);

    /**
     * 商品缓存 game_id(id,model)
     */
    private FastMap<Integer, FastMap<Integer, MoneyShopModel>> moneyShopModelDictionary;

    /**
     * 商品缓存 (id,model)
     */
    private FastMap<Integer, MoneyShopModel> moneyShopMap;

    /**
     * 单例
     */
    private static MoneyShopDict instance;

    /**
     * 私有构造
     */
    private MoneyShopDict() {
        moneyShopModelDictionary = new FastMap<Integer, FastMap<Integer, MoneyShopModel>>();
        moneyShopMap = new FastMap<Integer, MoneyShopModel>();
    }

    /**
     * 单例模式
     *
     * @return 字典单例
     */
    public static MoneyShopDict getInstance() {
        if (null == instance) {
            instance = new MoneyShopDict();
        }

        return instance;
    }

    public void load() {
        PerformanceTimer timer = new PerformanceTimer();
        PublicService publicService = SpringService.getBean(PublicService.class);
        List<MoneyShopModel> shopModelList = publicService.getPublicDAO().getValidMoneyShopModelList();
        moneyShopModelDictionary.clear();//把之前的clear掉
        moneyShopMap.clear();
        for (MoneyShopModel model : shopModelList) {
            if (!moneyShopModelDictionary.containsKey(model.getGameType())) {
                FastMap<Integer, MoneyShopModel> map = new FastMap<Integer, MoneyShopModel>();
                moneyShopModelDictionary.put(model.getGameType(), map);
            }
            moneyShopModelDictionary.get(model.getGameType()).put(model.getId(), model);
            moneyShopMap.put(model.getId(), model);
        }


        //放入redis缓存
        RedisService redisService = SpringService.getBean(RedisService.class);
        redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_STORE_MONEY, moneyShopMap);


        logger.info("加载字典MoneyShopDict,count=" + shopModelList.size() + timer.getStr());
    }

    public FastMap<Integer, FastMap<Integer, MoneyShopModel>> getMoneyShopModelDictionary() {
        return moneyShopModelDictionary;
    }

    public void setMoneyShopModelDictionary(FastMap<Integer, FastMap<Integer, MoneyShopModel>> shopModelDictionary) {
        this.moneyShopModelDictionary = shopModelDictionary;
    }

    public FastMap<Integer, MoneyShopModel> getMoneyShopModelMapByGameId(int game_id) {
        return moneyShopModelDictionary.get(game_id);
    }

    /**
     * 根据商品Id 获取商品
     *
     * @param shopID
     * @return
     */
    public MoneyShopModel getMoneyShopModel(int shopID) {
        return moneyShopMap.get(shopID);
    }

}
