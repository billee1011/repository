/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import com.cai.common.constant.C2SCmd;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.c2s.C2SProto.SceneReq;

/**
 * 
 *
 * @author wu_hc date: 2017年8月02日 上午16:11:00 <br/>
 */
@ICmd(code = C2SCmd.SECNE_REQ, desc = "场景相关")
public final class SceneReqHandler extends IClientHandler<SceneReq> {

	@Override
	protected void execute(SceneReq req, Request topRequest, C2SSession session) throws Exception {
	}
}
