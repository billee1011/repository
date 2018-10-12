package com.cai.game.mj.henan.wuzhi;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_WuZhi;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.util.PBUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;
import com.cai.util.SysParamServerUtil;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.mj.Wuzhi.IntegerArray;
import protobuf.clazz.mj.Wuzhi.LsdyCards;

public class HandlerDispatchCard_WuZhi extends MJHandlerDispatchCard<Table_WuZhi> {
	boolean ting_send_card = false;

	@SuppressWarnings("static-access")
	@Override
	public void exe(Table_WuZhi table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();

		if (table.GRR._left_card_count <= 0) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}

			table.process_next_banker();
			table.process_zi_mo_triple();
			table.process_jie_pao_fang_pao();
			table.process_an_gang_ming_gang();

			table.process_chi_hu_player_operate();

			if (table.hasZiMo || table.hasWin) {
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
						GameConstants.GAME_FINISH_DELAY + 1, TimeUnit.SECONDS);
			} else {
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_DRAW),
						GameConstants.GAME_FINISH_DELAY + 1, TimeUnit.SECONDS);
			}

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
			// _send_card_data = 0x05;
			// _send_card_data = 0x11;
			// _send_card_data = 0x12;
			_send_card_data = 0x11;
		}

		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		int card_type = Constants_WuZhi.HU_CARD_TYPE_ZI_MO;
		if (GameConstants.GANG_TYPE_ADD_GANG == _type || GameConstants.GANG_TYPE_JIE_GANG == _type || GameConstants.GANG_TYPE_AN_GANG == _type)
			card_type = Constants_WuZhi.HU_CARD_TYPE_GANG_KAI;

		table.analyse_state = table.NORMAL;
		int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], _send_card_data, chr, card_type, _seat_index);

		if (action != GameConstants.WIK_NULL) {
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
		} else {
			table.GRR._chi_hu_rights[_seat_index].set_empty();
			chr.set_empty();
		}

		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

		if (table.hasLsdy) {
			// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
			int count = 0;
			int ting_count = 0;
			ting_send_card = false;

			int card_type_count = GameConstants.MAX_ZI_FENG;

			if (table.is_bao_ting[_seat_index] == false) {
				for (int i = 0; i < card_type_count; i++) {
					count = table.lsdyCardsIndex[_seat_index][i];

					if (count > 0) {
						table.lsdyCardsIndex[_seat_index][i]--;

						table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(
								table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
								table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index, true,
								table.lsdyCardsIndex[_seat_index]);

						if (table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] > 0) {
							table._playerStatus[_seat_index]._hu_out_card_ting[ting_count] = table._logic.switch_to_card_data(i);

							ting_count++;
						}

						table.lsdyCardsIndex[_seat_index][i]++;
					}
				}
			}

			table._playerStatus[_seat_index]._hu_out_card_count = ting_count;

			if (ting_count > 0) {
				int tmp_cards[] = new int[GameConstants.MAX_COUNT];
				int tmp_hand_card_count = table._logic.switch_to_cards_data(table.lsdyCardsIndex[_seat_index], tmp_cards);

				for (int i = 0; i < tmp_hand_card_count; i++) {
					for (int j = 0; j < ting_count; j++) {
						if (tmp_cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
							tmp_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
							break;
						}
					}
				}

				int send_card_index = table._logic.switch_to_card_index(_send_card_data);
				table.GRR._cards_index[_seat_index][send_card_index]--;
				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
				table.GRR._cards_index[_seat_index][send_card_index]++;

				table.operate_player_cards_with_ting_lsdy(_seat_index, hand_card_count, cards, tmp_hand_card_count, tmp_cards, 0, null);

				if (table.is_bao_ting[_seat_index] == false) {
					// 能报听了
					curPlayerStatus.add_action(GameConstants.WIK_BAO_TING);
				}
			}
			// TODO: 出任意一张牌时，能胡哪些牌 -- End
		} else {
			// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
			int count = 0;
			int ting_count = 0;
			int send_card_index = table._logic.switch_to_card_index(_send_card_data);
			ting_send_card = false;

			int card_type_count = GameConstants.MAX_ZI_FENG;

			if (table.is_bao_ting[_seat_index] == false) {
				for (int i = 0; i < card_type_count; i++) {
					count = table.GRR._cards_index[_seat_index][i];

					if (count > 0) {
						table.GRR._cards_index[_seat_index][i]--;

						table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(
								table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
								table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index, true, null);

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
						if (cards[i] < GameConstants.CARD_ESPECIAL_TYPE_TING) {
							if (table._logic.is_magic_card(cards[i])) {
								cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
							}
						}
					}
				}

				table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);

				if (table.is_bao_ting[_seat_index] == false) {
					// 能报听了
					curPlayerStatus.add_action(GameConstants.WIK_BAO_TING);
				}
			}
			// TODO: 出任意一张牌时，能胡哪些牌 -- End
		}

		int real_card = _send_card_data;
		if (ting_send_card) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		} else if (table._logic.is_magic_card(_send_card_data)) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}

		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);

		table._provide_card = _send_card_data;

		if (table.hasLsdy) {
			if (table.GRR._left_card_count > 0) {
				m_gangCardResult.cbCardCount = 0;

				int cbActionMask = table._logic.analyse_gang_exclude_magic_card_lsdy(table.GRR._cards_index[_seat_index],
						table.lsdyCardsIndex[_seat_index], _send_card_data, table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],
						m_gangCardResult);

				if (cbActionMask != GameConstants.WIK_NULL) {
					if (table.is_bao_ting[_seat_index]) {
						boolean flag = false;
						for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
							// 删除手牌并放入落地牌之前，保存状态数据信息
							int tmp_card_index = table._logic.switch_to_card_index(m_gangCardResult.cbCardData[i]);
							int tmp_card_count = table.GRR._cards_index[_seat_index][tmp_card_index];
							int tmp_lsdy_card_count = table.lsdyCardsIndex[_seat_index][tmp_card_index];
							int tmp_weave_count = table.GRR._weave_count[_seat_index];

							// 删除手牌并加入一个落地牌组合，如果是暗杠，需要多加一个组合，如果是碰杠，并不需要加，因为等下分析听牌时要用
							// 发牌时，杠牌只要碰杠和暗杠这两种
							table.GRR._cards_index[_seat_index][tmp_card_index] = 0;
							table.lsdyCardsIndex[_seat_index][tmp_card_index] = 0;
							if (GameConstants.GANG_TYPE_AN_GANG == m_gangCardResult.type[i]) {
								table.GRR._weave_items[_seat_index][tmp_weave_count].public_card = 0;
								table.GRR._weave_items[_seat_index][tmp_weave_count].center_card = m_gangCardResult.cbCardData[i];
								table.GRR._weave_items[_seat_index][tmp_weave_count].weave_kind = GameConstants.WIK_GANG;
								table.GRR._weave_items[_seat_index][tmp_weave_count].provide_player = _seat_index;
								++table.GRR._weave_count[_seat_index];
							}

							boolean is_ting_state_after_gang = table.is_ting_card(table.GRR._cards_index[_seat_index],
									table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index,
									table.lsdyCardsIndex[_seat_index]);

							boolean can_gang = false;
							if (is_ting_state_after_gang) {
								can_gang = true;

								ChiHuRight tmpChr = new ChiHuRight();
								for (int tmpCard : table.alreadyWinCardsSet[_seat_index]) {
									chr.set_empty();
									int action_hu = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index],
											table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], tmpCard, tmpChr,
											Constants_WuZhi.HU_CARD_TYPE_ZI_MO, _seat_index);
									if (action_hu == GameConstants.WIK_NULL) {
										can_gang = false;
										break;
									}
								}
							}

							// 还原手牌数据和落地牌数据
							table.GRR._cards_index[_seat_index][tmp_card_index] = tmp_card_count;
							table.lsdyCardsIndex[_seat_index][tmp_card_index] = tmp_lsdy_card_count;
							table.GRR._weave_count[_seat_index] = tmp_weave_count;

							// 杠牌之后还是听牌状态，并不需要在gang handler里更新听牌状态，只要出牌时更新就可以
							if (is_ting_state_after_gang && can_gang) {
								curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
								flag = true;
							}
						}
						if (flag) { // 如果能杠，当前用户状态加上杠牌动作
							curPlayerStatus.add_action(GameConstants.WIK_GANG);
						}
					} else {
						curPlayerStatus.add_action(GameConstants.WIK_GANG);
						for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
							curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
						}
					}
				}
			}
		} else {
			if (table.GRR._left_card_count > 0) {
				m_gangCardResult.cbCardCount = 0;

				int cbActionMask = table._logic.analyse_gang_exclude_magic_card(table.GRR._cards_index[_seat_index], _send_card_data,
						table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult);

				if (cbActionMask != GameConstants.WIK_NULL) {
					if (table.is_bao_ting[_seat_index]) {
						boolean flag = false;
						for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
							// 删除手牌并放入落地牌之前，保存状态数据信息
							int tmp_card_index = table._logic.switch_to_card_index(m_gangCardResult.cbCardData[i]);
							int tmp_card_count = table.GRR._cards_index[_seat_index][tmp_card_index];
							int tmp_weave_count = table.GRR._weave_count[_seat_index];

							// 删除手牌并加入一个落地牌组合，如果是暗杠，需要多加一个组合，如果是碰杠，并不需要加，因为等下分析听牌时要用
							// 发牌时，杠牌只要碰杠和暗杠这两种
							table.GRR._cards_index[_seat_index][tmp_card_index] = 0;
							if (GameConstants.GANG_TYPE_AN_GANG == m_gangCardResult.type[i]) {
								table.GRR._weave_items[_seat_index][tmp_weave_count].public_card = 0;
								table.GRR._weave_items[_seat_index][tmp_weave_count].center_card = m_gangCardResult.cbCardData[i];
								table.GRR._weave_items[_seat_index][tmp_weave_count].weave_kind = GameConstants.WIK_GANG;
								table.GRR._weave_items[_seat_index][tmp_weave_count].provide_player = _seat_index;
								++table.GRR._weave_count[_seat_index];
							}

							boolean is_ting_state_after_gang = table.is_ting_card(table.GRR._cards_index[_seat_index],
									table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index, null);

							// 还原手牌数据和落地牌数据
							table.GRR._cards_index[_seat_index][tmp_card_index] = tmp_card_count;
							table.GRR._weave_count[_seat_index] = tmp_weave_count;

							// 杠牌之后还是听牌状态，并不需要在gang handler里更新听牌状态，只要出牌时更新就可以
							if (is_ting_state_after_gang) {
								curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
								flag = true;
							}
						}
						if (flag) { // 如果能杠，当前用户状态加上杠牌动作
							curPlayerStatus.add_action(GameConstants.WIK_GANG);
						}
					} else {
						curPlayerStatus.add_action(GameConstants.WIK_GANG);
						for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
							curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
						}
					}
				}
			}
		}

		if (table.is_bao_ting[_seat_index]) {
			if (curPlayerStatus.has_zi_mo()) {
				table.exe_jian_pao_hu(_seat_index, GameConstants.WIK_ZI_MO, _send_card_data);
			} else if (curPlayerStatus.has_action_by_code(GameConstants.WIK_GANG)) {
				table.operate_player_action(_seat_index, false);
				table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			} else {
				table.operate_player_action(_seat_index, true);
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), SysParamServerUtil.auto_out_card_time_mj(),
						TimeUnit.MILLISECONDS);
			}

			return;
		} else {
			// 判断玩家有没有杠牌或者胡牌的动作，如果有，改变玩家状态，并在客户端弹出相应的操作按钮
			if (curPlayerStatus.has_action()) {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
				table.operate_player_action(_seat_index, false);
			} else {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
		}

		return;
	}

	@Override
	public boolean handler_operate_card(Table_WuZhi table, int seat_index, int operate_code, int operate_card) {
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
		playerStatus.clean_status();

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			if (table.is_bao_ting[_seat_index]) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), SysParamServerUtil.auto_out_card_time_mj(),
						TimeUnit.MILLISECONDS);
			} else {
				table.change_player_status(seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}

			return true;
		}

		if (table._playerStatus[seat_index].has_zi_mo() && operate_code != GameConstants.WIK_ZI_MO) {
			table._playerStatus[seat_index].chi_hu_round_invalid();

			// 清空CHR
			table.GRR._chi_hu_rights[seat_index].set_empty();
		}

		switch (operate_code) {
		case GameConstants.WIK_GANG: {
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, false);
					return true;
				}
			}
			return false;
		}
		case GameConstants.WIK_ZI_MO: {
			table.hasZiMo = true;
			table.hasWin = true;

			table.GRR._chi_hu_rights[_seat_index].set_valid(true);

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table.ziMoCardsData[_seat_index].add(operate_card);
			table.alreadyWinCardsSet[_seat_index].add(operate_card);

			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

			table._player_result.zi_mo_count[_seat_index]++;
			table.totalZiMo[_seat_index]++;

			if (!table.GRR._chi_hu_rights[_seat_index].opr_and(Constants_WuZhi.CHR_GANG_KAI).is_empty()) {
				table.gangKaiCount[_seat_index]++;
			} else {
				table.ziMoCount[_seat_index]++;
			}

			int send_card_index = table._logic.switch_to_card_index(_send_card_data);

			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, 1, new long[] { GameConstants.WIK_ZI_MO }, 1,
					GameConstants.INVALID_SEAT);

			table.GRR._cards_index[_seat_index][send_card_index]--;

			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

			for (int i = 0; i < hand_card_count; i++) {
				if (table._logic.is_magic_card(cards[i])) {
					cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
			}

			if (table.hasLsdy) {
				table.operate_player_cards_lsdy(_seat_index, hand_card_count, cards, 0, null);
			} else {
				table.operate_player_cards(_seat_index, hand_card_count, cards, 0, null);
			}

			int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
			int ting_count = table._playerStatus[_seat_index]._hu_card_count;

			if (ting_count > 0) {
				table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards, table.ziMoCardsData[_seat_index]);
			}

			// 当前玩法第三次自摸的时候，才能结束游戏
			if (table.ziMoCount[_seat_index] + table.gangKaiCount[_seat_index] == 3) {
				table.process_next_banker();
				table.process_zi_mo_triple();
				table.process_jie_pao_fang_pao();
				table.process_an_gang_ming_gang();

				table.process_chi_hu_player_operate();

				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
						GameConstants.GAME_FINISH_DELAY + 1, TimeUnit.SECONDS);
			} else {
				int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
			}

			return true;
		}
		case GameConstants.WIK_BAO_TING: {
			operate_card = table.get_real_card(operate_card);

			if (table._logic.is_valid_card(operate_card) == false) {
				table.log_error("出牌,牌型出错");
				return false;
			}

			if (seat_index != _seat_index) {
				table.log_error("出牌,没到出牌");
				return false;
			}

			if (table.hasLsdy) {
				if (table._logic.remove_card_by_index(table.lsdyCardsIndex[_seat_index], operate_card) == false) {
					table.log_error("出牌删除出错");
					return false;
				}
			} else {
				if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], operate_card) == false) {
					table.log_error("出牌删除出错");
					return false;
				}
			}

			table.exe_out_card_bao_ting(_seat_index, operate_card, GameConstants.WIK_NULL);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_WuZhi table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		// 设置骰子点数
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[0]);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[1]);

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
				int_array.addItem(table.GRR._discard_cards[i][j]);
			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				if (table.GRR._weave_items[i][j].public_card == 0 && i != seat_index) {
					weaveItem_item.setCenterCard(-1);
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

		int lsdy_cards[] = new int[GameConstants.MAX_COUNT];
		int tmpCount = 0;
		if (table.hasLsdy)
			tmpCount = table._logic.switch_to_cards_data(table.lsdyCardsIndex[seat_index], lsdy_cards);

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;

		if ((out_ting_count > 0) && (seat_index == _seat_index)) {
			if (table.hasLsdy) {
				for (int j = 0; j < tmpCount; j++) {
					for (int k = 0; k < out_ting_count; k++) {
						if (lsdy_cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
							lsdy_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
							break;
						}
					}
				}
			} else {
				for (int j = 0; j < hand_card_count; j++) {
					for (int k = 0; k < out_ting_count; k++) {
						if (hand_cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
							hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
							break;
						}
						if (hand_cards[j] < GameConstants.CARD_ESPECIAL_TYPE_TING) {
							if (table._logic.is_magic_card(hand_cards[j])) {
								hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
							}
						}
					}
				}
			}
		} else {
			for (int j = 0; j < hand_card_count; j++) {
				if (table._logic.is_magic_card(hand_cards[j])) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		if (table.hasLsdy) {
			LsdyCards.Builder lsdyCardsBuilder = LsdyCards.newBuilder();
			int all_lsdy_cards[][] = new int[table.getTablePlayerNumber()][GameConstants.MAX_COUNT];
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				int newTmpCount = table._logic.switch_to_cards_data(table.lsdyCardsIndex[i], all_lsdy_cards[i]);
				lsdyCardsBuilder.addCardsCount(newTmpCount);

				IntegerArray.Builder cards = IntegerArray.newBuilder();
				if (i == seat_index) {
					for (int x = 0; x < tmpCount; x++) {
						cards.addCard(lsdy_cards[x]);
					}
				} else {
					for (int x = 0; x < newTmpCount; x++) {
						cards.addCard(all_lsdy_cards[i][x]);
					}
				}

				lsdyCardsBuilder.addCardsData(cards);
			}

			roomResponse.setCommResponse(PBUtil.toByteString(lsdyCardsBuilder));
		}

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
		if (ting_send_card) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		} else if (table._logic.is_magic_card(_send_card_data)) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}

		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index);
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards, table.ziMoCardsData[seat_index]);
		}

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}

	@Override
	public boolean handler_player_out_card(Table_WuZhi table, int seat_index, int card) {
		boolean is_from_bao_ting = card > GameConstants.CARD_ESPECIAL_TYPE_TING ? true : false;

		card = table.get_real_card(card);

		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("出牌,没到出牌");
			return false;
		}

		if (table.hasLsdy && is_from_bao_ting) {
			if (table._logic.remove_card_by_index(table.lsdyCardsIndex[_seat_index], card) == false) {
				table.log_error("出牌删除出错");
				return false;
			}
		} else {
			if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
				table.log_error("出牌删除出错");
				return false;
			}
		}

		table.exe_out_card(_seat_index, card, GameConstants.WIK_NULL);

		return true;
	}
}
