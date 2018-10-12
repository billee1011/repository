package com.lingyu.common.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class SpringContainer implements ApplicationContextAware {

	private static ApplicationContext context; 
	
	public SpringContainer(){
	}
	
	public static  <T> T getBean(Class<T> classType) {
		return context.getBean(classType);
	}

	public static  Object getBean(String beanName) {
		return context.getBean(beanName);
	}

	@Override
	public void setApplicationContext(ApplicationContext context)
			throws BeansException {
		SpringContainer.context = context;
	}

}
