package com.cai.game.hh.handler.nxphz;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.hh.handler.HHHandlerDispatchCard;

public class PHZHandlerChuLiFirstCard_NingXiang extends HHHandlerDispatchCard<NingXiangHHTable> {

	@Override
	public void exe(NingXiangHHTable table) {
		NingXiangPHZUtils.cleanPlayerStatus(table);

		table._playerStatus[_seat_index].chi_hu_round_valid();

		if (NingXiangPHZUtils.endHuangZhuang(table, false)) {
			return;
		}

		ChiHuRight chrs[] = new ChiHuRight[3];
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			chrs[i] = table.GRR._chi_hu_rights[i];
			chrs[i].set_empty();
		}

		boolean haveTianHu = false;
		int hu_xi_chi[] = new int[1];
		int i = _seat_index;

		hu_xi_chi[0] = 0;

		PlayerStatus playerStatus = table._playerStatus[i];

		int hu_xi = table.analyse_chi_hu_card(table.GRR._cards_index[i], table.GRR._weave_items[i], table.GRR._weave_count[i], i, _seat_index,
				table._send_card_data, chrs[i], Constants_NingXiang.CHR_TIAN_HU, hu_xi_chi, true);

		if (hu_xi != GameConstants.WIK_NULL) {
			chrs[i].opr_or(Constants_NingXiang.CHR_ZI_MO);

			playerStatus.add_action(GameConstants.WIK_ZI_MO);
			playerStatus.add_zi_mo(table._send_card_data, i);
			playerStatus.add_action(GameConstants.WIK_NULL);
			playerStatus.add_pass(_send_card_data, _seat_index);

			haveTianHu = true;
		}

		if (haveTianHu) {
			if (table._playerStatus[i].has_action()) {
				table._playerStatus[i].set_status(GameConstants.Player_Status_OPR_CARD);
				table.operate_player_action(i, false);
			}

			return;
		}

		bankerOperaterCard(table);
	}

	private boolean judgeSanTiWuKan(NingXiangHHTable table) {
		int send_index = table._logic.switch_to_card_index(_send_card_data);

		boolean is_fa_pai = false;

		int loop = 0;
		while (loop < table.getTablePlayerNumber()) {
			int i = (table._current_player + loop) % table.getTablePlayerNumber();

			loop++;

			int ti_count = 0;
			int sao_count = 0;

			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				if ((i == table._current_player) && (j == send_index)) {
					table.GRR._cards_index[i][j]++;
				}

				if (table.GRR._cards_index[i][j] == 4) {
					ti_count++;

					if ((i == table._current_player) && (j == send_index)) {
						is_fa_pai = true;
					}
				}

				if (table.GRR._cards_index[i][j] == 3) {
					sao_count++;

					if ((i == table._current_player) && (j == send_index)) {
						is_fa_pai = true;
					}
				}

				if ((i == table._current_player) && (j == send_index)) {
					table.GRR._cards_index[i][j]--;
				}
			}

			if (ti_count >= 2)
				table._ti_mul_long[i] = ti_count - 1;

			if ((ti_count >= 3) || (sao_count >= 5) || (ti_count + sao_count >= 5)) {

				ChiHuRight chr = table.GRR._chi_hu_rights[i];
				chr.set_empty();

				int card_type = Constants_NingXiang.CHR_ZI_MO;

				for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
					if ((i == table._current_player) && (j == send_index)) {
						table.GRR._cards_index[i][j]++;
					}

					if (table.GRR._cards_index[i][j] == 4) {
						ti_count++;
					} else if (table.GRR._cards_index[i][j] == 3) {
						sao_count++;
					}

					if ((i == table._current_player) && (j == send_index)) {
						table.GRR._cards_index[i][j]--;
					}
				}

				int weave_count = 0;
				for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
					if ((i == table._current_player) && (j == send_index)) {
						table.GRR._cards_index[i][j]++;
					}

					if (table.GRR._cards_index[i][j] == 4) {
						table._hu_weave_items[i][weave_count].center_card = table._logic.switch_to_card_data(j);
						table._hu_weave_items[i][weave_count].weave_kind = GameConstants.WIK_AN_LONG;
						table._hu_weave_items[i][weave_count].hu_xi = table._logic.get_weave_hu_xi(table._hu_weave_items[i][weave_count]);
						weave_count++;
					} else if (table.GRR._cards_index[i][j] == 3) {
						table._hu_weave_items[i][weave_count].center_card = table._logic.switch_to_card_data(j);
						table._hu_weave_items[i][weave_count].weave_kind = GameConstants.WIK_KAN;
						table._hu_weave_items[i][weave_count].hu_xi = table._logic.get_weave_hu_xi(table._hu_weave_items[i][weave_count]);
						weave_count++;
					}

					if ((i == table._current_player) && (j == send_index)) {
						table.GRR._cards_index[i][j]--;
					}
				}

				table._hu_weave_count[i] = weave_count;

				if (card_type == Constants_NingXiang.CHR_ZI_MO) {
					chr.opr_or(Constants_NingXiang.CHR_ZI_MO);
				}

				PlayerStatus curPlayerStatus = table._playerStatus[i];
				curPlayerStatus.reset();

				if ((i == table._current_player) && (is_fa_pai == true)) {
					table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT, false);
				}

				table.sanTi = true;

				table.GRR._chi_hu_rights[i].set_valid(true);
				table.GRR._chi_hu_card[i][0] = 0x00;

				table.process_chi_hu_player_operate(i, 0x00, true);
				table.process_chi_hu_player_score_phz(i, i, 0x00, true);

				if (table.GRR._chi_hu_rights[i].da_hu_count > 0) {
					table._player_result.da_hu_zi_mo[i]++;
				} else {
					table._player_result.xiao_hu_zi_mo[i]++;
				}

				table.countChiHuTimes(i, true);

				int delay = GameConstants.GAME_FINISH_DELAY_FLS;
				if (table.GRR._chi_hu_rights[i].type_count > 2) {
					delay += table.GRR._chi_hu_rights[i].type_count - 2;
				}

				table._cur_banker = i;
				table._shang_zhuang_player = i;

				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);

				return true;
			}
		}
		return false;

	}

	private void bankerOperaterCard(NingXiangHHTable table) {
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]++;

		int cards[] = new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index;

		if (judgeSanTiWuKan(table)) {
			return;
		}

		int an_long_Index[] = new int[5];
		int an_long_count = 0;

		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (table.GRR._cards_index[_seat_index][i] == 4) {
				an_long_Index[an_long_count++] = i;
			}
		}

		if (an_long_count > 0) {
			int _action = GameConstants.WIK_AN_LONG;
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
					GameConstants.INVALID_SEAT);

			for (int i = 0; i < an_long_count; i++) {
				int cbWeaveIndex = table.GRR._weave_count[_seat_index];
				table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = 1;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = table._logic.switch_to_card_data(an_long_Index[i]);
				table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_AN_LONG;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _seat_index;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].hu_xi = table._logic
						.get_weave_hu_xi(table.GRR._weave_items[_seat_index][cbWeaveIndex]);

				table.GRR._weave_count[_seat_index]++;

				table._long_count[_seat_index]++;

				table.GRR._cards_index[_seat_index][an_long_Index[i]] = 0;

				table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
			}

			hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
		}

		hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);

		table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT, false);

		curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);
		table.operate_player_status();

		return;
	}

	@Override
	public boolean handler_operate_card(NingXiangHHTable table, int seat_index, int operate_code, int operate_card, int luoCode) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("DispatchCard 没有这个操作:" + operate_code);
			return false;
		}

		table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1);

		if (operate_code == GameConstants.WIK_NULL) {
			if (playerStatus.has_zi_mo() == true) {
				int index = -1;

				for (int i = 0; i < table._guo_hu_pai_count[seat_index]; i++) {
					if (table._guo_hu_pai_cards[seat_index][i] == operate_card) {
						index = i;
					}
				}

				if (index == -1) {
					index = table._guo_hu_pai_count[seat_index]++;
				}

				table._guo_hu_pai_cards[seat_index][index] = operate_card;

				int all_hu_xi = 0;
				for (int i = 0; i < table._hu_weave_count[seat_index]; i++) {
					all_hu_xi += table._hu_weave_items[seat_index][i].hu_xi;
				}

				table._guo_hu_xi[seat_index][index] = all_hu_xi;
			}
		}

		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "DispatchCard 出牌,玩家操作已失效");
			return true;
		}

		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "DispatchCard 出牌,玩家已操作");
			return true;
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
				case GameConstants.WIK_EQS: {
					if (flag == false) {
						table._cannot_chi[seat_index][table._cannot_chi_count[seat_index]++] = operate_card;
						playerStatus.set_exe_pass(true);
						flag = true;
					}
					break;
				}
				case GameConstants.WIK_PENG: {
					table._cannot_peng[seat_index][table._cannot_peng_count[seat_index]++] = operate_card;
					playerStatus.set_exe_pass(true);
					break;
				}
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
			if (luoCode != -1)
				playerStatus.set_lou_pai_kind(luoCode);
		}

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

		int cbActionRank[] = new int[3];
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			int cbUserActionRank = 0;
			int cbTargetActionRank = 0;

			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform()) + table.getTablePlayerNumber() - p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count, table._playerStatus[i]._action)
							+ table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform()) + target_p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					cbTargetActionRank = table._logic.get_action_list_rank(table._playerStatus[target_player]._action_count,
							table._playerStatus[target_player]._action) + target_p;
				}

				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i; // 最高级别人
					target_action = table._playerStatus[i].get_perform();
					target_p = table.getTablePlayerNumber() - p;
				}
			}
		}

		if (table._playerStatus[target_player].is_respone() == false) {
			table.log_error("最用户操作");
			return true;
		}

		// 判断可不可以吃的上家用户
		int last_player = (target_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
		boolean flag = false;
		for (int j = 0; j < table._playerStatus[last_player]._action_count; j++) {
			switch (table._playerStatus[last_player]._action[j]) {
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
			case GameConstants.WIK_XXD:
			case GameConstants.WIK_DDX:
			case GameConstants.WIK_EQS: {
				if (target_action == GameConstants.WIK_NULL)
					continue;

				if (flag == false) {
					if (table._playerStatus[last_player].get_exe_pass() == true) {
						table._cannot_chi[last_player][table._cannot_chi_count[last_player]--] = 0;
						flag = true;
						table._playerStatus[last_player].set_exe_pass(false);
					}
				}

				break;
			}
			}
		}

		int eat_type = GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT | GameConstants.WIK_DDX | GameConstants.WIK_XXD
				| GameConstants.WIK_EQS;

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
					case GameConstants.WIK_EQS: {
						if (!((target_action == GameConstants.WIK_PENG) || (target_action == GameConstants.WIK_ZI_MO)))
							continue;

						if (flag_temp == false) {
							if (table._playerStatus[i].get_exe_pass() == true) {
								table._cannot_chi[i][table._cannot_chi_count[i]--] = 0;
								flag_temp = true;
							}
						}

						break;
					}
					case GameConstants.WIK_PENG: {
						if (!((target_action == GameConstants.WIK_NULL) || (target_action & eat_type) != GameConstants.WIK_NULL))
							continue;

						if (table._playerStatus[i].get_exe_pass() == false) {
							table._cannot_peng[i][table._cannot_peng_count[i]++] = operate_card;
						}

						break;
					}
					}
				}
			}

			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		switch (target_action) {
		case GameConstants.WIK_NULL: {
			if (seat_index == _seat_index) {
				bankerOperaterCard(table);

				return true;
			}

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

					table.log_error(next_player + "可以胡，而不胡的情况 " + _seat_index);

					return true;
				}

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

		case GameConstants.WIK_ZI_MO: {
			table.GRR._chi_hu_rights[target_player].set_valid(true);

			table.GRR._chi_hu_card[target_player][0] = operate_card;

			table._cur_banker = target_player;

			if (table.has_rule(GameConstants.GAME_RULE_DI_ERZI_LIANG_PAI) == false)
				table.operate_player_get_card(_seat_index, 1, new int[] { table._send_card_data }, GameConstants.INVALID_SEAT, false);

			table._shang_zhuang_player = target_player;

			table.process_chi_hu_player_operate(target_player, operate_card, true);
			table.process_chi_hu_player_score_phz(target_player, _seat_index, operate_card, true);

			if (table.GRR._chi_hu_rights[target_player].da_hu_count > 0) {
				table._player_result.da_hu_zi_mo[target_player]++;
			} else {
				table._player_result.xiao_hu_zi_mo[target_player]++;
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
	public boolean handler_player_be_in_room(NingXiangHHTable table, int seat_index) {
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
