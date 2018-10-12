/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.util.PBUtil;
import com.cai.dictionary.GameGroupRuleDict;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Common.CommonGameConfigProto;
import protobuf.clazz.Common.GameRuleRequest;
import protobuf.clazz.Protocol.Request;

/**
 * 
 */
@ICmd(code = C2SCmd.GAME_RULE_BY_GAME_ID, desc = "游戏玩法")
public final class GameRuleByGameIdHandler extends IClientHandler<GameRuleRequest> {

	@Override
	protected void execute(GameRuleRequest req, Request topRequest, C2SSession session) throws Exception {
		
		CommonGameConfigProto p = GameGroupRuleDict.getInstance().getBySubId(req.getId());
		if(p != null){
			session.send(PBUtil.toS2CCommonRsp(S2CCmd.GAME_RULE_BY_GAME_ID, p));
		}
		
	}

}
