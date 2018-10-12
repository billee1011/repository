package com.cai.game.universal.creazybullfight.handler;

import com.cai.common.domain.Player;
import com.cai.game.universal.creazybullfight.CreazyBullFightTable;

import protobuf.clazz.Protocol.RoomRequest;

public abstract class CreazyBullFightHandler {
    public abstract void exe(CreazyBullFightTable table);

    public boolean handler_player_ready(CreazyBullFightTable table, int seat_index) {
        return table.handler_player_ready(seat_index, false);
    }

    public boolean handler_player_be_in_room(CreazyBullFightTable table, int seat_index) {
        return true;
    }

    public boolean handler_player_out_card(CreazyBullFightTable table, int seat_index, int card) {
        return false;
    }

    public boolean handler_operate_card(CreazyBullFightTable table, int seat_index, int operate_code, int operate_card,
            int luoCode) {
        return false;
    }

    public boolean handler_release_room(CreazyBullFightTable table, Player player, int opr_code) {
        return table.handler_release_room(player, opr_code);
    }

    public boolean handler_audio_chat(CreazyBullFightTable table, Player player, com.google.protobuf.ByteString chat,
            int l, float audio_len) {
        return false;
    }

    public boolean handler_requst_audio_chat(int room_id, long account_id, RoomRequest room_rq) {
        return true;
    }

    public boolean handler_requst_emjoy_chat(int room_id, long account_id, RoomRequest room_rq) {
        return true;
    }
}
