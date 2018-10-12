package com.cai.game.shengji.handler.wzdd;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.PBUtil;
import com.cai.domain.SheduleArgs;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.shengji.handler.SJHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.wzdd.wzddRsp.CallBankerResponse_Wzdd;
import protobuf.clazz.wzdd.wzddRsp.TableResponse_Wzdd;

public class SJHandlerOutCardOperate_WZDD extends SJHandlerOutCardOperate<SJTable_WZDD> {

	@Override
	public void exe(SJTable_WZDD table) {
		if (_out_card_player != table._current_player) {
			return;
		}

		// 玩家不出
		if (_out_type == 0) {
			return;
		}
		int cbCardType = adjust_out_card_right(table);
		if (cbCardType == GameConstants.XFGD_CT_ERROR) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型");
			return;
		}

		for (int i = 0; i < _out_card_count; i++) {
			if (_out_cards_data[i] % 0x1000 > 0x100) {
				_out_cards_data[i] -= 0x100;
			}
		}
		table._table_score += table._logic.GetCardScore(_out_cards_data, _out_card_count);
		if (table._max_card_seat == GameConstants.INVALID_SEAT) {
			table._max_card_seat = _out_card_player;
			// 保存最大出牌玩家牌型数据
			for (int i = 0; i < table._turn_out_card_count; i++) {
				table._turn_out_card_data[i] = 0;

			}
			table._turn_out_card_count = _out_card_count;
			table._origin_out_card_count = _out_card_count;
			table._turn_out_card_type = cbCardType;
			for (int i = 0; i < table._turn_out_card_count; i++) {
				table._turn_out_card_data[i] = _out_cards_data[i];
				table._origin_out_card_data[i] = _out_cards_data[i];
			}
			table._kill_ed = GameConstants.INVALID_SEAT;
			table._kill_ing = GameConstants.INVALID_SEAT;
			if (table._logic.GetCardColor(_out_cards_data[0]) == table._zhu_type) {
				table.send_effect_type(table._max_card_seat, table.EFFECT_DIAO_ZHU, _out_cards_data, _out_card_count,
						cbCardType, GameConstants.INVALID_SEAT);
			}
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
				table._max_card_seat = _out_card_player;
				// 保存最大出牌玩家牌型数据
				for (int i = 0; i < table._turn_out_card_count; i++) {
					table._turn_out_card_data[i] = 0;
				}
				table._turn_out_card_count = _out_card_count;
				for (int i = 0; i < table._turn_out_card_count; i++) {
					table._turn_out_card_data[i] = _out_cards_data[i];
				}
			} else {
				table._kill_ed = GameConstants.INVALID_SEAT;
				table._kill_ing = GameConstants.INVALID_SEAT;
			}
		}

		for (int i = 0; i < _out_card_count; i++) {
			if (table._logic.GetCardValue(_out_cards_data[i]) == 10) {
				table._get_score_card[table._get_score_count++] = _out_cards_data[i];
			} else if (table._logic.GetCardValue(_out_cards_data[i]) == 11
					|| table._logic.GetCardValue(_out_cards_data[i]) == 12
					|| table._logic.GetCardValue(_out_cards_data[i]) == 13) {
				table._get_score_card[table._get_score_count++] = _out_cards_data[i];
			} else if (table._logic.GetCardValue(_out_cards_data[i]) == 1) {
				table._get_score_card[table._get_score_count++] = _out_cards_data[i];
			} else if (table._logic.GetCardValue(_out_cards_data[i]) == 14
					|| table._logic.GetCardValue(_out_cards_data[i]) == 15) {
				table._get_score_card[table._get_score_count++] = _out_cards_data[i];
			}
		}
		table._logic.SortCardList(table._get_score_card, table._get_score_count);
		table._logic.SortCardList(table._history_out_card[_out_card_player][0],
				table._history_out_count[_out_card_player][0]);
		// 下一个玩家
		table._current_player = (_out_card_player + 1) % table.getTablePlayerNumber();
		table.GRR._card_count[_out_card_player] -= _out_card_count;
		table._out_card_player = _out_card_player;
		table._history_out_count[_out_card_player][1] = _out_card_count;
		// 保存当前玩家本轮出牌数据
		Arrays.fill(table._cur_out_card_data[_out_card_player], 0);
		for (int i = 0; i < _out_card_count; i++) {
			table._cur_out_card_data[_out_card_player][i] = _out_cards_data[i];
			table._history_out_card[_out_card_player][1][i] = _out_cards_data[i];

		}
		table._cur_out_card_type[_out_card_player] = cbCardType;
		table._cur_out_card_count[_out_card_player] = _out_card_count;

		if (table._cur_out_card_count[table._current_player] != 0) {
			table._current_player = table._max_card_seat;
			table._get_score[table._max_card_seat] += table._table_score;
			if (table._table_score > 0 && table._max_card_seat == table.GRR._banker_player) {
				table.send_effect_type(table._max_card_seat, table.EFFECT_GET_SCORE, _out_cards_data, _out_card_count,
						cbCardType, GameConstants.INVALID_SEAT);
			}
			if (table.GRR._card_count[table._current_player] == 0) {
				int di_score = table._logic.GetCardScore(table._di_pai, table._di_pai_count);
				if (table._max_card_seat != table.GRR._banker_player) {
					di_score *= 2;
					table._get_score[table._max_card_seat] += di_score;
				} else {
					table._get_score[table._max_card_seat] += di_score;
				}
			}
			table._table_score = 0;
			table._max_card_seat = GameConstants.INVALID_SEAT;

			// 保留上轮数据
			for (int index = 0; index < table.getTablePlayerNumber(); index++) {
				table._history_out_count[index][0] = 0;
				for (int i = 0; i < table._history_out_count[index][1]; i++) {
					table._history_out_card[index][0][table._history_out_count[index][0]++] = table._history_out_card[index][1][i];
				}
			}

			for (int i = 0; i < table._turn_out_card_count; i++) {
				table._turn_out_card_data[i] = GameConstants.INVALID_CARD;
			}
			for (int i = 0; i < table._origin_out_card_count; i++) {
				table._origin_out_card_data[i] = GameConstants.INVALID_CARD;
			}
			table._turn_out_card_count = 0;
			table._origin_out_card_count = 0;
			table._turn_out_card_type = GameConstants.WZDD_CT_ERROR;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				Arrays.fill(table._cur_out_card_data[i], 0);
				table._cur_out_card_type[i] = GameConstants.WZDD_CT_ERROR;
				table._cur_out_card_count[i] = 0;
			}

			if (table.GRR._card_count[table._current_player] == 0) {
				int delay = 3;
				GameSchedule.put(
						new GameFinishRunnable(table.getRoom_id(), _out_card_player, GameConstants.Game_End_NORMAL),
						delay, TimeUnit.SECONDS);
				table._current_player = GameConstants.INVALID_SEAT;
			}

			table.send_history(GameConstants.INVALID_SEAT);
		}

		table._out_card_times[_out_card_player]++;
		table.RefreshScore(GameConstants.INVALID_SEAT);
		if (table._turn_out_card_count == 0) {

			if (table._current_player != GameConstants.INVALID_SEAT) {
				table._turn_out_card_count = this._out_card_count;
				SheduleArgs args = SheduleArgs.newArgs();
				args.set("seat_index", table._current_player);
				table.schedule(table.ID_TIMER_OUT_CARD_ROUND, args, 1000);
				table._current_player = GameConstants.INVALID_SEAT;
			}

		}
		table.operate_out_card(_out_card_player, _out_card_count, _out_cards_data, cbCardType,
				GameConstants.INVALID_SEAT);

		table._turn_out_shuai_pai_count = 0;

		if (table._kill_ed == GameConstants.INVALID_SEAT) {
			SheduleArgs nimal_one = SheduleArgs.newArgs();
			nimal_one.set("seat_index", _out_card_player);
			nimal_one.set("to_player", GameConstants.INVALID_SEAT);
			for (int i = 0; i < _out_card_count; i++) {
				nimal_one.set("card_data_" + i, _out_cards_data[i]);
			}
			nimal_one.set("card_count", _out_card_count);
			nimal_one.set("card_type", cbCardType);
			nimal_one.set("effect_type", 0);
			table.schedule(table.ID_TIMER_ANIMAL_DELAY, nimal_one, 500);

		} else {
			SheduleArgs nimal_one = SheduleArgs.newArgs();
			nimal_one.set("seat_index", _out_card_player);
			nimal_one.set("to_player", GameConstants.INVALID_SEAT);
			for (int i = 0; i < _out_card_count; i++) {
				nimal_one.set("card_data_" + i, _out_cards_data[i]);
			}
			nimal_one.set("card_count", _out_card_count);
			nimal_one.set("card_type", cbCardType);
			nimal_one.set("effect_type", table.EFFECT_SHA);
			table.schedule(table.ID_TIMER_ANIMAL_DELAY, nimal_one, 500);
		}

	}

	public int adjust_out_card_right(SJTable_WZDD table) {

		int cbCardType = table._logic.GetCardType(_out_cards_data, _out_card_count);

		if (table._origin_out_card_count == 0) {
			if (cbCardType == GameConstants.WZDD_CT_ERROR) {
				return cbCardType;
			}
		}

		if (table._origin_out_card_count != 0) {
			if (!table._logic.is_he_li(table._origin_out_card_data, table._origin_out_card_count, _out_cards_data,
					_out_card_count, table.GRR._cards_data[_out_card_player],
					table.GRR._card_count[_out_card_player])) {
				return GameConstants.WZDD_CT_ERROR;
			}
			if (cbCardType == GameConstants.XFGD_CT_ERROR) {
				cbCardType = GameConstants.WZDD_CT_DIAN;
			}
		}

		if (!table._logic.remove_cards_by_data(table.GRR._cards_data[_out_card_player],
				table.GRR._card_count[_out_card_player], _out_cards_data, _out_card_count)) {
			return GameConstants.WZDD_CT_ERROR;
		}
		return cbCardType;
	}

	@Override
	public boolean handler_player_be_in_room(SJTable_WZDD table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WZDD_RECONNECT_DATA);
		// 发送数据
		TableResponse_Wzdd.Builder tableResponse = TableResponse_Wzdd.newBuilder();
		tableResponse.setRoomInfo(table.getRoomInfo());
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		if (table._game_status != GameConstants.GS_WZDD_PLAY) {
			tableResponse.setCurrentPlayer(GameConstants.INVALID_SEAT);
		} else {
			tableResponse.setCurrentPlayer(table._current_player);
		}
		tableResponse.setPrOutCardPlayer(table._out_card_player);
		table.load_player_info_data_reconnect(tableResponse);
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
			tableResponse.addSelectDang(-1);
		}
		for (int i = 0; i < table._turn_out_card_count; i++) {
			tableResponse.addPrCardsData(table._origin_out_card_data[i]);
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
		tableResponse.setPrCardsCount(table._origin_out_card_count);
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
					table._origin_out_card_count, can_out_card_data);
			for (int i = 0; i < can_out_count; i++) {
				tableResponse.addUserCanOutData(can_out_card_data[i]);
			}
			tableResponse.setUserCanOutCount(can_out_count);
		}
		tableResponse.setZhuType(table._zhu_type);

		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));

		// 自己才有牌数据
		table.send_response_to_player(seat_index, roomResponse);
		table.send_zhu_pai_type(seat_index);
		if (table._game_status == GameConstants.GS_WZDD_PLAY) {
			table.send_zhu_count(seat_index);
		}
		if (table._game_status == GameConstants.GS_WZDD_CALL_BANKER) {
			roomResponse.setType(MsgConstants.RESPONSE_WZDD_CALL_BANKER);
			roomResponse.setRoomInfo(table.getRoomInfo());
			// 发送数据
			CallBankerResponse_Wzdd.Builder callbanker_response = CallBankerResponse_Wzdd.newBuilder();
			callbanker_response.setRoomInfo(table.getRoomInfo());
			callbanker_response.setCallPlayer(GameConstants.INVALID_SEAT);
			callbanker_response.setBankerPlayer(table.GRR._banker_player);
			callbanker_response.setCurrentPlayer(table._current_player);
			callbanker_response.setDangMin(table._min_dang);
			callbanker_response.setDangMax(table._max_dang);
			callbanker_response.setDisplayTime(10);
			roomResponse.setCommResponse(PBUtil.toByteString(callbanker_response));

			table.send_response_to_player(seat_index, roomResponse);
		}
		if (table._game_status == GameConstants.GS_WZDD_MAI_DI) {
			table.send_mai_di_begin(seat_index);
		}
		if (table._game_status == GameConstants.GS_WZDD_DING_ZHU) {
			table.send_ding_zhu_begin(seat_index);
		}

		table.RefreshScore(seat_index);
		table.send_history(seat_index);

		return true;
	}

}
