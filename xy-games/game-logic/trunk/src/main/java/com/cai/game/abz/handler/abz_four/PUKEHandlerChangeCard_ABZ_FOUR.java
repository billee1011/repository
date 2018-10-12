package com.cai.game.abz.handler.abz_four;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.PBUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.abz.PUKETable;
import com.cai.game.abz.handler.PUKEHandlerChangeCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.abz.AbzRsp.Change_card_Response;
import protobuf.clazz.abz.AbzRsp.TableResponse_Abz;

public class PUKEHandlerChangeCard_ABZ_FOUR extends PUKEHandlerChangeCard {
	protected int _seat_index;
	protected int _game_status;
	// private int _current_player =MJGameConstants.INVALID_SEAT;

	public PUKEHandlerChangeCard_ABZ_FOUR() {
	}

	public void reset_status(int seat_index, int game_status) {
		_seat_index = seat_index;
		_game_status = game_status;
	}

	@Override
	public void exe(PUKETable table) {
		table._game_status = GameConstants.GS_ABZ_CHANGE_CARD;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_ABZ_CHANGE_CARD);
			Change_card_Response.Builder change_card = Change_card_Response.newBuilder();
			int card_data[] = new int[GameConstants.CARD_COUNT_ABZ];
			int card_count = 0;
			for (int card_index = 0; card_index < table.GRR._card_count[i]; card_index++) {
				change_card.addHandCardData(table.GRR._cards_data[i][card_index]);
			}
			for (int j = 0; j < table.getTablePlayerNumber(); j++) {
				if (i == j) {
					continue;
				}

				for (int card_index = 0; card_index < table.GRR._card_count[j]; card_index++) {
					if (table._logic.GetCardValue(table.GRR._cards_data[j][card_index]) != 1) {
						card_data[card_count++] = table.GRR._cards_data[j][card_index];
					}
				}

			}
			table._logic.SortCardList(card_data, card_count);
			for (int card_index = 0; card_index < card_count; card_index++) {
				change_card.addCardData(card_data[card_index]);
			}
			change_card.setChangePlayer(GameConstants.INVALID_SEAT);
			change_card.setCurrentPlayer(table._current_player);
			change_card.setChangeAction(2);
			change_card.setBankerPlayer(table.GRR._banker_player);
			change_card.setRoomInfo(table.getRoomInfo());
			change_card.setDisplayTime(10);
			roomResponse.setCommResponse(PBUtil.toByteString(change_card));
			table.send_response_to_player(i, roomResponse);
		}
		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ABZ_CHANGE_CARD);
		Change_card_Response.Builder change_card = Change_card_Response.newBuilder();
		int card_data[] = new int[GameConstants.CARD_COUNT_ABZ];
		int card_count = 0;
		for (int card_index = 0; card_index < table.GRR._card_count[table._current_player]; card_index++) {
			change_card.addHandCardData(table.GRR._cards_data[table._current_player][card_index]);
		}
		for (int j = 0; j < table.getTablePlayerNumber(); j++) {
			if (table._current_player == j) {
				continue;
			}

			for (int card_index = 0; card_index < table.GRR._card_count[j]; card_index++) {
				if (table._logic.GetCardValue(table.GRR._cards_data[j][card_index]) != 1) {
					card_data[card_count++] = table.GRR._cards_data[j][card_index];
				}

			}

		}
		table._logic.SortCardList(card_data, card_count);
		for (int card_index = 0; card_index < card_count; card_index++) {
			change_card.addCardData(card_data[card_index]);
		}
		change_card.setChangePlayer(GameConstants.INVALID_SEAT);
		change_card.setCurrentPlayer(table._current_player);
		change_card.setChangeAction(2);
		change_card.setBankerPlayer(table.GRR._banker_player);
		change_card.setRoomInfo(table.getRoomInfo());
		change_card.setDisplayTime(10);
		roomResponse.setCommResponse(PBUtil.toByteString(change_card));
		table.GRR.add_room_response(roomResponse);
	}

	@Override
	public boolean handler_player_be_in_room(PUKETable table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ABZ_RECONNECT);

		TableResponse_Abz.Builder tableResponse = TableResponse_Abz.newBuilder();
		table.load_player_info_data_reconnect(tableResponse);
		tableResponse.setRoomInfo(table.getRoomInfo());
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setBaoNum(table._bao_num);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addCallBankerAction(table._select_bao[i]);
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			if (i == seat_index) {
				for (int j = 0; j < table.GRR._card_count[i]; j++) {
					cards.addItem(table.GRR._cards_data[i][j]);
				}

			} else if (table.GRR._banker_player != GameConstants.INVALID_SEAT
					&& table._select_bao[table.GRR._banker_player] == 1) {
				for (int j = 0; j < table.GRR._card_count[i]; j++) {
					cards.addItem(table.GRR._cards_data[i][j]);
				}
			}

			if (i == seat_index) {
				tableResponse.addCardCount(table.GRR._card_count[i]);
			} else {
				tableResponse.addCardCount(table.GRR._card_count[i]);
			}
			tableResponse.addChangCardData(table._chang_card[i]);
			tableResponse.addCardsData(cards);
			tableResponse.addChangeAciton(table._change_action[i]);
		}

		tableResponse.setCurrentPlayer(table._current_player);
		tableResponse.setIsFirstOut(0);
		tableResponse.setIsCurrentYaPai(0);
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));
		table.send_response_to_player(seat_index, roomResponse);

		if (seat_index == table._current_player) {
			roomResponse.setType(MsgConstants.RESPONSE_ABZ_CHANGE_CARD);
			Change_card_Response.Builder change_card = Change_card_Response.newBuilder();
			for (int card_index = 0; card_index < table.GRR._card_count[seat_index]; card_index++) {
				change_card.addHandCardData(table.GRR._cards_data[seat_index][card_index]);

			}
			int card_data[] = new int[GameConstants.CARD_COUNT_ABZ];
			int card_count = 0;
			for (int j = 0; j < table.getTablePlayerNumber(); j++) {
				if (seat_index == j) {
					continue;
				}
				for (int card_index = 0; card_index < table.GRR._card_count[j]; card_index++) {
					boolean is_change_ed = false;
					int taget_card_seat = GameConstants.INVALID_SEAT;
					for (int x = 0; x < table.getTablePlayerNumber(); x++) {
						if (table._chang_card[x] == table.GRR._cards_data[j][card_index]) {
							is_change_ed = true;
							taget_card_seat = x;
							break;
						}
					}
					if (!is_change_ed) {
						if (table._logic.GetCardValue(table.GRR._cards_data[j][card_index]) != 1) {
							card_data[card_count++] = table.GRR._cards_data[j][card_index];
						}
					} else {
						int same_count = 0;
						for (int change_index = 0; change_index < table.GRR._card_count[seat_index]; change_index++) {
							if (table._logic.GetCardLogicValue(table.GRR._cards_data[j][card_index]) == table._logic
									.GetCardLogicValue(table.GRR._cards_data[seat_index][change_index])) {
								same_count++;
							}
						}
						if (same_count == 3) {
							card_data[card_count++] = table.GRR._cards_data[j][card_index];
						}

					}
				}

			}
			table._logic.SortCardList(card_data, card_count);
			for (int card_index = 0; card_index < card_count; card_index++) {
				change_card.addCardData(card_data[card_index]);
			}
			change_card.setChangePlayer(GameConstants.INVALID_SEAT);
			change_card.setCurrentPlayer(table._current_player);
			change_card.setChangeAction(table._change_action[(table._current_player + table.getTablePlayerNumber() - 1)
					% table.getTablePlayerNumber()]);
			change_card.setBaoNum(table._bao_num);
			change_card.setBankerPlayer(table.GRR._banker_player);
			roomResponse.setCommResponse(PBUtil.toByteString(change_card));
			table.send_response_to_player(seat_index, roomResponse);
		}

		return true;
	}

	/**
	 * @param
	 * @param
	 * @param
	 * @return
	 */
	public boolean handler_change_card(PUKETable table, int seat_index, int origin_card, int target_card,
			int change_cation) {
		if (table._is_change[seat_index] || seat_index != table._current_player) {
			return false;
		}
		if (table._bao_num == 0 && change_cation == 0) {
			table.send_error_notify(seat_index, 2, "一包必须叫");
			return false;
		}

		// 判断换牌是否合理
		if (change_cation > 0) {
			boolean is_origin = false;
			boolean is_target = false;
			for (int i = 0; i < table.GRR._card_count[seat_index]; i++) {
				if (origin_card == table.GRR._cards_data[seat_index][i]) {
					is_origin = true;
					break;
				}
			}
			if (table._logic.GetCardValue(origin_card) == 1) {
				table.send_error_notify(seat_index, 2, "请选择正确的牌");
				return false;
			}
			if (!is_origin) {
				table.send_error_notify(seat_index, 2, "请选择正确的牌");
				return false;
			}
			if (table._logic.GetCardValue(target_card) == 1) {
				table.send_error_notify(seat_index, 2, "请选择正确的牌");
				return false;
			}
			boolean is_change_ed = false;
			int taget_card_seat = GameConstants.INVALID_SEAT;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._chang_card[i] == target_card) {
					is_change_ed = true;
					taget_card_seat = i;
					break;
				}
			}
			if (is_change_ed) {
				// 禁止换牌规则
				int same_count = 0;
				for (int change_index = 0; change_index < table.GRR._card_count[seat_index]; change_index++) {
					if (table._logic.GetCardLogicValue(target_card) == table._logic
							.GetCardLogicValue(table.GRR._cards_data[seat_index][change_index])) {
						same_count++;
					}
				}
				if (same_count != 3) {
					table.send_error_notify(seat_index, 2, "请选择正确的牌");
					return false;
				}
			}

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				for (int j = 0; j < table.GRR._card_count[i]; j++) {
					if (target_card == table.GRR._cards_data[i][j]) {
						is_target = true;
						table.GRR._cards_data[i][j] = origin_card;
						table._logic.SortCardList(table.GRR._cards_data[i], table.GRR._card_count[i]);
						break;
					}
				}
			}
			if (!is_target) {
				table.send_error_notify(seat_index, 2, "请选择正确的牌");
				return false;
			}

			table._chang_card[seat_index] = target_card;

			for (int i = 0; i < table.GRR._card_count[seat_index]; i++) {
				if (origin_card == table.GRR._cards_data[seat_index][i]) {
					table.GRR._cards_data[seat_index][i] = target_card;
					table._logic.SortCardList(table.GRR._cards_data[seat_index], table.GRR._card_count[seat_index]);
					break;
				}
			}
		}
		table._change_action[seat_index] = change_cation;
		boolean is_change_end = true;
		table._is_change[seat_index] = true;
		if (change_cation > 0) {
			table._bao_num++;
			table._cur_banker = seat_index;
		}

		int next_player = (table._current_player + 1) % table.getTablePlayerNumber();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._is_change[next_player]) {
				next_player = (next_player + 1) % table.getTablePlayerNumber();
			} else {
				is_change_end = false;
				break;
			}
		}
		if (is_change_end) {
			table._current_player = GameConstants.INVALID_SEAT;
			table.GRR._banker_player = table._cur_banker;
		} else {
			table._current_player = next_player;
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_ABZ_CHANGE_CARD);
			Change_card_Response.Builder change_card = Change_card_Response.newBuilder();
			int card_data[] = new int[GameConstants.CARD_COUNT_ABZ];
			int card_count = 0;
			for (int card_index = 0; card_index < table.GRR._card_count[i]; card_index++) {
				change_card.addHandCardData(table.GRR._cards_data[i][card_index]);
			}
			for (int j = 0; j < table.getTablePlayerNumber(); j++) {
				change_card.addTargetCard(table._chang_card[j]);
				if (i == j) {
					continue;
				}
				for (int card_index = 0; card_index < table.GRR._card_count[j]; card_index++) {
					boolean is_change_ed = false;
					int taget_card_seat = GameConstants.INVALID_SEAT;
					for (int x = 0; x < table.getTablePlayerNumber(); x++) {
						if (table._chang_card[x] == table.GRR._cards_data[j][card_index]) {
							is_change_ed = true;
							taget_card_seat = x;
							break;
						}
					}
					if (!is_change_ed) {
						if (table._logic.GetCardValue(table.GRR._cards_data[j][card_index]) != 1) {
							card_data[card_count++] = table.GRR._cards_data[j][card_index];
						}
					} else {
						int same_count = 0;
						for (int change_index = 0; change_index < table.GRR._card_count[i]; change_index++) {
							if (table._logic.GetCardLogicValue(table.GRR._cards_data[j][card_index]) == table._logic
									.GetCardLogicValue(table.GRR._cards_data[i][change_index])) {
								same_count++;
							}
						}
						if (same_count == 3) {
							card_data[card_count++] = table.GRR._cards_data[j][card_index];
						}
					}

				}

			}
			table._logic.SortCardList(card_data, card_count);
			for (int card_index = 0; card_index < card_count; card_index++) {
				change_card.addCardData(card_data[card_index]);
			}
			change_card.setOriginCard(origin_card);

			change_card.setChangePlayer(seat_index);
			change_card.setCurrentPlayer(table._current_player);
			change_card.setChangeAction(change_cation);
			change_card.setBaoNum(table._bao_num);
			change_card.setBankerPlayer(table.GRR._banker_player);
			change_card.setDisplayTime(10);
			roomResponse.setCommResponse(PBUtil.toByteString(change_card));
			table.send_response_to_player(i, roomResponse);
		}

		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ABZ_CHANGE_CARD);
		Change_card_Response.Builder change_card = Change_card_Response.newBuilder();
		int card_data[] = new int[GameConstants.CARD_COUNT_ABZ];
		int card_count = 0;
		if (table._current_player != GameConstants.INVALID_SEAT) {
			for (int card_index = 0; card_index < table.GRR._card_count[seat_index]; card_index++) {
				change_card.addHandCardData(table.GRR._cards_data[seat_index][card_index]);
			}
			for (int j = 0; j < table.getTablePlayerNumber(); j++) {
				if (table._current_player == j) {
					continue;
				}

				for (int card_index = 0; card_index < table.GRR._card_count[j]; card_index++) {
					card_data[card_count++] = table.GRR._cards_data[j][card_index];
				}

			}
			table._logic.SortCardList(card_data, card_count);
			for (int card_index = 0; card_index < card_count; card_index++) {
				change_card.addCardData(card_data[card_index]);
			}
		}
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			change_card.addTargetCard(table._chang_card[i]);
		}
		change_card.setOriginCard(origin_card);
		change_card.setChangeAction(change_cation);
		change_card.setChangePlayer(seat_index);
		change_card.setCurrentPlayer(table._current_player);
		change_card.setChangeAction(change_cation);
		change_card.setBaoNum(table._bao_num);
		change_card.setBankerPlayer(table.GRR._banker_player);
		change_card.setDisplayTime(10);
		roomResponse.setCommResponse(PBUtil.toByteString(change_card));
		table.GRR.add_room_response(roomResponse);

		table.operate_player_cards();
		if (!is_change_end) {
			return true;
		}

		if (table.has_rule(GameConstants.GAME_RULE_ABZ_YI_BAO_MIAN_DA) && table._bao_num == 1) {
			int delay = 2;
			GameSchedule.put(
					new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL), delay,
					TimeUnit.SECONDS);
		} else {
			table._game_status = GameConstants.GS_ABZ_PLAY;
			table._current_player = table.GRR._banker_player;
			table.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.ABZ_CT_ERROR,
					GameConstants.INVALID_SEAT);
			table._handler = table._handler_out_card_operate;
		}

		return true;
	}

}
