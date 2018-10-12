package com.cai.future.runnable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ERedisTopicType;
import com.cai.service.RedisServiceImpl;

import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsCmdResponse;


public class SystemStopReadyRunnable implements Runnable{
	
	private static Logger logger = LoggerFactory.getLogger(SystemStopReadyRunnable.class);

	@Override
	public void run() {
		
		//处理结算,通知中心,所有逻辑服结算
		//========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.CMD);
		//
		RsCmdResponse.Builder rsCmdResponseBuilder = RsCmdResponse.newBuilder();
		rsCmdResponseBuilder.setType(2);
		redisResponseBuilder.setRsCmdResponse(rsCmdResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topProxAndLogic);
		
	}
	
	

}
