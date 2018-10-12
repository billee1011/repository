package com.lingyu.game;

/**
 * GameServer服务器启动入口.
 * @author wangning
 * @date 2016年11月29日 上午10:43:49
 */
public class GameServer {

	public static void main(String[] args) {
		GameServerService service = new GameServerService(args);
		service.start();
	}
}