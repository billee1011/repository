/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import com.cai.common.constant.S2SCmd;
import com.cai.common.define.EServerType;
import com.cai.domain.Session;
import com.cai.service.SessionServiceImpl;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.s2s.S2SProto.LoginReq;

/**
 * @author wu date: 2017年8月29日 下午5:21:00 <br/>
 */
@IServerCmd(code = S2SCmd.S2S_LOGIN_REQ, desc = "代理服登录")
public class S2SLoginReqHandler extends IClientHandler<LoginReq> {

	@Override
	protected void execute(LoginReq req, Session session) throws Exception {
	    EServerType serverType = EServerType.type(req.getServerType());
		
		SessionServiceImpl.getInstance().online(serverType, req.getServerIndex(), session);
		logger.info("##### req login[ {} ] success,channel:{},resp:{}", serverType.name(), session.getChannel(), req);
	}
}
