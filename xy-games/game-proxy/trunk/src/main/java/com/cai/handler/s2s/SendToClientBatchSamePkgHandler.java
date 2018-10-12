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

import protobuf.clazz.BaseS2S.SendToClientsProto2;

/**
 * 
 */
@IServerCmd(code = S2SCmd.SEND_TO_CLENT_BATCH_SAME_PKG, desc = "数据批量发给客户端 每个人都相同数据包")
public class SendToClientBatchSamePkgHandler extends IServerHandler<SendToClientsProto2> {

	@Override
	public void execute(SendToClientsProto2 resp, S2SSession session) throws Exception {
		if(resp.getSendAll()){
         	C2SSessionService.getInstance().getAllOnlieSession().forEach(client->client.send(resp.getRsp()));
			return;
		}
		
		resp.getAccountIdList().forEach((id)->{
			C2SSession client = C2SSessionService.getInstance().getSession(id);
			
			if(client != null){
				client.send(resp.getRsp());
			}
		});
		
	}
}
