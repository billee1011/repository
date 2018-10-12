package com.lingyu.admin.config;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JDBCConfigManager {
	private static final Logger logger = LogManager
			.getLogger(JDBCConfigManager.class);
	public final static String ADMIN_CONFIG = "ADMIN_CONFIG";

	public static String getConfigFile() {
//		String filePath = System.getenv(ADMIN_CONFIG);
		String filePath = "E:\\mahjong\\trunk\\admin\\conf\\admin-config.xml"; 
		logger.info("加载DB配置文件开始 path={}", filePath);
		File file = new File(filePath);
		if (!file.exists()) {
			logger.error("jdbc配置文件不存在：{}", filePath);
			return null;
		}
		logger.info("加载DB配置文件完毕 path={}", filePath);
		return "file:" + filePath;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println(getConfigFile());
	}
}
