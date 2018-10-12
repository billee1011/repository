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

import com.cai.common.domain.Event;
import com.cai.common.util.LoadPackageClasses;
import com.cai.core.MonitorEvent;
import com.cai.domain.Session;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IServerHandler;
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
				mapping.put(cmdAnnotation.code(), handler);
			}
		} catch (ClassNotFoundException | IOException | InstantiationException | IllegalAccessException e) {
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

	@Override
	public void sessionCreate(Session session) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sessionFree(Session session) {
		// TODO Auto-generated method stub
		
	}

}
