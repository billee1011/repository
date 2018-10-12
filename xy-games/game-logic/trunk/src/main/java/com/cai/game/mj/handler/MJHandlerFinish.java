package com.cai.game.mj.handler;

import com.cai.common.constant.MsgConstants;
import com.cai.game.mj.AbstractMJTable;

import protobuf.clazz.Protocol.RoomResponse;

public class MJHandlerFinish extends AbstractMJHandler<AbstractMJTable> {

    @Override
    public void exe(AbstractMJTable table) {
    }

    @Override
    public boolean handler_player_be_in_room(AbstractMJTable table, int seat_index) {
    	RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
        roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_WHEN_GAME_FINISH);
        
        table.send_response_to_room(roomResponse);
        
        table.log_warn(" 小局结束的时候，断线重连了！！");
        
        return true;
    }
}
