package com.cai.dictionary;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.ChannelModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.dao.PublicDAO;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;
import com.google.common.collect.Maps;

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

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		PublicService publicService = SpringService.getBean(PublicService.class);
		PublicDAO dao = publicService.getPublicDAO();
		List<ChannelModel> channelList = dao.getChannelDictModelList();

		// 放入redis缓存
		RedisService redisService = SpringService.getBean(RedisService.class);

		if (null != channelList) {
			Map<Integer, ChannelModel> dict = channelList.stream().collect(Collectors.toMap(ChannelModel::getChannelId, c -> c));

			redisService.hSet(RedisConstant.DICT, RedisConstant.CHANNEL_DICT, dict);

			channelGroup = dict;
		} else {
			redisService.hSet(RedisConstant.DICT, RedisConstant.CHANNEL_DICT, Maps.newHashMap());
		}

		logger.info("load-> channel cache update success time:{} !!", timer.getStr());
	}

	public ChannelModel getChannelModel(int channelId) {
		return null != channelGroup ? channelGroup.get(channelId) : null;
	}
}
