package com.cai.game.hh.handler.xpphz;

import com.cai.common.constant.MsgConstants;
import com.cai.game.hh.handler.HHHandlerDispatchCard;

import protobuf.clazz.Protocol.RoomResponse;

/**
 * 
 * @author admin
 *
 */
public class PHZHandlerChongGuan_XP extends HHHandlerDispatchCard<HHTable_XP> {

	@Override
	public void exe(HHTable_XP table) {

	}

	@Override
	public boolean handler_player_be_in_room(HHTable_XP table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		table.load_room_info_data(roomResponse); // 加载房间的玩法 状态信息
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		roomResponse.setGameStatus(table._game_status);
		roomResponse.setPaoMax(1);
		roomResponse.setPaoMax(3);
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			roomResponse.addActions(table.chong[i]);
		}

		table.send_response_to_player(seat_index, roomResponse);
		
		if (table.chong[seat_index] == 0) {
			roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
			roomResponse.setPaoMax(1);
			roomResponse.setPaoMax(3);
			table.send_response_to_player(seat_index, roomResponse);
		}

		return true;
	}
}
