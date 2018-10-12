package com.cai.game.sdh.handler;

import com.cai.common.domain.Player;
import com.cai.game.sdh.SDHTable;

import protobuf.clazz.Protocol.RoomRequest;

public abstract class SDHHandler<T extends SDHTable> {

	public abstract void exe(T table);

	// 准备
	public boolean handler_player_ready(T table, int seat_index) {
		return table.handler_player_ready(seat_index, false);
	}

	public boolean handler_player_be_in_room(T table, int seat_index) {
		return true;
	}

	public boolean handler_release_room(T table, Player player, int opr_code) {
		return table.handler_release_room(player, opr_code);
	}

	public boolean handler_audio_chat(T table, Player player, com.google.protobuf.ByteString chat, int l, float audio_len) {
		return false;
	}

	public boolean handler_requst_audio_chat(int room_id, long account_id, RoomRequest room_rq) {
		return true;
	}

	public boolean handler_requst_emjoy_chat(int room_id, long account_id, RoomRequest room_rq) {
		return true;
	}
}
