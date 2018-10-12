package com.cai.game.mj.yu.kwx.TwoD.xg;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_KWX;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.handler.MJHandlerChiPeng;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerChiPeng_KWX_XG extends MJHandlerChiPeng<Table_KWX_XG_2D> {
	protected int _seat_index;
	protected int _action;
	protected int _card;
	protected int _provider;
	protected int _type;

	protected GangCardResult m_gangCardResult;

	public HandlerChiPeng_KWX_XG() {
		m_gangCardResult = new GangCardResult();
	}

	@Override
	public void reset_status(int seat_index, int provider, int action, int card, int type) {
		_seat_index = seat_index;
		_action = action;
		_card = card;
		_provider = provider;
		_type = type;
	}

	@Override
	public void exe(Table_KWX_XG_2D table) {

		// 有杠不杠来碰，以后不让你杠了，耶！！！！！
		if (table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_card)] == 1) {
			table._playerStatus[_seat_index].add_cards_abandoned_gang(table._logic.switch_to_card_index(_card));
		}

		int wIndex = table.GRR._weave_count[_seat_index]++;
		table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
		table.GRR._weave_items[_seat_index][wIndex].center_card = _card;
		table.GRR._weave_items[_seat_index][wIndex].weave_kind = _action;
		table.GRR._weave_items[_seat_index][wIndex].provide_player = _provider;

		table._current_player = _seat_index;

		WeaveItem weaves[] = new WeaveItem[GameConstants_KWX.MAX_WEAVE];
		int weave_count = table.GRR._weave_count[_seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
			weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
			weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
			weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player + GameConstants_KWX.WEAVE_SHOW_DIRECT;
		}

		table.operate_effect_action(_seat_index, GameConstants_KWX.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
				GameConstants_KWX.INVALID_SEAT);

		table.operate_remove_discard(this._provider, table.GRR._discard_count[_provider]);

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._playerStatus[_seat_index].chi_hu_round_valid();

		m_gangCardResult.cbCardCount = 0;
		if (table.GRR._left_card_count > 0) {
			int cbActionMask = table._logic.analyse_gang_by_card_hand_card_hu_bei(table.GRR._cards_index[_seat_index],
					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult, 0);

			if (cbActionMask != 0) {
				curPlayerStatus.add_action(GameConstants.WIK_GANG);
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
				}
			}
		}

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int count = 0;
		int ting_count = 0;

		int card_type_count = GameConstants_KWX.MAX_ZI_FENG;

		for (int i = 0; i < card_type_count; i++) {
			if (table._logic.is_magic_index(i))
				continue;

			count = table.GRR._cards_index[_seat_index][i];

			if (count > 0) {
				table.GRR._cards_index[_seat_index][i]--;

				table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(
						table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
						table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);

				if (table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] > 0) {
					table._playerStatus[_seat_index]._hu_out_card_ting[ting_count] = table._logic.switch_to_card_data(i);

					for (int p = 0; p < table.getTablePlayerNumber(); p++) {
						if (p == _seat_index) {
							continue;
						}
						for (int j = 0; j < table._playerStatus[p]._hu_card_count; j++) {
							if (table._playerStatus[p]._hu_cards[j] == table._logic.switch_to_card_data(i)) {
								continue;
							}
						}
					}

					ting_count++;
				}

				table.GRR._cards_index[_seat_index][i]++;
			}
		}

		table._playerStatus[_seat_index]._hu_out_card_count = ting_count;

		if (ting_count > 0) {
			int tmp_cards[] = new int[GameConstants_KWX.MAX_COUNT];
			int tmp_hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], tmp_cards);

			for (int i = 0; i < tmp_hand_card_count; i++) {
				for (int j = 0; j < ting_count; j++) {
					if (tmp_cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
						tmp_cards[i] += GameConstants_KWX.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}

			if (table.GRR._left_card_count > 12 && table.player_liang[_seat_index] != 1 && table.filterLiang(_seat_index)) {
				table.liang_4_type = _type;
				curPlayerStatus.add_action(GameConstants_KWX.WIK_LIANG);
				int[] liang_cards_index = new int[4];
				int[] liang_cards_data = new int[4];
				// int liang_count = table.checkLiangAddWeave(_seat_index,
				// liang_cards_index);
				int liang_count = 0; // 孝感玩法去掉扣牌的步骤，其余玩法不改动
				if (liang_count > 0) {
					for (int i = 0; i < liang_count; i++) {
						liang_cards_data[i] = table._logic.switch_to_card_data(liang_cards_index[i]);
					}
					curPlayerStatus.add_liang_card(liang_cards_data, _seat_index);
				} else {
					curPlayerStatus.add_liang_card(new int[] {}, _seat_index);
				}
			}
			table.filterHandCards(_seat_index, tmp_cards, tmp_hand_card_count);
			table.operate_player_cards_with_ting(_seat_index, tmp_hand_card_count, tmp_cards, weave_count, weaves);
		} else {
			int cards[] = new int[GameConstants_KWX.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			// 刷新手牌
			table.filterHandCards(_seat_index, cards, hand_card_count);
			table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);
		}
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		if (curPlayerStatus.has_action()) {
			table.change_player_status(_seat_index, GameConstants_KWX.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
			if (curPlayerStatus.has_action_by_code(GameConstants_KWX.WIK_LIANG)) {
				table.change_player_status(_seat_index, GameConstants_KWX.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
		} else {
			table.change_player_status(_seat_index, GameConstants_KWX.Player_Status_OUT_CARD);
			table.operate_player_status();
		}
	}

	@Override
	public boolean handler_operate_card(Table_KWX_XG_2D table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if ((operate_code != GameConstants_KWX.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("没有这个操作");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("不是当前玩家操作");
			return false;
		}

		if (operate_code == GameConstants_KWX.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants_KWX.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants_KWX.WIK_NULL }, 1);

			table._playerStatus[_seat_index].clean_action();
			table.change_player_status(_seat_index, GameConstants_KWX.INVALID_VALUE);
			table.change_player_status(_seat_index, GameConstants_KWX.Player_Status_OUT_CARD);
			table.operate_player_status();

			return true;
		}

		switch (operate_code) {
		case GameConstants_KWX.WIK_GANG: {
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, true);
					return true;
				}
			}
		}
			break;
		}

		return true;
	}

	@Override
	public boolean handler_player_out_card(Table_KWX_XG_2D table, int seat_index, int card) {
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

		table.exe_out_card(_seat_index, card, 0);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_KWX_XG_2D table, int seat_index) {
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
				int real_card = table.GRR._discard_cards[i][j];
				if (table._logic.is_magic_card(real_card)) {
					real_card += GameConstants_KWX.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
				int_array.addItem(real_card);
			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants_KWX.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player + GameConstants_KWX.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);

				if (table.GRR._weave_items[i][j].weave_kind == GameConstants_KWX.WIK_GANG && table.GRR._weave_items[i][j].public_card == 0
						&& i != seat_index) {
					// weaveItem_item.setCenterCard(0);
					//
					// for (int x = 0; x < 4; x++) {
					// weaveItem_item.addWeaveCard(-1);
					// }
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);

					int[] weave_cards = new int[4];
					int count = table._logic.get_weave_card_huangshi(table.GRR._weave_items[i][j].weave_kind,
							table.GRR._weave_items[i][j].center_card, weave_cards);
					for (int x = 0; x < count; x++) {
						if (table._logic.is_magic_card(weave_cards[x]))
							weave_cards[x] += GameConstants_KWX.CARD_ESPECIAL_TYPE_LAI_ZI;

						weaveItem_item.addWeaveCard(weave_cards[x]);
					}
				} else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);

					int[] weave_cards = new int[4];
					int count = table._logic.get_weave_card_huangshi(table.GRR._weave_items[i][j].weave_kind,
							table.GRR._weave_items[i][j].center_card, weave_cards);
					for (int x = 0; x < count; x++) {
						if (table._logic.is_magic_card(weave_cards[x]))
							weave_cards[x] += GameConstants_KWX.CARD_ESPECIAL_TYPE_LAI_ZI;

						weaveItem_item.addWeaveCard(weave_cards[x]);
					}
				}

				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);

			if (table.player_liang[i] == 1) {
				int[] temp_cards_index = Arrays.copyOf(table.GRR._cards_index[i], table.GRR._cards_index[i].length);
				int hand_card_count = table.liangShowCard(table, i, 0, temp_cards_index);
				tableResponse.addCardCount(hand_card_count);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants_KWX.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;

		if ((out_ting_count > 0) && (seat_index == _seat_index)) {
			for (int j = 0; j < hand_card_count; j++) {
				for (int k = 0; k < out_ting_count; k++) {
					if (hand_cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
						hand_cards[j] += GameConstants_KWX.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}
		}

		table.filterHandCards(seat_index, hand_cards, hand_card_count);
		for (int i = 0; i < hand_card_count; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		roomResponse.setOutCardCount(out_ting_count);

		for (int i = 0; i < out_ting_count; i++) {
			int ting_card_cout = table._playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			roomResponse.addOutCardTing(table._playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants_KWX.CARD_ESPECIAL_TYPE_TING);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(table._playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		table.send_response_to_player(seat_index, roomResponse);
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		table.operate_effect_action(_seat_index, GameConstants_KWX.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		if (table.player_liang[seat_index] == 1) {
			int[] temp_cards_index = Arrays.copyOf(table.GRR._cards_index[seat_index], table.GRR._cards_index[seat_index].length);
			table.liangShowCard(table, seat_index, 0, temp_cards_index);
		}
		table.handler_be_in_room_chu_zi(seat_index);
		return true;
	}
}
