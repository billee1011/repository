package com.cai.game.mj.handler.lygc;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.MJTable;
import com.cai.game.mj.handler.MJHandlerChiPeng;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerChiPeng_lygc extends MJHandlerChiPeng<MJTable> {
	private GangCardResult m_gangCardResult;

	public MJHandlerChiPeng_lygc() {
		m_gangCardResult = new GangCardResult();
	}

	@Override
	public void exe(MJTable table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
			table.operate_player_action(i, true);
		}

		int wIndex = table.GRR._weave_count[_seat_index]++;
		table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
		table.GRR._weave_items[_seat_index][wIndex].center_card = _card;
		table.GRR._weave_items[_seat_index][wIndex].weave_kind = _action;
		table.GRR._weave_items[_seat_index][wIndex].provide_player = _provider;

		table._current_player = _seat_index;

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, GameConstants.INVALID_SEAT);

		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		for (int j = 0; j < hand_card_count; j++) {
			if (table._logic.is_ci_card(table._logic.switch_to_card_index(cards[j]))
					&& (table.is_mj_type(GameConstants.GAME_TYPE_NEW_LUO_YANG_GANG_CI) || table.is_mj_type(GameConstants.GAME_TYPE_HENAN_LYGC))) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_CI;
			}
		}

		WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];
		int weave_count = table.GRR._weave_count[_seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			int center_card = table.GRR._weave_items[_seat_index][i].center_card;

			if (table.GRR._weave_items[_seat_index][i].weave_kind == GameConstants.WIK_PENG) {
				for (int j = 0; j < table.getTablePlayerNumber(); j++) {
					for (int m = 0; m < table.GRR._discard_count[j]; m++) {
						if (center_card == table.GRR._discard_cards[j][m]) {
							center_card += GameConstants.CARD_ESPECIAL_TYPE_PEN_CHANGER;
						}
					}
				}
			}

			if (table.GRR._weave_items[_seat_index][i].weave_kind == GameConstants.WIK_GANG) {
				center_card += GameConstants.CARD_ESPECIAL_TYPE_PEN_CHANGER;
			}

			weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
			weaves[i].center_card = center_card;
			weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
			weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player + GameConstants.WEAVE_SHOW_DIRECT;

			if (table._logic.is_ci_card(table._logic.switch_to_card_index(weaves[i].center_card))
					&& (table.is_mj_type(GameConstants.GAME_TYPE_NEW_LUO_YANG_GANG_CI) || table.is_mj_type(GameConstants.GAME_TYPE_HENAN_LYGC))) {
				weaves[i].center_card += GameConstants.CARD_ESPECIAL_TYPE_CI;
			}
		}

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int count = 0;
		int ting_count = 0;

		int card_type_count = GameConstants.MAX_ZI;
		if (table.has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG))
			card_type_count = GameConstants.MAX_ZI_FENG;

		for (int i = 0; i < card_type_count; i++) {
			count = table.GRR._cards_index[_seat_index][i];

			if (count > 0) {
				table.GRR._cards_index[_seat_index][i]--;

				table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_henan_ting_card_lygc(
						table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
						table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index]);

				if (table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] > 0) {
					table._playerStatus[_seat_index]._hu_out_card_ting[ting_count] = table._logic.switch_to_card_data(i);

					ting_count++;
				}

				table.GRR._cards_index[_seat_index][i]++;
			}
		}

		table._playerStatus[_seat_index]._hu_out_card_count = ting_count;

		if (ting_count > 0) {
			int tmp_cards[] = new int[GameConstants.MAX_COUNT];
			int tmp_hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], tmp_cards);

			for (int i = 0; i < tmp_hand_card_count; i++) {
				for (int j = 0; j < ting_count; j++) {
					if (tmp_cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
						tmp_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
					}
				}
				if (tmp_cards[i] < GameConstants.CARD_ESPECIAL_TYPE_TING) {
					if (table._logic.is_ci_card(table._logic.switch_to_card_index(tmp_cards[i]))) {
						tmp_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_CI;
					}
				}
			}

			table.operate_player_cards_with_ting(_seat_index, tmp_hand_card_count, tmp_cards, weave_count, weaves);
		} else {
			table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);
		}
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		table.operate_remove_discard(this._provider, table.GRR._discard_count[_provider]);

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._playerStatus[_seat_index].chi_hu_round_valid();

		if (curPlayerStatus.has_action()) {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
		}

		if (table._playerStatus[_seat_index].get_status() != GameConstants.Player_Status_OUT_CARD) {
			table.log_error("洛阳杠次，玩家状态错误，应该是出牌状态");
		}
	}

	@Override
	public boolean handler_player_out_card(MJTable table, int seat_index, int card) {
		card = table.get_real_card(card);

		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("出牌,没到出牌");
			return false;
		}

		if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
			table.log_error("出牌删除出错");
			return false;
		}

		if (table._playerStatus[_seat_index].get_status() != GameConstants.INVALID_VALUE) {
			table._playerStatus[_seat_index].set_status(GameConstants.INVALID_VALUE);
			table.exe_out_card(_seat_index, card, _action);
		}

		return true;
	}

	@Override
	public boolean handler_operate_card(MJTable table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("没有这个操作");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("不是当前玩家操作");
			return false;
		}

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			table._playerStatus[_seat_index].clean_action();

			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();

			return true;
		}

		switch (operate_code) {
		case GameConstants.WIK_GANG: {
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, false);
					return true;
				}
			}
			break;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

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
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				if (table._logic.is_ci_card(table._logic.switch_to_card_index(table.GRR._discard_cards[i][j]))) {
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_CI);
				} else {
					int_array.addItem(table.GRR._discard_cards[i][j]);
				}
			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				if (table._logic.is_ci_card(table._logic.switch_to_card_index(table.GRR._weave_items[i][j].center_card))) {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_CI);
				} else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}

				if (table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_PENG) {
					for (int k = 0; k < table.getTablePlayerNumber(); k++) {
						for (int m = 0; m < table.GRR._discard_count[k]; m++) {
							if (table.GRR._weave_items[i][j].center_card == table.GRR._discard_cards[k][m]) {
								weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_PEN_CHANGER);
							}
						}
					}
				}

				if (table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG) {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_PEN_CHANGER);
				}

				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		tableResponse.setSendCardData(0);
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;

		for (int j = 0; j < hand_card_count; j++) {
			if ((out_ting_count > 0) && (seat_index == _seat_index)) {
				for (int k = 0; k < out_ting_count; k++) {
					if (cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
						cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}
			if (cards[j] < GameConstants.CARD_ESPECIAL_TYPE_TING) {
				if (table._logic.is_ci_card(table._logic.switch_to_card_index(cards[j]))) {
					cards[j] += GameConstants.CARD_ESPECIAL_TYPE_CI;
				}
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(cards[i]);
		}

		roomResponse.setTable(tableResponse);

		roomResponse.setOutCardCount(out_ting_count);

		for (int i = 0; i < out_ting_count; i++) {
			int ting_card_cout = table._playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			roomResponse.addOutCardTing(table._playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(table._playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		table.send_response_to_player(seat_index, roomResponse);
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		// 听牌显示
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
