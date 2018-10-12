package com.lingyu.common.core;

import org.springframework.context.ApplicationContext;

import com.lingyu.common.entity.ServerInfo;

public abstract class AbstractServerContext {
	private static ApplicationContext appContext;
	private static ServerInfo serverInfo;


	public static ServerInfo getServerInfo() {
		return serverInfo;
	}

	public static void setServerInfo(ServerInfo serverInfo) {
		AbstractServerContext.serverInfo = serverInfo;
	}

	public static final <T> T getBean(Class<T> classType) {
		return appContext.getBean(classType);
	}

	public static final Object getBean(String beanName) {
		return appContext.getBean(beanName);
	}

	public static ApplicationContext getAppContext() {
		return appContext;
	}

	public static void setAppContext(ApplicationContext appContext) {
		AbstractServerContext.appContext = appContext;
	}
}
