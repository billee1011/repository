package com.cai.game.mj.yu.dcwdh.handler;

import com.cai.common.constant.game.GameConstants_KWX;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.handler.MJHandlerGang;
import com.cai.game.mj.yu.dcwdh.MJTable_DCWDH;

public class HandlerGang_DCWDH extends MJHandlerGang<MJTable_DCWDH> {

	public HandlerGang_DCWDH() {
	}

	@Override
	public void reset_status(int seat_index, int provide_player, int center_card, int action, int type, boolean self, boolean d) {
		_seat_index = seat_index;
		_provide_player = provide_player;
		_center_card = center_card;
		_action = action;
		_type = type;
		if (GameConstants_KWX.GANG_TYPE_AN_GANG == _type) {
			_p = false;
		} else {
			_p = true;
		}
		_self = self;
		_double = d;
	}

	@Override
	public void exe(MJTable_DCWDH table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action()) {
				table.operate_player_action(i, true);
			}

			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants_KWX.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();
		table._send_card_data = GameConstants_KWX.INVALID_VALUE;

		table.operate_effect_action(_seat_index, GameConstants_KWX.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
				GameConstants_KWX.INVALID_SEAT);

		this.exe_gang(table);
	}

	@Override
	protected boolean exe_gang(MJTable_DCWDH table) {
		table.player_continue_gang_count[_seat_index]++;

		int cbWeaveIndex = -1;
		if (GameConstants_KWX.GANG_TYPE_AN_GANG == _type) {
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;
		} else if (GameConstants_KWX.GANG_TYPE_JIE_GANG == _type) {
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;

			table.operate_remove_discard(this._provide_player, table.GRR._discard_count[_provide_player]);
		} else if (GameConstants_KWX.GANG_TYPE_ADD_GANG == _type) {
			// 看看是不是有碰的牌，明杠
			// 寻找组合
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
				if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants_KWX.WIK_PENG)) {
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

		table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = _p == true ? 1 : 0;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _center_card;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;

		// 设置用户
		table._current_player = _seat_index;

		// 删除手上的牌
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_center_card)] = 0;
		table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
		// 刷新手牌包括组合
		int cards[] = new int[GameConstants_KWX.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

		WeaveItem weaves[] = new WeaveItem[GameConstants_KWX.MAX_WEAVE];
		int weave_count = table.GRR._weave_count[_seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
			weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
			weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
			weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player + GameConstants_KWX.WEAVE_SHOW_DIRECT;
		}
		table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);

		table.GRR._gang_score[_seat_index].gang_count++;
		table.exe_dispatch_card(_seat_index, _type, 0);

		return true;
	}

}
