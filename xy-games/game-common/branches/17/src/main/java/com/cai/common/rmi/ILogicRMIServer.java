package com.cai.common.rmi;

import com.cai.common.domain.LogicStatusModel;

public interface ILogicRMIServer {
	
	public String sayHello();
	
	public Long getCurDate();

	/**
	 * 状态
	 * @return
	 */
	public LogicStatusModel getLogicStatus();
	
	/**
	 * 测试是否通
	 * @return
	 */
	public boolean test();


}


