/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.service;

import java.io.IOException;
import java.util.Set;
import java.util.SortedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.EMsgType;
import com.cai.common.domain.Event;
import com.cai.common.util.LoadPackageClasses;
import com.cai.core.MonitorEvent;
import com.cai.domain.Session;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.ExtensionRegistry;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.handler.IClientHandler;
import com.xianyi.framework.server.AbstractService;

import javolution.util.FastMap;
import protobuf.clazz.Protocol;

/**
 * 处理器管理中心
 * 
 * @author wu_hc
 */
public final class HandlerServiceImp extends AbstractService {

	/**
	 * 日志
	 */
	private static Logger log = LoggerFactory.getLogger(HandlerServiceImp.class);

	private static HandlerServiceImp INstance = new HandlerServiceImp();

	private final FastMap<Integer, IClientHandler<?>> mapping = new FastMap<>();

	public static HandlerServiceImp getInstance() {
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

	@SuppressWarnings("unchecked")
	@Override
	protected void startService() {
		LoadPackageClasses loader = new LoadPackageClasses(new String[] { "com.cai.handler" }, ICmd.class);
		try {
			ExtensionRegistry registry = ExtensionRegistry.newInstance();
			Protocol.registerAllExtensions(registry);

			Set<Class<?>> handlerClassz = loader.getClassSet();
			for (final Class<?> cls : handlerClassz) {
				ICmd cmdAnnotation = cls.getAnnotation(ICmd.class);
				if (null == cmdAnnotation)
					throw new RuntimeException(String.format("解析处理器[%s]出错，请检查注解是否正确!!", cls.getName()));

				IClientHandler<?> clientHandler = (IClientHandler<?>) cls.newInstance();

				FieldDescriptor fieldDescriptor = null;
				if (cmdAnnotation.msgType() != EMsgType.LOGIC_MSG) {
					fieldDescriptor = registry.findExtensionByName(cmdAnnotation.exName()).descriptor;
				}
				clientHandler.setMsgType(cmdAnnotation.msgType());
				clientHandler.setFieldDescriptor(fieldDescriptor);
				mapping.put(cmdAnnotation.code(), clientHandler);
			}
		} catch (ClassNotFoundException | IOException | InstantiationException | IllegalAccessException e) {
			log.error("解析处理器出错!", e);
			e.printStackTrace();
		}

		log.info("========Proxy注册Handlers完成========");
	}

	@Override
	public MonitorEvent montior() {
		return null;
	}

	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {

	}

	public void sessionCreate(Session session) {
	}

	@Override
	public void sessionFree(Session session) {
	}

	@Override
	public void dbUpdate(int _userID) {

	}

	public static void main(String[] args) {
		HandlerServiceImp.getInstance().startService();
	}

}
