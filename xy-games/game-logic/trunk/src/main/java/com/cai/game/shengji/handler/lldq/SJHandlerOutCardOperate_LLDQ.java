package com.cai.game.shengji.handler.lldq;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.PBUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.shengji.handler.SJHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.lldq.lldqRsp.MaiDiOpreate_Lldq;
import protobuf.clazz.lldq.lldqRsp.SendCardEnd_Lldq;
import protobuf.clazz.lldq.lldqRsp.TableResponse_Lldq;

public class SJHandlerOutCardOperate_LLDQ extends SJHandlerOutCardOperate<SJTable_LLDQ> {

	@Override
	public void exe(SJTable_LLDQ table) {
		if (_out_card_player != table._current_player) {
			return;
		}

		// 玩家不出
		if (_out_type == 0) {
			return;
		}
		int cbCardType = adjust_out_card_right(table);
		int effect_type = 0;
		if (cbCardType == GameConstants.LLDQ_CT_ERROR) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型");
			return;
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
			if (table._logic.GetCardColor(_out_cards_data[0]) == table._zhu_type
					|| table._logic.GetCardValue(_out_cards_data[0]) == table._logic._chang_zhu_one
					|| table._logic.GetCardValue(_out_cards_data[0]) == table._logic._chang_zhu_two
					|| table._logic.GetCardValue(_out_cards_data[0]) == table._logic._chang_zhu_three
					|| table._logic.GetCardValue(_out_cards_data[0]) == table._logic._chang_zhu_four) {
				effect_type = table.EFFECT_DIAO_ZHU;
			}

		} else {
			if (table._logic.comparecarddata(table._turn_out_card_data, table._turn_out_card_count, _out_cards_data,
					_out_card_count)) {
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

				if (table._logic.GetCardColor(_out_cards_data[0]) == table._zhu_type
						|| table._logic.GetCardValue(_out_cards_data[0]) == table._logic._chang_zhu_one
						|| table._logic.GetCardValue(_out_cards_data[0]) == table._logic._chang_zhu_two
						|| table._logic.GetCardValue(_out_cards_data[0]) == table._logic._chang_zhu_three
						|| table._logic.GetCardValue(_out_cards_data[0]) == table._logic._chang_zhu_four) {
					if (table._logic.GetCardColor(table._origin_out_card_data[0]) == table._zhu_type
							|| table._logic.GetCardValue(table._origin_out_card_data[0]) == table._logic._chang_zhu_one
							|| table._logic.GetCardValue(table._origin_out_card_data[0]) == table._logic._chang_zhu_two
							|| table._logic
									.GetCardValue(table._origin_out_card_data[0]) == table._logic._chang_zhu_three
							|| table._logic
									.GetCardValue(table._origin_out_card_data[0]) == table._logic._chang_zhu_four) {
						effect_type = table.EFFECT_DA_ZHU;
					} else {
						effect_type = table.EFFECT_BI_PAI;
					}
				} else {
					effect_type = table.EFFECT_DA_ZHU;
				}
			} else {
				for (int i = 0; i < _out_card_count; i++) {
					if (table._logic.GetCardColor(_out_cards_data[i]) != table._logic
							.GetCardColor(table._origin_out_card_data[0])
							&& table._logic.GetCardColor(_out_cards_data[i]) != table._zhu_type
							&& table._logic.GetCardValue(_out_cards_data[i]) != table._logic._chang_zhu_one
							&& table._logic.GetCardValue(_out_cards_data[i]) != table._logic._chang_zhu_two
							&& table._logic.GetCardValue(_out_cards_data[i]) != table._logic._chang_zhu_three
							&& table._logic.GetCardValue(_out_cards_data[i]) != table._logic._chang_zhu_four) {
						table.send_effect_type(_out_card_player, table.EFFECT_DIAN_PAI, 0, GameConstants.INVALID_SEAT);
						effect_type = table.EFFECT_DIAN_PAI;
						break;
					}
				}
			}
		}
		// 下一个玩家
		table._current_player = (_out_card_player + 1) % table.getTablePlayerNumber();
		table.GRR._card_count[_out_card_player] -= _out_card_count;
		table._out_card_player = _out_card_player;
		table._history_out_count[_out_card_player][table._out_card_times[_out_card_player]] = _out_card_count;
		// 保存当前玩家本轮出牌数据
		Arrays.fill(table._cur_out_card_data[_out_card_player], 0);
		for (int i = 0; i < _out_card_count; i++) {
			table._cur_out_card_data[_out_card_player][i] = _out_cards_data[i];
			table._history_out_card[_out_card_player][table._out_card_times[_out_card_player]][i] = _out_cards_data[i];

		}
		table._cur_out_card_type[_out_card_player] = effect_type;
		table._cur_out_card_count[_out_card_player] = _out_card_count;

		// 判断出牌玩家是否工友
		for (int i = 0; i < _out_card_count; i++) {
			// 判断另一工头
			if (table._logic.GetCardColor(_out_cards_data[i]) == table._logic.GetCardColor(table._call_baker_data[0])
					&& table._logic.GetCardValue(_out_cards_data[i]) == table._logic
							.GetCardValue(table._call_baker_data[0])
					&& _out_card_player != table.GRR._banker_player) {
				table._other_banker = _out_card_player;
				table.send_effect_type(_out_card_player, table.EFFECT_GONG_YOU, 0, GameConstants.INVALID_SEAT);
				table._get_score[_out_card_player] = -1;
				break;
			}

		}

		if (table._cur_out_card_count[table._current_player] != 0) {
			table._current_player = table._max_card_seat;

			if (table._table_score > 0 && table._max_card_seat != table.GRR._banker_player
					&& table._max_card_seat != table._other_banker) {
				// 飘分动画工头和工友不飘
				table._get_score[table._max_card_seat] += table._table_score;
				table.send_effect_type(table._max_card_seat, table.EFFECT_SCORE_FEI, table._table_score,
						GameConstants.INVALID_SEAT);
			}

			if (table.GRR._card_count[table._current_player] == 0) {
				// 底牌分数
				int di_score = table._logic.GetCardScore(table._di_pai, table._di_pai_count);
				if (table._max_card_seat != table.GRR._banker_player && table._max_card_seat != table._other_banker) {
					table._kou_di_score = di_score;
					if (_out_card_count == 1) {
						di_score *= 2;
						table._kou_di_fan_bei = 2;
					} else if (_out_card_count == 2) {
						di_score *= 4;
						table._kou_di_fan_bei = 4;
					} else {
						di_score *= 8 + ((_out_card_count / 2) - 2) * 4;
						table._kou_di_fan_bei = 8 + ((_out_card_count / 2) - 2) * 4;
					}
				} else {
					table._kou_di_fan_bei = 0;
					table._kou_di_score = 0;
				}
				if (table.has_rule(GameConstants.GAME_RULE_LLDQ_KOU_DI_JIA_JI)
						&& table._max_card_seat != table.GRR._banker_player
						&& table._max_card_seat != table._other_banker) {
					if (_out_card_count == 1) {
						table._kou_di_jia_ji = 1;
					} else if (_out_card_count == 2) {
						table._kou_di_jia_ji = 2;
					} else {
						table._kou_di_jia_ji = 3;
					}
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
				if (table.game_end_type()) {
					GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._current_player,
							GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
				}

				table._current_player = GameConstants.INVALID_SEAT;
			}

			if (table._table_score > 0 && table._max_card_seat != table.GRR._banker_player
					&& table._max_card_seat != table._other_banker) {
				table._table_score = 0;
				table.RefreshScore(GameConstants.INVALID_SEAT, 1);
			} else {
				table._table_score = 0;
				table.RefreshScore(GameConstants.INVALID_SEAT, 0);
			}
			table._max_card_seat = GameConstants.INVALID_SEAT;
		} else {
			table.RefreshScore(GameConstants.INVALID_SEAT, 0);
		}

		table._out_card_times[_out_card_player]++;

		if (table.has_rule(GameConstants.GAME_RULE_XFGD_TOU_XIANG)) {
			int xian_get_score = 0;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i != table.GRR._banker_player) {
					xian_get_score += table._get_score[i];
				}
			}
		}
		table.operate_out_card(_out_card_player, _out_card_count, _out_cards_data, cbCardType,
				GameConstants.INVALID_SEAT);
		table.send_effect_type(_out_card_player, effect_type, 0, GameConstants.INVALID_SEAT);
		table.send_sound_type(_out_card_player, table.SOUND_OUT_CARD, cbCardType, _out_card_count, _out_cards_data,
				effect_type, GameConstants.INVALID_SEAT);
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

	}

	public int adjust_out_card_right(SJTable_LLDQ table) {

		int cbCardType = table._logic.GetCardType(_out_cards_data, _out_card_count);

		if (table._origin_out_card_count == 0) {
			if (cbCardType == GameConstants.LLDQ_CT_ERROR) {
				return cbCardType;
			}
		} else {
			if (!table._logic.is_he_li(table._origin_out_card_data, table._origin_out_card_count, _out_cards_data,
					_out_card_count, table.GRR._cards_data[_out_card_player],
					table.GRR._card_count[_out_card_player])) {
				return GameConstants.LLDQ_CT_ERROR;
			}
			if (cbCardType == GameConstants.LLDQ_CT_ERROR) {
				cbCardType = GameConstants.LLDQ_CT_DIAN;
			}
		}

		if (!table._logic.remove_cards_by_data(table.GRR._cards_data[_out_card_player],
				table.GRR._card_count[_out_card_player], _out_cards_data, _out_card_count)) {
			return GameConstants.LLDQ_CT_ERROR;
		}

		return cbCardType;
	}

	@Override
	public boolean handler_player_be_in_room(SJTable_LLDQ table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_LLDQ_RECONNECT_DATA);
		roomResponse.setGameStatus(table._game_status);
		// 发送数据
		TableResponse_Lldq.Builder tableResponse = TableResponse_Lldq.newBuilder();
		tableResponse.setRoomInfo(table.getRoomInfo());
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
		for (int i = 0; i < table._call_banker_card_count; i++) {
			tableResponse.addSelectCard(table._call_baker_data[i]);
		}
		if (table._game_type == GameConstants.GS_LLDQ_PLAY || table._game_type == GameConstants.GS_LLDQ_MAI_DI
				|| table._game_type == GameConstants.GS_LLDQ_MAI_DI_WAIT) {
			if (table.is_select_fan_pai) {
				tableResponse.setSelectCardStatus(3);
			} else {
				tableResponse.setSelectCardStatus(2);
			}
		} else if (table._game_type == GameConstants.GS_LLDQ_SEND_CARD) {
			if (table._call_banker_card_count > 0) {
				tableResponse.setSelectCardStatus(1);
			} else {
				tableResponse.setSelectCardStatus(0);
			}
		} else {
			tableResponse.setSelectCardStatus(0);
		}
		tableResponse.setSelectSeatIndex(table._cur_banker);
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
		if (table._game_type == GameConstants.GS_LLDQ_PLAY) {
			tableResponse.setBankerPlayer(table.GRR._banker_player);
		} else {
			tableResponse.setBankerPlayer(GameConstants.INVALID_SEAT);
		}

		tableResponse.setMaxCardSeat(table._max_card_seat);
		tableResponse.setOtherBanker(table._other_banker);

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
		if (!table.has_rule(GameConstants.GAME_RULE_LLDQ_FAN_PAI_LIANG_ZHU)
				|| table._fan_pai_card_index == GameConstants.INVALID_CARD
				|| table._game_type != GameConstants.GS_LLDQ_SEND_CARD
				|| (table._game_type == GameConstants.GS_LLDQ_SEND_CARD
						&& table._fan_pai_card_index >= table._all_card_len - table.GRR._left_card_count)) {
			tableResponse.setFanPaiCard(GameConstants.INVALID_CARD);
		} else {
			tableResponse.setFanPaiCard(table._repertory_card[table._fan_pai_card_index]);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));

		// 自己才有牌数据
		table.send_response_to_player(seat_index, roomResponse);

		if (table._game_type == GameConstants.GS_LLDQ_PLAY) {
			table.RefreshScore(seat_index, 0);
		}

		if (table.has_rule(GameConstants.GAME_RULE_LLDQ_FAN_PAI_LIANG_ZHU)) {
			if (table._fan_pai_card_index >= table._all_card_len - table.GRR._left_card_count) {
				if (table._game_type == GameConstants.GS_LLDQ_CALL_BANKER) {
					table.send_effect_type(table._fan_pai_seat_index, table.EFFECT_FAN_PAI_WHITE,
							table._repertory_card[table._fan_pai_card_index], GameConstants.INVALID_SEAT);
					table.Refresh_Color_count(seat_index, true);
				} else if (table._game_type == GameConstants.GS_LLDQ_SEND_CARD) {
					table.send_effect_type(table._fan_pai_seat_index, table.EFFECT_FAN_PAI_RED,
							table._repertory_card[table._fan_pai_card_index], GameConstants.INVALID_SEAT);
					table.Refresh_Color_count(seat_index, false);
				}

			} else {
				if (table._game_type == GameConstants.GS_LLDQ_SEND_CARD) {
					table.Refresh_Color_count(seat_index, false);
				}

			}
		} else {
			if (table._game_type == GameConstants.GS_LLDQ_SEND_CARD) {
				table.Refresh_Color_count(seat_index, false);
			}
		}

		if (seat_index == table.GRR._banker_player) {
			if (table._game_type == GameConstants.GS_LLDQ_MAI_DI_WAIT) {
				roomResponse.setType(MsgConstants.RESPONSE_LLDQ_SEND_CARD_END);
				SendCardEnd_Lldq.Builder send_card_end = SendCardEnd_Lldq.newBuilder();
				send_card_end.setCardCount(8);
				send_card_end.setIsAnimal(0);
				roomResponse.setCommResponse(PBUtil.toByteString(send_card_end));
				table.send_response_to_player(seat_index, roomResponse);
			} else if (table._game_type == GameConstants.GS_LLDQ_MAI_DI) {
				table.RefreshCard(0, seat_index);
			}
		} else {
			if (table.hasShedule(table.ID_TIMER_SEND_CARD_END_WAIT)) {
				roomResponse.setType(MsgConstants.RESPONSE_LLDQ_SEND_CARD_END);
				SendCardEnd_Lldq.Builder send_card_end = SendCardEnd_Lldq.newBuilder();
				send_card_end.setCardCount(8);
				send_card_end.setIsAnimal(0);
				roomResponse.setCommResponse(PBUtil.toByteString(send_card_end));
				table.send_response_to_player(seat_index, roomResponse);
			}
		}

		if (table._game_type == GameConstants.GS_LLDQ_MAI_DI) {
			table.send_effect_type(table.GRR._banker_player, table.EFFECT_SELECT_DI_PAI, 0, seat_index);
		} else if (table._game_type == GameConstants.GS_LLDQ_MAI_DI_WAIT) {
			if (seat_index != table.GRR._banker_player) {
				table.send_effect_type(table.GRR._banker_player, table.EFFECT_SELECT_DI_PAI, 0, seat_index);
			}
		}
		// 显示操作按钮
		if (table._game_type == GameConstants.GS_LLDQ_MAI_DI_WAIT && seat_index == table.GRR._banker_player) {
			roomResponse.setType(MsgConstants.RESPONSE_LLDQ_MAIDI_OPREATE);
			MaiDiOpreate_Lldq.Builder mai_di_opetate = MaiDiOpreate_Lldq.newBuilder();
			mai_di_opetate.addOpreate(1);
			mai_di_opetate.addOpreate(2);
			roomResponse.setCommResponse(PBUtil.toByteString(mai_di_opetate));
			table.send_response_to_player(seat_index, roomResponse);
		} else if (table._game_type == GameConstants.GS_LLDQ_MAI_DI && seat_index == table.GRR._banker_player) {
			if (table._call_banker_card_count == 1) {
				int count = 0;
				for (int i = 0; i < table.GRR._card_count[table.GRR._banker_player]; i++) {
					if (table._logic.GetCardColor(table._call_baker_data[0]) == table._logic
							.GetCardColor(table.GRR._cards_data[table.GRR._banker_player][i])
							&& table._logic.GetCardValue(table._call_baker_data[0]) == table._logic
									.GetCardValue(table.GRR._cards_data[table.GRR._banker_player][i])) {
						count++;
					}
				}
				if (count == 1) {
					roomResponse.setType(MsgConstants.RESPONSE_LLDQ_MAIDI_OPREATE);
					MaiDiOpreate_Lldq.Builder mai_di_opetate = MaiDiOpreate_Lldq.newBuilder();
					mai_di_opetate.addOpreate(3);
					roomResponse.setCommResponse(PBUtil.toByteString(mai_di_opetate));
				} else {
					roomResponse.setType(MsgConstants.RESPONSE_LLDQ_MAIDI_OPREATE);
					MaiDiOpreate_Lldq.Builder mai_di_opetate = MaiDiOpreate_Lldq.newBuilder();
					mai_di_opetate.addOpreate(1);
					mai_di_opetate.addOpreate(3);
					roomResponse.setCommResponse(PBUtil.toByteString(mai_di_opetate));
				}
				table.send_response_to_player(seat_index, roomResponse);
			} else if (table._cur_banker == table._first_call_banker) {
				roomResponse.setType(MsgConstants.RESPONSE_LLDQ_MAIDI_OPREATE);
				MaiDiOpreate_Lldq.Builder mai_di_opetate = MaiDiOpreate_Lldq.newBuilder();
				mai_di_opetate.addOpreate(1);
				mai_di_opetate.addOpreate(3);
				roomResponse.setCommResponse(PBUtil.toByteString(mai_di_opetate));
				table.send_response_to_player(seat_index, roomResponse);
			} else {
				roomResponse.setType(MsgConstants.RESPONSE_LLDQ_MAIDI_OPREATE);
				MaiDiOpreate_Lldq.Builder mai_di_opetate = MaiDiOpreate_Lldq.newBuilder();
				mai_di_opetate.addOpreate(3);
				roomResponse.setCommResponse(PBUtil.toByteString(mai_di_opetate));
				table.send_response_to_player(seat_index, roomResponse);
			}

		}

		if (table._game_type == GameConstants.GS_LLDQ_CALL_BANKER && seat_index == table._fan_pai_seat_index) {
			table.Refresh_Color_count(seat_index, true);
			table.send_effect_type(seat_index, table.EFFECT_LIANG_ZHU_OR_FAN_PAI, 0, seat_index);
			table.send_effect_type(table._fan_pai_seat_index, table.EFFECT_FAN_PAI_WHITE,
					table._repertory_card[table._fan_pai_card_index], table._fan_pai_seat_index);
		} else if (table._game_type == GameConstants.GS_LLDQ_CALL_BANKER) {
			table.send_effect_type(table._fan_pai_seat_index, table.EFFECT_LIANG_ZHU_OR_FAN_PAI, 0, seat_index);
			table.send_effect_type(table._fan_pai_seat_index, table.EFFECT_FAN_PAI_WHITE,
					table._repertory_card[table._fan_pai_card_index], seat_index);
		}
		return true;
	}

}
