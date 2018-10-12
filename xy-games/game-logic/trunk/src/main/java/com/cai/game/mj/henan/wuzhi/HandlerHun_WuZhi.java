package com.cai.game.mj.henan.wuzhi;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.future.GameSchedule;
import com.cai.game.mj.handler.AbstractMJHandler;

public class HandlerHun_WuZhi extends AbstractMJHandler<Table_WuZhi> {
	protected int _da_dian_card;
	protected int _banker;

	public void reset_status(int banker) {
		_banker = banker;
	}

	@SuppressWarnings("static-access")
	@Override
	public void exe(Table_WuZhi table) {
		table._send_card_count++;
		_da_dian_card = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		table.GRR._left_card_count--;

		if (table.DEBUG_CARDS_MODE)
			_da_dian_card = 0x03;

		table.da_dian_card = _da_dian_card;

		if (table.DEBUG_MAGIC_CARD) {
			// SSHE后台管理系统，通过“mj#房间号#5#翻的牌”来指定翻出来的牌是哪张
			table.da_dian_card = _da_dian_card = table.magic_card_decidor;
			table.DEBUG_MAGIC_CARD = false;
		}

		// 将翻出来的牌显示在牌桌的正中央
		table.operate_show_card(_banker, GameConstants.Show_Card_Center, 1, new int[] { _da_dian_card }, GameConstants.INVALID_SEAT);

		table.magic_card_index = table._logic.switch_to_card_index(_da_dian_card);

		table._logic.add_magic_card_index(table.magic_card_index);
		table.GRR._especial_card_count = 1;
		table.GRR._especial_show_cards[0] = _da_dian_card + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			int[] hand_cards = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], hand_cards);
			for (int j = 0; j < hand_card_count; j++) {
				if (table._logic.is_magic_card(hand_cards[j])) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
			}

			table.operate_player_cards_lsdy(i, hand_card_count, hand_cards, 0, null);
		}

		GameSchedule.put(new Runnable() {
			@Override
			public void run() {
				// 将翻出来的牌从牌桌的正中央移除
				table.operate_show_card(_banker, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);

				table.exe_dispatch_card(table._current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
			}
		}, 1000, TimeUnit.MILLISECONDS);
	}
}
