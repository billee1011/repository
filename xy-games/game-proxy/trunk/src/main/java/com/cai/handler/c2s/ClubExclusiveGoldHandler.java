/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.RMICmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.Account;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.PBUtil;
import com.cai.common.util.SpringService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.ClubMsgProto.ClubExclusiveGoldProto;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.c2s.C2SProto.EmptyReq;

/**
 * 
 * 
 *
 * @author wu_hc date: 2017年12月19日 上午10:31:37 <br/>
 */
@ICmd(code = C2SCmd.CLUB_EXCLUSIVE_GOLD_INFO, desc = "俱乐部专属豆")
public final class ClubExclusiveGoldHandler extends IClientHandler<EmptyReq> {

	@Override
	protected void execute(EmptyReq req, Request topRequest, C2SSession session) throws Exception {
		final Account account = session.getAccount();
		if (null == account) {
			return;
		}

		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		ClubExclusiveGoldProto exclusiveProto = centerRMIServer.rmiInvoke(RMICmd.CLUB_EXCLUSIVE_GOLD_INFO, account.getAccount_id());
		if (null == exclusiveProto) {
			return;
		}
		session.send(PBUtil.toS2CCommonRsp(S2CCmd.CLUB_EXCLUSIVE_GOLD_INFO, exclusiveProto));
	}
}
