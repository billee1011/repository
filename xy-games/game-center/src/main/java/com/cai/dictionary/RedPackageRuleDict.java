package com.cai.dictionary;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.RedPackageActivityModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;
import com.cai.service.RedPackageServiceImp;

/**
 * 活动字典
 *
 */
public class RedPackageRuleDict {

	private Logger logger = LoggerFactory.getLogger(RedPackageRuleDict.class);
	
	private ConcurrentHashMap<Integer,RedPackageActivityModel> redPackageRuleDictionary;

	/**
	 * 单例
	 */
	private static RedPackageRuleDict instance;
	private SimpleDateFormat dateFormat = null;
	/**
	 * 私有构造
	 */
	private RedPackageRuleDict() {
		redPackageRuleDictionary = new ConcurrentHashMap<Integer,RedPackageActivityModel>();
		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static RedPackageRuleDict getInstance() {
		if (null == instance) {
			instance = new RedPackageRuleDict();
		}

		return instance;
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<RedPackageActivityModel> redPackageModelList = publicService.getPublicDAO().getRedPackageModelList();
		long nowTime = System.currentTimeMillis();
		if(redPackageModelList.size()>0){
			for(RedPackageActivityModel model : redPackageModelList){
				String[] daysTimeArea = model.getActivity_time().split("\\|");
				for(String dayArea:daysTimeArea){
					long endTime;
					try {
						endTime = dateFormat.parse(dayArea.split("\\~")[1]).getTime();
						if(endTime > nowTime){//踢掉活动过期但是未下线的活动
							redPackageRuleDictionary.put(model.getActivity_type(), model);
							break;
						}
					} catch (ParseException e) {
						e.printStackTrace();
					}
					
				}
			}
			try{
				RedPackageServiceImp.getInstance().dispatchRedPackage();
			}catch(Exception e){
				logger.info("分配红包库存失败",e);
			}
			//放入redis缓存
			RedisService redisService = SpringService.getBean(RedisService.class);
			redisService.hSet(RedisConstant.DICT, RedisConstant.DIR_RED_PACKAGE_RULE, redPackageRuleDictionary);
			logger.info("加载字典redPackageRuleDictionary success");
		}
		logger.info("加载字典redPackageRuleDictionary,count=" + redPackageModelList.size() + timer.getStr());
	}

	public ConcurrentHashMap<Integer, RedPackageActivityModel> getRedPackageRuleDictionary() {
		return redPackageRuleDictionary;
	}


}
