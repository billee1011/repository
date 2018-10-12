/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.define;

/**
 * 
 *
 * @author wu_hc date: 2017年8月9日 上午10:53:14 <br/>
 */
public interface LoginRspType {

	// 1=微信登录返回(wxLoginItemResponse,error_code)
	int WX = 1;

	// 2=快速登录返回(fastLogingItemResponse)
	int FAST = 2;

	// 3=平台转码微信登录返回(wxLoginItemResponse,error_code)
	int WX_CACHE = 3;

	// 4=通知客户端最低版本要求(clientVersionRequireResponse)
	int VERSION = 4;

	// 5=返回客户端ip
	int IP = 5;
}
