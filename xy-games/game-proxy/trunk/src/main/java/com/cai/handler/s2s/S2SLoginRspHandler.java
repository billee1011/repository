/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.s2s;

import com.cai.common.constant.S2SCmd;
import com.cai.common.define.EServerType;
import com.cai.common.handler.IServerHandler;
import com.cai.service.C2SSessionService;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.s2s.S2SProto.LoginRsp;

/**
 * 登陆服务器回调
 *
 * @author wu_hc date: 2017年8月29日 下午5:21:00 <br/>
 */
@IServerCmd(code = S2SCmd.S2S_LOGIN_RSP, desc = "登陆服务器返回")
public class S2SLoginRspHandler extends IServerHandler<LoginRsp> {

	@Override
	public void execute(LoginRsp resp, S2SSession session) throws Exception {

		EServerType serverType = EServerType.type(resp.getServerType());
		logger.info("##### req login[ {} ] success,channel:{},resp:{}", serverType.name(), session.channel(), resp);

		// 如果是登陆网关服成功，把当前玩家在线状态同步
		if (EServerType.GATE == serverType) {
			C2SSessionService.getInstance().notifyGateAllAccountOnline();
		} else if (EServerType.CLUB == serverType) {
			C2SSessionService.getInstance().notifyClubAllAccountOnline();
		}
	}
}
