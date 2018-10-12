package com.cai.game.mj.guangdong.heyuanhd;

import com.cai.common.constant.GameConstants;
import com.cai.game.mj.handler.AbstractMJHandler;
import com.cai.common.constant.game.mj.GameConstants_HYHD;

/**
 * 
 * 
 *
 * @author shiguoqiong date: 2018年3月21日 下午5:25:12 <br/>
 */
public class HandlerSelectMagic_HYHD extends AbstractMJHandler<Table_HYHD> {

	protected int _banker;

	public void reset_status(int banker) {
		_banker = banker;
	}

	@Override
	public void exe(Table_HYHD table) {
		// 选取鬼牌
		init_magicCard(table);
		
		// 处理每个玩家手上的牌,鬼牌
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			// 显示鬼牌
			//table.showSpecialCard(i);
			// 刷新手牌
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

	// 鬼牌初始化
	public void init_magicCard(Table_HYHD table) {
		table._logic.clean_magic_cards();
		
		if (table.has_rule(GameConstants_HYHD.GAME_RULE_SI_HUA_GUI)) {
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(GameConstants_HYHD.CHUN_MAGIC_CARD));
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(GameConstants_HYHD.XIA_MAGIC_CARD));
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(GameConstants_HYHD.QIU_MAGIC_CARD));
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(GameConstants_HYHD.DONG_MAGIC_CARD));
			table.GRR._especial_card_count = 1;
			table.GRR._especial_show_cards[0] = 0x61;
			
		}
		if (table.has_rule(GameConstants_HYHD.GAME_RULE_BA_HUA_GUI)) {
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(GameConstants_HYHD.CHUN_MAGIC_CARD));
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(GameConstants_HYHD.XIA_MAGIC_CARD));
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(GameConstants_HYHD.QIU_MAGIC_CARD));
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(GameConstants_HYHD.DONG_MAGIC_CARD));
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(GameConstants_HYHD.MEI_MAGIC_CARD));
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(GameConstants_HYHD.LAN_MAGIC_CARD));
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(GameConstants_HYHD.ZHU_MAGIC_CARD));
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(GameConstants_HYHD.JU_MAGIC_CARD));
			table.GRR._especial_card_count = 1;
			table.GRR._especial_show_cards[0] = 0x62;
		}
	}
}
