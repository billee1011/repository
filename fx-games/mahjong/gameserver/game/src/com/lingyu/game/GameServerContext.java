package com.lingyu.game;

import com.lingyu.common.config.ServerConfig;
import com.lingyu.common.core.AbstractServerContext;
import com.lingyu.common.db.GameRepository;
import com.lingyu.game.service.back.BackServerManager;
import com.lingyu.game.service.job.AsyncManager;
import com.lingyu.noark.data.DataManager;

public class GameServerContext extends AbstractServerContext {
	private static GameRepository gameRepository;
	private static AsyncManager asyncManager;
	private static DataManager dataManager;
	private static RouteManager routeManager;
	private static BackServerManager backServerManager;

	private static ServerConfig appConfig;


	public static AsyncManager getAsyncManager() {
		return asyncManager;
	}

	public static void setAsyncManager(AsyncManager asyncManager) {
		GameServerContext.asyncManager = asyncManager;
	}

	public static BackServerManager getBackServerManager() {
		return backServerManager;
	}

	public static void setBackServerManager(BackServerManager backServerManager) {
		GameServerContext.backServerManager = backServerManager;
	}

	public static RouteManager getRouteManager() {
		return routeManager;
	}

	public static void setRouteManager(RouteManager routeManager) {
		GameServerContext.routeManager = routeManager;
	}

	public static GameRepository getGameRepository() {
		return gameRepository;
	}

	public static void setGameRepository(GameRepository gameRepository) {
		GameServerContext.gameRepository = gameRepository;
	}

	public static DataManager getDataManager() {
		return dataManager;
	}

	public static void setDataManager(DataManager dataManager) {
		GameServerContext.dataManager = dataManager;
	}

	public static ServerConfig getAppConfig() {
		return appConfig;
	}

	public static void setAppConfig(ServerConfig appConfig) {
		GameServerContext.appConfig = appConfig;
	}

}
