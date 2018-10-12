/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.service;

import java.util.Map;
import java.util.Set;

import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.util.LoadPackageClasses;
import com.cai.common.util.XYGameException;
import com.cai.constant.ServiceOrder;
import com.xianyi.framework.core.service.AbstractService;
import com.xianyi.framework.core.service.IService;

import javolution.util.FastMap;

/**
 * 处理器管理中心
 *
 * @author wu_hc date: 2017年10月17日 下午4:09:29 <br/>
 */
@IService(order = ServiceOrder.RMI_HANDLER)
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
	public <R, T> IRMIHandler<R, T> getHandler(int cmd) {
		return (IRMIHandler<R, T>) mapping.get(cmd);
	}

	@Override
	public void start() throws Exception {
		LoadPackageClasses loader = new LoadPackageClasses(new String[] { "com.cai.rmi.handler" }, IRmi.class);
		try {

			Set<Class<?>> handlerClassz = loader.getClassSet();
			for (final Class<?> cls : handlerClassz) {
				IRmi cmdAnnotation = cls.getAnnotation(IRmi.class);
				if (null == cmdAnnotation)
					throw new RuntimeException(String.format("解析处理器[%s]出错，请检查注解是否正确!!", cls.getName()));

				IRMIHandler<?, ?> handler = (IRMIHandler<?, ?>) cls.newInstance();

				Object oldValue = mapping.put(cmdAnnotation.cmd(), handler);
				if (null != oldValue) {
					logger.error("rmi cmd[ {} ] 重复定义，请检查!!!!!", cmdAnnotation.cmd());
					throw new XYGameException(String.format("rmi cmd[ %d ] 重复定义，请检查!!!!!", cmdAnnotation.cmd()));
				}
			}
		} catch (Exception e) {
			logger.error("解析处理器出错!", e);
			e.printStackTrace();
		}

		logger.info("========LOGIC RMI 注册Handlers完成========");
	}

	@Override
	public void stop() throws Exception {

	}

}
