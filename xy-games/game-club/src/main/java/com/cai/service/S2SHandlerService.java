/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.service;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.util.LoadPackageClasses;
import com.cai.common.util.XYGameException;
import com.cai.constant.ServiceOrder;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Parser;
import com.xianyi.framework.core.service.AbstractService;
import com.xianyi.framework.core.service.IService;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.handler.IServerHandler;

import javolution.util.FastMap;

/**
 * 处理器管理中心
 *
 * @author wu_hc
 */
@IService(order = ServiceOrder.S2S_HANDLE)
public final class S2SHandlerService extends AbstractService {

	/**
	 * 日志
	 */
	private static Logger log = LoggerFactory.getLogger(S2SHandlerService.class);

	private static S2SHandlerService INstance = new S2SHandlerService();

	private final FastMap<Integer, IServerHandler<?>> mapping = new FastMap<>();

	public static S2SHandlerService getInstance() {
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
	public void start() throws Exception {
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

				Object oldValue = mapping.put(cmdAnnotation.code(), handler);
				if (null != oldValue) {
					logger.error("handler cmd[ {} ] 重复定义，请检查!!!!!", cmdAnnotation.code());
					throw new XYGameException(String.format("handler cmd[ %d ] 重复定义，请检查!!!!!", cmdAnnotation.code()));
				}
			}

		} catch (ClassNotFoundException | IOException | InstantiationException | IllegalAccessException e) {
			log.error("解析处理器出错!", e);
			e.printStackTrace();
		}

		log.info("========Club S2S 注册Handlers完成========");
	}

	@Override
	public void stop() throws Exception {
		mapping.clear();
	}

}
