package com.cai.game.mj.yu.kwx.TwoD;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_KWX;
import com.cai.common.constant.game.hongershi.HongErShiConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerLiangCardOperate_KWX extends AbstractMJHandler<Table_KWX_2D> {

	public int _seat_index;
	public int _operate_code;
	public int _operate_card;
	public List<Integer> _liang_cards;
	public int _liang_cards_count;

	public void reset_status(int seat_index, int operate_code, int operate_card, List<Integer> liang_cards, int liang_cards_count) {
		_seat_index = seat_index;
		_operate_code = operate_code;
		_operate_card = operate_card;
		_liang_cards = liang_cards;
		_liang_cards_count = liang_cards_count;
	}

	@Override
	public void exe(Table_KWX_2D table) {

		// TODO: 取消亮牌
		if (_operate_code == GameConstants_KWX.WIK_NULL) {
			if (table._playerStatus[_seat_index].has_action() && (table._playerStatus[_seat_index].is_respone() == false)) {
				table.operate_player_action(_seat_index, false);
			}
			return;
		}

		for (int i = 0; i < _liang_cards_count; i++) {
			int wIndex = table.GRR._weave_count[_seat_index]++;
			table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
			table.GRR._weave_items[_seat_index][wIndex].center_card = _liang_cards.get(i);
			table.GRR._weave_items[_seat_index][wIndex].weave_kind = GameConstants_KWX.WIK_LIANG;
			table.GRR._weave_items[_seat_index][wIndex].provide_player = _seat_index;

			int cbRemoveCard[] = new int[] { _liang_cards.get(i), _liang_cards.get(i), _liang_cards.get(i) };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[_seat_index], cbRemoveCard, 3)) {
				table.log_player_error(_seat_index, "碰牌删除出错");
				return;
			}
		}

		table._current_player = _seat_index;
		table.player_liang[_seat_index] = 1;
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();
		curPlayerStatus.operate(_operate_code, _operate_card);
		// 设置为报听状态
		curPlayerStatus.set_card_status(HongErShiConstants.CARD_STATUS_LIANG);
		if (table.first_liang == -1) {
			table.first_liang = _seat_index;
		}

		WeaveItem weaves[] = new WeaveItem[GameConstants_KWX.MAX_WEAVE];
		int weave_count = table.GRR._weave_count[_seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
			weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
			weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
			weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player + GameConstants_KWX.WEAVE_SHOW_DIRECT;
		}
		int cards[] = new int[GameConstants_KWX.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		table.operate_player_cards(_seat_index, 0, new int[] {}, weave_count, weaves);

		int[] temp_cards_index = Arrays.copyOf(table.GRR._cards_index[_seat_index], table.GRR._cards_index[_seat_index].length);
		temp_cards_index[table._logic.switch_to_card_index(table.get_real_card(_operate_card))]--;
		table._playerStatus[_seat_index]._hu_card_count = table.get_ting_card(table._playerStatus[_seat_index]._hu_cards, temp_cards_index,
				table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);

		table.operate_player_cards(_seat_index, 0, null, 0, null);
		// 显示胡牌
		cards = new int[GameConstants.MAX_COUNT];
		hand_card_count = table._logic.switch_to_cards_data(temp_cards_index, cards);
		table.operate_show_card(_seat_index, GameConstants.Show_Card_LY_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		table.handler_player_out_card(_seat_index, _operate_card);
		table.operate_effect_action(_seat_index, GameConstants_KWX.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants_KWX.WIK_LIANG }, 1,
				GameConstants_KWX.INVALID_SEAT);

	}

	@Override
	public boolean handler_player_out_card(Table_KWX_2D table, int seat_index, int card) {
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

		// 出牌
		table.exe_out_card(_seat_index, card, table.liang_4_type);

		return true;
	}

	@Override
	public boolean handler_operate_card(Table_KWX_2D table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if ((operate_code != GameConstants_KWX.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("没有这个操作");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("不是当前玩家操作");
			return false;
		}

		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}

		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();

		switch (operate_code) {
		case GameConstants_KWX.WIK_GANG: {
			GangCardResult m_gangCardResult = new GangCardResult();
			table._logic.analyse_gang_hong_zhong_all(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], m_gangCardResult, true, table._playerStatus[_seat_index].get_cards_abandoned_gang());

			for (int w = 0; w < table.GRR._weave_count[_seat_index]; w++) {
				if (table.GRR._weave_items[_seat_index][w].weave_kind == GameConstants_KWX.WIK_LIANG
						&& table.GRR._weave_items[_seat_index][w].center_card == operate_card) {
					int index = m_gangCardResult.cbCardCount++;
					m_gangCardResult.cbCardData[index] = operate_card;
					m_gangCardResult.isPublic[index] = 0;// 明刚
					m_gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
					break;
				}
			}
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], false, true);
					// if (m_gangCardResult.cbCardData[i] != _send_card_data) {
					return true;
				}
			}
		}
		case GameConstants_KWX.WIK_NULL: {
			table.change_player_status(_seat_index, GameConstants_KWX.Player_Status_OUT_CARD);
			table.operate_player_status();
			return true;
		}
		case GameConstants_KWX.WIK_ZI_MO: {

			table.GRR._chi_hu_rights[_seat_index].set_valid(true);

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == table._cur_banker) {
					table._player_result.qiang[table._cur_banker] = table.continue_banker_count;
				} else {
					table._player_result.qiang[i] = 0;
				}
			}

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table.GRR._win_order[_seat_index] = 1;

			table.set_niao_card(_seat_index, 0, true, 0);

			if (table.GRR._left_card_count == 0) {
				table.GRR._chi_hu_rights[_seat_index].opr_and(GameConstants_KWX.CHR_HAI_DI_HU);
			}

			table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

			table._player_result.zi_mo_count[_seat_index]++;

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants_KWX.Game_End_NORMAL), 0, TimeUnit.SECONDS);

			return true;
		}
		}
		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_KWX_2D table, int seat_index) {
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
					weaveItem_item.setCenterCard(0);

					for (int x = 0; x < 4; x++) {
						weaveItem_item.addWeaveCard(-1);
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

			if (i == table._provide_player) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
			// if (i != seat_index && table.player_liang[seat_index] == 1) {
			// table.operate_player_cards(i, 0, null, 0, null);
			// int cards[] = new int[GameConstants_KWX.MAX_COUNT];
			// int hand_card_count =
			// table._logic.switch_to_cards_data(table.GRR._cards_index[i],
			// cards);
			// table.operate_show_card(i, GameConstants.Show_Card_LY_HU,
			// hand_card_count, cards, GameConstants.INVALID_SEAT);
			// }
		}

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants_KWX.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		table.filterHandCards(seat_index, hand_cards, hand_card_count);
		if (seat_index == table._provide_player && table._send_card_data != GameConstants_KWX.INVALID_VALUE) {
			for (int c = 0; c < hand_card_count; c++) {
				if (hand_cards[c] == table._send_card_data || hand_cards[c] == (table._send_card_data + GameConstants_KWX.CARD_ESPECIAL_TYPE_NON_OUT)
						|| hand_cards[c] == (table._send_card_data + GameConstants_KWX.CARD_ESPECIAL_TYPE_TING)) {
					hand_cards[c] = 0;
					break;
				}
			}

			int show_hand_card_count = 0;
			for (int c = 0; c < hand_card_count; c++) {
				if (hand_cards[c] != 0) {
					hand_cards[show_hand_card_count++] = hand_cards[c];
				}
			}
			hand_card_count--;
		}

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

		for (int i = 0; i < hand_card_count && table.player_liang[seat_index] != 1; i++) {
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

		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			if (table.player_liang[p] != 1) {
				continue;
			}
			table.operate_player_cards(p, 0, null, 0, null);
			int cards[] = new int[GameConstants_KWX.MAX_COUNT];
			if (p == table._provide_player && table._send_card_data != GameConstants_KWX.INVALID_VALUE) {
				table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]--;
			}
			hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[p], cards);
			if (p == table._provide_player && table._send_card_data != GameConstants_KWX.INVALID_VALUE) {
				table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]++;
			}
			table.operate_show_card(p, GameConstants.Show_Card_LY_HU, hand_card_count, cards, GameConstants_KWX.INVALID_SEAT);
		}

		if (seat_index == table._provide_player && table._send_card_data != GameConstants_KWX.INVALID_VALUE) {
			int show_send_card = table._send_card_data;
			boolean add_ting = false;
			for (int t = 0; t < table._playerStatus[_seat_index]._hu_out_card_count; t++) {
				if (table._playerStatus[_seat_index]._hu_out_card_ting[t] == show_send_card) {
					add_ting = true;
				}
			}
			if (table.filterHandCard(seat_index, table._send_card_data)
					&& table.filterHandCards(seat_index, hand_cards, hand_card_count) != hand_card_count) {
				show_send_card += GameConstants_KWX.CARD_ESPECIAL_TYPE_NON_OUT;
			} else if (add_ting) {
				show_send_card += GameConstants_KWX.CARD_ESPECIAL_TYPE_TING;
			}
			table.operate_player_get_card(seat_index, 1, new int[] { show_send_card }, seat_index);
		}
		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}
		return true;
	}
}
