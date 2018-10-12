package com.cai.game.hh.handler.new_czphz;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_New_ChenZhou;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.hh.handler.HHHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerSanTiWuKan_ChenZhou extends HHHandlerDispatchCard<Table_New_ChenZhou> {
	boolean zhuang_has_tian_hu = false;
	boolean is_fa_pai = false;

	@SuppressWarnings({ "unused", "static-access" })
	@Override
	public void exe(Table_New_ChenZhou table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();

		table._current_player = _seat_index;
		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		table.GRR._left_card_count--;

		if (table.DEBUG_CARDS_MODE) {
			_send_card_data = 0x09;
		}

		table._send_card_data = _send_card_data;
		table._provide_player = _seat_index;

		boolean is_hu = false;
		int send_index = table._logic.switch_to_card_index(table._send_card_data);

		// TODO 庄家发第一张牌的时候，无论有什么情况，都显示抓牌，如果没人胡，在牌桌上关掉牌显示，并加到手牌
		table.operate_player_get_card(_seat_index, 1, new int[] { table._send_card_data }, GameConstants.INVALID_SEAT, false);

		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (table._current_player + p) % table.getTablePlayerNumber();
			int ti_count = 0;
			int sao_count = 0;
			int hong_pai_count = 0;
			int hei_pai_count = 0;
			int all_cards_count = 0;

			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				if ((i == table._current_player) && (j == send_index))
					table.GRR._cards_index[i][j]++;

				if (table._logic.color_hei(table._logic.switch_to_card_data(j)) == true) {
					hei_pai_count += table.GRR._cards_index[i][j];
				} else {
					hong_pai_count += table.GRR._cards_index[i][j];
				}

				if (table.GRR._cards_index[i][j] == 4) {
					ti_count++;
					if ((i == table._current_player) && (j == send_index))
						is_fa_pai = true;
				}
				if (table.GRR._cards_index[i][j] == 3) {
					sao_count++;
					if ((i == table._current_player) && (j == send_index))
						is_fa_pai = true;
				}

				if ((i == table._current_player) && (j == send_index))
					table.GRR._cards_index[i][j]--;
			}

			if (ti_count >= 2)
				table._ti_mul_long[i] = ti_count - 1;

			// TODO 判断庄家是否有一般胡
			if (i == table._current_player && ti_count <= 1) {
				ChiHuRight chr = table.GRR._chi_hu_rights[i];
				chr.set_empty();

				PlayerStatus tempPlayerStatus = table._playerStatus[i];
				tempPlayerStatus.reset();

				int hu_xi[] = new int[1];

				int action_hu = table.analyse_chi_hu_card(table.GRR._cards_index[i], table.GRR._weave_items[i], table.GRR._weave_count[i], i,
						_seat_index, _send_card_data, chr, GameConstants.HU_CARD_TYPE_FAN_PAI, hu_xi, true);

				if (action_hu != GameConstants.WIK_NULL) {
					tempPlayerStatus.add_action(GameConstants.WIK_CHI_HU);
					tempPlayerStatus.add_chi_hu(table._send_card_data, _seat_index);

					// TODO 天胡
					if (table.has_rule(Constants_New_ChenZhou.GAME_RULE_TIAN_DI_HU))
						chr.opr_or(Constants_New_ChenZhou.CHR_TIAN_HU);

					tempPlayerStatus.add_action(GameConstants.WIK_NULL);
					tempPlayerStatus.add_pass(table._send_card_data, _seat_index);

					if (!table.has_rule(Constants_New_ChenZhou.GAME_RULE_YOU_HU_BI_HU))
						table.operate_player_action(i, false);

					if (table.has_rule(Constants_New_ChenZhou.GAME_RULE_YOU_HU_BI_HU)) {
						GameSchedule.put(new Runnable() {
							@Override
							public void run() {
								table.handler_operate_card(i, GameConstants.WIK_CHI_HU, table._send_card_data, -1);
							}
						}, table.time_for_force_win, TimeUnit.MILLISECONDS);

						return;
					}

					is_hu = true;

					zhuang_has_tian_hu = true;
				} else {
					chr.set_empty();
				}
			}

			// 如果庄家有天胡
			if (i == table._current_player && zhuang_has_tian_hu)
				continue;

			// if ((ti_count >= 3) || (sao_count >= 5)) {
			// ChiHuRight chr = table.GRR._chi_hu_rights[i];
			// chr.set_empty();
			//
			// int all_hu_xi = 0;
			//
			// for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
			// if ((i == table._current_player) && (j == send_index))
			// table.GRR._cards_index[i][j]++;
			// if (table.GRR._cards_index[i][j] == 4) {
			// if (j < 10)
			// all_hu_xi += 12;
			// else
			// all_hu_xi += 9;
			// }
			// if (table.GRR._cards_index[i][j] == 3) {
			// if (j < 10)
			// all_hu_xi += 6;
			// else
			// all_hu_xi += 3;
			// }
			// if ((i == table._current_player) && (j == send_index))
			// table.GRR._cards_index[i][j]--;
			// }
			//
			// boolean b_hu_xi = false;
			//
			// if (all_hu_xi >= table.get_basic_hu_xi()) {
			// b_hu_xi = true;
			// }
			//
			// if (b_hu_xi == true) {
			// int weave_count = 0;
			//
			// for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
			// if ((i == table._current_player) && (j == send_index))
			// table.GRR._cards_index[i][j]++;
			//
			// if (table.GRR._cards_index[i][j] == 4 && (ti_count >= 3)) {
			// table._hu_weave_items[i][weave_count].center_card =
			// table._logic.switch_to_card_data(j);
			// table._hu_weave_items[i][weave_count].weave_kind =
			// GameConstants.WIK_AN_LONG;
			// table._hu_weave_items[i][weave_count].hu_xi = table._logic
			// .get_weave_hu_xi(table._hu_weave_items[i][weave_count]);
			//
			// weave_count++;
			// }
			//
			// if (table.GRR._cards_index[i][j] == 3 && (sao_count >= 5)) {
			// table._hu_weave_items[i][weave_count].center_card =
			// table._logic.switch_to_card_data(j);
			// table._hu_weave_items[i][weave_count].weave_kind =
			// GameConstants.WIK_KAN;
			// table._hu_weave_items[i][weave_count].hu_xi = table._logic
			// .get_weave_hu_xi(table._hu_weave_items[i][weave_count]);
			//
			// weave_count++;
			// }
			// if ((i == table._current_player) && (j == send_index))
			// table.GRR._cards_index[i][j]--;
			// }
			//
			// int hu_card = table._hu_weave_items[i][weave_count -
			// 1].center_card;
			// table._hu_weave_count[i] = weave_count;
			// all_cards_count = hong_pai_count + hei_pai_count;
			//
			// // TODO 特殊天胡不处理红黑点
			// // if
			// (table.has_rule(Constants_New_ChenZhou.GAME_RULE_HONG_HEI_DIAN)
			// // ||
			// table.has_rule(Constants_New_ChenZhou.GAME_RULE_HONG_HEI_DIAN_2_FAN))
			// {
			// // if (hong_pai_count == 0) {
			// // chr.opr_or(Constants_New_ChenZhou.CHR_ALL_HEI);
			// // } else if (hong_pai_count == 1) {
			// // chr.opr_or(Constants_New_ChenZhou.CHR_ONE_HONG);
			// // } else if (hong_pai_count >= 10) {
			// // chr.opr_or(Constants_New_ChenZhou.CHR_TEN_HONG_PAI);
			// // }
			// // }
			//
			// chr.opr_or(Constants_New_ChenZhou.CHR_SPECAIL_TIAN_HU); // 特殊天胡
			//
			// PlayerStatus curPlayerStatus = table._playerStatus[i];
			// curPlayerStatus.reset();
			//
			// curPlayerStatus.add_action(GameConstants.WIK_CHI_HU);
			// curPlayerStatus.add_chi_hu(hu_card, i);
			//
			// curPlayerStatus.add_action(GameConstants.WIK_NULL);
			// curPlayerStatus.add_pass(hu_card, i);
			//
			// if
			// (!table.has_rule(Constants_New_ChenZhou.GAME_RULE_YOU_HU_BI_HU))
			// table.operate_player_action(i, false);
			//
			// if
			// (table.has_rule(Constants_New_ChenZhou.GAME_RULE_YOU_HU_BI_HU)) {
			// GameSchedule.put(new Runnable() {
			// @Override
			// public void run() {
			// table.handler_operate_card(i, GameConstants.WIK_CHI_HU,
			// table._send_card_data, -1);
			// }
			// }, table.time_for_force_win, TimeUnit.MILLISECONDS);
			//
			// return;
			// }
			//
			// is_hu = true;
			// } else {
			// chr.set_empty();
			// }
			// }
		}

		if (is_hu == false) {
			table._provide_card = table._send_card_data;

			table.exe_chuli_first_card(_seat_index, GameConstants.WIK_NULL, table.time_for_deal_first_card);
		} else {
			// 等待别人操作这张牌
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._playerStatus[i].has_action()) {
					table._playerStatus[i].set_status(GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(i, false);
				}
			}
		}

		// 这行代码比较特殊，必须将_send_card_data重置为0，否则，小结算之后的断线重连会显示之前的牌
		_send_card_data = 0;

		return;
	}

	@SuppressWarnings("unused")
	@Override
	public boolean handler_operate_card(Table_New_ChenZhou table, int seat_index, int operate_code, int operate_card, int luoCode) {
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

		// TODO 特殊天胡时，不一定就是发的那张牌
		// if (operate_card != table._send_card_data) {
		// table.log_player_error(seat_index, "操作牌，与当前牌不一样");
		// return true;
		// }

		table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1);

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
					cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform()) + table.getTablePlayerNumber() - p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count, table._playerStatus[i]._action)
							+ table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					// 获取已经执行的动作的优先级
					cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform()) + target_p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbTargetActionRank = table._logic.get_action_list_rank(table._playerStatus[target_player]._action_count,
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
		case GameConstants.WIK_NULL: {
			table._provide_card = table._send_card_data;

			table.exe_chuli_first_card(_seat_index, GameConstants.WIK_NULL, table.time_for_deal_first_card);

			return true;
		}
		case GameConstants.WIK_CHI_HU: {
			table.GRR._chi_hu_rights[target_player].set_valid(true);

			table.GRR._chi_hu_card[target_player][0] = operate_card;

			table._cur_banker = target_player;

			table._shang_zhuang_player = target_player;

			table.process_chi_hu_player_operate(target_player, operate_card, true);

			table.process_chi_hu_player_score_phz(target_player, _seat_index, operate_card, true);

			table._player_result.hu_pai_count[target_player]++;
			table._player_result.ying_xi_count[target_player] += table._hu_xi[target_player];

			table.countChiHuTimes(target_player, true);

			int delay = table.time_for_display_win_border;

			if (table.GRR._chi_hu_rights[target_player].type_count > 2) {
				delay += table.GRR._chi_hu_rights[target_player].type_count - 2;
			}

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), target_player, GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_New_ChenZhou table, int seat_index) {
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
				int_array.addItem(table.GRR._discard_cards[i][j]);
			}
			tableResponse.addDiscardCards(int_array);

			// 组合扑克
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE_HH; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_item.setHuXi(table.GRR._weave_items[i][j].hu_xi);
				if (seat_index != i) {
					if ((table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_TI_LONG
							|| table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_AN_LONG
							|| table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_AN_LONG_LIANG)
							&& table.GRR._weave_items[i][j].public_card == 0) {
						weaveItem_item.setCenterCard(0);
					} else {
						if (table.is_mj_type(GameConstants.GAME_TYPE_PHZ_XT) && table.has_rule(GameConstants.GAME_RULE_DI_AN_WEI)
								&& table._xt_display_an_long[i] == true)
							weaveItem_item.setCenterCard(0);
						else
							weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
					}
				} else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			//
			tableResponse.addWinnerOrder(0);
			tableResponse.addHuXi(table._hu_xi[i]);

			// 牌
			if (i == _seat_index) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}

		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		// 如果断线重连的人是自己
		// if(seat_index == _seat_index){
		// if(!((seat_index == table._current_player) && (_send_card_data ==
		// 0)))
		// table._logic.remove_card_by_data(hand_cards, _send_card_data);
		// }

		for (int x = 0; x < hand_card_count; x++) {
			if (table.is_card_has_wei(hand_cards[x])) { // 如果是偎的牌
				// TODO 射跑后，手牌强制偎或提了，出牌不需考虑打出后剩余牌是否听牌，第二次射跑及后续射跑也不需考虑打出后是否听牌状态。
				if (table.shoot_count[_seat_index] > 1) {
					hand_cards[x] += GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT;
				} else {
					// 判断打出这张牌是否能听牌
					table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(hand_cards[x])]--;
					boolean b_is_ting_state = table.is_ting_state(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
							table.GRR._weave_count[_seat_index], _seat_index);
					table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(hand_cards[x])]++;

					if (b_is_ting_state)
						hand_cards[x] += GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT;
					else
						hand_cards[x] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_SHOOT;
				}
			}
		}

		for (int i = 0; i < GameConstants.MAX_HH_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		// 摸牌
		if (_send_card_data != 0)
			table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, seat_index, false);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		if (table._is_xiang_gong[seat_index] == true)
			table.operate_player_xiang_gong_flag(seat_index, table._is_xiang_gong[seat_index]);

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;
		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		return true;
	}

}
