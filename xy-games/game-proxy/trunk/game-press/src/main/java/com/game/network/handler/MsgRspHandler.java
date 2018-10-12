/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.game.network.handler;

import com.game.common.IClientHandler;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.Protocol.MsgAllResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;

/**
 * 
 * 
 * @author wu_hc date: 2017年10月13日 下午3:03:12 <br/>
 */
@ICmd(code = ResponseType.MSG_VALUE, exName = "msgAllResponse")
public final class MsgRspHandler extends IClientHandler<MsgAllResponse> {

	@Override
	protected void execute(MsgAllResponse rsp, Response response, S2SSession session) throws Exception {
		System.out.println(new String(rsp.getMsg().getBytes(), "utf-8"));
	}
}
