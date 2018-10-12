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

import protobuf.clazz.BaseS2S.SendToClientsProto;

/**
 * 
 */
@IServerCmd(code = S2SCmd.SEND_TO_CLIENT_BATCH, desc = "数据批量发给客户端 每个人都不同数据包")
public class SendToClientBatchHandler extends IServerHandler<SendToClientsProto> {

	@Override
	public void execute(SendToClientsProto resp, S2SSession session) throws Exception {
		resp.getClientsList().forEach((clientRsp)->{
			C2SSession client = C2SSessionService.getInstance().getSession(clientRsp.getAccountId());
			if(client != null){
				client.send(clientRsp.getRsp());
			}
		});
		
	}
}
