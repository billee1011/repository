package com.cai.common.util;

import protobuf.redis.ProtoRedis;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountResponse;

public class Test {
	
	public static void main(String[] args) {
		System.out.println("========");
		
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
		
		RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
		rsAccountResponseBuilder.setAccountId(222L);
		rsAccountResponseBuilder.setGameId(10);
		
		redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder.build());
		
		System.out.println(redisResponseBuilder.build());
		byte[] bytes = redisResponseBuilder.build().toByteArray();
		
		System.out.println("bytes length=" + bytes.length);
		
		System.out.println("==========解码============");
		
		
		PerformanceTimer timer = new PerformanceTimer();
		int k = 0;
		try {
			for(int i=0;i<100000;i++){
				RedisResponse redisResponse = ProtoRedis.RedisResponse.parseFrom(bytes);
				//System.out.println(redisResponse.getRsAccountResponse().getAccountId());
				k++;
			}
			System.out.println(timer.getStr());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
		
		
		
		
		
	}

}
