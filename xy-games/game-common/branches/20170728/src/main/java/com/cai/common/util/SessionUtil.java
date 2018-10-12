/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.common.util;

import com.xianyi.framework.core.transport.netty.session.C2SSession;

/**
 *
 * @author wu_hc
 */
public final class SessionUtil {
	private SessionUtil() {
	}

	/**
	 * 
	 * @param session
	 * @return
	 */
	public static int getLogicSvrId(C2SSession session) {
		Integer logicSvrId = session.attr(C2SSession.SESSION_LOGIC_ID).get();
		return null != logicSvrId ? logicSvrId.intValue() : -1;
	}

	/**
	 * 
	 * @param session
	 * @param logicSvrId
	 */
	public static void setLogicSvrId(C2SSession session, int logicSvrId) {
		session.attr(C2SSession.SESSION_LOGIC_ID).set(logicSvrId);
	}
}
