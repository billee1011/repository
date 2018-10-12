package com.cai.rmi.impl;

import com.cai.common.rmi.IClubRMIServer;
import com.cai.common.rmi.IRMIHandler;
import com.cai.service.RMIHandlerServiceImp;

/**
 * 
 * 
 *
 * @author wu_hc date: 2017年11月20日 下午5:10:12 <br/>
 */
public class ClubRMIServerImpl implements IClubRMIServer {

	@Override
	public <T, R> R rmiInvoke(int cmd, T message) {
		IRMIHandler<T, R> handler = RMIHandlerServiceImp.getInstance().getHandler(cmd);
		if (null != handler) {
			return handler.apply(message);
		}
		return null;
	}

}
