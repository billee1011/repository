package com.cai.game.btz.handler;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.game.btz.BTZTable;
import com.cai.game.btz.BTZUtils;

public class BTZHandlerDispatchCard<T extends BTZTable> extends BTZHandler<T>{
	protected int _seat_index;
	protected int _send_card_data;
	
	protected int _type;
	//private int _current_player =MJGameConstants.INVALID_SEAT; 
	
	protected GangCardResult m_gangCardResult;
	
	public BTZHandlerDispatchCard(){
		m_gangCardResult = new GangCardResult();
	}
	
	public void reset_status(int seat_index,int type){
		_seat_index = seat_index;
		_type= type;
	}
	
	@Override
	public void exe(T table) {
	}
	
	/***
	 * //用户出牌
	 */
	@Override
	public boolean handler_player_out_card(T table,int seat_index, int card) {
		
		

		// 效验参数
		if (seat_index != _seat_index) {
			return false;
		}

//		if (card == MJGameConstants.ZZ_MAGIC_CARD && table.is_mj_type(MJGameConstants.GAME_TYPE_HZ)) {
//			table.send_sys_response_to_player(seat_index, "癞子牌不能出癞子");
//			table.log_error("癞子牌不能出癞子");
//			return false;
//		}

		return true;
	}
	
	
	@Override
	public boolean handler_player_be_in_room(T table,int seat_index) {
	
		return true;
	}

}
