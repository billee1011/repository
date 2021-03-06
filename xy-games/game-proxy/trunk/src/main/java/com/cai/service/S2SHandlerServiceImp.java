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
import com.cai.common.handler.IServerHandler;
import com.cai.common.util.LoadPackageClasses;
import com.cai.core.MonitorEvent;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Parser;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.server.AbstractService;

import javolution.util.FastMap;

/**
 * 处理器管理中心
 * 
 * @author wu_hc
 */
public final class S2SHandlerServiceImp extends AbstractService {

	/**
	 * 日志
	 */
	private static Logger log = LoggerFactory.getLogger(S2SHandlerServiceImp.class);

	private static S2SHandlerServiceImp INstance = new S2SHandlerServiceImp();

	private final FastMap<Integer, IServerHandler<?>> mapping = new FastMap<>();

	public static S2SHandlerServiceImp getInstance() {
		return INstance;
	}

	/**
	 * 获得处理器
	 * 
	 * @param cmd
	 * @return
	 */
	public IServerHandler<?> getHandler(int cmd) {
		return mapping.get(cmd);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void startService() {
		LoadPackageClasses loader = new LoadPackageClasses(new String[] { "com.cai.handler.s2s" }, IServerCmd.class);
		try {

			Set<Class<?>> handlerClassz = loader.getClassSet();
			for (final Class<?> cls : handlerClassz) {
				IServerCmd cmdAnnotation = cls.getAnnotation(IServerCmd.class);
				if (null == cmdAnnotation)
					throw new RuntimeException(String.format("解析处理器[%s]出错，请检查注解是否正确!!", cls.getName()));

				IServerHandler<?> handler = (IServerHandler<?>) cls.newInstance();
				
				Class<?> clazz = cls;
				while (clazz != Object.class) {
					Type t = clazz.getGenericSuperclass();
					if (t instanceof ParameterizedType) {
						Type[] args = ((ParameterizedType) t).getActualTypeArguments();
						if (args[0] instanceof Class) {
							clazz = (Class<?>) args[0];
							Parser<? extends GeneratedMessage> parser = (Parser<? extends GeneratedMessage>) ((GeneratedMessage) clazz.getMethod("getDefaultInstance").invoke(null)).getParserForType();
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

		log.info("========Proxy S2S 注册Handlers完成========");
	}

	@Override
	public MonitorEvent montior() {
		return null;
	}

	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {

	}

	public void sessionCreate(C2SSession session) {
	}

	@Override
	public void dbUpdate(int _userID) {

	}

	public static void main(String[] args) {
		S2SHandlerServiceImp.getInstance().startService();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.xianyi.framework.server.AbstractService#sessionFree(com.xianyi.
	 * framework.core.transport.netty.session.C2SSession)
	 */
	@Override
	public void sessionFree(C2SSession session) {
		// TODO Auto-generated method stub

	}

}
