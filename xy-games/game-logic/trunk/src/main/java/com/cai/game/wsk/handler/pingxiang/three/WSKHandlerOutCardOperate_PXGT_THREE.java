package com.cai.game.wsk.handler.pingxiang.three;

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
import protobuf.clazz.pxgt.pxgtkRsp.TableResponse_pxgt;

public class WSKHandlerOutCardOperate_PXGT_THREE extends WSKHandlerOutCardOperate<WSKTable_PXGT_THREE> {

	@Override
	public void exe(WSKTable_PXGT_THREE table) {
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
		if (card_type == WSKConstants.PXGT_CT_ERROR) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return;
		}

		table._out_card_times[_out_card_player]++;
		table.GRR._card_count[_out_card_player] -= _out_card_count;

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
				table._cur_out_car_type[next_player] = WSKConstants.PXGT_CT_ERROR;
				Arrays.fill(table._cur_out_card_data[next_player], GameConstants.INVALID_CARD);
				next_player = (next_player + 1) % table.getTablePlayerNumber();
			} else {
				break;
			}
		}
		table._current_player = next_player;
		table._cur_out_card_count[next_player] = 0;
		Arrays.fill(table._cur_out_card_data[next_player], GameConstants.INVALID_CARD);
		int turn_score = 0;
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

			if (table._chuwan_shunxu[1] != GameConstants.INVALID_SEAT) {
				if (table._seat_team[table._chuwan_shunxu[1]] == table._seat_team[table._chuwan_shunxu[0]]) {
					table._current_player = GameConstants.INVALID_SEAT;
					turn_score = table._turn_have_score;
					table._get_score[table._out_card_player] += table._turn_have_score;
					table._player_info[table._chuwan_shunxu[1]]._guan_men_times++;
					table._player_info[table._chuwan_shunxu[0]]._guan_men_times++;
					table._get_score[table._chuwan_shunxu[0]] += 80;
					int team_score = 0;
					int other_seat = GameConstants.INVALID_SEAT;
					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						if (table._seat_team[i] != table._seat_team[table._chuwan_shunxu[0]]) {
							team_score += table._get_score[i];
							table._get_score[i] = 0;
							other_seat = i;
						}
					}
					team_score -= 80;
					if (team_score < 0) {
						team_score = 0;
					}
					table._get_score[other_seat] = team_score;
					GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._chuwan_shunxu[0],
							GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
				} else if (table._chuwan_shunxu[2] != GameConstants.INVALID_SEAT) {
					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						if (table.GRR._card_count[i] != 0) {
							table._chuwan_shunxu[3] = i;
							break;
						}
					}
					if (table._seat_team[table._chuwan_shunxu[0]] != table._seat_team[table._chuwan_shunxu[3]]) {
						table._get_score[table._chuwan_shunxu[0]] += 40;
						int team_score = 0;
						for (int i = 0; i < table.getTablePlayerNumber(); i++) {
							if (table._seat_team[i] == table._seat_team[table._chuwan_shunxu[3]]) {
								team_score += table._get_score[i];
								table._get_score[i] = 0;
							}
						}
						team_score -= 40;
						if (team_score < 0) {
							team_score = 0;
						}
						table._get_score[table._chuwan_shunxu[3]] = team_score;
					}
					table._current_player = GameConstants.INVALID_SEAT;
					turn_score = table._turn_have_score;
					table._get_score[table._out_card_player] += table._turn_have_score;
					GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._chuwan_shunxu[0],
							GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
				}

			} else if (table._chuwan_shunxu[2] != GameConstants.INVALID_SEAT) {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (table.GRR._card_count[i] != 0) {
						table._chuwan_shunxu[3] = i;
						break;
					}
				}
				if (table._seat_team[table._chuwan_shunxu[0]] != table._seat_team[table._chuwan_shunxu[3]]) {
					table._get_score[table._chuwan_shunxu[0]] += 40;
					int team_score = 0;
					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						if (table._seat_team[i] == table._seat_team[table._chuwan_shunxu[3]]) {
							team_score += table._get_score[i];
							table._get_score[i] = 0;
						}
					}
					team_score -= 40;
					if (team_score < 0) {
						team_score = 0;
					}
					table._get_score[table._chuwan_shunxu[3]] = team_score;
				}
				table._current_player = GameConstants.INVALID_SEAT;
				turn_score = table._turn_have_score;
				table._get_score[table._out_card_player] += table._turn_have_score;
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._chuwan_shunxu[0],
						GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
			}
		}
		table.RefreshCard(GameConstants.INVALID_SEAT);
		// 显示出牌
		table.operate_out_card(table._out_card_player, table._turn_out_card_count, table._turn_out_card_data,
				table._turn_out_card_type, GameConstants.INVALID_SEAT, false);

		if (card_type == WSKConstants.PXGT_CT_BOMB) {
			if (_out_card_count > 4) {
				if (table.has_rule(WSKConstants.GAME_RULE_SWK_AWARD_DOUBLE)) {
					table._xi_qian_score[_out_card_player] += Math.pow(2, (_out_card_count - 5))
							* table._boom_reward_cell;
					table.Send_Reward_effect((_out_card_count - 4) * table._boom_reward_cell, _out_card_player);
					table._player_info[_out_card_player]._boom_num++;
				} else {
					table._xi_qian_score[_out_card_player] += (_out_card_count - 4) * table._boom_reward_cell;
					table.Send_Reward_effect((_out_card_count - 4) * table._boom_reward_cell, _out_card_player);
					table._player_info[_out_card_player]._boom_num++;
				}

			}
		} else if (card_type >= WSKConstants.PXGT_CT_GUN_TONG_DC && _out_card_count > 6) {
			if (table.has_rule(WSKConstants.GAME_RULE_SWK_AWARD_DOUBLE)) {
				table._xi_qian_score[_out_card_player] += Math.pow(2, (_out_card_count / 3 - 2))
						* table._boom_reward_cell;
				table.Send_Reward_effect((_out_card_count / 3 - 1) * table._boom_reward_cell, _out_card_player);
				table._player_info[_out_card_player]._boom_num++;
			} else {
				table._xi_qian_score[_out_card_player] += (_out_card_count / 3 - 1) * table._boom_reward_cell;
				table.Send_Reward_effect((_out_card_count / 3 - 1) * table._boom_reward_cell, _out_card_player);
				table._player_info[_out_card_player]._boom_num++;
			}

		}

		if (turn_score > 0) {
			table.Refresh_user_get_score(turn_score, _out_card_player, 1, GameConstants.INVALID_SEAT);
		} else {
			table.Refresh_user_get_score(0, 0, 0, GameConstants.INVALID_SEAT);
		}

		// 刚出完刷新友方
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table.Refresh_Dui_You_Card(i);
		}
		// 友方出完刷新手牌
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._chuwan_shunxu[i] != GameConstants.INVALID_SEAT
					&& table._friend_seat[table._chuwan_shunxu[i]] == table._out_card_player) {
				table.Refresh_Dui_You_Card(table._chuwan_shunxu[i]);
			}
		}
	}

	public void user_pass_card(WSKTable_PXGT_THREE table) {
		if (table._turn_out_card_count == 0) {
			return;
		}
		// 清空接下去出牌玩家出牌数据
		int next_player = (_out_card_player + 1) % table.getTablePlayerNumber();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.GRR._card_count[next_player] == 0 && next_player != table._out_card_player) {
				if (table._cur_out_card_count[next_player] > 0) {
					table._current_player = GameConstants.INVALID_SEAT;
					table._cur_out_card_count[next_player] = 0;
					table._cur_out_car_type[next_player] = WSKConstants.PXGT_CT_ERROR;
					table.operate_out_card(next_player, 0, null, -2, GameConstants.INVALID_SEAT, false);
				}
				next_player = (next_player + 1) % table.getTablePlayerNumber();
			} else {
				break;
			}
		}
		table._cur_out_car_type[_out_card_player] = WSKConstants.PXGT_CT_PASS;
		// 一轮不出
		int turn_score = 0;
		if (next_player == table._out_card_player) {
			// 清空桌面牌分
			turn_score = table._turn_have_score;
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
				table._cur_out_car_type[next_player] = WSKConstants.PXGT_CT_ERROR;
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

			}

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._cur_out_card_count[i] = 0;
				table._cur_out_car_type[i] = WSKConstants.PXGT_CT_ERROR;
				Arrays.fill(table._cur_out_card_data[i], GameConstants.INVALID_CARD);
			}
		} else {
			table._current_player = next_player;
			table._prev_palyer = _out_card_player;
			table._cur_out_card_count[table._current_player] = 0;
			table._cur_out_car_type[table._current_player] = WSKConstants.PXGT_CT_ERROR;
			Arrays.fill(table._cur_out_card_data[table._current_player], GameConstants.INVALID_CARD);
		}

		// 显示出牌
		table.operate_out_card(_out_card_player, 0, null, GameConstants.WSK_GF_CT_PASS, GameConstants.INVALID_SEAT,
				false);

		if (table._turn_out_card_count == 0) {
			table._is_shou_chu = 1;
		}
		if (turn_score > 0) {
			table.Refresh_user_get_score(turn_score, table._out_card_player, 1, GameConstants.INVALID_SEAT);
		}
		// 刚出完刷新友方
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table.Refresh_Dui_You_Card(i);
		}
	}

	public int adjust_out_card_right(WSKTable_PXGT_THREE table) {
		int card_type = WSKConstants.PXGT_CT_ERROR;
		boolean isLast = false;
		if (table.GRR._card_count[_out_card_player] == _out_card_count) {
			isLast = true;
		}
		card_type = table._logic.GetCardType(_out_cards_data, _out_card_count, isLast);
		if ((card_type == WSKConstants.PXGT_CT_PLANE || card_type == WSKConstants.PXGT_CT_THREE
				|| card_type == WSKConstants.PXGT_CT_THREE_TAKE_TWO) && _out_card_count % 5 != 0
				&& _out_card_count != table.GRR._card_count[_out_card_player]) {
			return WSKConstants.PXGT_CT_ERROR;
		}
		if (card_type == WSKConstants.PXGT_CT_ERROR) {
			return card_type;
		}
		if (table._turn_out_card_count == 0
				&& (card_type == GameConstants.WSK_GF_CT_PLANE || card_type == GameConstants.WSK_GF_CT_PLANE_LOST)) {
			table._turn_three_link_num = table._logic.get_three_link_count(_out_cards_data, _out_card_count, card_type);
		}
		table._logic.sort_card_date_list_by_type(_out_cards_data, _out_card_count, card_type,
				table._turn_three_link_num);

		if (table._turn_out_card_count > 0) {
			if (!table._logic.CompareCard_WSK(table._turn_out_card_data, _out_cards_data, table._turn_out_card_count,
					_out_card_count, table._turn_three_link_num)) {
				return WSKConstants.PXGT_CT_ERROR;
			}
		}

		if (!table._logic.RemoveCard(_out_cards_data, _out_card_count, table.GRR._cards_data[_out_card_player],
				table.GRR._card_count[_out_card_player])) {
			return WSKConstants.PXGT_CT_ERROR;
		}

		return card_type;
	}

	@Override
	public boolean handler_player_be_in_room(WSKTable_PXGT_THREE table, int seat_index) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(WSKMsgConstants.RESPONSE_PXGT_RECONNECT_DATA);

		TableResponse_pxgt.Builder tableResponse = TableResponse_pxgt.newBuilder();
		table.load_player_info_data_reconnect(tableResponse);
		tableResponse.setRoomInfo(table.getRoomInfo());

		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(table._current_player);
		tableResponse.setPrevPlayer(table._prev_palyer);
		tableResponse.setPrOutCardPlayer(table._out_card_player);
		tableResponse.setPrCardsCount(table._turn_out_card_count);
		tableResponse.setPrOutCardType(table._turn_out_card_type);
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
			if (seat_index == i) {
				for (int j = 0; j < table.GRR._card_count[i]; j++) {
					cards.addItem(table.GRR._cards_data[i][j]);
				}
			}
			if (table._jiao_pai_seat != GameConstants.INVALID_SEAT) {
				if (i == table._jiao_pai_seat) {
					tableResponse.addJiaoCardData(table._jiao_pai_card);
				} else if (table._friend_seat[table._jiao_pai_seat] == i) {
					tableResponse.addJiaoCardData(table._jiao_pai_card);
				} else {
					tableResponse.addJiaoCardData(GameConstants.INVALID_CARD);
				}
			} else {
				tableResponse.addJiaoCardData(GameConstants.INVALID_CARD);
			}

			if (seat_index == table._current_player) {
				int tip_out_card[][] = new int[table.GRR._card_count[seat_index]
						* 2][table.GRR._card_count[seat_index]];
				int tip_out_count[] = new int[table.GRR._card_count[seat_index] * 2];
				int tip_type_count = 0;
				tip_type_count = table._logic.search_out_card(table.GRR._cards_data[seat_index],
						table.GRR._card_count[seat_index], table._turn_out_card_data, table._turn_out_card_count,
						table._turn_three_link_num, tip_out_card, tip_out_count, tip_type_count);
				for (int j = 0; j < 1; j++) {
					for (int x = 0; x < tip_out_count[j]; x++) {
						tableResponse.addUserCanOutData(tip_out_card[j][x]);
					}
					tableResponse.setUserCanOutCount(tip_out_count[j]);
				}
			}
			for (int j = 0; j < table._cur_out_card_count[i]; j++) {
				cur_out_cards.addItem(table._cur_out_card_data[i][j]);
			}
			if (table._cur_out_car_type[i] == WSKConstants.PXGT_CT_PASS) {
				tableResponse.addPlayerPass(1);
			} else {
				tableResponse.addPlayerPass(0);
			}
			tableResponse.addCardType(table._cur_out_car_type[i]);
			tableResponse.addOutCardsData(cur_out_cards);
			tableResponse.addCardsData(cards);
			tableResponse.addWinOrder(table._chuwan_shunxu[i]);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));
		table.send_response_to_player(seat_index, roomResponse);
		table.Refresh_user_get_score(0, 0, 0, seat_index);
		table.operate_out_card(table._out_card_player, table._turn_out_card_count, table._turn_out_card_data,
				table._turn_out_card_type, seat_index, false);
		table.Refresh_Dui_You_Card(seat_index);
		return true;
	}

}
