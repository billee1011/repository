/**
 *
 */
package com.cai.dictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.AppItem;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.module.LoginModule;
import com.cai.redis.service.RedisService;
import com.google.common.collect.Lists;

import javolution.util.FastMap;

/**
 * app版本管理
 *
 * @author xwy
 */
public class AppItemDict {

	private Logger logger = LoggerFactory.getLogger(AppItemDict.class);

	private List<AppItem> appItemList;
	private FastMap<Integer, ArrayList<AppItem>> appItemDictMap;

	/**
	 * 临时变量
	 */
	private volatile long autoIncreVersion;

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
		PerformanceTimer timer = new PerformanceTimer();
		try {
			RedisService redisService = SpringService.getBean(RedisService.class);
			List list1 = redisService.hGet(RedisConstant.DICT, RedisConstant.APPITEMKEY, ArrayList.class);

			ArrayList<AppItem> list0 = new ArrayList<AppItem>();
			for (int i = 0; i < list1.size(); i++) {
				AppItem appItem = JSONObject.parseObject(list1.get(i).toString(), AppItem.class);
				list0.add(appItem);
			}

			// 过滤出和上次有区别的子游戏,需要通知客户端
			List<AppItem> updateApps = filterUpdateApps(appItemList, list0);
			if (null != updateApps && !updateApps.isEmpty()) {
				LoginModule.syncUpdateAppItems(updateApps);
			}

			appItemList.clear();
			appItemList = list0;
			JSONObject object = redisService.hGet(RedisConstant.DICT, RedisConstant.APPITEMMAP, JSONObject.class);
			for (String string : object.keySet()) {
				JSONArray array = object.getJSONArray(string);
				ArrayList<AppItem> list = new ArrayList<AppItem>();
				for (int i = 0; i < array.size(); i++) {
					AppItem appItem = JSONObject.toJavaObject(array.getJSONObject(i), AppItem.class);
					list.add(appItem);
				}
				appItemDictMap.put(Integer.parseInt(string), list);
			}

			String version = redisService.get(RedisConstant.APPITEM_TMP_VERSION);
			if (StringUtils.isNotEmpty(version)) {
				try {
					autoIncreVersion = Long.parseLong(version);
				} catch (Exception e) {
					e.printStackTrace();
				}
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

	/**
	 * 过滤出有变化的的app列表
	 *
	 * @return
	 */
	private List<AppItem> filterUpdateApps(List<AppItem> cacheAppItems, ArrayList<AppItem> newAppItems) {

		List<AppItem> list = Lists.newArrayList();

		final Map<Integer, AppItem> cacheAppMaps = cacheAppItems.stream().collect(Collectors.toMap(AppItem::getAppId, AppItem -> AppItem));
		newAppItems.forEach((newApp) -> {
			AppItem app = cacheAppMaps.get(newApp.getAppId());
			if (null == app || !isSameVesionApp(newApp, app)) {
				list.add(newApp);
			}
		});
		return list;
	}

	/**
	 * 是否相同的app
	 *
	 * @param app1
	 * @param app2
	 * @return
	 */
	private static boolean isSameVesionApp(AppItem app1, AppItem app2) {
		return java.util.Objects.equals(app1, app2);
	}

	public long getAutoIncreVersion() {
		return autoIncreVersion;
	}

}
