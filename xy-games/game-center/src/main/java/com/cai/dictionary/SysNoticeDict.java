package com.cai.dictionary;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.SysNoticeModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;

import javolution.util.FastMap;

/**
 * 系统参数字典
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
		sysNoticeModelDictionary.clear();
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<SysNoticeModel> sysNoticeModelList = publicService.getPublicDAO().getSysNoticeModelList();
		for(SysNoticeModel model : sysNoticeModelList){
			if(!sysNoticeModelDictionary.containsKey(model.getGame_id())){
				FastMap<Integer,SysNoticeModel> map = new FastMap<Integer,SysNoticeModel>();
				sysNoticeModelDictionary.put(model.getGame_id(), map);
			}
			sysNoticeModelDictionary.get(model.getGame_id()).put(model.getNotice_id(), model);
		}
		
		//放入redis缓存
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_SYSNOTICE, sysNoticeModelDictionary);
		
		logger.info("加载字典SysNoticeDict,count=" + sysNoticeModelDictionary.size() + timer.getStr());
	}

	


	

	
	

}
