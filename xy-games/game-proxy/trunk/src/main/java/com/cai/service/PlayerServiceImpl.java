package com.cai.service;

import java.util.SortedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.domain.Event;
import com.cai.common.util.SessionUtil;
import com.cai.core.MonitorEvent;
import com.cai.core.SystemConfig;
import com.cai.util.MessageResponse;
import com.cai.util.RoomUtil;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.server.AbstractService;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.LogicRoomRequest;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Request.RequestType;

public class PlayerServiceImpl extends AbstractService {

	private static final Logger logger = LoggerFactory.getLogger(PlayerServiceImpl.class);

	private static PlayerServiceImpl instance = null;

	private PlayerServiceImpl() {
	}

	public static PlayerServiceImpl getInstance() {
		if (null == instance) {
			instance = new PlayerServiceImpl();
		}
		return instance;
	}

	/**
	 * 发送消息
	 * 
	 * @param session
	 * @param response
	 */
	public void sendAccountMsg(C2SSession session, Response response) {

		if (SystemConfig.gameDebug == 1) {
			System.out.println("转发服Encoder2<=========" + response.toByteArray().length + "b\n" + response);
		}

		session.send(response);

		// // 日志
		// Long account_id = null;
		// Account account = session.getAccount();
		// if (false && account != null) {
		// account_id = session.getAccount().getAccount_id();
		// String ip = session.getClientIP();
		// StringBuffer buf = new StringBuffer();
		// buf.append(response.toByteArray().length).append("B").append("|sessionId:").append(session.getAccountID())
		// .append("|accountId:" +
		// account.getAccount_id()).append("|").append("转发服消息|")
		// .append(response.toString());
		// long v1 = response.getResponseType().getNumber();
		// MongoDBServiceImpl.getInstance().player_log(account_id,
		// ELogType.response, buf.toString(), v1, null, ip);
		// }
		//

	}

	@Override
	protected void startService() {
		// TODO Auto-generated method stub

	}

	@Override
	public MonitorEvent montior() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sessionCreate(C2SSession session) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sessionFree(C2SSession session) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dbUpdate(int _userID) {
		// TODO Auto-generated method stub

	}

	/**
	 * 通知 逻辑服更新个人信息
	 * 
	 * @param session
	 * @param type
	 * @param addGold
	 * @param addMoney
	 */
	public void notifyLogicToUpdateAccountInfo(C2SSession session, int type, int addGold, int addMoney) {
		int roomId = RoomUtil.getRoomId(session.getAccountID());
		if (roomId != 0) {
			Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, session);
			LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();
			logicRoomRequestBuilder.setType(type);
			logicRoomRequestBuilder.setRoomId(roomId);
			logicRoomRequestBuilder.setAddGold(addGold);
			logicRoomRequestBuilder.setAddMoney(addMoney);
			logicRoomRequestBuilder.setLogicRoomAccountItemRequest(MessageResponse.getLogicRoomAccountItemRequest(session));
			requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());
			int logicSvrId = SessionUtil.getLogicSvrId(session, roomId);

			// 玩家在逻辑服，不需要处理
			if (logicSvrId <= 0) {
				return;
			}
			boolean flag = ClientServiceImpl.getInstance().sendMsg(logicSvrId, requestBuider.build());
			if (!flag) {
				logger.error("玩家{}操作类型{} 通知逻辑服失败!!!", session.getAccountID(), type);
				return;
			}
		}
	}

}
