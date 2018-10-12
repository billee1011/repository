package com.cai.game.wsk.handler.sxth;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.PBUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.wsk.data.tagAnalyseIndexResult_WSK;
import com.cai.game.wsk.handler.WSKHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.sxth.SxthRsp.TableResponse_Sxth;

public class WSKHandlerOutCardOperate_SXTH extends WSKHandlerOutCardOperate<WSKTable_SXTH> {

	@Override
	public void exe(WSKTable_SXTH table) {
		if (_out_card_player != table._current_player) {
			return;
		}
		// 玩家不出
		if (_out_type == 0) {
			user_pass_card(table);
			return;
		}

		table._logic.SortCardList(_out_cards_data, _out_card_count, GameConstants.WSK_ST_VALUE);
		// 出牌判断
		int card_type = adjust_out_card_right(table);
		if (card_type == GameConstants.SXTH_CT_ERROR) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return;
		}
		int score_effect_type = 0;
		_out_type = card_type;
		if (card_type == GameConstants.SXTH_CT_TONG_HUA) {
			// 花分
			table._flower_score[_out_card_player] += 1;
			score_effect_type = table.EFFECT_SCORE;
		} else if (card_type == GameConstants.SXTH_CT_BOMB) {

			table._flower_score[_out_card_player] += _out_card_count - 5;
			score_effect_type = table.EFFECT_SCORE;
		} else if (card_type == GameConstants.SXTH_CT_TONG_ZHANG && table._logic.GetCardColor(_out_cards_data[0]) == 4
				&& table.has_rule(GameConstants.GAME_RULE_SXTH_SHAO_JI)) {
			// 奖励分
			table._xi_qian_score[_out_card_player] -= _out_card_count;
			score_effect_type = table.EFFECT_FEI_JI;
		}
		// 桌面牌分
		table_pai_socre(table);

		table.GRR._card_count[_out_card_player] -= _out_card_count;
		table._out_card_times[_out_card_player]++;
		table._logic.SortCardList(table.GRR._cards_data[_out_card_player], table.GRR._card_count[_out_card_player],
				table._score_type[_out_card_player]);

		table._turn_have_score += table._logic.GetCardScore(_out_cards_data, _out_card_count);
		table._turn_out_card_type = card_type;
		table._turn_out_card_count = _out_card_count;
		table._out_card_player = _out_card_player;
		table._prev_palyer = _out_card_player;
		table._cur_out_card_count[_out_card_player] = _out_card_count;
		table._cur_out_car_type[_out_card_player] = card_type;
		Arrays.fill(table._turn_out_card_data, GameConstants.INVALID_CARD);
		for (int i = 0; i < _out_card_count; i++) {
			table._turn_out_card_data[i] = _out_cards_data[i];
			table._cur_out_card_data[_out_card_player][i] = table._turn_out_card_data[i];

		}
		// 清空接下去出牌玩家出牌数据
		int next_player = (_out_card_player + 1) % table.getTablePlayerNumber();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.GRR._card_count[next_player] == 0) {
				if (table._cur_out_card_count[next_player] > 0) {
					table.operate_out_card(next_player, 0, null, -2, GameConstants.INVALID_SEAT, false);
				}
				// 显示出牌
				table._current_player = next_player;
				table._cur_out_card_count[next_player] = 0;
				table._cur_out_car_type[next_player] = GameConstants.SXTH_CT_ERROR;
				Arrays.fill(table._cur_out_card_data[next_player], GameConstants.INVALID_CARD);
				next_player = (next_player + 1) % table.getTablePlayerNumber();
			} else {
				break;
			}
		}
		table._current_player = next_player;
		table._cur_out_card_count[next_player] = 0;
		table._cur_out_car_type[next_player] = GameConstants.SXTH_CT_ERROR;
		Arrays.fill(table._cur_out_card_data[next_player], GameConstants.INVALID_CARD);

		int shang_you_score = 0;

		if (table.GRR._card_count[_out_card_player] == 0) {
			int delay = 3;

			int out_finish_num = 0;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._chuwan_shunxu[i] == GameConstants.INVALID_SEAT) {
					table._chuwan_shunxu[i] = _out_card_player;
					out_finish_num++;
					break;
				} else {
					out_finish_num++;
				}
			}
			if (table._chuwan_shunxu[1] != GameConstants.INVALID_SEAT) {
				if (table._chuwan_shunxu[0] == table._friend_seat[table._chuwan_shunxu[1]]) {
					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						if (table.GRR._card_count[i] != 0) {
							for (int j = 0; j < table.getTablePlayerNumber(); j++) {
								if (table._chuwan_shunxu[j] == GameConstants.INVALID_SEAT) {
									table._chuwan_shunxu[j] = i;
									break;
								}
							}
						}
					}
					table.get_hand_reward_score();
					table._current_player = GameConstants.INVALID_SEAT;
					// 分数特效
					score_effect_type = table.EFFECT_SCORE;
					table._get_score[_out_card_player] += table._turn_have_score;
					GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), GameConstants.INVALID_SEAT,
							GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
				} else if (out_finish_num == 3) {
					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						if (table.GRR._card_count[i] != 0) {
							for (int j = 0; j < table.getTablePlayerNumber(); j++) {
								if (table._chuwan_shunxu[j] == GameConstants.INVALID_SEAT) {
									table._chuwan_shunxu[j] = i;
								}
							}
						}
					}
					table.get_hand_reward_score();
					table._current_player = GameConstants.INVALID_SEAT;
					// 分数特效
					score_effect_type = table.EFFECT_SCORE;
					table._get_score[_out_card_player] += table._turn_have_score;
					GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), GameConstants.INVALID_SEAT,
							GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
				}
			}

		}

		// 显示出牌
		table.operate_out_card(table._out_card_player, table._turn_out_card_count, table._turn_out_card_data,
				table._turn_out_card_type, GameConstants.INVALID_SEAT, false);

		table.Refresh_pai_score(GameConstants.INVALID_SEAT);

		if (score_effect_type != 0) {
			table.Refresh_user_get_score(GameConstants.INVALID_SEAT, true);
			table.send_effect_type(_out_card_player, score_effect_type, 0, GameConstants.INVALID_SEAT);
			if (score_effect_type == table.EFFECT_FEI_JI) {
				table.send_effect_type(_out_card_player, table.EFFECT_SCORE, 0, GameConstants.INVALID_SEAT);
			}
		} else {
			table.Refresh_user_get_score(GameConstants.INVALID_SEAT, false);
		}
	}

	public void table_pai_socre(WSKTable_SXTH table) {
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
		// if (!table._logic.RemoveCard(remove_card, remove_count,
		// table._pai_score_card, table._pai_score_count)) {
		// // table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
		// // return;
		// }
		table._pai_score_count -= remove_count;
		table._pai_score -= pai_score;
	}

	public void user_pass_card(WSKTable_SXTH table) {
		if (table._turn_out_card_count == 0) {
			return;
		}
		// 清空接下去出牌玩家出牌数据
		int next_player = (_out_card_player + 1) % table.getTablePlayerNumber();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.GRR._card_count[next_player] == 0) {
				if (next_player == table._out_card_player) {
					break;
				}
				if (table._cur_out_card_count[next_player] > 0) {
					table.operate_out_card(next_player, 0, null, -2, GameConstants.INVALID_SEAT, false);
				}
				next_player = (next_player + 1) % table.getTablePlayerNumber();
			} else {
				break;
			}
		}
		boolean has_score = false;
		table._cur_out_car_type[_out_card_player] = _out_type;
		// 一轮不出
		if (next_player == table._out_card_player) {

			// 清空桌面牌分
			if (table._turn_have_score > 0) {
				has_score = true;
			}
			table._get_score[table._out_card_player] += table._turn_have_score;
			table._turn_have_score = 0;
			table._turn_out_card_count = 0;

			if (table.GRR._card_count[next_player] == 0) {
				if (table._cur_out_card_count[next_player] > 0) {
					table.operate_out_card(next_player, 0, null, -2, GameConstants.INVALID_SEAT, false);
				}

				next_player = (next_player + 1) % table.getTablePlayerNumber();
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (table.GRR._card_count[next_player] == 0) {
						next_player = (next_player + 1) % table.getTablePlayerNumber();
					} else {
						break;
					}
				}

				table._current_player = next_player;
				table._cur_out_card_count[next_player] = 0;
				table._cur_out_car_type[next_player] = GameConstants.SXTH_CT_ERROR;
				Arrays.fill(table._cur_out_card_data[next_player], GameConstants.INVALID_CARD);

			} else {
				table._current_player = next_player;
				table._prev_palyer = _out_card_player;
				table._cur_out_card_count[table._current_player] = 0;
				table._cur_out_car_type[table._current_player] = GameConstants.SXTH_CT_ERROR;
				Arrays.fill(table._cur_out_card_data[table._current_player], GameConstants.INVALID_CARD);
			}
			Arrays.fill(table._turn_out_card_data, GameConstants.INVALID_CARD);
			Arrays.fill(table._turn_real_card_data, GameConstants.INVALID_CARD);
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._cur_out_card_count[i] = 0;
				table._cur_out_car_type[i] = GameConstants.SXTH_CT_ERROR;
				Arrays.fill(table._cur_out_card_data[i], GameConstants.INVALID_CARD);
			}
		} else {
			table._current_player = next_player;
			table._prev_palyer = _out_card_player;
			table._cur_out_card_count[table._current_player] = 0;
			table._cur_out_car_type[table._current_player] = GameConstants.SXTH_CT_ERROR;
			Arrays.fill(table._cur_out_card_data[table._current_player], GameConstants.INVALID_CARD);
		}

		// 显示出牌
		table.operate_out_card(_out_card_player, 0, null, GameConstants.SXTH_CT_PASS, GameConstants.INVALID_SEAT,
				false);

		if (table._turn_out_card_count == 0) {
			table._is_shou_chu = 1;
		}

		if (has_score) {
			table.Refresh_user_get_score(GameConstants.INVALID_SEAT, true);
			table.send_effect_type(table._out_card_player, table.EFFECT_SCORE, 0, GameConstants.INVALID_SEAT);
		} else {
			table.Refresh_user_get_score(GameConstants.INVALID_SEAT, false);
		}
	}

	public int adjust_out_card_right(WSKTable_SXTH table) {
		for (int i = 0; i < _out_card_count; i++) {
			if (_out_cards_data[i] > 0x100) {
				_out_cards_data[i] = _out_cards_data[i] % 0x100;
			}
		}
		table._logic.SortCardList(_out_cards_data, _out_card_count, GameConstants.WSK_ST_VALUE);
		int card_type = GameConstants.SXTH_CT_ERROR;
		card_type = table._logic.GetCardType(_out_cards_data, _out_card_count);
		if (card_type == GameConstants.SXTH_CT_ERROR) {
			return card_type;
		}
		if (table._turn_out_card_count != 0) {
			if (!table._logic.CompareCard_WSK(table._turn_out_card_data, _out_cards_data, table._turn_out_card_count,
					_out_card_count)) {
				return GameConstants.SXTH_CT_ERROR;
			}
		}
		if (!table._logic.RemoveCard(_out_cards_data, _out_card_count, table.GRR._cards_data[_out_card_player],
				table.GRR._card_count[_out_card_player])) {
			return GameConstants.SXTH_CT_ERROR;
		}

		return card_type;
	}

	@Override
	public boolean handler_player_be_in_room(WSKTable_SXTH table, int seat_index) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_SXTH_RECONNECT_DATA);

		TableResponse_Sxth.Builder tableResponse = TableResponse_Sxth.newBuilder();
		table.load_player_info_data_reconnect(tableResponse);
		tableResponse.setRoomInfo(table.getRoomInfo());

		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(table._current_player);
		tableResponse.setPrevPlayer(table._prev_palyer);
		tableResponse.setPrOutCardPlayer(table._out_card_player);
		tableResponse.setPrCardsCount(table._turn_out_card_count);
		tableResponse.setPrOutCardType(table._turn_out_card_type);

		if (seat_index == table._current_player) {
			int tip_out_card[][] = new int[table.GRR._card_count[seat_index] * 10][table.GRR._card_count[seat_index]];
			int tip_out_count[] = new int[table.GRR._card_count[seat_index] * 10];
			int tip_type_count = 0;
			tip_type_count = table._logic.search_out_card(table.GRR._cards_data[seat_index],
					table.GRR._card_count[seat_index], table._turn_out_card_data, table._turn_out_card_count,
					tip_out_card, tip_out_count, tip_type_count);
			for (int i = 0; i < tip_type_count; i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				int card_type = table._logic.GetCardType(tip_out_card[i], tip_out_count[i]);
				for (int j = 0; j < tip_out_count[i]; j++) {
					cards_card.addItem(tip_out_card[i][j]);
				}
				tableResponse.addUserCanOutType(card_type);
				tableResponse.addUserCanOutData(cards_card);
				tableResponse.addUserCanOutCount(tip_out_count[i]);

			}
		} else {
			int tip_out_card[][] = new int[table.GRR._card_count[seat_index] * 10][table.GRR._card_count[seat_index]];
			int tip_out_count[] = new int[table.GRR._card_count[seat_index] * 10];
			int tip_type_count = 0;
			tip_type_count = table._logic.search_out_card(table.GRR._cards_data[seat_index],
					table.GRR._card_count[seat_index], null, 0, tip_out_card, tip_out_count, tip_type_count);
			for (int i = 0; i < tip_type_count; i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				int card_type = table._logic.GetCardType(tip_out_card[i], tip_out_count[i]);
				for (int j = 0; j < tip_out_count[i]; j++) {
					cards_card.addItem(tip_out_card[i][j]);
				}
				tableResponse.addUserCanOutType(card_type);
				tableResponse.addUserCanOutData(cards_card);
				tableResponse.addUserCanOutCount(tip_out_count[i]);

			}
		}

		if (table._turn_out_card_count == 0 && seat_index == table._current_player) {
			tableResponse.setIsFirstOut(1);
		} else {
			tableResponse.setIsFirstOut(0);
		}
		for (int i = 0; i < table._turn_out_card_count; i++) {
			tableResponse.addPrCardsData(table._turn_out_card_data[i]);
		}
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (seat_index == i || (table._friend_seat[seat_index] == i && table.GRR._card_count[seat_index] == 0)) {
				tableResponse.addCardCount(table.GRR._card_count[i]);
			} else if (table.GRR._card_count[i] > 15) {
				tableResponse.addCardCount(-1);
			} else if (table.GRR._card_count[i] <= 15 && table.GRR._card_count[i] > 0) {
				tableResponse.addCardCount(-2);
			} else {
				tableResponse.addCardCount(0);
			}

			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder cur_out_cards = Int32ArrayResponse.newBuilder();
			if (i == seat_index) {
				for (int j = 0; j < table.GRR._card_count[i]; j++) {
					if (j == 0 || table._logic.GetCardValue(table.GRR._cards_data[i][j - 1]) != table._logic
							.GetCardValue(table.GRR._cards_data[i][j])) {
						int same_count = 1;
						for (int x = j + 1; x < table.GRR._card_count[i]; x++) {
							if (table._logic.GetCardValue(table.GRR._cards_data[i][j]) == table._logic
									.GetCardValue(table.GRR._cards_data[i][x])) {
								same_count++;
							} else {
								break;
							}
						}
						cards.addItem(table.GRR._cards_data[i][j] + same_count * 0x100);
					} else {
						cards.addItem(table.GRR._cards_data[i][j]);
					}
				}
			}
			for (int j = 0; j < table._cur_out_card_count[i]; j++) {
				cur_out_cards.addItem(table._cur_out_card_data[i][j]);
			}
			tableResponse.addCardType(table._cur_out_car_type[i]);
			tableResponse.addOutCardsCount(table._cur_out_card_count[i]);
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
		table.Refresh_user_get_score(seat_index, false);

		if (table._game_status == GameConstants.GS_SXTH_PLAY) {
			if (table._out_card_times[table.GRR._banker_player] == 0) {
				// 游戏计算开始奖励分
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
					table._logic.AnalysebCardDataToIndex(table.GRR._cards_data[i], table.GRR._card_count[i],
							card_index);
					if (table.has_rule(GameConstants.GAME_RULE_SXTH_EIGHT_JI)) {
						if (card_index.card_index[14] == 4 && card_index.card_index[13] == 4) {
							table.send_effect_type(i, table.EFFECT_EIGHT_KING, 0, GameConstants.INVALID_SEAT);
						} else if (card_index.card_index[14] == 3 && card_index.card_index[13] == 3) {
							table.send_effect_type(i, table.EFFECT_SIX_KING, 0, GameConstants.INVALID_SEAT);
						} else if (card_index.card_index[14] == 4 || card_index.card_index[13] == 4) {
							table.send_effect_type(i, table.EFFECT_FOUR_KING, 0, GameConstants.INVALID_SEAT);
						}
						for (int j = 0; j < GameConstants.WSK_MAX_INDEX; j++) {
							if (card_index.card_index[j] == 12) {
								table.send_effect_type(i, table.EFFECT_TWELVE_GOD, 0, GameConstants.INVALID_SEAT);
							}
						}
					} else {
						if (card_index.card_index[14] == 4 && card_index.card_index[13] == 4) {
							table.send_effect_type(i, table.EFFECT_EIGHT_KING, 0, GameConstants.INVALID_SEAT);
						} else if (card_index.card_index[14] == 3 && card_index.card_index[13] == 3) {
							table.send_effect_type(i, table.EFFECT_SIX_KING, 0, GameConstants.INVALID_SEAT);
						} else if (card_index.card_index[14] == 2 && card_index.card_index[13] == 2) {
							table.send_effect_type(i, table.EFFECT_FOUR_KING, 0, GameConstants.INVALID_SEAT);
						}
						for (int j = 0; j < GameConstants.WSK_MAX_INDEX; j++) {
							if (card_index.card_index[j] == 12) {
								table.send_effect_type(i, table.EFFECT_TWELVE_GOD, 0, GameConstants.INVALID_SEAT);
							}
						}
					}

				}
			}
			// 显示出牌
			table.operate_out_card(_out_card_player, _out_card_count, _out_cards_data, _out_type, seat_index, false);
		}

		return true;
	}

}
