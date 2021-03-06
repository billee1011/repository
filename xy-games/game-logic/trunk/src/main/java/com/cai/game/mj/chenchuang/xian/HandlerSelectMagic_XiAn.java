package com.cai.game.mj.chenchuang.xian;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.mj.Constants_MJ_XI_AN;
import com.cai.game.mj.handler.AbstractMJHandler;

public class HandlerSelectMagic_XiAn extends AbstractMJHandler<Table_XiAn> {
	protected int _banker;

	public void reset_status(int banker) {
		_banker = banker;
	}

	@Override
	public void exe(Table_XiAn table) {

		if (table.has_rule(Constants_MJ_XI_AN.GAME_RULE_HONG_ZHONG_LAI_ZI)) {
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(Constants_MJ_XI_AN.HZ_MAGIC_CARD));
			table.GRR._especial_card_count = 1;
			table.GRR._especial_show_cards[0] = Constants_MJ_XI_AN.HZ_MAGIC_CARD + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}

		// 处理每个玩家手上的牌，如果有王牌，处理一下
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			int[] hand_cards = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], hand_cards);
			for (int j = 0; j < hand_card_count; j++) {
				if (table._logic.is_magic_card(hand_cards[j])) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
			}
			// 玩家客户端刷新一下手牌
			table.operate_player_cards(i, hand_card_count, hand_cards, 0, null);
		}
	}

}
