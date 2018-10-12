package com.cai.game.hh.handler.czsrphz;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_CZ_SR;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.hh.handler.HHHandlerGang;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerGang_CZ_SR extends HHHandlerGang<Table_CZ_SR> {

	@Override
	public void exe(Table_CZ_SR table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action()) {
				table.operate_player_action(i, true);
			}

			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();

		if (_depatch == false)
			table.operate_out_card(this._provide_player, 0, null, GameConstants.OUT_CARD_TYPE_MID,
					GameConstants.INVALID_SEAT);
		else
			table.operate_player_get_card(this._provide_player, 0, null, GameConstants.INVALID_SEAT, false);

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 5,
				GameConstants.INVALID_SEAT);

		this.exe_gang(table);
	}

	@SuppressWarnings("unused")
	@Override
	protected boolean exe_gang(Table_CZ_SR table) {
		int cbCardIndex = table._logic.switch_to_card_index(_center_card);
		int cbWeaveIndex = -1;

		if (GameConstants.PAO_TYPE_AN_LONG == _type || GameConstants.PAO_TYPE_TI_MINE_LONG == _type
				|| GameConstants.PAO_TYPE_OHTER_PAO == _type) {
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;
			table._long_count[_seat_index]++;
		} else if (GameConstants.PAO_TYPE_MINE_SAO_LONG == _type || GameConstants.PAO_TYPE_OTHER_SAO_PAO == _type) {
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;

				if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants.WIK_WEI)) {
					cbWeaveIndex = i;
					table._long_count[_seat_index]++;
					break;
				}
			}

			if (cbWeaveIndex == -1) {
				table.log_player_error(_seat_index, "杠牌出错");
				return false;
			}
		} else if (GameConstants.PAO_TYPE_MINE_PENG_PAO  == _type) {
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;

				if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants.WIK_PENG)) {
					cbWeaveIndex = i;
					table._long_count[_seat_index]++;
					break;
				}
			}

			if (cbWeaveIndex == -1) {
				table.log_player_error(_seat_index, "杠牌出错");
				return false;
			}
		} else if (GameConstants.SAO_TYPE_MINE_SAO == _type) {
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;
			
//			if (table.has_rule(Constants_ChenZhou.GAME_RULE_SHE_PAO)) {
//				table.cards_has_wei[table._logic.switch_to_card_index(_center_card)]++; // 记录玩家偎牌的牌索引数据
//			}
//			table.cards_has_wei[table._logic.switch_to_card_index(_center_card)]++; // 记录玩家偎牌的牌索引数据
		}

		table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = _p == true ? 1 : 0;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _center_card;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].hu_xi = table._logic
				.get_weave_hu_xi(table.GRR._weave_items[_seat_index][cbWeaveIndex]);

		table._current_player = _seat_index;

		table.GRR._cards_index[_seat_index][cbCardIndex] = 0;
		table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);

		int cards[] = new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

		int hu_xi_count = table._logic.get_weave_hu_xi(table.GRR._weave_items[_seat_index][cbWeaveIndex]);
		
		// 先注释掉，等客户端一起联调
//		for (int x = 0; x < hand_card_count; x++) {
//			if (table.is_card_has_wei(cards[x])) { // 如果是偎的牌
//				// 判断打出这张牌是否能听牌
//				table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(cards[x])]--;
//				boolean b_is_ting_state = table.is_ting_state(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
//						table.GRR._weave_count[_seat_index], _seat_index);
//				table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(cards[x])]++;
//				
//				if (b_is_ting_state)
//					cards[x] += GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT;
//				else
//					cards[x] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_SHOOT;
//			}
//		}

		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
				table.GRR._weave_items[_seat_index]);

		// 这里也没必要吧？处理天胡的时候，每个玩家手里的提龙就已经处理好了 -- Begin
		int an_long_Index[] = new int[5];
		int an_long_count = 0;

		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (table.GRR._cards_index[_seat_index][i] == 4) {
				an_long_Index[an_long_count++] = i;
			}
		}

		if (an_long_count > 0) {
			int _action = GameConstants.WIK_AN_LONG;
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action },
					1, GameConstants.INVALID_SEAT);

			for (int i = 0; i < an_long_count; i++) {
				cbWeaveIndex = table.GRR._weave_count[_seat_index];
				table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = 1;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = table._logic
						.switch_to_card_data(an_long_Index[i]);
				table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_AN_LONG;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _seat_index;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].hu_xi = table._logic
						.get_weave_hu_xi(table.GRR._weave_items[_seat_index][cbWeaveIndex]);
				table.GRR._weave_count[_seat_index]++;
				table._long_count[_seat_index]++;

				table.GRR._cards_index[_seat_index][an_long_Index[i]] = 0;

				table.GRR._card_count[_seat_index] = table._logic
						.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
			}

			cards = new int[GameConstants.MAX_HH_COUNT];
			hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			
			// 先注释掉，等客户端一起联调
//			for (int x = 0; x < hand_card_count; x++) {
//				if (table.is_card_has_wei(cards[x])) { // 如果是偎的牌
//					// 判断打出这张牌是否能听牌
//					table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(cards[x])]--;
//					boolean b_is_ting_state = table.is_ting_state(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
//							table.GRR._weave_count[_seat_index], _seat_index);
//					table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(cards[x])]++;
//					
//					if (b_is_ting_state)
//						cards[x] += GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT;
//					else
//						cards[x] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_SHOOT;
//				}
//			}
			
			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
					table.GRR._weave_items[_seat_index]);
		}
		// 这里也没必要吧？处理天胡的时候，每个玩家手里的提龙就已经处理好了 -- End

		int pai_count = 0;
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (table.GRR._cards_index[_seat_index][i] < 3)
				pai_count += table.GRR._cards_index[_seat_index][i];
		}

		int action_hu = GameConstants.WIK_NULL;
		
		// 如果是起手提2龙之后，再来提偎跑的时候，直接给下家发牌
		if (table._ti_two_long[_seat_index] == true) {
			table._ti_two_long[_seat_index] = false;
			
			table._playerStatus[_seat_index]._hu_card_count = table.get_hh_ting_card_twenty(
					table._playerStatus[_seat_index]._hu_cards, table.GRR._cards_index[_seat_index],
					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index,
					_seat_index);

			int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
			int ting_count = table._playerStatus[_seat_index]._hu_card_count;

			if (ting_count > 0) {
				table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
			} else {
				ting_cards[0] = 0;
				table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
			}

			int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			table._current_player = next_player;
			table._last_player = next_player;
			table._last_card = 0;

			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);
			
			return true;
		}

		if ((table._long_count[_seat_index] <= 1 || GameConstants.SAO_TYPE_MINE_SAO == _type)
				&& (table._is_xiang_gong[_seat_index] == false)) { // 一个提跑并且不是相公，判断是否能胡牌
			ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
			chr.set_empty();

			int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
			
			if (_seat_index != _provide_player) {
				if (_depatch == true)
					card_type = GameConstants.HU_CARD_TYPE_FAN_PAI;
				else
					card_type = GameConstants.HU_CARD_TYPE_PAOHU;
			}

			int hu_xi[] = new int[1];

			action_hu = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index],
					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index,
					_provide_player, 0, chr, card_type, hu_xi, true);

			if (action_hu != GameConstants.WIK_NULL) {
				PlayerStatus tempPlayerStatus = table._playerStatus[_seat_index];

				if (_seat_index != _provide_player) {
					tempPlayerStatus.add_action(GameConstants.WIK_CHI_HU);
					tempPlayerStatus.add_chi_hu(_center_card, _provide_player);
				} else {
					tempPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
					tempPlayerStatus.add_zi_mo(_center_card, _provide_player);
				}

				tempPlayerStatus.add_action(GameConstants.WIK_NULL);
				tempPlayerStatus.add_pass(0, _seat_index);

				if (table.has_rule(Constants_CZ_SR.GAME_RULE_QIANG_ZHI_HU)) {
				    if (_seat_index != _provide_player) {
                        GameSchedule.put(new Runnable() {
                            @Override
                            public void run() {
                                table.handler_operate_card(_seat_index, GameConstants.WIK_CHI_HU, _center_card, -1);
                            }
                        }, 1500, TimeUnit.MILLISECONDS);
                    } else {
                        GameSchedule.put(new Runnable() {
                            @Override
                            public void run() {
                                table.handler_operate_card(_seat_index, GameConstants.WIK_ZI_MO, _center_card, -1);
                            }
                        }, 1500, TimeUnit.MILLISECONDS);
                    }
					return true;
				}

				if (tempPlayerStatus.has_action()) {
					tempPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(_seat_index, false);
					return true;
				}
			} else {
				chr.set_empty();
			}

			if (pai_count == 0) { // 如果是第1个提跑或者是偎牌，如果没牌可以出了，判断能不能胡，不能胡，设置成相公
				int all_hu_xi = 0;
				for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
					all_hu_xi += table.GRR._weave_items[_seat_index][i].hu_xi;
				}

				boolean b_hu_xi = false;
				if (all_hu_xi >= table.get_basic_hu_xi()) {
					b_hu_xi = true;
				}

				if (b_hu_xi == true) { // 如果能胡
					int hong_pai_count = 0;
					int hei_pai_count = 0;
					int all_cards_count = 0;

					for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
						table._hu_weave_items[_seat_index][i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
						table._hu_weave_items[_seat_index][i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
						table._hu_weave_items[_seat_index][i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
						table._hu_weave_items[_seat_index][i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player;
						table._hu_weave_items[_seat_index][i].hu_xi = table.GRR._weave_items[_seat_index][i].hu_xi;
						hong_pai_count += table._logic.calculate_weave_hong_pai(table._hu_weave_items[_seat_index][i]);
						hei_pai_count += table._logic.calculate_weave_hei_pai(table._hu_weave_items[_seat_index][i]);
					}

					table._hu_weave_count[_seat_index] = table.GRR._weave_count[_seat_index];

					all_cards_count = hong_pai_count + hei_pai_count;

					if (_seat_index != _provide_player) {
						chr.opr_or(GameConstants.CHR_JEI_PAO_HU);
					} else {
						chr.opr_or(GameConstants.CHR_ZI_MO);
					}

					if (hong_pai_count == 0) {
						chr.opr_or(GameConstants.CHR_ALL_HEI);
					} else if (hong_pai_count == 1) {
						chr.opr_or(GameConstants.CHR_ONE_HONG);
					} else if (hong_pai_count >= 10) {
						chr.opr_or(GameConstants.CHR_TEN_HONG_PAI);
					}

					PlayerStatus tempPlayerStatus = table._playerStatus[_seat_index];

					if (_seat_index != _provide_player) {
						tempPlayerStatus.add_action(GameConstants.WIK_CHI_HU);
						tempPlayerStatus.add_chi_hu(_center_card, _provide_player);
					} else {
						tempPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
						tempPlayerStatus.add_zi_mo(_center_card, _provide_player);
					}

					tempPlayerStatus.add_action(GameConstants.WIK_NULL);
					tempPlayerStatus.add_pass(0, _seat_index);

					if (table.has_rule(Constants_CZ_SR.GAME_RULE_QIANG_ZHI_HU)) { // 如果有强制胡牌
					    if (_seat_index != _provide_player) {
	                        GameSchedule.put(new Runnable() {
	                            @Override
	                            public void run() {
	                                table.handler_operate_card(_seat_index, GameConstants.WIK_CHI_HU, _center_card, -1);
	                            }
	                        }, 1500, TimeUnit.MILLISECONDS);
	                    } else {
	                        GameSchedule.put(new Runnable() {
	                            @Override
	                            public void run() {
	                                table.handler_operate_card(_seat_index, GameConstants.WIK_ZI_MO, _center_card, -1);
	                            }
	                        }, 1500, TimeUnit.MILLISECONDS);
	                    }
						return true;
					}

					if (tempPlayerStatus.has_action()) {
						tempPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);
						table.operate_player_action(_seat_index, false);
						return true; // 这里如果有胡牌的直接返回了？？？
					}
				} else { // 不能胡牌没牌可以出了不用设置成相公？？
					chr.set_empty();
				}
			}
		}

		// 思路不一样，先判断胡，再来判断的是否相公
		if ((table._is_xiang_gong[_seat_index] == false)
				&& (table._long_count[_seat_index] <= 1 || GameConstants.SAO_TYPE_MINE_SAO == _type)) { // 如果不是相公，并且提跑只有一个或者是偎牌操作，不是有点重复了吗？？？
			if (pai_count == 0) {
				table._is_xiang_gong[_seat_index] = true;
				table.operate_player_xiang_gong_flag(_seat_index, table._is_xiang_gong[_seat_index]);

				int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
				table._current_player = next_player;
				table._last_player = next_player;

				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);
			} else {
				if (table._long_count[_seat_index] <= 1 || GameConstants.SAO_TYPE_MINE_SAO == _type) { // 第一个提偎跑之后有牌可以出，else分支不会走吧？
					table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
					table.operate_player_status();
				} else { // 有牌可以出，但是是第2个以上的提跑，还是有点重复了吧？？？？
					table._playerStatus[_seat_index]._hu_card_count = table.get_hh_ting_card_twenty(
							table._playerStatus[_seat_index]._hu_cards, table.GRR._cards_index[_seat_index],
							table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index,
							_seat_index);

					int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
					int ting_count = table._playerStatus[_seat_index]._hu_card_count;

					if (ting_count > 0) {
						table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
					} else {
						ting_cards[0] = 0;
						table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
					}

					int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
					table._current_player = next_player;
					table._last_player = next_player;
					table._last_card = 0;

					table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);
				}
			}
		} else { // 如果是相公，或者提跑大于等于2个，给下家发牌
			table._playerStatus[_seat_index]._hu_card_count = table.get_hh_ting_card_twenty(
					table._playerStatus[_seat_index]._hu_cards, table.GRR._cards_index[_seat_index],
					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index, _seat_index);

			int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
			int ting_count = table._playerStatus[_seat_index]._hu_card_count;

			if (ting_count > 0) {
				table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
			} else {
				ting_cards[0] = 0;
				table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
			}

			int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			table._current_player = next_player;
			table._last_player = next_player;
			table._last_card = 0;

			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);
		}

		return true;
	}

	@SuppressWarnings("unused")
	@Override
	public boolean handler_operate_card(Table_CZ_SR table, int seat_index, int operate_code, int operate_card,
			int luoCode) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_info("没有这个操作:" + operate_code);
			return false;
		}
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return true;
		}
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { GameConstants.WIK_NULL }, 1);
		}

		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();

		// 变量定义 优先级最高操作的玩家和操作--不通炮的算法
		int target_player = seat_index;
		int target_action = operate_code;
		int target_lou_code = luoCode;
		int target_p = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			if (i == target_player) {
				target_p = table.getTablePlayerNumber() - p;
			}
		}
		int cbActionRank[] = new int[table.getTablePlayerNumber()];
		int cbMaxActionRand = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			// 获取动作
			int cbUserActionRank = 0;
			// 优先级别
			int cbTargetActionRank = 0;
			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					// 获取已经执行的动作的优先级
					cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform())
							+ table.getTablePlayerNumber() - p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
							table._playerStatus[i]._action) + table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					// 获取已经执行的动作的优先级
					cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform())
							+ target_p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbTargetActionRank = table._logic.get_action_list_rank(
							table._playerStatus[target_player]._action_count,
							table._playerStatus[target_player]._action) + target_p;
				}

				// 优先级别
				// 动作判断 优先级最高的人和动作
				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;// 最高级别人
					target_action = table._playerStatus[i].get_perform();
					target_lou_code = table._playerStatus[i].get_lou_kind();
					target_p = table.getTablePlayerNumber() - p;
					cbMaxActionRand = cbUserActionRank;
				}
			}
		}

		// 优先级最高的人还没操作
		if (table._playerStatus[target_player].is_respone() == false) {
			table.log_info("优先级最高的人还没操作");
			return true;
		}

		// 变量定义
		int target_card = table._playerStatus[target_player]._operate_card;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
			table.operate_player_action(i, true);
		}

		switch (target_action) {
		case GameConstants.WIK_NULL: { // 如果偎提跑之后，有胡不胡
			if ((table._is_xiang_gong[_seat_index] == false)
					&& (table._long_count[_seat_index] == 1 || GameConstants.SAO_TYPE_MINE_SAO == _type)) {
				int pai_count = 0;

				for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
					if (table.GRR._cards_index[_seat_index][i] < 3)
						pai_count += table.GRR._cards_index[_seat_index][i];
				}

				if (pai_count == 0) {
					table._is_xiang_gong[_seat_index] = true;
					table.operate_player_xiang_gong_flag(_seat_index, table._is_xiang_gong[_seat_index]);

					int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
					table._playerStatus[_seat_index].clean_action();
					table._playerStatus[_seat_index].clean_status();
					table._current_player = next_player;
					table._last_player = next_player;

					table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);
				} else {
					table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
					table.operate_player_status();
				}
			} else { // 如果不胡牌，并且是重提重跑或者是相公，给下家发牌
				table._playerStatus[_seat_index]._hu_card_count = table.get_hh_ting_card_twenty(
						table._playerStatus[_seat_index]._hu_cards, table.GRR._cards_index[_seat_index],
						table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index,
						_seat_index);

				int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
				int ting_count = table._playerStatus[_seat_index]._hu_card_count;

				if (ting_count > 0) {
					table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
				} else {
					ting_cards[0] = 0;
					table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
				}

				table._playerStatus[_seat_index].clean_action();
				table._playerStatus[_seat_index].clean_status();
				int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
				table._current_player = next_player;
				table._last_player = next_player;

				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1500);
			}

			return true;
		}
		case GameConstants.WIK_CHI_HU:
		case GameConstants.WIK_ZI_MO: { // 偎提跑之后胡牌
			table.GRR._chi_hu_rights[_seat_index].set_valid(true);

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table._cur_banker = target_player;

//			if (_depatch == true)
//				table.operate_player_get_card(this._provide_player, 1, new int[] { _center_card },
//						GameConstants.INVALID_SEAT, false);

			table._shang_zhuang_player = _seat_index;

			table.process_chi_hu_player_operate(target_player, operate_card, true);

			if (target_action == GameConstants.WIK_ZI_MO) {
				table.process_chi_hu_player_score_phz(target_player, _seat_index, operate_card, true);
				table._player_result.zi_mo_count[target_player]++;
			} else if (target_action == GameConstants.WIK_CHI_HU) { // 所有的非点炮的胡牌分计算都按自摸算，不然还是得重写一下这个方法
				if (_depatch == true)
					table.process_chi_hu_player_score_phz(target_player, _seat_index, operate_card, true);
				else
					table.process_chi_hu_player_score_phz(target_player, _provide_player, operate_card, false);
			}

			table.countChiHuTimes(target_player, true);

			int delay = GameConstants.GAME_FINISH_DELAY_FLS;
			if (table.GRR._chi_hu_rights[target_player].type_count > 2) {
				delay += table.GRR._chi_hu_rights[target_player].type_count - 2;
			}
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), target_player, GameConstants.Game_End_NORMAL),
					delay, TimeUnit.SECONDS);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_CZ_SR table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		// 游戏变量
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_seat_index);
		tableResponse.setCellScore(0);

		// 状态变量
		tableResponse.setActionCard(0);
		// tableResponse.setActionMask((_response[seat_index] == false) ?
		// _player_action[seat_index] : MJGameConstants.WIK_NULL);
		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);// 是否托管
			// 剩余牌数
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
					// 癞子
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_HUN);
				} else {
					int_array.addItem(table.GRR._discard_cards[i][j]);
				}
			}
			tableResponse.addDiscardCards(int_array);
			// 组合扑克
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE_HH; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				if (seat_index != i) {
					if ((table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_TI_LONG
							|| table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_AN_LONG
							|| table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_AN_LONG_LIANG)
							&& table.GRR._weave_items[i][j].public_card == 0) {
						weaveItem_item.setCenterCard(0);
					} else {
						if (table.is_mj_type(GameConstants.GAME_TYPE_PHZ_XT)
								&& table.has_rule(GameConstants.GAME_RULE_DI_AN_WEI)
								&& table._xt_display_an_long[i] == true)
							weaveItem_item.setCenterCard(0);
						else
							weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
					}
				} else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_item.setHuXi(table.GRR._weave_items[i][j].hu_xi);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			//
			tableResponse.addWinnerOrder(0);
			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));

		}

		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		// 先注释掉，等客户端一起联调
//		for (int x = 0; x < hand_card_count; x++) {
//			if (table.is_card_has_wei(hand_cards[x])) { // 如果是偎的牌
//				// 判断打出这张牌是否能听牌
//				table.GRR._cards_index[seat_index][table._logic.switch_to_card_index(hand_cards[x])]--;
//				boolean b_is_ting_state = table.is_ting_state(table.GRR._cards_index[seat_index],
//						table.GRR._weave_items[seat_index], table.GRR._weave_count[seat_index], seat_index);
//				table.GRR._cards_index[seat_index][table._logic.switch_to_card_index(hand_cards[x])]++;
//
//				if (b_is_ting_state)
//					hand_cards[x] += GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT;
//				else
//					hand_cards[x] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_SHOOT;
//			}
//		}
		
		for (int i = 0; i < GameConstants.MAX_HH_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
				seat_index);

		if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
			table.operate_player_action(seat_index, false);
		}

		if (table._is_xiang_gong[seat_index] == true)
			table.operate_player_xiang_gong_flag(seat_index, table._is_xiang_gong[seat_index]);
		

		table.istrustee[seat_index] = false;

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		
		return true;
	}
}
