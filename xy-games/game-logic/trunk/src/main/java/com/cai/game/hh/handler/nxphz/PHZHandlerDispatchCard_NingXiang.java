package com.cai.game.hh.handler.nxphz;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.mj.GameConstants_XiangXiang;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.hh.handler.HHHandlerDispatchCard;

public class PHZHandlerDispatchCard_NingXiang extends HHHandlerDispatchCard<NingXiangHHTable> {

	@SuppressWarnings("static-access")
	@Override
	public void exe(NingXiangHHTable table) {
		NingXiangPHZUtils.cleanPlayerStatus(table);

		if (NingXiangPHZUtils.endHuangZhuang(table, true)) {
			return;
		}

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

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

			int cards[] = new int[GameConstants.MAX_HH_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
		}

		if (an_long_count >= 2) {
			table._ti_two_long[_seat_index] = true;
		}

		NingXiangPHZUtils.ting_basic(table, _seat_index);

		table._current_player = _seat_index;

		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		if (table.DEBUG_CARDS_MODE) {
			_send_card_data = 0x17;
		}

		--table.GRR._left_card_count;
		table._last_card = _send_card_data;

		table.dispatch_card_count++;

		int card_type = GameConstants.HU_CARD_TYPE_ZIMO;

		int ti_sao = table.estimate_player_ti_wei_respond_phz(_seat_index, _send_card_data);
		if (ti_sao != GameConstants.WIK_NULL) {
			if (ti_sao == GameConstants.WIK_WEI) {
				boolean isChou = false;

				for (int i = 0; i < table._cannot_peng_count[_seat_index]; i++) {
					if (_send_card_data == table._cannot_peng[_seat_index][i]) {
						isChou = true;
					}
				}

				table.operate_player_get_card(_seat_index, 1, new int[] { isChou ? _send_card_data : _send_card_data | 0x100 },
						GameConstants.INVALID_SEAT, false);
			} else {
				table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT, false);
			}

			return;
		}

		ChiHuRight chr[] = new ChiHuRight[table.getTablePlayerNumber()];
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			chr[i] = table.GRR._chi_hu_rights[i];
			chr[i].set_empty();
		}

		int bHupai = 0;
		int action_hu[] = new int[3];
		int action_pao[] = new int[3];
		int pao_type[][] = new int[3][1];

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			int hu_xi_chi[] = new int[1];

			hu_xi_chi[0] = 0;

			PlayerStatus tempPlayerStatus = table._playerStatus[i];
			tempPlayerStatus.reset();

			action_hu[i] = table.analyse_chi_hu_card(table.GRR._cards_index[i], table.GRR._weave_items[i], table.GRR._weave_count[i], i, _seat_index,
					_send_card_data, chr[i], card_type, hu_xi_chi, true);// 自摸

			action_pao[i] = table.estimate_player_respond_phz(i, _seat_index, _send_card_data, pao_type[i], true);

			if (table._is_xiang_gong[i] == true)
				action_hu[i] = GameConstants.WIK_NULL;

			if (action_hu[i] != GameConstants.WIK_NULL) {
				tempPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
				tempPlayerStatus.add_zi_mo(_send_card_data, i);

				if (action_pao[i] != GameConstants.WIK_PAO) {
					tempPlayerStatus.add_action(GameConstants.WIK_NULL);
					tempPlayerStatus.add_pass(_send_card_data, _seat_index);
				} else {
					tempPlayerStatus.add_action(GameConstants.WIK_PAO);
					tempPlayerStatus.add_pao(_send_card_data, _seat_index);
				}

				if (bHupai == 0)
					table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT, false);

				ti_sao = GameConstants.WIK_ZI_MO;

				bHupai = 1;
			} else {
				chr[i].set_empty();
			}
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((action_pao[i] != GameConstants.WIK_NULL) && (bHupai == 0)) {
				ti_sao = GameConstants.WIK_PAO;

				table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT, false);

				table.exe_gang(i, _seat_index, _send_card_data, action_pao[i], pao_type[i][0], true, true, false, table.time_for_operate_dragon);

				return;
			} else if (action_pao[i] != GameConstants.WIK_NULL) {
				ti_sao = GameConstants.WIK_PAO;
			}
		}

		boolean bAroseAction = false;
		if (ti_sao != GameConstants.WIK_PAO) {
			bAroseAction = table.estimate_player_out_card_respond_hh(_seat_index, _send_card_data, true);

			if (bHupai == 0) {
				table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT, false);
			}
		}

		if ((bAroseAction == false) && (ti_sao == GameConstants.WIK_NULL)) {
			table.operate_player_action(_seat_index, true);
		} else {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				curPlayerStatus = table._playerStatus[i];

				if (table._playerStatus[i].has_action()) {
					table._playerStatus[i].set_status(GameConstants.Player_Status_OPR_CARD);//
					table.operate_player_action(i, false);
				}
			}
		}

		if (curPlayerStatus.has_action()) {
			if (table.isTrutess(_seat_index)) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS,
						TimeUnit.MILLISECONDS);

				return;
			}

			curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);

			table.operate_player_action(_seat_index, false);

			table.log_info(_seat_index + "操作状态" + bAroseAction);
		} else {
			if (table.isTrutess(_seat_index)) {

				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS,
						TimeUnit.MILLISECONDS);
				return;
			}

			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), GameConstants.DELAY_AUTO_OUT_CARD,
						TimeUnit.MILLISECONDS);
			} else if (ti_sao == GameConstants.WIK_NULL) {
				if (bAroseAction == false) {
					table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT, false);

					if (table._last_card != 0)
						table.exe_add_discard(_seat_index, 1, new int[] { table._last_card }, true, table.time_for_add_discard);

					int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();

					table._cannot_chi[_seat_index][table._cannot_chi_count[_seat_index]++] = _send_card_data;
					table._cannot_chi[next_player][table._cannot_chi_count[next_player]++] = _send_card_data;

					table._current_player = next_player;
					_seat_index = next_player;

					table.log_info(_seat_index + "  " + table._current_player + "  " + "下次 出牌用户");

					table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, table.time_for_dispatch_card);

					table._last_card = _send_card_data;
					table._last_player = table._current_player;

					table.log_info(next_player + "发牌" + bAroseAction);
				}
			}
		}

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

		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "DispatchCard 出牌,玩家操作已失效");
			return true;
		}

		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "DispatchCard 出牌,玩家已操作");
			return true;
		}

		if (operate_card != _send_card_data) {
			table.log_player_error(seat_index, "DispatchCard 操作牌，与当前牌不一样");
			return true;
		}

		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();

		switch (operate_code) {
		case GameConstants.WIK_LEFT:
		case GameConstants.WIK_CENTER:
		case GameConstants.WIK_RIGHT:
		case GameConstants.WIK_XXD:
		case GameConstants.WIK_DDX:
		case GameConstants.WIK_EQS: {
			if (luoCode != -1) {
				playerStatus.set_lou_pai_kind(luoCode);
			}
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
					target_lou_code = table._playerStatus[i].get_lou_kind();
					target_p = table.getTablePlayerNumber() - p;
				}
			}
		}

		if (table._playerStatus[target_player].is_respone() == false) {
			table.log_error("最用户操作");
			return true;
		}

		int target_card = table._playerStatus[target_player]._operate_card;
		int eat_type = GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT | GameConstants.WIK_DDX | GameConstants.WIK_XXD
				| GameConstants.WIK_EQS;

		if (target_action == GameConstants.WIK_NULL) {
			int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();

			table._cannot_chi[_seat_index][table._cannot_chi_count[_seat_index]++] = _send_card_data;
			table._cannot_chi[next_player][table._cannot_chi_count[next_player]++] = _send_card_data;

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				for (int j = 0; j < table._playerStatus[i]._action_count; j++) {
					if (table._playerStatus[i]._action[j] == GameConstants.WIK_PENG) {
						table._cannot_peng[i][table._cannot_peng_count[i]++] = _send_card_data;
					}
				}
			}
		} else if ((target_action & eat_type) != GameConstants_XiangXiang.WIK_NULL) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				for (int j = 0; j < table._playerStatus[i]._action_count; j++) {
					if (table._playerStatus[i]._action[j] == GameConstants.WIK_PENG) {
						table._cannot_peng[i][table._cannot_peng_count[i]++] = _send_card_data;
					}
				}
			}

			if (_seat_index != target_player)
				table._cannot_chi[_seat_index][table._cannot_chi_count[_seat_index]++] = _send_card_data;
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		switch (target_action) {
		case GameConstants.WIK_NULL: {
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					int cards[] = new int[GameConstants.MAX_HH_COUNT];
					int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], cards);

					table.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, table.GRR._weave_items[i],
							table.GRR._weave_count[i], GameConstants.INVALID_SEAT);
				}

				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), GameConstants.DELAY_AUTO_OUT_CARD,
						TimeUnit.MILLISECONDS);
			} else {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					int pao_type[] = new int[1];

					int action = table.estimate_player_respond_phz_chd(i, _seat_index, _send_card_data, pao_type, true);
					if (action != GameConstants.WIK_NULL) {
						table.exe_gang(i, _seat_index, _send_card_data, action, pao_type[0], true, true, false, table.time_for_operate_dragon);
						return true;
					}
				}

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
					table.log_error(next_player + "可以胡，而不胡的情况" + _seat_index);
					return true;
				}

				int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
				int ting_count = table._playerStatus[_seat_index]._hu_card_count;

				if (ting_count > 0) {
					table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
				} else {
					ting_cards[0] = 0;
					table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
				}

				table.exe_add_discard(_seat_index, 1, new int[] { _send_card_data }, true, table.time_for_add_discard);

				int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
				table._current_player = next_player;
				_seat_index = next_player;

				table._last_player = next_player;

				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, table.time_for_dispatch_card);

				table._last_card = _send_card_data;

				table.log_error(next_player + "发牌" + _seat_index + "  " + next_player);
			}
			return true;

		}
		case GameConstants.WIK_LEFT: {
			int cbRemoveCard[] = new int[] { target_card + 1, target_card + 2 };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(target_player, "吃牌删除出错");
				return false;
			}

			table.add_lou_weave(target_lou_code, target_player, target_card, _seat_index, table._lou_weave_item[target_player][0]);

			table.exe_chi_peng(target_player, _seat_index, target_action, target_card, GameConstants.CHI_PENG_TYPE_DISPATCH, target_lou_code);
			return true;
		}
		case GameConstants.WIK_RIGHT: {
			int cbRemoveCard[] = new int[] { target_card - 1, target_card - 2 };

			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}

			table.add_lou_weave(target_lou_code, target_player, target_card, _seat_index, table._lou_weave_item[target_player][2]);

			table.exe_chi_peng(target_player, _seat_index, target_action, target_card, GameConstants.CHI_PENG_TYPE_DISPATCH, target_lou_code);
			return true;
		}
		case GameConstants.WIK_CENTER: {
			int cbRemoveCard[] = new int[] { target_card - 1, target_card + 1 };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}

			table.add_lou_weave(target_lou_code, target_player, target_card, _seat_index, table._lou_weave_item[target_player][1]);

			table.exe_chi_peng(target_player, _seat_index, target_action, target_card, GameConstants.CHI_PENG_TYPE_DISPATCH, target_lou_code);
			return true;
		}
		case GameConstants.WIK_XXD: {
			int target_card_color = table._logic.get_card_color(target_card);

			int cbRemoveCard[] = new int[2];
			if (target_card_color == 0) {
				cbRemoveCard[0] = target_card;
				cbRemoveCard[1] = target_card + 16;
			} else {
				cbRemoveCard[0] = target_card - 16;
				cbRemoveCard[1] = target_card - 16;
			}

			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}

			table.add_lou_weave(target_lou_code, target_player, target_card, _seat_index, table._lou_weave_item[target_player][4]);

			table.exe_chi_peng(target_player, _seat_index, target_action, target_card, GameConstants.CHI_PENG_TYPE_DISPATCH, target_lou_code);

			return true;
		}
		case GameConstants.WIK_DDX: {
			int target_card_color = table._logic.get_card_color(target_card);

			int cbRemoveCard[] = new int[2];
			if (target_card_color == 0) {
				cbRemoveCard[0] = target_card + 16;
				cbRemoveCard[1] = target_card + 16;
			} else {
				cbRemoveCard[0] = target_card - 16;
				cbRemoveCard[1] = target_card;
			}

			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}

			table.add_lou_weave(target_lou_code, target_player, target_card, _seat_index, table._lou_weave_item[target_player][5]);

			table.exe_chi_peng(target_player, _seat_index, target_action, target_card, GameConstants.CHI_PENG_TYPE_DISPATCH, target_lou_code);

			return true;
		}
		case GameConstants.WIK_EQS: {
			int cbRemoveCard[] = new int[] { target_card, target_card };

			int target_card_value = table._logic.get_card_value(target_card);
			switch (target_card_value) {
			case 2:
				cbRemoveCard[0] = target_card + 5;
				cbRemoveCard[1] = target_card + 8;
				break;
			case 7:
				cbRemoveCard[0] = target_card - 5;
				cbRemoveCard[1] = target_card + 3;
				break;
			case 10:
				cbRemoveCard[0] = target_card - 8;
				cbRemoveCard[1] = target_card - 3;
				break;
			default:
				break;
			}

			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}

			table.add_lou_weave(target_lou_code, target_player, target_card, _seat_index, table._lou_weave_item[target_player][3]);

			table.exe_chi_peng(target_player, _seat_index, target_action, target_card, GameConstants.CHI_PENG_TYPE_DISPATCH, target_lou_code);

			return true;
		}
		case GameConstants.WIK_PENG: {
			int cbRemoveCard[] = new int[] { target_card, target_card };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "碰牌删除出错");
				return false;
			}

			table.exe_chi_peng(target_player, _seat_index, target_action, target_card, GameConstants.CHI_PENG_TYPE_DISPATCH, target_lou_code);

			return true;
		}
		case GameConstants.WIK_PAO: {
			int pao_type[] = new int[1];
			int action = table.estimate_player_respond_phz(target_player, _seat_index, _send_card_data, pao_type, true);
			if (action != GameConstants.WIK_NULL) {
				table.exe_gang(target_player, _seat_index, _send_card_data, action, pao_type[0], true, true, false, table.time_for_operate_dragon);
			}

			return true;
		}
		case GameConstants.WIK_ZI_MO: {
			table.GRR._chi_hu_rights[target_player].set_valid(true);

			table.GRR._chi_hu_card[target_player][0] = operate_card;

			table._cur_banker = target_player;

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
