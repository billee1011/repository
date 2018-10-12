package com.cai.dictionary;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.RecommendLimitModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.service.PublicService;
import com.google.common.collect.Maps;

/**
 * 推广员限制字典
 * 
 * @author tang
 *
 */
public class RecommendLimitDict {

	private Logger logger = LoggerFactory.getLogger(ActivityDict.class);

	private Map<Long, RecommendLimitModel> limitMap;

	private static RecommendLimitDict instance;

	public synchronized static RecommendLimitDict getInstance() {
		if (instance == null) {
			instance = new RecommendLimitDict();
		}
		return instance;
	}

	private RecommendLimitDict() {
		limitMap = Maps.newHashMap();
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<RecommendLimitModel> limitList = publicService.getPublicDAO().getRecommendLimitModelList();
		limitMap.clear();
		for (RecommendLimitModel model : limitList)
			limitMap.put(model.getAccount_id(), model);
		logger.info("加载字典RecommendLimitModel list,count=" + limitList.size() + timer.getStr());
	}
	
	public RecommendLimitModel getRecommendLimitModelById(long accountId){
		return limitMap.get(accountId);
	}
}
