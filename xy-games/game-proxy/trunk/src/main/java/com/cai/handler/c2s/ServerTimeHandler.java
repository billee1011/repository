/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.util.PBUtil;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.c2s.C2SProto.ServerTimeReq;
import protobuf.clazz.c2s.C2SProto.ServerTimeRsp;

/**
 * 
 *
 * @author wu_hc date: 2017年10月10日 上午17:11:00 <br/>
 */
@ICmd(code = C2SCmd.SERVER_TIME, desc = "请求服务器当前时间")
public final class ServerTimeHandler extends IClientHandler<ServerTimeReq> {

	@Override
	protected void execute(ServerTimeReq req, Request topRequest, C2SSession session) throws Exception {

		if (null == session.getAccount()) {
			logger.error("连接:[{}]请求服务器时间，但该连接还完成登陆操作!!", session.channel());
			return;
		}
		ServerTimeRsp.Builder builder = ServerTimeRsp.newBuilder();
		builder.setClientTime(req.getClientTime());
		builder.setServerTime((int) (System.currentTimeMillis() / 1000));
		session.send(PBUtil.toS2CCommonRsp(S2CCmd.SERVER_TIME, builder));
	}
}
