/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s.coin;

import com.cai.common.constant.C2SCmd;
import com.cai.common.util.PBUtil;
import com.cai.core.SystemConfig;
import com.cai.service.ClientServiceImpl;
import com.cai.util.MessageResponse;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.coin.CoinProtocol.ReliefReceiveRequest;
import protobuf.clazz.coin.CoinServerProtocol.S2SGameReliefRequest;


@ICmd(code = C2SCmd.COIN_RELIEF_RECEIVE, desc = "金币场救济金领取")
public final class CoinReliefReceiveHandler extends IClientHandler<ReliefReceiveRequest> {
	
	@Override
	protected void execute(ReliefReceiveRequest req, Request topRequest, C2SSession session) throws Exception {
		
		int gameId = req.getGameId();
		int detailId = req.getDetailId();
		S2SGameReliefRequest.Builder request = S2SGameReliefRequest.newBuilder();
		request.setAccountId(session.getAccountID());
		request.setGameId(gameId);
		request.setDetailId(detailId);
		
		boolean result = ClientServiceImpl.getInstance().sendToCoin(SystemConfig.connectCoin, 
				PBUtil.toS2SRequet(C2SCmd.COIN_RELIEF_RECEIVE, request).build());
		if (!result) {
			session.send(MessageResponse.getMsgAllResponse("金币场维护中，请稍后再试！").build());
		}
	}
	
}
