package com.cai.game.mj.handler.xingyi;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.GameConstants_ZYZJ;
import com.cai.future.GameSchedule;
import com.cai.game.mj.handler.AbstractMJHandler;

public class HandlerSelectMagic_XY extends AbstractMJHandler<Table_XY> {
	public int _da_dian_card;

	protected int _banker;

	public void reset_status(int banker) {
		_banker = banker;
	}

	@Override
	public void exe(Table_XY table) {
		if (table.GRR._left_card_count == 0) {
			// 处理每个玩家手上的牌，如果有王牌，处理一下
			/*for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				int[] hand_cards = new int[GameConstants.MAX_COUNT];
				int hand_card_count = table.switch_to_cards_data(table.GRR._cards_index[i], hand_cards, i);

				table.operate_player_cards(i, 0, null, 0, null);
				table.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, hand_cards, GameConstants.INVALID_SEAT);
			}*/
			return;
		}

		// 从牌堆拿出一张牌，并显示在牌桌的正中央

		_da_dian_card = table._repertory_card[table._all_card_len - table.GRR._left_card_count];

		//if (table.DEBUG_CARDS_MODE)
		//_da_dian_card = 0x27;
		
		if (table.DEBUG_CARDS_MODE) {
			//_da_dian_card = 0x19;
			//table.GRR._left_card_count= 0;
		}

		//_da_dian_card = 0x01;
		
		table.GRR._especial_show_cards[1] = _da_dian_card;

		// 将翻出来的牌显示在牌桌的正中央
		table.operate_show_card(_banker, GameConstants.Show_Card_Center, 1, new int[] { _da_dian_card }, GameConstants.INVALID_SEAT);

		int card_next = 0;
		int card_previous = 0;

		int cur_value = table._logic.get_card_value(_da_dian_card);
		int cur_color = table._logic.get_card_color(_da_dian_card);
		if (cur_value == 9) {
			card_next = _da_dian_card - 8;
		} else {
			card_next = _da_dian_card + 1;
		}
		if (cur_value == 1) {
			card_previous = _da_dian_card + 8;
		} else {
			card_previous = _da_dian_card - 1;
		}

		if (table.has_rule(GameConstants_ZYZJ.GAME_RULE_BEN_ZYZJ)) {//本鸡
			table.add_ji_card_index(table._logic.switch_to_card_index(_da_dian_card));
			table.ben_ji_card = _da_dian_card;
			table.GRR._especial_show_cards[2] = table.ben_ji_card;
			if (_da_dian_card == GameConstants_ZYZJ.YJ_CARD )
				table.jin_ji[0] = true;
			if (table.has_rule(GameConstants_ZYZJ.GAME_RULE_WG_ZYZJ)
					&& (_da_dian_card == GameConstants_ZYZJ.BA_TONG_CARD))
				table.jin_ji[1] = true;
		}

		if (table.has_rule(GameConstants_ZYZJ.GAME_RULE_YB_ZYZJ)) {//摇摆鸡
			table.shang_xia_ji[0] = card_previous;
			table.shang_xia_ji[1] = card_next;
			table.add_ji_card_index(table._logic.switch_to_card_index(card_previous));
			table.add_ji_card_index(table._logic.switch_to_card_index(card_next));
			table.GRR._especial_show_cards[3] = table.shang_xia_ji[0];
			table.GRR._especial_show_cards[4] = table.shang_xia_ji[1];
			if (card_previous == GameConstants_ZYZJ.YJ_CARD || card_next == GameConstants_ZYZJ.YJ_CARD)
				table.jin_ji[0] = true;
			if (table.has_rule(GameConstants_ZYZJ.GAME_RULE_WG_ZYZJ)
					&& (card_previous == GameConstants_ZYZJ.BA_TONG_CARD || card_next == GameConstants_ZYZJ.BA_TONG_CARD))
				table.jin_ji[1] = true;
		}else if(table.has_rule(GameConstants_ZYZJ.GAME_RULE_FAN_ZYZJ)){//翻鸡
			table.fan_ji_card = card_next;
			table.GRR._especial_show_cards[5] = table.fan_ji_card;
			table.add_ji_card_index(table._logic.switch_to_card_index(card_next));
			if (card_next == GameConstants_ZYZJ.YJ_CARD)
				table.jin_ji[0] = true;
			if (table.has_rule(GameConstants_ZYZJ.GAME_RULE_WG_ZYZJ) && card_next == GameConstants_ZYZJ.BA_TONG_CARD)
				table.jin_ji[1] = true;
		}

		// 处理每个玩家手上的牌，如果有王牌，处理一下
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.GRR._win_order[i] == 1)
				continue;
			int[] hand_cards = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], hand_cards);

			table.operate_player_cards(i, 0, null, 0, null);
			table.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, hand_cards, GameConstants.INVALID_SEAT);
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
