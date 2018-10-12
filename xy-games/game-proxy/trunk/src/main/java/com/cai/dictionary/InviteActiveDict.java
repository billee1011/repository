package com.cai.dictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.InviteActiveModel;
import com.cai.common.domain.json.PrizeJsonModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

/**
 * 活动字典
 *
 */
public class InviteActiveDict {

	private Logger logger = LoggerFactory.getLogger(InviteActiveDict.class);
	
	private Map<Integer,InviteActiveModel> inviteActiveDictionary;

	private InviteActiveModel inviteActiveModel;
	/**
	 * 单例
	 */
	private static InviteActiveDict instance;

	/**
	 * 私有构造
	 */
	private InviteActiveDict() {
		inviteActiveDictionary = new HashMap<Integer,InviteActiveModel>();
		inviteActiveModel = new InviteActiveModel();
		prizeList = new ArrayList<>();
	}
	
	public List<PrizeJsonModel> prizeList = null;
	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static InviteActiveDict getInstance() {
		if (null == instance) {
			instance = new InviteActiveDict();
		}
		return instance;
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		inviteActiveDictionary.clear();
		RedisService redisService = SpringService.getBean(RedisService.class);
		inviteActiveDictionary = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_INVITE_RED_PACKET, HashMap.class);
		if(inviteActiveDictionary!=null&&inviteActiveDictionary.size()>0){
			for(InviteActiveModel inviteActiveModel:inviteActiveDictionary.values()){
				this.inviteActiveModel = inviteActiveModel;
				if(StringUtils.isNotBlank(inviteActiveModel.getPrize_json())){
					List<PrizeJsonModel> prizeList = JSONArray.parseArray(inviteActiveModel.getPrize_json(), PrizeJsonModel.class);
					this.prizeList = prizeList;
				}
			}
		}
		//放入redis缓存
		logger.info("load inviteActiveDictionary success! "+timer.getStr());
	}

	public Map<Integer, InviteActiveModel> getInviteActiveDictionary() {
		return inviteActiveDictionary;
	}
	public InviteActiveModel getInviteActiveModel(){
		return inviteActiveModel;
	}

}
