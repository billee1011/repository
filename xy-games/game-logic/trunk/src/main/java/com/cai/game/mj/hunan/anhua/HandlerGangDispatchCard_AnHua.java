package com.cai.game.mj.hunan.anhua;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.CardsData;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.RemoveOutCardRunnable;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerGangDispatchCard_AnHua extends AbstractMJHandler<Table_AnHua> {
	private int _seat_index;
	private CardsData _gang_card_data = new CardsData(GameConstants.MAX_COUNT);
	private int[][] special_player_cards;
	private int trully_dispatch_count = 0;
	private boolean has_action = false;

	public void reset_status(int seat_index) {
		_seat_index = seat_index;
	}

	@SuppressWarnings("static-access")
	@Override
	public void exe(Table_AnHua table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
			table.operate_player_action(i, true);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();
		table._playerStatus[_seat_index].clear_cards_abandoned_hu();

		_gang_card_data.clean_cards();

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._out_card_data = GameConstants.INVALID_VALUE;
		table._out_card_player = GameConstants.INVALID_SEAT;
		table._current_player = _seat_index;

		table._provide_player = _seat_index;

		int bu_card;
		trully_dispatch_count = 0;
		for (int i = 0; i < GameConstants.AN_HUA_GANG_DRAW_COUNT && table.GRR._left_card_count > 0; i++) {
			table._send_card_count++;
			bu_card = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
			if (table.DEBUG_CARDS_MODE) {
				if (i == 0)
					bu_card = 0x22;
				if (i == 1)
					bu_card = 0x21;
				if (i == 2)
					bu_card = 0x28;
			}
			table.GRR._left_card_count--;
			trully_dispatch_count++;
			_gang_card_data.add_card(bu_card);
		}

		// 每个玩家一份杠之后翻出来的牌，分析之后，如果能胡，加特殊角标
		special_player_cards = new int[table.getTablePlayerNumber()][trully_dispatch_count];
		for (int player = 0; player < table.getTablePlayerNumber(); player++) {
			for (int i = 0; i < trully_dispatch_count; i++) {
				special_player_cards[player][i] = _gang_card_data.get_card(i);
			}
		}

		table.operate_out_card(_seat_index, trully_dispatch_count, _gang_card_data.get_cards(), GameConstants.OUT_CARD_TYPE_LEFT,
				GameConstants.INVALID_SEAT);

		has_action = table.estimate_gang_fa_pai(_seat_index, trully_dispatch_count, _gang_card_data.get_cards(), special_player_cards);

		if (!table._playerStatus[_seat_index].has_action()) {
			// 翻出来的牌不能胡牌 相当于自动托管
			table.istrustee[_seat_index] = true;
			table.operate_player_data();
		}

		if (has_action == false) {
			table._provide_player = GameConstants.INVALID_SEAT;
			table._out_card_player = _seat_index;

			GameSchedule.put(new RemoveOutCardRunnable(table.getRoom_id(), _seat_index, GameConstants.OUT_CARD_TYPE_LEFT), 1000,
					TimeUnit.MILLISECONDS);

			GameSchedule.put(new AddDiscardRunnable(table.getRoom_id(), _seat_index, _gang_card_data.get_card_count(), _gang_card_data.get_cards(),
					true, table.getMaxCount()), 1000, TimeUnit.MILLISECONDS);

			table._current_player = (_seat_index + 1) % table.getTablePlayerNumber();

			table.exe_dispatch_card(table._current_player, GameConstants.WIK_NULL, 1000);
		} else {
			table._provide_player = _seat_index;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._playerStatus[i].has_action()) {
					if (table.istrustee[i]) {
						if (i == _seat_index) {
							if (table._playerStatus[i].has_action_by_code(GameConstants.WIK_ZI_MO)) {
								table.exe_jian_pao_hu_new(i, GameConstants.WIK_ZI_MO, table.win_card_at_gang[i]);
							} else {
								table.exe_jian_pao_hu_new(i, GameConstants.WIK_NULL, 0);
							}
						} else {
							if (table._playerStatus[i].has_action_by_code(GameConstants.WIK_CHI_HU)) {
								table.exe_jian_pao_hu_new(i, GameConstants.WIK_CHI_HU, table.win_card_at_gang[i]);
							} else {
								table.exe_jian_pao_hu_new(i, GameConstants.WIK_NULL, 0);
							}
						}
					} else {
						table.operate_player_action(i, false);
					}
				}
			}
		}

		return;
	}

	@Override
	public boolean handler_operate_card(Table_AnHua table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return true;
		}
		
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("没有这个操作");
			return false;
		}

		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}

		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			if (_seat_index == seat_index) {
				// 本人有自摸 但是点了过 相当于进入自动托管 除非有胡 否则摸什么打什么 有碰杠自动过
				table.istrustee[_seat_index] = true;
				table.operate_player_data();
			}
		}

		if (table._playerStatus[seat_index].has_chi_hu() && operate_code != GameConstants.WIK_CHI_HU) {
			for (int i = 0; i < trully_dispatch_count; i++) {
				if (special_player_cards[seat_index][i] > GameConstants.CARD_ESPECIAL_TYPE_CAN_WIN) {
					table._playerStatus[seat_index].add_cards_abandoned_hu(_gang_card_data.get_card(i));
				}
			}
		}

		// 优先级判断，不通炮玩法
		int target_player = seat_index;
		int target_action = operate_code;
		int target_p = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			if (i == target_player) {
				target_p = table.getTablePlayerNumber() - p;
			}
		}
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();

			int cbUserActionRank = 0;
			int cbTargetActionRank = 0;

			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform()) + table.getTablePlayerNumber() - p;
				} else {
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count, table._playerStatus[i]._action)
							+ table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform()) + target_p;
				} else {
					cbTargetActionRank = table._logic.get_action_list_rank(table._playerStatus[target_player]._action_count,
							table._playerStatus[target_player]._action) + target_p;
				}

				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;
					target_action = table._playerStatus[i].get_perform();
					target_p = table.getTablePlayerNumber() - p;
				}
			}
		}

		if (table._playerStatus[target_player].is_respone() == false)
			return true;

		int target_card = table._playerStatus[target_player]._operate_card;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (i == target_player)
				continue;
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
			table.operate_player_action(i, true);
		}

		switch (target_action) {
		case GameConstants.WIK_NULL: {
			table.operate_out_card(_seat_index, 0, null, GameConstants.OUT_CARD_TYPE_LEFT, GameConstants.INVALID_SEAT);

			table.exe_add_discard(_seat_index, trully_dispatch_count, _gang_card_data.get_cards(), true, 0);

			table._current_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();

			table.exe_dispatch_card(table._current_player, GameConstants.WIK_NULL, 0);

			return true;
		}
		case GameConstants.WIK_ZI_MO: {
			table.operate_out_card(_seat_index, 0, null, GameConstants.OUT_CARD_TYPE_LEFT, GameConstants.INVALID_SEAT);

			table.GRR._chi_hu_rights[seat_index].set_valid(true);

			table._cur_banker = seat_index;

			table.GRR._chi_hu_card[seat_index][0] = target_card;
			table._player_result.zi_mo_count[seat_index]++;

			table.set_niao_card(seat_index);

			table.process_chi_hu_player_operate(seat_index, target_card, true);
			table.process_chi_hu_player_score(_seat_index, _seat_index, target_card, true);

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL), GameConstants.GAME_FINISH_DELAY,
					TimeUnit.SECONDS);

			return true;
		}
		case GameConstants.WIK_CHI_HU: {
			table.operate_out_card(_seat_index, 0, null, GameConstants.OUT_CARD_TYPE_LEFT, GameConstants.INVALID_SEAT);

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i != target_player) {
					table.GRR._chi_hu_rights[i].set_valid(false);
					table.GRR._chi_hu_rights[i].set_empty();
				}
			}

			table.GRR._chi_hu_rights[target_player].set_valid(true);

			table._cur_banker = target_player;

			table.set_niao_card(target_player);

			table.GRR._chi_hu_card[target_player][0] = target_card;
			table._player_result.jie_pao_count[target_player]++;
			table._player_result.dian_pao_count[_seat_index]++;

			table.process_chi_hu_player_operate(target_player, target_card, false);
			table.process_chi_hu_player_score(target_player, _seat_index, target_card, false);

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_AnHua table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		roomResponse.setIsGoldRoom(table.is_sys());

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_seat_index);
		tableResponse.setCellScore(0);
		tableResponse.setActionCard(0);
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);
		tableResponse.setSendCardData(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < 55; j++) {
				int_array.addItem(table.GRR._discard_cards[i][j]);
			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);

			if (i == _seat_index) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}

		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		for (int j = 0; j < hand_card_count; j++) {
			if (hand_cards[j] == table.joker_card_1 || hand_cards[j] == table.joker_card_2) {
				hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_GANG }, 1, seat_index);

		table.operate_out_card(_seat_index, trully_dispatch_count, _gang_card_data.get_cards(), GameConstants.OUT_CARD_TYPE_LEFT, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
