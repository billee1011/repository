/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.s2s;

import com.cai.common.domain.Account;
import com.cai.common.handler.IServerHandler;
import com.cai.common.util.SessionUtil;
import com.cai.dictionary.SysParamDict;
import com.cai.module.ClubMsgSender;
import com.cai.service.C2SSessionService;
import com.cai.service.ClientServiceImpl;
import com.cai.util.MessageResponse;
import com.cai.util.RoomUtil;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.LogicRoomAccountItemRequest;
import protobuf.clazz.Protocol.LogicRoomRequest;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.s2s.ClubServerProto.ClubCreateRoom;

/**
 * 
 */
//@IServerCmd(code = S2SCmd.CREATE_ENTER_ROOM_RSP, desc = "俱乐部开房")
public class ClubEnterRoomHandler extends IServerHandler<ClubCreateRoom> {

	@Override
	public void execute(ClubCreateRoom resp, S2SSession session) throws Exception {
		C2SSession client = C2SSessionService.getInstance().getSession(resp.getClientSessionId());
		if (client == null) {
			return;
		}

		/* client.getAccount().getWorkerLoop() */ClubMsgSender.worker(resp.getClubId()).runInLoop(new Runnable() {
			@Override
			public void run() {
				Account account = client.getAccount();
				if (account == null) {
					return;
				}

				if (RoomUtil.getRoomId(account.getAccount_id()) > 0) {
					logger.error("玩家[{}]尝试进入俱乐部房间失败,msg[{}]!", account, resp);
					return;
				}

				boolean isObserverGame = SysParamDict.getInstance().isObserverGameTypeIndex(resp.getClubRule().getGameTypeIndex());

				Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, client);
				LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();

				logicRoomRequestBuilder.setType(isObserverGame ? 56 : 2);
				RoomRequest.Builder b = RoomRequest.newBuilder();
				b.setRoomId(resp.getRoomId());
				b.setClubId(resp.getClubId());
				b.setRuleId(resp.getClubRule().getId());
				logicRoomRequestBuilder.setRoomRequest(b.build());
				logicRoomRequestBuilder.setAccountId(client.getAccount().getAccount_id());
				logicRoomRequestBuilder.setRoomId(resp.getRoomId());
				LogicRoomAccountItemRequest.Builder accountItemB = MessageResponse.getLogicRoomAccountItemRequest(client);
				accountItemB.setJoinId(resp.getJoinId());
				accountItemB.setClubOwner(resp.getClubOwnerId());
				logicRoomRequestBuilder.setLogicRoomAccountItemRequest(accountItemB);

				LogicRoomAccountItemRequest.Builder accountItemA = MessageResponse.getLogicRoomAccountItemRequest(client);
				accountItemA.setJoinId(resp.getJoinId());
				logicRoomRequestBuilder.setLogicRoomAccountItemRequest(MessageResponse.getLogicRoomAccountItemRequest(client));
				requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());

				SessionUtil.setLogicSvrId(client, resp.getLogicId(), resp.getRoomId());
				boolean flag = ClientServiceImpl.getInstance().sendMsg(resp.getLogicId(), requestBuider.build());
				if (!flag) {
					client.send(MessageResponse.getMsgAllResponse("服务器例行维护，请稍候再试！").build());
					return;
				}
			}
		});
	}
}
