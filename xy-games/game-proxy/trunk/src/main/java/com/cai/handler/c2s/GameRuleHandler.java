/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.SysParamEnum;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountRedis;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.PBUtil;
import com.cai.common.util.SpringService;
import com.cai.core.GbCdCtrl;
import com.cai.core.GbCdCtrl.Opt;
import com.cai.dictionary.GameGroupRuleDict;
import com.cai.dictionary.SysParamDict;
import com.cai.redis.service.RedisService;
import com.cai.util.MessageResponse;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Common.CommonAppConfigProto;
import protobuf.clazz.Common.GameRuleRequest;
import protobuf.clazz.Protocol.Request;

/**
 * 
 */
@ICmd(code = C2SCmd.GAME_RULE, desc = "游戏玩法")
public final class GameRuleHandler extends IClientHandler<GameRuleRequest> {

	@Override
	protected void execute(GameRuleRequest req, Request topRequest, C2SSession session) throws Exception {
		Account account = session.getAccount();
		if (account == null) {
			return;
		}

		if(!GbCdCtrl.canHandle(session, Opt.GAME_RULE)) return;
		int prosize = 0;
		AccountRedis accountRedis = SpringService.getBean(RedisService.class).hGet(RedisConstant.ACCOUNT_REDIS, account.getAccount_id() + "",
				AccountRedis.class);
		if (null != accountRedis) {
			prosize=accountRedis.getProxRoomMap().size();
		}

		SysParamModel sysParamModel = null;
		sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(req.getId()).get(SysParamEnum.ID_1107.getId());
		int count = 0;
		if (sysParamModel != null) {
			if (account.getAccountModel().getIs_agent() < 1) {
				count = sysParamModel.getVal2();
			} else {
				count = sysParamModel.getVal1();
			}
		}

		CommonAppConfigProto.Builder p = GameGroupRuleDict.getInstance().get(req.getId());
		if (p == null) {
			session.send(MessageResponse.getMsgAllResponse("即将开放,敬请期待!").build());
			return;
		}
		p.setOpenCount(Math.max(0, count - prosize));

		if (p != null) {
			session.send(PBUtil.toS2CCommonRsp(S2CCmd.GAME_RULE, p));
		}

	}

}
