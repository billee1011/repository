package com.cai.common.util;

public class IpUtil {
	
	/**
	 * 白名单ip
	 * @param ip
	 * @return
	 */
	public static boolean isWhiteIp(String ip){
		if(ip==null || "".equals(ip.trim()))
			return false;
		
		if("118.184.248.67".equals(ip) || "118.184.248.68".equals(ip)  || "118.184.248.70".equals(ip))
			return true;
		
		return false;
	}

}
