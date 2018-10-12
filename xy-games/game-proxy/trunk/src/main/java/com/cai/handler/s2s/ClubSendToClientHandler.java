/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.s2s;

import com.cai.common.constant.S2SCmd;
import com.cai.common.handler.IServerHandler;
import com.cai.service.C2SSessionService;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.s2s.ClubServerProto.ClubToClientRsp;

/**
 * 
 */
@IServerCmd(code = S2SCmd.SEND_TO_CLIENT_RSP, desc = "俱乐部")
public class ClubSendToClientHandler extends IServerHandler<ClubToClientRsp> {

	@Override
	public void execute(ClubToClientRsp resp, S2SSession session) throws Exception {
		C2SSession client = C2SSessionService.getInstance().getSession(resp.getClientSessionId());
		if (client != null) {
			client.send(resp.getRsp());
		}
	}
}
