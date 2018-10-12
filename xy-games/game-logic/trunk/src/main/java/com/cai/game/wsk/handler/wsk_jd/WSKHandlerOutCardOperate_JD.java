package com.cai.game.wsk.handler.wsk_jd;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.wsk.handler.WSKHandlerOutCardOperate;

public class WSKHandlerOutCardOperate_JD extends WSKHandlerOutCardOperate<WSKTable_JD> {

	@Override
	public void exe(WSKTable_JD table) {
		if (_out_card_player != table._current_player) {
			return;
		}
		// 玩家不出
		if (_out_type == 0) {
			if (table._out_card_player == _out_card_player) {
				return;
			}
			// 清空接下去出牌玩家出牌数据
			int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table.GRR._card_count[next_player] != 0) {
					break;
				}
				next_player = (next_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			}

			if (next_player == _out_card_player) {
				table._current_player = GameConstants.INVALID_SEAT;
				table.get_score[_out_card_player] += table.turn_have_score;
				table.turn_have_score = 0;
			} else {
				table._current_player = next_player;
				if (next_player == table._out_card_player) {
					table.get_score[_out_card_player] += table.turn_have_score;
					table.turn_have_score = 0;
					table._turn_out_card_count = 0;
					Arrays.fill(table._turn_out_card_data, GameConstants.INVALID_CARD);
				}
			}
			table._prev_palyer = _out_card_player;
			// 显示出牌
			table.operate_out_card(table._out_card_player, table._turn_out_card_count, table._turn_out_card_data,
					GameConstants.DMZ_CT_PASS, GameConstants.INVALID_SEAT, false);

			if (next_player == GameConstants.INVALID_SEAT) {
				GameSchedule.put(
						new GameFinishRunnable(table.getRoom_id(), _out_card_player, GameConstants.Game_End_NORMAL), 3,
						TimeUnit.SECONDS);
			}
			return;
		}

		// 出牌判断
		int card_type = adjust_out_card_right(table);
		if (card_type == GameConstants.WSK_CT_ERROR) {
			return;
		}
		table.turn_have_score += table._logic.GetCardScore(_out_cards_data, _out_card_count);
		table._turn_out_card_type = card_type;
		table._turn_out_card_count = _out_card_count;
		table._out_card_player = _out_card_player;
		table._prev_palyer = _out_card_player;
		Arrays.fill(table._turn_out_card_data, GameConstants.INVALID_CARD);
		for (int i = 0; i < _out_card_count; i++) {
			table._turn_out_card_data[i] = _out_cards_data[i];
		}
		// 清空接下去出牌玩家出牌数据
		int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
		table._current_player = next_player;
		table._cur_out_card_count[next_player] = 0;
		Arrays.fill(table._cur_out_card_data[next_player], GameConstants.INVALID_CARD);
		// 显示出牌
		table.operate_out_card(table._out_card_player, table._turn_out_card_count, table._turn_out_card_data,
				table._turn_out_card_type, GameConstants.INVALID_SEAT, false);
	}

	public int adjust_out_card_right(WSKTable_JD table) {
		int card_type = table._logic.GetCardType_WSK(_out_cards_data, _out_card_count);
		if (card_type == GameConstants.WSK_CT_ERROR) {
			return GameConstants.WSK_CT_ERROR;
		}
		if (table._out_card_player != _out_card_player) {
			if (!table._logic.CompareCard_WSK(table._turn_out_card_data, _out_cards_data, table._turn_out_card_count,
					_out_card_count)) {
				return GameConstants.WSK_CT_ERROR;
			}
		}
		return card_type;
	}

	@Override
	public boolean handler_player_be_in_room(WSKTable_JD table, int seat_index) {

		return true;
	}

}
