package com.cai.rmi.impl;

import java.util.function.Function;

import com.cai.common.domain.ProxyStatusModel;
import com.cai.common.rmi.IProxyRMIServer;
import com.cai.core.SystemConfig;
import com.cai.service.C2SSessionService;
import com.cai.service.RMIHandlerServiceImp;
import com.xianyi.framework.core.concurrent.WorkerLoopGroup;

public class ProxyRMIServerImpl implements IProxyRMIServer {

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

		C2SSessionService service = C2SSessionService.getInstance();

		int session_count = service.getAllSessionCount();// SessionServiceImpl.getInstance().getSessionMap().size();
		int online_count = service.getOnlineCount();// SessionServiceImpl.getInstance().getOnlineSessionMap().size();
		ProxyStatusModel model = new ProxyStatusModel();
		model.setProxy_game_id(SystemConfig.proxy_index);
		model.setOnline_playe_num(online_count);
		model.setSocket_connect_num(session_count);

		WorkerLoopGroup group = service.getWorkerGroup();
		long allCount = group.getTaskCount();
		long completedCount = group.getCompletedTaskCount();
		// 消息处理情况
		model.setMsg_receive_count(allCount);
		model.setMsg_completed_count(completedCount);
		model.setMsg_queue_count(allCount > completedCount ? allCount - completedCount : 0);

		return model;
	}

	/**
	 * 测试是否通
	 * 
	 * @return
	 */
	public boolean test() {
		return true;
	}

	@Override
	public <T, R> R rmiInvoke(int cmd, T message) {
		Function<T, R> handler = RMIHandlerServiceImp.getInstance().getHandler(cmd);
		if (null != handler) {
			return handler.apply(message);
		}
		return null;
	}

}
