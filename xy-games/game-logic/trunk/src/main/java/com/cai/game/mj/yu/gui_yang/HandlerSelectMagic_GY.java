package com.cai.game.mj.yu.gui_yang;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.GameConstants_GY;
import com.cai.future.GameSchedule;
import com.cai.game.mj.handler.AbstractMJHandler;

public class HandlerSelectMagic_GY extends AbstractMJHandler<Table_GY> {
	public int _da_dian_card;

	protected int _banker;

	public void reset_status(int banker) {
		_banker = banker;
	}

	@Override
	public void exe(Table_GY table) {
		if (table.GRR._left_card_count == 0) {
			// 处理每个玩家手上的牌，如果有王牌，处理一下
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				int[] hand_cards = new int[GameConstants.MAX_COUNT];
				int hand_card_count = table.switch_to_cards_data(table.GRR._cards_index[i], hand_cards, i);

				table.operate_player_cards(i, 0, null, 0, null);
				table.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, hand_cards, GameConstants.INVALID_SEAT);
			}
			return;
		}

		// 从牌堆拿出一张牌，并显示在牌桌的正中央
		// int index = RandomUtil.generateRandomNumber(table._all_card_len -
		// table.GRR._left_card_count, table._all_card_len - 1);
		// _da_dian_card = table._repertory_card[index];
		_da_dian_card = table._repertory_card[table._all_card_len - table.GRR._left_card_count];

		if (table.DEBUG_CARDS_MODE)
			_da_dian_card = 0x27;

		table.GRR._especial_show_cards[table.GRR._especial_card_count++] = _da_dian_card;

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

		if (table.has_rule(GameConstants_GY.GAME_RULE_TYPE_BEN_JI)) {
			table.add_ji_card_index(table._logic.switch_to_card_index(_da_dian_card));
			table.add_ji_card_index(table._logic.switch_to_card_index(card_next));

			if (_da_dian_card == GameConstants_GY.YJ_CARD || card_next == GameConstants_GY.YJ_CARD)
				table.jin_ji[0] = true;
			if (table.has_rule(GameConstants_GY.GAME_RULE_WU_GU_JI)
					&& (_da_dian_card == GameConstants_GY.BA_TONG_CARD || card_next == GameConstants_GY.BA_TONG_CARD))
				table.jin_ji[1] = true;
		}

		if (table.has_rule(GameConstants_GY.GAME_RULE_TYPE_SWING_JI)) {
			table.add_ji_card_index(table._logic.switch_to_card_index(card_previous));
			table.add_ji_card_index(table._logic.switch_to_card_index(card_next));

			if (card_previous == GameConstants_GY.YJ_CARD || card_next == GameConstants_GY.YJ_CARD)
				table.jin_ji[0] = true;
			if (table.has_rule(GameConstants_GY.GAME_RULE_WU_GU_JI)
					&& (card_previous == GameConstants_GY.BA_TONG_CARD || card_next == GameConstants_GY.BA_TONG_CARD))
				table.jin_ji[1] = true;
		}

		if (table.has_rule(GameConstants_GY.GAME_RULE_MAN_TANG_JI)) {
			table.add_ji_card_index(table._logic.switch_to_card_index(card_next));
			if (card_next == GameConstants_GY.YJ_CARD)
				table.jin_ji[0] = true;
			if (table.has_rule(GameConstants_GY.GAME_RULE_WU_GU_JI) && card_next == GameConstants_GY.BA_TONG_CARD)
				table.jin_ji[1] = true;
		}

		// 处理每个玩家手上的牌，如果有王牌，处理一下
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.GRR._win_order[i] == 1)
				continue;
			int[] hand_cards = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table.switch_to_cards_data(table.GRR._cards_index[i], hand_cards, i);

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
