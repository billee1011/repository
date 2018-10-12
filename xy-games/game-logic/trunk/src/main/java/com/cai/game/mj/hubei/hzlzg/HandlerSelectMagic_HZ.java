package com.cai.game.mj.hubei.hzlzg;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.RandomUtil;
import com.cai.future.GameSchedule;
import com.cai.game.mj.handler.AbstractMJHandler;

public class HandlerSelectMagic_HZ extends AbstractMJHandler<Table_HZ> {
	protected int _da_dian_card;

	protected int _banker;

	public void reset_status(int banker) {
		_banker = banker;
	}

	@Override
	public void exe(Table_HZ table) {
		// 从牌堆拿出一张牌，并显示在牌桌的正中央
		while (true) {
			int index = RandomUtil.generateRandomNumber(table._all_card_len - table.GRR._left_card_count,
			        table._all_card_len - 1);
			_da_dian_card = table._repertory_card[index];
			if (_da_dian_card != GameConstants.HZ_MAGIC_CARD)
				break;
		}

		if (table.DEBUG_CARDS_MODE)
			_da_dian_card = 0x16;

		// 将翻出来的牌显示在牌桌的正中央
		table.operate_show_card(_banker, GameConstants.Show_Card_Center, 1, new int[] { _da_dian_card },
		        GameConstants.INVALID_SEAT);

		int card_next = 0;

		table.pi_zi_card = _da_dian_card;
		int cur_value = table._logic.get_card_value(_da_dian_card);
		int cur_color = table._logic.get_card_color(_da_dian_card);

		if (cur_color == 3) {
			if (cur_value == 7) {
				card_next = _da_dian_card - 6;
			} else if (cur_value == 4) {
				card_next = _da_dian_card + 2;
			} else {
				card_next = _da_dian_card + 1;
			}
		} else {
			if (cur_value == 9) {
				card_next = _da_dian_card - 8;
			} else {
				card_next = _da_dian_card + 1;
			}
		}

		// 添加鬼
		table._logic.add_magic_card_index(table._logic.switch_to_card_index(card_next));
		table.GRR._especial_card_count = 2;
		table.GRR._especial_show_cards[0] = _da_dian_card + GameConstants.CARD_ESPECIAL_TYPE_PI_ZI;
		table.GRR._especial_show_cards[1] = card_next + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;

		// 处理每个玩家手上的牌，如果有王牌，处理一下
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			int[] hand_cards = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table.switch_to_cards_data(table.GRR._cards_index[i], hand_cards);
			// 玩家客户端刷新一下手牌
			table.operate_player_cards(i, hand_card_count, hand_cards, 0, null);
		}

		GameSchedule.put(new Runnable() {
			@Override
			public void run() {
				// 将翻出来的牌从牌桌的正中央移除
				table.operate_show_card(_banker, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);
			}
		}, 2000, TimeUnit.MILLISECONDS);
	}
}
