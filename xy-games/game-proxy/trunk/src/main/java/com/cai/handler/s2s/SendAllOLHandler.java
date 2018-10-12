/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.s2s;

import com.cai.common.constant.S2SCmd;
import com.cai.common.handler.IServerHandler;
import com.cai.service.C2SSessionService;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.CommonProto;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;

/**
 * 
 * 
 *
 * @author wu_hc date: 2018年4月24日 上午10:25:22 <br/>
 */
@IServerCmd(code = S2SCmd.SEND_TO_ALL_CLIENT, desc = "发消息给所有在线玩家")
public class SendAllOLHandler extends IServerHandler<CommonProto> {

	@Override
	public void execute(CommonProto resp, S2SSession session) throws Exception {
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.S2C);
		responseBuilder.setExtension(Protocol.s2CResponse, resp);

		C2SSessionService.getInstance().sendAllOLPlayers(responseBuilder);
	}
}
