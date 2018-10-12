/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.service;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import com.cai.common.domain.Event;
import com.cai.common.rmi.IRmi;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.util.LoadPackageClasses;
import com.cai.core.MonitorEvent;
import com.cai.domain.Session;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import javolution.util.FastMap;

/**
 * 处理器管理中心
 *
 * @author wu_hc date: 2017年10月17日 下午4:09:29 <br/>
 */
public final class RMIHandlerServiceImp extends AbstractService {

	private static RMIHandlerServiceImp INstance = new RMIHandlerServiceImp();

	private final Map<Integer, IRMIHandler<?, ?>> mapping = new FastMap<>();

	public static RMIHandlerServiceImp getInstance() {
		return INstance;
	}

	/**
	 * 获得处理器
	 * 
	 * @param cmd
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T, R> IRMIHandler<T, R> getHandler(int cmd) {
		return (IRMIHandler<T, R>) mapping.get(cmd);
	}

	@Override
	protected void startService() {
		LoadPackageClasses loader = new LoadPackageClasses(new String[] { "com.cai.rmi.handler" }, IRmi.class);
		try {

			Set<Class<?>> handlerClassz = loader.getClassSet();
			for (final Class<?> cls : handlerClassz) {
				IRmi cmdAnnotation = cls.getAnnotation(IRmi.class);
				if (null == cmdAnnotation)
					throw new RuntimeException(String.format("解析处理器[%s]出错，请检查注解是否正确!!", cls.getName()));

				IRMIHandler<?, ?> handler = (IRMIHandler<?, ?>) cls.newInstance();

				mapping.put(cmdAnnotation.cmd(), handler);
			}
		} catch (Exception e) {
			logger.error("解析处理器出错!", e);
			e.printStackTrace();
		}

		logger.info("========LOGIC RMI 注册Handlers完成========");
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

	@Override
	public void sessionCreate(Session session) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sessionFree(Session session) {
		// TODO Auto-generated method stub

	}

}
