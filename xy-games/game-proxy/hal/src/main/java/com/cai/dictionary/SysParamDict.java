package com.cai.dictionary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.RedisServiceImpl;

import javolution.util.FastMap;

/**
 * 系统参数字典
 * @author run
 *
 */
public class SysParamDict {

	private Logger logger = LoggerFactory.getLogger(SysParamDict.class);
	
	/**
	 * 系统参数缓存 game_id(id,model)
	 */
	private FastMap<Integer,FastMap<Integer,SysParamModel>> sysParamModelDictionary;

	/**
	 * 单例
	 */
	private static SysParamDict instance;

	/**
	 * 私有构造
	 */
	private SysParamDict() {
		sysParamModelDictionary = new FastMap<Integer,FastMap<Integer,SysParamModel>>();
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static SysParamDict getInstance() {
		if (null == instance) {
			instance = new SysParamDict();
		}

		return instance;
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		try{
			RedisService redisService = SpringService.getBean(RedisService.class);
			sysParamModelDictionary = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_SYSPARAM, FastMap.class);
		}catch(Exception e){
			logger.error("error",e);
		}
		logger.info("redis缓存加载加载字典sys_param"  + timer.getStr());
	}
	
	public FastMap<Integer, SysParamModel> getSysParamModelDictionaryByGameId(int game_id){
		return sysParamModelDictionary.get(game_id);
	}

	public FastMap<Integer, FastMap<Integer, SysParamModel>> getSysParamModelDictionary() {
		return sysParamModelDictionary;
	}

	public void setSysParamModelDictionary(FastMap<Integer, FastMap<Integer, SysParamModel>> sysParamModelDictionary) {
		this.sysParamModelDictionary = sysParamModelDictionary;
	}


	

	
	

}
