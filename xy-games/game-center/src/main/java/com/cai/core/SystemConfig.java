package com.cai.core;

import java.net.InetAddress;

import org.apache.commons.lang.StringUtils;

import com.cai.common.util.PropertiesUtil;
import com.google.common.base.Preconditions;


public class SystemConfig {
	
	/**
	 * 1=开启调试
	 */
	public static int gameDebug = 0;
	
	public static String localip = "";
	
	/**
	 * web端口
	 */
	public static int webPort;

	/**
	 * web签名安全码
	 */
	public static String webSecret;
	
	
	public static String clubToGroup;
	
	public static String DEFAULT = "http://39.108.11.126/api/route/init/scheduled/data";
	
	//
	
	static{
		try {
			InetAddress ia = InetAddress.getLocalHost();
			localip = ia.getHostAddress();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public static void init(PropertiesUtil prop){
		gameDebug = Integer.parseInt(prop.getProperty("game.debug"));
		webPort = Integer.parseInt(prop.getProperty("game.webPort"));
		webSecret = prop.getProperty("game.webSecret");
		clubToGroup =  prop.getProperty("clubToGroup");
		if(StringUtils.isEmpty(clubToGroup)){
			if(gameDebug == 0){
				clubToGroup = DEFAULT;
			}else {
				clubToGroup = "http://39.108.11.126:8081/api/route/init/scheduled/data";
			}
			
		}
	
	}

}
