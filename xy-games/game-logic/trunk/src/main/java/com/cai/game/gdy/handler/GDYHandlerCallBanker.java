package com.cai.game.gdy.handler;

import com.cai.game.gdy.AbstractGDYTable;

public class GDYHandlerCallBanker<T extends AbstractGDYTable> extends AbstractGDYHandler<T> {
	protected int _seat_index;
	protected int _game_status;
	// private int _current_player =MJGameConstants.INVALID_SEAT;

	public GDYHandlerCallBanker() {
	}

	public void reset_status(int seat_index, int game_status) {
		_seat_index = seat_index;
		_game_status = game_status;
	}

	@Override
	public void exe(T table) {

	}

	@Override
	public boolean handler_player_be_in_room(T table, int seat_index) {
		return true;
	}

	/**
	 * @param get_seat_index
	 * @param call_banker
	 *            -1为没有进行叫地主操作，0为不叫地主，大于0为叫地主
	 * @param qiang_bangker
	 *            -1为没有进行抢地主操作，0为不抢地主，大于0为抢地主
	 * @return
	 */
	public boolean handler_call_banker(T table, int seat_index, int call_banker, int qiang_bangker) {
		return true;
	}

}
