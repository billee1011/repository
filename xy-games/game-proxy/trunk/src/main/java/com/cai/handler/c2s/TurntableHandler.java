/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.handler.c2s;

import java.util.List;
import java.util.stream.Collectors;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.constant.TurntableCmd;
import com.cai.common.domain.Account;
import com.cai.common.domain.TurntableLogModel;
import com.cai.common.util.PBUtil;
import com.cai.core.Global;
import com.cai.dictionary.TurntableDict;
import com.cai.service.ClientServiceImpl;
import com.cai.service.MongoDBServiceImpl;
import com.cai.util.MessageResponse;
import com.google.protobuf.GeneratedMessage;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.activity.ActivityTurntableProto.TurntableLogsResponse;
import protobuf.clazz.activity.ActivityTurntableProto.TurntableReq;
import protobuf.clazz.activity.ActivityTurntableProto.TurntableRsp;
import protobuf.clazz.activity.ActivityTurntableServerProto.TurntableClientReq;

/**
 * 
 */
@ICmd(code = C2SCmd.TURNTABLE)
public class TurntableHandler extends IClientHandler<TurntableReq> {

	@Override
	protected void execute(TurntableReq request, Request topRequest, C2SSession session) throws Exception {

		Account account = session.getAccount();
		if (account == null)
			return;
		
		switch (request.getCmd()) {
		case TurntableCmd.C2S_GET_CONFIG:
			Response response = TurntableDict.getInstance().get(request.getId());
			if(response == null){
				return;
			}
			session.send(response);
			return;
		case TurntableCmd.C2S_GET_SELF_LOG:
			Global.getGameDispatchService().execute(new Runnable() {
				@Override
				public void run() {
					List<TurntableLogModel> log = MongoDBServiceImpl.getInstance().getTurntableLogs(account.getAccount_id(), request.getId());
					
					TurntableLogsResponse.Builder b = TurntableLogsResponse.newBuilder();
					b.addAllLogs(log.stream().map(TurntableLogModel::encode).collect(Collectors.toList()));
					sendTurntable(TurntableCmd.S2C_GET_SELF_LOG, request.getId(), b.build(), session);
				}
			});
			return;
		case TurntableCmd.C2S_GET_LOG:
			Global.getGameDispatchService().execute(new Runnable() {
				@Override
				public void run() {
					List<TurntableLogModel> log = MongoDBServiceImpl.getInstance().getTurntableLogs(0, request.getId());
					
					TurntableLogsResponse.Builder b = TurntableLogsResponse.newBuilder();
					b.addAllLogs(log.stream().map(TurntableLogModel::encode).collect(Collectors.toList()));
					sendTurntable(TurntableCmd.S2C_GET_LOG, request.getId(), b.build(), session);
				}
			});
			return;
		}
		TurntableClientReq.Builder b = TurntableClientReq.newBuilder();
		b.setAccountId(account.getAccount_id());
		b.setNickname(account.getNickName());
		b.setReq(request);

		boolean result = ClientServiceImpl.getInstance().sendToFoundation(1, PBUtil.toS2SRequet(S2SCmd.TURNTABLE, b.build()).build());
		if (!result) {
			//session.send(MessageResponse.getMsgAllResponse("抽奖系统正在例行维护中，请稍后再试！").build());
		}
	}
	
	private void sendTurntable(int cmd, int activityId, GeneratedMessage msg, C2SSession session){
		TurntableRsp.Builder b = TurntableRsp.newBuilder();
		b.setCmd(cmd);
		b.setId(activityId);
		b.setProtos(msg.toByteString());
		session.send(PBUtil.toS2CCommonRsp(S2CCmd.TURNTABLE, b).build());
	}
	

}
