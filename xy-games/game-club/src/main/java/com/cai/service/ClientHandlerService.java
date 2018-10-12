/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.service;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.util.LoadPackageClasses;
import com.cai.constant.ServiceOrder;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Parser;
import com.xianyi.framework.core.service.AbstractService;
import com.xianyi.framework.core.service.IService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.handler.IClientExHandler;

import javolution.util.FastMap;

/**
 * 
 * 
 *
 * @author wu_hc date: 2017年10月19日 下午12:07:44 <br/>
 */
@IService(order = ServiceOrder.CLIENT_HANDLER)
public final class ClientHandlerService extends AbstractService {

	/**
	 * 日志
	 */
	private static Logger log = LoggerFactory.getLogger(ClientHandlerService.class);

	private static ClientHandlerService INstance = new ClientHandlerService();

	private final FastMap<Integer, IClientExHandler<?>> mapping = new FastMap<>();

	public static ClientHandlerService getInstance() {
		return INstance;
	}

	/**
	 * 获得处理器
	 * 
	 * @param cmd
	 * @return
	 */
	public IClientExHandler<?> getHandler(int cmd) {
		return mapping.get(cmd);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void start() throws Exception {
		LoadPackageClasses loader = new LoadPackageClasses(new String[] { "com.cai.handler.client" }, ICmd.class);
		try {

			Set<Class<?>> handlerClassz = loader.getClassSet();
			for (final Class<?> cls : handlerClassz) {
				ICmd cmdAnnotation = cls.getAnnotation(ICmd.class);
				if (null == cmdAnnotation)
					throw new RuntimeException(String.format("解析处理器[%s]出错，请检查注解是否正确!!", cls.getName()));

				IClientExHandler<?> handler = (IClientExHandler<?>) cls.newInstance();

				Class<?> clazz = cls;
				while (clazz != Object.class) {
					Type t = clazz.getGenericSuperclass();
					if (t instanceof ParameterizedType) {
						Type[] args = ((ParameterizedType) t).getActualTypeArguments();
						if (args[0] instanceof Class) {
							clazz = (Class<?>) args[0];
							Parser<? extends GeneratedMessage> parser;
							parser = (Parser<? extends GeneratedMessage>) ((GeneratedMessage) clazz.getMethod("getDefaultInstance").invoke(null))
									.getParserForType();
							handler.setParse(parser);
							break;
						}
					}
					clazz = clazz.getSuperclass();
				}
				if (clazz == Object.class) {
					throw new RuntimeException(String.format("解析处理器[%s]出错，请检查注解是否正确!!", cls.getName()));
				}

				mapping.put(cmdAnnotation.code(), handler);
			}
		} catch (ClassNotFoundException | IOException | InstantiationException | IllegalAccessException e) {
			log.error("解析处理器出错!", e);
			e.printStackTrace();
		}

		log.info("========Club client 注册Handlers完成========");
	}

	@Override
	public void stop() throws Exception {
		mapping.clear();
	}

	public static void main(String[] args) {
		try {
			ClientHandlerService.getInstance().start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
