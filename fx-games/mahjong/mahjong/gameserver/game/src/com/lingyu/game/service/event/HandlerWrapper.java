package com.lingyu.game.service.event;

import java.lang.reflect.Method;

import com.esotericsoftware.reflectasm.MethodAccess;

public class HandlerWrapper {
	private Object handler;
	private byte group;
	private Method method;
	private String module;
	private MethodAccess access;
	private int methodIndex;// 方法索引

	
	public <T> void invoke(T event){
		access.invoke(handler, methodIndex, event);// 有参数调用
	}
	
	public Object getHandler() {
		return handler;
	}

	public void setHandler(Object handler) {
		this.handler = handler;
	}

	public byte getGroup() {
		return group;
	}

	public void setGroup(byte group) {
		this.group = group;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
	}

	public MethodAccess getAccess() {
		return access;
	}

	public void setAccess(MethodAccess access) {
		this.access = access;
	}

	public int getMethodIndex() {
		return methodIndex;
	}

	public void setMethodIndex(int methodIndex) {
		this.methodIndex = methodIndex;
	}
}
