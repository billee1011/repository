package com.cai.game.hongershi;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.hongershi.HongErShiConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.dictionary.SysParamDict;
import com.cai.game.hh.handler.HHHandlerChiPeng;

public class HandlerChiPeng extends HHHandlerChiPeng<HongErShiTable> {

	private int[] _weave_cards;

	public void reset_status(int seat_index, int provider, int action, int card, int type, int[] weave_cards) {
		_seat_index = seat_index;
		_action = action;
		_card = card;
		_provider = provider;
		_type = type;
		_weave_cards = weave_cards;
	}

	@Override
	public void exe(HongErShiTable table) {

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		if (_action == HongErShiConstants.WIK_CHI) {
			int wIndex = table.GRR._weave_count[_seat_index]++;
			table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
			table.GRR._weave_items[_seat_index][wIndex].center_card = _card;
			table.GRR._weave_items[_seat_index][wIndex].weave_kind = _action;
			table.GRR._weave_items[_seat_index][wIndex].weave_card = _weave_cards;

			table.GRR._weave_items[_seat_index][wIndex].provide_player = _provider;
		}
		table._current_player = _seat_index;

		// table.operate_effect_action(_seat_index,
		// GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action },
		// 1, GameConstants.INVALID_SEAT);

		table.operate_player_get_card(this._provider, 0, null, GameConstants.INVALID_SEAT, false);
		table.operate_out_card(this._provider, 0, null, GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);

		table.operate_player_cards(_seat_index, table.GRR._card_count[_seat_index], table.GRR._cards_data[_seat_index],
				table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
		if (_action != HongErShiConstants.WIK_CHI) {
			int wIndex = table.GRR._weave_count[_seat_index]++;
			table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
			table.GRR._weave_items[_seat_index][wIndex].center_card = _card;
			table.GRR._weave_items[_seat_index][wIndex].weave_kind = _action;
			table.GRR._weave_items[_seat_index][wIndex].weave_card = _weave_cards;

			table.GRR._weave_items[_seat_index][wIndex].provide_player = _provider;
		}

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		if (_action == HongErShiConstants.WIK_CHI) {
			curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
		} else {

			int gameId = table.getGame_id() == 0 ? 5 : table.getGame_id();
			SysParamModel sysParamModel1105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1105);
			int dispatch_time = 3000;
			if (sysParamModel1105 != null && sysParamModel1105.getVal2() > 0 && sysParamModel1105.getVal2() < 10000) {
				dispatch_time = sysParamModel1105.getVal2();
			}
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_status();
			table.operate_player_action(_seat_index, false);

			dispatch_time = 0;
			table.exe_dispatch_card(_seat_index, GameConstants.WIK_PENG, dispatch_time);
		}
	}

	@Override
	public boolean handler_operate_card(HongErShiTable table, int seat_index, int operate_code, int operate_card, int lou_pai) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_info("没有这个操作:" + operate_code);
			return false;
		}
		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);
		}
		if (seat_index != _seat_index) {
			table.log_info("不是当前玩家操作");
			return false;
		}

		if (operate_code == GameConstants.WIK_NULL) {
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();

			return true;
		}

		return true;
	}

	/***
	 * //用户出牌
	 */
	@Override
	public boolean handler_player_out_card(HongErShiTable table, int seat_index, int card) {
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

	@Override
	public boolean handler_player_be_in_room(HongErShiTable table, int seat_index) {

		table.handler_player_be_in_room(table, seat_index);
		return true;
	}
}
