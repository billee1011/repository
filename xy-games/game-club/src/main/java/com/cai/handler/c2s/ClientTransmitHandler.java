/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import com.cai.common.constant.S2SCmd;
import com.cai.service.ClientHandlerService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.CommonProto;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author wu_hc date: 2017年10月19日 上午11:26:37 <br/>
 */
@ICmd(code = S2SCmd.C_2_CLUB, desc = "客户端直接发过来的协议，代理服帮忙转发")
public final class ClientTransmitHandler extends IClientHandler<TransmitProto> {

	@Override
	protected void execute(TransmitProto req, C2SSession session) throws Exception {
		CommonProto commonProto = req.getCommonProto();
		int cmd = commonProto.getCmd();

		try {
			IClientExHandler<?> handler = ClientHandlerService.getInstance().getHandler(cmd);
			if (null != handler) {
				Object message = handler.toObject(commonProto);
				handler.doExecute(message, req, session);
			} else {
				logger.error("##########玩家:{} 请求[{}]号协议，但没有找到相应的处理器!#########", req.getAccountId(), cmd);
			}
		} catch (Exception e) {
			logger.error("##########处理消息错误,玩家:{},请求类型:{},e:{}", req.getAccountId(), cmd, e);
		}

	}
}
