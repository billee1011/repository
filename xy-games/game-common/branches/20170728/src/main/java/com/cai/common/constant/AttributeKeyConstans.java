/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.common.constant;

import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import io.netty.util.AttributeKey;

/**
 * 
 *
 * @author wu_hc date: 2017年7月24日 下午6:00:39 <br/>
 */
public interface AttributeKeyConstans {

	/**
	 * 代理服网络接收器
	 */
	AttributeKey<C2SSession> PROXY_ACEEPTOR = AttributeKey.valueOf("PROXY_ACEEPTOR");

	/**
	 * 代理服和逻辑服
	 */
	AttributeKey<S2SSession> PROXY_CONNECTOR = AttributeKey.valueOf("PROXY_CONNECTOR");
}
