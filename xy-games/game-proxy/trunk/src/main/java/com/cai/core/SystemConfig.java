package com.cai.core;

import java.net.InetAddress;

import org.apache.commons.lang.StringUtils;

import com.cai.common.util.PropertiesUtil;


public class SystemConfig {
	
	/**
	 * 游戏端口
	 */
	public static int game_socket_port;
	public static int game_websocket_port;
	/**
	 * 代理标识
	 */
	public static int proxy_index;
	public static int match_index = 1;
	public static int register_match_id = 1;
	
	
	public static int logic_sockcet_port;
	public static String loginc_socket_ip;
	public static String club_socket_host;
	public static String match_socket_host = "";
	
	public static String localip = "";
	
	/**
	 * 1=开启调试
	 */
	public static int gameDebug = 0;
	
	public static int connectClub = 0;
	
	public static int connectCoin = 1;
	public static int needConnectCoin = 1;

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
		
		game_socket_port = Integer.parseInt(prop.getProperty("game.socket.port"));
		proxy_index = Integer.parseInt(prop.getProperty("game.proxy_index"));
		
		//===============================================
		
		logic_sockcet_port = Integer.parseInt(prop.getProperty("logic.sockcet.port"));
		loginc_socket_ip = prop.getProperty("logic.socket.ip");
		gameDebug = Integer.parseInt(prop.getProperty("game.debug"));
		club_socket_host = prop.getProperty("club.host");
		if(StringUtils.isNotEmpty(prop.getProperty("game.connectClub"))){
			connectClub = Integer.parseInt(prop.getProperty("game.connectClub"));
		}
		if(StringUtils.isNotEmpty(prop.getProperty("game.connectCoin"))){
			connectCoin = Integer.parseInt(prop.getProperty("game.connectCoin"));
		}
		
		if(StringUtils.isNotEmpty(prop.getProperty("match.host"))){
			match_socket_host = prop.getProperty("match.host");
		}
			
	}

}
