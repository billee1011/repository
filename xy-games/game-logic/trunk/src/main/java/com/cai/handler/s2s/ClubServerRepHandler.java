/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.s2s;

import com.cai.common.constant.S2SCmd;
import com.cai.common.handler.IServerHandler;
import com.cai.core.SystemConfig;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.s2s.S2SProto.Pong;

/**
 * 
 */
@IServerCmd(code = S2SCmd.CLUB_REQ, desc = "俱乐部")
public class ClubServerRepHandler extends IServerHandler<Pong> {

	@Override
	public void execute(Pong resp, S2SSession session) throws Exception {

		if (SystemConfig.gameDebug == 1) {
			System.out.println(resp);
		}
	}
}
