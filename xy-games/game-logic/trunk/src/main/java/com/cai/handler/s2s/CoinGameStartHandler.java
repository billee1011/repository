/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.s2s;

import com.cai.coin.CoinService;
import com.cai.common.constant.C2SCmd;
import com.cai.common.handler.IServerHandler;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.coin.CoinServerProtocol.S2SMatchSuccessRequest;

@IServerCmd(code = C2SCmd.COIN_GAME_MATCH_SUC, desc = "匹配成功,等待开始")
public class CoinGameStartHandler extends IServerHandler<S2SMatchSuccessRequest> {

	@Override
	public void execute(S2SMatchSuccessRequest req, S2SSession session) throws Exception {
		
		CoinService.INTANCE().gameStart(req);
		
	}

}
