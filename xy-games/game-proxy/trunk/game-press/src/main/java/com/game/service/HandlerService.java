package com.game.service;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.EServerType;
import com.cai.common.util.LoadPackageClasses;
import com.game.common.IClientHandler;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Parser;
import com.xianyi.framework.core.transport.ICmd;

import javolution.util.FastMap;
import protobuf.clazz.Protocol;

/**
 * 处理器管理中心
 * 
 * @author wu_hc
 */
public final class HandlerService {

	/**
	 * 日志
	 */
	private static Logger log = LoggerFactory.getLogger(HandlerService.class);

	private static HandlerService INstance = new HandlerService();

	private final Map<Integer, IClientHandler<?>> mapping = new FastMap<>();

	public static HandlerService getInstance() {
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

	public void start() {
		LoadPackageClasses loader = new LoadPackageClasses(new String[] { "com.game.network.handler" }, ICmd.class);
		try {
			ExtensionRegistry registry = ExtensionRegistry.newInstance();
			Protocol.registerAllExtensions(registry);

			Set<Class<?>> handlerClassz = loader.getClassSet();
			for (final Class<?> cls : handlerClassz) {
				ICmd cmdAnnotation = cls.getAnnotation(ICmd.class);
				if (null == cmdAnnotation)
					throw new RuntimeException(String.format("解析处理器[%s]出错，请检查注解是否正确!!", cls.getName()));

				IClientHandler<?> clientHandler = (IClientHandler<?>) cls.newInstance();

				String exName = cmdAnnotation.exName();

				FieldDescriptor fieldDescriptor = null;
				if (cmdAnnotation.msgType() == EServerType.PROXY && StringUtils.isNotEmpty(exName)) {
					fieldDescriptor = registry.findExtensionByName(exName).descriptor;
				}
				clientHandler.setMsgType(cmdAnnotation.msgType());
				clientHandler.setFieldDescriptor(fieldDescriptor);
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
		} catch (ClassNotFoundException | IOException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			log.error("解析处理器出错!", e);
			e.printStackTrace();
		}

		log.info("========Proxy注册Handlers完成========");
	}

	public static void main(String[] args) {
		HandlerService.getInstance().start();
	}
}
