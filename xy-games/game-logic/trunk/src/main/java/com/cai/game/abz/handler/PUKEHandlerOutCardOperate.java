package com.cai.game.abz.handler;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.game.abz.PUKETable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class PUKEHandlerOutCardOperate<T extends PUKETable> extends AbstractPUKEHandler<T> {

	public int _out_card_player = GameConstants.INVALID_SEAT; // 出牌用户
	public int[] _out_cards_data =  new int[GameConstants.WSK_MAX_COUNT]; // 出牌扑克
	public int  _out_card_count =  0;
	public int _out_type;
	
	
	
	
	
	public void reset_status(int seat_index,int cards[],int card_count,int is_out){
		_out_card_player = seat_index;
		_out_cards_data =  new int[card_count]; 
		for(int i = 0; i<card_count;i++)
		{
			_out_cards_data[i] = cards[i];
		}
		_out_card_count = card_count;
		_out_type=is_out;
	}
	
	
	@Override
	public void exe(T table) {
	}
	
	
	
	
	@Override
	public boolean handler_player_be_in_room(T table,int seat_index) {		
		return true;
	}
	
	

}
