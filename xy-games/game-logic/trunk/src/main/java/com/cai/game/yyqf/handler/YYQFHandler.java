package com.cai.game.yyqf.handler;


import com.cai.common.domain.Player;
import com.cai.game.yyqf.YYQFTable;

import protobuf.clazz.Protocol.RoomRequest;

public abstract class YYQFHandler {

	public abstract void exe(YYQFTable table);

	// 准备
	public boolean handler_player_ready(YYQFTable table, int seat_index) {
		return table.handler_player_ready(seat_index,false);
	}

	public boolean handler_player_be_in_room(YYQFTable table, int seat_index) {
		return true;
	}

	public boolean handler_player_out_card(YYQFTable table, int seat_index, int card) {
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
	public boolean handler_operate_card(YYQFTable table, int seat_index, int operate_code, int operate_card,int luoCode) {
		return false;
	}

	public boolean handler_release_room(YYQFTable table, Player player, int opr_code) {
		return table.handler_release_room(player, opr_code);
	}

	public boolean handler_audio_chat(YYQFTable table, Player player, com.google.protobuf.ByteString chat, int l,
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
