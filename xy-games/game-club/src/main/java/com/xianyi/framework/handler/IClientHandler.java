/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.xianyi.framework.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.util.PBUtil;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Parser;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import protobuf.clazz.Protocol.S2SCommonProto;

/**
 * 处理客户端请求[客户端是相对而言]
 * 
 * @author wu_hc
 */
public abstract class IClientHandler<T extends GeneratedMessage> {

	/**
	 * 
	 */
	protected final Logger logger = LoggerFactory.getLogger(getClass());

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
	public void doExecute(Object message, C2SSession session) throws Exception {
		this.execute((T) message, session);
	}

	/**
	 * 
	 * @param message
	 * @param request
	 * @param session
	 * @throws Exception
	 */
	protected abstract void execute(T req, C2SSession session) throws Exception;

	@SuppressWarnings("unchecked")
	public T toObject(S2SCommonProto commProto) {
		// TODO Auto-generated method stub
		return (T) PBUtil.toObject(commProto.getByte(), parser, GeneratedMessage.class);
	}

	public void setParse(Parser<? extends GeneratedMessage> parser) {
		this.parser = parser;
	}

}
