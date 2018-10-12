/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import com.cai.coin.CoinService;
import com.cai.common.constant.C2SCmd;
import com.cai.domain.Session;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.coin.CoinServerProtocol.S2SGameEnterRequest;

@IServerCmd(code = C2SCmd.COIN_GAME_EXIT, desc = "匹配成功,退出游戏")
public class CoinGameExitHandler extends IClientHandler<S2SGameEnterRequest> {

	@Override
	protected void execute(S2SGameEnterRequest req, Session session) throws Exception {
		CoinService.INTANCE().gameExit(req.getAccountId(), session);
	}





}
