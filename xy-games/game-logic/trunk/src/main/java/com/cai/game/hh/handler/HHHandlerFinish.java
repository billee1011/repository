package com.cai.game.hh.handler;

import com.cai.common.constant.MsgConstants;
import com.cai.game.hh.HHTable;

import protobuf.clazz.Protocol.RoomResponse;

public class HHHandlerFinish<T extends HHTable> extends HHHandler<T> {

	@Override
	public void exe(T table) {
	}

	@Override
    public boolean handler_player_be_in_room(T table, int seat_index) {
    	RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
        roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_WHEN_GAME_FINISH);
        
        table.send_response_to_room(roomResponse);
        
        table.log_warn(" 小局结束的时候，断线重连了！！");
        
        return true;
    }
}
