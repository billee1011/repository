package com.cai.dictionary;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.ChannelModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

/**
 * 渠道
 * 
 *
 * @author wu_hc date: 2018年5月14日 上午11:43:53 <br/>
 */
public class ChannelModelDict {

	private Logger logger = LoggerFactory.getLogger(ChannelModelDict.class);
	private static final ChannelModelDict INStance = new ChannelModelDict();

	private volatile Map<Integer, ChannelModel> channelGroup = null;

	private ChannelModelDict() {
	}

	public static ChannelModelDict getInstance() {
		return INStance;
	}

	@SuppressWarnings("unchecked")
	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		try {
			RedisService redisService = SpringService.getBean(RedisService.class);
			Map<Integer, ChannelModel> modelGroup = redisService.hGet(RedisConstant.DICT, RedisConstant.CHANNEL_DICT, Map.class);

			channelGroup = modelGroup;
		} catch (Exception e) {
			logger.error("error", e);
		}
		logger.info("redis缓存加载字典ChannelDict" + timer.getStr());
	}

	public ChannelModel getChannelModel(int channelId) {
		return null != channelGroup ? channelGroup.get(channelId) : null;
	}
}
