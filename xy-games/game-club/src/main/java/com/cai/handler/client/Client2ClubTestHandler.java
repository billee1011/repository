/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.c2s.C2SProto.ServerTimeReq;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author wu_hc date: 2017年10月19日 上午11:48:33 <br/>
 */
@ICmd(code = C2SCmd.SERVER_TIME, desc = "")
public final class Client2ClubTestHandler extends IClientExHandler<ServerTimeReq> {

	@Override
	protected void execute(ServerTimeReq req, TransmitProto topReq, C2SSession session) throws Exception {
		System.out.println(req);
	}

}
