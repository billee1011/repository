/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.handler;

import java.util.List;

import com.cai.domain.Session;
import com.cai.util.MessageResponse;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.AccountPropertyRequest;
import protobuf.clazz.Protocol.AccountPropertyResponse;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.SyPropertyResponse;

/**
 * 通用属性获取
 * @author WalkerGeek
 */
@ICmd(code = RequestType.PROPERTY_VALUE, exName = "accountPropertyRequest")
public class PropertyHandler extends IClientHandler<AccountPropertyRequest> {

	
	@Override
	protected void execute(AccountPropertyRequest message, Request topRequest, Session session) throws Exception {
		// WalkerGeek Auto-generated method stub
		int game_id = message.getGameId();
		List<AccountPropertyResponse> outList  = MessageResponse.getSysAccountPropertyResponseList(game_id);
		SyPropertyResponse.Builder builder = SyPropertyResponse.newBuilder();
		builder.addAllAccountProperty(outList);
		builder.setGameId(game_id);
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.SY_PROPERTY);
		responseBuilder.setExtension(Protocol.syPropertyResponse, builder.build());
		session.send(responseBuilder.build());
		
	}

}
