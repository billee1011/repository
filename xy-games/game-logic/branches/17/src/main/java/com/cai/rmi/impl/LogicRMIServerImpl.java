package com.cai.rmi.impl;

import com.cai.common.domain.LogicStatusModel;
import com.cai.common.rmi.ILogicRMIServer;
import com.cai.core.RequestHandlerThreadPool;
import com.cai.core.SystemConfig;

public class LogicRMIServerImpl implements ILogicRMIServer{

	@Override
	public String sayHello() {
		System.out.println("logic say hello");
		return "logic say hello";
	}

	@Override
	public Long getCurDate() {
		return System.currentTimeMillis();
	}

	@Override
	public LogicStatusModel getLogicStatus() {
		LogicStatusModel model = new LogicStatusModel();
		model.setLogic_game_id(SystemConfig.logic_index);
		model.setOnline_playe_num(1);
		model.setSocket_connect_num(1);
		
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
