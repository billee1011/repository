package com.cai.game.universal.bullfight.handler;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.game.universal.bullfight.BullFightTable;

public class BullFightHandlerDispatchCard extends BullFightHandler {
    protected int _seat_index;
    protected int _send_card_data;

    protected int _type;

    protected GangCardResult m_gangCardResult;

    public BullFightHandlerDispatchCard() {
        m_gangCardResult = new GangCardResult();
    }

    public void reset_status(int seat_index, int type) {
        _seat_index = seat_index;
        _type = type;
    }

    @Override
    public void exe(BullFightTable table) {
    }

    @Override
    public boolean handler_player_out_card(BullFightTable table, int seat_index, int card) {
        card = table.get_real_card(card);

        if (table._logic.is_valid_card(card) == false) {
            table.log_error("出牌,牌型出错");
            return false;
        }

        if (seat_index != _seat_index) {
            table.log_error("出牌,没到出牌");
            return false;
        }

        table.exe_out_card(_seat_index, card, GameConstants.WIK_NULL);

        return true;
    }

    @Override
    public boolean handler_player_be_in_room(BullFightTable table, int seat_index) {
        return true;
    }
}
