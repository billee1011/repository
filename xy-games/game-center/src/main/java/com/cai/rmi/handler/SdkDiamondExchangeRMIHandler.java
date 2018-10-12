package com.cai.rmi.handler;

import java.util.concurrent.locks.ReentrantLock;

import com.cai.common.constant.RMICmd;
import com.cai.common.define.EDiamondOperateType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.sdk.SdkShop;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.util.GlobalExecutor;
import com.cai.common.vo.SdkDiamondExchangeDataVo;
import com.cai.dictionary.SdkAppDict;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PublicServiceImpl;
import com.cai.service.RedisServiceImpl;

import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RsAccountModelResponse;
import protobuf.redis.ProtoRedis.RsAccountResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;

/**
 * SDK钻石兑换，扣除钻石
 * @author chansonyan
 * 2018年9月11日
 */
@IRmi(cmd = RMICmd.SDK_DIAMOND_EXCHANGE, desc = "SDK钻石兑换，扣除钻石")
public class SdkDiamondExchangeRMIHandler extends IRMIHandler<SdkDiamondExchangeDataVo, Boolean>{

	@Override
	protected Boolean execute(SdkDiamondExchangeDataVo dataVo) {
		if(null == dataVo) {
			return false;
		}
		//获取account和token，验证是否正确
		Account account = PublicServiceImpl.getInstance().getAccount(dataVo.getAccountId());
		if(null == account) {
			return false;
		}
		
		//判断钻石是否足够
		ReentrantLock lock = account.getRedisLock();
		lock.lock();
		try {
			final AccountModel accountModel = account.getAccountModel();
			if(accountModel.getDiamond() < dataVo.getAmount()) {
				return false;
			}
			SdkShop sdkShop = SdkAppDict.getInstance().getShopById(dataVo.getAppId(), dataVo.getItemId());
			if(null == sdkShop) {
				return false;
			}
			int old = accountModel.getDiamond();
			accountModel.setDiamond(accountModel.getDiamond() - (int)dataVo.getAmount());
			accountModel.setNeedDB(true);
			
			//增加钻石兑换流水日志，直接为成功状态
			MongoDBServiceImpl.getInstance().addDiamondLog(dataVo.getOrderId(), dataVo.getAccountId(), accountModel.getAccount_name(), 0, EDiamondOperateType.EXCHANGE.getId(), 
					sdkShop.getId(), (int)dataVo.getAmount() , 0, 0, (int)dataVo.getAmount(), 0, "", "", "", 
					"", dataVo.getCpOrderId(), dataVo.getAppId(), sdkShop.getItemName(), 0, 0, 0, 0, old, accountModel.getDiamond(), 0);
			
			//钻石兑换日志
			GlobalExecutor.execute(new Runnable() {
				@Override
				public void run() {
					//发送钻石变化到代理服
					RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
					redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
					//
					RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
					rsAccountResponseBuilder.setAccountId(accountModel.getAccount_id());
					//
					RsAccountModelResponse.Builder rsAccountModelResponseBuilder = RsAccountModelResponse.newBuilder();
					rsAccountModelResponseBuilder.setDiamond(accountModel.getDiamond());
					rsAccountModelResponseBuilder.setDiamondChangeType(EDiamondOperateType.EXCHANGE.getId());
					rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponseBuilder);
					
					redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
					RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy.getId() + account.getLastProxyIndex());
				}
			});
			return true;
		} catch(Exception e) {
			logger.error("钻石兑换错误", e);
		} finally {
			lock.unlock();
		}
		return false;
	}

}
