/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.xianyi.framework.handler;

import com.google.protobuf.GeneratedMessage;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

/**
 * 
 *
 * @author wu_hc date: 2017年7月24日 下午6:31:39 <br/>
 */
public abstract class IServerHandler<T extends GeneratedMessage> {

	@SuppressWarnings("unchecked")
	public void doExecute(Object message, S2SSession session) throws Exception {
		this.execute((T) message, session);
	}

	/**
	 * 
	 * @param message
	 * @param topRequest
	 * @param session
	 * @throws Exception
	 */
	public abstract void execute(T message, S2SSession session) throws Exception;
}
