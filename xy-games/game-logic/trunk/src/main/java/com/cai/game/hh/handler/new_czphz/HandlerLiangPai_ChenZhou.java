package com.cai.game.hh.handler.new_czphz;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_New_ChenZhou;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.hh.handler.HHHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerLiangPai_ChenZhou extends HHHandlerDispatchCard<Table_New_ChenZhou> {

	@Override
	public void exe(Table_New_ChenZhou table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			// TODO 亮牌之前，显示玩家的听牌数据
			table._playerStatus[_seat_index]._hu_card_count = table.get_hh_ting_card_twenty(table._playerStatus[_seat_index]._hu_cards,
					table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index,
					_seat_index);

			int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
			int ting_count = table._playerStatus[_seat_index]._hu_card_count;

			if (ting_count > 0) {
				table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
			} else {
				ting_cards[0] = 0;
				table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
			}
		}

		// TODO 加到手牌数据，注意断线重连的时候，需要先删除一下虚拟的手牌数据
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]++;

		int cards[] = new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = 0;

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index;

		int an_long_Index[] = new int[5];
		int an_long_count = 0;
		@SuppressWarnings("unused")
		boolean ti_send_card = false;

		// TODO 提牌：庄家摸完牌就放下去，闲家在进牌或摸牌时放下去。亮一张。
		for (int k = 0; k < table.getTablePlayerNumber(); k++) {
			an_long_count = 0;

			for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
				if (table.GRR._cards_index[k][i] == 4) {
					an_long_Index[an_long_count++] = i;
					if (i == table._logic.switch_to_card_index(table._send_card_data))
						ti_send_card = true;
				}
			}

			if (an_long_count >= 2) {
				table._ti_mul_long[k] = an_long_count - 1;
			}

			if (k != _seat_index) {
				continue;
			}

			for (int i = 0; i < an_long_count; i++) {
				int cbWeaveIndex = table.GRR._weave_count[k];
				table.GRR._weave_items[k][cbWeaveIndex].public_card = 1;
				table.GRR._weave_items[k][cbWeaveIndex].center_card = table._logic.switch_to_card_data(an_long_Index[i]);
				table.GRR._weave_items[k][cbWeaveIndex].weave_kind = GameConstants.WIK_AN_LONG;
				table.GRR._weave_items[k][cbWeaveIndex].provide_player = _seat_index;
				table.GRR._weave_items[k][cbWeaveIndex].hu_xi = table._logic.get_weave_hu_xi(table.GRR._weave_items[k][cbWeaveIndex]);
				table.GRR._weave_count[k]++;
				table._long_count[k]++;

				table.GRR._cards_index[k][an_long_Index[i]] = 0;

				table.GRR._card_count[k] = table._logic.get_card_count_by_index(table.GRR._cards_index[k]);
			}

			if (an_long_count > 0) {
				int _action = GameConstants.WIK_AN_LONG;
				table.operate_effect_action(k, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, GameConstants.INVALID_SEAT);

				hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[k], cards);
				table.operate_player_cards(k, hand_card_count, cards, table.GRR._weave_count[k], table.GRR._weave_items[k]);
			}
		}

		ChiHuRight chr[] = new ChiHuRight[table.getTablePlayerNumber()];
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			chr[i] = table.GRR._chi_hu_rights[i];
			chr[i].set_empty();
		}

		// 天胡地胡
		@SuppressWarnings("unused")
		int card_type = GameConstants.HU_CARD_TYPE_FAN_PAI;

		int bHupai = 0;

		@SuppressWarnings("unused")
		int action_hu[] = new int[table.getTablePlayerNumber()];

		// TODO 闲家不能胡亮张
		// for (int p = 0; p < table.getTablePlayerNumber(); p++) {
		// int i = (table._current_player + p) % table.getTablePlayerNumber();
		//
		// int card_data = table._send_card_data;
		//
		// // TODO 将庄家的天胡判断，挪到判断特殊天胡的地方
		// if (table._current_player == i) {
		// card_data = 0;
		// continue;
		// }
		//
		// if (_seat_index != i && ti_send_card == true)
		// continue;
		//
		// PlayerStatus tempPlayerStatus = table._playerStatus[i];
		// tempPlayerStatus.reset();
		//
		// int hu_xi[] = new int[1];
		//
		// action_hu[i] = table.analyse_chi_hu_card(table.GRR._cards_index[i],
		// table.GRR._weave_items[i],
		// table.GRR._weave_count[i], i, _seat_index, card_data, chr[i],
		// card_type,
		// hu_xi, true);
		//
		// if (action_hu[i] != GameConstants.WIK_NULL) {
		// tempPlayerStatus.add_action(GameConstants.WIK_CHI_HU);
		// tempPlayerStatus.add_chi_hu(table._send_card_data, i);
		//
		// // TODO 天胡
		// if (table.has_rule(Constants_New_ChenZhou.GAME_RULE_TIAN_DI_HU))
		// if (table._current_player == i)
		// chr[i].opr_or(Constants_New_ChenZhou.CHR_TIAN_HU);
		//
		// if (table.has_rule(Constants_New_ChenZhou.GAME_RULE_YOU_HU_BI_HU)) {
		// GameSchedule.put(new Runnable() {
		// @Override
		// public void run() {
		// table.handler_operate_card(i, GameConstants.WIK_CHI_HU,
		// table._send_card_data, -1);
		// }
		// }, 1500, TimeUnit.MILLISECONDS);
		//
		// return;
		// }
		//
		// tempPlayerStatus.add_action(GameConstants.WIK_NULL);
		// tempPlayerStatus.add_pass(table._send_card_data, _seat_index);
		// table.operate_player_action(i, false);
		//
		// bHupai = 1;
		// } else {
		// chr[i].set_empty();
		// }
		// }

		if (bHupai == 0) {
			hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);

			// TODO 庄家发第一张牌的时候，无论有什么情况，都显示抓牌，如果没人胡，在牌桌上关掉牌显示，并加到手牌
			table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT, false);

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

				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, table.time_for_dispatch_card);

				return;
			}

			// TODO 起手就有双提龙：庄家如果起手听牌，直接免张，下家摸牌；庄家如果非听牌牌型，首轮出一张，下次进张免打；闲家进张免打。
			if (table._ti_mul_long[_seat_index] > 0) {

				table._ti_mul_long[_seat_index]--;
				boolean is_ting_state = table.is_ting_state(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
						table.GRR._weave_count[_seat_index], _seat_index);
				table._ti_mul_long[_seat_index]++;

				if (is_ting_state) {
					table._ti_mul_long[_seat_index]--;

					int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();

					table._playerStatus[_seat_index].clean_action();
					table._playerStatus[_seat_index].clean_status();
					table._current_player = next_player;
					table._last_player = next_player;

					table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, table.time_for_dispatch_card);
				} else {
					curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);
					table.operate_player_status();
				}
			} else {
				curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
		}

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
		if (operate_card != table._send_card_data) {
			table.log_player_error(seat_index, "操作牌，与当前牌不一样");
			return true;
		}

		table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1);

		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();

		if (operate_code == GameConstants.WIK_NULL) {
			boolean flag = false;
			for (int i = 0; i < playerStatus._action_count; i++) {
				switch (playerStatus._action[i]) {
				case GameConstants.WIK_LEFT:
				case GameConstants.WIK_CENTER:
				case GameConstants.WIK_RIGHT:
				case GameConstants.WIK_XXD:
				case GameConstants.WIK_DDX:
				case GameConstants.WIK_EQS:
				case GameConstants.WIK_YWS: {
					if (flag == false) {
						table._cannot_chi[seat_index][table._cannot_chi_count[seat_index]++] = operate_card;
						playerStatus.set_exe_pass(true);
						flag = true;
					}
				}
					break;
				case GameConstants.WIK_PENG: {
					table._cannot_peng[seat_index][table._cannot_peng_count[seat_index]++] = operate_card;
					playerStatus.set_exe_pass(true);
				}
					break;
				}
			}

		}

		switch (operate_code) {
		case GameConstants.WIK_LEFT:
		case GameConstants.WIK_CENTER:
		case GameConstants.WIK_RIGHT:
		case GameConstants.WIK_XXD:
		case GameConstants.WIK_DDX:
		case GameConstants.WIK_EQS:
		case GameConstants.WIK_YWS: {
			if (luoCode != -1)
				playerStatus.set_lou_pai_kind(luoCode);
		}
		}

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

		int target_card = table._playerStatus[target_player]._operate_card;

		int last_player = (target_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
		boolean flag = false;
		for (int j = 0; j < table._playerStatus[last_player]._action_count; j++) {

			switch (table._playerStatus[last_player]._action[j]) {
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
			case GameConstants.WIK_XXD:
			case GameConstants.WIK_DDX:
			case GameConstants.WIK_EQS:
			case GameConstants.WIK_YWS:
				if (target_action == GameConstants.WIK_NULL)
					continue;
				if (flag == false)
					if (table._playerStatus[last_player].get_exe_pass() == true) {
						table._cannot_chi[last_player][table._cannot_chi_count[last_player]--] = 0;
						flag = true;
						table._playerStatus[last_player].set_exe_pass(false);
					}

				break;
			}
		}

		int eat_type = GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT | GameConstants.WIK_DDX | GameConstants.WIK_XXD
				| GameConstants.WIK_EQS | GameConstants.WIK_YWS;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			boolean flag_temp = false;

			if (table._playerStatus[i].has_action()) {
				for (int j = 0; j < table._playerStatus[i]._action_count; j++) {

					switch (table._playerStatus[i]._action[j]) {
					case GameConstants.WIK_LEFT:
					case GameConstants.WIK_CENTER:
					case GameConstants.WIK_RIGHT:
					case GameConstants.WIK_XXD:
					case GameConstants.WIK_DDX:
					case GameConstants.WIK_EQS:
					case GameConstants.WIK_YWS:
						if (!((target_action == GameConstants.WIK_PENG) || (target_action == GameConstants.WIK_ZI_MO)))
							continue;
						if (flag_temp == false)
							if (table._playerStatus[i].get_exe_pass() == true) {
								table._cannot_chi[i][table._cannot_chi_count[i]--] = 0;
								flag_temp = true;
							}

						break;
					case GameConstants.WIK_PENG:
						if (!((target_action == GameConstants.WIK_NULL) || (target_action & eat_type) != GameConstants.WIK_NULL))
							continue;
						if (table._playerStatus[i].get_exe_pass() == false) {
							table._cannot_peng[i][table._cannot_peng_count[i]++] = operate_card;
						}
						break;
					}
				}
			}

			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		switch (target_action) {
		case GameConstants.WIK_NULL: {
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			if (table._long_count[_seat_index] > 0) {
				int _action = GameConstants.WIK_AN_LONG;
				table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
						GameConstants.INVALID_SEAT);

				int cards[] = new int[GameConstants.MAX_HH_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

				table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
						table.GRR._weave_items[_seat_index]);
			}

			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, table._send_card_data), GameConstants.DELAY_AUTO_OUT_CARD,
						TimeUnit.MILLISECONDS);
			} else {
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

					table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, table.time_for_dispatch_card);
					return true;
				}

				// _send_card_data = 0; // 这行代码会引起重连时出错

				// TODO 庄家发第一张牌的时候，无论有什么情况，都显示抓牌，如果没人胡，在牌桌上关掉牌显示，并加到手牌
				table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT, false);

				int cards[] = new int[GameConstants.MAX_HH_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
				table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
						table.GRR._weave_items[_seat_index]);

				PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
				curPlayerStatus.reset();

				curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
			return true;
		}
		case GameConstants.WIK_CHI_HU: {
			table.GRR._chi_hu_rights[target_player].set_valid(true);

			table.GRR._chi_hu_card[target_player][0] = operate_card;

			table._cur_banker = target_player;

			table._shang_zhuang_player = target_player;

			table.process_chi_hu_player_operate(target_player, operate_card, true);

			if (target_player == _seat_index) {
				table.GRR._chi_hu_rights[target_player].opr_or(Constants_New_ChenZhou.CHR_TIAN_HU);
			}

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
		if (seat_index == _seat_index) {
			table._logic.remove_card_by_data(hand_cards, _send_card_data);
		}

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

		table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT, false);

		table.send_response_to_player(seat_index, roomResponse);

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
