package com.cai.rmi.impl;

import com.cai.common.domain.ProxyStatusModel;
import com.cai.common.rmi.IProxyRMIServer;
import com.cai.core.RequestHandlerThreadPool;
import com.cai.core.SystemConfig;
import com.cai.service.SessionServiceImpl;

public class ProxyRMIServerImpl implements IProxyRMIServer{

	@Override
	public String sayHello() {
		System.out.println("proxy say hello");
		return "proxy say hello";
	}

	@Override
	public Long getCurDate() {
		return System.currentTimeMillis();
	}

	@Override
	public ProxyStatusModel getProxyStatus() {
		int session_count = SessionServiceImpl.getInstance().getSessionMap().size();
		int online_count = SessionServiceImpl.getInstance().getOnlineSessionMap().size();
		ProxyStatusModel model = new ProxyStatusModel();
		model.setProxy_game_id(SystemConfig.proxy_index);
		model.setOnline_playe_num(online_count);
		model.setSocket_connect_num(session_count);
		
		//消息处理情况
		model.setMsg_receive_count(RequestHandlerThreadPool.getInstance().getTpe().getTaskCount());
		model.setMsg_completed_count(RequestHandlerThreadPool.getInstance().getTpe().getCompletedTaskCount());
		model.setMsg_queue_count(RequestHandlerThreadPool.getInstance().getBlockQueue().size());
		
		return model;
	}
	
	/**
	 * 测试是否通
	 * @return
	 */
	public boolean test(){
		return true;
	}

}
