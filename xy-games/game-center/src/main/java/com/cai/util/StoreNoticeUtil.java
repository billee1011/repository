package com.cai.util;

import com.cai.common.define.ERedisTopicType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AddCardLog;
import com.cai.common.domain.ShopModel;
import com.cai.common.type.UIType;
import com.cai.dictionary.ShopDict;
import com.cai.service.PublicServiceImpl;
import com.cai.service.RedisServiceImpl;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.StoreNoticeResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;

public class StoreNoticeUtil {
	
	public static void storeNotice(AddCardLog addCardLog){
		if(addCardLog == null){
			return;
		}
		try{
			long accountId = addCardLog.getAccountId();
			Account account = PublicServiceImpl.getInstance().getAccount(accountId);
			if(account == null){
				return;
			}
			storeNotice0(account,addCardLog);
		}catch (Exception e) {
		}
	}
	
	private static void storeNotice0(Account account,AddCardLog addCardLog){
		int uiType = addCardLog.getUiType();
		int subUiType = addCardLog.getSubUiType();
		int opId = addCardLog.getOpId();
		int addGold = 0;
		ShopModel shop = ShopDict.getInstance().getShopModel(addCardLog.getShopId());
		if(shop != null){
			addGold = shop.getGold() + shop.getSend_gold();
		}
		
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.STORE_NOTICE);
		
		StoreNoticeResponse.Builder response = StoreNoticeResponse.newBuilder();
		response.setAccountId(account.getAccount_id());
		response.setOpType(UIType.OP_PAY);
		response.setGold(addGold);
		response.setCoin(0);
		response.setUiType(uiType);
		response.setUiSubType(subUiType);
		response.setOpId(opId);
		//
		redisResponseBuilder.setStoreResponse(response);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
				ERedisTopicType.topicProxy.getId() + account.getLastProxyIndex());
		
	}
}
