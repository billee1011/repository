package com.cai.game.hjk.handler;

import com.cai.common.domain.Player;
import com.cai.game.hjk.HJKTable;

import protobuf.clazz.Protocol.RoomRequest;

public abstract class HJKHandler {

	public abstract void exe(HJKTable table);

	// 准备
	public boolean handler_player_ready(HJKTable table, int seat_index) {
		return table.handler_player_ready(seat_index);
	}

	public boolean handler_player_be_in_room(HJKTable table, int seat_index) {
		return true;
	}

	public boolean handler_player_out_card(HJKTable table, int seat_index, int card) {
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
	public boolean handler_operate_card(HJKTable table, int seat_index, int operate_code, int operate_card,int luoCode) {
		return false;
	}
	/***
	 * //用户操作
	 * 
	 * @param seat_index操作用户
	 * @param operate_code
	 * @param operated_index 被操作的用户
	 */
	public boolean handler_operate_button(HJKTable table,int seat_index, int operate_code) {
		return false;
	}
	public boolean handler_call_banker(HJKTable table, int seat_index,int call_banker) {
		return false;
	}
	public boolean handler_add_jetton(HJKTable table, int seat_index, int jetton) {
		return false;
	}
	public boolean handler_open_cards(HJKTable table, int seat_index,boolean open_flag) {
		return false;
	}
	public boolean handler_Dispatch_cards(HJKTable table, int seat_index,boolean yao_button,boolean pass_button) {
		return false;
	}

	public boolean handler_release_room(HJKTable table, Player player, int opr_code) {
		return table.handler_release_room(player, opr_code);
	}

	public boolean handler_audio_chat(HJKTable table, Player player, com.google.protobuf.ByteString chat, int l,
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
