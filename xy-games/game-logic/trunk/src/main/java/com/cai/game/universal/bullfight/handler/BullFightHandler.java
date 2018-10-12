package com.cai.game.universal.bullfight.handler;

import com.cai.common.domain.Player;
import com.cai.game.universal.bullfight.BullFightTable;

import protobuf.clazz.Protocol.RoomRequest;

public abstract class BullFightHandler {

    public abstract void exe(BullFightTable table);

    public boolean handler_player_be_in_room(BullFightTable table, int seat_index) {
        return true;
    }

    public boolean handler_observer_be_in_room(BullFightTable table, Player player) {
        return true;
    }

    public boolean handler_player_out_card(BullFightTable table, int seat_index, int card) {
        return false;
    }

    public boolean handler_operate_card(BullFightTable table, int seat_index, int operate_code, int operate_card,
            int luoCode) {
        return false;
    }

    public boolean handler_call_banker(BullFightTable table, int seat_index, int call_banker) {
        return false;
    }

    public boolean handler_add_jetton(BullFightTable table, int seat_index, int jetton) {
        return false;
    }

    public boolean handler_open_cards(BullFightTable table, int seat_index, boolean open_flag) {
        return false;
    }

    public boolean handler_release_room(BullFightTable table, Player player, int opr_code) {
        return table.handler_release_room(player, opr_code);
    }

    public boolean handler_audio_chat(BullFightTable table, Player player, com.google.protobuf.ByteString chat, int l,
            float audio_len) {
        return false;
    }

    public boolean handler_requst_audio_chat(int room_id, long account_id, RoomRequest room_rq) {
        return true;
    }

    public boolean handler_requst_emjoy_chat(int room_id, long account_id, RoomRequest room_rq) {
        return true;
    }
}
