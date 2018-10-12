package com.cai.dictionary;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.MoneyShopModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.google.common.collect.Lists;

import javolution.util.FastMap;

/**
 * 金币商城字典
 */
public class MoneyShopDict {

    private Logger logger = LoggerFactory.getLogger(MoneyShopDict.class);

    /**
     * 商品缓存 game_id(id,model)
     */
    private FastMap<Integer, FastMap<Integer, MoneyShopModel>> shopModelDictionary;

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
        shopModelDictionary = new FastMap<Integer, FastMap<Integer, MoneyShopModel>>();
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
        try {

            moneyShopMap.clear();
            shopModelDictionary.clear();

            RedisService redisService = SpringService.getBean(RedisService.class);
            moneyShopMap = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_STORE_MONEY, FastMap.class);
            for (MoneyShopModel model : moneyShopMap.values()) {
                if (!shopModelDictionary.containsKey(model.getGameType())) {
                    FastMap<Integer, MoneyShopModel> map = new FastMap<Integer, MoneyShopModel>();
                    shopModelDictionary.put(model.getGameType(), map);
                }
                shopModelDictionary.get(model.getGameType()).put(model.getId(), model);
            }

        } catch (Exception e) {
            logger.error("error", e);
        }
        logger.info("redis缓存加载字典moenyshop" + timer.getStr());
    }


    /**
     *
     * @param game_id
     * @return
     */
    public List<MoneyShopModel> getShopModelByGameIdAndShopType(int game_id) {

        List<MoneyShopModel> shopModelList = Lists.newArrayList();
        for (MoneyShopModel shopModel : moneyShopMap.values()) {
            if (shopModel.getGameType() == 0 || shopModel.getGameType() == game_id) {
                if (shopModel.isStatusEffect()) {
                    shopModelList.add(shopModel);
                }

            }
        }
        //重新排序
        Collections.sort(shopModelList, new Comparator<MoneyShopModel>() {
            public int compare(MoneyShopModel p1, MoneyShopModel p2) {
                // id 大到小
                Integer k1 = p1.getDisplayOrder();
                Integer k2 = p2.getDisplayOrder();
                return k2.compareTo(k1);
            }
        });
        return shopModelList;
    }

    public FastMap<Integer, FastMap<Integer, MoneyShopModel>> getShopModelDictionary() {
        return shopModelDictionary;
    }

    public FastMap<Integer, MoneyShopModel> getMoneyShopMap() {
        return moneyShopMap;
    }


}
