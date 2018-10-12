package com.lingyu.common.http;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.lingyu.common.config.IConfig;
import com.lingyu.common.core.ServiceException;
import com.lingyu.msg.http.HttpMsg;

public class RpcBrokerService {
	private static final Logger logger = LogManager.getLogger(RpcBrokerService.class);
	private Map<String, MethodWrapper> cachedMethod = new HashMap<String, MethodWrapper>();
	private IConfig config;

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
	public void initialize(IConfig config, IHttpProcessor processor) throws ServiceException {
		this.config = config;
		logger.debug("注册Http监听方法开始");
		cachedMethod.putAll(registerMethods(processor));
		logger.debug("注册Http监听方法结束");
	}

	private Map<String, MethodWrapper> registerMethods(Object instance) throws ServiceException {
		Map<String, MethodWrapper> cachedMethod = new HashMap<String, MethodWrapper>();
		MethodAccess access = MethodAccess.get(instance.getClass());
		Method[] methods = instance.getClass().getDeclaredMethods();
		for (Method method : methods) {
			try {
				if(Modifier.isPublic(method.getModifiers())){
					String methodName = method.getName();
					int methodIndex = access.getIndex(methodName, method.getParameterTypes());
					String lowerCaseMethodName = methodName.toLowerCase();
					cachedMethod.put(lowerCaseMethodName, new MethodWrapper(method, instance, access, methodIndex));
				}
			} catch (Exception e) {
				logger.error(e.getMessage(),e);
			}
		}
		return cachedMethod;
	}

	/**
	 * 
	 * @param functionName
	 * @param parameter null，表示是无参数的调用
	 * @return
	 */
	public HttpMsg call(MethodWrapper wrapper, Object args) throws ServiceException {
		HttpMsg returnValue = null;
		try {
			returnValue = (HttpMsg) wrapper.invoke(args);// 有参数调用
			returnValue.setWorldId(config.getWorldId());   
			returnValue.setWorldName(config.getWorldName());
		} catch (Exception e) {
			throw new ServiceException(e);
		}
		return returnValue;
	}
	/*public HttpMsg call(MethodWrapper wrapper, Object parameter) throws ServiceException {
		HttpMsg returnValue = null;
		try {
			//
			// 有参数调用
			returnValue = (HttpMsg) wrapper.getMethod().invoke(wrapper.getInstance(), parameter);
			returnValue.setWorldId(config.getWorldId());   
			returnValue.setWorldName(config.getWorldName());
		} catch (Exception e) {
			throw new ServiceException(e);
		}

		return returnValue;
	}*/

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
