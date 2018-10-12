/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import com.cai.common.constant.S2SCmd;
import com.cai.common.define.EServerStatus;
import com.cai.common.define.EServerType;
import com.cai.common.util.PBUtil;
import com.cai.config.SystemConfig;
import com.cai.service.SessionService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;
import protobuf.clazz.s2s.S2SProto.LoginReq;
import protobuf.clazz.s2s.S2SProto.LoginRsp;

/**
 * 
 *
 * @author wu_hc date: 2017年8月29日 下午3:58:43 <br/>
 */
@ICmd(code = S2SCmd.S2S_LOGIN_REQ, desc = "服务器请求登陆")
public final class ServerLoginHandler extends IClientHandler<LoginReq> {

	/**
	 * 安全码，写死
	 */
	private static final String SAFE_CODE = "DFASE##@546654";   

	@Override
	public void execute(LoginReq req, C2SSession session) throws Exception {

		// 1处理请求
		String safeCode = req.getSafeCode();
		if (!SAFE_CODE.equals(safeCode)) {
			logger.error("client request login,but safe code error!channel:{},safecode:{}", session.channel(), safeCode);
			session.shutdownGracefully();
			return;
		}

		int serverType = req.getServerType();
		EServerType eType = EServerType.type(serverType);
		if (EServerType.UNKOWN == eType || (eType != EServerType.PROXY && eType != EServerType.LOGIC)) {
			logger.error("不支持该服务器连接到club server,channel:{},type:{}", session.channel(), eType.name());
			session.shutdownGracefully();
			return;
		}

		int serverIndex = req.getServerIndex();
		SessionService.getInstance().online(eType, serverIndex, session);

		// 2回复
		LoginRsp.Builder builder = LoginRsp.newBuilder();
		builder.setStatus(EServerStatus.ACTIVE.getStatus());
		builder.setServerIndex(SystemConfig.club_index);
		builder.setServerType(EServerType.CLUB.type());
		session.send(PBUtil.toS2SResponse(S2SCmd.S2S_LOGIN_RSP, builder));
	}
}
