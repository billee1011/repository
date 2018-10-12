package com.lingyu.admin.facade;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.lingyu.common.core.ServiceException;
import com.lingyu.common.http.MethodWrapper;

public class RpcBrokerService {
	private static final Logger logger = LogManager.getLogger(RpcBrokerService.class);
	private Map<String, MethodWrapper> cachedMethod = new HashMap<String, MethodWrapper>();

	public static RpcBrokerService getInstance() {
		return InstanceHolder.INSTANCE;
	}

	private static class InstanceHolder {
		private static final RpcBrokerService INSTANCE = new RpcBrokerService();
	}

	/**
	 * 
	 * @throws Exception
	 */
	public void initialize(IFacade facade) throws ServiceException {
		logger.debug("注册Http监听方法开始");
		cachedMethod.putAll(registerMethods(facade));
		logger.debug("注册Http监听方法结束");
	}

	private Map<String, MethodWrapper> registerMethods(Object instance) throws ServiceException {
		Map<String, MethodWrapper> cachedMethod = new HashMap<String, MethodWrapper>();
		Method[] methods = instance.getClass().getDeclaredMethods();
		MethodAccess access = MethodAccess.get(instance.getClass());
		for (Method method : methods) {
			String methodName = method.getName();
			String lowerCaseMethodName = methodName.toLowerCase();
			int methodIndex = access.getIndex(method.getName(), method.getParameterTypes());
			cachedMethod.put(lowerCaseMethodName, new MethodWrapper(method, instance,access,methodIndex));

		}
		return cachedMethod;
	}

	/**
	 * 
	 * @param functionName
	 * @param parameter null，表示是无参数的调用
	 * @return
	 */
	public Object call(MethodWrapper wrapper, Object parameter) throws ServiceException {
		Object returnValue = null;
		try {
			//
			// 有参数调用
			returnValue =wrapper.invoke(parameter);

		} catch (Exception e) {
			throw new ServiceException(e);
		}

		return returnValue;
	}

	/**
	 * 
	 * @param functionName
	 * @param parameter
	 * @return
	 */
	public MethodWrapper matcherMethod(String functionName) {
		String lowerCaseFunctionName = functionName == null ? null : functionName.toLowerCase();
		return cachedMethod.get(lowerCaseFunctionName);
	}
}
