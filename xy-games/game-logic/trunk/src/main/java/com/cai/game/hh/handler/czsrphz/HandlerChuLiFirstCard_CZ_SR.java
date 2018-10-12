package com.cai.game.hh.handler.czsrphz;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.Constants_CZ_SR;
import com.cai.common.constant.game.Constants_ChenZhou;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.hh.handler.HHHandlerDispatchCard;

public class HandlerChuLiFirstCard_CZ_SR extends HHHandlerDispatchCard<Table_CZ_SR> {

	@Override
	public void exe(Table_CZ_SR table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();

		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]++;

		int cards[] = new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = 0;

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index;

		int an_long_Index[] = new int[5];
		int an_long_count = 0;
		boolean ti_send_card = false;

		for (int k = 0; k < table.getTablePlayerNumber(); k++) {
			an_long_count = 0;

			for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
				if (table.GRR._cards_index[k][i] == 4) {
					an_long_Index[an_long_count++] = i;
					if (i == table._logic.switch_to_card_index(table._send_card_data))
						ti_send_card = true;
				}
			}

			for (int i = 0; i < an_long_count; i++) {
				int cbWeaveIndex = table.GRR._weave_count[k];
				table.GRR._weave_items[k][cbWeaveIndex].public_card = 1;
				table.GRR._weave_items[k][cbWeaveIndex].center_card = table._logic
						.switch_to_card_data(an_long_Index[i]);
				table.GRR._weave_items[k][cbWeaveIndex].weave_kind = GameConstants.WIK_AN_LONG;
				table.GRR._weave_items[k][cbWeaveIndex].provide_player = _seat_index;
				table.GRR._weave_items[k][cbWeaveIndex].hu_xi = table._logic
						.get_weave_hu_xi(table.GRR._weave_items[k][cbWeaveIndex]);
				table.GRR._weave_count[k]++;
				table._long_count[k]++;

				table.GRR._cards_index[k][an_long_Index[i]] = 0;

				table.GRR._card_count[k] = table._logic.get_card_count_by_index(table.GRR._cards_index[k]);
			}

			if (an_long_count > 0) {
				int _action = GameConstants.WIK_AN_LONG;
				table.operate_effect_action(k, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
						GameConstants.INVALID_SEAT);

				hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[k], cards);
				table.operate_player_cards(k, hand_card_count, cards, table.GRR._weave_count[k],
						table.GRR._weave_items[k]);
			}

			if (an_long_count >= 2) { // 天胡是3提，所以这里暗龙最多2个
				table._ti_two_long[k] = true; // 起手提2龙
			}
		}

		ChiHuRight chr[] = new ChiHuRight[table.getTablePlayerNumber()];
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			chr[i] = table.GRR._chi_hu_rights[i];
			chr[i].set_empty();
		}
		
		// 天胡不能算自摸
		int card_type = Constants_CZ_SR.HU_CARD_TYPE_FAN_PAI;
		
		int bHupai = 0;
		
		int action_hu[] = new int[table.getTablePlayerNumber()];

		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            int i = (table._current_player + p) % table.getTablePlayerNumber();
            
			int card_data = table._send_card_data;

			if (table._current_player == i)
				card_data = 0;

			if (_seat_index != i && ti_send_card == true)
				continue;
			
			if (_seat_index == i) {
				card_type = Constants_CZ_SR.HU_CARD_TYPE_ZIMO;
			}

			PlayerStatus tempPlayerStatus = table._playerStatus[i];
			tempPlayerStatus.reset();

			int hu_xi[] = new int[1];

			action_hu[i] = table.analyse_chi_hu_card(table.GRR._cards_index[i], table.GRR._weave_items[i],
					table.GRR._weave_count[i], i, _seat_index, card_data, chr[i], card_type, hu_xi, true);

			if (action_hu[i] != GameConstants.WIK_NULL) {
				if (_seat_index == i) {
					tempPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
					tempPlayerStatus.add_chi_hu(table._send_card_data, i);
				} else {
					tempPlayerStatus.add_action(GameConstants.WIK_CHI_HU);
					tempPlayerStatus.add_chi_hu(table._send_card_data, i);
				}
				
				if (table.has_rule(Constants_ChenZhou.GAME_RULE_QIANG_ZHI_HU)) {
				    if (_seat_index != i) {
                        GameSchedule.put(new Runnable() {
                            @Override
                            public void run() {
                                table.handler_operate_card(i, GameConstants.WIK_CHI_HU, table._send_card_data, -1);
                            }
                        }, 1500, TimeUnit.MILLISECONDS);
                    } else {
                        GameSchedule.put(new Runnable() {
                            @Override
                            public void run() {
                                table.handler_operate_card(i, GameConstants.WIK_ZI_MO, table._send_card_data, -1);
                            }
                        }, 1500, TimeUnit.MILLISECONDS);
                    }
                    
                    return;
                }

				tempPlayerStatus.add_action(GameConstants.WIK_NULL);
				tempPlayerStatus.add_pass(table._send_card_data, _seat_index);
				table.operate_player_action(i, false);

				bHupai = 1;
			} else {
				chr[i].set_empty();
			}

		}

		if (bHupai == 0) {
			hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
					table.GRR._weave_items[_seat_index]);
			table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT, false);

			// 如果庄家起手提2龙，直接免打1张，下家直接摸牌，否则出牌，其他地方使用table._long_count来辨别重跑重提
//			if (table._ti_two_long[_seat_index]) {
//				table._ti_two_long[_seat_index] = false;
//				
//				int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
//
//				table._playerStatus[_seat_index].clean_action();
//				table._playerStatus[_seat_index].clean_status();
//				table._current_player = next_player;
//				table._last_player = next_player;
//
//				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);
//			} else {
//				curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);
//				table.operate_player_status();
//			}
			curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
		}

		return;
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
		if (operate_card != table._send_card_data) {
			table.log_player_error(seat_index, "操作牌，与当前牌不一样");
			return true;
		}

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { GameConstants.WIK_NULL }, 1);
		}

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

		int eat_type = GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT
				| GameConstants.WIK_DDX | GameConstants.WIK_XXD | GameConstants.WIK_EQS | GameConstants.WIK_YWS;

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
						if (!((target_action == GameConstants.WIK_NULL)
								|| (target_action & eat_type) != GameConstants.WIK_NULL))
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
				table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
						new long[] { _action }, 1, GameConstants.INVALID_SEAT);

				int cards[] = new int[GameConstants.MAX_HH_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

				table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
						table.GRR._weave_items[_seat_index]);
			}

			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, table._send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
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

					table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);
					return true;
				}

				// _send_card_data = 0; // 这行代码会引起重连时出错
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
		case GameConstants.WIK_ZI_MO:
		case GameConstants.WIK_CHI_HU: {
			table.GRR._chi_hu_rights[target_player].set_valid(true);

			table.GRR._chi_hu_card[target_player][0] = operate_card;

			table._cur_banker = target_player;

			table.operate_player_get_card(_seat_index, 1, new int[] { table._send_card_data },
					GameConstants.INVALID_SEAT, false);

			table._shang_zhuang_player = target_player;

			table.process_chi_hu_player_operate(target_player, operate_card, true);

			if (target_player == _seat_index) {
				table.GRR._chi_hu_rights[target_player].opr_or(Constants_CZ_SR.CHR_TIAN_HU);
			}
			table.process_chi_hu_player_score_phz(target_player, _seat_index, operate_card, true);

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
		super.handler_player_be_in_room(table, seat_index);
		table.istrustee[seat_index] = false;
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;
		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		return true;
	}
}
