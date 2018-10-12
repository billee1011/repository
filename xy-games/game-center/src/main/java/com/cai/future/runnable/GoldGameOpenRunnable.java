/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.future.runnable;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.cai.common.constant.RedisConstant;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.SpringService;
import com.cai.dictionary.AppItemDict;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;
import com.cai.service.RedisServiceImpl;

import javolution.util.FastMap;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RsDictUpdateResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsDictUpdateResponse.RsDictType;

/**
 * @author xwy --指定时间收费
 *
 */
public class GoldGameOpenRunnable implements Runnable {

	private static Logger logger = Logger.getLogger(FiveCleanRunnble.class);

	
	private int gameID;
	
	private int id;
	
	private long createTime;
	
	public GoldGameOpenRunnable(int gameID,int id,long createTime) {
		this.gameID=gameID;
		this.id = id;
		this.createTime=createTime;
	}
	
	@Override
	public void run() {
		try {
			FastMap<Integer, SysParamModel> paramMap= SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameID);
			if(paramMap==null) {
				logger.error("GoldGameOpenRunnable 找不到参数"+gameID+"id="+id);
				return;
			}
			SysParamModel paramModel = paramMap.get(id);
			if(paramModel==null) {
				logger.error("GoldGameOpenRunnable 找不到参数"+gameID+"id="+id);
				return;
			}
			if(paramModel.getFinish_time()==null) {
				logger.error("paramModel.getFinish_time()==null"+gameID+"id="+id);
				return;
			}
			
			if(paramModel.getFinish_time().getTime()!=createTime){
				logger.error("参数有改动不处理"+paramModel.getFinish_time().getTime()+"createTime="+createTime);
				return;
			}
			if(!SysGameTypeDict.getInstance().isGoldGameType(id)) {//是收费索引的参数
				logger.error("不是收费索引id="+id);
				return;
			}
			if(paramModel.getVal1()==1&&paramModel.getVal2()==0&&paramModel.getFinish_time()!=null) {//当前已经开放了，并且是免费的，而且是有指定时间收费的
				paramModel.setVal2(1);
				paramModel.setFinish_time(null);
				
				PublicService publicService = SpringService.getBean(PublicService.class);
				publicService.getPublicDAO().updateSysParamModel(paramModel);
				
				
				RedisService redisService = SpringService.getBean(RedisService.class);
				redisService.hSet(RedisConstant.DICT, RedisConstant.DICT_SYSPARAM, SysParamDict.getInstance().getSysParamModelDictionary());
				
				// ========同步到中心========
				RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
				redisResponseBuilder.setRsResponseType(RsResponseType.DICT_UPDATE);
				//
				RsDictUpdateResponse.Builder rsDictUpdateResponseBuilder = RsDictUpdateResponse.newBuilder();
				rsDictUpdateResponseBuilder.setRsDictType(RsDictType.SYS_PARAM);
				//
				redisResponseBuilder.setRsDictUpdateResponse(rsDictUpdateResponseBuilder);
				RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topProxAndLogic);
				
				
				GameSchedule.put(new Runnable() {
					
					@Override
					public void run() {
						SpringService.getBean(ICenterRMIServer.class).reLoadSysParamModelDictionary();
						SpringService.getBean(ICenterRMIServer.class).reLoadGameGroupRuleDictionary();
						
						
						
						SpringService.getBean(ICenterRMIServer.class).reLoadAppItemDictionary();
						
					}
				}, 5,TimeUnit.SECONDS);
				
				
				// ===================
				logger.error("定时收费成功");
				updateAppItemFlag(gameID);
				logger.info("更新并加载appitem完毕！");
			}else {
				logger.error("paramModel参数跟之前不一样"+gameID+"id="+id);
			}
			
			
		} catch (Exception e) {
			logger.error("定时收费异常",e);
		}
	}
	public void updateAppItemFlag(int appId){
		try{
			PublicService publicService = SpringService.getBean(PublicService.class);
			publicService.getPublicDAO().updateAppItemZeroFlag(appId);
		}catch(Exception e){
			logger.error("更新并加载appitem失败 ",e);
		}
	}

}
