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
	
	/**
	 * 后台测试牌型
	 * @param cards  
	 */
	public String testCard(String cards);


}


