/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s.coin;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.CoinPlayerRedis;
import com.cai.common.util.PBUtil;
import com.cai.common.util.SpringService;
import com.cai.core.SystemConfig;
import com.cai.redis.service.RedisService;
import com.cai.service.ClientServiceImpl;
import com.cai.util.MessageResponse;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.coin.CoinProtocol.GameEnterRequest;
import protobuf.clazz.coin.CoinServerProtocol.S2SGameEnterRequest;


@ICmd(code = C2SCmd.COIN_GAME_ENTER, desc = "请求进入金币房间")
public final class CoinGameEnterHandler extends IClientHandler<GameEnterRequest> {
	
	@Override
	protected void execute(GameEnterRequest req, Request topRequest, C2SSession session) throws Exception {
		
		long accountId = session.getAccountID();
		RedisService redisService = SpringService.getBean(RedisService.class);
		CoinPlayerRedis playerRedis = redisService.hGet(RedisConstant.COIN_PLAYER_INFO, accountId+"", CoinPlayerRedis.class);
		if(playerRedis == null){
			session.send(MessageResponse.getMsgAllResponse("未进行任何金币场游戏,进入失败!").build());
			return;
		}
		
		S2SGameEnterRequest.Builder request = S2SGameEnterRequest.newBuilder();
		request.setAccountId(accountId);
		request.setProxyId(SystemConfig.proxy_index);
		
		boolean result = ClientServiceImpl.getInstance().sendMsg(playerRedis.getLogicId(), 
				PBUtil.toS2SRequet(C2SCmd.COIN_GAME_ENTER, request).build());
		if (!result) {
			session.send(MessageResponse.getMsgAllResponse("金币场逻辑服维护中，请稍后再试!").build());
		}
	}
	
}
