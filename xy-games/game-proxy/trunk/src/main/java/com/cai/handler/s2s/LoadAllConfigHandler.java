/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.s2s;

import com.cai.common.constant.MatchCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.handler.IServerHandler;
import com.cai.common.type.SystemType;
import com.cai.common.util.PBUtil;
import com.cai.service.C2SSessionService;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.core.transport.netty.session.S2SSession;
import protobuf.clazz.BaseS2S.SendLoadConfigs;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.match.MatchClientHeaderRsp.MatchClientResponse;
import protobuf.clazz.match.MatchClientRsp.MatchEmpty;
/**
 * 
 */
@IServerCmd(code = S2SCmd.LOAD_ALL_CONFIG, desc = "通知客户端重新加载配置")
public class LoadAllConfigHandler extends IServerHandler<SendLoadConfigs> {

	@Override
	public void execute(SendLoadConfigs resp, S2SSession s2s_session) throws Exception {
		int configType = resp.getConfigType();
		Response response = null;
		switch (configType) {
		case SystemType.LOAD_MATCH:
			// 通知玩家重新加载
			MatchClientResponse.Builder matchResponse = MatchClientResponse.newBuilder();
			matchResponse.setCmd(MatchCmd.S2C_MATCH_CONFIG_RESET);
			matchResponse.setData(MatchEmpty.newBuilder().build().toByteString());
			response = PBUtil.toS2CCommonRsp(S2CCmd.MATCH, matchResponse).build();
			break;
		}
		
		if(response != null){
			final Response sendResp = response;
			if(resp.getIsToAll()){
				C2SSessionService.getInstance().getAllOnlieSession().forEach((session) -> {
					session.send(sendResp);
				});
			}else{
				resp.getAccountIdsList().forEach((id)->{
					C2SSession client = C2SSessionService.getInstance().getSession(id);
					if(client != null){
						client.send(sendResp);
					}
				});
			}
		}
		
	}
}
