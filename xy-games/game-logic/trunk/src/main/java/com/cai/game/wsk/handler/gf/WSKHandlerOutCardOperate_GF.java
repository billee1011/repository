package com.cai.game.wsk.handler.gf;

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
import protobuf.clazz.gfWsk.gfWskRsp.TableResponse_Wsk_GF;

public class WSKHandlerOutCardOperate_GF extends WSKHandlerOutCardOperate<WSKTable_GF> {

	@Override
	public void exe(WSKTable_GF table) {
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
		if (card_type == GameConstants.WSK_GF_CT_ERROR) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return;
		}

		// 桌面牌分
		table_pai_socre(table);

		table.GRR._card_count[_out_card_player] -= _out_card_count;
		table.out_card_sort_card_by_data(_out_card_player, _out_cards_data, _out_card_count);

		// 罚王检测
		int total_wang_count = table._wang_count[_out_card_player];
		if (table._fei_wang_count[_out_card_player] < total_wang_count) {
			int wang_count = table._logic.Get_Wang_Count(_out_cards_data, _out_card_count);
			if (wang_count > 0 && (card_type <= GameConstants.WSK_GF_CT_510K_SC)) {
				for (int i = 0; i < _out_card_count; i++) {
					if (table._logic.GetCardValue(_out_cards_data[i]) == 14
							|| table._logic.GetCardValue(_out_cards_data[i]) == 15) {
						table._fei_wang_card[_out_card_player][table._fei_wang_count[_out_card_player]++] = _out_cards_data[i];
					}
				}
			}
		}

		// 罚王检测
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
			table._user_data_score[_out_card_player][table._user_data.get(_out_card_player)
					.getCardsDataCount()] = xian_qian_score;
			int c = card_type;
			if (table.has_rule(GameConstants.GAME_RULE_WSK_GF_TWO_SHENG_DANG)) {
				if (table._logic.GetCardValue(_out_cards_data[0]) == 2) {
					if (card_type <= GameConstants.WSK_GF_CT_BOMB_8) {
						c += 1;
					}
				}
			}
			if (table.has_rule(GameConstants.GAME_RULE_WSK_GF_SEVEN_SHENG_DANG)) {
				if (table._logic.GetCardValue(_out_cards_data[0]) == 7) {
					if (card_type <= GameConstants.WSK_GF_CT_BOMB_8) {
						c += 1;
					}
				}
			}
			if (table.has_rule(GameConstants.GAME_RULE_WSK_GF_J_SHENG_DANG)) {
				if (table._logic.GetCardValue(_out_cards_data[0]) == 11) {
					if (card_type <= GameConstants.WSK_GF_CT_BOMB_8) {
						c += 1;
					}
				}
			}
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int i = 0; i < _out_card_count && c > GameConstants.WSK_GF_CT_BOMB_4; i++) {
				cards_card.addItem(_out_cards_data[i]);
			}
			table._user_data.get(_out_card_player).addCardsData(cards_card);
		}

		table._turn_have_score += table._logic.GetCardScore(_out_cards_data, _out_card_count);
		table._turn_out_card_type = card_type;
		table._turn_out_card_count = _out_card_count;
		table._out_card_player = _out_card_player;
		table._prev_palyer = _out_card_player;
		table.prev_out_palyer = _out_card_player;
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
				table._current_player = GameConstants.INVALID_SEAT;
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._chuwan_shunxu[0],
						GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);

				// 没出出去的罚王
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (_out_card_player == i) {
						continue;
					}
					if (!table._logic.have_card_num(table.GRR._cards_data[i], table.GRR._card_count[i], 4)
							&& table.fa_wang[i] == 0) {
						for (int j = 0; j < table.GRR._card_count[i]; j++) {
							if (table.GRR._cards_data[i][j] == 0x4E || table.GRR._cards_data[i][j] == 0x4F
									|| table.GRR._cards_data[i][j] == 0x4E + 0x100
									|| table.GRR._cards_data[i][j] == 0x4F + 0x100) {
								table._fei_wang_card[i][table._fei_wang_count[i]++] = table.GRR._cards_data[i][j];
							}
						}
					}
				}
				// 内销
				if (!table.checkChunTian(_out_card_player)) {
					if (table.has_rule(GameConstants.GAME_RULE_WSK_GF_NEI_XIAO)
							&& _out_card_player == table.GRR._banker_player) {
						for (int i = 0; i < table.getTablePlayerNumber(); i++) {
							int score = table._logic.GetHandCardXianScore(table.GRR._cards_data[i],
									table.GRR._card_count[i], table.sheng_dang_biaozhi, table._user_data.get(i),
									table._user_data_score[i]);
							for (int j = 0; j < table.getTablePlayerNumber(); j++) {
								if (j == _out_card_player || i == j) {
									continue;
								}
								table._xi_qian_score[i] += score;
								table._xi_qian_score[j] -= score;
							}
						}
					} else if (!table.has_rule(GameConstants.GAME_RULE_WSK_GF_NEI_XIAO)
							&& _out_card_player != table.GRR._banker_player) {
						for (int i = 0; i < table.getTablePlayerNumber(); i++) {
							int score = table._logic.GetHandCardXianScore(table.GRR._cards_data[i],
									table.GRR._card_count[i], table.sheng_dang_biaozhi, table._user_data.get(i),
									table._user_data_score[i]);
							for (int j = 0; j < table.getTablePlayerNumber(); j++) {
								if (j == table.GRR._banker_player) {
									continue;
								}
								table._xi_qian_score[i] += score;
								table._xi_qian_score[j] -= score;
							}
						}
					}
				}

			} else {
				// 一二游
				if ((table._chuwan_shunxu[0] != GameConstants.INVALID_SEAT
						&& table._chuwan_shunxu[1] != GameConstants.INVALID_SEAT
						&& table._chuwan_shunxu[1] == table._friend_seat[table._chuwan_shunxu[0]])
						|| (table._chuwan_shunxu[0] != GameConstants.INVALID_SEAT
								&& table._chuwan_shunxu[1] != GameConstants.INVALID_SEAT
								&& table._chuwan_shunxu[2] != GameConstants.INVALID_SEAT)) {
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

					// 没出出去的罚王
					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						if (table._fei_wang_count[i] == 0) {
							if (!table._logic.have_card_num(table.GRR._cards_data[i], table.GRR._card_count[i], 4)
									&& table.fa_wang[i] == 0) {
								for (int j = 0; j < table.GRR._card_count[i]; j++) {
									if (table.GRR._cards_data[i][j] == 0x4E || table.GRR._cards_data[i][j] == 0x4F
											|| table.GRR._cards_data[i][j] == 0x4E + 0x100
											|| table.GRR._cards_data[i][j] == 0x4F + 0x100) {
										table._fei_wang_card[i][table._fei_wang_count[i]++] = table.GRR._cards_data[i][j];
									}
								}
							}
						}
					}
				}
			}
		}
		// 显示出牌
		table.operate_out_card(table._out_card_player, table._turn_out_card_count, table._turn_out_card_data,
				table._turn_out_card_type, GameConstants.INVALID_SEAT, false);

		// table.Refresh_pai_score(GameConstants.INVALID_SEAT);
		table.Refresh_user_get_score(GameConstants.INVALID_SEAT);

		if (!table._is_yi_da_san) {
			// 刚出完刷新友方
			if (table.GRR._card_count[_out_card_player] == 0) {
				table.Refresh_Dui_You_Card(_out_card_player);
			}
			// 友方出完刷新手牌
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._chuwan_shunxu[i] != GameConstants.INVALID_SEAT
						&& table._friend_seat[table._chuwan_shunxu[i]] == table._out_card_player) {
					table.Refresh_Dui_You_Card(table._chuwan_shunxu[i]);
				}
			}
		}
	}

	public void table_pai_socre(WSKTable_GF table) {
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

	public void user_pass_card(WSKTable_GF table) {
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
				boolean jie_feng = true;
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					for (int j = 0; j < table.GRR._card_count[i]; j++) {
						if (table.GRR._cards_data[i][j] == table._jiao_pai_card
								|| table.GRR._cards_data[i][j] - 0x100 == table._jiao_pai_card) {
							jie_feng = false;
						}
					}
				}
				int friend_seat = table._friend_seat[table._out_card_player];
				if (jie_feng) {
					next_player = friend_seat;
				} else {

					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						if (table.GRR._card_count[next_player] == 0) {
							next_player = (next_player + 1) % table.getTablePlayerNumber();
						} else {
							break;
						}
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

	public int adjust_out_card_right(WSKTable_GF table) {
		int card_type = GameConstants.WSK_GF_CT_ERROR;
		boolean isLast = false;
		if (table.GRR._card_count[_out_card_player] == _out_card_count) {
			isLast = true;
		}
		card_type = table._logic.GetCardType(_out_cards_data, _out_card_count, isLast);
		if (card_type == GameConstants.WSK_GF_CT_PLANE_LOST || card_type == GameConstants.WSK_GF_CT_THREE) {
			if (!isLast) {
				return GameConstants.WSK_GF_CT_ERROR;
			}
		}
		if (card_type == GameConstants.WSK_GF_CT_ERROR) {
			return card_type;
		}
		if (table._turn_out_card_count == 0
				&& (card_type == GameConstants.WSK_GF_CT_PLANE || card_type == GameConstants.WSK_GF_CT_PLANE_LOST)) {
			table._turn_three_link_num = table._logic.get_three_link_count(_out_cards_data, _out_card_count, card_type);
		}
		table._logic.sort_card_date_list_by_type(_out_cards_data, _out_card_count, card_type,
				table._turn_three_link_num);
		boolean pre_is_Last = false;
		if (table.prev_out_palyer != -1 && table.GRR._card_count[table.prev_out_palyer] == 0) {
			pre_is_Last = true;
		}
		if (table._turn_out_card_count != 0) {
			if (!table._logic.CompareCard_WSK(table._turn_out_card_data, _out_cards_data, table._turn_out_card_count,
					_out_card_count, pre_is_Last, isLast, table._turn_three_link_num)) {
				return GameConstants.WSK_GF_CT_ERROR;
			}
		}
		if (!table._logic.RemoveCard(_out_cards_data, _out_card_count, table.GRR._cards_data[_out_card_player],
				table.GRR._card_count[_out_card_player])) {
			return GameConstants.WSK_GF_CT_ERROR;
		}

		return card_type;
	}

	@Override
	public boolean handler_player_be_in_room(WSKTable_GF table, int seat_index) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_RECONNECT_DATA);

		TableResponse_Wsk_GF.Builder tableResponse = TableResponse_Wsk_GF.newBuilder();
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
			for (int j = 0; j < table._cur_out_card_count[i]; j++) {
				cur_out_cards.addItem(table._cur_out_card_data[i][j]);
			}
			for (int j = 0; j < table._fei_wang_count[i]; j++) {
				wang_cards.addItem(table._fei_wang_card[i][j]);
			}
			tableResponse.addFeiWang(wang_cards);
			tableResponse.addOutCardsData(cur_out_cards);
			tableResponse.addCardsData(cards);
			tableResponse.addWinOrder(table._chuwan_shunxu[i]);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));
		table.send_response_to_player(seat_index, roomResponse);

		table.Refresh_pai_score(seat_index);
		table.Refresh_user_get_score(seat_index);

		table.operate_out_card(table._out_card_player, table._turn_out_card_count, table._turn_out_card_data,
				table._turn_out_card_type, seat_index, false);
		table.Refresh_Dui_You_Card(seat_index);
		return true;
	}

}
