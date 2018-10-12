/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.service;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.SortedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.Event;
import com.cai.common.util.LoadPackageClasses;
import com.cai.core.MonitorEvent;
import com.cai.domain.Session;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Parser;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.handler.IClientExHandler;

import javolution.util.FastMap;

/**
 * 
 * 
 *
 * @author wu_hc date: 2017年10月19日 下午12:07:44 <br/>
 */
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
	public void startService() {
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
		} catch (Exception e) {
			log.error("解析处理器出错!", e);
			e.printStackTrace();
		}

		log.info("========Club client 注册Handlers完成========");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.service.AbstractService#montior()
	 */
	@Override
	public MonitorEvent montior() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.service.AbstractService#onEvent(com.cai.common.domain.Event)
	 */
	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.cai.service.AbstractService#sessionCreate(com.cai.domain.Session)
	 */
	@Override
	public void sessionCreate(Session session) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.service.AbstractService#sessionFree(com.cai.domain.Session)
	 */
	@Override
	public void sessionFree(Session session) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.service.AbstractService#dbUpdate(int)
	 */
	@Override
	public void dbUpdate(int _userID) {
		// TODO Auto-generated method stub

	}
}
