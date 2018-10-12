package com.cai.game.czbg.handler;

import com.cai.common.domain.Player;
import com.cai.game.czbg.CZBGTable;

import protobuf.clazz.Protocol.RoomRequest;

public abstract class CZBGHandler {

	public abstract void exe(CZBGTable table);

	public boolean handler_player_be_in_room(CZBGTable table, int seat_index) {
		return true;
	}

	/**
	 * 围观者
	 * 
	 * @param table
	 * @param player
	 * @return
	 */
	public boolean handler_observer_be_in_room(CZBGTable table, Player player) {
		return true;
	}

	public boolean handler_player_out_card(CZBGTable table, int seat_index, int card) {
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
	public boolean handler_operate_card(CZBGTable table, int seat_index, int operate_code, int operate_card, int luoCode) {
		return false;
	}

	public boolean handler_call_banker(CZBGTable table, int seat_index, int call_banker) {
		return false;
	}

	public boolean handler_add_jetton(CZBGTable table, int seat_index, int jetton) {
		return false;
	}

	public boolean handler_open_cards(CZBGTable table, int seat_index, boolean open_flag) {
		return false;
	}

	public boolean handler_release_room(CZBGTable table, Player player, int opr_code) {
		return table.handler_release_room(player, opr_code);
	}

	public boolean handler_audio_chat(CZBGTable table, Player player, com.google.protobuf.ByteString chat, int l, float audio_len) {
		return false;
	}

	public boolean handler_requst_audio_chat(int room_id, long account_id, RoomRequest room_rq) {
		return true;
	}

	public boolean handler_requst_emjoy_chat(int room_id, long account_id, RoomRequest room_rq) {

		return true;
	}

}
