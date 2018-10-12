package com.cai.dictionary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.InviteActiveModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PublicService;

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
	//已经发送红包的个数，领取红包个数超过总设定的个数就停止红包发送
	private AtomicInteger totalInviteCounts;

	/**
	 * 私有构造
	 */
	private InviteActiveDict() {
		inviteActiveDictionary = new HashMap<Integer,InviteActiveModel>();
		inviteActiveModel = new InviteActiveModel();
		totalInviteCounts  = new AtomicInteger(0);
	}

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
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<InviteActiveModel> inviteActiveModelList = publicService.getPublicDAO().getInviteActiveModelList();
		 Map<Integer,InviteActiveModel> inviteActiveDictionary = new HashMap<Integer,InviteActiveModel>();
		for(InviteActiveModel model : inviteActiveModelList){
			inviteActiveDictionary.put(model.getId(), model);
			inviteActiveModel = model;
		}
		this.inviteActiveDictionary = inviteActiveDictionary;
		long effectiveInviteCounts = MongoDBServiceImpl.getInstance().getAllRedPacketCount();
		totalInviteCounts.set((int)effectiveInviteCounts);
		//放入redis缓存
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_INVITE_RED_PACKET, inviteActiveDictionary);
		logger.info("load inviteActiveDictionary success! "+timer.getStr());
	}

	public Map<Integer, InviteActiveModel> getInviteActiveDictionary() {
		return inviteActiveDictionary;
	}
	public InviteActiveModel getInviteActiveModel(){
		return inviteActiveModel;
	}

	public AtomicInteger getTotalInviteCounts() {
		return totalInviteCounts;
	}

	public void setTotalInviteCounts(AtomicInteger totalInviteCounts) {
		this.totalInviteCounts = totalInviteCounts;
	}

}
