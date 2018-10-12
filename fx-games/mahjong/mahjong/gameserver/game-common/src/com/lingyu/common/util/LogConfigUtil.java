package com.lingyu.common.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;

public class LogConfigUtil {
	public static final String DEFAULT_SOLO_FILE_NAME = "log4j2.xml";
	public static final String DEFAULT_MULTI_FILE_NAME = "log4j2-template.xml";
	private static final String CF_NAME_PATTERN = "log4j2_{0}_{1}.xml";
	private static final Charset DEFAULT_CHARSET = Charset.forName("utf8");

	/**
	 * 默认加载当前目录下默认配置文件
	 * 
	 * @see LogConfigUtil#DEFAULT_SOLO_FILE_NAME
	 */
	public static void configSoloFile() {
		configSoloFile(DEFAULT_SOLO_FILE_NAME);
	}

	/**
	 * 简化版本
	 * 
	 * @see LogConfigUtil#DEFAULT_MULTI_FILE_NAME
	 * @param serverName
	 * @param index
	 */
	public static void configMultiFile(String serverName, String index) {
		configMultiFile(DEFAULT_MULTI_FILE_NAME, serverName, index);
	}

	/**
	 * @param filename
	 */
	public static void configSoloFile(String filename) {
		File cf = new File(filename);
		config(cf);
	}

	/**
	 * 分布式服务器配置方案
	 * 
	 * @param templateFileName logback模板文件名
	 * @param serverName 需要替换字符串，如gate
	 * @param serverId 如10，则生成gate_10
	 */
	public static void configMultiFile(String templateFileName, String serverName, String serverId) {
		File templateFile = new File(templateFileName);
		File targetFile = new File(MessageFormat.format(CF_NAME_PATTERN, serverName, serverId));
		try {
			if (!targetFile.exists()) {
				if (templateFile.exists()) {
					boolean isCreate = targetFile.createNewFile();
					if (isCreate) {
						PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(targetFile), DEFAULT_CHARSET));
						StringBuffer config = new StringBuffer();
						String aline = "";
						FileInputStream fis = new FileInputStream(templateFile);
						BufferedReader in = new BufferedReader(new InputStreamReader(fis));
						while ((aline = in.readLine()) != null) {
							config.append(aline).append("\n");
						}
						in.close();
						Pattern p = Pattern.compile(serverName);
						Matcher m = p.matcher(config.toString());
						String realConfig = m.replaceAll(serverName + serverId);
						out.write(realConfig);
						out.flush();
						out.close();
					}
				} else {
					System.exit(1);
				}
			}
			config(targetFile);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void config(File configer) {
		File file = new File("log4j2.xml");
		BufferedInputStream in = null;
		
			try {
				in = new BufferedInputStream(new FileInputStream(file));
				final ConfigurationSource source = new ConfigurationSource(in);
				Configurator.initialize(null, source);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		

	}

	public static void main(String[] args) {
		LoggerContext ctx = Configurator.initialize("APP", "log4j2.xml");
		LogManager.getLogger("org.apache.test.TestConfigurator").info("dsdsads{}", 1);;
		Configuration config = ctx.getConfiguration();

		final Map<String, String> map = new HashMap<String, String>(config.getProperties());
		map.put("LOG_HOME", "/data/log/game2");

		ctx.reconfigure();

		final Map<String, Appender> maps = config.getAppenders();
		Collection<Appender> list = maps.values();
		for (Appender e : list) {
			if (e instanceof RollingRandomAccessFileAppender) {

			}
			System.out.println(e);
		}
		// assertNotNull("Appenders map should not be null.", map);
		// assertTrue("Appenders map should not be empty.", map.size() > 0);
		// assertTrue("Wrong configuration", map.containsKey("List"));
		Configurator.shutdown(ctx);
		config = ctx.getConfiguration();

	}
}
