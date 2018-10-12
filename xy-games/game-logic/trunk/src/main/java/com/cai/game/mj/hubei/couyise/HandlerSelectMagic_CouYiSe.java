package com.cai.game.mj.hubei.couyise;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.Constants_CouYiSe;
import com.cai.future.GameSchedule;
import com.cai.game.mj.handler.AbstractMJHandler;

public class HandlerSelectMagic_CouYiSe extends AbstractMJHandler<Table_CouYiSe> {
    protected int _pi_zi;

    protected int _banker;

    public void reset_status(int banker) {
        _banker = banker;
    }

    @Override
    public void exe(Table_CouYiSe table) {
        // 从牌堆拿出一张牌，并显示在牌桌的正中央
        /**
         * 摇出来的两个骰子，点数之和用来起牌，比如摇出来4和5，起牌时，以玩家1为起始位置，逆时针数第9家（玩家1）的第10墩牌，开始抓牌。
         * 翻牌时，以起牌位置（玩家1）的顺时针（从右到左）的第1墩牌的上面一张牌，为翻出来的牌。
         * 翻出来的牌，可以再次使用，留在牌堆里作为有效牌使用，可以被杠走也可以被抓走。
         * 癞子牌参与吃时，任意牌值的癞子都可以参与吃（只能一癞子一正常牌参与吃，否则就是锁碰了）。
         */
        int pi_zi_index = table._all_card_len - (table.tou_zi_dian_shu[0] + table.tou_zi_dian_shu[1]) * 2;
        _pi_zi = table._repertory_card[pi_zi_index];

        if (table.DEBUG_CARDS_MODE)
            _pi_zi = 0x16;

        table.pi_zi = _pi_zi;

        // 将翻出来的牌显示在牌桌的正中央
        table.operate_show_card(_banker, GameConstants.Show_Card_Center, 1, new int[] { _pi_zi },
                GameConstants.INVALID_SEAT);

        int card_next = 0;

        int cur_value = table._logic.get_card_value(_pi_zi);
        int cur_color = table._logic.get_card_color(_pi_zi);

        if (cur_color == 3) {
            if (cur_value == 4) { // 北风为痞子，则跳过红中，发财为癞子;
                card_next = _pi_zi + 2;
            } else if (cur_value == 5) { // 若翻到红中为痞子，则发财为痞子，白板为癞子
                _pi_zi += 1;
                table.pi_zi = _pi_zi;
                card_next = _pi_zi + 1;
            } else if (cur_value == 7) { // 若翻到白板为痞子，则东风为癞子
                card_next = _pi_zi - 6;
            } else {
                card_next = _pi_zi + 1;
            }
        } else {
            if (cur_value == 9) {
                card_next = _pi_zi - 8;
            } else {
                card_next = _pi_zi + 1;
            }
        }

        table.magic_card = card_next;
        table.magic_card_index = table._logic.switch_to_card_index(card_next);

        // 添加鬼
        table._logic.add_magic_card_index(table._logic.switch_to_card_index(card_next));
        table.GRR._especial_card_count = 2;
        table.GRR._especial_show_cards[0] = card_next + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
        table.GRR._especial_show_cards[1] = _pi_zi + GameConstants.CARD_ESPECIAL_TYPE_PI_ZI;

        // 处理每个玩家手上的牌，如果有王牌，处理一下
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            int[] hand_cards = new int[GameConstants.MAX_COUNT];
            int hand_card_count = table._logic.switch_to_cards_data_couyise(table.GRR._cards_index[i], hand_cards,
                    table.pi_zi);
            for (int j = 0; j < hand_card_count; j++) {
                if (table._logic.is_magic_card(hand_cards[j])) {
                    hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
                } else if (hand_cards[j] == table.pi_zi) {
                    hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_PI_ZI;
                } else if (hand_cards[j] == Constants_CouYiSe.HZ_CARD) {
                    hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HZ;
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
