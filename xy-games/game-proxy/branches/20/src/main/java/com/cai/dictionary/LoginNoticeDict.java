package com.cai.dictionary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.LoginNoticeModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

import javolution.util.FastMap;

/**
 * 登录公告字典
 * @author run
 *
 */
public class LoginNoticeDict {

	private Logger logger = LoggerFactory.getLogger(LoginNoticeDict.class);
	
	/**
	 * 代理服字典
	 */
	private FastMap<Integer,LoginNoticeModel> loginNoticeDict;
	
	

	/**
	 * 单例
	 */
	private static LoginNoticeDict instance;

	/**
	 * 私有构造
	 */
	private LoginNoticeDict() {
		loginNoticeDict = new FastMap<Integer,LoginNoticeModel>();
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static LoginNoticeDict getInstance() {
		if (null == instance) {
			instance = new LoginNoticeDict();
		}

		return instance;
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		try{
			RedisService redisService = SpringService.getBean(RedisService.class);
			loginNoticeDict = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_LOGIN_NOTICE, FastMap.class);
		}catch(Exception e){
			logger.error("error",e);
		}
		logger.info("redis缓存加载字典LoginNoticeDict"  + timer.getStr());
		
	}

	public FastMap<Integer, LoginNoticeModel> getLoginNoticeDict() {
		return loginNoticeDict;
	}

	public void setLoginNoticeDict(FastMap<Integer, LoginNoticeModel> loginNoticeDict) {
		this.loginNoticeDict = loginNoticeDict;
	}


}
