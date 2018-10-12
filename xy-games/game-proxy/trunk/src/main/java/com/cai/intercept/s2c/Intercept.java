/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.intercept.s2c;

import com.xianyi.framework.core.transport.netty.session.C2SSession;

import protobuf.clazz.Protocol.Response;

/**
 * s->s->c 拦截器
 *
 * @author wu_hc date: 2017年9月8日 下午12:30:16 <br/>
 */
public interface Intercept {
	/**
	 * 
	 * @param response
	 */
	boolean intercept(Response response, C2SSession session);
}
