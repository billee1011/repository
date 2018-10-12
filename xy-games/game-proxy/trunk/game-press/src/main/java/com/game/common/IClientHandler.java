/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.game.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.EServerType;
import com.cai.common.util.PBUtil;
import com.game.common.util.PressGlobalExecutor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Parser;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import io.netty.util.internal.ThreadLocalRandom;
import protobuf.clazz.Protocol.CommonProto;
import protobuf.clazz.Protocol.Response;

/**
 * 处理客户端请求
 * 
 *
 * @author wu_hc date: 2017年10月12日 上午10:11:20 <br/>
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
	private EServerType msgType;

	private Parser<? extends GeneratedMessage> parser;

	/**
	 * request为顶层包,message为具体包
	 * 
	 * @param message
	 * @param request
	 * @param session
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public void doExecute(Object message, Response response, S2SSession session) throws Exception {
		this.execute((T) message, response, session);
	}

	/**
	 * request为顶层包,message为具体包
	 * 
	 * @param message
	 * @param request
	 * @param session
	 * @throws Exception
	 */
	public void doExecute(CommonProto commProto, Response response, S2SSession session) throws Exception {
		this.execute(toObject(commProto), response, session);
	}

	@SuppressWarnings("unchecked")
	public T toObject(CommonProto commProto) {
		return (T) PBUtil.toObject(commProto.getByte(), parser, GeneratedMessage.class);
	}

	/**
	 * 
	 * @param message
	 * @param request
	 * @param session
	 * @throws Exception
	 */
	protected abstract void execute(T rsp, Response response, S2SSession session) throws Exception;

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

	public void setParse(Parser<? extends GeneratedMessage> parser) {
		this.parser = parser;
	}

	public EServerType getMsgType() {
		return msgType;
	}

	public void setMsgType(EServerType msgType) {
		this.msgType = msgType;
	}

	/**
	 * 
	 * @param task
	 * @param delay
	 */
	protected final void schdule(Runnable task, long delay) {
		PressGlobalExecutor.schedule(task, delay);
	}

	/**
	 * 
	 * @param task
	 * @param delay
	 */
	protected final void schduleRandom(Runnable task) {
		PressGlobalExecutor.schedule(task, ThreadLocalRandom.current().nextInt(100, 1000));
	}
}
