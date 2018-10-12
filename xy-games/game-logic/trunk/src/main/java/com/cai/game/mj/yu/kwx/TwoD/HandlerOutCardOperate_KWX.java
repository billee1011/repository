package com.cai.game.mj.yu.kwx.TwoD;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_KWX;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.MJHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerOutCardOperate_KWX extends MJHandlerOutCardOperate<Table_KWX_2D> {

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
	public void exe(Table_KWX_2D table) {

		PlayerStatus playerStatus = table._playerStatus[_out_card_player];

		table.change_player_status(_out_card_player, GameConstants_KWX.INVALID_VALUE);
		playerStatus.clean_action();

		table._out_card_count++;
		table._out_card_player = _out_card_player;
		table._out_card_data = _out_card_data;

		int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();

		if (table.player_liang[_out_card_player] != 1) {
			int cards[] = new int[GameConstants_KWX.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_out_card_player], cards);
			table.operate_player_cards(_out_card_player, hand_card_count, cards, 0, null);
		} else {
			int cards[] = new int[GameConstants_KWX.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_out_card_player], cards);
			table.operate_show_card(_out_card_player, GameConstants.Show_Card_LY_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);
		}

		table.operate_out_card(_out_card_player, 1, new int[] { _out_card_data }, GameConstants_KWX.OUT_CARD_TYPE_MID,
				GameConstants_KWX.INVALID_SEAT);

		table._playerStatus[_out_card_player]._hu_card_count = table.get_ting_card(table._playerStatus[_out_card_player]._hu_cards,
				table.GRR._cards_index[_out_card_player], table.GRR._weave_items[_out_card_player], table.GRR._weave_count[_out_card_player],
				_out_card_player);
		int ting_cards[] = table._playerStatus[_out_card_player]._hu_cards;
		int ting_count = table._playerStatus[_out_card_player]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(_out_card_player, ting_count, ting_cards);
		} else {
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(_out_card_player, 1, ting_cards);
		}

		table._provide_player = _out_card_player;
		table._provide_card = _out_card_data;

		table.exe_add_discard(_out_card_player, 1, new int[] { _out_card_data }, false, GameConstants_KWX.DELAY_SEND_CARD_DELAY);
		boolean bAroseAction = table.estimate_player_out_card_respond(_out_card_player, _out_card_data, _type);

		if (bAroseAction == false) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i].clean_action();
				table.change_player_status(i, GameConstants_KWX.INVALID_VALUE);
			}

			table.operate_player_action(_out_card_player, true);
			table.exe_dispatch_card(next_player, GameConstants_KWX.WIK_NULL, GameConstants_KWX.DELAY_SEND_CARD_DELAY);
		} else {
			boolean flag = false;
			int hu_count = 0;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];
				if (!playerStatus.has_action()) {
					continue;
				}

				if (table.player_liang[i] == 1 && table._playerStatus[i].has_action_by_code(GameConstants_KWX.WIK_CHI_HU)) {
					ting_cards = table._playerStatus[i]._hu_cards;
					ting_cards[0] = 0;
					table.operate_chi_hu_cards(i, 1, ting_cards);

					table.GRR._win_order[i] = 1; // 用来计算和处理吃三比的消散
					table._playerStatus[i].operate(GameConstants_KWX.WIK_CHI_HU, _out_card_data);
					table.GRR._chi_hu_rights[i].set_valid(true);
					table.process_chi_hu_player_operate(i, _out_card_data, false);

					flag = true;
					hu_count++;
				} else {
					if (table._playerStatus[i].has_action_by_code(GameConstants_KWX.WIK_CHI_HU)) {
						hu_count++;
					}
					table.operate_player_action(i, false);
					if (table.has_rule(GameConstants_KWX.GAME_RULE_AUTO_10)) {
						final int seat = i;
						if (table.future != null) {
							table.future.cancel(true);
							table.future = null;
						}
						table.future = GameSchedule.put(new Runnable() {
							@Override
							public void run() {
								table.istrustee[seat] = true;
								handler_check_auto_behaviour_not_gold(table, seat, _out_card_data);
							}
						}, GameConstants.DELAY_JIAN_PAO_HU_NEW * 10, TimeUnit.MILLISECONDS);
					}
				}
			}

			if (flag) {

				int jie_pao_count = 0;
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {
						continue;
					}
					jie_pao_count++;
				}

				if (hu_count == jie_pao_count) {
					table.remove_discard_after_operate(_out_card_player, _out_card_data);
					if (jie_pao_count > 0) {
						// table._cur_banker = (table._cur_banker + 1 +
						// table.getTablePlayerNumber()) %
						// table.getTablePlayerNumber();
						for (int i = 0; i < table.getTablePlayerNumber(); i++) {
							if (i == table._cur_banker) {
								table._player_result.qiang[table._cur_banker] = table.continue_banker_count;
							} else {
								table._player_result.qiang[i] = 0;
							}
						}

						for (int i = 0; i < GameConstants_KWX.GAME_PLAYER; i++) {
							if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {
								continue;
							}

							if (jie_pao_count > 1) {
								table._cur_banker = _out_card_player;
							} else {
								table._cur_banker = i;
							}
							if ((_type == GameConstants_KWX.GANG_TYPE_AN_GANG || _type == GameConstants_KWX.GANG_TYPE_ADD_GANG
									|| _type == GameConstants_KWX.GANG_TYPE_JIE_GANG) && table.player_continue_gang[_out_card_player] > 1) {
								if (table.player_continue_gang[_out_card_player] == 2) {
									table.GRR._chi_hu_rights[i].opr_or(GameConstants_KWX.CHR_PAO_GANG_GANG);
								}
								if (table.player_continue_gang[_out_card_player] == 3) {
									table.GRR._chi_hu_rights[i].opr_or(GameConstants_KWX.CHR_PAO_GANG_GANG_GANG);
								}
								if (table.player_continue_gang[_out_card_player] == 4) {
									table.GRR._chi_hu_rights[i].opr_or(GameConstants_KWX.CHR_PAO_GANG_GANG_GANG_GANG);
								}
							}

							table.GRR._chi_hu_card[i][0] = _out_card_data;
							table.process_chi_hu_player_score(i, _out_card_player, _out_card_data, false);

							// 记录
							table._player_result.jie_pao_count[i]++;
							table._player_result.dian_pao_count[_out_card_player]++;
							if (table.GRR._chi_hu_rights[i].da_hu_count > 0) {
								table._player_result.da_hu_jie_pao[i]++;
								table._player_result.da_hu_dian_pao[_out_card_player]++;
							} else {
								table._player_result.xiao_hu_jie_pao[i]++;
								table._player_result.xiao_hu_dian_pao[_out_card_player]++;
							}
						}

						GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants_KWX.Game_End_NORMAL), 0,
								TimeUnit.SECONDS);
					}
				}
			}
		}
	}

	@Override
	public boolean handler_operate_card(Table_KWX_2D table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return false;
		}

		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return false;
		}

		if ((operate_code != GameConstants_KWX.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false) {
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}

		if (table.future != null) {
			table.future.cancel(true);
			table.future = null;
		}
		playerStatus.operate(operate_code, operate_card);

		if (operate_code == GameConstants_KWX.WIK_CHI_HU) {
			int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(seat_index, 1, ting_cards);

			table.GRR._win_order[seat_index] = 1; // 用来计算和处理吃三比的消散

			table.GRR._chi_hu_rights[seat_index].set_valid(true);
			table.process_chi_hu_player_operate(seat_index, operate_card, false);
		} else if (operate_code == GameConstants_KWX.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants_KWX.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants_KWX.WIK_NULL }, 1);

			if (table._playerStatus[seat_index].has_action_by_code(GameConstants_KWX.WIK_PENG)) {
				table._playerStatus[seat_index].add_cards_abandoned_peng(_out_card_data);
			}
			if (table._playerStatus[seat_index].has_action_by_code(GameConstants_KWX.WIK_CHI_HU)) {
				table._playerStatus[seat_index].add_cards_abandoned_hu(_out_card_data);
				table.pass_hu_fan[seat_index] = table.pass_hu_fan[seat_index] > table.get_fan(seat_index, _out_card_player)
						? table.pass_hu_fan[seat_index]
						: table.get_fan(seat_index, _out_card_player);
				// table._playerStatus[seat_index].chi_hu_round_invalid();
			}
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
				return false;
		}

		int target_player = seat_index;
		int target_action = operate_code;

		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_out_card_player + p) % table.getTablePlayerNumber();
			int cbUserActionRank = 0;

			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform());
				} else {
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count, table._playerStatus[i]._action);
				}

				int cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform());

				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;
					target_action = table._playerStatus[i].get_perform();
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
		case GameConstants_KWX.WIK_LEFT: {
			int cbRemoveCard[] = new int[] { target_card + 1, target_card + 2 };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			table._card_can_not_out_after_chi[target_player] = target_card;
			table._chi_pai_count[target_player][_out_card_player]++; // 吃三比，不用在意提供者是谁
			table.exe_chi_peng(target_player, _out_card_player, target_action, target_card, GameConstants_KWX.CHI_PENG_TYPE_OUT_CARD);
			return true;
		}
		case GameConstants_KWX.WIK_RIGHT: {
			int cbRemoveCard[] = new int[] { target_card - 1, target_card - 2 };

			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			table._card_can_not_out_after_chi[target_player] = target_card;
			table._chi_pai_count[target_player][_out_card_player]++; // 吃三比，不用在意提供者是谁
			table.exe_chi_peng(target_player, _out_card_player, target_action, target_card, GameConstants_KWX.CHI_PENG_TYPE_OUT_CARD);
			return true;
		}
		case GameConstants_KWX.WIK_CENTER: {
			int cbRemoveCard[] = new int[] { target_card - 1, target_card + 1 };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			table._card_can_not_out_after_chi[target_player] = target_card;
			table._chi_pai_count[target_player][_out_card_player]++; // 吃三比，不用在意提供者是谁
			table.exe_chi_peng(target_player, _out_card_player, target_action, target_card, GameConstants_KWX.CHI_PENG_TYPE_OUT_CARD);
			return true;
		}
		case GameConstants_KWX.WIK_PENG: {
			int cbRemoveCard[] = new int[] { target_card, target_card };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "碰牌删除出错");
				return false;
			}

			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			table._chi_pai_count[target_player][_out_card_player]++; // 吃三比，不用在意提供者是谁
			table.exe_chi_peng(target_player, _out_card_player, target_action, target_card, GameConstants_KWX.CHI_PENG_TYPE_OUT_CARD);
			return true;
		}
		case GameConstants_KWX.WIK_GANG: {
			// 杠的吃三比放到杠牌handler里
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			// boolean flag = false; // 第一张牌 被人杠了就要多给每人一分
			// if (_type == GameConstants_KWX.OutCard_Type_Di_Hu)
			// flag = true;
			if (_type == GameConstants_KWX.GANG_TYPE_AN_GANG || _type == GameConstants_KWX.GANG_TYPE_ADD_GANG
					|| _type == GameConstants_KWX.GANG_TYPE_JIE_GANG) {
				table.player_continue_gang[target_player]++;
			}
			table.exe_gang(target_player, _out_card_player, target_card, target_action, GameConstants_KWX.GANG_TYPE_JIE_GANG, false, true);
			return true;
		}
		case GameConstants_KWX.WIK_NULL: {
			// table.exe_add_discard(this._out_card_player, 1, new int[] {
			// this._out_card_data }, false, 0);

			_current_player = table._current_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();

			table.exe_dispatch_card(_current_player, GameConstants_KWX.WIK_NULL, 0);

			return true;
		}
		case GameConstants_KWX.WIK_CHI_HU: {
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			GameSchedule.put(new Runnable() {
				@Override
				public void run() {
					table.operate_remove_discard(_out_card_player, table.GRR._discard_count[_out_card_player]);
				}
			}, 2, TimeUnit.SECONDS);
			// 吃胡等待 因为胡牌的等级是一样的，可以一炮多响，看看是不是还有能胡的
			for (int i = 0; i < GameConstants_KWX.GAME_PLAYER; i++) {
				if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
					return false;
			}

			int jie_pao_count = 0;
			for (int i = 0; i < GameConstants_KWX.GAME_PLAYER; i++) {
				if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {
					continue;
				}
				jie_pao_count++;
			}

			if (jie_pao_count > 0) {

				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (i == table._cur_banker) {
						table._player_result.qiang[table._cur_banker] = table.continue_banker_count;
					} else {
						table._player_result.qiang[i] = 0;
					}
				}

				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {
						continue;
					}
					if (jie_pao_count > 1) {
						table._cur_banker = _out_card_player;
					} else {
						table._cur_banker = i;
					}
					if ((_type == GameConstants_KWX.GANG_TYPE_AN_GANG || _type == GameConstants_KWX.GANG_TYPE_ADD_GANG
							|| _type == GameConstants_KWX.GANG_TYPE_JIE_GANG) && table.player_continue_gang[_out_card_player] > 1) {
						if (table.player_continue_gang[_out_card_player] == 2) {
							table.GRR._chi_hu_rights[i].opr_or(GameConstants_KWX.CHR_PAO_GANG_GANG);
						}
						if (table.player_continue_gang[_out_card_player] == 3) {
							table.GRR._chi_hu_rights[i].opr_or(GameConstants_KWX.CHR_PAO_GANG_GANG_GANG);
						}
						if (table.player_continue_gang[_out_card_player] == 4) {
							table.GRR._chi_hu_rights[i].opr_or(GameConstants_KWX.CHR_PAO_GANG_GANG_GANG_GANG);
						}
					}

					if (table.GRR._left_card_count == 0) {
						table.GRR._chi_hu_rights[i].opr_or(GameConstants_KWX.CHR_HAI_DI_HU);
					}

					table.GRR._chi_hu_card[i][0] = target_card;
					table.process_chi_hu_player_score(i, _out_card_player, _out_card_data, false);

					// 记录
					table._player_result.jie_pao_count[i]++;
					table._player_result.dian_pao_count[_out_card_player]++;
					if (table.GRR._chi_hu_rights[i].da_hu_count > 0) {
						table._player_result.da_hu_jie_pao[i]++;
						table._player_result.da_hu_dian_pao[_out_card_player]++;
					} else {
						table._player_result.xiao_hu_jie_pao[i]++;
						table._player_result.xiao_hu_dian_pao[_out_card_player]++;
					}
				}

				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants_KWX.Game_End_NORMAL), 500,
						TimeUnit.MILLISECONDS);
				return true;
			}
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
		tableResponse.setCurrentPlayer(_out_card_player);
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

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		tableResponse.setSendCardData(0);

		int hand_cards[] = new int[GameConstants_KWX.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		table.filterHandCards(seat_index, hand_cards, hand_card_count);
		for (int i = 0; i < hand_card_count && table.player_liang[seat_index] != 1; i++) {
			tableResponse.addCardsData(hand_cards[i]);
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
			}

			// table.operate_out_card(_out_card_player, 1, new int[] {
			// _out_card_data }, GameConstants.OUT_CARD_TYPE_MID, seat_index);

			if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
				table.operate_player_action(seat_index, false);
			}
		}

		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			if (table.player_liang[p] == 1) {
				table.operate_player_cards(p, 0, null, 0, null);
				int cards[] = new int[GameConstants_KWX.MAX_COUNT];
				hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[p], cards);
				table.operate_show_card(p, GameConstants.Show_Card_LY_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);
			}
		}
		return true;
	}
}
