package com.cai.game.phz.handler.yjghz;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.GameConstants_YJGHZ;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.game.phz.handler.PHZHandlerChiPeng;

public class WHZHandlerChiPeng_YuanJiang extends PHZHandlerChiPeng<YuanJiangGHZTable> {

	private GangCardResult m_gangCardResult;

	public WHZHandlerChiPeng_YuanJiang() {
		m_gangCardResult = new GangCardResult();
	}

	@Override
	public void exe(YuanJiangGHZTable table) {
		// 组合扑克
		int wIndex = table.GRR._weave_count[_seat_index]++;
		table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
		table.GRR._weave_items[_seat_index][wIndex].center_card = _card;
		table.GRR._weave_items[_seat_index][wIndex].weave_kind = _action;

		table.GRR._weave_items[_seat_index][wIndex].provide_player = _provider;
		table.GRR._weave_items[_seat_index][wIndex].hu_xi = table._logic.get_weave_hu_xi_yjghz(table.GRR._weave_items[_seat_index][wIndex]);
		int cbMingIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		table._logic.ming_index_temp(cbMingIndexTemp, table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], false, 0);
		table._hu_xi[_seat_index] = table._logic.get_all_hu_xi_weave(table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],
				cbMingIndexTemp);
		// 设置用户
		table._current_player = _seat_index;

		// 效果
		int eat_type = GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT | GameConstants.WIK_DDX | GameConstants.WIK_XXD
				| GameConstants.WIK_EQS | GameConstants.WIK_YWS;

		if (_lou_card == -1 || (eat_type & _action) == 0)
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
					GameConstants.INVALID_SEAT);
		else
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_LOU }, 1,
					GameConstants.INVALID_SEAT);
		if (_type == GameConstants.CHI_PENG_TYPE_OUT_CARD) {
			table.operate_out_card(this._provider, 0, null, GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);
		}
		if (_type == GameConstants.CHI_PENG_TYPE_DISPATCH) {
			table.operate_player_get_card(table._last_player, 0, null, GameConstants.INVALID_SEAT, false);
		}

		// 刷新手牌包括组合
		int cards[] = new int[GameConstants.MAX_YYWHZ_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();
		boolean is_liu = false;
		is_liu = table.estimate_player_chipeng_qing_piao_respond(_seat_index, _seat_index, _card);

		if (is_liu) {
			table.operate_player_action(_seat_index, false);
			curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
			table.operate_player_status();
		} else if (table.is_can_out_card(_seat_index)) {
			curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
			table.operate_player_status();
		} else {
			table._is_xiang_gong[_seat_index] = true;
			table.operate_player_xiang_gong_flag(_seat_index, table._is_xiang_gong[_seat_index]);
			int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			table._current_player = next_player;
			table._last_player = next_player;

			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);
		}

	}

	/***
	 * //用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @param luoCode
	 * @return
	 */
	@Override
	public boolean handler_operate_card(YuanJiangGHZTable table, int seat_index, int operate_code, int operate_card, int lou_pai) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		// 效验操作
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("HHHandlerChiPeng_YX 没有这个操作:" + operate_code);
			return false;
		}
		if (seat_index != _seat_index) {
			table.log_error("HHHandlerChiPeng_YX 不是当前玩家操作");
			return false;
		}
		table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1);
		// 放弃操作
		if (operate_code == GameConstants.WIK_NULL) {
			int next_player = _seat_index;
			table._current_player = next_player;
			table._last_player = next_player;

			PlayerStatus curPlayerStatus = table._playerStatus[next_player];
			curPlayerStatus.reset();
			if (table.is_can_out_card(_seat_index)) {
				table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
				table.operate_player_action(_seat_index, false);
			} else {
				next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
				// 用户状态
				table._playerStatus[_seat_index].clean_action();
				table._playerStatus[_seat_index].clean_status();
				table._current_player = next_player;
				table._last_player = next_player;

				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);
			}

			return true;
		}

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
		int cbActionRank[] = new int[3];
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
					cbUserActionRank = table.get_action_rank(table._playerStatus[i].get_perform(), i, _seat_index) + table.getTablePlayerNumber() - p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbUserActionRank = table.get_action_list_rank(table._playerStatus[i]._action_count, table._playerStatus[i]._action, i,
							_seat_index) + table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					// 获取已经执行的动作的优先级
					cbTargetActionRank = table.get_action_rank(table._playerStatus[target_player].get_perform(), target_player, _seat_index)
							+ target_p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbTargetActionRank = table.get_action_list_rank(table._playerStatus[target_player]._action_count,
							table._playerStatus[target_player]._action, target_player, _seat_index) + target_p;
				}

				// 优先级别
				// 动作判断 优先级最高的人和动作
				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;// 最高级别人
					target_action = table._playerStatus[i].get_perform();
					target_p = table.getTablePlayerNumber() - p;
					cbMaxActionRand = cbUserActionRank;
				}
			}
		}

		// 优先级最高的人还没操作
		if (table._playerStatus[target_player].is_respone() == false) {
			table.log_error("优先级最高的人还没操作");
			return true;
		}
		// 执行动作
		switch (target_action) {
		case GameConstants_YJGHZ.WIK_QING_NEI: // 清牌
		case GameConstants_YJGHZ.WIK_QING_WAI: // 清牌
		{
			table.exe_liu(target_player, _seat_index, target_action, operate_card, GameConstants.CHI_PENG_TYPE_DISPATCH, 0);
			return true;
		}
		case GameConstants.WIK_YIYANGWHZ_PIAO: {
			table.exe_piao(target_player, _seat_index, target_action, operate_card, GameConstants.CHI_PENG_TYPE_DISPATCH, 0);
			return true;
		}
		}
		return true;
	}

	@Override
	public boolean handler_player_be_in_room(YuanJiangGHZTable table, int seat_index) {
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
