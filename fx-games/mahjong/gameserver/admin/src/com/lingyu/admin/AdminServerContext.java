package com.lingyu.admin;

import javax.servlet.ServletContext;

import org.springframework.context.ApplicationContext;

/**
 * 
 * 
 */
public class AdminServerContext {
	private static ApplicationContext applicationContext;

	private static ServletContext servletContext;

	private static String version = "";

	public static String getVersion() {
		return version;
	}

	public static void setVersion(String version) {
		AdminServerContext.version = version;
	}

	public static ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public static void setApplicationContext(ApplicationContext applicationContext) {
		AdminServerContext.applicationContext = applicationContext;
	}

	public static ServletContext getServletContext() {
		return servletContext;
	}

	public static void setServletContext(ServletContext servletContext) {
		AdminServerContext.servletContext = servletContext;
	}

	/**
	 * 
	 * @param classType
	 * @return
	 */
	public static final <T> T getBean(Class<T> classType) {
		return applicationContext.getBean(classType);
	}

	/**
	 * 
	 * @param classType
	 * @return
	 */
	public static final Object getBean(String beanName) {
		return applicationContext.getBean(beanName);
	}

}
