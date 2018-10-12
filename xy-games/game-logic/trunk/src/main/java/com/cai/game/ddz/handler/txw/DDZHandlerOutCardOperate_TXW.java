package com.cai.game.ddz.handler.txw;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.PBUtil;
import com.cai.domain.SheduleArgs;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.ddz.DDZConstants;
import com.cai.game.ddz.DDZMsgConstants;
import com.cai.game.ddz.handler.DDZHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.txw.TxwRsp.TableResponse_Txw;

public class DDZHandlerOutCardOperate_TXW extends DDZHandlerOutCardOperate<TXW_Table> {
	@Override
	public void exe(TXW_Table table) {
		table._prev_palyer = _out_card_player;
		// 玩家不出
		if (this._b_out_card == 0) {

			no_out_card(table);

			return;
		}

		int cbCardType = adjust_out_card_right(table);
		if (cbCardType == DDZConstants.TXW_CT_ERROR) {
			return;
		}
		table.cancelShedule(table.ID_TIMER_AUTO_OPREATE);
		table.cancelShedule(table.ID_TIMER_AUTO_TRUESS);
		table.cancelShedule(table.ID_TIMER_AUTO_PASS);
		_b_out_card = cbCardType;
		_seat_index = this._out_card_player;
		_out_type = cbCardType;
		if ((cbCardType == DDZConstants.TXW_CT_BOMB_CARD || cbCardType == DDZConstants.TXW_CT_HONG)
				&& table.has_rule(DDZConstants.GAME_RULE_TXW_BOOM_DOUBLE)) {
			int boom_times = 2;
			if (cbCardType == DDZConstants.TXW_CT_HONG) {
				boom_times = 4;
			}
			table._times *= boom_times;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i != table.GRR._banker_player && table._is_out[i] == 0) {
					table._user_times[i] *= boom_times;
					if (table._user_times[i] > table._boom_count_limit) {
						table._user_times[i] = table._boom_count_limit;
					}

				}

			}
			if (table.GRR._banker_player != GameConstants.INVALID_SEAT) {
				table._user_times[table.GRR._banker_player] = 0;
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (i != table.GRR._banker_player && table._is_out[i] == 0) {
						table._user_times[table.GRR._banker_player] += table._user_times[i];
					}
				}
			}
			table.send_player_times(GameConstants.INVALID_SEAT);

		}
		table._out_card_times[_out_card_player]++;

		int pre_turn_out_type = table._turn_out_card_type;
		int pre_out_player = table._out_card_player;
		// 保存当前出牌玩家
		table._out_card_player = _out_card_player;
		table._turn_out_card_type = cbCardType;
		table._turn_out__player = _out_card_player;
		// 保存该轮出牌信息
		table.GRR._cur_round_pass[_out_card_player] = 0;
		table.GRR._cur_round_count[_out_card_player] = this._out_card_count;
		_card_count = this._out_card_count;
		for (int i = 0; i < this._out_card_count; i++) {
			table.GRR._cur_round_data[_out_card_player][i] = this._out_cards_data[i];
			// 保存该次出牌数据
			table._turn_out_card_data[i] = this._out_cards_data[i];
			table._turn_out_real_data[i] = this._out_cards_data[i];
			_cards_data[i] = this._out_cards_data[i];
		}
		table._turn_out_card_count = this._out_card_count;
		table.GRR._card_count[_out_card_player] -= this._out_card_count;

		int next_player = (_out_card_player + 1) % table.getPlayerCount();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._is_out[next_player] == 1) {
				next_player = (next_player + 1) % table.getPlayerCount();
			} else {
				break;
			}
		}
		if (0 != table.GRR._card_count[_out_card_player]) {
			table._current_player = next_player;
			table.GRR._cur_round_count[table._current_player] = 0;
			table.GRR._cur_round_pass[table._current_player] = 0;
			table.GRR._cur_card_type[table._current_player] = DDZConstants.TXW_CT_ERROR;
			for (int j = 0; j < this._out_card_count; j++) {
				table.GRR._cur_round_data[table._current_player][j] = GameConstants.INVALID_CARD;
			}
		} else {
			table._current_player = GameConstants.INVALID_SEAT;
		}

		// 显示出牌
		table.operate_out_card(table._out_card_player, table._turn_out_card_count, table._turn_out_card_data,
				cbCardType, GameConstants.INVALID_SEAT);
		if (0 == table.GRR._card_count[_out_card_player]) {
			table._cur_banker = _out_card_player;
			if (table._round == 1) {
				table.send_round_finish(_out_card_player);
				table.schedule(table.ID_TIMER_ROUND_FINISH, SheduleArgs.newArgs(), 3000);
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					table.handler_request_trustee(i, false, 0);
				}
			} else {
				int delay = 3;
				table.send_round_finish(_out_card_player);
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					table.handler_request_trustee(i, false, 0);
				}
				GameSchedule.put(
						new GameFinishRunnable(table.getRoom_id(), _out_card_player, GameConstants.Game_End_NORMAL),
						delay, TimeUnit.SECONDS);

			}
			return;
		}

		if (table._current_player != GameConstants.INVALID_SEAT) {
			if (table.istrustee[table._current_player]) {
				table.schedule(table.ID_TIMER_AUTO_OPREATE, SheduleArgs.newArgs(), 1000);
			} else {
				table.schedule(table.ID_TIMER_AUTO_TRUESS, SheduleArgs.newArgs(), table._truess_time);
				if (table._logic.SearchOutCard_txw(table.GRR._cards_data[table._current_player],
						table.GRR._card_count[table._current_player], table._turn_out_card_data,
						table._turn_out_card_count) == 0) {
					table.schedule(table.ID_TIMER_AUTO_PASS, SheduleArgs.newArgs(), 5000);
				} else {
					if (table._logic.CompareCard(table._turn_out_card_data,
							table.GRR._cards_data[table._current_player], table._turn_out_card_count,
							table.GRR._card_count[table._current_player])) {
						table.schedule(table.ID_TIMER_AUTO_ALL_OUT, SheduleArgs.newArgs(), 1000);

					}
				}
			}

		}

	}

	public int adjust_out_card_right(TXW_Table table) {

		table._logic.sort_card_date_list(_out_cards_data, _out_card_count);
		int cbCardType = table._logic.GetCardType(this._out_cards_data, this._out_card_count, this._out_cards_data);
		if (cbCardType == DDZConstants.TXW_CT_ERROR) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return cbCardType;
		}

		table._logic.sort_card_date_list_by_type(this._out_cards_data, this._out_card_count, cbCardType);
		if (table._turn_out_card_count > 0) {
			if (!table._logic.CompareCard(table._turn_out_card_data, _out_cards_data, table._turn_out_card_count,
					_out_card_count)) {
				table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
				return DDZConstants.TXW_CT_ERROR;
			}
		}
		if (!table._logic.remove_cards_by_data(table.GRR._cards_data[_out_card_player],
				table.GRR._card_count[_out_card_player], this._out_cards_data, this._out_card_count)) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return DDZConstants.TXW_CT_ERROR;
		}
		return cbCardType;
	}

	// 玩家不出
	public void no_out_card(TXW_Table table) {

		table.GRR._cur_round_count[_out_card_player] = 0;
		table.GRR._cur_round_pass[_out_card_player] = 1;

		if (table._turn_out_card_count == 0) {
			return;
		}
		table.cancelShedule(table.ID_TIMER_AUTO_TRUESS);
		table.cancelShedule(table.ID_TIMER_AUTO_PASS);
		table.cancelShedule(table.ID_TIMER_AUTO_OPREATE);
		for (int i = 0; i < table.get_hand_card_count_max(); i++) {
			table.GRR._cur_round_data[_out_card_player][i] = GameConstants.INVALID_CARD;
		}
		table.GRR._cur_card_type[table._current_player] = DDZConstants.TXW_CT_PASS;
		// 判断下一个玩家
		// 清空接下去出牌玩家出牌数据
		int next_player = (_out_card_player + 1) % table.getPlayerCount();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._is_out[next_player] == 1) {
				next_player = (next_player + 1) % table.getPlayerCount();
			} else {
				break;
			}
		}
		table._current_player = next_player;
		table.GRR._cur_card_type[table._current_player] = DDZConstants.TXW_CT_ERROR;
		table.GRR._cur_round_count[table._current_player] = 0;
		table.GRR._cur_round_pass[table._current_player] = 0;
		for (int j = 0; j < this._out_card_count; j++) {
			table.GRR._cur_round_data[table._current_player][j] = GameConstants.INVALID_CARD;
		}
		if (table._current_player == table._out_card_player) {
			// 出完一圈牌
			table._turn_out_card_count = 0;
			for (int i = 0; i < table._turn_out_card_count; i++) {
				table._turn_out_card_data[i] = GameConstants.INVALID_CARD;
			}
			table._turn_out_card_count = 0;
			table._turn_out_card_type = DDZConstants.TXW_CT_ERROR;
			Arrays.fill(table._turn_out_card_data, GameConstants.INVALID_CARD);
			Arrays.fill(table.GRR._cur_round_count, 0);
			Arrays.fill(table.GRR._cur_round_pass, 0);
			if (table._logic.GetCardType(table.GRR._cards_data[table._current_player],
					table.GRR._card_count[table._current_player],
					table.GRR._cards_data[table._current_player]) != DDZConstants.TXW_CT_ERROR) {
				table.schedule(table.ID_TIMER_AUTO_ALL_OUT, SheduleArgs.newArgs(), 1000);

			}
		} else {
			if (table._logic.SearchOutCard_txw(table.GRR._cards_data[table._current_player],
					table.GRR._card_count[table._current_player], table._turn_out_card_data,
					table._turn_out_card_count) == 0) {
				table.schedule(table.ID_TIMER_AUTO_PASS, SheduleArgs.newArgs(), 5000);
			}
		}

		// 显示出牌
		table.operate_out_card(_out_card_player, 0, _out_cards_data, DDZConstants.TXW_CT_PASS,
				GameConstants.INVALID_SEAT);

		if (table._current_player != GameConstants.INVALID_SEAT) {
			if (table.istrustee[table._current_player]) {
				table.schedule(table.ID_TIMER_AUTO_OPREATE, SheduleArgs.newArgs(), 1000);
			} else {
				table.schedule(table.ID_TIMER_AUTO_TRUESS, SheduleArgs.newArgs(), table._truess_time);
				if (table._logic.SearchOutCard_txw(table.GRR._cards_data[table._current_player],
						table.GRR._card_count[table._current_player], table._turn_out_card_data,
						table._turn_out_card_count) == 0) {
					table.schedule(table.ID_TIMER_AUTO_PASS, SheduleArgs.newArgs(), 5000);
				} else {
					if (table._logic.CompareCard(table._turn_out_card_data,
							table.GRR._cards_data[table._current_player], table._turn_out_card_count,
							table.GRR._card_count[table._current_player])) {
						table.schedule(table.ID_TIMER_AUTO_ALL_OUT, SheduleArgs.newArgs(), 1000);

					}
				}
			}
		}

	}

	@Override
	public boolean handler_player_be_in_room(TXW_Table table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(DDZMsgConstants.RESPONSE_TXW_RECONNECT_DATA);

		TableResponse_Txw.Builder tableResponse_ddz = TableResponse_Txw.newBuilder();
		table.load_player_info_data_reconnect(tableResponse_ddz);
		tableResponse_ddz.setRoomInfo(table.getRoomInfo());

		tableResponse_ddz.setBankerPlayer(table.GRR._banker_player);
		tableResponse_ddz.setCurrentPlayer(table._current_player);
		tableResponse_ddz.setPrevPlayer(table._prev_palyer);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse_ddz.addOutCardsCount(table.GRR._cur_round_count[i]);
			tableResponse_ddz.addPlayerPass(table.GRR._cur_round_pass[i]);
			Int32ArrayResponse.Builder out_cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder out_change_cards = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < table.GRR._cur_round_count[i]; j++) {
				if (table.GRR._cur_round_count[i] > 0) {
					out_cards.addItem(table.GRR._cur_round_data[i][j]);
					out_change_cards.addItem(table.GRR._cur_round_data[i][j]);
				}
			}
			tableResponse_ddz.addCardCount(table.GRR._card_count[i]);
			tableResponse_ddz.addCardType(table.GRR._cur_card_type[i]);
			tableResponse_ddz.addOutCardsData(i, out_cards);
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			tableResponse_ddz.addCardsData(i, cards_card);
		}

		if (table._current_player == seat_index) {
			if (table._turn_out_card_count == 0) {
				tableResponse_ddz.setIsCanYa(true);
			} else {
				if (table._logic.SearchOutCard_txw(table.GRR._cards_data[table._current_player],
						table.GRR._card_count[table._current_player], table._turn_out_card_data,
						table._turn_out_card_count) == 0) {
					tableResponse_ddz.setIsCanYa(false);
				} else {
					tableResponse_ddz.setIsCanYa(true);
				}
			}
		}
		if (table._turn_out_card_count == 0) {
			tableResponse_ddz.setIsFirstOut(1);
		} else {
			tableResponse_ddz.setIsFirstOut(0);
		}
		// 手牌--将自己的手牌数据发给自己
		Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
		for (int j = 0; j < table.GRR._card_count[seat_index]; j++) {
			cards_card.addItem(table.GRR._cards_data[seat_index][j]);
		}
		tableResponse_ddz.setCardsData(seat_index, cards_card);
		for (int i = 0; i < table._turn_out_card_count; i++) {
			if (table._turn_out_card_count > 0) {
				tableResponse_ddz.addPrCardsData(table._turn_out_card_data[i]);
			}
		}
		tableResponse_ddz.setPrCardsCount(table._turn_out_card_count);
		tableResponse_ddz.setPrOutCardType(table._turn_out_card_type);
		tableResponse_ddz.setPrOutCardPlayer(table._turn_out__player);
		tableResponse_ddz.setRound(table._round);
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_ddz));
		table.send_response_to_player(seat_index, roomResponse);

		// 显示出牌
		table.operate_out_card(_seat_index, _card_count, _cards_data, _out_type, seat_index);
		table.send_player_times(seat_index);
		table.send_callbaner_result();
		return true;
	}
}
