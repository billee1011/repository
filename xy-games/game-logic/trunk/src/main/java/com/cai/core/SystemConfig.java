package com.cai.core;

import java.net.InetAddress;

import org.apache.commons.lang.StringUtils;

import com.cai.common.util.PropertiesUtil;

public class SystemConfig {

	public static int game_socket_port;
	public static int logic_index;
	public static String localip = "";
	public static String club_socket_host;
	public static String match_socket_host = "";
	// 比赛场
	public static int match_id = 1;
	public static int register_match_id = 1;
	// 金币场
	public static int connectCoin = 1;
	public static int needConnectCoin = 1;
	/**
	 * 1=开启调试
	 */
	public static int gameDebug = 0;

	public static int connectClub = 0;
	//

	static {
		try {
			InetAddress ia = InetAddress.getLocalHost();
			localip = ia.getHostAddress();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void init(PropertiesUtil prop) {

		game_socket_port = Integer.parseInt(prop.getProperty("game.socket.port"));
		logic_index = Integer.parseInt(prop.getProperty("game.logic_index"));
		gameDebug = Integer.parseInt(prop.getProperty("game.debug"));
		club_socket_host = prop.getProperty("club.host");
		if (StringUtils.isNotEmpty(prop.getProperty("match.host"))) {
			match_socket_host = prop.getProperty("match.host");
		}

		if (StringUtils.isNotEmpty(prop.getProperty("game.connectClub"))) {
			connectClub = Integer.parseInt(prop.getProperty("game.connectClub"));
		}

		if(StringUtils.isNotEmpty(prop.getProperty("game.connectCoin"))){
			connectCoin = Integer.parseInt(prop.getProperty("game.connectCoin"));
		}
	}

}
