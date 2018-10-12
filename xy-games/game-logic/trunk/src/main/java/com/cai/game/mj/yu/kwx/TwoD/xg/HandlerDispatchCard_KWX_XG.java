package com.cai.game.mj.yu.kwx.TwoD.xg;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_KWX;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerDispatchCard_KWX_XG extends MJHandlerDispatchCard<Table_KWX_XG_2D> {
	boolean ting_send_card = false;
	protected int _seat_index;
	protected int _send_card_data;
	protected int _type;

	protected GangCardResult m_gangCardResult;

	public HandlerDispatchCard_KWX_XG() {
		m_gangCardResult = new GangCardResult();
	}

	@Override
	public void reset_status(int seat_index, int type) {
		_seat_index = seat_index;
		_type = type;
	}

	@SuppressWarnings("static-access")
	@Override
	public void exe(Table_KWX_XG_2D table) {

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants_KWX.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();
		table._playerStatus[_seat_index].clear_cards_abandoned_peng();
		table._playerStatus[_seat_index].clear_cards_abandoned_hu();

		if (table.GRR._left_card_count == 0) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants_KWX.INVALID_VALUE;
			}
			/*
			 * int liang_count = 0;
			 * 
			 * for (int i = 0; i < table.getTablePlayerNumber(); i++) { if
			 * (table.player_liang[i] == 1) { liang_count++; } } if (liang_count
			 * != table.getTablePlayerNumber() && liang_count != 0) { for (int i
			 * = 0; i < table.getTablePlayerNumber(); i++) {
			 * table.GRR._game_score[table.first_liang] -= table.get_di_fen();
			 * table.GRR._game_score[i] += table.get_di_fen(); } }
			 */

			// TODO:流局谁摸最后一张牌谁做庄
			// table._cur_banker = (_seat_index + table.getTablePlayerNumber() -
			// 1) % table.getTablePlayerNumber();
			table.cal_cha_jiao();
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants_KWX.Game_End_DRAW), 0, TimeUnit.SECONDS);

			return;
		}

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();
		table.pass_hu_fan[_seat_index] = 0;

		table._current_player = _seat_index;

		table._send_card_count++;

		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];

		--table.GRR._left_card_count;

		table._provide_player = _seat_index;

		if (table.DEBUG_CARDS_MODE) {
			_send_card_data = 0x14;
		}
		int show_send_card = _send_card_data;
		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		int card_type = GameConstants_KWX.HU_CARD_TYPE_ZI_MO;
		if (_type == GameConstants_KWX.GANG_TYPE_AN_GANG || _type == GameConstants_KWX.GANG_TYPE_ADD_GANG
				|| _type == GameConstants_KWX.GANG_TYPE_JIE_GANG) {
			card_type = GameConstants_KWX.HU_CARD_TYPE_GANG_KAI_HUA;
		} else {
			table.clearContinueGang();
		}
		int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], _send_card_data, chr, card_type, _seat_index);

		if (action != GameConstants_KWX.WIK_NULL) {
			curPlayerStatus.add_action(GameConstants_KWX.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
		} else {
			table.GRR._chi_hu_rights[_seat_index].set_empty();
			chr.set_empty();
		}

		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int count = 0;
		int ting_count = 0;
		int send_card_index = table._logic.switch_to_card_index(_send_card_data);
		ting_send_card = false;

		int card_type_count = GameConstants_KWX.MAX_ZI_FENG;

		for (int i = 0; i < card_type_count && table.player_liang[_seat_index] != 1; i++) {
			if (table._logic.is_magic_index(i))
				continue;

			count = table.GRR._cards_index[_seat_index][i];

			if (count > 0) {
				table.GRR._cards_index[_seat_index][i]--;

				table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(
						table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
						table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);

				if (table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] > 0) {

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
			// table.GRR._cards_index[_seat_index][send_card_index]--;

			int cards[] = new int[GameConstants_KWX.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

			// table.GRR._cards_index[_seat_index][send_card_index]++;

			for (int i = 0; i < hand_card_count; i++) {
				for (int j = 0; j < ting_count; j++) {
					if (cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
						cards[i] += GameConstants_KWX.CARD_ESPECIAL_TYPE_TING;
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
			table.filterHandCards(_seat_index, cards, hand_card_count);
			for (int c = 0; c < hand_card_count; c++) {
				if (cards[c] == _send_card_data || cards[c] == (_send_card_data + GameConstants_KWX.CARD_ESPECIAL_TYPE_NON_OUT)
						|| cards[c] == (_send_card_data + GameConstants_KWX.CARD_ESPECIAL_TYPE_TING)) {
					cards[c] = 0;
					break;
				}
			}
			int show_cards[] = new int[GameConstants_KWX.MAX_COUNT];
			int show_hand_card_count = 0;
			for (int c = 0; c < hand_card_count; c++) {
				if (cards[c] != 0) {
					show_cards[show_hand_card_count++] = cards[c];
				}
			}
			table.operate_player_cards_with_ting(_seat_index, show_hand_card_count, show_cards, 0, null);
		} else {
			if (table.player_liang[_seat_index] == 1) {
				// table.GRR._cards_index[_seat_index][send_card_index]--
				// int[] temp_cards_index =
				// Arrays.copyOf(table.GRR._cards_index[_seat_index],
				// table.GRR._cards_index[_seat_index].length);
				// table.liangShowCard(table, _seat_index, 0, temp_cards_index);
				// table.GRR._cards_index[_seat_index][send_card_index]++;
			} else {
				table.GRR._cards_index[_seat_index][send_card_index]--;
				int cards[] = new int[GameConstants_KWX.MAX_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
				// 刷新手牌
				table.filterHandCards(_seat_index, cards, hand_card_count);
				table.operate_player_cards(_seat_index, hand_card_count, cards, 0, null);
				table.GRR._cards_index[_seat_index][send_card_index]++;
			}
		}
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		int cards[] = new int[GameConstants_KWX.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

		if (table.filterHandCard(_seat_index, _send_card_data) && table.filterHandCards(_seat_index, cards, hand_card_count) != hand_card_count) {
			show_send_card += GameConstants_KWX.CARD_ESPECIAL_TYPE_NON_OUT;
		} else if (ting_send_card) {
			show_send_card += GameConstants_KWX.CARD_ESPECIAL_TYPE_TING;
		}
		table._provide_card = _send_card_data;

		table.operate_player_get_card(_seat_index, 1, new int[] { show_send_card }, GameConstants_KWX.INVALID_SEAT);

		m_gangCardResult.cbCardCount = 0;

		int cbActionMask = table.analyse_gang_hong_zhong_all(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], m_gangCardResult, true, table._playerStatus[_seat_index].get_cards_abandoned_gang(), _seat_index,
				_send_card_data, false);

		for (int w = 0; w < table.GRR._weave_count[_seat_index]; w++) {
			if (table.GRR._weave_items[_seat_index][w].weave_kind == GameConstants_KWX.WIK_LIANG
					&& table.GRR._weave_items[_seat_index][w].center_card == _send_card_data) {
				cbActionMask |= GameConstants.WIK_GANG;

				int index = m_gangCardResult.cbCardCount++;
				m_gangCardResult.cbCardData[index] = _send_card_data;
				m_gangCardResult.isPublic[index] = 0;// 明刚
				m_gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
				break;
			}
		}
		if (cbActionMask != GameConstants_KWX.WIK_NULL && table.GRR._left_card_count != 0) {
			if (table.GRR._left_card_count == 0 && m_gangCardResult.cbCardCount == 1
					&& m_gangCardResult.type[0] == GameConstants_KWX.GANG_TYPE_AN_GANG) {
				// TODO:荒庄前最后一张：摸到能明杠、暗杠的牌不能杠牌
			} else {
				curPlayerStatus.add_action(GameConstants_KWX.WIK_GANG);
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
				}
			}
		}

		if (curPlayerStatus.has_action()) {
			if (table.player_liang[_seat_index] == 1 && table._playerStatus[_seat_index].has_action_by_code(GameConstants_KWX.WIK_ZI_MO)) {
				if ((_type == GameConstants_KWX.GANG_TYPE_AN_GANG || _type == GameConstants_KWX.GANG_TYPE_ADD_GANG
						|| _type == GameConstants_KWX.GANG_TYPE_JIE_GANG) && table.player_continue_gang[_seat_index] > 1) {
					if (table.player_continue_gang[_seat_index] == 2) {
						table.GRR._chi_hu_rights[_seat_index].opr_or(GameConstants_KWX.CHR_PAO_GANG_GANG);
					}
					if (table.player_continue_gang[_seat_index] == 3) {
						table.GRR._chi_hu_rights[_seat_index].opr_or(GameConstants_KWX.CHR_PAO_GANG_GANG_GANG);
					}
					if (table.player_continue_gang[_seat_index] == 4) {
						table.GRR._chi_hu_rights[_seat_index].opr_or(GameConstants_KWX.CHR_PAO_GANG_GANG_GANG_GANG);
					}
				}
				table.GRR._chi_hu_rights[_seat_index].set_valid(true);

				table._cur_banker = _seat_index;

				table.set_niao_card(_seat_index, 0, true, 0);

				table.GRR._chi_hu_card[_seat_index][0] = _send_card_data;

				table.GRR._win_order[_seat_index] = 1;

				table.process_chi_hu_player_operate(_seat_index, _send_card_data, true);
				table.process_chi_hu_player_score(_seat_index, _seat_index, _send_card_data, true);

				table._player_result.zi_mo_count[_seat_index]++;

				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants_KWX.Game_End_NORMAL), 0, TimeUnit.SECONDS);

				return;
			} else {
				table.change_player_status(_seat_index, GameConstants_KWX.Player_Status_OPR_CARD);
				table.operate_player_action(_seat_index, false);
				// if
				// (curPlayerStatus.has_action_by_code(GameConstants_KWX.WIK_LIANG))
				// {
				table.change_player_status(_seat_index, GameConstants_KWX.Player_Status_OUT_CARD);
				table.operate_player_status();
				// }
			}
		} else {
			// 不能换章,自动出牌
			if (table.player_liang[_seat_index] == 1) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), GameConstants_KWX.GANG_LAST_CARD_DELAY,
						TimeUnit.MILLISECONDS);
			} else {
				table.change_player_status(_seat_index, GameConstants_KWX.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
		}

		return;
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

		// if (_type == GameConstants_KWX.DispatchCard_Type_Tian_Hu) {
		// // 出牌
		// table.exe_out_card(_seat_index, card,
		// GameConstants_KWX.OutCard_Type_Di_Hu);
		// } else {
		// 出牌
		table.exe_out_card(_seat_index, card, _type);
		// }

		return true;
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

		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}

		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();

		if (operate_code == GameConstants_KWX.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants_KWX.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants_KWX.WIK_NULL }, 1);

			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (GameConstants_KWX.GANG_TYPE_ADD_GANG == m_gangCardResult.type[i]) {
					table._playerStatus[_seat_index].add_cards_abandoned_gang(table._logic.switch_to_card_index(m_gangCardResult.cbCardData[i]));
				}
			}
			// 不能换章,自动出牌
			if (table.player_liang[_seat_index] == 1) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), GameConstants_KWX.DELAY_AUTO_OUT_CARD,
						TimeUnit.MILLISECONDS);
			} else {
				table.change_player_status(_seat_index, GameConstants_KWX.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
			return true;
		}

		switch (operate_code) {
		case GameConstants_KWX.WIK_GANG: {
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					if (operate_card == _send_card_data) {
						table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, true);
					} else {
						table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, false);
					}
					// if (m_gangCardResult.cbCardData[i] != _send_card_data) {
					return true;
				}
			}
		}
		case GameConstants_KWX.WIK_ZI_MO: {

			if ((_type == GameConstants_KWX.GANG_TYPE_AN_GANG || _type == GameConstants_KWX.GANG_TYPE_ADD_GANG
					|| _type == GameConstants_KWX.GANG_TYPE_JIE_GANG) && table.player_continue_gang[_seat_index] > 1) {
				if (table.player_continue_gang[_seat_index] == 2) {
					table.GRR._chi_hu_rights[_seat_index].opr_or(GameConstants_KWX.CHR_PAO_GANG_GANG);
				}
				if (table.player_continue_gang[_seat_index] == 3) {
					table.GRR._chi_hu_rights[_seat_index].opr_or(GameConstants_KWX.CHR_PAO_GANG_GANG_GANG);
				}
				if (table.player_continue_gang[_seat_index] == 4) {
					table.GRR._chi_hu_rights[_seat_index].opr_or(GameConstants_KWX.CHR_PAO_GANG_GANG_GANG_GANG);
				}
			}

			table.GRR._chi_hu_rights[_seat_index].set_valid(true);

			table._cur_banker = _seat_index;

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table.GRR._win_order[_seat_index] = 1;

			table.set_niao_card(_seat_index, 0, true, 0);

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

				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);

				int[] weave_cards = new int[4];
				int count = table._logic.get_weave_card_huangshi(table.GRR._weave_items[i][j].weave_kind, table.GRR._weave_items[i][j].center_card,
						weave_cards);
				for (int x = 0; x < count; x++) {
					if (table._logic.is_magic_card(weave_cards[x]))
						weave_cards[x] += GameConstants_KWX.CARD_ESPECIAL_TYPE_LAI_ZI;

					weaveItem_item.addWeaveCard(weave_cards[x]);
				}

				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);
			//
			// if (table.player_liang[i] == 1) {
			// int[] temp_cards_index = Arrays.copyOf(table.GRR._cards_index[i],
			// table.GRR._cards_index[i].length);
			// if (i == _seat_index) {
			// temp_cards_index[table._logic.switch_to_card_index(_send_card_data)]--;
			// }
			// int hand_card_count = table.liangShowCard(table, i, 0,
			// temp_cards_index);
			// if (i == _seat_index) {
			// tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i])
			// - 1);
			// } else {
			// tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			// }
			// } else {
			if (i == _seat_index && _seat_index == seat_index) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
				// }
			}
		}

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants_KWX.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		table.filterHandCards(seat_index, hand_cards, hand_card_count);
		if (seat_index == _seat_index) {
			for (int c = 0; c < hand_card_count; c++) {
				if (hand_cards[c] == _send_card_data || hand_cards[c] == (_send_card_data + GameConstants_KWX.CARD_ESPECIAL_TYPE_NON_OUT)
						|| hand_cards[c] == (_send_card_data + GameConstants_KWX.CARD_ESPECIAL_TYPE_TING)) {
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

		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		//
		// if (table.player_liang[seat_index] == 1) {
		// int[] temp_cards_index =
		// Arrays.copyOf(table.GRR._cards_index[seat_index],
		// table.GRR._cards_index[seat_index].length);
		// if (seat_index == _seat_index) {
		// temp_cards_index[table._logic.switch_to_card_index(_send_card_data)]--;
		// }
		// table.liangShowCard(table, seat_index, 0, temp_cards_index);
		// }

		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			if (table.player_liang[p] != 1) {
				continue;
			}
			int[] temp_cards_index = Arrays.copyOf(table.GRR._cards_index[p], table.GRR._cards_index[p].length);
			if (p == _seat_index) {
				temp_cards_index[table._logic.switch_to_card_index(_send_card_data)]--;
			}
			table.liangShowCard(table, p, 0, temp_cards_index);
		}
		if ((_seat_index == seat_index || table.player_liang[_seat_index] == 1) || table._game_type_index == GameConstants.GAME_TYPE_KWX_XG_2D) {
			int show_send_card = _send_card_data;
			if (table.filterHandCard(_seat_index, _send_card_data)
					&& table.filterHandCards(_seat_index, hand_cards, hand_card_count) != hand_card_count) {
				show_send_card += GameConstants_KWX.CARD_ESPECIAL_TYPE_NON_OUT;
			} else if (ting_send_card) {
				show_send_card += GameConstants_KWX.CARD_ESPECIAL_TYPE_TING;
			}
			table.operate_player_get_card(_seat_index, 1, new int[] { show_send_card }, _seat_index);
		}

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		table.handler_be_in_room_chu_zi(seat_index);
		return true;
	}
}
