/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s.coin;

import com.cai.common.constant.C2SCmd;
import com.cai.dictionary.CoinDict;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.coin.CoinProtocol.GameDetailRequest;


@ICmd(code = C2SCmd.COIN_GAME_DETAIL, desc = "金币场游戏详情")
public final class GameDetailsHandler extends IClientHandler<GameDetailRequest> {
	
	@Override
	protected void execute(GameDetailRequest req, Request topRequest, C2SSession session) throws Exception {
		int gameId = req.getGameId();
		session.send(CoinDict.getInstance().getGameDetailResp(gameId));
	}
	
}
