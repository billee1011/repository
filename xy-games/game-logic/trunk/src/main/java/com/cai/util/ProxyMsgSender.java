package com.cai.util;

import com.cai.common.constant.S2SCmd;
import com.cai.common.util.PBUtil;
import com.cai.service.SessionServiceImpl;
import com.google.protobuf.GeneratedMessage;

import protobuf.clazz.s2s.S2SProto.S2STransmitProto;

/**
 * 
 * 
 *
 * @author wu_hc date: 2017年12月28日 下午3:37:37 <br/>
 */
public final class ProxyMsgSender {

	/**
	 * 发送消息给玩家所在的代理服.
	 * 
	 * @param accountId
	 * @param cmd
	 * @param builder
	 */
	public static void sendToProxyWithAccountId(long accountId, int cmd, GeneratedMessage.Builder<?> builder) {
		final SessionServiceImpl sender = SessionServiceImpl.getInstance();

		S2STransmitProto.Builder transmitBuilder = S2STransmitProto.newBuilder();
		transmitBuilder.setAccountId(accountId);
		transmitBuilder.setRequest(PBUtil.toS2SResponse(cmd, builder));
		sender.sendGate(PBUtil.toS2SRequet(S2SCmd.S_G_S, transmitBuilder).build());

	}
}
