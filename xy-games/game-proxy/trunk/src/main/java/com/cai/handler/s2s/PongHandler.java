/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.s2s;

import com.cai.common.constant.S2SCmd;
import com.cai.common.handler.IServerHandler;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.s2s.S2SProto.Pong;

/**
 * 
 *
 * @author wu_hc date: 2017年9月1日 下午12:31:20 <br/>
 */
@IServerCmd(code = S2SCmd.PONG, desc = "ping pong 返回")
public class PongHandler extends IServerHandler<Pong> {

	@Override
	public void execute(Pong resp, S2SSession session) throws Exception {

	}
}
