/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.util;

import com.cai.common.define.EServerType;
import com.cai.common.rmi.IClubRMIServer;
import com.cai.common.rmi.ILogicRMIServer;
import com.cai.common.rmi.IProxyRMIServer;
import com.cai.common.util.SpringService;
import com.cai.service.RMIServiceImpl;

/**
 * 
 *
 * @author wu_hc date: 2017年10月17日 下午3:42:47 <br/>
 */
public final class RMIMsgSender {

	/**
	 * 
	 * @param eType
	 * @param serverIndex
	 * @param cmd
	 * @param message
	 */
	public static <T, R> R call(final EServerType eType, int serverIndex, int cmd, T message) {

		if (EServerType.PROXY == eType) {
			IProxyRMIServer rmiServer = RMIServiceImpl.getInstance().getIProxyRMIByIndex(serverIndex);
			if (null != rmiServer) {
				rmiServer.rmiInvoke(cmd, message);
			}
		} else if (EServerType.LOGIC == eType) {
			ILogicRMIServer rmiServer = RMIServiceImpl.getInstance().getLogicRMIByIndex(serverIndex);
			if (null != rmiServer) {
				return rmiServer.rmiInvoke(cmd, message);
			}
		}
		return null;

	}

	/**
	 * 
	 * @param serverIndex
	 * @param cmd
	 * @param message
	 * @return
	 */
	public static <T, R> R callLogic(int serverIndex, int cmd, T message) {
		return RMIMsgSender.call(EServerType.LOGIC, serverIndex, cmd, message);
	}

	/**
	 * 
	 * @param serverIndex
	 * @param cmd
	 * @param message
	 * @return
	 */
	public static <T, R> R callClub(int cmd, T message) {
		IClubRMIServer server = SpringService.getBean(IClubRMIServer.class);
		return server.rmiInvoke(cmd, message);
	}

	private RMIMsgSender() {
	}
}
