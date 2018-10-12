/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import com.cai.common.constant.S2SCmd;
import com.cai.common.util.PBUtil;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.s2s.S2SProto.Ping;
import protobuf.clazz.s2s.S2SProto.Pong;

/**
 * @author wu_hc date: 2017年9月1日 下午12:33:47 <br/>
 */
@ICmd(code = S2SCmd.PING, desc = "服务器Ping请求")
public class PingHandler extends IClientHandler<Ping> {

	static final Pong.Builder builder = Pong.newBuilder();

	@Override
	protected void execute(Ping message, C2SSession session) throws Exception {
		session.send(PBUtil.toS2SResponse(S2SCmd.PONG, builder));
	}
}
