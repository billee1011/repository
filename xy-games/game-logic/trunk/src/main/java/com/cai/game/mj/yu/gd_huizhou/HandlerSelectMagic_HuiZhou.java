package com.cai.game.mj.yu.gd_huizhou;

import com.cai.common.constant.GameConstants;
import com.cai.game.mj.handler.AbstractMJHandler;

/**
 * 
 * 
 *
 * @author shiguoqiong date: 2018年3月21日 下午5:25:12 <br/>
 */
public class HandlerSelectMagic_HuiZhou extends AbstractMJHandler<Table_HuiZhou> {

	protected int _banker;

	public void reset_status(int banker) {
		_banker = banker;
	}

	@Override
	public void exe(Table_HuiZhou table) {
		// 处理每个玩家手上的牌，如果有王牌，处理一下
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			int[] hand_cards = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], hand_cards);
			for (int j = 0; j < hand_card_count; j++) {
				if (table._logic.is_magic_card(hand_cards[j])) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_GUI;
				}
			}
			// 玩家客户端刷新一下手牌
			table.operate_player_cards(i, hand_card_count, hand_cards, 0, null);
		}
	}
}
