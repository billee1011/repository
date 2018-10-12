package com.cai.game.shengji.handler.yz240;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.PBUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.shengji.SJConstants;
import com.cai.game.shengji.handler.SJHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Xpsj.XpsjRsp.TableResponse_Xpsj;
import protobuf.clazz.yzsj.yzsjRsp.TableResponse_yzsj;

public class SJHandlerOutCardOperate_YZ_240 extends SJHandlerOutCardOperate<SJTable_YZ_240> {

	@Override
	public void exe(SJTable_YZ_240 table) {
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
		if (cbCardType == SJConstants.XP_SJ_CT_ERROR) {
			return;
		}
		// 桌面牌分
		table_pai_socre(table);
		table.Refresh_pai_score(GameConstants.INVALID_SEAT);
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

			if (_out_card_count == 1 && table._logic.GetCardValue(_out_cards_data[0]) == 5
					&& table._logic.GetCardColor(_out_cards_data[0]) != table._zhu_type
					&& table.has_rule(GameConstants.GAME_RULE_XPSJ_HAVE_5_A)) {
				table._have_5_must_A = true;
			}
		} else {
			if (table._logic.comparecarddata(table._turn_out_card_data, table._turn_out_card_count, _out_cards_data,
					_out_card_count)) {

				if (_out_cards_data[0] > table._zhu_value) {
					if (table._turn_out_card_data[0] > table._zhu_value
							&& table._origin_out_card_data[0] < table._zhu_value) {
						effect_type = table.EFFECT_CALL_GAI_BI;
					} else if (table._turn_out_card_data[0] < table._zhu_value) {
						effect_type = table.EFFECT_CALL_BI_PAI;
					}
				}
				target_seat = table._max_card_seat;
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
				if (_out_card_count == 1 && table._logic.GetCardValue(_out_cards_data[0]) == 1) {
					table._have_5_must_A = false;
				} else if (_out_card_count == 1 && _out_cards_data[0] > table._zhu_value) {
					table._have_5_must_A = false;
				}
			}
		}
		for (int i = 0; i < table._history_out_count[_out_card_player][1]; i++) {
			table._history_out_card[_out_card_player][0][table._history_out_count[_out_card_player][0]++] = table._history_out_card[_out_card_player][1][i];
		}
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

			if (table.GRR._card_count[table._current_player] == 0) {
				int di_score = table._logic.GetCardScore(table._di_pai, table._di_pai_count);

			}
			table._table_score = 0;
			table._have_5_must_A = false;
			table._max_card_seat = GameConstants.INVALID_SEAT;

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
				table._cur_out_card_type[i] = 0;
				table._cur_out_card_count[i] = 0;
			}

			int xian_get_score = 0;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i != table.GRR._banker_player) {
					xian_get_score += table._get_score[i];
				}
			}
			if (table.GRR._card_count[table._current_player] == 0) {
				int delay = 3;
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._current_player,
						GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
				table._current_player = GameConstants.INVALID_SEAT;
			}

			table.RefreshScore(GameConstants.INVALID_SEAT, 1);
		} else {
			table.RefreshScore(GameConstants.INVALID_SEAT, 0);
		}
		for (int i = 0; i < _out_card_count; i++) {
			if (table._logic.GetCardValue(_out_cards_data[i]) == 5) {
				table._get_score_card[table._get_score_count++] = _out_cards_data[i] % 0x100;
				table._user_get_score_card[_out_card_player][table._user_get_score_count[_out_card_player]++] = _out_cards_data[i];
			} else if (table._logic.GetCardValue(_out_cards_data[i]) == 10
					|| table._logic.GetCardValue(_out_cards_data[i]) == 13) {
				table._get_score_card[table._get_score_count++] = _out_cards_data[i] % 0x100;
				table._user_get_score_card[_out_card_player][table._user_get_score_count[_out_card_player]++] = _out_cards_data[i];
			} else if (table._logic.GetCardValue(_out_cards_data[i]) == 14
					|| table._logic.GetCardValue(_out_cards_data[i]) == 15) {
				table._get_score_card[table._get_score_count++] = _out_cards_data[i] % 0x100;
				table._user_get_score_card[_out_card_player][table._user_get_score_count[_out_card_player]++] = _out_cards_data[i];
			}
		}
		table._logic.SortCardList(table._get_score_card, table._get_score_count);
		table._out_card_times[_out_card_player]++;

		table.send_history(GameConstants.INVALID_SEAT);
		
		//顯示出牌
		table.operate_out_card(_out_card_player, _out_card_count, _out_cards_data, cbCardType,GameConstants.INVALID_SEAT);
		
		if (effect_type != 0) {
			table.send_effect_type(_out_card_player, effect_type, target_seat, GameConstants.INVALID_SEAT);
		}
		if (table._current_player != GameConstants.INVALID_SEAT) {
			// if (table.GRR._card_count[_out_card_player] == 0) {
			// table._handler_out_card_operate.reset_status(table._current_player,
			// table.GRR._cards_data[table._current_player],
			// table.GRR._card_count[table._current_player],
			// GameConstants.XFGD_CT_DOUBLE);
			// table._handler_out_card_operate.exe(table);
			// }
		} else {
			table.send_di_pai_data(GameConstants.INVALID_SEAT);
		}

		for (int i = 0; i < _out_card_count; i++) {
			// 判断另一工头
			if (_out_cards_data[i] == table._call_baker_data[0] && _out_card_player != table.GRR._banker_player) {
				table._other_banker = _out_card_player;
			}

		}

		if (table._current_player != GameConstants.INVALID_SEAT) {
			if (table.GRR._card_count[_out_card_player] == 0 && this._out_card_count == 1) {
				table._handler_out_card_operate.reset_status(table._current_player,
						table.GRR._cards_data[table._current_player], table.GRR._card_count[table._current_player],
						GameConstants.XFGD_CT_DOUBLE);
				table._handler_out_card_operate.exe(table);
			}
		}
	}

	public int adjust_out_card_right(SJTable_YZ_240 table) {

		int cbCardType = table._logic.GetCardType(_out_cards_data, _out_card_count);

		if (table._origin_out_card_count == 0) {
			// 反主第一次出牌必须把叫的牌打出去
			if (table._out_card_times[_out_card_player] == 0 && _out_card_player == table.GRR._banker_player
					&& table._call_banker_card_count > 1) {
				/*if (table.has_rule(GameConstants.GAME_RULE_XPSJ_NO_ZHU_DA_CHANG_ZHU) && table._zhu_type == 4) {
					for (int i = 0; i < this._out_card_count; i++) {
						if (table._logic.GetCardValue(_out_cards_data[i]) != table._logic._chang_zhu_one
								&& table._logic.GetCardValue(_out_cards_data[i]) != table._logic._chang_zhu_two
								&& table._logic.GetCardValue(_out_cards_data[i]) != table._logic._chang_zhu_three
								&& table._logic.GetCardValue(_out_cards_data[i]) != table._logic._chang_zhu_four) {
							cbCardType = SJConstants.XP_SJ_CT_ERROR;
							table.send_error_notify(_out_card_player, 2, "无主先打常主");
						}
					}
				} else */{
					int count = 0;
					for (int j = 0; j < _out_card_count; j++) {
						if (_out_cards_data[j] == table._call_baker_data[0] + table._zhu_value) {
							count++;
						}
					}
					if (count < table._call_banker_card_count) {
						cbCardType = SJConstants.XP_SJ_CT_ERROR;
						table.send_error_notify(_out_card_player, 2, "反主必须先出叫牌");

						return cbCardType;
					}
				}
			}
			if (cbCardType == SJConstants.XP_SJ_CT_ERROR) {
				table.send_error_notify(_out_card_player, 2, "请选择正确的牌型");
				return cbCardType;
			}
		} else {
			if (table._origin_out_card_count != _out_card_count) {
				table.send_error_notify(_out_card_player, 2, "请选择正确的牌型");
				return SJConstants.XP_SJ_CT_ERROR;
			}
			if (!table._logic.is_he_li(table._origin_out_card_data, table._origin_out_card_count,
					table._turn_out_card_data, table._turn_out_card_count, _out_cards_data, _out_card_count,
					table.GRR._cards_data[_out_card_player], table.GRR._card_count[_out_card_player],
					table._table_score, table._have_5_must_A, false)) {
				table.send_error_notify(_out_card_player, 2, "请选择正确的牌型");
				return SJConstants.XP_SJ_CT_ERROR;
			}
			if (cbCardType == SJConstants.XP_SJ_CT_ERROR) {
				cbCardType = SJConstants.XP_SJ_CT_DIAN;
			}
		}

		if (!table._logic.remove_cards_by_data(table.GRR._cards_data[_out_card_player],
				table.GRR._card_count[_out_card_player], _out_cards_data, _out_card_count)) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型");
			return SJConstants.XP_SJ_CT_ERROR;
		}

		return cbCardType;
	}

	public void table_pai_socre(SJTable_YZ_240 table) {
		int pai_score = 0;
		int remove_card[] = new int[table.get_hand_card_count_max()];
		int remove_count = 0;
		for (int i = 0; i < _out_card_count; i++) {
			int value = table._logic.GetCardValue(_out_cards_data[i]);
			if (value == 5 || value == 10 || value == 13 || value == 14 || value == 15) {
				remove_card[remove_count++] = _out_cards_data[i];
			}
			if (value == 5) {
				pai_score += 5;
			} else if (value == 10 || value == 13 ) {
				pai_score += 10;
			}else if(value == 14 || value == 15){
				if(table.has_rule(GameConstants.GAME_RULE_YZSJ_300)){
					if(value == 14){
						pai_score += 20;
					}else if(value == 15){
						pai_score += 30;
					}
				}else{
					pai_score += 10;
				}
			}
		}
		for(int i = 0;i < remove_count;i++){
			if(remove_card[i] > 0x1000){
				remove_card[i] -= 0x1000;
			}
		}
		if (!table._logic.remove_cards_by_data(table._pai_score_card, table._pai_score_count, remove_card,
				remove_count)) {
			// table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			// return;
		}
		table._pai_score_count -= remove_count;
		table._pai_score -= pai_score;
	}

	@Override
	public boolean handler_player_be_in_room(SJTable_YZ_240 table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(SJConstants.RESPONSE_XPSJ_RECONNECT_DATA);
		roomResponse.setGameStatus(table._game_status);
		// 发送数据
		TableResponse_yzsj.Builder tableResponse = TableResponse_yzsj.newBuilder();
		tableResponse.setRoomInfo(table.getRoomInfo());
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(table._current_player);
		tableResponse.setPrOutCardPlayer(table._out_card_player);
		table.load_player_info_data_reconnect(tableResponse);
		if (table.has_rule(GameConstants.GAME_RULE_XPSJ_HAVE_SCORE_BI_GUAN) && table._table_score > 0) {
			tableResponse.setIsScoreMust(true);
		} else {
			tableResponse.setIsScoreMust(false);
		}
		tableResponse.setIsMustA(table._have_5_must_A);
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
			tableResponse.addPrCardsData(table._origin_out_card_data[i]);
			tableResponse.addTurnCardsData(table._turn_out_card_data[i]);
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

		if (table._current_player == seat_index) {
			int can_out_card_data[] = new int[table.get_hand_card_count_max()];
			int can_out_count = 0;
			if (table._out_card_times[seat_index] == 0 && seat_index == table.GRR._banker_player
					&& table._logic._zhu_type == 4) {
				if (table.has_rule(GameConstants.GAME_RULE_XPSJ_NO_ZHU_DA_CHANG_ZHU)) {
					for (int i = 0; i < table.GRR._card_count[table._current_player]; i++) {
						if (table._logic.GetCardValue(
								table.GRR._cards_data[table._current_player][i]) == table._logic._chang_zhu_three
								|| table._logic.GetCardValue(
										table.GRR._cards_data[table._current_player][i]) == table._logic._chang_zhu_four
								|| table._logic.GetCardValue(
										table.GRR._cards_data[table._current_player][i]) == table._logic._chang_zhu_one
								|| table._logic.GetCardValue(
										table.GRR._cards_data[table._current_player][i]) == table._logic._chang_zhu_two) {
							can_out_card_data[can_out_count++] = table.GRR._cards_data[table._current_player][i];
						}
					}
				} else {
					for (int i = 0; i < table._call_banker_card_count; i++) {
						can_out_card_data[can_out_count++] = table._call_baker_data[i] + 0x1000;
					}
				}

			} else if (table._out_card_times[seat_index] == 0 && seat_index == table.GRR._banker_player
					&& table._call_banker_card_count > 1) {
				for (int i = 0; i < table._call_banker_card_count; i++) {
					can_out_card_data[can_out_count++] = table._call_baker_data[i] + 0x1000;
				}

			} else {
				int must_out_data[] = new int[table.get_hand_card_count_max()];
				int must_out_count[] = new int[1];
				can_out_count = table._logic.Player_Can_out_card(table.GRR._cards_data[table._current_player],
						table.GRR._card_count[table._current_player], table._origin_out_card_data,
						table._origin_out_card_count, can_out_card_data, table._turn_out_card_count,
						table._turn_out_card_data, must_out_data, must_out_count, table._table_score,
						table._have_5_must_A, false);
				//can_out_count = table._logic.player_can_out_card_yz(table.GRR._cards_data[table._current_player],
				//				table.GRR._card_count[table._current_player], table._origin_out_card_data,
				//				table._origin_out_card_count, can_out_card_data, table._turn_out_card_count,
				//				table._turn_out_card_data, must_out_data, must_out_count, table._table_score,
				//				table._have_5_must_A, false);
			}

			for (int i = 0; i < can_out_count; i++) {
				tableResponse.addUserCanOutData(can_out_card_data[i]);
			}
			tableResponse.setUserCanOutCount(can_out_count);
		}
		tableResponse.setZhuType(table._zhu_type);

		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));

		// 自己才有牌数据
		table.send_response_to_player(seat_index, roomResponse);

		if (table._game_status == SJConstants.GS_XPSJ_CALL_BANKER
				|| table._game_status == SJConstants.GS_XPSJ_CALL_BANKER) {
			table.Refresh_Color_count(seat_index);
			if (table._call_banker_card_count > 0) {
				table.send_call_data(table._cur_banker, seat_index);
			}
		}
		if (table._game_status == SJConstants.GS_XPSJ_PLAY) {
			table.RefreshScore(seat_index, 0);
			table.send_history(seat_index);
			table.send_zhu_data(seat_index);
		}
		// table.send_di_pai(seat_index);
		// table.send_zhu_pai_type(seat_index);

		return true;
	}

}
