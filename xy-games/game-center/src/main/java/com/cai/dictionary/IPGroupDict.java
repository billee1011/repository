/**
 * 
 */
package com.cai.dictionary;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author xwy
 *
 */
import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.IPGroupModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;

import javolution.util.FastMap;

/**
 *
 * @author xwy
 */
public class IPGroupDict {


    private Logger logger = LoggerFactory.getLogger(IPGroupDict.class);

    /**
     */
    private FastMap<Integer, FastMap<Integer, IPGroupModel>> ipgroupDictionary;

    /**
     */
    private FastMap<Integer, IPGroupModel> ipgroupMap;

    /**
     * 单例
     */
    private static IPGroupDict instance;

    /**
     * 私有构造
     */
    private IPGroupDict() {
    	ipgroupDictionary = new FastMap<Integer, FastMap<Integer, IPGroupModel>>();
    	ipgroupMap = new FastMap<Integer, IPGroupModel>();
    }

    /**
     * 单例模式
     *
     * @return 字典单例
     */
    public static IPGroupDict getInstance() {
        if (null == instance) {
            instance = new IPGroupDict();
        }

        return instance;
    }

    public void load() {
        PerformanceTimer timer = new PerformanceTimer();
        PublicService publicService = SpringService.getBean(PublicService.class);
        List<IPGroupModel> goodsList = publicService.getPublicDAO().getValidIPGroupModelList();
        ipgroupDictionary.clear();//把之前的clear掉
        ipgroupMap.clear();
        for (IPGroupModel model : goodsList) {
            if (!ipgroupDictionary.containsKey(model.getGame_type())) {
                FastMap<Integer, IPGroupModel> map = new FastMap<Integer, IPGroupModel>();
                ipgroupDictionary.put(model.getGame_type(), map);
            }
            ipgroupDictionary.get(model.getGame_type()).put(model.getId(), model);
            ipgroupMap.put(model.getId(), model);
        }


        //放入redis缓存
        RedisService redisService = SpringService.getBean(RedisService.class);
        redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_IP, ipgroupMap);


        logger.info("加载字典iplist,count=" + goodsList.size() + timer.getStr());
    }

    public FastMap<Integer, FastMap<Integer, IPGroupModel>> getIPGroupModelDictionary() {
        return ipgroupDictionary;
    }

    public void setIPModelDictionary(FastMap<Integer, FastMap<Integer, IPGroupModel>> shopModelDictionary) {
        this.ipgroupDictionary = shopModelDictionary;
    }

    public FastMap<Integer, IPGroupModel> getIPGroupModelMapByGameId(int game_id) {
        return ipgroupDictionary.get(game_id);
    }

    /**
     * @param shopID
     * @return
     */
    public IPGroupModel getIPGroupModelModel(int id) {
        return ipgroupMap.get(id);
    }

}
