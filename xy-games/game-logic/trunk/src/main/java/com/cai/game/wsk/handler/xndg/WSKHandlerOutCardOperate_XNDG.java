package com.cai.game.wsk.handler.xndg;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.PBUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.wsk.handler.WSKHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.xndg.XndgRsp.TableResponse_xndg;

public class WSKHandlerOutCardOperate_XNDG extends WSKHandlerOutCardOperate<WSKTable_XNDG> {

	@Override
	public void exe(WSKTable_XNDG table) {
		if (_out_card_player != table._current_player) {
			return;
		}
		// 玩家不出
		if (_out_type == 0) {
			user_pass_card(table);
			return;
		}

		table._logic.SortCardList(_out_cards_data, _out_card_count, GameConstants.WSK_ST_ORDER);
		// 出牌判断
		int card_type = adjust_out_card_right(table);
		if (card_type == GameConstants.XNDG_CT_ERROR) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return;
		}
		// 桌面牌分
		table_pai_socre(table);

		table.GRR._card_count[_out_card_player] -= _out_card_count;

		if (!table._is_yi_da_san) {
			if (_out_card_player == table._friend_seat[table.GRR._banker_player]) {
				for (int i = 0; i < _out_card_count; i++) {
					if (_out_cards_data[i] == table._jiao_pai_card) {
						table._out_card_ming_ji = table._jiao_pai_card;
						table.Refresh_Ming_Pai(GameConstants.INVALID_SEAT);
					}
				}
			}
		}

		table._turn_have_score += table._logic.GetCardScore(_out_cards_data, _out_card_count);
		table._turn_out_card_type = card_type;
		table._turn_out_card_count = _out_card_count;
		table._out_card_player = _out_card_player;
		table._prev_palyer = _out_card_player;
		table._cur_out_card_count[_out_card_player] = _out_card_count;
		Arrays.fill(table._turn_out_card_data, GameConstants.INVALID_CARD);
		int wang_count = table._logic.Get_Wang_Count(_out_cards_data, _out_card_count);
		for (int i = 0; i < _out_card_count; i++) {
			table._turn_out_card_data[i] = _out_change_cards_data[i];
			if (_out_change_cards_data[i] > 0x100) {
				table._turn_real_card_data[i] = _out_cards_data[_out_card_count - wang_count--];
			} else {
				table._turn_real_card_data[i] = _out_change_cards_data[i];
			}
			table._cur_out_card_data[_out_card_player][i] = table._turn_real_card_data[i];
		}
		// 清空接下去出牌玩家出牌数据
		int next_player = (_out_card_player + 1) % table.getTablePlayerNumber();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.GRR._card_count[next_player] == 0) {
				if (table._cur_out_card_count[next_player] > 0) {
					table.operate_out_card(next_player, 0, null, -2, GameConstants.INVALID_CARD, false);
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

		int shang_you_score = 0;

		if (table.GRR._card_count[_out_card_player] == 0) {
			int delay = 3;

			if (table._is_yi_da_san) {
				table._tou_num[_out_card_player]++;
				table._chuwan_shunxu[0] = _out_card_player;
				table._current_player = GameConstants.INVALID_SEAT;
				GameSchedule.put(
						new GameFinishRunnable(table.getRoom_id(), _out_card_player, GameConstants.Game_End_NORMAL),
						delay, TimeUnit.SECONDS);
			} else {
				int out_finish_num = 0;
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (table._chuwan_shunxu[i] == GameConstants.INVALID_SEAT) {
						out_finish_num++;
						table._chuwan_shunxu[i] = _out_card_player;
						if (i == 0) {
							table._tou_num[_out_card_player]++;
						}
						break;
					} else {
						out_finish_num++;
					}
				}

				if (out_finish_num == 1) {
					if (table._get_score[table._chuwan_shunxu[0]] >= 200) {
						table._current_player = GameConstants.INVALID_SEAT;
						GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._chuwan_shunxu[0],
								GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
					}
				} else if (out_finish_num == 2) {
					int xia_you_score = 0;
					if (table._chuwan_shunxu[1] == table._friend_seat[table._chuwan_shunxu[0]]) {
						table._current_player = GameConstants.INVALID_SEAT;
						// 结束的话把桌面分数给最后一个出牌玩家
						table._get_score[_out_card_player] += table._turn_have_score;
						table._turn_have_score = 0;
						GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._chuwan_shunxu[0],
								GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
					} else {
						if (table._get_score[table._chuwan_shunxu[1]] >= 200) {
							table._current_player = GameConstants.INVALID_SEAT;
							// 结束的话把桌面分数给最后一个出牌玩家
							table._get_score[_out_card_player] += table._turn_have_score;
							table._turn_have_score = 0;
							GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._chuwan_shunxu[0],
									GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
						} else if (table._get_score[table._chuwan_shunxu[1]] >= 105
								&& table._get_score[table._chuwan_shunxu[0]]
										+ table._get_score[table._friend_seat[table._chuwan_shunxu[0]]] > 0) {
							table._current_player = GameConstants.INVALID_SEAT;
							// 结束的话把桌面分数给最后一个出牌玩家
							table._get_score[_out_card_player] += table._turn_have_score;
							table._turn_have_score = 0;
							GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._chuwan_shunxu[0],
									GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
						} else if (table._get_score[table._chuwan_shunxu[1]] > 0
								&& table._get_score[table._chuwan_shunxu[0]]
										+ table._get_score[table._friend_seat[table._chuwan_shunxu[0]]] >= 105) {
							table._current_player = GameConstants.INVALID_SEAT;
							// 结束的话把桌面分数给最后一个出牌玩家
							table._get_score[_out_card_player] += table._turn_have_score;
							table._turn_have_score = 0;
							GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._chuwan_shunxu[0],
									GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
						}
					}
				} else {
					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						if (table.GRR._card_count[i] != 0) {
							table._chuwan_shunxu[3] = i;
							break;
						}
					}
					table._current_player = GameConstants.INVALID_SEAT;
					// 结束的话把桌面分数给最后一个出牌玩家
					table._get_score[_out_card_player] += table._turn_have_score;
					table._turn_have_score = 0;
					GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._chuwan_shunxu[0],
							GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
				}
			}
		}

		// 显示出牌
		table.operate_out_card(table._out_card_player, table._turn_out_card_count, table._turn_real_card_data,
				table._turn_out_card_type, GameConstants.INVALID_SEAT, false);

		table.Refresh_pai_score(GameConstants.INVALID_SEAT);
		table.Refresh_user_get_score(GameConstants.INVALID_SEAT);

	}

	public void table_pai_socre(WSKTable_XNDG table) {
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

	public void user_pass_card(WSKTable_XNDG table) {
		if (table._turn_out_card_count == 0) {
			return;
		}
		// 清空接下去出牌玩家出牌数据
		int next_player = (_out_card_player + 1) % table.getTablePlayerNumber();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.GRR._card_count[next_player] == 0 && next_player != table._out_card_player) {
				if (table._cur_out_card_count[next_player] > 0) {
					table.operate_out_card(next_player, 0, null, -2, GameConstants.INVALID_CARD, false);
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
			Arrays.fill(table._turn_real_card_data, GameConstants.INVALID_CARD);
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._cur_out_card_count[i] = 0;
				Arrays.fill(table._cur_out_card_data[i], GameConstants.INVALID_CARD);
			}
			if (table.GRR._card_count[next_player] == 0) {
				if (table._cur_out_card_count[next_player] > 0) {
					table.operate_out_card(next_player, 0, null, -2, GameConstants.INVALID_CARD, false);
				}
				if (table._out_card_ming_ji != GameConstants.INVALID_CARD) {
					next_player = table._friend_seat[next_player];
				} else {
					next_player = (next_player + 1) % table.getTablePlayerNumber();
					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						if (table.GRR._card_count[next_player] == 0) {
							next_player = (next_player + 1) % table.getTablePlayerNumber();
						} else {
							break;
						}
					}
				}
				table._current_player = next_player;
				table._cur_out_card_count[next_player] = 0;
				Arrays.fill(table._cur_out_card_data[next_player], GameConstants.INVALID_CARD);

			} else {
				table._current_player = next_player;
				table._prev_palyer = _out_card_player;
				table._cur_out_card_count[table._current_player] = 0;
				Arrays.fill(table._cur_out_card_data[table._current_player], GameConstants.INVALID_CARD);
			}

			int delay = 3;

			if (!table._is_yi_da_san) {
				int out_finish_num = 0;
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (table._chuwan_shunxu[i] != GameConstants.INVALID_SEAT) {
						out_finish_num++;
					}
				}

				if (out_finish_num == 1) {
					if (table._get_score[table._chuwan_shunxu[0]] >= 200) {
						table._current_player = GameConstants.INVALID_SEAT;
						GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._chuwan_shunxu[0],
								GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
					}
				} else if (out_finish_num == 2) {
					int xia_you_score = 0;
					if (table._chuwan_shunxu[1] == table._friend_seat[table._chuwan_shunxu[0]]) {
						table._current_player = GameConstants.INVALID_SEAT;
						GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._chuwan_shunxu[0],
								GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
					} else {
						if (table._get_score[table._chuwan_shunxu[1]] >= 200) {
							table._current_player = GameConstants.INVALID_SEAT;
							GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._chuwan_shunxu[0],
									GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
						} else if (table._get_score[table._chuwan_shunxu[1]] >= 105
								&& table._get_score[table._chuwan_shunxu[0]]
										+ table._get_score[table._friend_seat[table._chuwan_shunxu[0]]] > 0) {
							table._current_player = GameConstants.INVALID_SEAT;
							GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._chuwan_shunxu[0],
									GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
						} else if (table._get_score[table._chuwan_shunxu[1]] > 0
								&& table._get_score[table._chuwan_shunxu[0]]
										+ table._get_score[table._friend_seat[table._chuwan_shunxu[0]]] >= 105) {
							table._current_player = GameConstants.INVALID_SEAT;
							GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._chuwan_shunxu[0],
									GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
						}
					}
				}
			}

		} else {
			table._current_player = next_player;
			table._prev_palyer = _out_card_player;
			table._cur_out_card_count[table._current_player] = 0;
			Arrays.fill(table._cur_out_card_data[table._current_player], GameConstants.INVALID_CARD);
		}

		// 显示出牌
		table.operate_out_card(_out_card_player, 0, null, GameConstants.XNDG_CT_PASS, GameConstants.INVALID_SEAT,
				false);

		if (table._turn_out_card_count == 0) {
			table._is_shou_chu = 1;
		}
		table.Refresh_user_get_score(GameConstants.INVALID_SEAT);
	}

	public int adjust_out_card_right(WSKTable_XNDG table) {

		table._logic.make_change_card(_out_change_cards_data, _out_card_count, _out_cards_data, _out_type);
		table._logic.SortCardList(_out_change_cards_data, _out_card_count, GameConstants.WSK_ST_ORDER);
		int card_type = GameConstants.XNDG_CT_ERROR;
		card_type = table._logic.GetCardType(_out_change_cards_data, _out_card_count);
		if (card_type == GameConstants.XNDG_CT_ERROR) {
			return card_type;
		}
		if (table._turn_out_card_count != 0) {
			if (!table._logic.CompareCard_WSK(table._turn_out_card_data, _out_change_cards_data,
					table._turn_out_card_count, _out_card_count)) {
				return GameConstants.XNDG_CT_ERROR;
			}
		}
		if (!table._logic.RemoveCard(_out_cards_data, _out_card_count, table.GRR._cards_data[_out_card_player],
				table.GRR._card_count[_out_card_player])) {
			return GameConstants.XNDG_CT_ERROR;
		}

		return card_type;
	}

	@Override
	public boolean handler_player_be_in_room(WSKTable_XNDG table, int seat_index) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_XNDG_RECONNECT_DATA);

		TableResponse_xndg.Builder tableResponse = TableResponse_xndg.newBuilder();
		table.load_player_info_data_reconnect(tableResponse);
		tableResponse.setRoomInfo(table.getRoomInfo());

		tableResponse.setBankerPlayer(table.GRR._banker_player);
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
			if (table.has_rule(GameConstants.GAME_RULE_XNDG_DISPLAY_CARD) || seat_index == i) {
				tableResponse.addCardCount(table.GRR._card_count[i]);
			} else {
				tableResponse.addCardCount(-1);
			}

			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder cur_out_cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder wang_cards = Int32ArrayResponse.newBuilder();
			if (table._is_yi_da_san || table._out_card_ming_ji == GameConstants.INVALID_CARD) {
				if (i == seat_index) {
					for (int j = 0; j < table.GRR._card_count[i]; j++) {
						cards.addItem(table.GRR._cards_data[i][j]);
					}
				}
			} else {
				if (i == seat_index
						|| (table._friend_seat[seat_index] == i && table.GRR._card_count[seat_index] == 0)) {
					for (int j = 0; j < table.GRR._card_count[i]; j++) {
						cards.addItem(table.GRR._cards_data[i][j]);
					}
				}
			}

			for (int j = 0; j < table._cur_out_card_count[i]; j++) {
				cur_out_cards.addItem(table._cur_out_card_data[i][j]);
			}
			tableResponse.addOutCardsData(cur_out_cards);
			tableResponse.addCardsData(cards);
			tableResponse.addWinOrder(table._chuwan_shunxu[i]);
		}
		tableResponse.setJiaoCardData(table._jiao_pai_card);
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));
		table.send_response_to_player(seat_index, roomResponse);

		table.Refresh_pai_score(seat_index);
		table.Refresh_user_get_score(seat_index);
		table.Refresh_Ming_Pai(seat_index);
		return true;
	}

}
