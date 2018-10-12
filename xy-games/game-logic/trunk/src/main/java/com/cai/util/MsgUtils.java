package com.cai.util;

import com.cai.common.constant.S2SCmd;
import com.cai.common.define.ESysMsgType;
import com.cai.common.util.PBUtil;
import com.cai.domain.Session;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.MsgAllResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.s2s.ClubServerProto.ClubToClientRsp;

public class MsgUtils {

	public static void sendMsg(Session session, long accountId, String msg) {
		session.send(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLIENT_RSP, getMsgToCLubResponse(accountId, msg)));
	}

	public static ClubToClientRsp.Builder getClubServerResponse(long account_id) {
		ClubToClientRsp.Builder b = ClubToClientRsp.newBuilder();
		b.setClientSessionId(account_id);
		return b;
	}
	
	/**
	 * 普通提示信息
	 * 
	 * @param account_id
	 * 
	 * @param type
	 * @param msg
	 * @return
	 */
	public static ClubToClientRsp.Builder getMsgToCLubResponse(long account_id, String msg) {

		return getClubServerResponse(account_id).setRsp(getMsgAllResponse(msg, ESysMsgType.NONE));
	}

	public static Response.Builder getMsgAllResponse(String msg, ESysMsgType type) {
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.MSG);
		MsgAllResponse.Builder msgBuilder = MsgAllResponse.newBuilder();
		msgBuilder.setType(type.getId());
		msgBuilder.setMsg(msg);
		responseBuilder.setExtension(Protocol.msgAllResponse, msgBuilder.build());
		return responseBuilder;
	}
}
