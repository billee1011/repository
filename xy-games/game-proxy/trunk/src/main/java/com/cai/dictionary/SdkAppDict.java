package com.cai.dictionary;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.sdk.SdkApp;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

import protobuf.clazz.c2s.C2SProto.SdkAppResponse;

/**
 * SDK APP和 shop
 * @author chansonyan
 * 2018年9月25日
 */
@SuppressWarnings("unchecked")
public class SdkAppDict {

	private Logger logger = LoggerFactory.getLogger(SdkAppDict.class);
	
	private Map<Long, SdkApp> sdkAppMap ;
	
	private static SdkAppDict instance;
	
	private SdkAppResponse.Builder builder = null;

	/**
	 * 私有构造
	 */
	private SdkAppDict() {
		sdkAppMap = new HashMap<>();
	}

	/**
	 * 单例模式
	 * 
	 */
	public static SdkAppDict getInstance() {
		if (null == instance) {
			instance = new SdkAppDict();
		}
		return instance;
	}

	public void load() {
		RedisService redisService = SpringService.getBean(RedisService.class);
		this.sdkAppMap = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_SDK_APP, HashMap.class);
		if(sdkAppMap!=null) {
			builder = SdkAppResponse.newBuilder();
			protobuf.clazz.c2s.C2SProto.SdkApp.Builder sdkAppBuilder = null;
			for(SdkApp temp : this.sdkAppMap.values()) {
				sdkAppBuilder = protobuf.clazz.c2s.C2SProto.SdkApp.newBuilder();
				sdkAppBuilder.setAppId(temp.getAppId());
				sdkAppBuilder.setAppKey(temp.getAppKey());
				if(StringUtils.isBlank(temp.getAppName())) {
					sdkAppBuilder.setAppName("");
				} else {
					sdkAppBuilder.setAppName(temp.getAppName());
				}
				if(StringUtils.isBlank(temp.getIcon())) {
					sdkAppBuilder.setIcon("");
				} else {
					sdkAppBuilder.setIcon(temp.getIcon());
				}
				sdkAppBuilder.setUrl(temp.getUrl());
				sdkAppBuilder.setAppSecret(temp.getAppSecret());
				sdkAppBuilder.setOrientation(temp.getOrientation());
				builder.addCpApp(sdkAppBuilder);
			}
			//获取配置的SDK URL地址
			SysParamModel sysParamModel30 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(30);
			if(null != sysParamModel30) {
				builder.setSdkUrl(sysParamModel30.getStr1());
			}
		}
		logger.info("加载字典SdkAppDict,count=" + this.sdkAppMap.size());
	}
	
	
	public Map<Long, SdkApp> getSdkAppMap() {
		return sdkAppMap;
	}
	
	public SdkAppResponse.Builder getBuilder() {
		return builder;
	}

	public SdkApp getAppById(Long appId) {
		return this.sdkAppMap.get(appId);
	}

}
