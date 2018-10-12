/**
 * 
 */
package com.cai.dictionary;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.AppItem;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;

import javolution.util.FastMap;

/**
 * app版本管理
 *
 * @author tb
 */
public class AppItemDict {

	private Logger logger = LoggerFactory.getLogger(AppItemDict.class);

	private ArrayList<AppItem> appItemList;
	private FastMap<Integer, ArrayList<AppItem>> appItemDictMap;

	/**
	 * 单例
	 */
	private static AppItemDict instance;

	/**
	 * 私有构造
	 */
	private AppItemDict() {
		appItemList = new ArrayList<AppItem>();
		appItemDictMap = new FastMap<Integer, ArrayList<AppItem>>();
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
		try {
			PerformanceTimer timer = new PerformanceTimer();
			PublicService publicService = SpringService.getBean(PublicService.class);
			ArrayList<AppItem> applist = (ArrayList<AppItem>) publicService.getPublicDAO().getAllAppItemList();
			appItemDictMap.clear();
			for (AppItem appItem : applist) {
				appItem.setCity(CityDict.getInstance().cityCodeToAdcode(appItem.getCity()));
				appItem.setNot_fit_city(CityDict.getInstance().cityCodeToAdcode(appItem.getNot_fit_city()));
				
				ArrayList<AppItem> appAllList = (ArrayList<AppItem>) publicService.getPublicDAO().getAppItemListByAppId(appItem.getAppId());
				if (appAllList != null) {
					for(AppItem appModel:appAllList){
						appModel.setCity(CityDict.getInstance().cityCodeToAdcode(appModel.getCity()));
						appModel.setNot_fit_city(CityDict.getInstance().cityCodeToAdcode(appModel.getNot_fit_city()));
					}
					appItemDictMap.put(appItem.getAppId(), appAllList);
				}
			}
			appItemList.clear();// 把之前的clear掉
			appItemList = applist;
			// 放入redis缓存
			RedisService redisService = SpringService.getBean(RedisService.class);
			redisService.hSet(RedisConstant.DICT, RedisConstant.APPITEMKEY, JSONObject.toJSON(appItemList));
			redisService.hSet(RedisConstant.DICT, RedisConstant.APPITEMMAP, JSONObject.toJSON(appItemDictMap));

			// 新增版本号
			try {
				redisService.incr(RedisConstant.APPITEM_TMP_VERSION);
			} catch (Exception e) {
				e.printStackTrace();
			}

			logger.info("加载字典appItemList,count=" + appItemList.size() + timer.getStr());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public ArrayList<AppItem> getAppItemList() {
		return appItemList;
	}

	public void setAppItemList(ArrayList<AppItem> appItemList) {
		this.appItemList = appItemList;
	}

}
