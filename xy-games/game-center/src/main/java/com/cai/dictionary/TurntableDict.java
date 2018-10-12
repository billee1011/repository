package com.cai.dictionary;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.TurntableActiveModel;
import com.cai.common.domain.TurntablePrizeModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;

/**
 * 转盘字典
 * 
 * @author yu
 *
 */
public class TurntableDict {

	private Logger logger = LoggerFactory.getLogger(TurntableDict.class);

	List<TurntableActiveModel> activitys;

	private static TurntableDict instance;

	public synchronized static TurntableDict getInstance() {
		if (instance == null) {
			instance = new TurntableDict();
		}
		return instance;
	}

	private TurntableDict() {
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		PublicService publicService = SpringService.getBean(PublicService.class);
		activitys = publicService.getPublicDAO().getTurntableActiveModelList();
		
		List<TurntablePrizeModel> prizes  = publicService.getPublicDAO().getTurntablePrizeModelList();
		
		activitys.forEach((activity)->{
			prizes.forEach((prize)->{
				if(prize.getActive_id() == activity.getId()){
					activity.getPrizes().add(prize);
				}
			});
		});
		
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_TURUNTABLE_AWARD_POOL, activitys);

		logger.info("加载字典TurntableDict,count=" + activitys.size() + timer.getStr());
	}

//	public List<TurntableSystemModel> getGoods() {
//		return goods;
//	}
}
