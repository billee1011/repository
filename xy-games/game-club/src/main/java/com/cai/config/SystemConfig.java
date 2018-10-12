package com.cai.config;

import com.cai.common.util.PropertiesUtil;

import org.apache.commons.lang.StringUtils;

public class SystemConfig {

	public static int webPort;

	/**
	 * 端口
	 */
	public static int game_socket_port;

	/**
	 * 编号
	 */
	public static int club_index;

	/**
	 * 1=开启调试
	 */
	public static int gameDebug = 0;

	public static String clubToGroup;

	public static String DEFAULT = "http://39.108.11.126/api/route/init/scheduled/data";

	public static String webSecret;

	public static void init(PropertiesUtil prop) {
		game_socket_port = Integer.parseInt(prop.getProperty("game.socket.port"));
		club_index = Integer.parseInt(prop.getProperty("game.club_index"));
		gameDebug = Integer.parseInt(prop.getProperty("game.debug"));
		clubToGroup = prop.getProperty("clubToGroup");

		webPort = Integer.parseInt(prop.getProperty("game.webPort"));
		webSecret = prop.getProperty("game.webSecret");

		if (StringUtils.isEmpty(clubToGroup)) {
			if (gameDebug == 0) {
				clubToGroup = DEFAULT;
			} else {
				clubToGroup = "http://39.108.11.126:8018/api/route/init/scheduled/data";
			}

		}
	}

}
