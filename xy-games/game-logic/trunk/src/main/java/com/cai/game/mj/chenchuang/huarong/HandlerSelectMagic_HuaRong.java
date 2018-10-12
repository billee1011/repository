package com.cai.game.mj.chenchuang.huarong;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.mj.GameConstants_MJ_XIAN_NING;
import com.cai.future.GameSchedule;
import com.cai.game.mj.handler.AbstractMJHandler;

public class HandlerSelectMagic_HuaRong extends AbstractMJHandler<Table_HuaRong> {
	public static int _da_dian_card;

    protected int _banker;

    public void reset_status(int banker) {
        _banker = banker;
    }

    @SuppressWarnings("static-access")
	@Override
    public void exe(Table_HuaRong table) {
        // 从牌堆拿出一张牌，并显示在牌桌的正中央
        table._send_card_count++;
        _da_dian_card = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
        table.GRR._left_card_count--;

        if (table.DEBUG_CARDS_MODE)
            _da_dian_card = 0x12;

        // 将翻出来的牌显示在牌桌的正中央
        table.operate_show_card(_banker, GameConstants.Show_Card_Center, 1, new int[] { _da_dian_card + GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN},
                GameConstants.INVALID_SEAT);

        int card_next = 0;

        int cur_value = table._logic.get_card_value(_da_dian_card);
        int cur_color = table._logic.get_card_color(_da_dian_card);

        if (cur_color == 3) {
        	card_next = GameConstants_MJ_XIAN_NING.YT_MAGIC_CARD;
        } else {
            if (cur_value == 9) {
                card_next = _da_dian_card - 8;
            } else {
                card_next = _da_dian_card + 1;
            }
        }

        int magic_card_index = table._logic.switch_to_card_index(card_next);

        //添加赖根
        table._logic.add_lai_gen_card(_da_dian_card);
        // 添加鬼
        table._logic.add_magic_card_index(magic_card_index);
        table.GRR._especial_card_count = 2;
        table.GRR._especial_show_cards[0] = _da_dian_card + GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN;
        table.GRR._especial_show_cards[1] = card_next + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;

        // 处理每个玩家手上的牌，如果有王牌，处理一下
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            int[] hand_cards = new int[GameConstants.MAX_COUNT];
            int hand_card_count = table._logic.switch_to_cards_data_ezhou(table.GRR._cards_index[i], hand_cards);
            for (int j = 0; j < hand_card_count; j++) {
                if (table._logic.is_magic_card(hand_cards[j])) {
                    hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
                }else if(table._logic.is_lai_gen_card(hand_cards[j])){
                	hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN;
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
        }, 3000, TimeUnit.MILLISECONDS);
    }

}
