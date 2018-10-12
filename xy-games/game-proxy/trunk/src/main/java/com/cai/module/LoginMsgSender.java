/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.module;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.AttributeKeyConstans;
import com.cai.common.util.SessionUtil;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.LoginResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;

/**
 *
 * @author wu_hc
 */
public final class LoginMsgSender {

	private static final Logger logger = LoggerFactory.getLogger(LoginMsgSender.class);

	/**
	 * 登陆返回
	 * 
	 * @param session
	 * @param type
	 * @param errorCode
	 */
	public static void sendLoginFailedRsp(C2SSession session, int type, int errorCode) {

		LoginResponse.Builder loginResponse = LoginResponse.newBuilder();
		loginResponse.setType(type);
		loginResponse.setErrorCode(errorCode);

		Integer loginType = SessionUtil.getAttr(session, AttributeKeyConstans.LOGIN_TYPE);
		if (null != loginType) {
			loginResponse.setLoginType(loginType.intValue());
		}

		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.LOING);
		responseBuilder.setExtension(Protocol.loginResponse, loginResponse.build());
		session.send(responseBuilder.build());

		logger.info("{}:登陆失败type:{},errorCode:{}", loginType, type, errorCode);
	}

}
