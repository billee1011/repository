package com.cai.dictionary;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.TurntableActiveModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

/**
 * 转盘字典
 */
public class TurntableDict {

	private Logger logger = LoggerFactory.getLogger(TurntableDict.class);

	private Map<Integer, TurntableActiveModel> datas;

	private static TurntableDict instance = new TurntableDict();;

	public static TurntableDict getInstance() {
		return instance;
	}

	private TurntableDict() {
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		RedisService redisService = SpringService.getBean(RedisService.class);
		@SuppressWarnings("unchecked")
		List<TurntableActiveModel> activitys = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_TURUNTABLE_AWARD_POOL, ArrayList.class);
		if(activitys!=null) {
			datas = activitys.stream().collect(Collectors.toMap(TurntableActiveModel::getId, temp -> temp));
		}
		logger.info("加载缓存数据 turntableDict timer" + timer.getStr());

	}

	public boolean checkHasActiveModel() {
	
		Date date = new Date();
		if(datas!=null) {
			for (TurntableActiveModel active : datas.values()) {
				if (active.getState() != 1) {
					continue;
				}

				if (date.before(active.getStart_time()) || date.after(active.getEnd_time())) {
					continue;
				}
				return true;
			}
			return false;
		}
		return false;
	}

}
