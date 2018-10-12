package com.cai.game.hongershi.hy;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.hongershi.HongErShiConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.util.GameDescUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.hh.handler.HHHandlerDispatchCard;

public class HandlerChuLiFirstCard extends HHHandlerDispatchCard<HongErShiTable_HY> {

	@Override
	public void exe(HongErShiTable_HY table) {

		int seatIndex = table._cur_banker;

		int count = 0;
		boolean has_tou = false;
		do {
			if (table.logic.countKingNumber(table.GRR._cards_data[seatIndex], table.GRR._card_count[seatIndex]) > 0) {
				table.touCards[seatIndex] = true;
				has_tou = true;
			}
			if (GameDescUtil.has_rule(table.gameRuleIndexEx, HongErShiConstants.RULE_7_IS_KING)
					&& table.logic.countSevenNumber(table.GRR._cards_data[seatIndex], table.GRR._card_count[seatIndex]) > 0) {
				table.touCards[seatIndex] = true;
				has_tou = true;
			}
			seatIndex = (seatIndex + 1) % table.getTablePlayerNumber();
			count++;
		} while (count < table.getTablePlayerNumber());

		boolean has_an_operate = false;
		// 没有人要偷牌 则开始检查暗杠暗碰
		int[][] cardsFour = new int[table.getTablePlayerNumber()][2]; // 暗杠
		int[][] cardsThree = new int[table.getTablePlayerNumber()][2]; // 暗碰
		int[][] countNumber = new int[table.getTablePlayerNumber()][2]; // 暗杠、暗碰数量
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			table.logic.checkLgThree(table.GRR._cards_data[p], table.GRR._card_count[p], cardsFour[p], cardsThree[p], countNumber[p]);
			if (countNumber[p][0] > 0 || countNumber[p][1] > 0) {
				has_an_operate = true;
			}
		}

		if (has_tou || has_an_operate) {
			table.exe_Handler_First_Operate(table._cur_banker);
			return;
		}

		if (table.estimate_player_bao_ting()) {
			table.exe_Handler_bao_ting(false);
			return;
		}

		ChiHuRight chr = new ChiHuRight();
		int hu = table.analyse_chi_hu_card(table._cur_banker, table._cur_banker, 0, HongErShiConstants.WIK_TIAN_HU, chr, true);

		table._game_status = GameConstants.GS_MJ_PLAY;
		table.refresh_game_status(true);
		if (hu != GameConstants.WIK_NULL) {
			table._playerStatus[table._cur_banker].add_action(HongErShiConstants.WIK_ZI_MO);
			table._playerStatus[table._cur_banker].add_zi_mo(_send_card_data, table._cur_banker);
			table._playerStatus[table._cur_banker].add_action(GameConstants.WIK_NULL);
			table._playerStatus[table._cur_banker].add_pass(this._send_card_data, table._cur_banker);

			if (table._playerStatus[table._cur_banker].has_action()) {
				table._playerStatus[table._cur_banker].set_status(GameConstants.Player_Status_OPR_CARD);//
				// 操作状态
				table.operate_player_action(table._cur_banker, false);
			}
		} else {
			table._playerStatus[table._cur_banker].set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
			table.operate_player_status();
		}

		return;
	}

	@SuppressWarnings("unused")
	@Override
	public boolean handler_operate_card(HongErShiTable_HY table, int seat_index, int operate_code, int operate_card, int luoCode) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_info("红二十 HandlerChuLiFirstCard 没有这个操作:" + operate_code);
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

		table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_card }, 1);

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

		int target_card = table._playerStatus[target_player]._operate_card;

		int last_player = (target_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
		boolean flag = false;

		switch (target_action) {
		case GameConstants.WIK_NULL: {
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
			curPlayerStatus.reset();

			curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
			return true;
		}
		case HongErShiConstants.WIK_ZI_MO: {
			table.GRR._chi_hu_rights[target_player].set_valid(true);

			table.GRR._chi_hu_card[target_player][0] = operate_card;

			table._cur_banker = target_player;

			table._shang_zhuang_player = target_player;

			table.process_chi_hu_player_operate(target_player, operate_card, true);

			if (target_player == _seat_index) {
				table.GRR._chi_hu_rights[table._cur_banker].opr_or(HongErShiConstants.WIK_TIAN_HU);
			}

			table.process_chi_hu_player_score_phz(target_player, _seat_index, operate_card, true);

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

		// 效验参数
		if (seat_index != table._cur_banker) {
			table.log_error("出牌,没到出牌");
			return false;
		}
		if (table._playerStatus[table._cur_banker].get_status() != GameConstants.Player_Status_OUT_CARD) {
			table.log_error("状态不对不能出牌");
			return false;
		}

		// 删除扑克
		int card_count = table.logic.remove_cards_by_cards(table.GRR._cards_data[table._cur_banker], table.GRR._card_count[table._cur_banker],
				new int[] { card }, 1);
		if (card_count == -1) {
			table.log_error("出牌删除出错");
			return false;
		} else {
			table.GRR._card_count[table._cur_banker] = card_count;
		}

		// 出牌
		table.exe_out_card(table._cur_banker, card, HongErShiConstants.WIK_DI_HU);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(HongErShiTable_HY table, int seat_index) {

		table.handler_player_be_in_room(table, seat_index);
		return true;
	}
}
