/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.s2s;

import com.cai.common.domain.Account;
import com.cai.common.handler.IServerHandler;
import com.cai.common.util.SessionUtil;
import com.cai.dictionary.SysParamDict;
import com.cai.service.C2SSessionService;
import com.cai.service.ClientServiceImpl;
import com.cai.util.MessageResponse;
import com.cai.util.RoomUtil;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.LogicRoomRequest;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.s2s.ClubServerProto.ClubCreateRoom;

/**
 * 
 * 
 *
 * @author wu_hc date: 2017年11月27日 下午7:44:02 <br/>
 */
//@IServerCmd(code = S2SCmd.CLUB_ROOM_OBSERVER, desc = "俱乐部房间围观")
public class ClubObserverRoomHandler extends IServerHandler<ClubCreateRoom> {

	@Override
	public void execute(ClubCreateRoom resp, S2SSession session) throws Exception {
		C2SSession client = C2SSessionService.getInstance().getSession(resp.getClientSessionId());
		if (client == null) {
			return;
		}

		final Account account = client.getAccount();
		if (null == account) {
			return;
		}
		account.getWorkerLoop().runInLoop(new Runnable() {
			@Override
			public void run() {

				if (RoomUtil.getRoomId(account.getAccount_id()) > 0) {
					return;
				}

				boolean isObserverGame = SysParamDict.getInstance().isObserverGameTypeIndex(resp.getClubRule().getGameTypeIndex());
				if (!isObserverGame) { // 没有围观，直接反馈错误码
					client.send(MessageResponse.getMsgAllResponse("房间已满，不能加入!!").build());
					return;
				}

				Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, client);
				LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();

				logicRoomRequestBuilder.setType(56);
				RoomRequest.Builder b = RoomRequest.newBuilder();
				b.setRoomId(resp.getRoomId());
				b.setClubId(resp.getClubId());
				b.setRuleId(resp.getClubRule().getId());
				logicRoomRequestBuilder.setRoomRequest(b.build());
				logicRoomRequestBuilder.setRoomId(resp.getRoomId());
				logicRoomRequestBuilder.setAccountId(client.getAccount().getAccount_id());
				logicRoomRequestBuilder.setLogicRoomAccountItemRequest(MessageResponse.getLogicRoomAccountItemRequest(client));
				requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());

				SessionUtil.setLogicSvrId(client, resp.getLogicId(), resp.getRoomId());
				boolean flag = ClientServiceImpl.getInstance().sendMsg(resp.getLogicId(), requestBuider.build());
				if (!flag) {
					client.send(MessageResponse.getMsgAllResponse("逻辑服务器链接失败").build());
					return;
				}
			}
		});
	}
}
