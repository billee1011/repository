/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.s2s;

import com.cai.common.constant.MatchCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.handler.IServerHandler;
import com.cai.common.util.PBUtil;
import com.cai.common.util.SessionUtil;
import com.cai.service.C2SSessionService;
import com.google.protobuf.GeneratedMessage;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.Protocol.Response;
import protobuf.clazz.match.MatchClientHeaderRsp.MatchClientResponse;
import protobuf.clazz.match.MatchClientRsp.MatchGameStartResponse;
import protobuf.clazz.match.MatchRsp.MatchS2SRequest;
import protobuf.clazz.match.MatchRsp.MatchServerStartProto;

@IServerCmd(code = S2SCmd.MATCH_SERVER, desc = "")
public class MatchServerProxyHandler extends IServerHandler<MatchS2SRequest> {

	@Override
	public void execute(MatchS2SRequest resp, S2SSession session) throws Exception {
		
		switch (resp.getCmd()) {
		case S2S_MATCH_START:
			matchStart(resp.getMatchStart(),session);
			break;
		default:
			break;
		}
	}
	
	private void matchStart(MatchServerStartProto start, S2SSession session){
		MatchGameStartResponse.Builder b = MatchGameStartResponse.newBuilder();
		b.setId(start.getId());
		b.setMatchId(start.getMatchId());
		Response.Builder rsp = PBUtil.toS2CCommonRsp(S2CCmd.MATCH, getMatchResponse(MatchCmd.S2C_MATCH_START, b));
		start.getAccountIdsList().forEach((accountId)->{
			C2SSession client = C2SSessionService.getInstance().getSession(accountId);
			if(client != null){
				SessionUtil.setLogicSvrId(client, start.getLogicIndex(), 0);
				client.send(rsp);
			}
		});
	}
	
	public static MatchClientResponse.Builder getMatchResponse(int cmd , GeneratedMessage.Builder<?> builder){
		MatchClientResponse.Builder b = MatchClientResponse.newBuilder();
		b.setCmd(cmd);
		b.setData(builder.build().toByteString());
		return b;
	}
}
