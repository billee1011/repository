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
import com.cai.core.SystemConfig;
import com.cai.domain.Session;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Parser;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.handler.IClientHandler;

import javolution.util.FastMap;

/**
 * 
 * 处理器管理中心
 *
 * @author wu_hc date: 2017年8月30日 下午6:21:27 <br/>
 */
public final class C2SHandlerServiceImpl extends AbstractService {

	/**
	 * 日志
	 */
	private final static Logger logger = LoggerFactory.getLogger(C2SHandlerServiceImpl.class);

	private static C2SHandlerServiceImpl INstance = new C2SHandlerServiceImpl();

	private final FastMap<Integer, IClientHandler<?>> mapping = new FastMap<>();

	public static C2SHandlerServiceImpl getInstance() {
		return INstance;
	}

	/**
	 * 获得处理器
	 * 
	 * @param cmd
	 * @return
	 */
	public IClientHandler<?> getHandler(int cmd) {
		return mapping.get(cmd);
	}

	@Override
	protected void startService() {
		LoadPackageClasses loader = new LoadPackageClasses(new String[] { "com.cai.handler.c2s" }, IServerCmd.class);
		try {
			Set<Class<?>> handlerClassz = loader.getClassSet();
			for (final Class<?> cls : handlerClassz) {
				IServerCmd cmdAnnotation = cls.getAnnotation(IServerCmd.class);
				if (null == cmdAnnotation)
					throw new RuntimeException(String.format("解析处理器[%s]出错，请检查注解是否正确!!", cls.getName()));
				if (SystemConfig.gameDebug == 0 && cmdAnnotation.code() <= 0) {
					return;
				}
				IClientHandler<?> clientHandler = (IClientHandler<?>) cls.newInstance();

				Class<?> clazz = cls;
				while (clazz != Object.class) {
					Type t = clazz.getGenericSuperclass();
					if (t instanceof ParameterizedType) {
						Type[] args = ((ParameterizedType) t).getActualTypeArguments();
						if (args[0] instanceof Class) {
							clazz = (Class<?>) args[0];
							@SuppressWarnings("unchecked")
							Parser<? extends GeneratedMessage> parser = (Parser<? extends GeneratedMessage>) ((GeneratedMessage) clazz
									.getMethod("getDefaultInstance").invoke(null)).getParserForType();
							clientHandler.setParse(parser);
							break;
						}
					}
					clazz = clazz.getSuperclass();
				}
				if (clazz == Object.class) {
					throw new RuntimeException(String.format("解析处理器[%s]出错，请检查注解是否正确!!", cls.getName()));
				}

				mapping.put(cmdAnnotation.code(), clientHandler);
			}
		} catch (Exception e) {
			logger.error("解析处理器出错!", e);
			e.printStackTrace();
		}

		logger.info("========logic server 注册c2s Handlers完成========");
	}

	@Override
	public MonitorEvent montior() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sessionCreate(Session session) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sessionFree(Session session) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dbUpdate(int _userID) {
		// TODO Auto-generated method stub
		
	}

}
