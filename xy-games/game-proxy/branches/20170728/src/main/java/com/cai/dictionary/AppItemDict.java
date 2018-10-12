/**
 * 
 */
package com.cai.dictionary;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.AppItem;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

import javolution.util.FastMap;

/**
 * app版本管理
 *
 * @author xwy
 */
public class AppItemDict {


    private Logger logger = LoggerFactory.getLogger(AppItemDict.class);

    private List<AppItem> appItemList ;
    private FastMap<Integer,ArrayList<AppItem>> appItemDictMap;

    /**
     * 单例
     */
    private static AppItemDict instance;

    /**
     * 私有构造
     */
    private AppItemDict() {
    	appItemList = new ArrayList<AppItem>();
    	appItemDictMap = new FastMap<Integer,ArrayList<AppItem>>();
    }
    /**
     * 单例模式
     *
     * @return 字典单例
     */
    public static AppItemDict getInstance() {
        if (null == instance) {
            instance = new AppItemDict();
        }

        return instance;
    }

    public void load() {
    	PerformanceTimer timer = new PerformanceTimer();
		try {
			RedisService redisService = SpringService.getBean(RedisService.class);
			List list1 =  redisService.hGet(RedisConstant.DICT, RedisConstant.APPITEMKEY,ArrayList.class);
			ArrayList<AppItem> list0 = new ArrayList<AppItem>();
			for(int i=0;i<list1.size();i++){
				AppItem appItem = JSONObject.parseObject(list1.get(i).toString(),AppItem.class);
				list0.add(appItem);
			}
			appItemList.clear();
			appItemList = list0;
			JSONObject object = redisService.hGet(RedisConstant.DICT, RedisConstant.APPITEMMAP, JSONObject.class);
			for(String string:object.keySet()){
				JSONArray array = object.getJSONArray(string);
				ArrayList<AppItem> list = new ArrayList<AppItem>();
				for(int i=0;i<array.size();i++){
					AppItem appItem =JSONObject.toJavaObject(array.getJSONObject(i), AppItem.class);
					list.add(appItem);
				}
				appItemDictMap.put(Integer.parseInt(string), list);
			}
			
		} catch (Exception e) {
			logger.error("error", e);
		}
		logger.info("redis缓存加载字典AppItemDict" + timer.getStr());
    }
	public List<AppItem> getAppItemList() {
		return appItemList;
	}

	public void setAppItemList(List<AppItem> appItemList) {
		this.appItemList = appItemList;
	}
	public FastMap<Integer, ArrayList<AppItem>> getAppItemDictMap() {
		return appItemDictMap;
	}
	public void setAppItemDictMap(FastMap<Integer, ArrayList<AppItem>> appItemDictMap) {
		this.appItemDictMap = appItemDictMap;
	}
    
}
