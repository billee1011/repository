/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.game.network.handler;

import com.cai.common.constant.MsgConstants;
import com.game.Player;
import com.game.Room;
import com.game.common.IClientHandler;
import com.game.common.util.SessionUtil;
import com.game.manager.RoomMananger;
import com.game.network.tasks.CreateRoomTask;
import com.game.network.tasks.JoinRoomTask;
import com.game.network.tasks.ReadyRoomTask;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomResponse;

/**
 * 
 *
 * @author wu_hc date: 2017年10月12日 下午12:41:46 <br/>
 */
@ICmd(code = ResponseType.ROOM_VALUE, exName = "roomResponse")
public final class RoomRspHandler extends IClientHandler<RoomResponse> {
	@Override
	protected void execute(RoomResponse rsp, Response response, S2SSession session) throws Exception {

		final Player player = SessionUtil.getPlayer(session);
		logger.info("player:{},roomId:{},roomType:{}", player, rsp, rsp.getType());
		int roomId = rsp.getRoomId();
		final RoomInfo roomInfo = rsp.getRoomInfo();

		if (null != roomInfo) {
			roomId = 0 >= roomId ? roomInfo.getRoomId() : roomId;
		}
		switch (rsp.getType()) {
		case MsgConstants.RESPONSE_CREATE_RROXY_ROOM_SUCCESS: { // 创建成功

			Room room = new Room();
			room.setRoomId(roomId);
			room.setGameRuleIndex(roomInfo.getGameRuleIndex());
			room.setCreate_player_id(roomInfo.getCreatePlayerId());
			room.setGame_round(roomInfo.getGameRound());
			room.setRoomIfo(roomInfo);

			RoomMananger.getInstance().add(room);

			joinGame(player, roomId);
		}
			break;
		case MsgConstants.RESPONSE_ENTER_ROOM: { // 进入房间
			Room room = new Room();
			room.setPlayers(rsp.getPlayersList());
			room.setRoomId(roomId);
			room.setGameRuleIndex(roomInfo.getGameRuleIndex());
			room.setCreate_player_id(roomInfo.getCreatePlayerId());
			room.setGame_round(roomInfo.getGameRound());
			room.setRoomIfo(roomInfo);

			RoomMananger.getInstance().add(room);

			final int roomId__ = roomId;
			// 还未开始
			if (roomInfo.getCurRound() == 0) {
				schdule(() -> {
					Runnable r = new ReadyRoomTask(player, roomId__);
					player.getWorker().runInLoop(r);
				}, 200L);

			}

			player.sendTrustee(roomId);
		}
			break;
		case 3: { // 没有房间，如果有空余房间，则加入，没有则创建新房间
			Room room = RoomMananger.getInstance().getUnFullRoom();
			if (null == room) {
				CreateRoomTask task = new CreateRoomTask(player);
				task.run();
			} else {
				joinGame(player, room.getRoomId());
			}
		}
			break;
		case MsgConstants.RESPONSE_GAME_END: { // 游戏结束

			// 大局结束
			if (roomInfo.getCurRound() == roomInfo.getGameRound()) {
				RoomMananger.getInstance().remove(roomId);
				// 是否继续创建房间?????GAME-TODO
				CreateRoomTask task = new CreateRoomTask(player);
				task.run();
			} else {
				Runnable r = new ReadyRoomTask(player, roomId);
				r.run();
			}
		}
			break;
		case MsgConstants.RESPONSE_REFRESH_PLAYERS: {// 刷新玩家
			final Room room = RoomMananger.getInstance().get(roomId);
			if (null != room) {
				room.setGame_status(rsp.getGameStatus());
				room.setRoomIfo(roomInfo);
				room.setPlayers(rsp.getPlayersList());
			}
		}
			break;
		case MsgConstants.RESPONSE_GAME_START: { // 游戏开始
			logger.info("RESPONSE_GAME_START");
		}
			break;
		case MsgConstants.RESPONSE_ROOM_OWNER_START: { // 房租开始游戏
			logger.info("RESPONSE_ROOM_OWNER_START");
		}
			break;
		case MsgConstants.RESPONSE_PLAYER_READY: { // 万家准备-跑胡子

			final Room room = RoomMananger.getInstance().get(roomId);
			if (null != room) {
				RoomPlayerResponse playerRsp = room.getPlayer(rsp.getOperatePlayer());
				if (null != playerRsp && playerRsp.getAccountId() == player.getAccountId()) {
					player.setReady(true);
				}
			}
		}
			break;
		case MsgConstants.RESPONSE_MY_ROOMS: {
			logger.info("{}", rsp.toString());
		}
			break;
		case MsgConstants.RESPONSE_SEND_CARD: { // 发牌->抢装

		}
			break;
		default:
			break;
		}
	}

	/**
	 * 
	 * @param session
	 */
	private void joinGame(Player player, int roomId) {
		JoinRoomTask task = new JoinRoomTask(player, roomId);
		task.run();
	}
}
