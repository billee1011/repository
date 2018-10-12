/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.handler;

import com.cai.common.define.EMsgType;
import com.cai.domain.Session;
import com.cai.service.ClientServiceImpl;
import com.cai.util.MessageResponse;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;

/**
 *
 * @author wu_hc
 */
@ICmd(code = RequestType.LOGIC_ROOM_VALUE, exName = "request", msgType = EMsgType.LOGIC_MSG)
public final class LogicMsgHandler extends IClientHandler<Request> {

	@Override
	protected void execute(Request message, Request topRequest, Session session) throws Exception {
		Request logicRequest = MessageResponse.getLogicRequest(message.toBuilder(), session).build();
		ClientServiceImpl.getInstance().sendMsg(logicRequest);
	}

}
