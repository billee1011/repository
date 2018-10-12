/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import com.cai.common.constant.C2SCmd;
import com.cai.common.define.EPlayerStatus;
import com.cai.module.LoginModule;
import com.cai.service.C2SSessionService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.c2s.C2SProto.EmptyReq;

/**
 * 
 * 
 *
 * @author wu_hc date: 2018年1月10日 下午2:49:05 <br/>
 */
@ICmd(code = C2SCmd.CLIENT_LOGIN_FINISH, desc = "客户端判定登陆完成")
public final class ClientLoginSucessHandler extends IClientHandler<EmptyReq> {

	@Override
	protected void execute(EmptyReq req, Request topRequest, C2SSession session) throws Exception {
		C2SSessionService service = C2SSessionService.getInstance();

		service.notifyClub(session, EPlayerStatus.ONLINE);
		service.notifyGate(session, EPlayerStatus.ONLINE);

		if (LoginModule.tryEnterRoomWhenLoginSuccess) {
			LoginModule.enterRoomIfExsit(session);
		}
	}
}
