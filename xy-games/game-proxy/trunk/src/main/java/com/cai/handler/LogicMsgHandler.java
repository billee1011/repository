/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.handler;

import com.cai.common.define.EServerType;
import com.cai.common.util.SessionUtil;
import com.cai.service.ClientServiceImpl;
import com.cai.util.MessageResponse;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;

/**
 *
 * @author wu_hc
 */
@Deprecated
@ICmd(code = RequestType.LOGIC_ROOM_VALUE, exName = "request", msgType = EServerType.LOGIC)
public final class LogicMsgHandler extends IClientHandler<Request> {

	@Override
	protected void execute(Request message, Request topRequest, C2SSession session) throws Exception {
		Request logicRequest = MessageResponse.getLogicRequest(message.toBuilder(), session).build();
		int logicSvrId = SessionUtil.getLastAccessLogicSvrId(session);
		if (logicSvrId <= 0) {
			logger.error("玩家请求[{}] LOGIC_ROOM_VALUE，但没有相关逻辑服.", session.getAccount());
			return;
		}
		ClientServiceImpl.getInstance().sendMsg(logicSvrId, logicRequest);
	}

}
