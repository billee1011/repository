package com.cai.game.ddz.handler.lfddz;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.util.PBUtil;
import com.cai.domain.SheduleArgs;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.ddz.DDZConstants;
import com.cai.game.ddz.DDZMsgConstants;
import com.cai.game.ddz.handler.DDZHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.ddz_lf.ddz_lfRsp.TableResponse_DDZ_LF;

public class DDZHandlerOutCardOperate_LF extends DDZHandlerOutCardOperate<DDZ_LF_Table> {
	@Override
	public void exe(DDZ_LF_Table table) {
		// TODO Auto-generated method stub
		PlayerStatus playerStatus = table._playerStatus[_out_card_player];
		playerStatus.reset();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}
		table._prev_palyer = _out_card_player;

		if (table._auto_out_card_scheduled != null) {
			table._auto_out_card_scheduled.cancel(false);
		}
		if (table._trustee_auto_opreate_scheduled[_out_card_player] != null) {
			table._trustee_auto_opreate_scheduled[_out_card_player].cancel(false);
		}
		table._auto_out_card_scheduled = null;
		table._trustee_auto_opreate_scheduled[_out_card_player] = null;

		table.cancelShedule(table.ID_TIMER_AUTO_OUT_CARD);
		// 玩家不出
		if (this._b_out_card == 0) {

			no_out_card(table);
			return;
		}
		for (int i = 0; i < _out_card_count; i++) {
			if (_out_cards_data[i] > 0x100) {
				_out_cards_data[i] -= 0x100;
			}
		}

		int cbCardType = adjust_out_card_right(table);
		if (cbCardType == DDZConstants.DDZ_LF_CT_ERROR) {
			return;
		}
		if (_out_card_player == table.GRR._banker_player) {
			for (int i = 0; i < _out_card_count; i++) {
				_out_cards_data[i] += 0x200;
			}
		}

		if (cbCardType == DDZConstants.DDZ_LF_CT_BOMB_CARD || cbCardType == DDZConstants.DDZ_LF_CT_MISSILE_CARD) {
			int boom_times = 2;
			if (table._logic.get_card_value(_out_cards_data[0]) == 3 && _out_card_count == 4) {
				boom_times = 8;
			}
			table._times *= boom_times;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i != table.GRR._banker_player) {
					if (table._user_times[i] * boom_times > Math.pow(2, table._boom_count_limit)) {
						table._user_times[i] = (int) Math.pow(2, table._boom_count_limit);
					} else {
						table._user_times[i] *= boom_times;
					}

				}

			}
			table._user_times[table.GRR._banker_player] = 0;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i != table.GRR._banker_player) {
					table._user_times[table.GRR._banker_player] += table._user_times[i];
				}
			}

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == table.GRR._banker_player) {
					table.Send_di_Card(false, true, i);
				} else {
					table.Send_di_Card(false, false, i);
				}
			}
			table.Send_di_Card(false, true, GameConstants.INVALID_SEAT);
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
		for (int i = 0; i < this._out_card_count; i++) {
			table.GRR._cur_round_data[_out_card_player][i] = this._out_cards_data[i];
			// 保存该次出牌数据
			table._turn_out_card_data[i] = this._out_cards_data[i];
			table._turn_out_real_data[i] = this._out_cards_data[i];
		}
		table._turn_out_card_count = this._out_card_count;
		table.GRR._card_count[_out_card_player] -= this._out_card_count;

		// 刷新手牌
		int cards[] = new int[GameConstants.MAX_HH_COUNT];

		// 清空接下去出牌玩家出牌数据
		int next_player = (_out_card_player + table.getPlayerCount() + 1) % table.getPlayerCount();
		if (0 != table.GRR._card_count[_out_card_player]) {
			table._current_player = next_player;
			table.GRR._cur_round_count[table._current_player] = 0;
			table.GRR._cur_round_pass[table._current_player] = 0;
			for (int j = 0; j < this._out_card_count; j++) {
				table.GRR._cur_round_data[table._current_player][j] = GameConstants.INVALID_CARD;
			}
		} else {
			table._current_player = GameConstants.INVALID_SEAT;
		}

		// 显示出牌
		table.operate_out_card(table._out_card_player, table._turn_out_card_count, table._turn_out_card_data,
				cbCardType, GameConstants.INVALID_SEAT);
		table.refresh_card_data(0);
		if (0 == table.GRR._card_count[_out_card_player]) {
			int delay = 1;
			table._banker_select = _out_card_player;
			GameSchedule.put(
					new GameFinishRunnable(table.getRoom_id(), _out_card_player, GameConstants.Game_End_NORMAL), delay,
					TimeUnit.SECONDS);
			return;
		}

		int delay = 0;
		if (!table._logic.SearchOutCard(table.GRR._cards_data[table._current_player],
				table.GRR._card_count[table._current_player], table._turn_out_card_data, table._turn_out_card_count)) {
			delay = 3;

		} else {
			delay = 20;
			if (table._logic.is_auto_out(table.GRR._cards_data[table._current_player],
					table.GRR._card_count[table._current_player], table._turn_out_card_data, table._turn_out_card_count)
					&& table.GRR._card_count[table._current_player] == 1) {
				SheduleArgs args = SheduleArgs.newArgs();
				args.set("card_count", table.GRR._card_count[table._current_player]);
				args.set("seat_index", table._current_player);
				for (int i = 0; i < table.GRR._card_count[table._current_player]; i++) {
					args.set("card_data_" + i, table.GRR._cards_data[table._current_player][i]);
				}

				table.schedule(table.ID_TIMER_AUTO_OUT_CARD, args, 1500);
			}
		}
		// table._auto_out_card_scheduled = GameSchedule.put(
		// new DDZAutoOutCardRunnable(table.getRoom_id(), table._current_player,
		// table), delay, TimeUnit.SECONDS);

	}

	public int adjust_out_card_right(DDZ_LF_Table table) {

		table._logic.sort_card_date_list(_out_cards_data, _out_card_count);
		int cbCardType = table._logic.GetCardType(this._out_cards_data, this._out_card_count, this._out_cards_data);
		if (cbCardType == DDZConstants.DDZ_LF_CT_ERROR) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return cbCardType;
		}
		if (cbCardType == DDZConstants.DDZ_LF_CT_FOUR_TAKE_TWO && _out_card_count == 5
				&& _out_card_count != table.GRR._card_count[_out_card_player]) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return DDZConstants.DDZ_LF_CT_ERROR;
		}
		if (cbCardType == DDZConstants.DDZ_LF_CT_THREE || cbCardType == DDZConstants.DDZ_LF_CT_THREE_LINE_LOST) {
			if (table.GRR._card_count[_out_card_player] != _out_card_count) {
				table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
				return DDZConstants.DDZ_LF_CT_ERROR;
			}
		}

		table._logic.sort_card_date_list_by_type(this._out_cards_data, this._out_card_count, cbCardType);
		if (table._turn_out_card_count > 0) {
			if (!table._logic.CompareCard(table._turn_out_card_data, _out_cards_data, table._turn_out_card_count,
					_out_card_count)) {
				table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
				return DDZConstants.DDZ_LF_CT_ERROR;
			}
		}
		if (table.GRR._card_count[_out_card_player] - _out_card_count == 1) {
			int hand_data[] = new int[table.GRR._card_count[_out_card_player]];
			for (int i = 0; i < table.GRR._card_count[_out_card_player]; i++) {
				hand_data[i] = table.GRR._cards_data[_out_card_player][i];
			}
			if (!table._logic.remove_cards_by_data(hand_data, table.GRR._card_count[_out_card_player],
					this._out_cards_data, this._out_card_count)) {
				table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
				return DDZConstants.DDZ_LF_CT_ERROR;
			}
			if (hand_data[0] == 0x5E) {
				table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
				return DDZConstants.DDZ_LF_CT_ERROR;
			}
		}
		if (!table._logic.remove_cards_by_data(table.GRR._cards_data[_out_card_player],
				table.GRR._card_count[_out_card_player], this._out_cards_data, this._out_card_count)) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return DDZConstants.DDZ_LF_CT_ERROR;
		}
		return cbCardType;
	}

	// 玩家不出
	public void no_out_card(DDZ_LF_Table table) {

		table.GRR._cur_round_count[_out_card_player] = 0;
		table.GRR._cur_round_pass[_out_card_player] = 1;

		if (table._turn_out_card_count == 0) {
			return;
		}

		for (int i = 0; i < table.get_hand_card_count_max(); i++) {
			table.GRR._cur_round_data[_out_card_player][i] = GameConstants.INVALID_CARD;
		}

		// 判断下一个玩家
		int next_player = (_out_card_player + table.getPlayerCount() + 1) % table.getPlayerCount();
		table._current_player = next_player;
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
			table._turn_out_card_type = GameConstants.DDZ_CT_ERROR;
			Arrays.fill(table._turn_out_card_data, GameConstants.INVALID_CARD);
			Arrays.fill(table.GRR._cur_round_count, 0);
			Arrays.fill(table.GRR._cur_round_pass, 0);
		}

		// 显示出牌
		table.operate_out_card(_out_card_player, 0, _out_cards_data, GameConstants.DDZ_CT_PASS,
				GameConstants.INVALID_SEAT);

		int delay = 0;
		if (!table._logic.SearchOutCard(table.GRR._cards_data[table._current_player],
				table.GRR._card_count[table._current_player], table._turn_out_card_data, table._turn_out_card_count)) {
			delay = 3;
		} else {
			delay = 20;
		}
		// table._auto_out_card_scheduled = GameSchedule.put(
		// new DDZAutoOutCardRunnable(table.getRoom_id(), table._current_player,
		// table), delay, TimeUnit.SECONDS);

		if (table._logic.is_auto_out(table.GRR._cards_data[table._current_player],
				table.GRR._card_count[table._current_player], table._turn_out_card_data, table._turn_out_card_count)
				&& table.GRR._card_count[table._current_player] == 1) {
			SheduleArgs args = SheduleArgs.newArgs();
			args.set("card_count", table.GRR._card_count[table._current_player]);
			args.set("seat_index", table._current_player);
			for (int i = 0; i < table.GRR._card_count[table._current_player]; i++) {
				args.set("card_data_" + i, table.GRR._cards_data[table._current_player][i]);
			}

			table.schedule(table.ID_TIMER_AUTO_OUT_CARD, args, 1500);
		}

	}

	@Override
	public boolean handler_player_be_in_room(DDZ_LF_Table table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(DDZMsgConstants.RESPONSE_DDZ_RECONNECT_DATA);

		TableResponse_DDZ_LF.Builder tableResponse_ddz = TableResponse_DDZ_LF.newBuilder();
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
			tableResponse_ddz.addChangeCardsData(out_change_cards);
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			tableResponse_ddz.addCardsData(i, cards_card);
		}

		if (table._current_player == seat_index) {
			if (table._turn_out_card_count == 0) {
				tableResponse_ddz.setIsYaPai(1);
			} else {
				if (!table._logic.SearchOutCard(table.GRR._cards_data[table._current_player],
						table.GRR._card_count[table._current_player], table._turn_out_card_data,
						table._turn_out_card_count)) {
					tableResponse_ddz.setIsYaPai(0);
				} else {
					tableResponse_ddz.setIsYaPai(1);
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
				tableResponse_ddz.addPrChangeCardsData(table._turn_out_card_data[i]);
			}
		}
		tableResponse_ddz.setPrCardsCount(table._turn_out_card_count);
		tableResponse_ddz.setPrOutCardType(table._turn_out_card_type);
		tableResponse_ddz.setPrOutCardPlayer(table._turn_out__player);

		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_ddz));
		table.send_response_to_player(seat_index, roomResponse);

		if (seat_index == table.GRR._banker_player) {
			table.Send_di_Card(false, true, seat_index);
		} else {
			table.Send_di_Card(false, false, seat_index);
		}

		int cur_action[] = new int[2];
		cur_action[0] = 0;
		cur_action[1] = 2;
		table.call_banker_resopnse(2, 2, GameConstants.INVALID_SEAT, 0, GameConstants.INVALID_SEAT, cur_action,
				seat_index);
		return true;
	}
}
