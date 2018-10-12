package com.cai.game.btz.handler;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.game.btz.BTZTable;
import com.cai.game.btz.BTZUtils;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class BTZHandlerAddJetton<T extends BTZTable> extends BTZHandler<T>{
	protected int _seat_index;
	protected int _game_status;
	//private int _current_player =MJGameConstants.INVALID_SEAT; 
	
	protected GangCardResult m_gangCardResult;
	
	public BTZHandlerAddJetton(){
		m_gangCardResult = new GangCardResult();
	}
	
	public void reset_status(int seat_index,int game_status){
		_seat_index = seat_index;
		_game_status = game_status;

	}
	
	@Override
	public void exe(T table) {
	}
	
}
