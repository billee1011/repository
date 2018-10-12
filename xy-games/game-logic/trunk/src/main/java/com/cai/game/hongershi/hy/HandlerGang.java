package com.cai.game.hongershi.hy;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.GameConstants_KWX;
import com.cai.common.constant.game.hongershi.HongErShiConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.hh.handler.HHHandlerGang;

public class HandlerGang extends HHHandlerGang<HongErShiTable_HY> {

	@Override
	public void exe(HongErShiTable_HY table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action()) {
				table.operate_player_action(i, true);
			}

			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();

		table.operate_player_get_card(this._provide_player, 0, null, GameConstants.INVALID_SEAT, false);
		table.operate_out_card(this._provide_player, 0, null, GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);

		if (_type == GameConstants.GANG_TYPE_ADD_GANG) {

			boolean bAroseAction = table.estimate_gang_respond(_seat_index, _center_card);

			if (bAroseAction == false) {
				this.exe_gang(table);
			} else {
				PlayerStatus playerStatus = null;

				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					playerStatus = table._playerStatus[i];
					if (playerStatus.has_action_by_code(HongErShiConstants.WIK_CHI_HU)) {
						table.change_player_status(i, GameConstants_KWX.Player_Status_OPR_CARD);
						table.operate_player_action(i, false);
					}
				}
			}

		} else {
			this.exe_gang(table);
		}
	}

	@SuppressWarnings("unused")
	@Override
	protected boolean exe_gang(HongErShiTable_HY table) {
		int cbCardIndex = table._logic.switch_to_card_index(_center_card);
		int cbWeaveIndex = -1;

		if (GameConstants.GANG_TYPE_AN_GANG == _type || HongErShiConstants.WIK_AN_PENG == _type) {
			// 暗杠
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;
		} else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
				if (table.logic.get_card_value(cbCenterCard) == table.logic.get_card_value(_center_card)
						&& cbWeaveKind == HongErShiConstants.WIK_AN_PENG) {
					cbWeaveIndex = i;// 第几个组合可以碰
					_provide_player = _seat_index;
					break;
				}
			}
			if (cbWeaveIndex == -1) {
				// 别人打的牌
				cbWeaveIndex = table.GRR._weave_count[_seat_index];
				table.GRR._weave_count[_seat_index]++;
			}
			table.operate_remove_discard(this._provide_player, table.GRR._discard_count[_provide_player]);
		} else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
			// 看看是不是有碰的牌，明杠
			// 寻找组合
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
				if (table.logic.get_card_value(cbCenterCard) == table.logic.get_card_value(_center_card)
						&& (cbWeaveKind == HongErShiConstants.WIK_AN_PENG || cbWeaveKind == HongErShiConstants.WIK_PENG)) {
					cbWeaveIndex = i;// 第几个组合可以碰
					_provide_player = _seat_index;
					break;
				}
			}

			if (cbWeaveIndex == -1) {
				table.log_player_error(_seat_index, "杠牌出错");
				return false;
			}
		}

		int[] weave_cards = new int[4];
		for (int i = 0; i < 4; i++) {
			weave_cards[i] = ((i & GameConstants.LOGIC_MASK_VALUE) << 4) | table.logic.get_card_value(_center_card);
		}
		table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = _p == true ? 1 : 0;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _center_card;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _type;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_card = weave_cards;

		// 设置用户
		table._current_player = _seat_index;

		// 删除手上的牌
		table.GRR._card_count[_seat_index] = table.logic.remove_card_by_card_value(table.GRR._cards_data[_seat_index],
				table.GRR._card_count[_seat_index], _center_card, new int[4]);

		WeaveItem weaves[] = new WeaveItem[HongErShiConstants.MAX_WEAVE];
		int weave_count = table.GRR._weave_count[_seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
			weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
			weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
			weaves[i].weave_card = table.GRR._weave_items[_seat_index][i].weave_card;
			weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player + GameConstants.WEAVE_SHOW_DIRECT;
		}

		table.operate_player_cards(_seat_index, table.GRR._card_count[_seat_index], table.GRR._cards_data[_seat_index], weave_count, weaves);

		table.exe_dispatch_card(_seat_index, _type, 0);
		return true;
	}

	@SuppressWarnings("unused")
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
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);
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
					cbUserActionRank = table.logic.get_action_rank(table._playerStatus[i].get_perform()) + table.getTablePlayerNumber() - p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbUserActionRank = table.logic.get_action_list_rank(table._playerStatus[i]._action_count, table._playerStatus[i]._action)
							+ table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					// 获取已经执行的动作的优先级
					cbTargetActionRank = table.logic.get_action_rank(table._playerStatus[target_player].get_perform()) + target_p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbTargetActionRank = table.logic.get_action_list_rank(table._playerStatus[target_player]._action_count,
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
			if ((table._is_xiang_gong[_seat_index] == false) && (table._long_count[_seat_index] == 1 || GameConstants.SAO_TYPE_MINE_SAO == _type)) {
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

				table._playerStatus[_seat_index].clean_action();
				table._playerStatus[_seat_index].clean_status();
				int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
				table._current_player = next_player;
				table._last_player = next_player;

				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1500);
			}

			return true;
		}
		case HongErShiConstants.WIK_CHI_HU:
		case HongErShiConstants.WIK_ZI_MO: { // 偎提跑之后胡牌
			table.GRR._chi_hu_rights[target_player].set_valid(true);

			table.GRR._chi_hu_card[target_player][0] = operate_card;

			table._cur_banker = target_player;

			// if (_depatch == true)
			// table.operate_player_get_card(this._provide_player, 1, new int[]
			// { _center_card },
			// GameConstants.INVALID_SEAT, false);

			table._shang_zhuang_player = _seat_index;

			table.GRR._chi_hu_rights[target_player].opr_or(HongErShiConstants.WIK_QIANG_GANG);
			table.process_chi_hu_player_operate(target_player, operate_card, true);

			if (target_action == HongErShiConstants.WIK_ZI_MO) {
				table.process_chi_hu_player_score_phz(target_player, _seat_index, operate_card, true);
				table._player_result.zi_mo_count[target_player]++;
			} else if (target_action == HongErShiConstants.WIK_CHI_HU) { // 所有的非点炮的胡牌分计算都按自摸算，不然还是得重写一下这个方法
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

	/***
	 * //用户出牌
	 */
	@Override
	public boolean handler_player_out_card(HongErShiTable_HY table, int seat_index, int card) {
		// 错误断言
		card = table.get_real_card(card);

		if (table.logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		if (table._playerStatus[seat_index].get_status() != GameConstants.Player_Status_OUT_CARD) {
			table.log_error("状态不对不能出牌");
			return false;
		}

		// 删除扑克
		int card_count = table.logic.remove_cards_by_cards(table.GRR._cards_data[seat_index], table.GRR._card_count[seat_index], new int[] { card },
				1);
		if (card_count == -1) {
			table.log_error("出牌删除出错");
			return false;
		} else {
			table.GRR._card_count[seat_index] = card_count;
		}

		// 出牌
		table.exe_out_card(seat_index, card, GameConstants.WIK_NULL);

		return true;
	}
}
