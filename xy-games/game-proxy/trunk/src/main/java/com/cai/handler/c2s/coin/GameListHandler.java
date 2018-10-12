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
import protobuf.clazz.coin.CoinProtocol.GameListRequest;

@ICmd(code = C2SCmd.COIN_GAME_LIST, desc = "金币场游戏列表")
public final class GameListHandler extends IClientHandler<GameListRequest> {
	
	@Override
	protected void execute(GameListRequest req, Request topRequest, C2SSession session) throws Exception {
		int gameType = req.getGameTypeId();
		session.send(CoinDict.getInstance().getGameListResp(gameType));
	}
	
}
