package com.cai.core;

public enum ThreadNameEnum {
	/***
	 * 逻辑服nettyBoss线程组
	 */
	GAME_BOSS_SERVER("逻辑服nettyBoss线程组"),
	/***
	 * 逻辑服nettyWork线程组
	 */
	GAME_WORK_SERVER("逻辑服nettyWork线程组"),
	/**
	 * 数据库持久化线程
	 */
	DB_SERVER("数据库持久化线程"),
	/***
	 * Quartz调度线程
	 */
	QUARTZ_THREAD("QuartzThread"),
	/***
	 * 业务逻辑线程
	 */
	LOGIC_SERVER("业务逻辑线程"),

	/**
	 * 异步处理线程池
	 */
	ASYN_WORK_THREAD("异步处理线程池"),

	/**
	 * DB检测线程
	 */
	DB_CHECK_THREAD("DB检测线程"),
	
	/**
	 * 微信登录线程
	 */
	WX_LOGIN_THREAD("微信登录线程");
	
	

	private String code;

	private ThreadNameEnum(String code) {
		this.code = code;
	}

	public String getCode() {
		return this.code;
	}
}
