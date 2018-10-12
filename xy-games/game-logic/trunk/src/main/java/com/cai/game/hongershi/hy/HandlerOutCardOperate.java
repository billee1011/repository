package com.cai.game.hongershi.hy;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.hongershi.HongErShiConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.hh.handler.HHHandlerOutCardOperate;

public class HandlerOutCardOperate extends HHHandlerOutCardOperate<HongErShiTable_HY> {

	@Override
	public void exe(HongErShiTable_HY table) {
		table.isBegin = false;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}

		PlayerStatus playerStatus = table._playerStatus[_out_card_player];
		playerStatus.reset();

		table._out_card_count++;
		table._out_card_player = _out_card_player;
		table._out_card_data = _out_card_data;
		table._last_card = _out_card_data;

		int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
		table._current_player = next_player;

		table.operate_player_cards(_out_card_player, table.GRR._card_count[_out_card_player], table.GRR._cards_data[_out_card_player],
				table.GRR._weave_count[_out_card_player], table.GRR._weave_items[_out_card_player]);

		table.operate_out_card(_out_card_player, 1, new int[] { table._out_card_data }, GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);

		table._provide_player = _out_card_player;
		table._provide_card = table._out_card_data;

		// 判断有没有人接炮
		ChiHuRight chr[] = new ChiHuRight[table.getTablePlayerNumber()];
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			chr[i] = table.GRR._chi_hu_rights[i];
			chr[i].set_empty();
		}

		int action_hu[] = new int[table.getTablePlayerNumber()];

		int card_type = GameConstants.HU_CARD_TYPE_PAOHU;
		if (_type == HongErShiConstants.WIK_GANG_SHANG_PAO) {
			card_type = HongErShiConstants.WIK_GANG_SHANG_PAO;
		}
		int bHupai = 0;

		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_out_card_player + p) % table.getTablePlayerNumber();

			if (i == _out_card_player)
				continue;

			int hu_xi_chi[] = new int[1];
			hu_xi_chi[0] = 0;

			PlayerStatus tempPlayerStatus = table._playerStatus[i];
			tempPlayerStatus.reset();

			action_hu[i] = table.analyse_chi_hu_card(i, _out_card_player, _out_card_data, card_type, chr[i], true);

			if (action_hu[i] != GameConstants.WIK_NULL) {
				bHupai = 1;

				tempPlayerStatus.add_action(HongErShiConstants.WIK_CHI_HU);
				tempPlayerStatus.add_action(GameConstants.WIK_NULL);
				tempPlayerStatus.add_tou(table._out_card_data, HongErShiConstants.WIK_CHI_HU, i);

				// GameSchedule.put(new Runnable() {
				// @Override
				// public void run() {
				// table.handler_operate_card(i, HongErShiConstants.WIK_CHI_HU,
				// table._out_card_data, -1);
				// }
				// }, 1500, TimeUnit.MILLISECONDS);

			} else {
				chr[i].set_empty();
			}
		}

		table._playerStatus[_out_card_player]._hu_card_count = table.get_hh_ting_card_twenty(table._playerStatus[_out_card_player]._hu_cards,
				table.GRR._cards_index[_out_card_player], table.GRR._weave_items[_out_card_player], table.GRR._weave_count[_out_card_player],
				_out_card_player, _out_card_player);

		int ting_cards[] = table._playerStatus[_out_card_player]._hu_cards;
		int ting_count = table._playerStatus[_out_card_player]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(_out_card_player, ting_count, ting_cards);
		} else {
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(_out_card_player, 1, ting_cards);
		}

		boolean bAroseAction = table.estimate_player_card_respond(_out_card_player, table._out_card_data, false);

		if (bAroseAction == false && bHupai == 0) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i].clean_action();
				table._playerStatus[i].clean_status();
			}

			table.operate_player_action(_out_card_player, true);

			table._cannot_chi[next_player][table._cannot_chi_count[next_player]++] = table._out_card_data;

			// table.operate_out_card(_out_card_player, 0, null,
			// GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);

			int discard_time = 2000;
			int gameId = table.getGame_id() == 0 ? 5 : table.getGame_id();
			SysParamModel sysParamModel1105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1105);
			if (sysParamModel1105 != null && sysParamModel1105.getVal1() > 0 && sysParamModel1105.getVal1() < 10000) {
				discard_time = sysParamModel1105.getVal1();
			}
			table.exe_add_discard(_out_card_player, 1, new int[] { table._out_card_data }, true, discard_time);

			_current_player = table._current_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			// _out_card_data = 0; // 这样代码可以回引起重连的bug
			table._last_player = _current_player;

			int dispatch_time = 3000;
			if (sysParamModel1105 != null && sysParamModel1105.getVal2() > 0 && sysParamModel1105.getVal2() < 10000) {
				dispatch_time = sysParamModel1105.getVal2();
			}
			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, dispatch_time);
		} else {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];
				if (table._playerStatus[i].has_action()) {
					table._playerStatus[i].set_status(GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(i, false);
				}

			}
		}
	}

	@Override
	public boolean handler_operate_card(HongErShiTable_HY table, int seat_index, int operate_code, int operate_card, int luoCode) {
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
			// table.operate_out_card(_out_card_player, 0, null,
			// GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);
		}

		if (table._playerStatus[seat_index].has_action_by_code(HongErShiConstants.WIK_CHI_HU) && operate_code != HongErShiConstants.WIK_CHI_HU) {
			table._playerStatus[seat_index].add_cards_abandoned_hu(_out_card_data);
		}
		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();

		// 变量定义 优先级最高操作的玩家和操作--不通炮的算法
		int target_player = seat_index;
		int target_action = operate_code;
		int target_lou_code = luoCode;
		int target_p = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_out_card_player + p) % table.getTablePlayerNumber();
			if (i == target_player) {
				target_p = table.getTablePlayerNumber() - p;
			}
		}
		int cbActionRank[] = new int[table.getTablePlayerNumber()];
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {

			int i = (_out_card_player + p) % table.getTablePlayerNumber();
			int cbUserActionRank = 0;
			int cbTargetActionRank = 0;

			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					cbUserActionRank = table.logic.get_action_rank(table._playerStatus[i].get_perform()) + table.getTablePlayerNumber() - p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					cbUserActionRank = table.logic.get_action_list_rank(table._playerStatus[i]._action_count, table._playerStatus[i]._action)
							+ table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					cbTargetActionRank = table.logic.get_action_rank(table._playerStatus[target_player].get_perform()) + target_p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					cbTargetActionRank = table.logic.get_action_list_rank(table._playerStatus[target_player]._action_count,
							table._playerStatus[target_player]._action) + target_p;
				}

				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;
					target_action = table._playerStatus[i].get_perform();
					target_lou_code = table._playerStatus[i].get_lou_kind();
					target_p = table.getTablePlayerNumber() - p;
				}
			}
		}

		if (table._playerStatus[target_player].is_respone() == false) {
			table.log_info("优先级最高的人还没操作");
			return true;
		}

		int target_card = table._playerStatus[target_player]._operate_card;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		table.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 5,
				GameConstants.INVALID_SEAT);

		switch (target_action) {
		case GameConstants.WIK_NULL: {
			// table.operate_out_card(_out_card_player, 0, null,
			// GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);
			table.operate_player_get_card(_out_card_player, 0, null, GameConstants.INVALID_SEAT, false);

			int discard_time = 2000;
			int gameId = table.getGame_id() == 0 ? 5 : table.getGame_id();
			SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1104);
			if (sysParamModel1104 != null && sysParamModel1104.getVal4() > 0 && sysParamModel1104.getVal4() < 10000) {
				discard_time = sysParamModel1104.getVal4();
			}

			if (table._last_card != 0)
				table.exe_add_discard(_out_card_player, 1, new int[] { table._last_card }, true, discard_time);

			int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			table._cannot_chi[_out_card_player][table._cannot_chi_count[_out_card_player]++] = table._send_card_data;
			table._cannot_chi[next_player][table._cannot_chi_count[next_player]++] = table._send_card_data;

			table._current_player = next_player;
			_out_card_player = next_player;

			int dispatch_time = 3000;
			if (sysParamModel1104 != null && sysParamModel1104.getVal5() > 0 && sysParamModel1104.getVal5() < 10000) {
				dispatch_time = sysParamModel1104.getVal5();
			}
			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, dispatch_time);

			table._last_card = table._send_card_data;
			table._last_player = table._current_player;

			return true;
		}
		case HongErShiConstants.WIK_CHI: {
			int[] cbRemoveCard = new int[] { target_card };
			int count = table.logic.remove_cards_by_cards(table.GRR._cards_data[target_player], table.GRR._card_count[target_player], cbRemoveCard,
					1);
			if (count == -1) {
				table.log_player_error(target_player, "吃牌删除出错");
				return false;
			} else {
				table.GRR._card_count[target_player] = count;
			}

			table.exe_chi_peng(target_player, _out_card_player, target_action, target_card, GameConstants.CHI_PENG_TYPE_DISPATCH,
					new int[] { target_card, _out_card_data });
			return true;
		}
		case HongErShiConstants.WIK_PENG:
		case HongErShiConstants.WIK_AN_PENG: {
			int[] remove_cards = new int[3];
			int count = 0;

			for (int c = 0; c < table.GRR._card_count[target_player]; c++) {
				if (table.logic.get_card_value(table.GRR._cards_data[target_player][c]) == table.logic.get_card_value(target_card)) {
					remove_cards[count++] = table.GRR._cards_data[target_player][c];
				}

				if (count == 2) {
					break;
				}
			}

			int card_count = table.logic.remove_cards_by_cards(table.GRR._cards_data[target_player], table.GRR._card_count[target_player],
					remove_cards, 3);

			remove_cards[count++] = target_card;
			if (card_count == -1) {
				table.log_player_error(target_player, "碰牌删除出错");
				return false;
			} else {
				table.GRR._card_count[target_player] = card_count;
			}

			table.exe_chi_peng(target_player, _out_card_player, target_action, target_card, GameConstants.CHI_PENG_TYPE_DISPATCH, remove_cards);
			return true;
		}
		case HongErShiConstants.WIK_GANG: {
			for (int i = 0; i < table.m_gangCardResult[target_player].cbCardCount; i++) {
				if (operate_card == table.m_gangCardResult[target_player].cbCardData[i]) {
					table.exe_gang(target_player, _out_card_player, operate_card, target_action, table.m_gangCardResult[target_player].type[i], true,
							true, false, 1000);
				}
			}

			return true;
		}
		case HongErShiConstants.WIK_CHI_HU: {
			if (_type == HongErShiConstants.WIK_DI_HU) {
				table.GRR._chi_hu_rights[target_player].opr_or(HongErShiConstants.WIK_DI_HU);
			}

			table.GRR._chi_hu_rights[target_player].set_valid(true);

			table.GRR._chi_hu_rights[target_player].opr_or(HongErShiConstants.WIK_ZHUA_PAO);
			table.GRR._chi_hu_card[target_player][0] = operate_card;

			table._cur_banker = target_player;

			table._shang_zhuang_player = target_player;

			// table.operate_out_card(this._out_card_player, 0, null,
			// GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);

			table.process_chi_hu_player_operate(target_player, operate_card, true);

			table.process_chi_hu_player_score_phz(target_player, _out_card_player, operate_card, false);

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
	public boolean handler_player_be_in_room(HongErShiTable_HY table, int seat_index) {

		table.handler_player_be_in_room(table, seat_index);
		return true;
	}
}
