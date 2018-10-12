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
import protobuf.clazz.coin.CoinProtocol.OneGameDetailRequest;

@ICmd(code = C2SCmd.COIN_ONE_GAME_DETAIL, desc = "金币场游戏详情")
public final class GameDetailHandler extends IClientHandler<OneGameDetailRequest> {

	@Override
	protected void execute(OneGameDetailRequest req, Request topRequest, C2SSession session) throws Exception {
		int detailId = req.getDetailId();
		Object resp = CoinDict.getInstance().getGameDetailRespById(detailId);
		if (null != resp) {
			session.send(resp);
		}
	}

}
