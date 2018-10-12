/**
 * 
 */
package com.cai.game;

import java.util.HashSet;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.cai.common.constant.MsgConstants;
import com.cai.common.define.SysGameTypeEnum;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.Player;
import com.cai.common.domain.Room;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.LocationUtil;
import com.cai.dictionary.SysParamDict;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.MsgAllResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RoomResponse;

/**
 * @author xwy
 *
 */
public class RoomUtil {

	private static Logger logger = Logger.getLogger(RoomUtil.class);

	/**
	 * 记录机器人开房
	 * 
	 * @param room
	 */
	public static void realkou_dou(Room room) {
		if (room.is_sys()) {
			return;
		}

		int[] roundGoldArray = SysGameTypeEnum.getGoldIndexByTypeIndex(room._game_type_index);
		if (roundGoldArray == null) {
			return;
		}

		SysParamModel findParam = null;
		for (int index : roundGoldArray) {
			SysParamModel tempParam = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(room.getGame_id()).get(index);// 扑克类30
			if (tempParam == null) {
				continue;
			}
			if (tempParam.getVal1() == room._game_round) {
				findParam = tempParam;
				break;
			}
		}
		if (findParam == null) {
			return;
		}
		int check_gold = findParam.getVal2();
		boolean create_result = true;
		// 注意游戏ID不一样
		if (check_gold == 0) {
			create_result = false;
		} else {
			// 是否免费的
			SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(room.getGame_id()).get(room.game_index);

			if (sysParamModel != null && sysParamModel.getVal2() == 1) {
				// 收费
				StringBuilder buf = new StringBuilder();
				buf.append("真实扣豆 创建房间:" + room.getRoom_id()).append("game_id:" + room.getGame_id())
						.append(",game_type_index:" + room._game_type_index).append(",game_round:" + room._game_round);
				PlayerServiceImpl.getInstance().subRealGold(room.getRoom_owner_account_id(), check_gold, false, buf.toString());

				if (StringUtils.isNotEmpty(room.groupID)) {
					PlayerServiceImpl.getInstance().subRobotGold(room.getRoom_owner_account_id(), check_gold, false, buf.toString(), room.groupID,
							room.groupName, room._game_type_index, room._game_round, room._game_rule_index, room.getRoom_id());
				}

			}
		}
	}

	public static boolean kou_dou(Room room) {

		int game_type_index = room._game_type_index;
		// 收费索引
		room.game_index = SysGameTypeEnum.getGameGoldTypeIndex(game_type_index);
		room.setGame_id(SysGameTypeEnum.getGameIDByTypeIndex(game_type_index));
		if (room.is_sys()) {
			return true;
		}
		int check_gold = 0;
		boolean create_result = true;

		int[] roundGoldArray = SysGameTypeEnum.getGoldIndexByTypeIndex(game_type_index);
		if (roundGoldArray == null) {
			return false;
		}

		SysParamModel findParam = null;
		for (int index : roundGoldArray) {
			SysParamModel tempParam = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(room.getGame_id()).get(index);// 扑克类30
			if (tempParam == null) {
				continue;
			}
			if (tempParam.getVal1() == room._game_round) {
				findParam = tempParam;
				break;
			}
		}
		if (findParam == null) {
			return false;
		}
		check_gold = findParam.getVal2();

		// 注意游戏ID不一样
		if (check_gold == 0) {
			create_result = false;
		} else {
			// 是否免费的
			SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(room.getGame_id()).get(room.game_index);

			if (sysParamModel != null && sysParamModel.getVal2() == 1) {
				// 收费
				StringBuilder buf = new StringBuilder();
				buf.append("创建房间:" + room.getRoom_id()).append("game_id:" + room.getGame_id()).append(",game_type_index:" + game_type_index)
						.append(",game_round:" + room._game_round);
				AddGoldResultModel result = PlayerServiceImpl.getInstance().subGold(room.getRoom_owner_account_id(), check_gold, false,
						buf.toString());
				if (result.isSuccess() == false) {
					create_result = false;
				} else {
					// 扣豆成功
					room.cost_dou = check_gold;
				}
			}

		}
		if (create_result == false) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);
			Player player = null;
			for (int i = 0; i < room.get_players().length; i++) {
				send_response_to_player(room, i, roomResponse);

				player = room.get_players()[i];
				if (player == null)
					continue;
				if (i == 0) {
					send_error_notify(room, i, 1, "闲逸豆不足,游戏解散");
				} else {
					send_error_notify(room, i, 1, "创建人闲逸豆不足,游戏解散");
				}

			}

			// 删除房间
			PlayerServiceImpl.getInstance().delRoomId(room.getRoom_id());
			return false;
		}

		return true;
	}

	public static boolean send_response_to_player(Room room, int seat_index, RoomResponse.Builder roomResponse) {

		roomResponse.setRoomId(room.getRoom_id());// 日志用的
		if (room.get_players()[seat_index] == null) {
			return false;
		}
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.ROOM);
		responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());
		PlayerServiceImpl.getInstance().send(room.get_players()[seat_index], responseBuilder.build());
		return true;
	}

	public static boolean send_error_notify(Room room, int seat_index, int type, String msg) {
		MsgAllResponse.Builder e = MsgAllResponse.newBuilder();
		e.setType(type);
		e.setMsg(msg);
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.MSG);
		responseBuilder.setExtension(Protocol.msgAllResponse, e.build());
		PlayerServiceImpl.getInstance().send(room.get_players()[seat_index], responseBuilder.build());

		return false;
	}

	public static boolean send_error_notify(Player player, int type, String msg, int time) {
		MsgAllResponse.Builder e = MsgAllResponse.newBuilder();
		e.setType(type);
		e.setMsg(msg);
		e.setTime(time);
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.MSG);
		responseBuilder.setExtension(Protocol.msgAllResponse, e.build());
		PlayerServiceImpl.getInstance().send(player, responseBuilder.build());

		return false;
	}

	public static boolean send_response_to_other(Room room, int seat_index, RoomResponse.Builder roomResponse) {

		roomResponse.setRoomId(room.getRoom_id());// 日志用的
		Player player = null;
		for (int i = 0; i < room.get_players().length; i++) {
			player = room.get_players()[i];
			if (player == null)
				continue;
			if (i == seat_index)
				continue;
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.ROOM);
			responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());
			// this.log_error("nickname =
			// "+this.get_players()[seat_index].getNick_name() + "nickname =
			// "+this.get_players()[i].getNick_name()
			// +roomResponse.getAudioLen()+ "time=" +
			// System.currentTimeMillis());
			PlayerServiceImpl.getInstance().send(room.get_players()[i], responseBuilder.build());
		}
		room.observers().sendAll(roomResponse);
		return true;
	}

	/**
	 * 只给围观者发
	 * 
	 * @param roomResponse
	 * @return
	 */
	public static boolean send_response_to_observer(Room room, RoomResponse.Builder roomResponse) {
		room.observers().sendAll(roomResponse);
		return true;
	}

	public static boolean handler_audio_chat(Room room, Player player, com.google.protobuf.ByteString chat, int l, float audio_len) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_AUDIO_CHAT);
		roomResponse.setOperatePlayer(player.get_seat_index());
		roomResponse.setAudioChat(chat);
		roomResponse.setAudioSize(l);
		roomResponse.setAudioLen(audio_len);
		send_response_to_other(room, player.get_seat_index(), roomResponse);
		return true;
	}

	public static boolean send_response_to_room(Room room, RoomResponse.Builder roomResponse) {

		roomResponse.setRoomId(room.getRoom_id());// 日志用的
		Player player = null;
		for (int i = 0; i < room.get_players().length; i++) {
			player = room.get_players()[i];
			if (player == null)
				continue;
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.ROOM);
			responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());

			PlayerServiceImpl.getInstance().send(room.get_players()[i], responseBuilder.build());
		}
		room.observers().sendAll(roomResponse);
		return true;
	}

	public static boolean handler_emjoy_chat(Room room, Player player, int id) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EMJOY_CHAT);
		roomResponse.setOperatePlayer(player.get_seat_index());
		roomResponse.setEmjoyId(id);
		send_response_to_room(room, roomResponse);

		return true;
	}

	public static void getLocationTip(Room room) {
		try {
			String tipMsg = "";

			StringBuffer buf = new StringBuffer();
			HashSet<String> names = new HashSet<String>();
			for (int i = 0; i < room.get_players().length; i++) {
				if (room.get_players()[i] == null)
					continue;
				Player player = room.get_players()[i];
				if (player.locationInfor == null || player.locationInfor.getPosX() == 0 || player.locationInfor.getPosX() == 0) {
					continue;
				}
				for (int j = i; j < room.get_players().length; j++) {
					if (room.get_players()[j] == null)
						continue;
					Player targetPlayer = room.get_players()[j];
					if (targetPlayer.locationInfor == null || targetPlayer.locationInfor.getPosX() == 0
							|| targetPlayer.locationInfor.getPosX() == 0) {
						continue;
					}
					if (targetPlayer.getAccount_id() == player.getAccount_id())
						continue;
					double distance = LocationUtil.LantitudeLongitudeDist(player.locationInfor.getPosX(), player.locationInfor.getPosY(),
							targetPlayer.locationInfor.getPosX(), targetPlayer.locationInfor.getPosY());

					int tipDistance = 1000;
					SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(room.getGame_id()).get(5005);
					if (sysParamModel != null && sysParamModel.getVal5() > 0) {
						tipDistance = sysParamModel.getVal5();
					}
					if (distance < tipDistance) {
						names.add(player.getNick_name());
						names.add(targetPlayer.getNick_name());
					}
				}
			}
			for (String name : names) {
				buf.append(name).append(" ");
			}
			if (names.size() > 1) {
				tipMsg = buf.append("距离过近").toString();
				for (int i = 0; i < room.get_players().length; i++) {
					Player player = room.get_players()[i];
					if (player == null)
						continue;
					send_error_notify(player, 2, tipMsg, 5);
				}
			}

		} catch (Exception e) {
			logger.error("定位提示异常", e);
		}
	}

}
