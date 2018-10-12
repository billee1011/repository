package com.cai.game.hh.handler.hcphz;

import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.game.hh.handler.HHHandlerChiPeng;

public class PHZHandlerChiPeng_HeChi extends HHHandlerChiPeng<HeChiHHTable> {

	private GangCardResult m_gangCardResult;

	public PHZHandlerChiPeng_HeChi() {
		m_gangCardResult = new GangCardResult();
	}

	@Override
	public void exe(HeChiHHTable table) {
		// 组合扑克
		int wIndex = table.GRR._weave_count[_seat_index]++;
		table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
		table.GRR._weave_items[_seat_index][wIndex].center_card = _card;
		table.GRR._weave_items[_seat_index][wIndex].weave_kind = _action;

		table.GRR._weave_items[_seat_index][wIndex].provide_player = _provider;
		table.GRR._weave_items[_seat_index][wIndex].hu_xi = table.get_weave_hu_xi(table.GRR._weave_items[_seat_index][wIndex]);
		// 设置用户
		table._current_player = _seat_index;

		// 效果
		int eat_type = GameConstants_HeChi.WIK_LEFT | GameConstants_HeChi.WIK_CENTER | GameConstants_HeChi.WIK_RIGHT | GameConstants_HeChi.WIK_DDX
				| GameConstants_HeChi.WIK_XXD | GameConstants_HeChi.WIK_EQS | GameConstants_HeChi.WIK_YWS;

		if (_lou_card == -1 || (eat_type & _action) == 0)
			table.operate_effect_action(_seat_index, GameConstants_HeChi.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
					GameConstants_HeChi.INVALID_SEAT);
		else
			table.operate_effect_action(_seat_index, GameConstants_HeChi.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants_HeChi.WIK_LOU }, 1,
					GameConstants_HeChi.INVALID_SEAT);
		if (_type == GameConstants_HeChi.CHI_PENG_TYPE_OUT_CARD) {
			// 删掉出来的那张牌
			// table.operate_out_card(this._provider, 0,
			// null,MJGameConstants_HeChi.OUT_CARD_TYPE_MID,MJGameConstants_HeChi.INVALID_SEAT);
			// table.operate_remove_discard(this._provider,
			// table.GRR._discard_count[_provider]);
			table.operate_out_card(this._provider, 0, null, GameConstants_HeChi.OUT_CARD_TYPE_MID, GameConstants_HeChi.INVALID_SEAT);
		}
		if (_type == GameConstants_HeChi.CHI_PENG_TYPE_DISPATCH) {
			table.log_info(table._last_player + "CHI_PENG_TYPE_DISPATCH");
			table.operate_player_get_card(table._last_player, 0, null, GameConstants_HeChi.INVALID_SEAT, false);
		}

		// 刷新手牌包括组合
		int cards[] = new int[GameConstants_HeChi.MAX_HH_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		int an_long_Index[] = new int[5];
		int an_long_count = 0;
		//// 玩家出牌 响应判断,是否有提 暗龙
		for (int i = 0; i < GameConstants_HeChi.MAX_HH_INDEX; i++) {
			if (table.GRR._cards_index[_seat_index][i] == 4) {
				an_long_Index[an_long_count++] = i;
			}
		}
		if (an_long_count > 0) {
			int _action = GameConstants_HeChi.WIK_AN_LONG;
			// 效果
			table.operate_effect_action(_seat_index, GameConstants_HeChi.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
					GameConstants_HeChi.INVALID_SEAT);

			for (int i = 0; i < an_long_count; i++) {
				int cbWeaveIndex = table.GRR._weave_count[_seat_index];
				table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = 1;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = table._logic.switch_to_card_data(an_long_Index[i]);
				table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = GameConstants_HeChi.WIK_AN_LONG;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _seat_index;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].hu_xi = table.get_weave_hu_xi(table.GRR._weave_items[_seat_index][cbWeaveIndex]);
				table.GRR._weave_count[_seat_index]++;
				table._long_count[_seat_index]++;
				// 删除手上的牌
				table.GRR._cards_index[_seat_index][an_long_Index[i]] = 0;

				table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);

			}
			// 刷新手牌包括组合
			cards = new int[GameConstants_HeChi.MAX_HH_COUNT];
			hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);

		}

		if (an_long_count >= 2) {
			table._ti_two_long[_seat_index] = true;
		}

		if (table._ti_two_long[_seat_index] == false) {
			curPlayerStatus.set_status(GameConstants_HeChi.Player_Status_OUT_CARD);// 出牌状态
			table.operate_player_status();
		} else {
			if (table._ti_two_long[_seat_index] == true)
				table._ti_two_long[_seat_index] = false;
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
			int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			table._current_player = next_player;
			table._last_player = next_player;
			table._last_card = 0;
			table.exe_dispatch_card(next_player, GameConstants_HeChi.WIK_NULL, 500);

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
	public boolean handler_operate_card(HeChiHHTable table, int seat_index, int operate_code, int operate_card, int lou_pai) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		// 效验操作
		if ((operate_code != GameConstants_HeChi.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_info("HHHandlerChiPeng_YX 没有这个操作:" + operate_code);
			return false;
		}
		if (operate_code == GameConstants_HeChi.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants_HeChi.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants_HeChi.WIK_NULL }, 1);
		}
		if (seat_index != _seat_index) {
			table.log_info("HHHandlerChiPeng_YX 不是当前玩家操作");
			return false;
		}

		// 放弃操作
		if (operate_code == GameConstants_HeChi.WIK_NULL) {
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			table._playerStatus[_seat_index].set_status(GameConstants_HeChi.Player_Status_OUT_CARD);
			table.operate_player_status();

			return true;
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(HeChiHHTable table, int seat_index) {
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
