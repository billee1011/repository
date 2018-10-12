package com.cai.game.mj.yu.dcwdh.handler;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_DCWDH;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;
import com.cai.game.mj.yu.dcwdh.MJTable_DCWDH;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerDispatchCard_DCWDH extends MJHandlerDispatchCard<MJTable_DCWDH> {

	private boolean ting_send_card;

	@Override
	public void exe(MJTable_DCWDH table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants_DCWDH.INVALID_VALUE);

		}
		table._playerStatus[_seat_index].chi_hu_round_valid();
		table._playerStatus[_seat_index].clear_cards_abandoned_peng();

		if (table.GRR._left_card_count == table.getCsDingNiaoNum()) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants_DCWDH.INVALID_VALUE;
			}

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants_DCWDH.Game_End_DRAW), 0, TimeUnit.SECONDS);
			return;
		}

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index;

		table._send_card_count++;

		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];

		--table.GRR._left_card_count;

		table._provide_player = _seat_index;

		if (table.DEBUG_CARDS_MODE) {
			_send_card_data = 0x32;
		}
		table._send_card_data = _send_card_data;

		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		int card_type = GameConstants_DCWDH.HU_CARD_TYPE_ZI_MO;
		if (_type == GameConstants_DCWDH.GANG_TYPE_AN_GANG || _type == GameConstants_DCWDH.GANG_TYPE_ADD_GANG
				|| _type == GameConstants_DCWDH.GANG_TYPE_JIE_GANG) {
			card_type = GameConstants_DCWDH.HU_CARD_TYPE_GANG_KAI_HUA;
		}
		int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], _send_card_data, chr, card_type, _seat_index);

		if (_type == GameConstants_DCWDH.DispatchCard_Type_Tian_Hu) {
			table.check_banker_tian_hu(table.GRR._cards_index[_seat_index], _seat_index, _send_card_data);
		}
		if (action != GameConstants_DCWDH.WIK_NULL || (_type == GameConstants_DCWDH.DispatchCard_Type_Tian_Hu && table.banker_tian_hu)) {
			curPlayerStatus.add_action(GameConstants_DCWDH.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);

			if (_type == GameConstants_DCWDH.DispatchCard_Type_Tian_Hu) {
				if (action != GameConstants_DCWDH.WIK_NULL) {
					chr.opr_or(GameConstants_DCWDH.CHR_TIAN_HU_REAL);
				} else {
					chr.opr_or(GameConstants_DCWDH.CHR_TIAN_HU);
				}
			}
		} else {
			table.GRR._chi_hu_rights[_seat_index].set_empty();
			chr.set_empty();

			action = table.analyse_bao_hu(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], _send_card_data, chr, card_type, _seat_index);
			if (action != GameConstants_DCWDH.WIK_NULL) {
				curPlayerStatus.add_action(GameConstants_DCWDH.WIK_ZI_MO);
				curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
			} else {
				table.GRR._chi_hu_rights[_seat_index].set_empty();
				chr.set_empty();
			}
		}

		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

		int ting_count = 0;
		int send_card_index = table._logic.switch_to_card_index(_send_card_data);
		ting_send_card = false;
		for (int i = 0; i < GameConstants_DCWDH.MAX_ZI_FENG; i++) {
			if (table.GRR._cards_index[_seat_index][i] > 0) {
				table.GRR._cards_index[_seat_index][i]--;

				table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card_bao_hu(
						table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
						table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);

				if (table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] > 0) {
					table._playerStatus[_seat_index]._hu_out_card_ting[ting_count] = table._logic.switch_to_card_data(i);

					ting_count++;

					if (send_card_index == i) {
						ting_send_card = true;
					}
				}

				table.GRR._cards_index[_seat_index][i]++;
			}
		}

		table._playerStatus[_seat_index]._hu_out_card_count = ting_count;
		if (ting_count > 0) {
			table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]--;
			int cards[] = new int[GameConstants_DCWDH.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

			for (int i = 0; i < hand_card_count; i++) {
				for (int j = 0; j < ting_count; j++) {
					if (cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
						cards[i] += GameConstants_DCWDH.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}
			table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);
		}

		int show_send_card = _send_card_data;
		if (ting_send_card) {
			show_send_card += GameConstants_DCWDH.CARD_ESPECIAL_TYPE_TING;
		}
		table._provide_card = _send_card_data;

		table.operate_player_get_card(_seat_index, 1, new int[] { show_send_card }, GameConstants_DCWDH.INVALID_SEAT);

		m_gangCardResult.cbCardCount = 0;

		int cbActionMask = table.analyse_gang_hong_zhong_all(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], m_gangCardResult, true, table._playerStatus[_seat_index].get_cards_abandoned_gang(), _seat_index,
				_send_card_data);

		for (int w = 0; w < table.GRR._weave_count[_seat_index]; w++) {
			if (table.GRR._weave_items[_seat_index][w].weave_kind == GameConstants_DCWDH.WIK_LIANG
					&& table.GRR._weave_items[_seat_index][w].center_card == _send_card_data) {
				cbActionMask |= GameConstants.WIK_GANG;

				int index = m_gangCardResult.cbCardCount++;
				m_gangCardResult.cbCardData[index] = _send_card_data;
				m_gangCardResult.isPublic[index] = 0;// 明刚
				m_gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
				break;
			}
		}
		if (cbActionMask != GameConstants_DCWDH.WIK_NULL && table.GRR._left_card_count != table.getCsDingNiaoNum()) {
			if (table.GRR._left_card_count == 0 && m_gangCardResult.cbCardCount == 1
					&& m_gangCardResult.type[0] == GameConstants_DCWDH.GANG_TYPE_AN_GANG) {
				// TODO:荒庄前最后一张：摸到能明杠、暗杠的牌不能杠牌
			} else {
				curPlayerStatus.add_action(GameConstants_DCWDH.WIK_GANG);
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
				}
			}
		}
		if (curPlayerStatus.has_action()) {
			table.change_player_status(_seat_index, GameConstants_DCWDH.Player_Status_OUT_CARD);
			table.operate_player_status();
			table.operate_player_action(_seat_index, false);
		} else {
			table.change_player_status(_seat_index, GameConstants_DCWDH.Player_Status_OUT_CARD);
			table.operate_player_status();
		}
	}

	@Override
	public boolean handler_operate_card(MJTable_DCWDH table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if ((operate_code != GameConstants_DCWDH.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
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
		case GameConstants_DCWDH.WIK_GANG: {
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					if (operate_card == _send_card_data) {
						table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, true);
					} else {
						table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, false);
					}
					return true;
				}
			}
		}
		case GameConstants_DCWDH.WIK_NULL: {
			table.change_player_status(_seat_index, GameConstants_DCWDH.Player_Status_OUT_CARD);
			table.operate_player_status();
			return true;
		}
		case GameConstants_DCWDH.WIK_ZI_MO: {

			table.GRR._chi_hu_rights[_seat_index].set_valid(true);

			if (_seat_index != table._cur_banker) {
				table._cur_banker = (table._cur_banker + 1) % table.getTablePlayerNumber();
			}

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table.GRR._win_order[_seat_index] = 1;

			table.set_niao_card(_seat_index, 0, true, 0);
			table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

			table._player_result.zi_mo_count[_seat_index]++;

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants_DCWDH.Game_End_NORMAL), 0, TimeUnit.SECONDS);

			return true;
		}
		}
		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_DCWDH table, int seat_index) {
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
					real_card += GameConstants_DCWDH.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
				int_array.addItem(real_card);
			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants_DCWDH.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player + GameConstants_DCWDH.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);

				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);

				int[] weave_cards = new int[4];
				int count = table._logic.get_weave_card_huangshi(table.GRR._weave_items[i][j].weave_kind, table.GRR._weave_items[i][j].center_card,
						weave_cards);
				for (int x = 0; x < count; x++) {
					if (table._logic.is_magic_card(weave_cards[x]))
						weave_cards[x] += GameConstants_DCWDH.CARD_ESPECIAL_TYPE_LAI_ZI;

					weaveItem_item.addWeaveCard(weave_cards[x]);
				}

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

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants_DCWDH.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);
		int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;

		if (seat_index == _seat_index) {
			for (int c = 0; c < hand_card_count; c++) {
				if (hand_cards[c] == _send_card_data) {
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
		if ((out_ting_count > 0) && (seat_index == _seat_index)) {
			for (int j = 0; j < hand_card_count; j++) {
				for (int k = 0; k < out_ting_count; k++) {
					if (hand_cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
						hand_cards[j] += GameConstants_DCWDH.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}
		}
		for (int i = 0; i < hand_card_count; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		roomResponse.setOutCardCount(out_ting_count);
		for (int i = 0; i < out_ting_count; i++) {
			int ting_card_cout = table._playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			roomResponse.addOutCardTing(table._playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants_DCWDH.CARD_ESPECIAL_TYPE_TING);
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

		if (_seat_index == seat_index) {
			int show_send_card = _send_card_data;
			if (ting_send_card) {
				show_send_card += GameConstants_DCWDH.CARD_ESPECIAL_TYPE_TING;
			}
			table.operate_player_get_card(_seat_index, 1, new int[] { show_send_card }, _seat_index);
		}

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}
		return true;
	}

}
