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
import protobuf.clazz.coin.CoinProtocol.GameTypeListRequest;


@ICmd(code = C2SCmd.COIN_GAME_TYPE_LIST, desc = "金币场类型")
public final class GameTypeHandler extends IClientHandler<GameTypeListRequest> {
	
	@Override
	protected void execute(GameTypeListRequest req, Request topRequest, C2SSession session) throws Exception {
		session.send(CoinDict.getInstance().getGameTypeResp());
	}
	
}
