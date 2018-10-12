package com.cai.game.hongershi.hy;

import com.cai.common.constant.GameConstants;
import com.cai.game.hh.handler.HHHandlerDispatchCard;

public class HandlerDispatchFirstCard extends HHHandlerDispatchCard<HongErShiTable_HY> {

	@SuppressWarnings({ "static-access" })
	@Override
	public void exe(HongErShiTable_HY table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();

		table._current_player = _seat_index;
		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		table.GRR._left_card_count--;

		if (table.DEBUG_CARDS_MODE) {
			_send_card_data = 0x13;
		}

		table._send_card_data = _send_card_data;
		table._provide_player = _seat_index;

		table.is_mo_or_show = true;
		table.operate_player_mo_card(_seat_index, 1, new int[] { table._send_card_data }, table.GRR._card_count[_seat_index],
				GameConstants.INVALID_SEAT, true);
		table.GRR._cards_data[_seat_index][table.GRR._card_count[_seat_index]++] = _send_card_data;
		table._provide_card = table._send_card_data;
		table.exe_chuli_first_card(_seat_index, GameConstants.WIK_NULL, 500);

		return;
	}

	@Override
	public boolean handler_player_be_in_room(HongErShiTable_HY table, int seat_index) {

		table.handler_player_be_in_room(table, seat_index);
		return true;
	}
}
