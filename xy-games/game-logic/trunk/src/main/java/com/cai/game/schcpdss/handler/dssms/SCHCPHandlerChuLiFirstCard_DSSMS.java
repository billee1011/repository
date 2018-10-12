/**
 * 
 */
package com.cai.game.schcpdss.handler.dssms;

import java.util.Arrays;

/**
 * @author xwy
 *
 */

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.schcpdss.handler.SCHCPDSSHandlerDispatchCard;

public class SCHCPHandlerChuLiFirstCard_DSSMS extends SCHCPDSSHandlerDispatchCard<SCHCPDSSTable_MS> {

	@Override
	public void exe(SCHCPDSSTable_MS table) {
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

		// 荒庄结束
		if (table.GRR._left_card_count == 0) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}
			// table._cur_banker = (table.GRR._banker_player +
			// table.getTablePlayerNumber() + 1)
			// % table.getTablePlayerNumber();
			table._shang_zhuang_player = GameConstants.INVALID_SEAT;
			// 流局
			table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);

			return;
		}

		// 刷新手牌包括组合
		// _send_card_data = table._send_card_data;

		table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT, false);

		boolean have_check_peng = true;
		if (table._game_type_index == GameConstants.GAME_TYPE_QIONG_LAI_HONG) {
			have_check_peng = false;
		}

		// 加到手牌
		int next_player = _seat_index;
		while (table._guo_peng_count < table.getTablePlayerNumber() && have_check_peng) {
			int count = 0;
			if (table._ti_mul_long[next_player] > 0) {
				table.exe_dispatch_add_card(next_player);
				return;
			}
			if (table._ti_mul_long[next_player] == 0) {
				count = table.estimate_player_tou(next_player);
				if (count > 0) {
					table._ti_mul_long[next_player] += count;
					if (table._ti_mul_long[next_player] > 0) {
						table._playerStatus[next_player].set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
						table.operate_player_action(next_player, false);
						int operate_code = GameConstants.DSS_WIK_DH_ONE;
						if (table._playerStatus[next_player].has_action_by_code(GameConstants.DSS_WIK_DH_ONE))
							operate_code = GameConstants.DSS_WIK_DH_ONE;
						else if (table._playerStatus[next_player].has_action_by_code(GameConstants.DSS_WIK_DH_TWO))
							operate_code = GameConstants.DSS_WIK_DH_TWO;
						else if (table._playerStatus[next_player].has_action_by_code(GameConstants.DSS_WIK_DH_THREE))
							operate_code = GameConstants.DSS_WIK_DH_THREE;
						else if (table._playerStatus[next_player].has_action_by_code(GameConstants.DSS_WIK_DH_FOUR))
							operate_code = GameConstants.DSS_WIK_DH_FOUR;
						int operate_card = table._playerStatus[next_player].get_operate_card();
						table.handler_operate_card(next_player, operate_code, operate_card, -1);
						return;
					}
				}
				if (count == 0) {
					if (table.estimate_player_peng(next_player) != GameConstants.CP_WIK_NULL)
						count++;
				}
			}
			table._ti_mul_long[next_player] += count;
			if (table._ti_mul_long[next_player] > 0) {
				table._playerStatus[next_player].set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
				table.operate_player_action(next_player, false);

				return;
			}

			next_player = (next_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			table._current_player = next_player;
			table._guo_peng_count++;
		}
		next_player = (table._cur_banker + table.getTablePlayerNumber() - 1) % table.getTablePlayerNumber();

		while (table._guo_bao_ting < table.getTablePlayerNumber() && have_check_peng) {
			if (table._is_ting_pai[next_player] != 0) {
				next_player = (next_player + table.getTablePlayerNumber() - 1) % table.getTablePlayerNumber();
			}
			if (table._is_ting_pai[next_player] == 0) {

				int ting_cards[] = new int[GameConstants.MAX_CP_COUNT];
				Arrays.fill(ting_cards, 0);
				int ting_count = table.get_hh_ting_card_twenty(ting_cards, table.GRR._cards_index[next_player], table.GRR._weave_items[next_player],
						table.GRR._weave_count[next_player], next_player, next_player);
				if (ting_count > 0) {
					table._is_ting_pai[next_player] = -1;
					table._playerStatus[next_player].add_action(GameConstants.DSS_WIK_BAO_TING);
					table._playerStatus[next_player].add_bao_ting(0, GameConstants.DSS_WIK_BAO_TING, next_player);
					table._playerStatus[next_player].add_action(GameConstants.DSS_WIK_NULL);
					table._playerStatus[next_player].add_pass(0, next_player);
					table._playerStatus[next_player].set_status(GameConstants.Player_Status_OPR_CARD);//
					// 操作状态
					table.operate_player_action(next_player, false);
					return;
				}

			}
			next_player = (next_player + table.getTablePlayerNumber() - 1) % table.getTablePlayerNumber();
			table._current_player = next_player;
			table._guo_bao_ting++;
		}
		_seat_index = table._cur_banker;
		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();
		int action = 0;
		int is_hu = 0;
		int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
		if (table._long_count[_seat_index] == 0) {
			action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], _seat_index, _seat_index, 0, chr, card_type, true);// 自摸
			if (table._playerStatus[_seat_index]._hu_card_count == 0)
				table._playerStatus[_seat_index]._hu_card_count = table.get_hh_ting_card_twenty(table._playerStatus[_seat_index]._hu_cards,
						table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index,
						_seat_index);

		}
		if (action != GameConstants.WIK_NULL) {
			// 添加动作
			table._playerStatus[_seat_index].add_action(GameConstants.WIK_ZI_MO);
			table._playerStatus[_seat_index].add_zi_mo(table._banker_card, _seat_index);
			table._playerStatus[_seat_index].add_action(GameConstants.CP_WIK_NULL);
			table._playerStatus[_seat_index].add_pass(table._banker_card, _seat_index);
			is_hu = 1;
		} else {
			chr.set_empty();
		}
		if(table._long_count[_seat_index] == 0){
			ChiHuRight chr_wu[] = new ChiHuRight[table.getTablePlayerNumber()];
			for(int i = 0; i< table.getTablePlayerNumber();i++){
				if(action != GameConstants.WIK_NULL && i == table._cur_banker )
					continue;
				
				chr_wu[i] = table.GRR._chi_hu_rights[i];
				chr_wu[i].set_empty();
				if(table._logic.is_wu_cheng(table.GRR._cards_index[i], table.GRR._weave_count[i]) == true){
					chr_wu[i].opr_or_long(GameConstants.CHR_CP_WU_CHENG);
					table._playerStatus[i].add_action(GameConstants.DSS_WIK_WUCHENG);
					table._playerStatus[i].add_type(table._banker_card,GameConstants.DSS_WIK_WUCHENG, i);
					table._playerStatus[i].add_action(GameConstants.CP_WIK_NULL);
					table._playerStatus[i].add_pass(table._banker_card, i);
					
					is_hu = 1;
				}
				else 
					chr_wu[i].set_empty();
			}
		}
		
		
		
		if (table._playerStatus[table._cur_banker].has_zi_mo()||is_hu == 1) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				int cards[] = new int[GameConstants.DSS_MAX_CP_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], cards);
				table.operate_player_cards(i, hand_card_count, cards, table.GRR._weave_count[i], table.GRR._weave_items[i]);
			}
			table._current_player = table._cur_banker;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._playerStatus[i].has_action()) {
					table._playerStatus[i].set_status(GameConstants.Player_Status_OPR_CARD);//
					// 操作状态
					table.operate_player_action(i, false);
				}

			}
			return;
		}
		table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT, false);

		table._current_player = _seat_index;
		table._playerStatus[table._cur_banker].set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
		table.operate_player_status();
		// table._is_tian_hu = true;

	}

	public boolean handler_ask_player(SCHCPDSSTable_MS table, int seat_index) {

		int next_player = (seat_index + table.getTablePlayerNumber() - 1) % table.getTablePlayerNumber();
		table._current_player = next_player;
		table._guo_bao_ting++;
		table.exe_chuli_first_card(seat_index, GameConstants.WIK_NULL, 1000);
		return true;

	}

	/***
	 * //用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	@Override
	public boolean handler_operate_card(SCHCPDSSTable_MS table, int seat_index, int operate_code, int operate_card, int luoCode) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		// 效验操作
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("DispatchCard 没有这个操作:" + operate_code);
			return false;
		}

		table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1);

		if (operate_code == GameConstants.WIK_NULL) {
			if (playerStatus.has_zi_mo() == true)
				table._guo_hu_pai_cards[seat_index][table._guo_hu_pai_count[seat_index]++] = operate_card;
		} else {
			table._guo_hu_xt[seat_index] = -1;
			table._guo_hu_pai_count[seat_index] = 0;
		}
		// 是否已经响应
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "DispatchCard 出牌,玩家操作已失效");
			return true;
		}
		// if (seat_index != _seat_index) {
		// table.log_error("DispatchCard 不是当前玩家操作");
		// return false;
		// }
		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "DispatchCard 出牌,玩家已操作");
			return true;
		}
		// 记录玩家的操作
		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();

		// 变量定义 优先级最高操作的玩家和操作--不通炮的算法
		int target_player = seat_index;
		int target_action = operate_code;
		int target_p = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			if (i == target_player) {
				target_p = table.getTablePlayerNumber() - p;
			}
		}
		int cbActionRank[] = new int[table.getTablePlayerNumber()];
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
					target_p = table.getTablePlayerNumber() - p;
				}
			}
		}

		// 优先级最高的人还没操作

		if (table._playerStatus[target_player].is_respone() == false) {
			return true;
		}

		// 执行动作
		switch (target_action) {
		case GameConstants.WIK_NULL: {

			// 用户状态

			if (table._is_ting_pai[target_player] == -1) {
				table._is_ting_pai[target_player] = 2;
				table.operate_player_xiang_gong_flag(target_player, false);
				handler_ask_player(table, target_player);
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {

					table._playerStatus[i].clean_action();
					table._playerStatus[i].clean_status();

					table.operate_player_action(i, true);
				}
				return true;
			}
			// 用户状态
			if (table._playerStatus[target_player].has_zi_mo()||table._playerStatus[target_player].has_action_by_bh_code(GameConstants.DSS_WIK_WUCHENG)) {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {

					table._playerStatus[i].clean_action();
					table._playerStatus[i].clean_status();

					table.operate_player_action(i, true);
				}
				table._playerStatus[target_player].clean_action();
				table._playerStatus[target_player].clean_status();
				_seat_index = table._cur_banker;
				table._current_player = _seat_index;
				_send_card_data = 0;
				table._playerStatus[table._cur_banker].set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态

				table.operate_player_status();
				return true;

			}
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {

				table._playerStatus[i].clean_action();
				table._playerStatus[i].clean_status();

				table.operate_player_action(i, true);
			}

			table._playerStatus[target_player].clean_action();
			table._playerStatus[target_player].clean_status();
			int next_player = (target_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			table._guo_peng_count++;
			table._current_player = next_player;
			table.exe_chuli_first_card(next_player, GameConstants.WIK_NULL, 1000);

			return true;

		}
		case GameConstants.DSS_WIK_BAO_TING: {
			table._is_ting_pai[target_player] = 1;
			table.operate_player_xiang_gong_flag(target_player, true);
			// 效果
			table.operate_effect_action(target_player, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.DSS_WIK_BAO_TING }, 1,
					GameConstants.INVALID_SEAT);
			handler_ask_player(table, target_player);
			return true;
		}
		case GameConstants.DSS_WIK_DH_ONE:// 一张
		case GameConstants.DSS_WIK_DH_TWO:// 两张
		case GameConstants.DSS_WIK_DH_THREE:// 三张
		case GameConstants.DSS_WIK_DH_FOUR:// 四张
		{
			int effect_action = 0;
			int count = 0;
			do {
				if (count == 0) {
					operate_card = 0x12;
					target_action = table._playerStatus[target_player].is_get_card(operate_card);
				} else {
					operate_card = 0x0b;
					target_action = table._playerStatus[target_player].is_get_card(operate_card);
				}
				if (target_action == 0) {
					count++;
					continue;
				} else {
					count++;
				}
				int cbWeaveIndex = table.GRR._weave_count[target_player];
				for (int i = 0; i < table.GRR._weave_count[target_player]; i++) {
					if (operate_card == table.GRR._weave_items[target_player][i].center_card) {
						cbWeaveIndex = i;
						break;
					}
				}

				table.GRR._weave_items[target_player][cbWeaveIndex].public_card = 1;
				table.GRR._weave_items[target_player][cbWeaveIndex].center_card = operate_card;
				table.GRR._weave_items[target_player][cbWeaveIndex].weave_kind = target_action;
				table.GRR._weave_items[target_player][cbWeaveIndex].provide_player = target_player;
				table.GRR._weave_items[target_player][cbWeaveIndex].hu_xi = 0;
				int cbCardIndex = table._logic.switch_to_card_index(operate_card);
				table.GRR._cards_index[target_player][cbCardIndex] = 0;
				if (cbWeaveIndex == table.GRR._weave_count[target_player])
					table.GRR._weave_count[target_player]++;
				effect_action = target_action;

			} while (count < 2);

			// 用户状态
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {

				table._playerStatus[i].clean_action();
				table._playerStatus[i].clean_status();

				table.operate_player_action(i, true);
			}
			// 效果
			table.operate_effect_action(target_player, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { effect_action }, 1,
					GameConstants.INVALID_SEAT);
			// 刷新手牌包括组合
			int cards[] = new int[GameConstants.DSS_MAX_CP_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[target_player], cards);
			table.operate_player_cards(target_player, hand_card_count, cards, table.GRR._weave_count[target_player],
					table.GRR._weave_items[target_player]);
			table.exe_dispatch_add_card(target_player);
			return true;
		}
		case GameConstants.DSS_WIK_TOU: {
			// 用户状态
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {

				table._playerStatus[i].clean_action();
				table._playerStatus[i].clean_status();

				table.operate_player_action(i, true);
			}

			int cbWeaveIndex = table.GRR._weave_count[target_player];
			table.GRR._weave_items[target_player][cbWeaveIndex].public_card = 1;
			table.GRR._weave_items[target_player][cbWeaveIndex].center_card = operate_card;
			table.GRR._weave_items[target_player][cbWeaveIndex].weave_kind = target_action;
			table.GRR._weave_items[target_player][cbWeaveIndex].provide_player = target_player;
			table.GRR._weave_items[target_player][cbWeaveIndex].hu_xi = table._logic.get_analyse_tuo_shu(target_action, operate_card);
			int cbCardIndex = table._logic.switch_to_card_index(operate_card);
			if (table.GRR._cards_index[target_player][cbCardIndex] > 3)
				table.GRR._cards_index[target_player][cbCardIndex] = 1;
			else
				table.GRR._cards_index[target_player][cbCardIndex] = 0;
			table.GRR._weave_count[target_player]++;
			// 效果
			table.operate_effect_action(target_player, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { target_action }, 1,
					GameConstants.INVALID_SEAT);
			// 刷新手牌包括组合
			int cards[] = new int[GameConstants.DSS_MAX_CP_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[target_player], cards);
			table.operate_player_cards(target_player, hand_card_count, cards, table.GRR._weave_count[target_player],
					table.GRR._weave_items[target_player]);
			table.exe_dispatch_add_card(target_player);
			return true;
		}
		case GameConstants.DSS_WIK_WUCHENG:
		case GameConstants.WIK_ZI_MO: // 自摸
		{
			// 用户状态
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {

				table._playerStatus[i].clean_action();
				table._playerStatus[i].clean_status();

				table.operate_player_action(i, true);
			}

			table.GRR._chi_hu_rights[target_player].set_valid(true);

			// if (table.has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_ZHUANG))
			// {// 轮装
			// if (table.GRR._banker_player == target_player) {
			// table._banker_select = target_player;
			// } else {
			// table._banker_select = (table.GRR._banker_player +
			// table.getTablePlayerNumber() + 1)
			// % table.getTablePlayerNumber();
			// }
			// }

			table._shang_zhuang_player = target_player;
			table.GRR._chi_hu_card[target_player][0] = operate_card;
			table._xing_player[target_player] = 1;
			table.set_niao_card(target_player, operate_card, true);// 结束后设置鸟牌
			table._shang_zhuang_player = target_player;
			table.process_chi_hu_player_operate(target_player, operate_card, true);

			if (table._game_type_index == GameConstants.GAME_TYPE_QIONG_LAI_HONG) {
				table.process_chi_hu_player_score_qlhf(target_player, _seat_index, operate_card, true);
			} else {
				table.process_chi_hu_player_score_schcp(target_player, _seat_index, operate_card, true);
			}

			table.countChiHuTimes(target_player, true);

			int delay = GameConstants.GAME_FINISH_DELAY_FLS;
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
	public boolean handler_player_be_in_room(SCHCPDSSTable_MS table, int seat_index) {
		int user_index = -1;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_cp_tou()) {
				user_index = i;
				break;
			}
		}
		if (user_index != -1)
			table.operate_effect_action(user_index, GameConstants.EFFECT_ACTION_CP, 1, new long[] { GameConstants.CP_WAIT_TOU }, 1, seat_index);
		super.handler_player_be_in_room(table, seat_index);
		table.istrustee[seat_index] = false;

		return true;
	}
}
