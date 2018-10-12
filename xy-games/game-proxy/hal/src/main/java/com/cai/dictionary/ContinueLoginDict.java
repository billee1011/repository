package com.cai.dictionary;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.ContinueLoginModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

import javolution.util.FastMap;
import protobuf.clazz.Protocol.ContinueLoginResponse;

/**
 * 连续登录
 *
 */
public class ContinueLoginDict {

	private Logger logger = LoggerFactory.getLogger(ContinueLoginDict.class);

	private FastMap<Integer, FastMap<Integer, ContinueLoginModel>> continueLoginDictionary;

	/**
	 * 单例
	 */
	private static ContinueLoginDict instance;

	/**
	 * 私有构造
	 */
	private ContinueLoginDict() {
		continueLoginDictionary = new FastMap<Integer, FastMap<Integer, ContinueLoginModel>>();
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static ContinueLoginDict getInstance() {
		if (null == instance) {
			instance = new ContinueLoginDict();
		}

		return instance;
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		try {
			RedisService redisService = SpringService.getBean(RedisService.class);
			continueLoginDictionary = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_CONTINUE_LOGIN,
					FastMap.class);
		} catch (Exception e) {
			logger.error("error", e);
		}
		logger.info("redis缓存加载字典continueLoginDict" + timer.getStr());
	}

	public FastMap<Integer, FastMap<Integer, ContinueLoginModel>> getContinueLoginDictionary() {
		return continueLoginDictionary;
	}
	
	
	public ContinueLoginModel getContinueLoginModel(int gameID,int day) {
		FastMap<Integer, ContinueLoginModel> map = continueLoginDictionary.get(gameID);
		if(map==null) {
			map = continueLoginDictionary.get(0);
		}
		if(map!=null) {
			return map.get(day);
		}
		return null;
	} 
	

	/**
	 * 
	 * @return
	 */
	public List<ContinueLoginResponse> getContinueLoginResponseList(int gameID) {
		FastMap<Integer, ContinueLoginModel> map = continueLoginDictionary.get(gameID);
		List<ContinueLoginResponse> login = new ArrayList<ContinueLoginResponse>();
		List<ContinueLoginModel> list = new ArrayList<ContinueLoginModel>();
		if(map==null) {
			map = continueLoginDictionary.get(0);
		}
		if (map != null) {
			list.addAll(map.values());
		}
		for (ContinueLoginModel model : list) {
			protobuf.clazz.Protocol.ContinueLoginResponse.Builder builder = ContinueLoginResponse.newBuilder();
			builder.setDay(model.getDay());
			builder.addAllRewardResponses(model.getRewardList());
			login.add(builder.build());
		}
		return login;

	}

}
