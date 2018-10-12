package com.cai.game.wsk.gzhbzp;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.Player;

import protobuf.clazz.Protocol.RoomRequest;

public abstract class AbstractHandler_GZHBZP {
	public abstract void exe(Table_GZHBZP table);

	protected boolean handler_check_auto_behaviour(Table_GZHBZP table, int seat_index, int card_data) {
		return false;
	}

	public boolean handler_be_set_trustee(Table_GZHBZP table, int seat_index) {
		handler_check_auto_behaviour(table, seat_index, GameConstants.INVALID_VALUE);
		return false;
	}

	public boolean handler_player_ready(Table_GZHBZP table, int seat_index) {
		return table.handler_player_ready(seat_index, false);
	}

	public boolean handler_player_be_in_room(Table_GZHBZP table, int seat_index) {
		return true;
	}

	public boolean handler_player_out_card(Table_GZHBZP table, int seat_index, int card) {
		return false;
	}

	public boolean handler_operate_card(Table_GZHBZP table, int seat_index, int operate_code, int operate_card) {
		return false;
	}

	public boolean handler_release_room(Table_GZHBZP table, Player player, int opr_code) {
		return table.handler_release_room(player, opr_code);
	}

	public boolean handler_audio_chat(Table_GZHBZP table, Player player, com.google.protobuf.ByteString chat, int l, float audio_len) {
		return false;
	}

	public boolean handler_requst_audio_chat(int room_id, long account_id, RoomRequest room_rq) {
		return true;
	}

	public boolean handler_requst_emjoy_chat(int room_id, long account_id, RoomRequest room_rq) {
		return true;
	}
}
