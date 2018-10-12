package com.cai.game.wsk.handler.dcts;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.PBUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.wsk.WSKConstants;
import com.cai.game.wsk.WSKMsgConstants;
import com.cai.game.wsk.handler.WSKHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.dcts.dctsRsp.TableResponse_dcts;

public class WSKHandlerOutCardOperate_Dcts extends WSKHandlerOutCardOperate<WSKTable_Dcts> {

	@Override
	public void exe(WSKTable_Dcts table) {
		if (_out_card_player != table._current_player) {
			return;
		}
		// 玩家不出
		if (_out_type == 0) {
			user_pass_card(table);
			return;
		}

		table._logic.SortCardList_Out(_out_cards_data, _out_card_count, GameConstants.WSK_ST_ORDER);
		// 出牌判断
		int card_type = adjust_out_card_right(table);
		if (card_type == WSKConstants.DCTS_CT_ERROR) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return;
		}

		table.GRR._card_count[_out_card_player] -= _out_card_count;

		if (_out_card_player == table._friend_seat[table.GRR._banker_player]) {
			for (int i = 0; i < _out_card_count; i++) {
				if (_out_cards_data[i] == table._jiao_pai_card) {
					table._out_card_ming_ji = table._jiao_pai_card;
					int data[] = new int[1];
					data[0] = table._out_card_ming_ji;
					table.send_effect_type(_out_card_player, 2, data, 1, GameConstants.INVALID_SEAT);
				}
			}
		}
		// 喜钱
		int xian_qian_score = table._logic.GetCardXianScore(_out_cards_data, _out_card_count, card_type);
		if (xian_qian_score > 0) {
			table._xi_qian_times[_out_card_player]++;
			table._xi_qian_score[_out_card_player] += xian_qian_score * (table.getTablePlayerNumber() - 1);

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == _out_card_player) {
					continue;
				}
				table._xi_qian_score[i] -= xian_qian_score;
			}
			table.operate_player_data();
		}

		table._turn_have_score += table._logic.GetCardScore(_out_cards_data, _out_card_count);
		table._turn_out_card_type = card_type;
		table._turn_out_card_count = _out_card_count;
		table._out_card_player = _out_card_player;
		table._prev_palyer = _out_card_player;
		table._cur_out_card_count[_out_card_player] = _out_card_count;
		Arrays.fill(table._turn_out_card_data, GameConstants.INVALID_CARD);
		for (int i = 0; i < _out_card_count; i++) {
			table._turn_out_card_data[i] = _out_cards_data[i];
			table._cur_out_card_data[_out_card_player][i] = _out_cards_data[i];
		}
		// 清空接下去出牌玩家出牌数据
		int next_player = (_out_card_player + 1) % table.getTablePlayerNumber();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.GRR._card_count[next_player] == 0) {
				if (table._cur_out_card_count[next_player] > 0) {
					table._current_player = GameConstants.INVALID_SEAT;
					table.operate_out_card(next_player, 0, null, -2, GameConstants.INVALID_SEAT, false);
				}
				// 显示出牌
				table._current_player = next_player;
				table._cur_out_card_count[next_player] = 0;
				Arrays.fill(table._cur_out_card_data[next_player], GameConstants.INVALID_CARD);
				next_player = (next_player + 1) % table.getTablePlayerNumber();
			} else {
				break;
			}
		}
		table._current_player = next_player;
		table._cur_out_card_count[next_player] = 0;
		Arrays.fill(table._cur_out_card_data[next_player], GameConstants.INVALID_CARD);

		if (table.GRR._card_count[_out_card_player] == 0) {
			int delay = 3;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._chuwan_shunxu[i] == GameConstants.INVALID_SEAT) {
					if (i == 0) {
						table._cur_banker = _out_card_player;
					}
					table._chuwan_shunxu[i] = _out_card_player;
					break;
				}
			}
			if (table._is_yi_da_san) {
				if (table._chuwan_shunxu[2] != GameConstants.INVALID_SEAT || table._seat_team[_out_card_player] == 1) {
					table._current_player = GameConstants.INVALID_SEAT;
					if (table._seat_team[_out_card_player] != 1) {
						for (int j = 0; j < table.getTablePlayerNumber(); j++) {
							if (table.GRR._card_count[j] != 0) {
								for (int i = 0; i < table.getTablePlayerNumber(); i++) {
									if (table._chuwan_shunxu[i] == GameConstants.INVALID_SEAT) {
										table._chuwan_shunxu[i] = j;
										break;
									}
								}
							}

							table._cur_out_card_count[j] = 0;
							Arrays.fill(table._cur_out_card_data[j], GameConstants.INVALID_CARD);
						}
					}
					table._get_score[_out_card_player] += table._turn_have_score;
					GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._chuwan_shunxu[0],
							GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
				}
			} else {
				// 一二游
				if ((table._chuwan_shunxu[0] != GameConstants.INVALID_SEAT
						&& table._chuwan_shunxu[1] != GameConstants.INVALID_SEAT
						&& table._seat_team[table._chuwan_shunxu[1]] == table._seat_team[table._chuwan_shunxu[0]])
						|| table._chuwan_shunxu[2] != GameConstants.INVALID_SEAT) {
					for (int j = 0; j < table.getTablePlayerNumber(); j++) {
						if (table.GRR._card_count[j] != 0) {
							for (int i = 0; i < table.getTablePlayerNumber(); i++) {
								if (table._chuwan_shunxu[i] == GameConstants.INVALID_SEAT) {
									table._chuwan_shunxu[i] = j;
									break;
								}
							}
						}

						table._cur_out_card_count[j] = 0;
						Arrays.fill(table._cur_out_card_data[j], GameConstants.INVALID_CARD);
					}
					table._get_score[table._out_card_player] += table._turn_have_score;
					table._current_player = GameConstants.INVALID_SEAT;
					GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._chuwan_shunxu[0],
							GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
				}
			}
		}
		// 显示出牌
		table.operate_out_card(table._out_card_player, table._turn_out_card_count, table._turn_out_card_data,
				table._turn_out_card_type, GameConstants.INVALID_SEAT, false);

		// table.Refresh_pai_score(GameConstants.INVALID_SEAT);
		table.Refresh_user_get_score(GameConstants.INVALID_SEAT);

		// if (!table._is_yi_da_san) {
		// // 刚出完刷新友方
		// for (int i = 0; i < table.getTablePlayerNumber(); i++) {
		// table.Refresh_Dui_You_Card(_out_card_player);
		// }
		// }

	}

	public void table_pai_socre(WSKTable_Dcts table) {
		int pai_score = 0;
		int remove_card[] = new int[table.get_hand_card_count_max()];
		int remove_count = 0;
		for (int i = 0; i < _out_card_count; i++) {
			int value = table._logic.GetCardLogicValue(_out_cards_data[i]);
			if (value == 5 || value == 10 || value == 13) {
				remove_card[remove_count++] = _out_cards_data[i];
			}
			if (value == 5) {
				pai_score += 5;
			} else if (value == 10 || value == 13) {
				pai_score += 10;
			}
		}
		if (!table._logic.RemoveCard(remove_card, remove_count, table._pai_score_card, table._pai_score_count)) {
			// table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			// return;
		}
		table._pai_score_count -= remove_count;
		table._pai_score -= pai_score;
	}

	public void user_pass_card(WSKTable_Dcts table) {
		if (table._turn_out_card_count == 0) {
			return;
		}
		// 清空接下去出牌玩家出牌数据
		int next_player = (_out_card_player + 1) % table.getTablePlayerNumber();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.GRR._card_count[next_player] == 0 && next_player != table._out_card_player) {
				if (table._cur_out_card_count[next_player] > 0) {
					table._current_player = GameConstants.INVALID_SEAT;
					table.operate_out_card(next_player, 0, null, -2, GameConstants.INVALID_SEAT, false);
				}
				next_player = (next_player + 1) % table.getTablePlayerNumber();
			} else {
				break;
			}
		}

		// 一轮不出
		if (next_player == table._out_card_player) {
			// 清空桌面牌分
			table._get_score[table._out_card_player] += table._turn_have_score;
			table._turn_have_score = 0;
			table._turn_out_card_count = 0;

			Arrays.fill(table._turn_out_card_data, GameConstants.INVALID_CARD);
			if (table.GRR._card_count[next_player] == 0) {
				if (table._cur_out_card_count[next_player] > 0) {
					table._current_player = GameConstants.INVALID_SEAT;
					table.operate_out_card(next_player, 0, null, -2, GameConstants.INVALID_SEAT, false);
				}
				table._current_player = next_player;
				table._cur_out_card_count[next_player] = 0;
				Arrays.fill(table._cur_out_card_data[next_player], GameConstants.INVALID_CARD);

				next_player = (next_player + 1) % table.getTablePlayerNumber();
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (table.GRR._card_count[next_player] == 0) {
						next_player = (next_player + 1) % table.getTablePlayerNumber();
					} else {
						break;
					}
				}
				table._current_player = next_player;

			} else {
				table._current_player = next_player;
				table._prev_palyer = _out_card_player;
				table._cur_out_card_count[table._current_player] = 0;
				Arrays.fill(table._cur_out_card_data[table._current_player], GameConstants.INVALID_CARD);
			}
		} else {
			table._current_player = next_player;
			table._prev_palyer = _out_card_player;
			table._cur_out_card_count[table._current_player] = 0;
			Arrays.fill(table._cur_out_card_data[table._current_player], GameConstants.INVALID_CARD);
		}

		// 显示出牌
		table.operate_out_card(_out_card_player, 0, null, GameConstants.WSK_GF_CT_PASS, GameConstants.INVALID_SEAT,
				false);

		if (table._turn_out_card_count == 0) {
			table._is_shou_chu = 1;
		}
		table.Refresh_user_get_score(GameConstants.INVALID_SEAT);

	}

	public int adjust_out_card_right(WSKTable_Dcts table) {
		int card_type = WSKConstants.DCTS_CT_ERROR;
		card_type = table._logic.GetCardType(_out_cards_data, _out_card_count);

		if (card_type == WSKConstants.DCTS_CT_ERROR) {
			return card_type;
		}
		table._logic.sort_card_date_list_by_type(_out_cards_data, _out_card_count, card_type,
				table._turn_three_link_num);

		if (table._turn_out_card_count != 0) {
			if (!table._logic.CompareCard_WSK(table._turn_out_card_data, _out_cards_data, table._turn_out_card_count,
					_out_card_count, table._turn_three_link_num)) {
				return WSKConstants.DCTS_CT_ERROR;
			}
		}
		if (!table._logic.RemoveCard(_out_cards_data, _out_card_count, table.GRR._cards_data[_out_card_player],
				table.GRR._card_count[_out_card_player])) {
			return WSKConstants.DCTS_CT_ERROR;
		}

		return card_type;
	}

	@Override
	public boolean handler_player_be_in_room(WSKTable_Dcts table, int seat_index) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(WSKMsgConstants.RESPONSE_DCTS_RECONNECT_DATA);

		TableResponse_dcts.Builder tableResponse = TableResponse_dcts.newBuilder();
		table.load_player_info_data_reconnect(tableResponse);
		tableResponse.setRoomInfo(table.getRoomInfo());

		tableResponse.setBankerPlayer(GameConstants.INVALID_SEAT);
		tableResponse.setCurrentPlayer(table._current_player);
		tableResponse.setPrevPlayer(table._prev_palyer);
		tableResponse.setPrOutCardPlayer(table._out_card_player);
		tableResponse.setPrCardsCount(table._turn_out_card_count);
		tableResponse.setPrOutCardType(table._turn_out_card_type);
		tableResponse.setIsYiDaSan(table._is_yi_da_san);
		if (table._turn_out_card_count == 0 && seat_index == table._current_player) {
			tableResponse.setIsFirstOut(1);
		} else {
			tableResponse.setIsFirstOut(0);
		}
		for (int i = 0; i < table._turn_out_card_count; i++) {
			tableResponse.addPrCardsData(table._turn_out_card_data[i]);
		}
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.GRR._card_count[i] <= 5 || i == seat_index) {
				tableResponse.addCardCount(table.GRR._card_count[i]);
			} else {
				tableResponse.addCardCount(table.GRR._card_count[i]);
			}

			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder cur_out_cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder wang_cards = Int32ArrayResponse.newBuilder();
			if (seat_index == i) {
				for (int j = 0; j < table.GRR._card_count[i]; j++) {
					cards.addItem(table.GRR._cards_data[i][j]);
				}
			}

			tableResponse.addOutCardsData(cur_out_cards);
			tableResponse.addCardsData(cards);
			boolean is_out_finish = false;
			for (int j = 0; j < table.getTablePlayerNumber(); j++) {
				if (table._chuwan_shunxu[j] == i) {
					tableResponse.addWinOrder(j);
					is_out_finish = true;
					break;
				}

			}
			if (!is_out_finish) {
				tableResponse.addWinOrder(GameConstants.INVALID_SEAT);
			}
		}
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));
		table.send_response_to_player(seat_index, roomResponse);

		table.Refresh_pai_score(seat_index);
		table.Refresh_user_get_score(seat_index);

		table.operate_out_card(table._out_card_player, table._turn_out_card_count, table._turn_out_card_data,
				table._turn_out_card_type, seat_index, false);
		return true;
	}

}
