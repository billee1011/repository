package com.cai.game.zhadan.handler;

import com.cai.common.constant.GameConstants;
import com.cai.game.zhadan.AbstractZhaDanTable;

public class ZhaDanHandlerOutCardOperate<T extends AbstractZhaDanTable> extends AbstractZhaDanHandler<T> {

	public int _out_card_player = GameConstants.INVALID_SEAT; // 出牌用户
	public int[] _out_cards_data = new int[GameConstants.WSK_MAX_COUNT]; // 出牌扑克
	public int _out_card_count = 0;
	public int[] _cards = new int[GameConstants.WSK_MAX_COUNT]; // 出牌扑克
	public int _card_count = 0;
	public int _seat_index = GameConstants.INVALID_SEAT;
	public int _out_type = -1;
	public int _pai_score_card[] = new int[GameConstants.WSK_MAX_COUNT];
	public int _pai_score_count = 0;

	public void reset_status(int seat_index, int cards[], int card_count, int is_out) {
		_out_card_player = seat_index;
		_out_cards_data = new int[card_count];
		for (int i = 0; i < card_count; i++) {
			_out_cards_data[i] = cards[i];
		}
		_out_card_count = card_count;
		_out_type = is_out;
	}

	public void reset() {
		_out_card_count = 0;
		_seat_index = GameConstants.INVALID_SEAT;
		_out_card_count = 0;
		_out_card_player = GameConstants.INVALID_SEAT;
	}

	@Override
	public void exe(T table) {
	}

	@Override
	public boolean handler_player_be_in_room(T table, int seat_index) {
		return true;
	}

}
