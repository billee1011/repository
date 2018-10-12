/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.s2s;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.S2SCmd;
import com.cai.common.define.ERoomStatus;
import com.cai.common.define.ESysMsgType;
import com.cai.common.domain.Player;
import com.cai.common.domain.Room;
import com.cai.common.handler.IServerHandler;
import com.cai.game.AbstractRoom;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.ClubMsgSender;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import io.netty.channel.Channel;
import protobuf.clazz.ClubMsgProto.ClubTablePlayerProto;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.s2s.ClubServerProto.ClubRoomStatusProto;

/**
 * 
 *
 * @author wu_hc date: 2017年9月1日 下午12:31:20 <br/>
 */
@IServerCmd(code = S2SCmd.C_2_LOGIC_ROOM_STATUS, desc = "俱乐部->逻辑服同步的房间状态")
public class Club2LRoomStatusHandler extends IServerHandler<ClubRoomStatusProto> {

	@Override
	public void execute(ClubRoomStatusProto resp, S2SSession session) throws Exception {
		ERoomStatus status = ERoomStatus.of(resp.getType());
		if (ERoomStatus.NONE == status) {
			return;
		}

		int roomId = resp.getRoomId();
		if (roomId > 0) {
			final AbstractRoom room = PlayerServiceImpl.getInstance().getRoomMap().get(roomId);
			if (null != room) {
				if (ERoomStatus.DEL == status) { // 删除俱乐部
					room.clubInfo.clubDel = true;
					notifyUpdateRule(room, resp.getRuleId(), 4);

				} else if (ERoomStatus.UPDATE_RULE == status) { // 修改玩法
					room.clubInfo.updateRule = true;
					// notifyUpdateRule(room, resp.getRuleId(), 3);

				} else if (ERoomStatus.ADD_RULE == status) { // 添加包间
					// notifyUpdateRule(room, resp.getRuleId(), 1);

				} else if (ERoomStatus.DELETE_RULE == status) { // 删除包间
					room.clubInfo.updateRule = true;
					// notifyUpdateRule(room, resp.getRuleId(), 2);

				} else if (ERoomStatus.KICK_PLAYER == status) { // 踢出玩家
					kickPlayerOut(resp.getPlayer(), room);

				} else if (ERoomStatus.L_2_CLUB_ROOM_SYNC == status || ERoomStatus.TABLE_REFRESH == status) { // 要求同步房间数据
					ClubMsgSender.syncAppointClubRoomStatusTOClubServer(resp.getRoomId(), resp.getClubId());
					// ClubMsgSender.roomPlayerStatusUpdate(ERoomStatus.TABLE_REFRESH,
					// room);
				}
			}
		}
	}

	/**
	 * 
	 * @param playerPB
	 * @param room
	 */
	private final void kickPlayerOut(final ClubTablePlayerProto playerPB, AbstractRoom room) {
		room.runInRoomLoop(() -> {
			Player player = room.get_player(playerPB.getAccountId());
			if (null != player) {
				Channel channel = player.getChannel();
				player.setChannel(null);
				try {
					room.getRoomLock().lock();
					boolean r = room.handler_release_room(player, GameConstants.Release_Room_Type_QUIT);
					if (r) {
						player.setChannel(channel);
						room.send_error_notify(player, ESysMsgType.INCLUDE_ERROR.getId(), "您已被管理人员请出牌桌");

						RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
						quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
						quit_roomResponse.setRoomId(room.getRoom_id());
						Response.Builder responseBuilder = Response.newBuilder();
						responseBuilder.setResponseType(ResponseType.ROOM);
						responseBuilder.setExtension(Protocol.roomResponse, quit_roomResponse.build());
						PlayerServiceImpl.getInstance().send(player, responseBuilder.build());

						player.setChannel(null);
						player.setRoom_id(0);
						player.set_seat_index(GameConstants.INVALID_SEAT);
						logger.warn("俱乐部[{}] 房间[{}] , 踢人[{}]操作:", room.clubInfo, room.getRoom_id(), player.getAccount_id());
					}
				} finally {
					room.getRoomLock().unlock();
				}
			}
		});
	}

	/**
	 * 
	 * @param room
	 * @param category
	 */
	private final void notifyUpdateRule(final Room room, int ruleId, int category) {
		for (int i = 0; i < room.getPlayerCount(); i++) {
			Player player = room.get_players()[i];
			if (null != player) {
				ClubMsgSender.notifyUpdateRule(player, room.clubInfo.clubId, ruleId, category);
			}
		}
	}
}
