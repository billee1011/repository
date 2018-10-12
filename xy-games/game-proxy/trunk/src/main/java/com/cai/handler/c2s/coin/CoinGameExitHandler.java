/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s.coin;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.CoinPlayerRedis;
import com.cai.common.util.PBUtil;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.ClientServiceImpl;
import com.cai.util.MessageResponse;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.coin.CoinProtocol.GameExitRequest;
import protobuf.clazz.coin.CoinServerProtocol.S2SGameExitRequest;


@ICmd(code = C2SCmd.COIN_GAME_EXIT, desc = "请求退出金币房间")
public final class CoinGameExitHandler extends IClientHandler<GameExitRequest> {
	
	@Override
	protected void execute(GameExitRequest req, Request topRequest, C2SSession session) throws Exception {
		
		long accountId = session.getAccountID();
		RedisService redisService = SpringService.getBean(RedisService.class);
		CoinPlayerRedis playerRedis = redisService.hGet(RedisConstant.COIN_PLAYER_INFO, accountId+"", CoinPlayerRedis.class);
		if(playerRedis == null){
			session.send(MessageResponse.getMsgAllResponse("未进行任何金币场游戏,退出失败!").build());
			return;
		}
		
		S2SGameExitRequest.Builder request = S2SGameExitRequest.newBuilder();
		request.setAccountId(accountId);
		
		boolean result = ClientServiceImpl.getInstance().sendMsg(playerRedis.getLogicId(), 
				PBUtil.toS2SRequet(C2SCmd.COIN_GAME_EXIT, request).build());
		if (!result) {
			session.send(MessageResponse.getMsgAllResponse("金币场逻辑服维护中，请稍后再试!").build());
		}
	}
	
}
