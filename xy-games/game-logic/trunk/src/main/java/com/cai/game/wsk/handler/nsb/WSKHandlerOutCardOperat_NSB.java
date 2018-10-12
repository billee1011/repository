package com.cai.game.wsk.handler.nsb;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.wsk.GameConstants_NSB;
import com.cai.common.util.PBUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.wsk.handler.WSKHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.dmz.DmzRsp.RoomInfoDmz;
import protobuf.clazz.dmz.DmzRsp.TableResponse_Dmz;

public class WSKHandlerOutCardOperat_NSB extends WSKHandlerOutCardOperate<WSKTable_NSB> {

	@Override
	public void exe(WSKTable_NSB table) {
		if (_out_card_player != table._current_player) {
			return;
		}

		if (_out_type == 0) {
			if (table._turn_out_card_count == 0) {
				return;
			}

			int next_player = (_out_card_player + 1 + table.getTablePlayerNumber()) % table.getTablePlayerNumber();
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table.GRR._card_count[next_player] == 0 && next_player != table._out_card_player) {
					// 显示出牌
					table._current_player = next_player;
					table._cur_out_card_count[next_player] = 0;
					Arrays.fill(table._cur_out_card_data[next_player], GameConstants.INVALID_CARD);
					// table.operate_out_card(_out_card_player,
					// table._turn_out_card_count, table._turn_out_card_data,
					// table._turn_out_card_type,
					// GameConstants.INVALID_SEAT, true);
					table.operate_out_card(_out_card_player, 0, null, GameConstants_NSB.DMZ_CT_PASS, GameConstants_NSB.INVALID_SEAT, false);
					next_player = (next_player + 1 + table.getTablePlayerNumber()) % table.getTablePlayerNumber();
				} else {
					break;
				}
			}

			if (next_player == table._out_card_player) {
				table._get_score[table._out_card_player] += table._turn_have_score;
				table._turn_have_score = 0;
				table._turn_out_card_count = 0;
				Arrays.fill(table._turn_out_card_data, GameConstants_NSB.INVALID_CARD);

				if (table.GRR._card_count[table._out_card_player] == 0) {
					table._current_player = next_player;
					table._cur_out_card_count[next_player] = 0;
					Arrays.fill(table._cur_out_card_data[next_player], GameConstants.INVALID_CARD);
					// table.operate_out_card(table._out_card_player,
					// table._turn_out_card_count, table._turn_out_card_data,
					// table._turn_out_card_type,
					// GameConstants.INVALID_SEAT, true);
					table.operate_out_card(_out_card_player, 0, null, GameConstants_NSB.DMZ_CT_PASS, GameConstants_NSB.INVALID_SEAT, false);
					int friend_player = (table._out_card_player + 2 + table.getTablePlayerNumber()) % table.getTablePlayerNumber();
					if (table.GRR._card_count[friend_player] == 0) {
						table._current_player = GameConstants_NSB.INVALID_SEAT;
						int delay = 3;
						for (int player = 0; player < table.getTablePlayerNumber(); player++) {
							if (table.GRR._card_count[(table._chuwan_shunxu[0] + player + table.getTablePlayerNumber())
									% table.getTablePlayerNumber()] != 0) {
								for (int rank = 0; rank < table.getTablePlayerNumber(); rank++) {
									if (table._chuwan_shunxu[rank] == GameConstants_NSB.INVALID_SEAT) {
										table._chuwan_shunxu[rank] = (table._chuwan_shunxu[0] + player + table.getTablePlayerNumber())
												% table.getTablePlayerNumber();
										break;
									}
								}
							}
						}
						table._cur_banker = table._chuwan_shunxu[0];
						GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._chuwan_shunxu[0], GameConstants_NSB.Game_End_NORMAL),
								delay, TimeUnit.SECONDS);

						return;
					} else {
						next_player = (_out_card_player + 1 + table.getTablePlayerNumber()) % table.getTablePlayerNumber();
						for (int i = 0; i < table.getTablePlayerNumber(); i++) {
							if (table.GRR._card_count[next_player] == 0) {
								// 显示出牌
								table._cur_out_card_count[next_player] = 0;
								Arrays.fill(table._cur_out_card_data[next_player], GameConstants.INVALID_CARD);
								table.operate_out_card(table._out_card_player, table._turn_out_card_count, table._turn_out_card_data,
										table._turn_out_card_type, GameConstants.INVALID_SEAT, true);
								next_player = (next_player + 1) % table.getTablePlayerNumber();
							} else {
								break;
							}
						}
						table._current_player = next_player;
					}
				} else {
					table._current_player = table._out_card_player;
				}
			} else {
				table._current_player = next_player;
			}

			table._prev_palyer = _out_card_player;
			table._cur_out_card_count[table._current_player] = 0;
			Arrays.fill(table._cur_out_card_data[table._current_player], GameConstants_NSB.INVALID_CARD);

			// 显示出牌
			table.operate_out_card(_out_card_player, 0, null, GameConstants_NSB.DMZ_CT_PASS, GameConstants_NSB.INVALID_SEAT, false);
			if (table._turn_out_card_count == 0) {
				table._is_shou_chu = 1;
			}
			table.Refresh_user_get_score(GameConstants_NSB.INVALID_SEAT);
			return;
		}
		// ------------end _out_card==0----------

		table._logic.SortCardList(_out_cards_data, _out_card_count, GameConstants_NSB.WSK_ST_VALUE);
		// 出牌判断
		int card_type = adjust_out_card_right(table);
		if (card_type == GameConstants_NSB.CT_ERROR) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return;
		}

		// 牌分操作
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

		table.GRR._card_count[_out_card_player] -= _out_card_count;
		table._turn_have_score += table._logic.GetCardScore(_out_cards_data, _out_card_count);
		table._turn_out_card_type = card_type;
		table._turn_out_card_count = _out_card_count;
		table._out_card_player = _out_card_player;
		table._prev_palyer = _out_card_player;
		table._cur_out_card_count[_out_card_player] = _out_card_count;
		Arrays.fill(table._turn_out_card_data, GameConstants_NSB.INVALID_CARD);
		for (int i = 0; i < _out_card_count; i++) {
			table._turn_out_card_data[i] = _out_cards_data[i];
			table._cur_out_card_data[_out_card_player][i] = _out_cards_data[i];
		}
		// 清空接下去出牌玩家出牌数据
		int next_player = (_out_card_player + 1) % table.getTablePlayerNumber();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.GRR._card_count[next_player] == 0) {
				// 显示出牌
				table._current_player = next_player;
				table._cur_out_card_count[next_player] = 0;
				Arrays.fill(table._cur_out_card_data[next_player], GameConstants.INVALID_CARD);
				table.operate_out_card(table._out_card_player, table._turn_out_card_count, table._turn_out_card_data, table._turn_out_card_type,
						GameConstants.INVALID_SEAT, true);
				next_player = (next_player + 1) % table.getTablePlayerNumber();
			} else {
				break;
			}
		}
		table._current_player = next_player;
		table._cur_out_card_count[next_player] = 0;
		Arrays.fill(table._cur_out_card_data[next_player], GameConstants_NSB.INVALID_CARD);

		if (table.GRR._card_count[_out_card_player] == 0) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._chuwan_shunxu[i] == GameConstants.INVALID_SEAT) {
					table._chuwan_shunxu[i] = _out_card_player;
					break;
				}
			}
		}

		if (table.GRR._card_count[(_out_card_player + 1) % table.getTablePlayerNumber()] == 0
				&& table.GRR._card_count[(_out_card_player + 3) % table.getTablePlayerNumber()] == 0) {
			table._current_player = GameConstants.INVALID_SEAT;
			int delay = 3;
			for (int j = 0; j < table.getTablePlayerNumber(); j++) {
				if (table.GRR._card_count[(table._chuwan_shunxu[0] + j + table.getTablePlayerNumber()) % table.getTablePlayerNumber()] != 0) {
					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						if (table._chuwan_shunxu[i] == GameConstants.INVALID_SEAT) {
							table._chuwan_shunxu[i] = (table._chuwan_shunxu[0] + j + table.getTablePlayerNumber()) % table.getTablePlayerNumber();
							break;
						}
					}
				}

				table._cur_out_card_count[j] = 0;
				Arrays.fill(table._cur_out_card_data[j], GameConstants.INVALID_CARD);
			}
			table._get_score[table._out_card_player] += table._turn_have_score;
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._chuwan_shunxu[0], GameConstants.Game_End_NORMAL), delay,
					TimeUnit.SECONDS);
		}

		table._is_shou_chu = 0;
		// 显示出牌
		table.operate_out_card(table._out_card_player, table._turn_out_card_count, table._turn_out_card_data, table._turn_out_card_type,
				GameConstants_NSB.INVALID_SEAT, false);

		table.Refresh_pai_score(GameConstants_NSB.INVALID_SEAT);
		table.Refresh_user_get_score(GameConstants_NSB.INVALID_SEAT);
	}

	public int adjust_out_card_right(WSKTable_NSB table) {
		int card_type = table._logic.GetCardType_WSK(_out_cards_data, _out_card_count);
		if (card_type == GameConstants_NSB.CT_ERROR) {
			return GameConstants_NSB.CT_ERROR;
		}
		if (card_type == GameConstants_NSB.CT_THREE && !table.has_rule(GameConstants_NSB.GAME_RULE_SAN_DAI_DUI)) {
			return GameConstants_NSB.CT_ERROR;
		}
		if (card_type == GameConstants_NSB.CT_THREE_LINK && !table.has_rule(GameConstants_NSB.GAME_RULE_FEI_JI_DAI_DUI)) {
			return GameConstants_NSB.CT_ERROR;
		}
		if (table._turn_out_card_count != 0) {
			if (!table._logic.CompareCard_WSK(table._turn_out_card_data, _out_cards_data, table._turn_out_card_count, _out_card_count)) {
				return GameConstants_NSB.CT_ERROR;
			}
		}
		if (!table._logic.RemoveCard(_out_cards_data, _out_card_count, table.GRR._cards_data[_out_card_player],
				table.GRR._card_count[_out_card_player])) {
			return GameConstants_NSB.CT_ERROR;
		}

		return card_type;

	}

	@Override
	public boolean handler_player_be_in_room(WSKTable_NSB table, int seat_index) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DMZ_RECONNECT_DATA);

		TableResponse_Dmz.Builder tableResponse_dmz = TableResponse_Dmz.newBuilder();
		table.load_player_info_data_reconnect(tableResponse_dmz);
		RoomInfoDmz.Builder room_info = table.getRoomInfoDmz();
		tableResponse_dmz.setRoomInfo(room_info);

		tableResponse_dmz.setBankerPlayer(table.GRR._banker_player);
		tableResponse_dmz.setCurrentPlayer(table._current_player);
		tableResponse_dmz.setPrevPlayer(table._prev_palyer);
		tableResponse_dmz.setPrOutCardPlayer(table._out_card_player);
		tableResponse_dmz.setPrCardsCount(table._turn_out_card_count);
		tableResponse_dmz.setPrOutCardType(table._turn_out_card_type);
		if (table._turn_out_card_count == 0 && seat_index == table._current_player) {
			tableResponse_dmz.setIsFirstOut(1);
		} else {
			tableResponse_dmz.setIsFirstOut(0);
		}
		for (int i = 0; i < table._turn_out_card_count; i++) {
			tableResponse_dmz.addPrCardsData(table._turn_out_card_data[i]);
		}
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse_dmz.addCardCount(table.GRR._card_count[i]);
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder cur_out_cards = Int32ArrayResponse.newBuilder();
			if (seat_index == i || table._is_ming_pai[i] == 1) {
				for (int j = 0; j < table.GRR._card_count[i]; j++) {
					cards.addItem(table.GRR._cards_data[i][j]);
				}
			}
			for (int j = 0; j < table._cur_out_card_count[i]; j++) {
				cur_out_cards.addItem(table._cur_out_card_data[i][j]);
			}
			tableResponse_dmz.addOutCardsData(cur_out_cards);
			tableResponse_dmz.addCardsData(cards);
			tableResponse_dmz.addMingPaiStatus(table._is_ming_pai[i]);
			tableResponse_dmz.addWinOrder(table._chuwan_shunxu[i]);
		}
		if (table._is_ming_pai[(seat_index + 2) % table.getTablePlayerNumber()] == 0) {
			tableResponse_dmz.setMingPaiStr("对家请求明牌是否同意？");
		} else {
			tableResponse_dmz.setMingPaiStr("");
		}
		if (table._is_ming_pai[seat_index] == 1) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < table.GRR._card_count[(seat_index + 2 + table.getTablePlayerNumber()) % table.getTablePlayerNumber()]; j++) {
				cards.addItem(table.GRR._cards_data[(seat_index + 2 + table.getTablePlayerNumber()) % table.getTablePlayerNumber()][j]);
			}
			tableResponse_dmz.setFriendCardsData(cards);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_dmz));
		table.send_response_to_player(seat_index, roomResponse);

		table.Refresh_pai_score(seat_index);
		table.Refresh_user_get_score(seat_index);
		return true;
	}
}
