package com.cai.net.util;

import com.google.protobuf.Descriptors.FieldDescriptor;

public final class RequestClassHandlerBinding<hT>
{
	/**
	 * 扩展名描述
	 */
	private FieldDescriptor fieldDescriptor;
	
	/**
	 * 处理类
	 */
	private Class<? extends hT> handlerClass;
	

	public RequestClassHandlerBinding()
	{
	}
	
	public RequestClassHandlerBinding(FieldDescriptor fieldDescriptor, Class<? extends hT> _handlerClass)
	{
		this.fieldDescriptor = fieldDescriptor;
		this.handlerClass = _handlerClass;
	}


	public Class<? extends hT> getHandlerClass()
	{
		return handlerClass;
	}

	public void setHandlerClass(Class<? extends hT> handlerClass)
	{
		this.handlerClass = handlerClass;
	}
	
	
	
	public FieldDescriptor getFieldDescriptor() {
		return fieldDescriptor;
	}

	public void setFieldDescriptor(FieldDescriptor fieldDescriptor) {
		this.fieldDescriptor = fieldDescriptor;
	}

	public String toString()
	{
		return "--------"+this.handlerClass.getName();
	}
}
