/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import com.cai.tasks.PressGetFreeRoomTask;
import com.cai.util.PressUtil;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.c2s.C2SProto.EmptyReq;

/**
 * 
 *
 * @author wu_hc date: 2017年10月25日 下午4:38:46 <br/>
 */
//@ICmd(code = C2SCmd.FAST_JOIN_ROOM, desc = "快速加入房间,此处对redis压力很大，刚好可以测试redis性能")
public final class FastJoinRoomHandler extends IClientHandler<EmptyReq> {

	@Override
	protected void execute(EmptyReq req, Request topRequest, final C2SSession session) throws Exception {
		PressUtil.EXECUTOR.execute(new PressGetFreeRoomTask(session.channel()));
	}
}
