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
import protobuf.clazz.coin.CoinProtocol.OneGameRuleRequest;


@ICmd(code = C2SCmd.COIN_GAME_RULE, desc = "金币场游戏规则")
public final class GameRuleHandler extends IClientHandler<OneGameRuleRequest> {
	
	@Override
	protected void execute(OneGameRuleRequest req, Request topRequest, C2SSession session) throws Exception {
		int detailId = req.getDetailId();
		session.send(CoinDict.getInstance().getGameRuleResp(detailId));
	}
	
}
