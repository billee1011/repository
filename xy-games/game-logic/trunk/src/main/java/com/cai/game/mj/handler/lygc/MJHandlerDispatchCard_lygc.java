package com.cai.game.mj.handler.lygc;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.MJTable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerDispatchCard_lygc extends MJHandlerDispatchCard<MJTable> {
	boolean out_send_ting_card = false;

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

		if (table._playerStatus[seat_index].get_status() != GameConstants.INVALID_VALUE) {
			table._playerStatus[seat_index].set_status(GameConstants.INVALID_VALUE);
			if (_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
				table.exe_out_card(_seat_index, card, GameConstants.HU_CARD_TYPE_GANG_KAI);
			} else {
				table.exe_out_card(_seat_index, card, GameConstants.WIK_NULL);
			}
		}

		return true;
	}

	@SuppressWarnings("static-access")
	@Override
	public void exe(MJTable table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();

		if (table.GRR._left_card_count <= 0) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}

			table.process_chi_hu_player_operate_liuju();

			table._cur_banker = table.GRR._banker_player;

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_DRAW), table.game_finish_delay_lygc,
					TimeUnit.SECONDS);

			return;
		}

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index;

		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		--table.GRR._left_card_count;
		table._send_card_data = _send_card_data;

		table._provide_player = _seat_index;

		if (table.DEBUG_CARDS_MODE) {
			_send_card_data = 0x34;
		}

		table._send_card_data = _send_card_data;

		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		boolean has_pi_ci = false;
		if (table.has_rule(GameConstants.GAME_RULE_HENAN_PICI)) {
			table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

			if (table.GRR._cards_index[_seat_index][table._logic.get_ci_card_index()] >= 3) {
				has_pi_ci = true;
				table._playerStatus[_seat_index].add_action(GameConstants.WIK_LYGC_PI_CI);
				table._playerStatus[_seat_index].add_lygc_ci(
						table._logic.switch_to_card_data(table._logic.get_ci_card_index()) + GameConstants.CARD_ESPECIAL_TYPE_CI, _seat_index,
						GameConstants.WIK_LYGC_PI_CI);
			}

			table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]--;
		}

		int card_type = GameConstants.HU_CARD_TYPE_ZIMO;

		if (!table.has_rule(GameConstants.GAME_RULE_HENAN_YCI) && !has_pi_ci) {
			int action = table.analyse_chi_hu_card_henan_lygc_new(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], _send_card_data, chr, card_type);// 自摸

			if (action != GameConstants.WIK_NULL) {
				curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
				curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
			} else {
				chr.set_empty();
			}
		}

		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int count = 0;
		int ting_count = 0;
		int send_card_index = table._logic.switch_to_card_index(_send_card_data);
		out_send_ting_card = false;

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

					if (send_card_index == i) {
						out_send_ting_card = true;
					}
				}

				table.GRR._cards_index[_seat_index][i]++;
			}
		}

		table._playerStatus[_seat_index]._hu_out_card_count = ting_count;

		if (ting_count > 0) {
			table.GRR._cards_index[_seat_index][send_card_index]--;

			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

			table.GRR._cards_index[_seat_index][send_card_index]++;

			for (int i = 0; i < hand_card_count; i++) {
				for (int j = 0; j < ting_count; j++) {
					if (cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
						cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
					}
				}
				if (cards[i] < GameConstants.CARD_ESPECIAL_TYPE_TING) {
					if (table._logic.is_ci_card(table._logic.switch_to_card_index(cards[i]))) {
						cards[i] += GameConstants.CARD_ESPECIAL_TYPE_CI;
					}
				}
			}

			table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);
		}
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		int real_card = _send_card_data;
		if (out_send_ting_card) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		} else {
			if (table._logic.is_ci_card(table._logic.switch_to_card_index(real_card)))
				real_card += GameConstants.CARD_ESPECIAL_TYPE_CI;
		}

		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);

		if (table.has_rule(GameConstants.GAME_RULE_HENAN_BAOCI) && table.GRR._left_card_count == table._bao_ci_start
				&& table.has_display_bao_ci_start == false) {
			table.has_display_bao_ci_start = true;
			table.operate_effect_action(GameConstants.INVALID_SEAT, GameConstants.Effect_Action_Other, 1,
					new long[] { GameConstants.CHR_HENAN_BAO_CI_START }, 5, GameConstants.INVALID_SEAT);
			table._bao_ci_state = GameConstants.LYGC_BAO_CI_SATRT;
		}

		table._provide_card = _send_card_data;

		m_gangCardResult.cbCardCount = 0;
		if (table.GRR._left_card_count >= 0) {
			int cbActionMask = table._logic.analyse_gang_card_all_lygc(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], m_gangCardResult, true, GameConstants.INVALID_CARD);

			if (cbActionMask != GameConstants.WIK_NULL) {
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					if (table.has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && table._logic.is_magic_card(m_gangCardResult.cbCardData[i])) {// 鬼牌不能杆
						continue;
					}

					curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
					curPlayerStatus.add_action(GameConstants.WIK_GANG);

					if ((table.is_mj_type(GameConstants.GAME_TYPE_NEW_LUO_YANG_GANG_CI) || table.is_mj_type(GameConstants.GAME_TYPE_HENAN_LYGC))
							&& !has_pi_ci) {
						int ci_card = table._logic.switch_to_card_data(table._logic.get_ci_card_index());

						int gang_card_num = table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(m_gangCardResult.cbCardData[i])];
						table.GRR._cards_index[_seat_index][table._logic
								.switch_to_card_index(m_gangCardResult.cbCardData[i])] = GameConstants.INVALID_VALUE;
						boolean flag = false;
						int cbCardCount = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);

						if (table.GRR._weave_count[_seat_index] == 3 && cbCardCount == 1) {
							table.GRR._weave_items[_seat_index][table.GRR._weave_count[_seat_index]].public_card = m_gangCardResult.isPublic[i];
							table.GRR._weave_items[_seat_index][table.GRR._weave_count[_seat_index]].center_card = m_gangCardResult.cbCardData[i];
							table.GRR._weave_items[_seat_index][table.GRR._weave_count[_seat_index]].weave_kind = GameConstants.WIK_GANG;
							table.GRR._weave_items[_seat_index][table.GRR._weave_count[_seat_index]].provide_player = _seat_index;
							table.GRR._weave_count[_seat_index]++;
							flag = true;
						}

						boolean IS_CI = table.estimate_lygc_gang_ci(_seat_index, ci_card);
						if (IS_CI && table._playerStatus[_seat_index].has_action_by_code(GameConstants.WIK_LYGC_CI) == false) {
							table._playerStatus[_seat_index].add_action(GameConstants.WIK_LYGC_CI);
							table._playerStatus[_seat_index].add_lygc_ci(m_gangCardResult.cbCardData[i], _seat_index, GameConstants.WIK_LYGC_CI);// 加上杠
						}

						table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(m_gangCardResult.cbCardData[i])] = gang_card_num;
						if (flag) {
							table.GRR._weave_count[_seat_index]--;
							table.GRR._weave_items[_seat_index][table.GRR._weave_count[_seat_index]].public_card = 0;
							table.GRR._weave_items[_seat_index][table.GRR._weave_count[_seat_index]].center_card = GameConstants.INVALID_CARD;
							table.GRR._weave_items[_seat_index][table.GRR._weave_count[_seat_index]].weave_kind = GameConstants.WIK_NULL;
							table.GRR._weave_items[_seat_index][table.GRR._weave_count[_seat_index]].provide_player = GameConstants.INVALID_VALUE;
						}
					}
				}
			}
		}

		if (curPlayerStatus.has_action_by_code(GameConstants.WIK_GANG)) {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_OR_OUT_CARD);
			table.operate_player_action(_seat_index, false);
		} else if (curPlayerStatus.has_action()) {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
		}

		return;
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

		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}

		playerStatus.operate(operate_code, operate_card);
		table.change_player_status(seat_index, GameConstants.INVALID_VALUE);

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			table._playerStatus[seat_index].clean_action();

			if (table._playerStatus[seat_index].lock_huan_zhang()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), seat_index, _send_card_data), GameConstants.DELAY_AUTO_OUT_CARD,
						TimeUnit.MILLISECONDS);
			} else {
				table.change_player_status(seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}

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
		}
			break;
		case GameConstants.WIK_LYGC_CI: {
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					table.LYGC_CI_STATE = true;

					table.exe_gang(_seat_index, _seat_index, operate_card, GameConstants.WIK_GANG, m_gangCardResult.type[i], true, false);
					return true;
				}
			}
		}
			break;
		case GameConstants.WIK_LYGC_PI_CI: {
			table.GRR._chi_hu_rights[_seat_index].set_valid(true);

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table.GRR._chi_hu_rights[_seat_index].opr_or(GameConstants.CHR_HENAN_PI_CI);
			table._cur_banker = _seat_index;
			table.process_chi_hu_player_operate_pi_ci(_seat_index, table._logic.switch_to_card_data(table._logic.get_ci_card_index()), true);
			table.process_chi_hu_player_score_henan_lygc(_seat_index, _seat_index, operate_card, GameConstants.HU_CARD_TYPE_PI_CI, true);

			table._player_result.zi_mo_count[_seat_index]++;
			table._player_result.pi_ci_ci_shu[_seat_index]++;

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
					table.game_finish_delay_lygc, TimeUnit.SECONDS);

			return true;
		}
		case GameConstants.WIK_ZI_MO: {
			table.GRR._chi_hu_rights[_seat_index].set_valid(true);

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table._cur_banker = _seat_index;
			table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			table.process_chi_hu_player_score_henan_lygc(_seat_index, _seat_index, operate_card, GameConstants.HU_CARD_TYPE_ZIMO, true);

			table._player_result.zi_mo_count[_seat_index]++;
			table._player_result.zi_mo_ci_shu[_seat_index]++;

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
					table.game_finish_delay_lygc, TimeUnit.SECONDS);

			return true;
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

			if (i == _seat_index) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}

		tableResponse.setSendCardData(0);

		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);

		if (seat_index == _seat_index) {
			table._logic.remove_card_by_data(cards, _send_card_data);
		}

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

		int real_card = _send_card_data;
		if (out_send_ting_card) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		} else {
			if (table._logic.is_ci_card(table._logic.switch_to_card_index(real_card)))
				real_card += GameConstants.CARD_ESPECIAL_TYPE_CI;
		}

		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index);
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		// 听牌显示
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		return true;
	}
}
