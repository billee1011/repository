/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.game.network;

import com.game.common.IClientHandler;
import com.game.service.HandlerService;
import com.google.protobuf.GeneratedMessage;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.Protocol.Response;

/**
 * 请求执行器
 * 
 * @author wu_hc
 */
public final class RspExecutor implements Runnable {

	/**
	 * 处理器
	 */
	private static final HandlerService HANDLER = HandlerService.getInstance();

	/**
	 * 请求包[顶层]
	 */
	private final Response response;

	/**
	 * 会话
	 */
	private final S2SSession session;

	/**
	 * 
	 * @param request
	 * @param session
	 */
	public RspExecutor(Response response, S2SSession session) {
		this.response = response;
		this.session = session;
	}

	@Override
	public void run() {
		try {
			int id = response.getResponseType().getNumber();
			IClientHandler<? extends GeneratedMessage> handler = HANDLER.getHandler(id);

			if (null == handler) {
				return;
			}
			if (null == handler.getFieldDescriptor()) {
				handler.doExecute(null, response, session);
			} else {
				Object message = response.getField(handler.getFieldDescriptor());
				handler.doExecute(message, response, session);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
	}

}
