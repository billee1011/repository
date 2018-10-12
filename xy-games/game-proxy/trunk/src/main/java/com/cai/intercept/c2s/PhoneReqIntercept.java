/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.intercept.c2s;

import com.cai.common.constant.C2SCmd;
import com.cai.common.util.GlobalExecutor;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.ReqExExecutor;

import protobuf.clazz.Protocol.CommonProto;
import protobuf.clazz.Protocol.Request;

/**
 * 
 * 
 *
 * @author wu_hc date: 2017年12月5日 下午9:10:32 <br/>
 */
public final class PhoneReqIntercept implements ReqIntercept {

	@Override
	public boolean intercept(CommonProto commProto, Request topRequest, C2SSession session) {
		int cmd = commProto.getCmd();

		if (cmd == C2SCmd.PHONE_IDENTIFY_CODE || cmd == C2SCmd.PHONE_LOGIN_REQ) {
			GlobalExecutor.asyn_execute(new ReqExExecutor(commProto, topRequest, session));
			return true;
		}
		return false;
	}

}
