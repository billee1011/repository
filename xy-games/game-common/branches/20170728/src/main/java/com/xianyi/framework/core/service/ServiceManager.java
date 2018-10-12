/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.xianyi.framework.core.service;

import java.lang.reflect.Method;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.util.LoadPackageClasses;

/**
 * 服务管理中心
 * 
 * @author wu_hc
 */
public final class ServiceManager {

	/**
	 * 日志
	 */
	private static Logger log = LoggerFactory.getLogger(ServiceManager.class);

	private static final ServiceManager MGR = new ServiceManager();

	public static ServiceManager getInstance() {
		return ServiceManager.MGR;
	}

	private ServiceManager() {
	}

	@SuppressWarnings("unchecked")
	public void loadServices(String[] packagesToScan) {
		LoadPackageClasses loader = new LoadPackageClasses(packagesToScan, IService.class);
		try {

			Set<Class<?>> handlerClassz = loader.getClassSet();
			for (final Class<?> cls : handlerClassz) {
				IService serviceAnnotation = cls.getAnnotation(IService.class);
				if (null == serviceAnnotation)
					throw new RuntimeException(String.format("解析服务[%s]出错，请检查注解是否正确!!", cls.getName()));
				Service clientHandler = (Service) cls.newInstance();
				Method method = cls.getMethod("start");
				method.invoke(clientHandler);
			}
		} catch (Exception e) {
			log.error("解析服务出错!", e);
			e.printStackTrace();
		}

		log.info("========服务初始化完成========");
	}
	
	public static void main(String[] args){
		ServiceManager.getInstance().loadServices(new String[]{"com.xianyi.framework.core.service"});
	}
}
