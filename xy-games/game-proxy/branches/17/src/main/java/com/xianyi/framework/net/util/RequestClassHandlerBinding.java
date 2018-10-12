package com.xianyi.framework.net.util;

import com.cai.common.define.EMsgType;
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
	
	/**
	 * 消息类型
	 */
	private EMsgType eMsgType;

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
	
	
	
	public EMsgType geteMsgType() {
		return eMsgType;
	}

	public void seteMsgType(EMsgType eMsgType) {
		this.eMsgType = eMsgType;
	}

	public FieldDescriptor getFieldDescriptor() {
		return fieldDescriptor;
	}

	public void setFieldDescriptor(FieldDescriptor fieldDescriptor) {
		this.fieldDescriptor = fieldDescriptor;
	}

	public String toString()
	{
		return "-----"+this.handlerClass.getName();
	}
}
