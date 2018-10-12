/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.dictionary;

import java.util.Collections;
import java.util.Map;

import com.cai.common.constant.RedisConstant;
import com.cai.common.dictionary.DictHolder;
import com.cai.common.domain.GateServerModel;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

/**
 * 服务器处理
 * 
 * @author wu_hc
 */
public class ServerDict {
	/**
	 * 
	 * /** 网关服
	 */
	private volatile Map<Integer, GateServerModel> gateServerModelDict;

	/**
	 * 单例
	 */
	private final static ServerDict instance = new ServerDict();

	/**
	 * 私有构造
	 */
	private ServerDict() {
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static ServerDict getInstance() {
		return instance;
	}

	/**
	 * 
	 */
	public void load() {

		RedisService redisService = SpringService.getBean(RedisService.class);
		DictHolder gateHolder = redisService.hGet(RedisConstant.DICT, RedisConstant.DIR_SERVER_GATE, DictHolder.class);
		if (null != gateHolder) {
			gateServerModelDict = gateHolder.getDicts();
		}
	}

	/**
	 * 
	 * @return
	 */
	public Map<Integer, GateServerModel> getGateServerDict() {
		if (null == gateServerModelDict) {
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(gateServerModelDict);
	}
}
