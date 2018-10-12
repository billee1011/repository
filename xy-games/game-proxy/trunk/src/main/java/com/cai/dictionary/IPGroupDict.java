/**
 * 
 */
package com.cai.dictionary;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.define.ELogType;
import com.cai.common.define.EPropertyType;
import com.cai.common.domain.Account;
import com.cai.common.domain.IPGroupModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.core.SystemConfig;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.MessageResponse;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

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
			
			FastMap<Integer, IPGroupModel> allMap = ipgroupDictionary.get(0);
			if(allMap!=null) {
				for(Map.Entry<Integer,FastMap<Integer,IPGroupModel>> entry:ipgroupDictionary.entrySet()){
					for(Entry<Integer, IPGroupModel> entryGroup:allMap.entrySet()) {
						entry.getValue().put(entryGroup.getKey(), entryGroup.getValue());
					}
				}
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

	public void updateIpToPlayer(C2SSession session) {
		// ===============备用ip组========================================
		try {
			if(session==null||session.getAccount()==null) return ;
			Account account = session.getAccount();
			FastMap<Integer, IPGroupModel> ipMap = ipgroupDictionary.get(account.getGame_id());
			if(ipMap==null) {
				ipMap = ipgroupDictionary.get(0);
				if(ipMap==null) {
					return;
				}
				
			}
			
			Date date = account.getAccountModel().getCreate_time();
			Date now = new Date();
			if(now.getTime()-date.getTime()<24*60*60*1000) {
				if(SystemConfig.gameDebug == 1) {
					logger.warn("account create time"+date);
				}
				return;
			}
			
			int onlineTime = account.getAccountModel().getHistory_online()/(60*60);
			int[] ipArray = new int[ipMap.size()];
			int i=0;
			
			int tempMaxIpModel = 0;
			int tempIndex=0;
			
			
			/**
			 *  login_times>150           460288             573364
  
  login_times>200           417924             549653
  
                                                           上时间 120667
  
  login_times>300            355959            511100
  
                                                         加上时间 112235
  
  login_times>500            276563             453809
  
                                                       加上时间 98738
  
  login_times>1000           172336             354249
                                                       加上时间74101
  
  login_times>2000           84194             227373
  
  login_times>3000             46386                   147075
			 * 
			 * 
			 * */
			
			int loginTimes =  account.getAccountModel().getLogin_times();//加上登录次数防止挂机的
			
			for(IPGroupModel ipModel:ipMap.values()) {
				if(ipModel.getWeight()<=onlineTime && loginTimes>= ipModel.getWeight()*6) {
					if(ipModel.getWeight()>tempMaxIpModel){
						tempMaxIpModel=ipModel.getWeight();
						tempIndex=i;
					}
					ipArray[i++]=ipModel.getId();
				}
			}
			
			int index=0;
			if(i>0) {
//				index = RandomUtil.getRandomNumber(i);
				index=tempIndex;
				if(index<=0 || index>=ipArray.length) {
					index=ipArray[0];
				}else {
					index=ipArray[index];
				}
			}
			IPGroupModel indexModel = ipMap.get(index);
			String ip="";
			int port = 0;
			if(indexModel!=null) {
				ip=indexModel.getIp();
				port=indexModel.getPort();
				
				MongoDBServiceImpl.getInstance().player_log_serverIP(account.getAccount_id(), ELogType.serverIpNEW,
						"服务器分配ip:" + ip ,null, null,
						null);
			}else {
				if(SystemConfig.gameDebug == 1) {
					logger.warn("size is"+ipArray.length+"and index = "+index);
				}
				if(i>0){
					logger.error("分配逻辑有bug size is"+ipArray.length+"and index = "+index);
				}
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
			
			
			
		} catch (Exception e) {
			logger.error("ip update error",e);
		}
	}

}
