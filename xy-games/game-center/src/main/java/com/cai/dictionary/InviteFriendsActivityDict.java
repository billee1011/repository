package com.cai.dictionary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.InviteFriendsActivityModel;
import com.cai.common.domain.activity.MainPrizes;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;

/**
 * 邀请好友活动字典
 * 
 * @author wuhaoran 2018年8月18日
 */
public class InviteFriendsActivityDict {

	private Logger logger = LoggerFactory.getLogger(InviteFriendsActivityDict.class);

	private Map<Integer, InviteFriendsActivityModel> inviteFriendsActivityDictionary;

	/**
	 * 单例
	 */
	private static InviteFriendsActivityDict instance;

	/**
	 * 私有构造
	 */
	private InviteFriendsActivityDict() {
		inviteFriendsActivityDictionary = new HashMap<>();
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static InviteFriendsActivityDict getInstance() {
		if (null == instance) {
			instance = new InviteFriendsActivityDict();
		}

		return instance;
	}

	public void load() {
		inviteFriendsActivityDictionary.clear();
		RedisService redisService = SpringService.getBean(RedisService.class);
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<InviteFriendsActivityModel> inviteFriendsList = publicService.getPublicDAO().getInviteFriendsActivityModelList();
		for (InviteFriendsActivityModel inviteFriendsModel : inviteFriendsList) {
			if (!StringUtils.isBlank(inviteFriendsModel.getInvite_prize())) {
				inviteFriendsModel.setMainPrize(JSONObject.parseObject(inviteFriendsModel.getInvite_prize(), MainPrizes.class));
			}
			inviteFriendsActivityDictionary.put(inviteFriendsModel.getId(), inviteFriendsModel);
		}
		redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_INVITE_FRIENDS_ACTIVITY, inviteFriendsActivityDictionary);
		logger.info("加载字典InviteFriendsActivityDict,count=" + inviteFriendsActivityDictionary.size());
	}

}
