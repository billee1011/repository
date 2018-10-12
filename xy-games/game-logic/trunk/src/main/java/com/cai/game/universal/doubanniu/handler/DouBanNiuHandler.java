package com.cai.game.universal.doubanniu.handler;

import com.cai.common.domain.Player;
import com.cai.game.universal.doubanniu.DouBanNiuTable;

import protobuf.clazz.Protocol.RoomRequest;

public abstract class DouBanNiuHandler {

    public abstract void exe(DouBanNiuTable table);

    public boolean handler_player_ready(DouBanNiuTable table, int seat_index) {
        return table.handler_player_ready(seat_index, false);
    }

    public boolean handler_player_be_in_room(DouBanNiuTable table, int seat_index) {
        return true;
    }

    public boolean handler_player_out_card(DouBanNiuTable table, int seat_index, int card) {
        return false;
    }

    public boolean handler_operate_card(DouBanNiuTable table, int seat_index, int operate_code, int operate_card,
            int luoCode) {
        return false;
    }

    public boolean handler_release_room(DouBanNiuTable table, Player player, int opr_code) {
        return table.handler_release_room(player, opr_code);
    }

    public boolean handler_audio_chat(DouBanNiuTable table, Player player, com.google.protobuf.ByteString chat, int l,
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
