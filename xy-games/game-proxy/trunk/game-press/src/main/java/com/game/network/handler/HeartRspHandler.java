/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.game.network.handler;

import com.game.common.IClientHandler;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.Protocol.HeartResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;

/**
 * 
 * 
 * @author wu_hc date: 2017年10月13日 下午3:03:12 <br/>
 */
@ICmd(code = ResponseType.HEAR_VALUE, exName = "heartResponse")
public final class HeartRspHandler extends IClientHandler<HeartResponse> {

	@Override
	protected void execute(HeartResponse rsp, Response response, S2SSession session) throws Exception {
//		System.out.println(rsp);
	}
}
