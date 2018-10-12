package com.lingyu.common.http;

import java.lang.reflect.Method;

import com.esotericsoftware.reflectasm.MethodAccess;

public class MethodWrapper {
	private Method method;
	private Object instance;
	private Class<?> paramClazz;// 参数类型
	private final MethodAccess access;
	private final int methodIndex;// 方法索引

	public MethodWrapper(Method method, Object instance,MethodAccess access,int methodIndex) {
		this.method = method;
		this.instance = instance;
		this.paramClazz = method.getParameterTypes()[0];
		this.access = access;
		this.methodIndex = methodIndex;
	}

	public Object invoke(){
		return access.invoke(instance, methodIndex);
	}
	
	public Object invoke(Object... args){
		return access.invoke(instance, methodIndex,args);
	}
	
	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public Object getInstance() {
		return instance;
	}

	public void setInstance(Object instance) {
		this.instance = instance;
	}

	public Class<?> getParamClazz() {
		return paramClazz;
	}

	public void setParamClazz(Class<?> paramClazz) {
		this.paramClazz = paramClazz;
	}

}
