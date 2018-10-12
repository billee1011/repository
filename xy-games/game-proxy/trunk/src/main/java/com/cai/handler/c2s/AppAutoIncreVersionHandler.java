/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.util.PBUtil;
import com.cai.dictionary.AppItemDict;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.c2s.C2SProto.AppAutoIncreVersionRsp;
import protobuf.clazz.c2s.C2SProto.EmptyReq;

/**
 * 
 *
 * @author wu_hc date: 2017年7月31日 上午11:11:00 <br/>
 */
@ICmd(code = C2SCmd.APP_AUTO_INCR_VERSION, desc = "app推送")
public final class AppAutoIncreVersionHandler extends IClientHandler<EmptyReq> {

	@Override
	protected void execute(EmptyReq req, Request topRequest, C2SSession session) throws Exception {

		AppAutoIncreVersionRsp.Builder builder = AppAutoIncreVersionRsp.newBuilder();
		builder.setVersion((int) AppItemDict.getInstance().getAutoIncreVersion());
		session.send(PBUtil.toS2CCommonRsp(S2CCmd.APP_AUTO_INCR_VERSION, builder));
	}
}
