package com.cai.dictionary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.SysNoticeModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

import javolution.util.FastMap;

/**
 * 系统公告字典
 * @author run
 *
 */
public class SysNoticeDict {

	private Logger logger = LoggerFactory.getLogger(SysNoticeDict.class);
	
	/**
	 * 系统参数缓存 game_id(id,model)
	 */
	private FastMap<Integer,FastMap<Integer,SysNoticeModel>> sysNoticeModelDictionary;

	/**
	 * 单例
	 */
	private static SysNoticeDict instance;

	/**
	 * 私有构造
	 */
	private SysNoticeDict() {
		sysNoticeModelDictionary = new FastMap<Integer,FastMap<Integer,SysNoticeModel>>();
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static SysNoticeDict getInstance() {
		if (null == instance) {
			instance = new SysNoticeDict();
		}

		return instance;
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		try{
			RedisService redisService = SpringService.getBean(RedisService.class);
			sysNoticeModelDictionary = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_SYSNOTICE, FastMap.class);
		}catch(Exception e){
			logger.error("error",e);
		}
		logger.info("redis缓存加载字典sys_notice"  + timer.getStr());
	}

	public FastMap<Integer, FastMap<Integer, SysNoticeModel>> getSysNoticeModelDictionary() {
		return sysNoticeModelDictionary;
	}

	public void setSysNoticeModelDictionary(FastMap<Integer, FastMap<Integer, SysNoticeModel>> sysNoticeModelDictionary) {
		this.sysNoticeModelDictionary = sysNoticeModelDictionary;
	}
	


	

	
	

}
