package com.cai.game.tdz.handler;

import com.cai.common.domain.Player;
import com.cai.game.tdz.TDZTable;

public abstract class TDZHandler<T extends TDZTable> {

	public abstract void exe(T table);

	public boolean handler_player_be_in_room(T table, int seat_index) {
		return true;
	}

	public boolean handler_player_out_card(T table, int seat_index, int card) {
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
	public boolean handler_call_banker(T table, int seat_index,int call_banker) {
		return false;	
	}
	public boolean handler_add_jetton(T table, int seat_index, int jetton) {
		return false;
	}
	public boolean handler_open_cards(T table, int seat_index,boolean open_flag) {
		return false;
	}

	public boolean handler_release_room(T table, Player player, int opr_code) {
		return table.handler_release_room(player, opr_code);
	}

}
