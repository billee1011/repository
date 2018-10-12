package com.cai.game.hjk.handler;

import com.cai.common.domain.GangCardResult;
import com.cai.game.hjk.HJKTable;

public class HJKHandlerDispatchCard extends HJKHandler{
	protected int _status;
	protected int _send_card_data;

	//private int _current_player =MJGameConstants.INVALID_SEAT; 
	
	protected GangCardResult m_gangCardResult;
	
	public HJKHandlerDispatchCard(){
		m_gangCardResult = new GangCardResult();
	}
	
	public void reset_status(int status){
		_status = status;
	}
	
	@Override
	public void exe(HJKTable table) {
	}
	
	/***
	 * //用户出牌
	 */
	@Override
	public boolean handler_player_out_card(HJKTable table,int seat_index, int card) {
		// 错误断言
		card = table.get_real_card(card);
		


		return true;
	}
	
	
	@Override
	public boolean handler_player_be_in_room(HJKTable table,int seat_index) {
	


		return true;
	}

}
