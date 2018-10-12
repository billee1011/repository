package com.cai.fls.handler;

import com.cai.common.domain.Player;
import com.cai.fls.FLSTable;

import protobuf.clazz.Protocol.RoomRequest;

public abstract class FLSHandler {

	public abstract void exe(FLSTable table);

	// 准备
	public boolean handler_player_ready(FLSTable table, int seat_index) {
		return table.handler_player_ready(seat_index);
	}

	public boolean handler_player_be_in_room(FLSTable table, int seat_index) {
		return true;
	}

	public boolean handler_player_out_card(FLSTable table, int seat_index, int card) {
		return false;
	}

	/***
	 * //用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	public boolean handler_operate_card(FLSTable table, int seat_index, int operate_code, int operate_card) {
		return false;
	}

	public boolean handler_release_room(FLSTable table, Player player, int opr_code) {
		return table.handler_release_room(player, opr_code);
	}

	public boolean handler_audio_chat(FLSTable table, Player player, com.google.protobuf.ByteString chat, int l,
			float audio_len) {
		return false;
	}

	public boolean handler_requst_audio_chat(int room_id, long account_id, RoomRequest room_rq) {
		return true;
	}

	public boolean handler_requst_emjoy_chat(int room_id, long account_id, RoomRequest room_rq) {

		return true;
	}
}
