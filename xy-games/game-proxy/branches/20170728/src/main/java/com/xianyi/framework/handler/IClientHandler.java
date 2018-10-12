/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.xianyi.framework.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.EMsgType;
import com.cai.domain.Session;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessage;

import protobuf.clazz.Protocol.Request;

/**
 * 处理客户端请求
 * 
 * @author wu_hc
 */
public abstract class IClientHandler<T extends GeneratedMessage> {

	/**
	 * 
	 */
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * T 字段的描述[###自动绑定，不要手动修改####]
	 */
	private FieldDescriptor fieldDescriptor;

	/**
	 * 消息去向[###自动绑定，不要手动修改####]
	 */
	private EMsgType msgType;

	/**
	 * request为顶层包,message为具体包
	 * 
	 * @param message
	 * @param request
	 * @param session
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public void doExecute(Object message, Request request, Session session) throws Exception {
		this.execute((T) message, request, session);
	}

	/**
	 * 
	 * @param message
	 * @param request
	 * @param session
	 * @throws Exception
	 */
	protected abstract void execute(T message, Request topRequest, Session session) throws Exception;

	/**
	 * 返回描述
	 * 
	 * @return
	 */
	public FieldDescriptor getFieldDescriptor() {
		return fieldDescriptor;
	}

	public void setFieldDescriptor(FieldDescriptor fieldDescriptor) {
		this.fieldDescriptor = fieldDescriptor;
	}

	public EMsgType getMsgType() {
		return msgType;
	}

	public void setMsgType(EMsgType msgType) {
		this.msgType = msgType;
	}

}
