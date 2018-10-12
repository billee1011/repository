package com.cai.game.mj.hubei.ezhou;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_EZ;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.MJHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerOutCardOperate_EZ extends MJHandlerOutCardOperate<Table_EZ> {
	public int _out_card_player;
	public int _out_card_data;
	public int _type;

	@Override
	public void reset_status(int seat_index, int card, int type) {
		_out_card_player = seat_index;
		_out_card_data = card;
		_type = type;
	}

	@Override
	public void exe(Table_EZ table) {
		PlayerStatus playerStatus = table._playerStatus[_out_card_player];

		table.change_player_status(_out_card_player, GameConstants.INVALID_VALUE);
		playerStatus.clean_action();

		table._out_card_count++;
		table._out_card_player = _out_card_player;
		table._out_card_data = _out_card_data;

		int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
		// table._current_player = next_player;

		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data_ezhou(table.GRR._cards_index[_out_card_player], cards);

		for (int i = 0; i < hand_card_count; i++) {
			if (table._logic.is_magic_card(cards[i])) {
				cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			} else if (cards[i] == Constants_EZ.HZ_CARD) {
				cards[i] += GameConstants.CARD_ESPECIAL_TYPE_HZ;
			}
		}

		table.operate_player_cards(_out_card_player, hand_card_count, cards, 0, null);

		// TODO: 处理红中杠和癞子杠
		int real_card = _out_card_data;
		if (table._logic.is_magic_card(real_card)) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;

			table.exe_add_discard(_out_card_player, 1, new int[] { real_card }, false, GameConstants.DELAY_SEND_CARD_DELAY);

			table.operate_out_card(_out_card_player, 1, new int[] { real_card }, GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);

			table.exe_gang(_out_card_player, _out_card_player, _out_card_data, GameConstants.WIK_GANG, GameConstants.GANG_TYPE_LAI_ZI, true, false);

			return;
		} else if (real_card == Constants_EZ.HZ_CARD) {
			table.hong_zhong_gang_count++;

			real_card += GameConstants.CARD_ESPECIAL_TYPE_HZ;

			table.exe_add_discard(_out_card_player, 1, new int[] { real_card }, false, GameConstants.DELAY_SEND_CARD_DELAY);

			table.operate_out_card(_out_card_player, 1, new int[] { real_card }, GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);

			table.exe_gang(_out_card_player, _out_card_player, _out_card_data, GameConstants.WIK_GANG, GameConstants.GANG_TYPE_HONG_ZHONG, true,
					false);

			return;
		} else {
			table.operate_out_card(_out_card_player, 1, new int[] { real_card }, GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);
		}

		// 有包赔玩法，并且已经开始包赔的时候，做出牌判断
		if (table.start_compensation_judge && table.has_rule(Constants_EZ.GAME_RULE_KAO_ZHANG_KOU_FEN)) {
			boolean only_out_card_player_changed = true;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				int real_i = (_out_card_player + i) % table.getTablePlayerNumber();
				if (real_i == _out_card_player) {
					// 如果出牌人的番为0
					if (table.player_multiple_count[real_i] == 0) {
						only_out_card_player_changed = false;
						break;
					}
				} else {
					// 如果其他人的番为非0
					if (table.player_multiple_count[real_i] != 0) {
						only_out_card_player_changed = false;
						break;
					}
				}
			}

			if (only_out_card_player_changed) {
				table.gang_da_kao = false;
			} else {
				// 第1，判断听牌数据，出牌之前的吃碰和发牌的时候，就存储了可以听牌的牌值数据
				int ting_count = table._playerStatus[_out_card_player]._hu_out_card_count;
				if (ting_count > 0) {
					// 判断是否出了一张能听牌的牌
					boolean isOutTingCard = false;
					for (int x = 0; x < ting_count; x++) {
						int ting_card = table._playerStatus[_out_card_player]._hu_out_card_ting[x];
						if (_out_card_data == ting_card) {
							isOutTingCard = true;
							break;
						}
					}
					if (isOutTingCard) {
						table.gang_da_kao = false;
					} else {
						table.gang_da_kao = true;
					}
				} else {
					// 第2，如果有5对牌以上，判断是否是拆了对子打，注意3张一样的牌时候，比如3张9筒
					boolean more_than_5_pair = table.check_more_than_5_pair(table.GRR._cards_index[_out_card_player], _out_card_data,
							table.GRR._weave_count[_out_card_player]);
					if (more_than_5_pair) {
						// 超过5对牌的时候，未成对的牌，随便打
						// 如果手牌里的索引的牌是个单数，肯定拆了对子牌
						int tmp_card_index = table._logic.switch_to_card_index(_out_card_data);
						int tmp_card_count = table.GRR._cards_index[_out_card_player][tmp_card_index];
						if (tmp_card_count % 2 == 1) {
							table.gang_da_kao = true;
						} else {
							table.gang_da_kao = false;
						}
					} else {
						// 第3，判断吊张；第4，判断边张，注意大于1张的牌，不是边张；
						int magic_count = table._logic.magic_count(table.GRR._cards_index[_out_card_player]);
						if (magic_count > 0) {
							// 注意事项：当手里的牌，有癞子牌的时候，是不需要去判断吊张的了，因为癞子可以拆给任意牌。
							// 第4，如果有边张，但是没打边张
							int[] bian_zhang_cards = new int[GameConstants.MAX_COUNT];
							int bian_zhang_count = table.get_bian_zhang(table.GRR._cards_index[_out_card_player], _out_card_data, bian_zhang_cards);
							if (bian_zhang_count > 0) {
								boolean isOutBianZhang = false;
								for (int y = 0; y < bian_zhang_count; y++) {
									if (_out_card_data == bian_zhang_cards[y]) {
										isOutBianZhang = true;
										break;
									}
								}
								if (isOutBianZhang) {
									table.gang_da_kao = false;
								} else {
									table.gang_da_kao = true;
								}
							} else {
								table.gang_da_kao = false;
							}
						} else {
							int[] diao_zhang_cards = new int[GameConstants.MAX_COUNT];
							int diao_zhang_count = table.get_diao_zhang(table.GRR._cards_index[_out_card_player], _out_card_data, diao_zhang_cards);
							if (diao_zhang_count > 0) {
								// 第3，如果有吊张，但是没打掉张，注意特殊情况下的吊张的判断，（3万是癞子）手牌为：1万
								// 3万 7万 7万 9万 1条 3条 5条，这个时候，1万是吊张吗？
								boolean isOutDiaoZhang = false;
								for (int y = 0; y < diao_zhang_count; y++) {
									if (_out_card_data == diao_zhang_cards[y]) {
										isOutDiaoZhang = true;
										break;
									}
								}
								if (isOutDiaoZhang) {
									table.gang_da_kao = false;
								} else {
									table.gang_da_kao = true;
								}
							} else {
								// 第4，如果有边张，但是没打边张
								int[] bian_zhang_cards = new int[GameConstants.MAX_COUNT];
								int bian_zhang_count = table.get_bian_zhang(table.GRR._cards_index[_out_card_player], _out_card_data,
										bian_zhang_cards);
								if (bian_zhang_count > 0) {
									boolean isOutBianZhang = false;
									for (int y = 0; y < bian_zhang_count; y++) {
										if (_out_card_data == bian_zhang_cards[y]) {
											isOutBianZhang = true;
											break;
										}
									}
									if (isOutBianZhang) {
										table.gang_da_kao = false;
									} else {
										table.gang_da_kao = true;
									}
								} else {
									table.gang_da_kao = false;
								}
							}
						}
					}
				}
			}
		}

		table._playerStatus[_out_card_player]._hu_card_count = table.get_ting_card(table._playerStatus[_out_card_player]._hu_cards,
				table._playerStatus[_out_card_player]._hu_out_cards_fan[0], table.GRR._cards_index[_out_card_player],
				table.GRR._weave_items[_out_card_player], table.GRR._weave_count[_out_card_player], _out_card_player);
		int ting_cards[] = table._playerStatus[_out_card_player]._hu_cards;
		int ting_count = table._playerStatus[_out_card_player]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(_out_card_player, ting_count, ting_cards);

			// 出牌之后，如果有听牌数据，显示‘自动胡牌’按钮
			table.operate_auto_win_card(_out_card_player, true);
		} else {
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(_out_card_player, 1, ting_cards);
		}

		table._provide_player = _out_card_player;
		table._provide_card = _out_card_data;

		table.exe_add_discard(_out_card_player, 1, new int[] { _out_card_data }, false, GameConstants.DELAY_SEND_CARD_DELAY);

		boolean bAroseAction = false;
		if (!table._logic.is_magic_card(_out_card_data)) {
			bAroseAction = table.estimate_player_out_card_respond(_out_card_player, _out_card_data, _type);
		}

		if (bAroseAction == false) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i].clean_action();
				table.change_player_status(i, GameConstants.INVALID_VALUE);
			}

			table.operate_player_action(_out_card_player, true);

			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		} else {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];

				if (playerStatus.has_action()) {
					if (table.istrustee[i] && !table.is_match() && !table.isClubMatch() && !table.isCoinRoom()) {
						handler_be_set_trustee(table, i);
					} else {
						table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
						table.operate_player_action(i, false);
					}
				}
			}
		}
	}

	@Override
	public boolean handler_operate_card(Table_EZ table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return false;
		}

		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false) {
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}

		playerStatus.operate(operate_code, operate_card);

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);
		}

		if (table._playerStatus[seat_index].has_chi_hu() && operate_code != GameConstants.WIK_CHI_HU) {
			// 新的过圈胡规则
			table.score_when_abandoned_jie_pao[seat_index] = table.score_when_jie_pao_hu[seat_index];
			// table._playerStatus[seat_index].chi_hu_round_invalid();
		}

		int target_player = seat_index;
		int target_action = operate_code;
		int target_p = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_out_card_player + p) % table.getTablePlayerNumber();
			if (i == target_player) {
				target_p = table.getTablePlayerNumber() - p;
			}
		}
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_out_card_player + p) % table.getTablePlayerNumber();
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

		int target_card = _out_card_data;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		switch (target_action) {
		case GameConstants.WIK_LEFT: {
			int cbRemoveCard[] = new int[] { target_card + 1, target_card + 2 };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}

			boolean lai_zi_chi = false;
			// TODO: 癞子参与吃，相当于打出了一个癞子杠
			if (table._logic.is_magic_card(target_card + 1)) {
				table.GRR._lai_zi_pi_zi_gang[target_player][table.GRR._player_niao_count[target_player]++] = target_card + 1;
				lai_zi_chi = true;
			} else if (table._logic.is_magic_card(target_card + 2)) {
				table.GRR._lai_zi_pi_zi_gang[target_player][table.GRR._player_niao_count[target_player]++] = target_card + 2;
				lai_zi_chi = true;
			}

			table.remove_discard_after_operate(_out_card_player, _out_card_data);

			if (lai_zi_chi) {
				table.exe_chi_peng(target_player, _out_card_player, target_action, target_card, GameConstants.CHI_TYPE_LAI_ZI_CHI);
			} else {
				table.exe_chi_peng(target_player, _out_card_player, target_action, target_card, GameConstants.CHI_PENG_TYPE_OUT_CARD);
			}
			return true;
		}
		case GameConstants.WIK_RIGHT: {
			int cbRemoveCard[] = new int[] { target_card - 1, target_card - 2 };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}

			boolean lai_zi_chi = false;
			// TODO: 癞子参与吃，相当于打出了一个癞子杠
			if (table._logic.is_magic_card(target_card - 1)) {
				table.GRR._lai_zi_pi_zi_gang[target_player][table.GRR._player_niao_count[target_player]++] = target_card - 1;
				lai_zi_chi = true;
			} else if (table._logic.is_magic_card(target_card - 2)) {
				table.GRR._lai_zi_pi_zi_gang[target_player][table.GRR._player_niao_count[target_player]++] = target_card - 2;
				lai_zi_chi = true;
			}

			table.remove_discard_after_operate(_out_card_player, _out_card_data);

			if (lai_zi_chi) {
				table.exe_chi_peng(target_player, _out_card_player, target_action, target_card, GameConstants.CHI_TYPE_LAI_ZI_CHI);
			} else {
				table.exe_chi_peng(target_player, _out_card_player, target_action, target_card, GameConstants.CHI_PENG_TYPE_OUT_CARD);
			}
			return true;
		}
		case GameConstants.WIK_CENTER: {
			int cbRemoveCard[] = new int[] { target_card - 1, target_card + 1 };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}

			boolean lai_zi_chi = false;
			// TODO: 癞子参与吃，相当于打出了一个癞子杠
			if (table._logic.is_magic_card(target_card - 1)) {
				table.GRR._lai_zi_pi_zi_gang[target_player][table.GRR._player_niao_count[target_player]++] = target_card - 1;
				lai_zi_chi = true;
			} else if (table._logic.is_magic_card(target_card + 1)) {
				table.GRR._lai_zi_pi_zi_gang[target_player][table.GRR._player_niao_count[target_player]++] = target_card + 1;
				lai_zi_chi = true;
			}

			table.remove_discard_after_operate(_out_card_player, _out_card_data);

			if (lai_zi_chi) {
				table.exe_chi_peng(target_player, _out_card_player, target_action, target_card, GameConstants.CHI_TYPE_LAI_ZI_CHI);
			} else {
				table.exe_chi_peng(target_player, _out_card_player, target_action, target_card, GameConstants.CHI_PENG_TYPE_OUT_CARD);
			}
			return true;
		}
		case GameConstants.WIK_PENG: {
			int cbRemoveCard[] = new int[] { target_card, target_card };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "碰牌删除出错");
				return false;
			}
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			table.exe_chi_peng(target_player, _out_card_player, target_action, target_card, GameConstants.WIK_PENG);
			return true;
		}
		case GameConstants.WIK_GANG: {
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			table.exe_gang(target_player, _out_card_player, target_card, target_action, GameConstants.GANG_TYPE_JIE_GANG, false, false);
			return true;
		}
		case GameConstants.WIK_NULL: {
			_current_player = table._current_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();

			table.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);

			return true;
		}
		case GameConstants.WIK_CHI_HU: {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == target_player) {
					table.GRR._chi_hu_rights[i].set_valid(true);
				} else if (i == _out_card_player) {
					table.GRR._chi_hu_rights[i].set_valid(false);
				} else {
					table.GRR._chi_hu_rights[i].set_valid(false);
					table.GRR._chi_hu_rights[i].set_empty();
				}
			}

			table._cur_banker = target_player;

			table.GRR._chi_hu_card[target_player][0] = target_card;

			// 客户端播放放炮动画
			table.GRR._chi_hu_rights[_out_card_player].opr_or(Constants_EZ.CHR_FANG_PAO);
			table.operate_effect_action(_out_card_player, GameConstants.EFFECT_ACTION_TYPE_HU, 1, new long[] { Constants_EZ.CHR_FANG_PAO }, 1,
					GameConstants.INVALID_SEAT);

			table.process_chi_hu_player_operate(target_player, target_card, false);
			table.process_chi_hu_player_score(target_player, _out_card_player, _out_card_data, false);

			table._player_result.jie_pao_count[target_player]++;
			table._player_result.dian_pao_count[_out_card_player]++;

			// 3秒后再弹出小结算
			// int delay = GameConstants.GAME_FINISH_DELAY +
			// table.GRR._chi_hu_rights[target_player].type_count;
			int delay = GameConstants.GAME_FINISH_DELAY + 1;

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_EZ table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		// roomResponse.setTarget(seat_index);
		// roomResponse.setScoreType(table.get_player_fan_shu(seat_index));
		// table.send_response_to_other(seat_index, roomResponse);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_out_card_player);
		tableResponse.setCellScore(0);

		tableResponse.setActionCard(0);

		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				int real_card = table.GRR._discard_cards[i][j];
				if (j == 0 && i == table.GRR._banker_player && table.is_mj_type(GameConstants.GAME_TYPE_3D_E_ZHOU)) {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN;
				} else if (table._logic.is_magic_card(real_card)) {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				} else if (real_card == Constants_EZ.HZ_CARD) {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_HZ;
				}
				int_array.addItem(real_card);
			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);

				if (table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG && table.GRR._weave_items[i][j].public_card == 0) {
					// 暗杠的牌的显示
					if (seat_index == i) {
						weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
					} else {
						weaveItem_item.setCenterCard(0);
					}
				} else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		tableResponse.setSendCardData(0);

		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		table._logic.switch_to_cards_data_ezhou(table.GRR._cards_index[seat_index], hand_cards);

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			if (table._logic.is_magic_card(hand_cards[i])) {
				hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			} else if (hand_cards[i] == Constants_EZ.HZ_CARD) {
				hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_HZ;
			}
			tableResponse.addCardsData(hand_cards[i]);
		}

		// TODO 添加是否托管
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(table.istrustee[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		if (table.GRR._chi_hu_rights[seat_index].is_valid()) {
			table.process_chi_hu_player_operate_reconnect(seat_index, _out_card_data, false); // 效果
		} else {
			// 听牌显示
			int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
			int ting_count = table._playerStatus[seat_index]._hu_card_count;

			if (ting_count > 0) {
				table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);

				// 出牌之后，如果有听牌数据，显示‘自动胡牌’按钮
				table.operate_auto_win_card(seat_index, true);
			}

			if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
				table.operate_player_action(seat_index, false);
			}
		}

		return true;
	}

	@Override
	public boolean handler_be_set_trustee(Table_EZ table, int seat_index) {
		if (!table.istrustee[seat_index])
			return false;

		PlayerStatus curPlayerStatus = table._playerStatus[seat_index];

		if (curPlayerStatus.has_chi_hu() && _out_card_data != GameConstants.INVALID_VALUE) {
			// 有接炮就胡牌
			table.operate_player_action(seat_index, true);

			table.exe_jian_pao_hu(seat_index, GameConstants.WIK_CHI_HU, _out_card_data);

			return true;
		} else {
			// 别人出牌后，有吃、碰、接杠，等待5秒，如果5秒之内点了‘吃碰杠’操作，进行‘吃碰杠’动作并自动取消托管
			if (curPlayerStatus.has_action() && curPlayerStatus.is_respone() == false) {
				table.change_player_status(seat_index, GameConstants.Player_Status_OPR_CARD);
				table.operate_player_action(seat_index, false);

				// 添加定时任务，5秒之内点了操作，取消定时任务
				table._trustee_schedule[seat_index] = GameSchedule.put(new Runnable() {
					@Override
					public void run() {
						// 关闭操作按钮
						table.operate_player_action(seat_index, true);

						table.exe_jian_pao_hu(seat_index, GameConstants.WIK_NULL, _out_card_data);
					}
				}, table.action_wait_time, TimeUnit.MILLISECONDS);
			} else {
				// 没接炮就过牌
				table.exe_jian_pao_hu(seat_index, GameConstants.WIK_NULL, _out_card_data);
			}
			return true;
		}
	}
}
