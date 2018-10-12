/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.intercept.c2s;

import com.xianyi.framework.core.transport.netty.session.C2SSession;

import protobuf.clazz.Protocol.CommonProto;
import protobuf.clazz.Protocol.Request;

/**
 * 
 *
 * @author wu_hc date: 2017年10月19日 上午11:01:00 <br/>
 */
public interface ReqIntercept {

	/**
	 * 
	 * @param commProto
	 * @param topRequest
	 * @param session
	 * @return 是否拦截
	 */
	boolean intercept(CommonProto commProto, Request topRequest, C2SSession session);
}
