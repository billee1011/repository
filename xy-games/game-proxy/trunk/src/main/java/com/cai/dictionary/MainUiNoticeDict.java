package com.cai.dictionary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.MainUiNoticeModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

import javolution.util.FastMap;

/**
 * 主界面公告字典
 * @author run
 *
 */
public class MainUiNoticeDict {

	private Logger logger = LoggerFactory.getLogger(MainUiNoticeDict.class);
	
	/**
	 * 系统参数缓存 game_id(id,model)
	 */
	private FastMap<Integer,FastMap<Integer,MainUiNoticeModel>> mainUiNoticeDictionary;

	/**
	 * 单例
	 */
	private static MainUiNoticeDict instance;

	/**
	 * 私有构造
	 */
	private MainUiNoticeDict() {
		mainUiNoticeDictionary = new FastMap<Integer,FastMap<Integer,MainUiNoticeModel>>();
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static MainUiNoticeDict getInstance() {
		if (null == instance) {
			instance = new MainUiNoticeDict();
		}

		return instance;
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		try{
			RedisService redisService = SpringService.getBean(RedisService.class);
			mainUiNoticeDictionary = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_MAIN_UI_NOTICE, FastMap.class);
		}catch(Exception e){
			logger.error("error",e);
		}
		logger.info("redis缓存加载字典MainUiNoticeDict"  + timer.getStr());
	}

	public FastMap<Integer, FastMap<Integer, MainUiNoticeModel>> getMainUiNoticeDictionary() {
		return mainUiNoticeDictionary;
	}



	

	
	

}
