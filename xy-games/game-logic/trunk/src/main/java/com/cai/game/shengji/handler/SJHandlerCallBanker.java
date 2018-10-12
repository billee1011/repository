package com.cai.game.shengji.handler;

import com.cai.common.constant.GameConstants;
import com.cai.game.shengji.SJTable;

public class SJHandlerCallBanker<T extends SJTable> extends AbstractSJHandler<T> {

	public int _seat_index = GameConstants.INVALID_SEAT;
	public int _call_action = -1;

	public void reset_status(int seat_index, int call_action) {
		_seat_index = seat_index;
		_call_action = call_action;
	}

	@Override
	public void exe(T table) {
	}

	@Override
	public boolean handler_player_be_in_room(T table, int seat_index) {
		return true;
	}

	public boolean handler_call_banker(T table, int seat_index, int call_action) {
		return true;
	}

}
