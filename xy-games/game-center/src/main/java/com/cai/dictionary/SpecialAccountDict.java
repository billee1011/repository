package com.cai.dictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.cai.common.domain.SpecialAccountModel;
import com.cai.common.domain.json.AccountGameDetailModel;
import com.cai.common.util.SpringService;
import com.cai.service.PublicService;

/**
 * 活动字典
 *
 */
public class SpecialAccountDict {

	private Logger logger = LoggerFactory.getLogger(SpecialAccountDict.class);
	

	private SpecialAccountModel specialAccountModel = null;
	/**
	 * 单例
	 */
	private static SpecialAccountDict instance;

	/**
	 * 私有构造
	 */
	private SpecialAccountDict() {
		specialAccountModel = new SpecialAccountModel();
	}

	/**
	 * 单例模式
	 * 
	 * @return 字典单例
	 */
	public static SpecialAccountDict getInstance() {
		if (null == instance) {
			instance = new SpecialAccountDict();
		}

		return instance;
	}
	public static Map<Long,Integer> maxSubLimitMap = new HashMap<>(); 
	
	public static Map<Long,Integer> specialPercentMap = new HashMap<>(); //下级推广员以及推广员带来的返利比
	
	public static Map<Long,Integer> specialAgentPercentMap = new HashMap<>(); //直属代理返利比
	
	public static Map<Long,AccountGameDetailModel> gameDetailMap = new HashMap<>();
	
	public static List<Long> specialAccountList = new ArrayList<>();

	public void load() {
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<SpecialAccountModel> list =  publicService.getPublicDAO().getSpecialAccountModelList();
		if(list.size() > 0){
			specialAccountModel = list.get(0);
			initMaxSubLimitMap();
			initSpecialPercentMap();
			initGameDetailMap();
			initSpecialAccountList();
			initSpecialAgentPercentMap();
		}
		logger.info("加载特殊账号到缓存");
	}
	
	private void initMaxSubLimitMap(){
		String maxSubLimit = specialAccountModel.getSub_limit();
		if(StringUtils.isBlank(maxSubLimit)){
			return;
		}
		String[] singleSub = maxSubLimit.split(",");
		for(String sub:singleSub){
			String[] subs = StringUtils.split(sub, "#");
			maxSubLimitMap.put(Long.parseLong(subs[0]), Integer.parseInt(subs[1]));
		}
	}
	private void initSpecialPercentMap(){
		String specialPercent = specialAccountModel.getRecommend_receive_percent();
		if(StringUtils.isBlank(specialPercent)){
			return;
		}
		String[] singleSpecialPercent = specialPercent.split(",");
		for(String special:singleSpecialPercent){
			String[] specials = StringUtils.split(special, "#");
			int percent = Integer.parseInt(specials[1]);
			if(percent>10){
				return;
			}
			specialPercentMap.put(Long.parseLong(specials[0]), percent);
		}
	}
	private void initSpecialAgentPercentMap(){
		String specialPercent = specialAccountModel.getRecommend_agent_receive_percent();
		if(StringUtils.isBlank(specialPercent)){
			return;
		}
		String[] singleSpecialPercent = specialPercent.split(",");
		for(String special:singleSpecialPercent){
			String[] specials = StringUtils.split(special, "#");
			int percent = Integer.parseInt(specials[1]);
			if(percent>30){
				return;
			}
			specialAgentPercentMap.put(Long.parseLong(specials[0]), Integer.parseInt(specials[1]));
		}
	}
	private void initSpecialAccountList(){
		String privilege_allow_subId = specialAccountModel.getPrivilege_allow_subId();
		if(StringUtils.isBlank(privilege_allow_subId)){
			return;
		}
		String[] ids = privilege_allow_subId.split(",");
		for(String id:ids){
			specialAccountList.add(Long.parseLong(id));
		}
	}
	private void initGameDetailMap(){
		String privilege_allow_gameDetail = specialAccountModel.getPrivilege_allow_gameDetail();
		if(StringUtils.isBlank(privilege_allow_gameDetail)){
			return;
		}
		List<AccountGameDetailModel> list = JSON.parseArray(privilege_allow_gameDetail, AccountGameDetailModel.class);
		for(AccountGameDetailModel model:list){
			gameDetailMap.put(model.getAccountId(), model);
		}
	}
}
