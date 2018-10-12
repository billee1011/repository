package com.cai.game.mj.handler.henanxc;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.util.GameDescUtil;
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

public class MJHandlerDispatchCard_XC extends MJHandlerDispatchCard<MJTable> {
	private boolean can_bao_ting;
	boolean ting_send_card = false;

	@SuppressWarnings("static-access")
	@Override
	public void exe(MJTable table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();

		int llcard = 0;
		if (table.has_rule(GameConstants.GAME_RULE_HENAN_SHUAIHUN) && table.has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			llcard = 20 + table.GRR._piao_lai_count * 10;
		} else if (table.has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			llcard = 20;
		}

		if (table.GRR._left_card_count <= llcard) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}

			table._cur_banker = table.GRR._banker_player;

			table._shang_zhuang_player = GameConstants.INVALID_SEAT;
			table._lian_zhuang_player = table._cur_banker;

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_DRAW),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

			return;
		}

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index;

		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		--table.GRR._left_card_count;

		if (table.DEBUG_CARDS_MODE) {
			_send_card_data = 0x07;
		}

		table._send_card_data = _send_card_data;

		table._provide_player = _seat_index;

		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		can_bao_ting = false;

		if (GameDescUtil.has_rule(table.getGameRuleIndexEx(), GameConstants.GAME_RULE_HENAN_BAO_TING)) {
			if (table.GRR._cards_index[_seat_index][table._logic.get_magic_card_index(0)] == 4
					|| (table.GRR._cards_index[_seat_index][table._logic.get_magic_card_index(0)] == 3
							&& table._logic.is_magic_card(_send_card_data))) {
				curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
				curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
			}

			if (table._playerStatus[_seat_index].is_bao_ting() == true) {
			} else {
				int send_card_index = table._logic.switch_to_card_index(_send_card_data);

				table.GRR._cards_index[_seat_index][send_card_index]++;

				int count = 0;
				int ting_count = 0;
				ting_send_card = false;

				for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
					count = table.GRR._cards_index[_seat_index][i];

					if (count > 0) {
						table.GRR._cards_index[_seat_index][i]--;

						table.xc_analyse_type = table.XC_ANALYSE_BAO_TING;

						table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_xc_ting_card(_seat_index,
								table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
								table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index]);

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

				table.GRR._cards_index[_seat_index][send_card_index]--;

				table._playerStatus[_seat_index]._hu_out_card_count = ting_count;

				if (ting_count > 0) {
					can_bao_ting = true;

					int cards[] = new int[GameConstants.MAX_COUNT];

					int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

					for (int i = 0; i < hand_card_count; i++) {
						for (int j = 0; j < ting_count; j++) {
							if (cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
								cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
								break;
							}
						}

						if (table.has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && cards[i] < GameConstants.CARD_ESPECIAL_TYPE_TING
								&& table._logic.is_magic_card(cards[i])) {
							cards[i] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
						}
					}

					table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);

					table.GRR._cards_index[_seat_index][send_card_index]++;

					int real_card = _send_card_data;

					if (ting_send_card) {
						real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
					} else if (table.has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && table._logic.is_magic_card(_send_card_data)) {
						real_card += GameConstants.CARD_ESPECIAL_TYPE_HUN;
					}

					table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);

					curPlayerStatus.add_action(GameConstants.WIK_BAO_TING);
				}
			}
		}

		if (!can_bao_ting) {
			if ((table.getRuleValue(GameConstants.GAME_RULE_HENAN_BAO_TING) != 1 && table._playerStatus[_seat_index]._hu_card_count > 0)
					|| table._playerStatus[_seat_index].is_bao_ting() == true) {
				int action = GameConstants.WIK_NULL;

				for (int i = 0; i < table._playerStatus[_seat_index]._hu_card_count; i++) {
					int hucard = table._playerStatus[_seat_index]._hu_cards[i];
					hucard = hucard > GameConstants.CARD_ESPECIAL_TYPE_HUN ? hucard - GameConstants.CARD_ESPECIAL_TYPE_HUN : hucard;

					if (hucard == _send_card_data || hucard == -1) {
						table.xc_analyse_type = table.XC_ANALYSE_NORMAL;

						action = table.analyse_chi_hu_card_henan_xc(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
								table.GRR._weave_count[_seat_index], _send_card_data, chr, GameConstants.HU_CARD_TYPE_ZIMO, true);
						break;
					}
				}

				if (action != GameConstants.WIK_NULL) {
					curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
					curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
				} else {
					table.GRR._chi_hu_rights[_seat_index].set_empty();
					chr.set_empty();
				}
			}

			// 加到手牌
			table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

			if (!GameDescUtil.has_rule(table.getGameRuleIndexEx(), GameConstants.GAME_RULE_HENAN_BAO_TING)) {
				// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
				int count = 0;
				int ting_count = 0;
				int send_card_index = table._logic.switch_to_card_index(_send_card_data);
				ting_send_card = false;

				int card_type_count = GameConstants.MAX_ZI_FENG;

				for (int i = 0; i < card_type_count; i++) {
					count = table.GRR._cards_index[_seat_index][i];

					if (count > 0) {
						table.GRR._cards_index[_seat_index][i]--;

						table.xc_analyse_type = table.XC_ANALYSE_TING;

						table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_xc_ting_card(_seat_index,
								table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
								table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index]);

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
					table.GRR._cards_index[_seat_index][send_card_index]--;

					int cards[] = new int[GameConstants.MAX_COUNT];
					int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

					table.GRR._cards_index[_seat_index][send_card_index]++;

					for (int i = 0; i < hand_card_count; i++) {
						for (int j = 0; j < ting_count; j++) {
							if (cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
								cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
								break;
							}
						}

						if (table.has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && cards[i] < GameConstants.CARD_ESPECIAL_TYPE_TING
								&& table._logic.is_magic_card(cards[i])) {
							cards[i] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
						}
					}

					table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);
				}
				// TODO: 出任意一张牌时，能胡哪些牌 -- End
			}

			int real_card = _send_card_data;

			if (ting_send_card) {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
			} else if (table.has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && table._logic.is_magic_card(_send_card_data)) {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_HUN;
			}

			table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);
		}

		table._provide_card = _send_card_data;

		m_gangCardResult.cbCardCount = 0;
		if (table.GRR._left_card_count > llcard) {
			int cbActionMask = table._logic.analyse_gang_card_all_xc(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], m_gangCardResult, table.has_rule(GameConstants.GAME_RULE_HENAN_HTG));

			if (cbActionMask != GameConstants.WIK_NULL) {
				boolean has_gang = false;

				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					if ((table._playerStatus[_seat_index].is_bao_ting() == false) || (table._playerStatus[_seat_index].is_bao_ting() == true
							&& table.check_gang_huan_zhang_xc(_seat_index, m_gangCardResult.cbCardData[i]) == false)) {
						has_gang = true;
						// 加上杠
						curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
					}
				}

				if (has_gang == true) {
					curPlayerStatus.add_action(GameConstants.WIK_GANG); // 杠
				}
			}
		}

		if (curPlayerStatus.has_action()) {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			if (table._playerStatus[_seat_index].is_bao_ting()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), 1000, TimeUnit.MILLISECONDS);
			} else {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
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

		table.change_player_status(_seat_index, GameConstants.INVALID_VALUE);
		table.operate_player_action(_seat_index, true);

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			if (table._playerStatus[_seat_index].is_bao_ting()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), 1000, TimeUnit.MILLISECONDS);
			} else {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}

			if (can_bao_ting) {
				table.operate_player_cards_flag(_seat_index, 0, null, 0, null);
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
		case GameConstants.WIK_ZI_MO: {
			table.GRR._chi_hu_rights[_seat_index].set_valid(true);
			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table._cur_banker = _seat_index;

			table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			table.process_chi_hu_player_score_henan_xc(_seat_index, _seat_index, operate_card, true);

			table._player_result.zi_mo_count[_seat_index]++;

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL), GameConstants.GAME_FINISH_DELAY,
					TimeUnit.SECONDS);

			return true;
		}
		case GameConstants.WIK_BAO_TING: {
			operate_card -= GameConstants.CARD_ESPECIAL_TYPE_TING;

			if (table._logic.is_valid_card(operate_card) == false) {
				table.log_error("出牌,牌型出错");
				return false;
			}

			// 效验参数
			if (seat_index != _seat_index) {
				table.log_error("出牌,没到出牌");
				return false;
			}

			// 删除扑克
			if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], operate_card) == false) {
				table.log_error("出牌删除出错");
				return false;
			}

			// 报听
			table.exe_out_card_bao_ting(_seat_index, operate_card, GameConstants.WIK_NULL);
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
				if (j == table.GRR._chi_hu_rights[i].bao_ting_index) {
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_BAO_TING);
				} else {
					if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
						int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_HUN);
					} else {
						int_array.addItem(table.GRR._discard_cards[i][j]);
					}
				}
			}

			tableResponse.addDiscardCards(int_array);
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);

			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				if (table._logic.is_magic_card(table.GRR._weave_items[i][j].center_card)) {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_HUN);
				} else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
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

		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		if (seat_index == _seat_index) {
			table._logic.remove_card_by_data(hand_cards, _send_card_data);
		}

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;

		if ((out_ting_count > 0) && (seat_index == _seat_index)) {
			if (table._playerStatus[_seat_index].has_action_by_code(GameConstants.WIK_BAO_TING)
					|| !GameDescUtil.has_rule(table.getGameRuleIndexEx(), GameConstants.GAME_RULE_HENAN_BAO_TING)) {
				roomResponse.setOutCardCount(out_ting_count);

				for (int j = 0; j < hand_card_count; j++) {
					for (int k = 0; k < out_ting_count; k++) {
						if (hand_cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
							hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
							break;
						}
					}
					if (table.has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && hand_cards[j] < GameConstants.CARD_ESPECIAL_TYPE_TING
							&& table._logic.is_magic_card(hand_cards[j])) {
						hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
					}
				}
			}
		} else {
			for (int j = 0; j < hand_card_count; j++) {
				if (table.has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && table._logic.is_magic_card(hand_cards[j])) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
				}
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		if (table._playerStatus[_seat_index].has_action_by_code(GameConstants.WIK_BAO_TING)
				|| !GameDescUtil.has_rule(table.getGameRuleIndexEx(), GameConstants.GAME_RULE_HENAN_BAO_TING)) {
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
		}

		table.send_response_to_player(seat_index, roomResponse);

		int real_card = _send_card_data;
		if (ting_send_card) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		} else if (table.has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && table._logic.is_magic_card(_send_card_data)) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_HUN;
		}

		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index);
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_henan_xc_cards(seat_index, ting_count, ting_cards);
		}

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
