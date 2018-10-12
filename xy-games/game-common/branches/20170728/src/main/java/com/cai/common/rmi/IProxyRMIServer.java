package com.cai.common.rmi;

import com.cai.common.domain.ProxyStatusModel;

public interface IProxyRMIServer {
	
	public String sayHello();
	
	public Long getCurDate();
	
	/**
	 * 状态
	 * @return
	 */
	public ProxyStatusModel getProxyStatus();
	
	/**
	 * 测试是否通
	 * @return
	 */
	public boolean test();
	


}


