package com.cai.game.mj.yu.gd_tdh;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.GameConstants_TDH;
import com.cai.future.GameSchedule;
import com.cai.game.mj.handler.AbstractMJHandler;

public class HandlerSelectMagic_TDH extends AbstractMJHandler<Table_TDH> {
	public int _da_dian_card;

	protected int _banker;

	public void reset_status(int banker) {
		_banker = banker;
	}

	@Override
	public void exe(Table_TDH table) {

		table._logic.clean_magic_cards();

		if (table.has_rule(GameConstants_TDH.GAME_RULE_HZ_GUI)) {
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(GameConstants_TDH.HZ_MAGIC_CARD));
			table.GRR._especial_card_count = 1;
			table.GRR._especial_show_cards[0] = GameConstants_TDH.HZ_MAGIC_CARD + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}
		if (table.has_rule(GameConstants_TDH.GAME_RULE_BB_GUI)) {
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(GameConstants_TDH.BB_MAGIC_CARD));
			table.GRR._especial_card_count = 1;
			table.GRR._especial_show_cards[0] = GameConstants_TDH.BB_MAGIC_CARD + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}
		if (table.has_rule(GameConstants_TDH.GAME_RULE_SI_HUA_GUI)) {
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(GameConstants_TDH.CHUN_MAGIC_CARD));
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(GameConstants_TDH.XIA_MAGIC_CARD));
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(GameConstants_TDH.QIU_MAGIC_CARD));
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(GameConstants_TDH.DONG_MAGIC_CARD));
		}
		if (table.has_rule(GameConstants_TDH.GAME_RULE_BA_HUA_GUI)) {
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(GameConstants_TDH.CHUN_MAGIC_CARD));
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(GameConstants_TDH.XIA_MAGIC_CARD));
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(GameConstants_TDH.QIU_MAGIC_CARD));
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(GameConstants_TDH.DONG_MAGIC_CARD));
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(GameConstants_TDH.MEI_MAGIC_CARD));
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(GameConstants_TDH.LAN_MAGIC_CARD));
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(GameConstants_TDH.ZHU_MAGIC_CARD));
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(GameConstants_TDH.JU_MAGIC_CARD));
		}
		if (table.has_rule(GameConstants_TDH.GAME_RULE_FAN_GUI) || table.has_rule(GameConstants_TDH.GAME_RULE_FAN_SHUANG_GUI)) {
			// 从牌堆拿出一张牌，并显示在牌桌的正中央
			table._send_card_count++;
			_da_dian_card = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
			table.GRR._left_card_count--;

			if (table.DEBUG_CARDS_MODE)
				_da_dian_card = 0x29;

			// 将翻出来的牌显示在牌桌的正中央
			table.operate_show_card(_banker, GameConstants.Show_Card_Center, 1, new int[] { _da_dian_card }, GameConstants.INVALID_SEAT);

			int card_next = get_next_card(table, _da_dian_card);

			// 添加鬼
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(card_next));
			table.GRR._especial_card_count = 1;
			table.GRR._especial_show_cards[0] = card_next + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			if (table.has_rule(GameConstants_TDH.GAME_RULE_FAN_SHUANG_GUI)) {
				table._logic.add_magic_card_index(table._logic.switch_to_card_index(get_next_card(table, card_next)));
				table.GRR._especial_card_count = 2;
				table.GRR._especial_show_cards[1] = get_next_card(table, card_next) + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}

			GameSchedule.put(new Runnable() {
				@Override
				public void run() {
					// 将翻出来的牌从牌桌的正中央移除
					table.operate_show_card(_banker, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);
				}
			}, 2000, TimeUnit.MILLISECONDS);
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

	private int get_next_card(Table_TDH table, int card) {
		int card_next = 0;

		int cur_value = table._logic.get_card_value(card);
		int cur_color = table._logic.get_card_color(card);

		if (cur_color == 3) {
			if (cur_value == 7) {
				card_next = card - 2;
			} else {
				card_next = card + 1;
			}
		} else {
			if (cur_value == 9) {
				card_next = card - 8;
			} else {
				card_next = card + 1;
			}
		}

		return card_next;
	}
}
