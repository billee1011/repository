/**
 * 
 */
package com.cai.dictionary;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.cai.common.constant.RedisConstant;
import com.cai.common.define.ELogType;
import com.cai.common.define.EPropertyType;
import com.cai.common.domain.Account;
import com.cai.common.domain.IPGroupModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.domain.Session;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.MessageResponse;

import javolution.util.FastMap;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.AccountPropertyListResponse;
import protobuf.clazz.Protocol.AccountPropertyResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;

/**
 * @author xwy
 *
 */
public class IPGroupDict {

	private Logger logger = LoggerFactory.getLogger(IPGroupDict.class);

	/**
	 */
	private FastMap<Integer, FastMap<Integer, IPGroupModel>> ipgroupDictionary;

	/**
	 */
	private FastMap<Integer, IPGroupModel> ipgroupMap;

	/**
	 * 单例
	 */
	private static IPGroupDict instance;

	/**
	 * 私有构造
	 */
	private IPGroupDict() {
		ipgroupDictionary = new FastMap<Integer, FastMap<Integer, IPGroupModel>>();
		ipgroupMap = new FastMap<Integer, IPGroupModel>();
	}

	/**
	 * 单例模式
	 *
	 * @return 字典单例
	 */
	public static IPGroupDict getInstance() {
		if (null == instance) {
			instance = new IPGroupDict();
		}

		return instance;
	}

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		try {
			ipgroupMap.clear();
			ipgroupDictionary.clear();

			RedisService redisService = SpringService.getBean(RedisService.class);
			ipgroupMap = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_IP, FastMap.class);

			for (IPGroupModel model : ipgroupMap.values()) {
				if (!ipgroupDictionary.containsKey(model.getGame_type())) {
					FastMap<Integer, IPGroupModel> map = new FastMap<Integer, IPGroupModel>();
					ipgroupDictionary.put(model.getGame_type(), map);
				}
				ipgroupDictionary.get(model.getGame_type()).put(model.getId(), model);
			}

		} catch (Exception e) {
			logger.error("error", e);
		}
		logger.info("redis缓存加载字典DICT_IP" + timer.getStr());

	}

	public FastMap<Integer, FastMap<Integer, IPGroupModel>> getIPGroupModelDictionary() {
		return ipgroupDictionary;
	}

	public void setIPModelDictionary(FastMap<Integer, FastMap<Integer, IPGroupModel>> shopModelDictionary) {
		this.ipgroupDictionary = shopModelDictionary;
	}

	public FastMap<Integer, IPGroupModel> getIPGroupModelMapByGameId(int game_id) {
		return ipgroupDictionary.get(game_id);
	}

	/**
	 * @param shopID
	 * @return
	 */
	public IPGroupModel getIPGroupModelModel(int id) {
		return ipgroupMap.get(id);
	}

	public void updateIpToPlayer(Session session) {
		// ===============备用ip组========================================
		try {
			if(session==null||session.getAccount()==null) return ;
			Account account = session.getAccount();
			FastMap<Integer, IPGroupModel> ipMap = ipgroupDictionary.get(account.getGame_id());
			if(ipMap==null) return;
			
			Date date = account.getAccountModel().getCreate_time();
			Date now = new Date();
			if(now.getTime()-date.getTime()<24*60*60) return;
			
			int onlineTime = account.getAccountModel().getHistory_online()/(60*60);
			int[] ipArray = new int[ipMap.size()];
			int i=0;
			for(IPGroupModel ipModel:ipMap.values()) {
				if(ipModel.getWeight()<=onlineTime) {
					ipArray[i++]=ipModel.getId();
				}
			}
			if(i>0) {
				int index = RandomUtil.getRandomNumber(i);
				if(index<=0) {
					index=ipArray[index];
				}else {
					index=ipArray[index-1];
				}
			
				
				IPGroupModel indexModel = ipMap.get(index);
				String ip="";
				int port = 0;
				if(indexModel!=null) {
					ip=indexModel.getIp();
					port=indexModel.getPort();
				}
				
				
				AccountPropertyListResponse.Builder accountPropertyListResponseBuilder = AccountPropertyListResponse
						.newBuilder();
				AccountPropertyResponse.Builder accountPropertyResponseBuilder = MessageResponse.getAccountPropertyResponse(
						EPropertyType.SLB_IP_LIST.getId(), port, null, null, null, null, ip, null, null);
				accountPropertyListResponseBuilder.addAccountProperty(accountPropertyResponseBuilder);
				Response.Builder responseBuilder = Response.newBuilder();
				responseBuilder.setResponseType(ResponseType.PROPERTY);
				responseBuilder.setExtension(Protocol.accountPropertyListResponse, accountPropertyListResponseBuilder.build());
				PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
				
				MongoDBServiceImpl.getInstance().player_log(account.getAccount_id(), ELogType.serverIp,
						"服务器分配ip:" + ip ,null, null,
						null);
			}
			
		} catch (Exception e) {
			logger.error("ip update error",e);
		}
	}

}
