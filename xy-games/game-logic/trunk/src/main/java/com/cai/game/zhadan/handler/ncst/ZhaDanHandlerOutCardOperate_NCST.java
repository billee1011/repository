package com.cai.game.zhadan.handler.ncst;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.PBUtil;
import com.cai.domain.SheduleArgs;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.zhadan.ZhaDanConstants;
import com.cai.game.zhadan.ZhaDanMsgConstants;
import com.cai.game.zhadan.handler.ZhaDanHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.ncst.NcstRsp.TableResponse_Ncst;

public class ZhaDanHandlerOutCardOperate_NCST extends ZhaDanHandlerOutCardOperate<ZhaDanTable_NCST> {

	@Override
	public void exe(ZhaDanTable_NCST table) {
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
		if (card_type == ZhaDanConstants.NCST_CT_ERROR) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return;
		}
		if (card_type == ZhaDanConstants.NCST_CT_BOMB) {
			table._player_info[_out_card_player]._boom_num++;
		}

		int score_effect_type = 0;
		_out_type = card_type;

		table.GRR._card_count[_out_card_player] -= _out_card_count;

		table._logic.SortCardList(table.GRR._cards_data[_out_card_player], table.GRR._card_count[_out_card_player],
				table._score_type[_out_card_player]);
		table._out_card_times[_out_card_player]++;

		table._turn_have_score += table._logic.GetCardScore(_out_cards_data, _out_card_count);
		table._turn_out_card_type = card_type;
		table._turn_out_card_count = _out_card_count;
		table._out_card_player = _out_card_player;
		table._cur_out_card_count[_out_card_player] = _out_card_count;
		table._cur_out_card_type[_out_card_player] = card_type;
		Arrays.fill(table._turn_out_card_data, GameConstants.INVALID_CARD);
		for (int i = 0; i < _out_card_count; i++) {
			table._turn_out_card_data[i] = _out_cards_data[i];
			table._cur_out_card_data[_out_card_player][i] = table._turn_out_card_data[i];
			_cards[i] = _out_cards_data[i];
		}
		_card_count = _out_card_count;
		_seat_index = _out_card_player;
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
				table._cur_out_card_type[next_player] = GameConstants.SXTH_CT_ERROR;
				Arrays.fill(table._cur_out_card_data[next_player], GameConstants.INVALID_CARD);
				next_player = (next_player + 1) % table.getTablePlayerNumber();
			} else {
				break;
			}
		}
		table._current_player = next_player;
		table._cur_out_card_count[next_player] = 0;
		table._cur_out_card_type[next_player] = GameConstants.SXTH_CT_ERROR;
		Arrays.fill(table._cur_out_card_data[next_player], GameConstants.INVALID_CARD);

		boolean is_round_end = false;
		if (table.GRR._card_count[_out_card_player] == 0) {
			int delay = 3;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._win_order[i] == GameConstants.INVALID_SEAT) {
					table._win_order[i] = _out_card_player;
					break;
				}
			}

			table._cur_banker = table._win_order[0];
			boolean is_end = false;
			if (table._round != 1 && table._win_order[0] != GameConstants.INVALID_SEAT
					&& table._win_order[1] == GameConstants.INVALID_SEAT) {
				int team_one_score = 0;

				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (table._seat_team[i] == table._seat_team[table._win_order[0]]) {
						team_one_score += table._prv_get_score[i];
					}
				}
				team_one_score += table._get_score[table._win_order[0]];
				if (team_one_score >= 300 * table._round / 2) {
					table._current_player = GameConstants.INVALID_SEAT;
					GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), GameConstants.INVALID_SEAT,
							GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
					is_end = true;
				}
			}
			if (!is_end) {
				if (table._win_order[2] != GameConstants.INVALID_SEAT) {
					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						if (table.GRR._card_count[i] != 0) {
							for (int j = 0; j < table.getTablePlayerNumber(); j++) {

								if (table._win_order[j] == GameConstants.INVALID_SEAT) {
									table._win_order[j] = i;
									break;
								}
							}
						}

					}
					if (table._seat_team[table._win_order[0]] == table._seat_team[table._win_order[2]]) {
						table._get_score[table._win_order[0]] += table._get_score[table._win_order[3]]
								+ table._logic.GetCardScore(table._di_pai, table._di_pai_count);
						table._get_score[table._win_order[2]] += table._logic.GetCardScore(
								table.GRR._cards_data[table._win_order[3]], table.GRR._card_count[table._win_order[3]])
								+ table._turn_have_score;
						table._get_score[table._win_order[3]] = 0;
						int shang_you_score = table._get_score[table._win_order[0]]
								+ table._get_score[table._win_order[2]] + table._prv_get_score[table._win_order[0]]
								+ table._prv_get_score[table._win_order[2]];
						int xia_you_score = table._get_score[table._win_order[1]]
								+ table._get_score[table._win_order[3]] + table._prv_get_score[table._win_order[1]]
								+ table._prv_get_score[table._win_order[3]];
						if (table._round == 1) {
							if (shang_you_score >= 200 || xia_you_score >= 200) {
								table._current_player = GameConstants.INVALID_SEAT;
								GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), GameConstants.INVALID_SEAT,
										GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
							} else {
								is_round_end = true;
								table._current_player = GameConstants.INVALID_SEAT;
								int end_score[] = new int[table.getTablePlayerNumber()];
								table.cal_score_reward(end_score);
								table.send_round_finish();
								table.schedule(table.ID_TIMER_ROUND_FINISH, SheduleArgs.newArgs(), 3000);

								for (int i = 0; i < table.getTablePlayerNumber(); i++) {
									table._cur_out_card_count[i] = 0;
									table._cur_out_card_type[i] = GameConstants.SXTH_CT_ERROR;
									Arrays.fill(table._cur_out_card_data[i], GameConstants.INVALID_CARD);
								}
								_out_card_count = 0;
							}
						} else {
							if (shang_you_score >= 300 * table._round / 2 || xia_you_score >= 300 * table._round / 2) {
								table._current_player = GameConstants.INVALID_SEAT;
								GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), GameConstants.INVALID_SEAT,
										GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
							} else {
								is_round_end = true;
								table._current_player = GameConstants.INVALID_SEAT;
								int end_score[] = new int[table.getTablePlayerNumber()];
								table.cal_score_reward(end_score);
								table.send_round_finish();

								table.schedule(table.ID_TIMER_ROUND_FINISH, SheduleArgs.newArgs(), 3000);

								for (int i = 0; i < table.getTablePlayerNumber(); i++) {
									table._cur_out_card_count[i] = 0;
									table._cur_out_card_type[i] = GameConstants.SXTH_CT_ERROR;
									Arrays.fill(table._cur_out_card_data[i], GameConstants.INVALID_CARD);
								}
								_out_card_count = 0;
							}
						}
					} else {
						table._get_score[table._win_order[0]] += table._get_score[table._win_order[3]]
								+ table._logic.GetCardScore(table._di_pai, table._di_pai_count);
						table._get_score[table._win_order[2]] += table._logic.GetCardScore(
								table.GRR._cards_data[table._win_order[3]], table.GRR._card_count[table._win_order[3]])
								+ table._turn_have_score;
						table._get_score[table._win_order[3]] = 0;
						int shang_you_score = table._get_score[table._win_order[0]]
								+ table._get_score[table._win_order[3]] + table._prv_get_score[table._win_order[0]]
								+ table._prv_get_score[table._win_order[3]];
						int xia_you_score = table._get_score[table._win_order[1]]
								+ table._get_score[table._win_order[2]] + table._prv_get_score[table._win_order[1]]
								+ table._prv_get_score[table._win_order[2]];
						if (table._round == 1) {
							if (shang_you_score >= 200 || xia_you_score >= 200) {
								table._current_player = GameConstants.INVALID_SEAT;
								GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), GameConstants.INVALID_SEAT,
										GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
							} else {
								is_round_end = true;
								table._current_player = GameConstants.INVALID_SEAT;
								int end_score[] = new int[table.getTablePlayerNumber()];
								table.cal_score_reward(end_score);
								table.send_round_finish();
								table.schedule(table.ID_TIMER_ROUND_FINISH, SheduleArgs.newArgs(), 3000);

								for (int i = 0; i < table.getTablePlayerNumber(); i++) {
									table._cur_out_card_count[i] = 0;
									table._cur_out_card_type[i] = GameConstants.SXTH_CT_ERROR;
									Arrays.fill(table._cur_out_card_data[i], GameConstants.INVALID_CARD);
								}
								_out_card_count = 0;
							}
						} else {
							if (shang_you_score >= 300 * table._round / 2 || xia_you_score >= 300 * table._round / 2) {
								table._current_player = GameConstants.INVALID_SEAT;
								GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), GameConstants.INVALID_SEAT,
										GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
							} else {
								is_round_end = true;
								table._current_player = GameConstants.INVALID_SEAT;
								int end_score[] = new int[table.getTablePlayerNumber()];
								table.cal_score_reward(end_score);
								table.send_round_finish();

								table.schedule(table.ID_TIMER_ROUND_FINISH, SheduleArgs.newArgs(), 3000);

								for (int i = 0; i < table.getTablePlayerNumber(); i++) {
									table._cur_out_card_count[i] = 0;
									table._cur_out_card_type[i] = GameConstants.SXTH_CT_ERROR;
									Arrays.fill(table._cur_out_card_data[i], GameConstants.INVALID_CARD);
								}
								_out_card_count = 0;
							}
						}
					}

				} else if (table._win_order[1] != GameConstants.INVALID_SEAT) {
					if (table._seat_team[table._win_order[1]] == table._seat_team[table._win_order[0]]) {
						table._current_player = GameConstants.INVALID_SEAT;
						GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._win_order[0],
								GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
					}

				}
			}

		}

		// 显示出牌
		table.operate_out_card(table._out_card_player, table._turn_out_card_count, table._turn_out_card_data,
				table._turn_out_card_type, GameConstants.INVALID_SEAT, false);

		table.Refresh_user_get_score(GameConstants.INVALID_SEAT, GameConstants.INVALID_SEAT, 0);

	}

	public void user_pass_card(ZhaDanTable_NCST table) {
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
		table._cur_out_card_type[_out_card_player] = _out_type;
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
				table._cur_out_card_type[next_player] = GameConstants.SXTH_CT_ERROR;
				Arrays.fill(table._cur_out_card_data[next_player], GameConstants.INVALID_CARD);

			} else {
				table._current_player = next_player;
				table._cur_out_card_count[table._current_player] = 0;
				table._cur_out_card_type[table._current_player] = GameConstants.SXTH_CT_ERROR;
				Arrays.fill(table._cur_out_card_data[table._current_player], GameConstants.INVALID_CARD);
			}
			Arrays.fill(table._turn_out_card_data, GameConstants.INVALID_CARD);
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._cur_out_card_count[i] = 0;
				table._cur_out_card_type[i] = GameConstants.SXTH_CT_ERROR;
				Arrays.fill(table._cur_out_card_data[i], GameConstants.INVALID_CARD);
			}
		} else {
			table._current_player = next_player;
			table._cur_out_card_count[table._current_player] = 0;
			table._cur_out_card_type[table._current_player] = GameConstants.SXTH_CT_ERROR;
			Arrays.fill(table._cur_out_card_data[table._current_player], GameConstants.INVALID_CARD);
		}

		// 显示出牌
		table.operate_out_card(_out_card_player, 0, null, GameConstants.SXTH_CT_PASS, GameConstants.INVALID_SEAT,
				false);

		if (has_score) {
			table.Refresh_user_get_score(table._out_card_player, GameConstants.INVALID_SEAT, 1);
		}

	}

	public int adjust_out_card_right(ZhaDanTable_NCST table) {
		table._logic.SortCardList(_out_cards_data, _out_card_count, GameConstants.WSK_ST_VALUE);
		int card_type = ZhaDanConstants.NCST_CT_ERROR;
		card_type = table._logic.GetCardType(_out_cards_data, _out_card_count);
		if (card_type == ZhaDanConstants.NCST_CT_ERROR) {
			return card_type;
		}
		if (table._turn_out_card_count != 0) {
			if (!table._logic.CompareCard(table._turn_out_card_data, _out_cards_data, table._turn_out_card_count,
					_out_card_count)) {
				return ZhaDanConstants.NCST_CT_ERROR;
			}
		}
		if (!table._logic.RemoveCard(_out_cards_data, _out_card_count, table.GRR._cards_data[_out_card_player],
				table.GRR._card_count[_out_card_player])) {
			return ZhaDanConstants.NCST_CT_ERROR;
		}

		return card_type;
	}

	@Override
	public boolean handler_player_be_in_room(ZhaDanTable_NCST table, int seat_index) {
		int team = -1;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._ming_pai_agree[i] == 1) {
				team = table._seat_team[i];
				break;
			}
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(ZhaDanMsgConstants.RESPONSE_NCST_RECONNECT_DATA);

		TableResponse_Ncst.Builder tableResponse = TableResponse_Ncst.newBuilder();
		table.load_player_info_data_reconnect(tableResponse);
		tableResponse.setRoomInfo(table.getRoomInfo());

		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(table._current_player);
		tableResponse.setPrOutCardPlayer(table._out_card_player);
		tableResponse.setPrCardsCount(table._turn_out_card_count);
		tableResponse.setPrOutCardType(table._turn_out_card_type);
		tableResponse.setRound(table._round);
		tableResponse.setGameCell((int) table.game_cell);
		if (seat_index == table._current_player) {
			int tip_out_card[][] = new int[table.GRR._card_count[seat_index] * 2][table.GRR._card_count[seat_index]];
			int tip_out_count[] = new int[table.GRR._card_count[seat_index] * 2];
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

			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder cur_out_cards = Int32ArrayResponse.newBuilder();
			if (seat_index == i || table._is_ming_pai[i] == 1 || (table.GRR._card_count[seat_index] == 0
					&& table._seat_team[i] == table._seat_team[seat_index])) {
				for (int j = 0; j < table.GRR._card_count[i]; j++) {
					cards.addItem(table.GRR._cards_data[i][j]);
				}
			} else if (table._seat_team[i] == team) {
				for (int j = 0; j < table.GRR._card_count[i]; j++) {
					cards.addItem(table.GRR._cards_data[i][j]);
				}
			}
			if (table.GRR._card_count[i] <= 11 || i == seat_index) {
				tableResponse.addCardCount(table.GRR._card_count[i]);
			} else {
				tableResponse.addCardCount(-1);
			}
			for (int j = 0; j < table._cur_out_card_count[i]; j++) {
				cur_out_cards.addItem(table._cur_out_card_data[i][j]);
			}
			tableResponse.addCardType(table._cur_out_card_type[i]);
			tableResponse.addOutCardsCount(table._cur_out_card_count[i]);
			tableResponse.addOutCardsData(cur_out_cards);
			tableResponse.addCardsData(cards);
			boolean is_out_finish = false;
			for (int j = 0; j < table.getTablePlayerNumber(); j++) {
				if (table._win_order[j] == i) {
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

		table.Refresh_user_get_score(GameConstants.INVALID_SEAT, seat_index, 0);

		// 显示出牌
		table.operate_out_card(_seat_index, _card_count, _cards, _out_type, seat_index, false);

		return true;
	}

}
