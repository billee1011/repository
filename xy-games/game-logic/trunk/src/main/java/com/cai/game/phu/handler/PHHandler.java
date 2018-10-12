package com.cai.game.phu.handler;

import com.cai.common.domain.Player;
import com.cai.game.phu.PHTable;

import protobuf.clazz.Protocol.RoomRequest;

public abstract class PHHandler<T extends PHTable> {

	public abstract void exe(T table);

	// 准备
	public boolean handler_player_ready(T table, int seat_index) {
		return table.handler_player_ready(seat_index,false);
	}

	public boolean handler_player_be_in_room(T table, int seat_index) {
		return true;
	}

	public boolean handler_player_out_card(T table, int seat_index, int card) {
		return false;
	}
	public boolean handler_ask_player(T table, int seat_index,boolean  is_ask){
		return false;
	}
	public boolean handler_status_operate_card(T table, int seat_index, int operate_code, int operate_card,int luoCode,int handler_status)
	{
		return false;
	}

	/***
	 * //用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @param luoCode
	 * @return
	 */
	public boolean handler_operate_card(T table, int seat_index, int operate_code, int operate_card,int luoCode) {
		return false;
	}

	public boolean handler_release_room(T table, Player player, int opr_code) {
		return table.handler_release_room(player, opr_code);
	}

	public boolean handler_audio_chat(T table, Player player, com.google.protobuf.ByteString chat, int l,
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
