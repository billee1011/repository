package com.cai.game.mj.hubei.huangshi;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.Constants_HuangShi;
import com.cai.future.GameSchedule;
import com.cai.game.mj.handler.AbstractMJHandler;

public class HandlerSelectMagic_HuangShi extends AbstractMJHandler<Table_HuangShi> {
	protected int _da_dian_card;

	protected int _banker;

	public void reset_status(int banker) {
		_banker = banker;
	}

	@SuppressWarnings("static-access")
	@Override
	public void exe(Table_HuangShi table) {
		// 从牌堆拿出一张牌，并显示在牌桌的正中央
		/**
		 * 起牌时，更加摇出来的骰子点数进行选择，可以看小也可以看大，比如摇出来3、4，可以按3也可以按4，从庄家位置逆时针数。
		 * 癞子是在起完牌后要出牌牌之前，第一张摸的那张牌做为翻癞子的痞子牌。翻出来的牌直接变成废牌。
		 */
		table._send_card_count++;
		_da_dian_card = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		table.GRR._left_card_count--;

		if (table.DEBUG_CARDS_MODE) {
			_da_dian_card = 0x07;
		}

		table.da_dian_card = _da_dian_card;

		// 将翻出来的牌显示在牌桌的正中央
		table.operate_show_card(_banker, GameConstants.Show_Card_Center, 1, new int[] { _da_dian_card }, GameConstants.INVALID_SEAT);

		boolean can_fa_cai_gang = table.has_rule(Constants_HuangShi.GAME_RULE_HONG_ZHONG_FA_CAI_GANG);

		int card_next = 0;

		int cur_value = table._logic.get_card_value(_da_dian_card);
		int cur_color = table._logic.get_card_color(_da_dian_card);

		if (cur_color == 3) {
			if (cur_value == 5) {
				if (can_fa_cai_gang) {
					// 红中痞子，白板癞子
					card_next = _da_dian_card + 2;
				} else {
					// 红中痞子，发财癞子
					card_next = _da_dian_card + 1;
				}
			} else if (cur_value == 6) {
				// 发财痞子，白板癞子
				card_next = _da_dian_card + 1;
			} else if (cur_value == 7) {
				if (can_fa_cai_gang) {
					// 白板痞子，白板癞子
					card_next = _da_dian_card;
				} else {
					// 白板痞子，发财癞子
					card_next = _da_dian_card - 1;
				}
			}
		} else {
			if (cur_value == 9) {
				card_next = _da_dian_card - 8;
			} else {
				card_next = _da_dian_card + 1;
			}
		}

		table.magic_card = card_next;

		// 添加鬼
		table._logic.add_magic_card_index(table._logic.switch_to_card_index(card_next));
		table.GRR._especial_card_count = 1;
		table.GRR._especial_show_cards[0] = card_next + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		table.GRR._especial_show_cards[1] = _da_dian_card; // 翻出来的牌是哪一张，在牌桌上只有3张了

		// 处理每个玩家手上的牌，如果有王牌，处理一下
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			int[] hand_cards = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data_huangshi(table.GRR._cards_index[i], hand_cards, can_fa_cai_gang);
			for (int j = 0; j < hand_card_count; j++) {
				if (table._logic.is_magic_card(hand_cards[j])) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				} else if (hand_cards[j] == Constants_HuangShi.HONG_ZHONG_CARD) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_GANG;
				} else if (hand_cards[j] == Constants_HuangShi.FA_CAI_CARD && can_fa_cai_gang) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_GANG;
				}
			}
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
