package com.cai.game.shengji.handler.xfgd_four;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.PBUtil;
import com.cai.domain.SheduleArgs;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.shengji.data.tagAnalyseCardType;
import com.cai.game.shengji.handler.SJHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.xfgd.xfgdRsp.TableResponse_Xfgd;

public class SJHandlerOutCardOperate_XFGD_FOUR extends SJHandlerOutCardOperate<SJTable_XFGD_Four> {

	@Override
	public void exe(SJTable_XFGD_Four table) {
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
				table._turn_out_card_type = cbCardType;
				for (int i = 0; i < table._turn_out_card_count; i++) {
					table._turn_out_card_data[i] = _out_cards_data[i];
				}
			} else {
				table._kill_ed = GameConstants.INVALID_SEAT;
				table._kill_ing = GameConstants.INVALID_SEAT;
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
			//

			table._get_score[table._max_card_seat] += table._table_score;
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
				if (i != table.GRR._banker_player) {
					xian_get_score += table._get_score[i];
				}
			}
			if (table.GRR._card_count[table._current_player] == 0) {
				int delay = 3;
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._current_player,
						GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
			} else if (table._select_dang[table.GRR._banker_player] == 10 && xian_get_score > 0) {
				int delay = 3;
				GameSchedule.put(
						new GameFinishRunnable(table.getRoom_id(), _out_card_player, GameConstants.Game_End_NORMAL),
						delay, TimeUnit.SECONDS);
			} else if (xian_get_score >= ((10 - table._select_dang[table.GRR._banker_player]) * 10 + 5) * 2) {
				int delay = 3;
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._current_player,
						GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
			} else if (xian_get_score > (10 - table._select_dang[table.GRR._banker_player]) * 10 && !table._is_po_dang
					&& xian_get_score < ((10 - table._select_dang[table.GRR._banker_player]) * 10 + 5) * 2) {
				if (!table.has_rule(GameConstants.GAME_RULE_XFGD_DAOGUANG)
						|| table._select_dang[table.GRR._banker_player] == 10) {
					int delay = 3;
					GameSchedule.put(
							new GameFinishRunnable(table.getRoom_id(), _out_card_player, GameConstants.Game_End_NORMAL),
							delay, TimeUnit.SECONDS);
				} else if (table.has_rule(GameConstants.GAME_RULE_XFGD_TOU_XIANG)) {
					table._is_po_dang = true;
					table.tou_xiang_begin();
					table.schedule(table.ID_TIMER_CLEAR_CARD, SheduleArgs.newArgs(), 1500);
				} else {
					table.schedule(table.ID_TIMER_CLEAR_CARD, SheduleArgs.newArgs(), 1500);
				}
			} else {
				table.schedule(table.ID_TIMER_CLEAR_CARD, SheduleArgs.newArgs(), 1500);
			}

			table._current_player = GameConstants.INVALID_SEAT;
			table.operate_out_card(_out_card_player, _out_card_count, _out_cards_data, cbCardType,
					GameConstants.INVALID_SEAT);
		} else {
			table.operate_out_card(_out_card_player, _out_card_count, _out_cards_data, cbCardType,
					GameConstants.INVALID_SEAT);
		}
		table._out_card_times[_out_card_player]++;
		table.RefreshScore(GameConstants.INVALID_SEAT);
		table.send_zhu_count(GameConstants.INVALID_SEAT);
		table.send_history(GameConstants.INVALID_SEAT);

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

	public int adjust_out_card_right(SJTable_XFGD_Four table) {

		int cbCardType = table._logic.GetCardType(_out_cards_data, _out_card_count);

		if (table._origin_out_card_count == 0) {
			if (cbCardType == GameConstants.XFGD_CT_ERROR) {
				return cbCardType;
			}
		}

		if (table._origin_out_card_count != 0) {
			// 没有人出过牌
			if (!table._logic.is_he_li(table._origin_out_card_data, table._origin_out_card_count, _out_cards_data,
					_out_card_count, table.GRR._cards_data[_out_card_player],
					table.GRR._card_count[_out_card_player])) {
				return GameConstants.XFGD_CT_ERROR;
			}
			if (cbCardType == GameConstants.XFGD_CT_SHUAI_PAI) {
				cbCardType = GameConstants.XFGD_CT_SINGLE;
			}
			if (cbCardType == GameConstants.XFGD_CT_ERROR) {
				cbCardType = GameConstants.XFGD_CT_SINGLE;
			}
		} else {
			// 甩牌
			if (cbCardType == GameConstants.XFGD_CT_SHUAI_PAI) {
				tagAnalyseCardType type_card = new tagAnalyseCardType();
				table._logic.Analyse_card_type(_out_cards_data, _out_card_count, type_card);
				for (int i = 1; i < type_card.type_count; i++) {
					if (type_card.type[0] != type_card.type[i]) {
						return GameConstants.XFGD_CT_ERROR;
					}
				}
				int color = table._logic.GetCardColor(_out_cards_data[0]);
				if (_out_cards_data[0] > table._zhu_value) {
					color = table._zhu_type;
				}
				for (int i = 1; i < _out_card_count; i++) {
					int color_temp = table._logic.GetCardColor(_out_cards_data[i]);
					if (_out_cards_data[i] > table._zhu_value) {
						color_temp = table._zhu_type;
					}
					if (color_temp != color) {
						return GameConstants.XFGD_CT_ERROR;
					}
				}

				for (int j = 0; j < table.getTablePlayerNumber(); j++) {
					if (j == _out_card_player) {
						continue;
					}
					int hand_card_data_temp[] = new int[table.GRR._card_count[j]];
					int hand_card_count_temp = table.GRR._card_count[j];
					for (int x = 0; x < hand_card_count_temp; x++) {
						hand_card_data_temp[x] = table.GRR._cards_data[j][x];
					}
					int i = type_card.type_count - 1;
					int can_out_card_data[] = new int[table.get_hand_card_count_max()];
					int out_data[] = new int[type_card.count[i]];
					for (int x = 0; x < type_card.count[i]; x++) {
						out_data[x] = type_card.card_data[i][x];
					}
					boolean is_zhu = false;
					int can_out_count = table._logic.Player_Can_out_card(hand_card_data_temp, hand_card_count_temp,
							out_data, type_card.count[i], can_out_card_data);
					if (table._logic.GetCardColor_Count(hand_card_data_temp, hand_card_count_temp,
							color) < _out_card_count) {
						is_zhu = true;
					}
					if (!is_zhu) {
						tagAnalyseCardType can_out_type_card = new tagAnalyseCardType();
						table._logic.get_card_all_type(can_out_card_data, can_out_count, can_out_type_card);
						for (int x = 0; x < can_out_type_card.type_count; x++) {
							if (table._logic.comparecarddata(type_card.card_data[i], type_card.count[i],
									can_out_type_card.card_data[x], can_out_type_card.count[x])) {
								table.send_error_notify(_out_card_player, 2, "甩牌失败");
								for (int z = 0; z < _out_card_count; z++) {
									table._turn_out_shuai_pai_data[z] = _out_cards_data[z];
								}
								for (int z = 0; z < type_card.count[0]; z++) {
									_out_cards_data[z] = type_card.card_data[0][z];
								}
								table._turn_out_shuai_pai_count = _out_card_count;
								_out_card_count = type_card.count[0];
								int type = type_card.type[0];
								for (int y = 1; y < type_card.type_count; y++) {
									if (!table._logic.comparecarddata(_out_cards_data, _out_card_count,
											type_card.card_data[y], type_card.count[y])) {
										for (int z = 0; z < type_card.count[y]; z++) {
											_out_cards_data[z] = type_card.card_data[y][z];
										}
										_out_card_count = type_card.count[y];
										type = type_card.type[y];
									}
								}
								if (!table._logic.remove_cards_by_data(table.GRR._cards_data[_out_card_player],
										table.GRR._card_count[_out_card_player], _out_cards_data, _out_card_count)) {
									return GameConstants.XFGD_CT_ERROR;
								}
								return type;
							}
						}
					} else {
						int must_out_data[] = new int[hand_card_count_temp];
						int must_out_count = 0;
						if (color == table._zhu_type) {
							for (int q = 0; q < hand_card_count_temp; q++) {
								if (hand_card_data_temp[q] > table._zhu_value) {
									must_out_data[must_out_count++] = hand_card_data_temp[q];
								}
							}
						} else {
							for (int q = 0; q < hand_card_count_temp; q++) {
								if (table._logic.GetCardColor(hand_card_data_temp[q]) == color
										&& hand_card_data_temp[q] < table._zhu_value) {
									must_out_data[must_out_count++] = hand_card_data_temp[q];
								}
							}
						}
						if (must_out_count != 0) {
							if (_out_card_count - must_out_count >= type_card.count[i]) {
								tagAnalyseCardType can_out_type_card = new tagAnalyseCardType();
								table._logic.get_card_all_type(hand_card_data_temp, hand_card_count_temp,
										can_out_type_card);
								for (int x = 0; x < can_out_type_card.type_count; x++) {
									if (table._logic.comparecarddata(type_card.card_data[i], type_card.count[i],
											can_out_type_card.card_data[x], can_out_type_card.count[x])) {
										table.send_error_notify(_out_card_player, 2, "甩牌失败");
										for (int z = 0; z < _out_card_count; z++) {
											table._turn_out_shuai_pai_data[z] = _out_cards_data[z];
										}
										table._turn_out_shuai_pai_count = _out_card_count;
										for (int z = 0; z < type_card.count[0]; z++) {
											_out_cards_data[z] = type_card.card_data[0][z];
										}
										_out_card_count = type_card.count[0];
										int type = type_card.type[0];
										for (int y = 1; y < type_card.type_count; y++) {
											if (!table._logic.comparecarddata(_out_cards_data, _out_card_count,
													type_card.card_data[y], type_card.count[y])) {
												for (int z = 0; z < type_card.count[y]; z++) {
													_out_cards_data[z] = type_card.card_data[y][z];
												}
												_out_card_count = type_card.count[y];
												type = type_card.type[y];
											}
										}
										if (!table._logic.remove_cards_by_data(table.GRR._cards_data[_out_card_player],
												table.GRR._card_count[_out_card_player], _out_cards_data,
												_out_card_count)) {
											return GameConstants.XFGD_CT_ERROR;
										}
										return type;
									}
								}
							} else {
								tagAnalyseCardType can_out_type_card = new tagAnalyseCardType();
								table._logic.get_card_all_type(must_out_data, must_out_count, can_out_type_card);
								for (int x = 0; x < can_out_type_card.type_count; x++) {
									if (table._logic.comparecarddata(type_card.card_data[i], type_card.count[i],
											can_out_type_card.card_data[x], can_out_type_card.count[x])) {
										table.send_error_notify(_out_card_player, 2, "甩牌失败");
										for (int z = 0; z < _out_card_count; z++) {
											table._turn_out_shuai_pai_data[z] = _out_cards_data[z];
										}
										table._turn_out_shuai_pai_count = _out_card_count;
										for (int z = 0; z < type_card.count[0]; z++) {
											_out_cards_data[z] = type_card.card_data[0][z];
										}
										_out_card_count = type_card.count[0];
										int type = type_card.type[0];
										for (int y = 1; y < type_card.type_count; y++) {
											if (!table._logic.comparecarddata(_out_cards_data, _out_card_count,
													type_card.card_data[y], type_card.count[y])) {
												for (int z = 0; z < type_card.count[y]; z++) {
													_out_cards_data[z] = type_card.card_data[y][z];
												}
												_out_card_count = type_card.count[y];
												type = type_card.type[y];
											}
										}
										if (!table._logic.remove_cards_by_data(table.GRR._cards_data[_out_card_player],
												table.GRR._card_count[_out_card_player], _out_cards_data,
												_out_card_count)) {
											return GameConstants.XFGD_CT_ERROR;
										}
										return type;
									}
								}
							}

						} else {
							tagAnalyseCardType can_out_type_card = new tagAnalyseCardType();
							table._logic.get_card_all_type(can_out_card_data, can_out_count, can_out_type_card);
							for (int x = 0; x < can_out_type_card.type_count; x++) {
								if (table._logic.comparecarddata(type_card.card_data[i], type_card.count[i],
										can_out_type_card.card_data[x], can_out_type_card.count[x])) {
									table.send_error_notify(_out_card_player, 2, "甩牌失败");
									for (int z = 0; z < _out_card_count; z++) {
										table._turn_out_shuai_pai_data[z] = _out_cards_data[z];
									}
									table._turn_out_shuai_pai_count = _out_card_count;
									for (int z = 0; z < type_card.count[0]; z++) {
										_out_cards_data[z] = type_card.card_data[0][z];
									}
									_out_card_count = type_card.count[0];
									int type = type_card.type[0];
									for (int y = 1; y < type_card.type_count; y++) {
										if (!table._logic.comparecarddata(_out_cards_data, _out_card_count,
												type_card.card_data[y], type_card.count[y])) {
											for (int z = 0; z < type_card.count[y]; z++) {
												_out_cards_data[z] = type_card.card_data[y][z];
											}
											_out_card_count = type_card.count[y];
											type = type_card.type[y];
										}
									}
									if (!table._logic.remove_cards_by_data(table.GRR._cards_data[_out_card_player],
											table.GRR._card_count[_out_card_player], _out_cards_data,
											_out_card_count)) {
										return GameConstants.XFGD_CT_ERROR;
									}
									return type;
								}
							}
						}
					}

				}
			}
		}

		if (!table._logic.remove_cards_by_data(table.GRR._cards_data[_out_card_player],
				table.GRR._card_count[_out_card_player], _out_cards_data, _out_card_count)) {
			return GameConstants.XFGD_CT_ERROR;
		}
		return cbCardType;
	}

	@Override
	public boolean handler_player_be_in_room(SJTable_XFGD_Four table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_XFGD_RECONNECT_DATA);
		// 发送数据
		TableResponse_Xfgd.Builder tableResponse = TableResponse_Xfgd.newBuilder();
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

		table.RefreshScore(seat_index);
		table.send_history(seat_index);
		table.send_di_pai(seat_index);
		table.send_zhu_count(seat_index);
		// table.send_zhu_pai_type(seat_index);

		if (table._is_po_dang) {
			if (table._is_banker_tou_xiang == -1) {
				if (seat_index == table.GRR._banker_player) {
					table.tou_xiang_begin();
				}
			} else if (table._is_banker_tou_xiang == 1) {
				table.send_tou_xiang_result(seat_index);
			}

		}
		return true;
	}

}
