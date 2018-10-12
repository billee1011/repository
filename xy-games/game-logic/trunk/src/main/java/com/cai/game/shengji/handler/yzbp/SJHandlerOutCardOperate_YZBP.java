package com.cai.game.shengji.handler.yzbp;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.PBUtil;
import com.cai.domain.SheduleArgs;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.shengji.SJMsgConstants;
import com.cai.game.shengji.data.tagAnalyseCardType;
import com.cai.game.shengji.handler.SJHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.yzbp.yzbpRsp.TableResponse_Yzbp;

public class SJHandlerOutCardOperate_YZBP extends SJHandlerOutCardOperate<SJTable_YZBP> {

	@Override
	public void exe(SJTable_YZBP table) {
		if (_out_card_player != table._current_player) {
			return;
		}

		// 玩家不出
		if (_out_type == 0) {
			return;
		}
		int cbCardType = adjust_out_card_right(table);
		int effect_type = 0;
		int target_seat = GameConstants.INVALID_SEAT;
		int orign_seat = GameConstants.INVALID_SEAT;
		if (cbCardType == GameConstants.XFGD_CT_ERROR) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型");
			return;
		}
		if (!table._logic.remove_cards_by_data(table.GRR._cards_data[_out_card_player],
				table.GRR._card_count[_out_card_player], _out_cards_data, _out_card_count)) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型");
			return;
		}
		if (table._max_card_seat == GameConstants.INVALID_SEAT) {
			table._max_card_seat = _out_card_player;
			// 保存最大出牌玩家牌型数据
			for (int i = 0; i < table._turn_out_card_count; i++) {
				table._turn_out_card_data[i] = 0;

			}
			table._turn_out_card_count = _out_card_count;
			table._origin_out_card_count = _out_card_count;
			table._origin_out_card_player = _out_card_player;
			table._turn_out_card_type = cbCardType;
			table._origin_out_card_type = cbCardType;
			for (int i = 0; i < table._turn_out_card_count; i++) {
				table._turn_out_card_data[i] = _out_cards_data[i];
				table._origin_out_card_data[i] = _out_cards_data[i];
			}
			table._kill_ed = GameConstants.INVALID_SEAT;
			table._kill_ing = GameConstants.INVALID_SEAT;
		} else {
			if (table._logic.comparecarddata(table._turn_out_card_data, table._turn_out_card_count, _out_cards_data,
					_out_card_count)) {
				if (table._turn_out_card_data[0] < table._zhu_value && _out_cards_data[0] > table._zhu_value) {
					table._kill_ed = table._max_card_seat;
					table._kill_ing = _out_card_player;
				} else {
					table._kill_ed = GameConstants.INVALID_SEAT;
					table._kill_ing = GameConstants.INVALID_SEAT;
				}

				if (_out_cards_data[0] > table._zhu_value) {
					if (table._turn_out_card_data[0] > table._zhu_value
							&& table._origin_out_card_data[0] < table._zhu_value) {
						effect_type = table.EFFECT_CALL_GAI_BI;
						target_seat = table._max_card_seat;
						orign_seat = _out_card_player;
					}

				}
				table._max_card_seat = _out_card_player;
				// 保存最大出牌玩家牌型数据
				for (int i = 0; i < table._turn_out_card_count; i++) {
					table._turn_out_card_data[i] = 0;
				}
				table._turn_out_card_count = _out_card_count;
				table._turn_out_card_type = cbCardType;
				for (int i = 0; i < table._turn_out_card_count; i++) {
					table._turn_out_card_data[i] = _out_cards_data[i];
				}
			} else {
				table._kill_ed = GameConstants.INVALID_SEAT;
				table._kill_ing = GameConstants.INVALID_SEAT;
			}
		}
		if (_out_card_player == table.GRR._banker_player) {
			table._extra_out_card_count = _out_card_count;
			for (int i = 0; i < _out_card_count; i++) {
				table._extra_out_card_data[i] = _out_cards_data[i];
			}

		}
		_out_type = cbCardType;
		table._table_score += table._logic.GetCardScore(_out_cards_data, _out_card_count);
		// 下一个玩家
		table._current_player = (_out_card_player + 1) % table.getTablePlayerNumber();
		table.GRR._card_count[_out_card_player] -= _out_card_count;
		table._out_card_player = _out_card_player;
		boolean is_kou_di = false;
		// 保存当前玩家本轮出牌数据
		Arrays.fill(table._cur_out_card_data[_out_card_player], 0);
		for (int i = 0; i < _out_card_count; i++) {
			if (table._logic.GetCardValue(_out_cards_data[i]) == 5
					|| table._logic.GetCardValue(_out_cards_data[i]) == 10
					|| table._logic.GetCardValue(_out_cards_data[i]) == 13) {
				_pai_score_card[_pai_score_count++] = _out_cards_data[i];
			}
			table._out_cards_data[i] = _out_cards_data[i];
			table._cur_out_card_data[_out_card_player][i] = _out_cards_data[i];
			// table._history_out_card[_out_card_player][table._out_card_times[_out_card_player]][table._history_out_count[_out_card_player]++][i]
			// = _out_cards_data[i];
			table._history_out_card[_out_card_player][table._out_card_times[_out_card_player]][table._history_out_count[_out_card_player][table._out_card_times[_out_card_player]]++] = _out_cards_data[i];
		}
		table._cur_out_card_type[_out_card_player] = cbCardType;
		table._cur_out_card_count[_out_card_player] = _out_card_count;
		table._out_card_times[_out_card_player]++;
		table._turn_out_card_type = cbCardType;
		if (table._cur_out_card_count[table._current_player] != 0) {
			// 记录牌分
			if (table._max_card_seat != table.GRR._banker_player) {
				for (int i = 0; i < this._pai_score_count; i++) {
					table._pai_score_card[table._pai_score_count++] = this._pai_score_card[i];
				}
			}
			_pai_score_count = 0;

			table._get_score[table._max_card_seat] += table._table_score;
			if (table.GRR._card_count[table._current_player] == 0 && table._turn_out_card_data[0] > table._zhu_value
					&& table._max_card_seat != table.GRR._banker_player) {
				int di_score = table._logic.GetCardScore(table._di_pai, table._di_pai_count);
				int turn_type = table._logic.GetCardType(table._turn_out_card_data, table._turn_out_card_count);
				if (turn_type == GameConstants.XFGD_CT_DOUBLE) {
					di_score *= 2;
				} else if (turn_type == GameConstants.XFGD_CT_DOUBLE_LINK) {
					di_score *= _out_card_count;
				} else if (turn_type == GameConstants.XFGD_CT_SHUAI_PAI) {
					int max_type = GameConstants.XFGD_CT_SINGLE;
					int max_count = 4;
					tagAnalyseCardType type_card = new tagAnalyseCardType();
					table._logic.Analyse_card_type(table._turn_out_card_data, table._turn_out_card_count, type_card);
					for (int i = 0; i < type_card.type_count; i++) {
						if (type_card.type[i] > max_type) {
							max_type = type_card.type[i];
						} else if (max_type == type_card.type[i]
								&& type_card.type[i] == GameConstants.XFGD_CT_DOUBLE_LINK) {
							if (type_card.count[i] > max_count) {
								max_count = type_card.count[i];
							}
						}
					}
					if (max_type == GameConstants.XFGD_CT_DOUBLE) {
						di_score *= 2;
					} else if (max_type == GameConstants.XFGD_CT_DOUBLE_LINK) {
						di_score *= max_count;
					}
				}
				table._kou_di_score = di_score;
				is_kou_di = true;
			}
			if (table._table_score > 0 && table._max_card_seat != table.GRR._banker_player) {
				if (table._pai_score_count > 0) {
					SheduleArgs args = SheduleArgs.newArgs();
					args.set("pai_data", table._pai_score_card[table._pai_score_count - 1]);
					args.set("score", table._table_score);
					table.schedule(table.ID_TIMER_GET_SCORE_EFFECT, args, 1500);

				}

			}
			table._table_score = 0;

			for (int i = 0; i < table._turn_out_card_count; i++) {
				table._turn_out_card_data[i] = GameConstants.INVALID_CARD;
			}
			for (int i = 0; i < table._origin_out_card_count; i++) {
				table._origin_out_card_data[i] = GameConstants.INVALID_CARD;
			}
			table._turn_out_card_count = 0;
			table._origin_out_card_count = 0;
			table._turn_out_card_type = GameConstants.XFGD_CT_ERROR;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				Arrays.fill(table._cur_out_card_data[i], 0);
				table._cur_out_card_type[i] = GameConstants.XFGD_CT_ERROR;
				table._cur_out_card_count[i] = 0;
			}

			int xian_get_score = 0;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._is_cam_han_lai_fen[i] = false;
				table._is_han_lai_fen[i] = false;
				if (i != table.GRR._banker_player) {
					xian_get_score += table._get_score[i];
				}

				table.send_han_lai_score(i, 3, i);
				if (table._is_cam_han_lai_fen[i]) {
					table.send_han_lai_score(i, 3, i);
				}
				if (table._is_han_lai_fen[i]) {
					table.send_han_lai_score(i, 4, GameConstants.INVALID_SEAT);
				}

			}

			if (table.GRR._card_count[table._current_player] == 0) {
				if (is_kou_di) {
					table.schedule(table.ID_TIMER_END_KOU_DI, SheduleArgs.newArgs(), 1500);

				} else {
					int delay = 3;
					GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._current_player,
							GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
				}
			} else {
				table.schedule(table.ID_TIMER_CLEAR_CARD, SheduleArgs.newArgs(), 1500);
			}
			table._current_player = GameConstants.INVALID_SEAT;
			table.operate_out_card(_out_card_player, _out_card_count, _out_cards_data, cbCardType,
					GameConstants.INVALID_SEAT);

			_out_card_player = GameConstants.INVALID_SEAT;
			_out_card_count = 0;
			_out_type = GameConstants.XFGD_CT_ERROR;
		} else {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i != table.GRR._banker_player) {
					if (table._max_card_seat == table.GRR._banker_player) {
						if (table._logic.Search_Can_Big_Card(table._turn_out_card_data, table._turn_out_card_count,
								table.GRR._cards_data[i], table.GRR._card_count[i], table._origin_out_card_data,
								table._origin_out_card_count) && !table._is_han_lai_fen[i]) {
							if (table._cur_out_card_count[i] == 0) {
								table._is_cam_han_lai_fen[i] = true;
								table.send_han_lai_score(i, 1, i);
							}

						}
					}
				}
			}
			table.send_han_lai_score(_out_card_player, 3, _out_card_player);
			table.send_han_lai_score(_out_card_player, 4, GameConstants.INVALID_SEAT);
			table.operate_out_card(_out_card_player, _out_card_count, _out_cards_data, cbCardType,
					GameConstants.INVALID_SEAT);
		}
		if (effect_type != 0) {
			SheduleArgs args = SheduleArgs.newArgs();
			args.set("orign_seat", orign_seat);
			args.set("target_seat", target_seat);
			args.set("effect_type", effect_type);
			table.schedule(table.ID_TIMER_GAI_BI_TYPE, args, 500);

		}

		table._out_card_count = _out_card_count;
		table._out_card_player = _out_card_player;
		// table.RefreshScore(GameConstants.INVALID_SEAT);
		table.send_history(GameConstants.INVALID_SEAT);
		table.Refresh_pai_score(GameConstants.INVALID_SEAT);
		table._turn_out_shuai_pai_count = 0;
		if (table._current_player != GameConstants.INVALID_SEAT) {
			if (table.GRR._card_count[_out_card_player] == 0) {
				table._handler_out_card_operate.reset_status(table._current_player,
						table.GRR._cards_data[table._current_player], table.GRR._card_count[table._current_player],
						GameConstants.XFGD_CT_DOUBLE);
				table._handler_out_card_operate.exe(table);
			}
		}

	}

	public int adjust_out_card_right(SJTable_YZBP table) {

		int cbCardType = table._logic.GetCardType(_out_cards_data, _out_card_count);

		if (table._origin_out_card_count == 0) {
			if (cbCardType == GameConstants.XFGD_CT_ERROR) {
				return cbCardType;
			}
		}

		if (table._origin_out_card_count != 0) {
			if (!table._logic.is_he_li(table._origin_out_card_data, table._origin_out_card_count, _out_cards_data,
					_out_card_count, table.GRR._cards_data[_out_card_player], table.GRR._card_count[_out_card_player],
					table._extra_out_card_data, table._extra_out_card_count, table._is_han_lai_fen[_out_card_player])) {
				return GameConstants.XFGD_CT_ERROR;
			}
			if (cbCardType == GameConstants.XFGD_CT_SHUAI_PAI) {
				cbCardType = GameConstants.XFGD_CT_DIAN_PAI;
			}
			if (cbCardType == GameConstants.XFGD_CT_ERROR) {
				cbCardType = GameConstants.XFGD_CT_DIAN_PAI;
			}

			int color = table._logic.GetCardColor(_out_cards_data[0]);
			if (_out_cards_data[0] > table._zhu_value) {
				color = table._zhu_type;
			}
			int turn_color = table._logic.GetCardColor(table._origin_out_card_data[0]);
			if (table._origin_out_card_data[0] > table._zhu_value) {
				turn_color = table._zhu_type;
			}
			int turn_type = table._logic.GetCardType(table._origin_out_card_data, table._origin_out_card_count);
			if (color != turn_color && color != table._zhu_type) {
				return GameConstants.XFGD_CT_DIAN_PAI;
			}
			if (turn_type == GameConstants.XFGD_CT_DOUBLE || turn_type == GameConstants.XFGD_CT_SINGLE) {
				if (turn_type != cbCardType) {
					return GameConstants.XFGD_CT_DIAN_PAI;
				}
			}
		} else {

			if (cbCardType == GameConstants.XFGD_CT_SHUAI_PAI) {
				tagAnalyseCardType type_card = new tagAnalyseCardType();
				table._logic.Analyse_card_type(_out_cards_data, _out_card_count, type_card);

				for (int i = 1; i < _out_card_count; i++) {
					if (_out_cards_data[i] < table._zhu_value) {
						return GameConstants.XFGD_CT_ERROR;
					}
				}

				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (i == _out_card_player) {
						continue;
					}
					for (int j = 0; j < table.GRR._card_count[i]; j++) {
						if (table._logic.is_zhu_card(table.GRR._cards_data[i][j])) {
							return GameConstants.XFGD_CT_ERROR;
						}
					}

				}
			}
		}

		return cbCardType;
	}

	@Override
	public boolean handler_player_be_in_room(SJTable_YZBP table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(SJMsgConstants.RESPONSE_YZBP_RECONNECT_DATA);
		// 发送数据
		TableResponse_Yzbp.Builder tableResponse = TableResponse_Yzbp.newBuilder();
		tableResponse.setRoomInfo(table.getRoomInfo());
		table.load_player_info_data_reconnect(tableResponse);
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(table._current_player);
		tableResponse.setPrOutCardPlayer(table._out_card_player);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder out_cards_card = Int32ArrayResponse.newBuilder();
			if (i == seat_index) {
				for (int j = 0; j < table.GRR._card_count[i]; j++) {
					cards_card.addItem(table.GRR._cards_data[i][j]);
				}
			}
			for (int j = 0; j < table._cur_out_card_count[i]; j++) {
				out_cards_card.addItem(table._cur_out_card_data[i][j]);
			}
			tableResponse.addOutCardsType(table._cur_out_card_type[i]);
			tableResponse.addOutCardsCount(table._cur_out_card_count[i]);
			tableResponse.addOutCardsData(out_cards_card);
			tableResponse.addCardsData(cards_card);
			tableResponse.addCardCount(table.GRR._card_count[i]);
		}
		for (int i = 0; i < table._turn_out_card_count; i++) {
			tableResponse.addPrCardsData(table._turn_out_card_data[i]);
		}
		if (table._current_player == seat_index) {
			if (table._turn_out_card_count == 0) {
				tableResponse.setIsFirstOut(1);
			} else {
				tableResponse.setIsFirstOut(0);
			}
		} else {
			tableResponse.setIsFirstOut(0);
		}
		tableResponse.setPrOutCardType(table._turn_out_card_type);
		tableResponse.setPrCardsCount(table._turn_out_card_count);
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setMaxCardSeat(table._max_card_seat);
		if (table.GRR._banker_player != GameConstants.INVALID_SEAT) {
			tableResponse.setCallDang(table._select_dang[table.GRR._banker_player]);
		} else {
			tableResponse.setCallDang(0);
		}

		if (table._current_player == seat_index) {
			int can_out_card_data[] = new int[table.get_hand_card_count_max()];
			int can_out_count = table._logic.Player_Can_out_card(table.GRR._cards_data[table._current_player],
					table.GRR._card_count[table._current_player], table._origin_out_card_data,
					table._origin_out_card_count, can_out_card_data, table._extra_out_card_data,
					table._extra_out_card_count, table._is_han_lai_fen[seat_index]);
			for (int i = 0; i < can_out_count; i++) {
				tableResponse.addUserCanOutData(can_out_card_data[i]);
			}
			tableResponse.setUserCanOutCount(can_out_count);
		}
		tableResponse.setZhuType(table._zhu_type);

		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));

		// 自己才有牌数据
		table.send_response_to_player(seat_index, roomResponse);

		table.operate_out_card(_out_card_player, _out_card_count, _out_cards_data, _out_type, seat_index);

		table.RefreshScore(seat_index);
		table.send_history(seat_index);
		table.send_di_pai(seat_index);
		table.send_zhu_pai_type(seat_index, 2);
		table.Refresh_pai_score(seat_index);
		if (table._pai_score_count > 0) {
			table.Send_get_score_effect(table._pai_score_card[table._pai_score_count - 1], table._table_score, 0,
					seat_index);
		}

		if (seat_index != table.GRR._banker_player && table._is_cam_han_lai_fen[seat_index]
				&& !table._is_han_lai_fen[seat_index] && table._cur_out_card_count[seat_index] == 0) {
			table.send_han_lai_score(seat_index, 1, seat_index);
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._is_han_lai_fen[i] && table._cur_out_card_count[i] == 0) {
				table.send_han_lai_score(i, 2, seat_index);
			}
		}

		return true;
	}

}
